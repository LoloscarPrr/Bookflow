package com.bookflow.reader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.crashlytics.FirebaseCrashlytics

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseCrashlytics.getInstance().apply {
            setCustomKey("bookflow_architecture", "fresh-rebuild")
            log("BookFlow app started")
        }

        setContent {
            MaterialTheme {
                BookFlowBootstrapScreen()
            }
        }
    }
}

@Composable
private fun BookFlowBootstrapScreen() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "BookFlow",
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text = "Reading · Narration · AI Comprehension",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
