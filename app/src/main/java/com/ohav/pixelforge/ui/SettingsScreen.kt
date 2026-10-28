package com.ohav.pixelforge.ui

import android.content.Context
import androidx.compose.animation.animateContentSize
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
        IconButton(onClick = onBack, modifier = Modifier.enter(0)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "חזרה") }
        Text("הגדרות", Modifier.enter(1), fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))

        Section("עדכונים", 2) { UpdateRow() }
        Spacer(Modifier.height(14.dp))
        Section("מפתח Gemini", 3) {
            var key by remember { mutableStateOf(prefs.getString("ai_key", "") ?: "") }
            Text("נשמר רק במכשיר. בלי מפתח, תמונות AI נוצרות דרך Pollinations החינמי.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                key, { key = it.trim(); prefs.edit().putString("ai_key", key).apply() },
                Modifier.fillMaxWidth(), singleLine = true, label = { Text("API key") },
                visualTransformation = PasswordVisualTransformation(),
            )
        }
        Spacer(Modifier.height(14.dp))
        Section("יצירת תמונות AI", 4) {
            var provider by remember { mutableStateOf(prefs.getString("ai_provider", "auto") ?: "auto") }
            var cap by remember { mutableIntStateOf(prefs.getInt("ai_cap", 30)) }
            Text("Pollinations חינמי ובלי מפתח. Gemini באיכות גבוהה יותר, דורש מפתח עם חיוב (~4 סנט לתמונה).", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("auto" to "אוטומטי", "pollinations" to "Pollinations", "gemini" to "Gemini").forEach { (k, t) ->
                    FilterChip(provider == k, { provider = k; prefs.edit().putString("ai_provider", k).apply() }, { Text(t) })
                }
            }
            Text("תקרה יומית: $cap תמונות · נוצלו היום ${com.ohav.pixelforge.forge.AiImages.usedToday(ctx)}")
            Slider(cap.toFloat(), { cap = it.toInt(); prefs.edit().putInt("ai_cap", cap).apply() }, valueRange = 0f..200f)
        }
        Spacer(Modifier.height(14.dp))
        Text("גרסה ${BuildConfig.VERSION_NAME}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
    }
}

@Composable
private fun Section(title: String, index: Int, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.enter(index).animateContentSize().fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp)).padding(18.dp)) {
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
