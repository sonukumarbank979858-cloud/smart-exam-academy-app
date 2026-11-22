package com.smartexam.academy.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartexam.academy.BuildConfig
import kotlinx.coroutines.launch

const val SUPPORT_EMAIL = "classessonu65@gmail.com"
const val APP_LINK = "https://github.com/sonukumarbank979858-cloud/smart-exam-academy-app/releases/latest"

// ═════════════════════════ Profile ═════════════════════════

@Composable
fun ProfileScreen(themeMode: Int, onTheme: (Int) -> Unit, onLogout: () -> Unit) {
    val repo = LocalRepo.current
    val nav = LocalNav.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val profile = rememberLoad("profile") { repo.profile() }
    val attempts = rememberLoad("attempts") { try { repo.attempts() } catch (_: Exception) { repo.local.history() } }
    var dialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    var confirmLogout by remember { mutableStateOf(false) }

    val p = profile.data
    val name = p?.fullName ?: repo.prefs.cachedName

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PlainHeader("मेरी प्रोफ़ाइल", "आपका खाता और प्रगति") {
            OutlinedButton(onClick = { nav.push(Route.EditProfile) }, shape = RoundedCornerShape(50)) {
                Icon(Icons.Default.Edit, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("प्रोफ़ाइल बदलें")
            }
            Spacer(Modifier.width(8.dp))
        }
        Column(Modifier.padding(horizontal = 16.dp)) {
            SCard(onClick = { nav.push(Route.EditProfile) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(56.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Brand.Navy, Brand.NavyLight))), contentAlignment = Alignment.Center) {
                        Text(name.take(1).uppercase().ifBlank { "S" }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(name.ifBlank { "विद्यार्थी" }, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(repo.supa.session.email ?: "", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (p != null && (p.state.isNotBlank() || p.district.isNotBlank()))
                            Text(listOf(p.state, p.district).filter { it.isNotBlank() }.joinToString(" · "), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.KeyboardArrowRight, null)
                }
            }

            Spacer(Modifier.height(14.dp))
            Text("आपके आँकड़े", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            val list = attempts.data ?: emptyList()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("${list.size}", "टेस्ट दिए", Modifier.weight(1f), Brand.Purple)
                StatTile(if (list.isEmpty()) "—" else fmtNum(Math.round(list.map { it.score }.average() * 10) / 10.0), "औसत स्कोर", Modifier.weight(1f))
                StatTile(if (list.isEmpty()) "—" else fmtNum(list.maxOf { it.score }), "सर्वश्रेष्ठ", Modifier.weight(1f), Brand.Green)
            }
            Spacer(Modifier.height(10.dp))
            SCard {
                InfoRow(Icons.Default.DateRange, "पिछले प्रयास (Attempt history)", "अपने सभी टेस्ट प्रयास देखें") { nav.push(Route.History) }
                InfoRow(Icons.Default.Notifications, "सूचनाएँ (Notifications)", "नए टेस्ट, परिणाम और घोषणाएँ") { nav.push(Route.Notifications) }
                InfoRow(Icons.Default.Star, "AI गुरु", "अपने AI कोच से पूछें") { nav.tab(Route.AiGuru) }
            }

            Spacer(Modifier.height(14.dp))
            Text("थीम (Appearance)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            SegmentedToggle(listOf("सिस्टम", "दिन", "रात"), themeMode, Modifier.fillMaxWidth()) { onTheme(it) }

            Spacer(Modifier.height(14.dp))
            Text("व्यक्तिगत जानकारी", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            SCard {
                InfoRow(Icons.Default.Phone, "मोबाइल नंबर", p?.phone ?: "")
                InfoRow(Icons.Default.Email, "ईमेल", repo.supa.session.email ?: "")
                InfoRow(Icons.Default.Place, "राज्य", p?.state ?: "")
                InfoRow(Icons.Default.LocationOn, "ज़िला", p?.district ?: "")
                InfoRow(Icons.Default.AccountBox, "श्रेणी", p?.category ?: "")
                InfoRow(Icons.Default.Star, "परीक्षा की तैयारी", p?.examInterest ?: "")
                TextButton(onClick = { nav.push(Route.EditProfile) }) { Icon(Icons.Default.Edit, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("प्रोफ़ाइल बदलें") }
            }

            Spacer(Modifier.height(14.dp))
            Text("मदद और कानूनी", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            SCard {
                InfoRow(Icons.Default.Call, "मदद (Help)", SUPPORT_EMAIL) {
                    try {
                        ctx.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$SUPPORT_EMAIL"))
                            .putExtra(Intent.EXTRA_SUBJECT, "Smart Exam Academy — मदद"))
                    } catch (_: Exception) {}
                }
                InfoRow(Icons.Default.Share, "ऐप शेयर करें", "दोस्तों को तैयारी के लिए बुलाएँ") {
                    val text = "Smart Exam Academy पर फ्री मॉक टेस्ट दें — SSC, Railway, Banking, BPSC और भी बहुत कुछ, हिंदी + English में।\n$APP_LINK"
                    ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "शेयर करें"))
                }
                InfoRow(Icons.Default.Info, "अक्सर पूछे जाने वाले प्रश्न (FAQ)", "ऐप के बारे में सामान्य सवाल") { dialog = "FAQ" to FAQ }
                InfoRow(Icons.Default.Lock, "गोपनीयता नीति (Privacy policy)", "हम आपका डेटा कैसे रखते हैं") { dialog = "गोपनीयता नीति" to PRIVACY }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = { confirmLogout = true }, Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Brand.Red)
            ) { Icon(Icons.Default.ExitToApp, null); Spacer(Modifier.width(8.dp)); Text("लॉग आउट", fontWeight = FontWeight.Bold) }
            Text("Smart Exam Academy v${BuildConfig.VERSION_NAME}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(16.dp))
        }
    }

    dialog?.let { (title, body) ->
        AlertDialog(
            onDismissRequest = { dialog = null }, title = { Text(title) },
            text = { Text(body, Modifier.verticalScroll(rememberScrollState()), fontSize = 14.sp) },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text("ठीक है") } }
        )
    }
    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("लॉग आउट करें?") },
            text = { Text("ऑफ़लाइन सेव टेस्ट फ़ोन में रहेंगे। बिना सिंक हुए प्रयास हट जाएँगे।") },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    scope.launch {
                        try { repo.syncPending() } catch (_: Exception) {}
                        repo.supa.logout(); repo.local.clearAll(); repo.prefs.cachedName = ""; repo.prefs.lastNotified = ""
                        com.smartexam.academy.Notifier.cancel(ctx)
                        onLogout()
                    }
                }) { Text("लॉग आउट", color = Brand.Red) }
            },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("रद्द करें") } }
        )
    }
}

private const val FAQ = """• टेस्ट कैसे दें?
परीक्षाएँ → अपनी परीक्षा → टेस्ट पैक → "टेस्ट शुरू करें"।

• नेगेटिव मार्किंग?
हर टेस्ट के निर्देश पेज पर सही/ग़लत उत्तर के अंक लिखे होते हैं।

• ऑफ़लाइन टेस्ट?
टेस्ट सूची में ⬇ दबाकर टेस्ट सेव करें। "ऑफ़लाइन" टैब से बिना इंटरनेट टेस्ट दें; रिज़ल्ट इंटरनेट आने पर सिंक हो जाएगा।

• भाषा?
निर्देश पेज और टेस्ट के दौरान ऊपर EN/हि बटन से English या हिंदी चुनें।

• पेड टेस्ट?
पेड पैक जल्द ही ऐप में उपलब्ध होंगे।"""

private const val PRIVACY = """Smart Exam Academy आपकी ये जानकारी रखता है: ईमेल (लॉग इन के लिए), नाम, मोबाइल नंबर, राज्य, ज़िला, श्रेणी और आपके टेस्ट प्रयास।

इसका इस्तेमाल सिर्फ़ आपका खाता चलाने, रिज़ल्ट/विश्लेषण दिखाने और लीडरबोर्ड (केवल नाम और स्कोर) के लिए होता है। हम आपकी जानकारी बेचते नहीं हैं।

डेटा Supabase के सुरक्षित सर्वर पर रहता है; आपकी प्रोफ़ाइल और प्रयास सिर्फ़ आप (और ऐप एडमिन) देख सकते हैं।

खाता या डेटा हटवाने के लिए $SUPPORT_EMAIL पर ईमेल करें।"""

// ═════════════════════════ Offline ═════════════════════════

@Composable
fun OfflineScreen() {
    val repo = LocalRepo.current
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    var tick by remember { mutableIntStateOf(0) }
    val saved = remember(tick) { repo.local.offlineList() }
    val pending = remember(tick) { repo.local.pending() }
    var syncing by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        PlainHeader("ऑफ़लाइन", "बिना इंटरनेट के टेस्ट दें")
        if (pending.isNotEmpty()) {
            SCard(Modifier.padding(horizontal = 16.dp), color = Brand.OrangeSoft) {
                Text("${pending.size} प्रयास सिंक होने बाकी हैं", fontWeight = FontWeight.Bold, color = Brand.OrangeDark)
                msg?.let { Text(it, fontSize = 12.sp, color = Brand.OrangeDark) }
                TextButton(onClick = {
                    syncing = true
                    scope.launch {
                        msg = try { "${repo.syncPending()} प्रयास सिंक हुए" } catch (e: Exception) { friendly(e) }
                        syncing = false; tick++
                    }
                }, enabled = !syncing) { Text(if (syncing) "सिंक हो रहा है…" else "अभी सिंक करें") }
            }
            Spacer(Modifier.height(8.dp))
        }
        if (saved.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CheckCircle, null, tint = Brand.Grey, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(10.dp))
                Text("कोई ऑफ़लाइन टेस्ट सेव नहीं है", fontWeight = FontWeight.Bold)
                Text("किसी टेस्ट पैक में टेस्ट के सामने ⬇ दबाएँ — वह यहाँ सेव हो जाएगा।", textAlign = TextAlign.Center,
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(14.dp))
                OutlinedButton(onClick = { nav.tab(Route.Exams()) }) { Text("परीक्षाएँ देखें") }
            }
        } else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(saved, key = { it.test.id }) { b ->
                SCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            if (b.packTitle.isNotBlank()) Text(b.packTitle, fontSize = 11.sp, color = Brand.OrangeDark, fontWeight = FontWeight.SemiBold)
                            Text(b.test.title, fontWeight = FontWeight.Bold)
                            Text("⏱ ${b.test.durationMin} मिनट • ${b.questions.size} प्रश्न", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { repo.local.deleteOffline(b.test.id); tick++ }) { Icon(Icons.Default.Delete, "हटाएँ", tint = Brand.Grey) }
                    }
                    Spacer(Modifier.height(6.dp))
                    Button(onClick = { nav.push(Route.Instructions(b.test.id)) }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Brand.Teal)) { Text("▶ टेस्ट शुरू करें", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
