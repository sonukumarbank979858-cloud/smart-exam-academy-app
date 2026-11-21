package com.smartexam.academy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartexam.academy.data.Attempt
import com.smartexam.academy.data.Question
import com.smartexam.academy.data.Scorer
import com.smartexam.academy.data.TestBundle
import kotlinx.coroutines.launch

private fun pct(a: Attempt, b: TestBundle): Double {
    val max = Scorer.maxMarks(b)
    return if (max > 0) Math.round(a.score * 1000 / max) / 10.0 else 0.0
}

private fun accuracy(correct: Int, wrong: Int): Int =
    if (correct + wrong == 0) 0 else Math.round(correct * 100f / (correct + wrong))

@Composable
private fun ScoreCard(b: TestBundle, a: Attempt) {
    val p = pct(a, b)
    val good = p >= 60
    SCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (good) "शाबाश! बढ़िया प्रदर्शन 🎉" else "अभ्यास जारी रखें 💪", color = if (good) Brand.Green else Brand.Red, fontWeight = FontWeight.SemiBold)
                Text("${fmtNum(a.score)} / ${fmtNum(Scorer.maxMarks(b))} अंक", fontWeight = FontWeight.Black, fontSize = 24.sp)
            }
            Text("${fmtNum(p)}%", fontWeight = FontWeight.Black, fontSize = 20.sp, color = if (good) Brand.Green else Brand.Red,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(if (good) Brand.GreenSoft else Brand.RedSoft).padding(horizontal = 12.dp, vertical = 8.dp))
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("${a.correct}", "सही", Modifier.weight(1f), Brand.Green)
            StatTile("${a.wrong}", "ग़लत", Modifier.weight(1f), Brand.Red)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("${a.skipped}", "छोड़े", Modifier.weight(1f))
            StatTile("${a.correct + a.wrong}/${b.questions.size}", "उत्तर दिए", Modifier.weight(1f), Brand.NavyLight)
        }
        Spacer(Modifier.height(10.dp))
        Row {
            Text("सटीकता: ${accuracy(a.correct, a.wrong)}%", Modifier.weight(1f), fontSize = 13.sp)
            Text("कुल समय: ${fmtDuration(a.timeTakenSec)}", fontSize = 13.sp)
        }
    }
}

// ═════════════════════════ Result ═════════════════════════

@Composable
fun ResultScreen(b: TestBundle, a: Attempt) {
    val nav = LocalNav.current
    val repo = LocalRepo.current
    var attempt by remember { mutableStateOf(a) }
    val scope = rememberCoroutineScope()
    var syncing by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        NavyHeader("टेस्ट रिज़ल्ट", "${b.test.title} • ${fmtDate(attempt.createdAt)}", onBack = { nav.pop() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
            ScoreCard(b, attempt)
            if (!attempt.synced) {
                Spacer(Modifier.height(10.dp))
                SCard(color = Brand.OrangeSoft) {
                    Text("यह रिज़ल्ट फ़ोन में सेव है — इंटरनेट मिलने पर सर्वर पर सिंक होगा।", color = Brand.OrangeDark, fontSize = 13.sp)
                    TextButton(onClick = {
                        syncing = true
                        scope.launch {
                            try { if (repo.syncPending() > 0) attempt = attempt.copy(synced = true) } catch (_: Exception) {}
                            syncing = false
                        }
                    }, enabled = !syncing) { Text(if (syncing) "सिंक हो रहा है…" else "अभी सिंक करें") }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("आगे क्या?", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionTile("विश्लेषण", "विषय और कठिनाई के अनुसार", Icons.Default.Info, listOf(Color(0xFF7C4DFF), Color(0xFF536DFE)), Modifier.weight(1f)) {
                    nav.push(Route.Analysis(b, attempt))
                }
                ActionTile("उत्तर देखें", "सही उत्तर और व्याख्या", Icons.Default.List, listOf(Color(0xFF1E88E5), Color(0xFF42A5F5)), Modifier.weight(1f)) {
                    nav.push(Route.Review(b, attempt))
                }
            }
            Spacer(Modifier.height(10.dp))
            ActionTile("लीडरबोर्ड", "रैंक और टॉप स्कोर देखें", Icons.Default.Star, listOf(Brand.OrangeDark, Color(0xFFFFA000)), Modifier.fillMaxWidth()) {
                nav.push(Route.Leaderboard(b.test.id, b.test.title))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionTile("दोबारा दें", "यही टेस्ट फिर से", Icons.Default.Refresh, listOf(Color(0xFFFF7043), Color(0xFFFF8A65)), Modifier.weight(1f)) {
                    nav.replace(Route.TakeTest(b, repo.prefs.hindiDefault && b.test.hasHindi || !b.test.hasEnglish))
                }
                ActionTile("हो गया", "होम पर वापस", Icons.Default.Done, listOf(Brand.Green, Color(0xFF43A047)), Modifier.weight(1f)) {
                    nav.tab(Route.Home)
                }
            }
        }
    }
}

@Composable
private fun ActionTile(title: String, sub: String, icon: ImageVector, colors: List<Color>, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(colors)).clickable(onClick = onClick).padding(14.dp)
    ) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = .2f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(title, color = Color.White, fontWeight = FontWeight.Bold)
        Text(sub, color = Color.White.copy(alpha = .85f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ═════════════════════════ Analysis ═════════════════════════

private data class Breakdown(val name: String, val total: Int, val correct: Int, val wrong: Int) {
    val skipped get() = total - correct - wrong
}

private fun breakdown(qs: List<Question>, a: Attempt, name: String): Breakdown {
    var c = 0; var w = 0
    qs.forEach { q -> a.answers[q.id]?.let { if (it == q.correct) c++ else w++ } }
    return Breakdown(name, qs.size, c, w)
}

@Composable
fun AnalysisScreen(b: TestBundle, a: Attempt) {
    val nav = LocalNav.current
    val t = b.test
    val qs = remember(b) { b.ordered() }
    val subjects = remember(b, a) {
        val secs = b.sections.sortedBy { it.sort }
        val list = secs.map { s -> breakdown(qs.filter { it.sectionId == s.id }, a, s.name) }.filter { it.total > 0 }
        val other = qs.filter { q -> secs.none { it.id == q.sectionId } }
        if (other.isNotEmpty()) list + breakdown(other, a, "General") else list
    }
    val levels = remember(b, a) {
        listOf("easy" to "आसान (Easy)", "medium" to "मध्यम (Medium)", "hard" to "कठिन (Hard)")
            .map { (k, label) -> breakdown(qs.filter { it.difficulty.equals(k, true) }, a, label) }.filter { it.total > 0 }
    }

    Column(Modifier.fillMaxSize()) {
        NavyHeader("प्रदर्शन विश्लेषण", t.title, onBack = { nav.pop() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
            ScoreCard(b, a)
            Spacer(Modifier.height(16.dp))
            Text("▍विषयवार विश्लेषण (Subject-wise)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            subjects.forEach { s ->
                val marks = s.correct * t.marksCorrect - s.wrong * t.marksWrong
                SCard(Modifier.padding(bottom = 8.dp)) {
                    Text(s.name, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${accuracy(s.correct, s.wrong)}% सटीकता", color = Brand.Teal, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Text("${fmtNum(marks)} / ${fmtNum(s.total * t.marksCorrect)} अंक", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { if (s.total == 0) 0f else s.correct.toFloat() / s.total },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                        color = Brand.Green, trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Row {
                        MiniStat("${s.correct + s.wrong}/${s.total}", "प्रयास", Modifier.weight(1f))
                        MiniStat("${s.correct}", "सही", Modifier.weight(1f), Brand.Green)
                        MiniStat("${s.wrong}", "ग़लत", Modifier.weight(1f), Brand.Red)
                        MiniStat("${s.skipped}", "छोड़े", Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("▍कठिनाई के अनुसार (Difficulty-wise)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            SCard {
                levels.forEachIndexed { i, l ->
                    if (i > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(l.name, fontWeight = FontWeight.SemiBold)
                            Text("${l.correct} सही • ${l.wrong} ग़लत • ${l.total} कुल", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${if (l.total == 0) 0 else l.correct * 100 / l.total}%", fontWeight = FontWeight.Bold, color = Brand.NavyLight)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            BigButton("उत्तर और व्याख्या देखें", color = Brand.Navy) { nav.push(Route.Review(b, a)) }
        }
    }
}

@Composable
private fun MiniStat(v: String, label: String, modifier: Modifier, color: Color = MaterialTheme.colorScheme.onSurface) {
    Column(modifier) {
        Text(v, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ═════════════════════════ Answer review ═════════════════════════

@Composable
fun ReviewScreen(b: TestBundle, a: Attempt) {
    val nav = LocalNav.current
    val repo = LocalRepo.current
    val t = b.test
    val qs = remember(b) { b.ordered() }
    var hindi by remember { mutableStateOf(if (!t.hasEnglish) true else if (!t.hasHindi) false else repo.prefs.hindiDefault) }
    var filter by remember { mutableIntStateOf(0) }
    var section by remember { mutableIntStateOf(0) }
    val secs = b.sections.sortedBy { it.sort }

    fun status(q: Question): Int = when (a.answers[q.id]) { null -> 3; q.correct -> 1; else -> 2 }
    val shown = qs.withIndex().filter { (_, q) ->
        (filter == 0 || status(q) == filter) && (section == 0 || q.sectionId == secs[section - 1].id)
    }
    val counts = (1..3).map { s -> qs.count { status(it) == s } }

    Column(Modifier.fillMaxSize()) {
        NavyHeader("उत्तर समीक्षा", "${qs.size} प्रश्न • व्याख्या देखने के लिए टैप करें", onBack = { nav.pop() })
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("भाषा", Modifier.weight(1f), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (t.hasEnglish && t.hasHindi) SegmentedToggle(listOf("English", "हिंदी"), if (hindi) 1 else 0) { hindi = it == 1 }
        }
        if (secs.size > 1) {
            ChipRow(listOf("सभी विषय") + secs.map { it.name }, section) { section = it }
            Spacer(Modifier.height(6.dp))
        }
        ChipRow(listOf("सभी ${qs.size}", "सही ${counts[0]}", "ग़लत ${counts[1]}", "छोड़े ${counts[2]}"), filter) { filter = it }
        Spacer(Modifier.height(6.dp))
        if (shown.isEmpty()) EmptyBox("इस फ़िल्टर में कोई प्रश्न नहीं")
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(shown, key = { it.value.id }) { (i, q) -> ReviewCard(i, q, a.answers[q.id], hindi, t.marksCorrect, t.marksWrong) }
        }
    }
}

@Composable
private fun ReviewCard(i: Int, q: Question, pick: Int?, hindi: Boolean, mc: Double, mw: Double) {
    var open by remember { mutableStateOf(false) }
    SCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${i + 1}", fontWeight = FontWeight.Bold, modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 9.dp, vertical = 3.dp))
            Spacer(Modifier.width(8.dp))
            when {
                pick == null -> Tag("छोड़ा", Color(0xFFE6E9EF), Color(0xFF555F70))
                pick == q.correct -> Tag("सही", Brand.GreenSoft, Brand.Green)
                else -> Tag("ग़लत", Brand.RedSoft, Brand.Red)
            }
            Spacer(Modifier.weight(1f))
            Text(
                when { pick == null -> "0 अंक"; pick == q.correct -> "+${fmtNum(mc)} अंक"; else -> "−${fmtNum(mw)} अंक" },
                fontSize = 12.sp, fontWeight = FontWeight.Bold,
                color = when { pick == null -> Brand.Grey; pick == q.correct -> Brand.Green; else -> Brand.Red }
            )
        }
        Text(q.difficulty.replaceFirstChar { it.uppercase() }, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
        Text(q.text(hindi), fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(vertical = 8.dp))
        q.options(hindi).forEachIndexed { oi, o ->
            val st = when { oi == q.correct -> OptState.CORRECT; oi == pick -> OptState.WRONG_PICK; else -> OptState.NONE }
            val note = when { oi == q.correct && oi == pick -> "सही • आपका उत्तर"; oi == q.correct -> "सही उत्तर"; oi == pick -> "आपका उत्तर"; else -> null }
            OptionRow(oi, o, st, note)
            Spacer(Modifier.height(6.dp))
        }
        val exp = q.explanation(hindi)
        if (exp != null) {
            Row(Modifier.fillMaxWidth().clickable { open = !open }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("व्याख्या देखें", color = Brand.NavyLight, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Icon(if (open) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, tint = Brand.NavyLight)
            }
            if (open) Text(exp, fontSize = 14.sp, lineHeight = 21.sp,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.secondaryContainer).padding(12.dp),
                color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

// ═════════════════════════ Leaderboard ═════════════════════════

@Composable
fun LeaderboardScreen(testId: String, title: String) {
    val nav = LocalNav.current
    val repo = LocalRepo.current
    val rows = rememberLoad(testId) { repo.leaderboard(testId) }
    Column(Modifier.fillMaxSize()) {
        NavyHeader("लीडरबोर्ड", "$title • पहला प्रयास", onBack = { nav.pop() })
        LoadBox(rows, Modifier.weight(1f)) { list ->
            if (list.isEmpty()) EmptyBox("अभी तक कोई स्कोर नहीं")
            else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(list) { r ->
                    SCard(border = if (r.isMe) Brand.Orange else null, color = if (r.isMe) Brand.OrangeSoft else MaterialTheme.colorScheme.surface) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val medal = when (r.rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> "#${r.rank}" }
                            Text(medal, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.width(48.dp),
                                color = if (r.isMe) Color(0xFF14181F) else MaterialTheme.colorScheme.onSurface)
                            Column(Modifier.weight(1f)) {
                                Text(r.name + if (r.isMe) " (आप)" else "", fontWeight = FontWeight.SemiBold,
                                    color = if (r.isMe) Color(0xFF14181F) else MaterialTheme.colorScheme.onSurface)
                                Text(fmtDuration(r.timeSec), fontSize = 12.sp, color = Brand.Grey)
                            }
                            Text(fmtNum(r.score), fontWeight = FontWeight.Black, fontSize = 18.sp, color = Brand.NavyLight)
                        }
                    }
                }
            }
        }
    }
}

// ═════════════════════════ Attempt history ═════════════════════════

@Composable
fun HistoryScreen() {
    val nav = LocalNav.current
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    val list = rememberLoad("history") {
        try { repo.syncPending() } catch (_: Exception) {}
        try { repo.attempts() } catch (e: Exception) { repo.local.history().ifEmpty { throw e } }
    }
    var opening by remember { mutableStateOf<String?>(null) }
    var err by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        NavyHeader("पिछले प्रयास", "आपके सभी टेस्ट प्रयास", onBack = { nav.pop() })
        err?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
        LoadBox(list, Modifier.weight(1f)) { l ->
            if (l.isEmpty()) EmptyBox("अभी तक कोई टेस्ट नहीं दिया। पहला टेस्ट शुरू करें!")
            else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(l) { idx, a ->
                    SCard(onClick = {
                        if (opening != null) return@SCard
                        opening = "$idx"; err = null
                        scope.launch {
                            try {
                                val b = repo.bundle(a.testId)
                                nav.push(Route.Result(b, a.copy(testTitle = b.test.title)))
                            } catch (e: Exception) { err = friendly(e) }
                            opening = null
                        }
                    }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(a.testTitle, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(fmtDate(a.createdAt), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("✔ ${a.correct}  ✘ ${a.wrong}  – ${a.skipped}  •  ${fmtDuration(a.timeTakenSec)}" + if (!a.synced) "  • सिंक बाकी" else "",
                                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (opening == "$idx") CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                            else Text(fmtNum(a.score) + if (a.totalMarks > 0) "/${fmtNum(a.totalMarks)}" else "",
                                fontWeight = FontWeight.Black, color = if (a.score >= 0) Brand.Green else Brand.Red)
                        }
                    }
                }
            }
        }
    }
}
