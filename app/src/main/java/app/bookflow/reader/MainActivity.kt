package app.bookflow.reader

import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.bookflow.reader.core.data.DocxTextExtractor
import app.bookflow.reader.core.data.RuleBasedSceneDirector
import app.bookflow.reader.core.data.SupabaseVoiceRenderer
import app.bookflow.reader.core.domain.BookDocument
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() { override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { BookFlowApp() } } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun BookFlowApp() {
    val context = LocalContext.current
    var importedBook by remember { mutableStateOf<BookDocument?>(null) }; var openedBook by remember { mutableStateOf<BookDocument?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) {
        runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        val mime = context.contentResolver.getType(uri).orEmpty(); val title = queryDisplayName(context, uri) ?: "Libro"
        val text = when { mime == "text/plain" || title.endsWith(".txt",true) -> runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull(); mime.contains("wordprocessingml",true) || title.endsWith(".docx",true) -> runCatching { context.contentResolver.openInputStream(uri)?.use(DocxTextExtractor::extract) }.getOrNull(); else -> null }
        importedBook = BookDocument(uri.toString(),title,mime,text)
    } }
    MaterialTheme { Surface(Modifier.fillMaxSize()) { if(openedBook!=null) ReaderScreen(openedBook!!){openedBook=null} else Scaffold(topBar={TopAppBar(title={Text("BookFlow")})}){p->LibraryScreen(Modifier.padding(p),importedBook,{launcher.launch(arrayOf("application/pdf","application/vnd.openxmlformats-officedocument.wordprocessingml.document","text/plain"))}){openedBook=it}} } }
}

@Composable private fun LibraryScreen(modifier:Modifier,book:BookDocument?,onImport:()->Unit,onOpen:(BookDocument)->Unit){
    Column(modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
        Text("Tu biblioteca",style=MaterialTheme.typography.headlineMedium); Text("Lectura · Narración · IA",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold); Button(onClick=onImport){Text("Importar libro")}
        if(book==null) Box(Modifier.fillMaxWidth().weight(1f),contentAlignment=Alignment.Center){Text("Importa un libro para leerlo o escucharlo con narración IA.")}
        else Card(Modifier.fillMaxWidth().clickable{onOpen(book)}){ Row(Modifier.padding(14.dp),horizontalArrangement=Arrangement.spacedBy(14.dp),verticalAlignment=Alignment.CenterVertically){
            if(book.isPdf) PdfCover(book.uriString,Modifier.width(105.dp).height(150.dp))
            Column(Modifier.weight(1f)){Text(cleanBookTitle(book.title),style=MaterialTheme.typography.titleLarge,maxLines=3,overflow=TextOverflow.Ellipsis);Spacer(Modifier.height(6.dp));Text(if(book.isPdf)"PDF · listo para leer" else if(book.textContent!=null)"Listo para narrar" else "Documento importado")}
        }}
    }
}

@Composable private fun PdfCover(uriString:String,modifier:Modifier){ val context=LocalContext.current; val bitmap=remember(uriString){renderPdfPageFromUri(context,uriString,0,0.55f)}; if(bitmap!=null) Image(bitmap.asImageBitmap(),"Portada",modifier,contentScale=ContentScale.Crop) else Card(modifier){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("PDF")}} }

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun ReaderScreen(book:BookDocument,onBack:()->Unit){
    val context=LocalContext.current; val director=remember{RuleBasedSceneDirector()};val renderer=remember(context){SupabaseVoiceRenderer(context.applicationContext)};val scope=rememberCoroutineScope();var player by remember{mutableStateOf<MediaPlayer?>(null)};var generating by remember{mutableStateOf(false)};var status by remember{mutableStateOf("Listo para leer.")}
    DisposableEffect(Unit){onDispose{runCatching{player?.release()}}}
    Scaffold(topBar={TopAppBar(title={Text(cleanBookTitle(book.title),maxLines=1,overflow=TextOverflow.Ellipsis)},navigationIcon={Button(onClick=onBack,modifier=Modifier.padding(horizontal=6.dp)){Text("Volver")}})}){padding->Column(Modifier.padding(padding).fillMaxSize()){
        if(book.textContent!=null){Card(Modifier.fillMaxWidth().padding(12.dp)){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text("Narración IA",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold);Text("BookFlow analizará automáticamente el texto del libro y dirigirá la voz.");Button(enabled=!generating,onClick={generating=true;status="Analizando y generando narración…";scope.launch{try{val plan=director.createPlan(book.textContent.take(1200));val voice=if(plan.speakerId=="narrator_female")FEMALE_VOICE_ID else MALE_VOICE_ID;val segment=renderer.render(plan,voice);runCatching{player?.release()};player=MediaPlayer().apply{setDataSource(segment.localUri);setOnPreparedListener{status="Reproduciendo · ${plan.speakerLabel} · ${plan.mood}";it.start()};setOnCompletionListener{status="Narración terminada."};prepareAsync()}}catch(e:Exception){status="No pude narrar: ${e.message}"}finally{generating=false}}}){Text(if(generating)"Generando…" else "Narrar desde aquí")};Text(status,style=MaterialTheme.typography.bodySmall)}};Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)){Text(book.textContent,style=MaterialTheme.typography.bodyLarge)}}
        else if(book.isPdf){Card(Modifier.fillMaxWidth().padding(12.dp)){Text("Narración PDF será el siguiente paso; por ahora BookFlow usa su primera página como carátula y permite lectura visual.",Modifier.padding(14.dp))};Box(Modifier.weight(1f)){PdfBookReader(book.uriString)}} else Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("Este formato todavía no puede narrarse.")}
    }}
}

@Composable private fun PdfBookReader(uriString:String){val context=LocalContext.current;var renderer by remember(uriString){mutableStateOf<PdfRenderer?>(null)};var descriptor by remember(uriString){mutableStateOf<ParcelFileDescriptor?>(null)};DisposableEffect(uriString){descriptor=runCatching{context.contentResolver.openFileDescriptor(Uri.parse(uriString),"r")}.getOrNull();renderer=descriptor?.let{runCatching{PdfRenderer(it)}.getOrNull()};onDispose{runCatching{renderer?.close()};runCatching{descriptor?.close()}}};val pdf=renderer?:return Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("Abriendo PDF…")};Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){for(i in 0 until pdf.pageCount)remember(uriString,i){renderPdfPage(pdf,i,1.5f)}?.let{Image(it.asImageBitmap(),"Página ${i+1}",Modifier.fillMaxWidth(),contentScale=ContentScale.FillWidth)}}}
private fun renderPdfPageFromUri(context:Context,uri:String,index:Int,scale:Float):Bitmap?=runCatching{context.contentResolver.openFileDescriptor(Uri.parse(uri),"r")?.use{fd->PdfRenderer(fd).use{r->renderPdfPage(r,index,scale)}}}.getOrNull()
private fun renderPdfPage(renderer:PdfRenderer,index:Int,scale:Float):Bitmap?=runCatching{renderer.openPage(index).use{p->Bitmap.createBitmap((p.width*scale).toInt(),(p.height*scale).toInt(),Bitmap.Config.ARGB_8888).also{b->b.eraseColor(android.graphics.Color.WHITE);p.render(b,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)}}}.getOrNull()
private val BookDocument.isPdf get()=mimeType.contains("pdf",true)||title.endsWith(".pdf",true)
private fun cleanBookTitle(title:String)=title.substringBeforeLast(".").replace('_',' ').replace(Regex("\\s+")," ").trim()
private fun queryDisplayName(context:Context,uri:Uri):String?{var cursor:Cursor?=null;return try{cursor=context.contentResolver.query(uri,null,null,null,null);val c=cursor?:return null;val i=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(c.moveToFirst()&&i>=0)c.getString(i)else null}finally{cursor?.close()}}
private const val MALE_VOICE_ID="pNInz6obpgDQGcFmaJgB";private const val FEMALE_VOICE_ID="EXAVITQu4vr4xnSDxMaL"
