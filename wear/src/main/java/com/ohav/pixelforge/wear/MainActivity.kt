package com.ohav.pixelforge.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { WatchHome() } }
    }
}

@Composable
private fun WatchHome() {
    val ctx = LocalContext.current
    var faces by remember { mutableStateOf<List<String>?>(null) }
    var error by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        try { faces = FacePusher.list(ctx) } catch (e: Exception) { error = "${e.javaClass.simpleName}: ${e.message}" }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Orbit Studio", style = MaterialTheme.typography.titleMedium)
        Text(
            when {
                error.isNotEmpty() -> error
                faces == null -> "טוען…"
                else -> "פני שעון מותקנים: ${faces!!.size}\n${faces!!.joinToString(" · ")}"
            },
            textAlign = TextAlign.Center,
        )
        Text("שלח פני שעון מהאפליקציה בטלפון", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
        Button(onClick = { permission.launch("com.google.wear.permission.SET_PUSHED_WATCH_FACE_AS_ACTIVE") }) {
            Text("אשר החלפת פני שעון")
        }
    }
}
