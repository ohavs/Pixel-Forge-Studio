package com.ohav.pixelforge.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ohav.pixelforge.BuildConfig
import com.ohav.pixelforge.update.Release
import com.ohav.pixelforge.update.UpdateManager
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .systemBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
    ) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "חזרה") }
        Text("הגדרות", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))

        Section("עדכונים") { UpdateRow() }
        Spacer(Modifier.height(14.dp))
        Section("מפתח AI") {
            var key by remember { mutableStateOf(prefs.getString("ai_key", "") ?: "") }
            Text("נשמר רק במכשיר. ללא מפתח, יצירה ב-AI כבויה.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                key, { key = it.trim(); prefs.edit().putString("ai_key", key).apply() },
                Modifier.fillMaxWidth(), singleLine = true, label = { Text("API key") },
                visualTransformation = PasswordVisualTransformation(),
            )
        }
        Spacer(Modifier.height(14.dp))
        Text("גרסה ${BuildConfig.VERSION_NAME}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp)).padding(18.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun UpdateRow() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("") }
    var release by remember { mutableStateOf<Release?>(null) }
    var busy by remember { mutableStateOf(false) }
    Text(status.ifEmpty { "גרסה מותקנת: ${BuildConfig.VERSION_NAME}" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(10.dp))
    Button(enabled = !busy, onClick = {
        scope.launch {
            busy = true
            try {
                val r = release
                if (r == null) {
                    val latest = UpdateManager.latest()
                    if (UpdateManager.isNewer(latest)) { release = latest; status = "גרסה חדשה: ${latest.versionName}" }
                    else status = "האפליקציה מעודכנת"
                } else if (!UpdateManager.canInstall(ctx)) {
                    status = "אשר התקנה ממקור זה ולחץ שוב"
                    UpdateManager.requestInstallPermission(ctx)
                } else {
                    val d = UpdateManager.download(ctx, r) { status = "מוריד… $it%" }
                    status = "מתקין…"
                    UpdateManager.install(ctx, d)
                }
            } catch (e: Exception) {
                status = "שגיאה: ${e.message}"
            }
            busy = false
        }
    }) { Text(if (release == null) "בדוק עדכונים" else "הורד והתקן") }
}
