package app.bookflow.reader.core.diagnostics

import android.content.Context
import android.util.Log
import app.bookflow.reader.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics

internal enum class DiagnosticArea(val key: String) {
    URI_PERMISSION("uri_permission"),
    TEXT_IMPORT("text_import"),
    DOCX_IMPORT("docx_import"),
    PDF_EXTRACTION("pdf_extraction"),
    PDF_OPEN("pdf_open"),
    PDF_RENDER("pdf_render"),
    NARRATION("narration"),
    PLAYBACK("playback"),
}

/**
 * Privacy-safe bridge to Crashlytics.
 *
 * It deliberately records only allow-listed technical areas, exception types and stack traces.
 * Original exception messages are not uploaded because they may contain a document URI or text.
 */
internal object CrashReporter {
    private const val TAG = "BookFlowCrashReporter"

    @Volatile
    private var crashlytics: FirebaseCrashlytics? = null

    fun initialize(context: Context) {
        if (!BuildConfig.FIREBASE_CONFIGURED) {
            Log.i(TAG, "Firebase configuration absent; crash reporting is inactive.")
            return
        }

        runCatching {
            if (FirebaseApp.getApps(context).isEmpty()) {
                checkNotNull(FirebaseApp.initializeApp(context)) {
                    "Firebase configuration could not be initialized"
                }
            }
            FirebaseCrashlytics.getInstance().apply {
                setCrashlyticsCollectionEnabled(true)
                setCustomKey("bookflow_version", BuildConfig.VERSION_NAME)
                setCustomKey("bookflow_build", BuildConfig.VERSION_CODE)
            }
        }.onSuccess {
            crashlytics = it
            it.log("bookflow_started")
        }.onFailure {
            Log.e(TAG, "Crash reporting initialization failed.", it)
        }
    }

    fun setLayoutClass(layoutClass: String) {
        val safeClass = layoutClass.takeIf { it in setOf("compact", "regular", "wide") } ?: "unknown"
        crashlytics?.setCustomKey("layout_class", safeClass)
    }

    fun recordNonFatal(area: DiagnosticArea, error: Throwable) {
        val reporter = crashlytics ?: return
        val safeType = error.javaClass.simpleName
            .filter { it.isLetterOrDigit() || it == '_' }
            .take(80)
            .ifBlank { "Throwable" }
        val sanitized = RuntimeException("BookFlow non-fatal: ${area.key} [$safeType]").apply {
            stackTrace = error.stackTrace
        }

        reporter.setCustomKey("last_error_area", area.key)
        reporter.setCustomKey("last_error_type", safeType)
        reporter.recordException(sanitized)
    }
}
