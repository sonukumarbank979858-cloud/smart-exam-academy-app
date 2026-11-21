package com.smartexam.academy.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.smartexam.academy.data.Question
import com.smartexam.academy.data.Scorer
import com.smartexam.academy.data.TestBundle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Questions grouped by section order, then by their own order. */
fun TestBundle.ordered(): List<Question> {
    val idx = sections.sortedBy { it.sort }.mapIndexed { i, s -> s.id to i }.toMap()
    return questions.withIndex().sortedWith(compareBy({ idx[it.value.sectionId] ?: Int.MAX_VALUE }, { it.index })).map { it.value }
}

// ═════════════════════════ Instructions ═════════════════════════

@Composable
fun InstructionsScreen(testId: String) {
    val repo = LocalRepo.current
    val nav = LocalNav.current
    val bundle = rememberLoad(testId) { repo.bundle(testId) }
    val past = rememberLoad(testId, "past") { repo.attemptsForTest(testId) }
    var hindi by remember { mutableStateOf(repo.prefs.hindiDefault) }

    Column(Modifier.fillMaxSize()) {
        PlainHeader("टेस्ट निर्देश", "EXAM BRIEFING", onBack = { nav.pop() })
        LoadBox(bundle, Modifier.weight(1f)) { b ->
            val t = b.test
            val canEn = t.hasEnglish; val canHi = t.hasHindi
            val useHindi = if (!canEn) true else if (!canHi) false else hindi
            val qs = b.ordered()
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                SCard {
                    Row { Tag(b.packTitle.ifBlank { "MOCK TEST" }.uppercase(), Brand.OrangeSoft, Brand.OrangeDark) }
                    Spacer(Modifier.height(8.dp))
                    Text(t.title, fontWeight = FontWeight.Black, fontSize = 22.sp)
                    Text("टेस्ट शुरू करने से पहले पैटर्न, नेगेटिव मार्किंग और समय ध्यान से देखें।",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile("${qs.size}", "प्रश्न", Modifier.weight(1f))
                        StatTile(fmtNum(Scorer.maxMarks(b)), "कुल अंक", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile("${t.durationMin}", "मिनट", Modifier.weight(1f))
                        StatTile("${b.sections.size.coerceAtLeast(1)}", "विषय/सेक्शन", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("अंक योजना: ", fontSize = 13.sp)
                        Text("+${fmtNum(t.marksCorrect)} सही", color = Brand.Green, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("   −${fmtNum(t.marksWrong)} ग़लत", color = Brand.Red, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                SCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("प्रश्नों की भाषा", fontWeight = FontWeight.Bold)
                            Text("टेस्ट के दौरान भी बदल सकते हैं", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (canEn && canHi) SegmentedToggle(listOf("English", "हिंदी"), if (useHindi) 1 else 0) {
                            hindi = it == 1; repo.prefs.hindiDefault = hindi
                        } else Tag(if (useHindi) "हिंदी" else "English", Brand.Navy, Color.White)
                    }
                }
                Spacer(Modifier.height(10.dp))
                SCard {
                    Text("विषयवार विभाजन (Subject breakdown)", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    val groups = if (b.sections.isEmpty()) listOf("General" to qs.size)
                    else b.sections.sortedBy { it.sort }.map { s -> s.name to qs.count { it.sectionId == s.id } }
                        .let { l -> val other = qs.count { q -> b.sections.none { it.id == q.sectionId } }; if (other > 0) l + ("General" to other) else l }
                    groups.forEach { (name, n) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Brand.OrangeSoft), contentAlignment = Alignment.Center) {
                                Text(name.take(1).uppercase(), color = Brand.OrangeDark, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                            Text("$n प्रश्न • ${fmtNum(n * t.marksCorrect)} अंक", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                SCard {
                    Text("ⓘ  ज़रूरी निर्देश", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    val lines = listOfNotNull(
                        t.instructions?.takeIf { it.isNotBlank() },
                        "हर सही उत्तर पर +${fmtNum(t.marksCorrect)} अंक और हर ग़लत उत्तर पर −${fmtNum(t.marksWrong)} अंक। छोड़े गए प्रश्न पर 0।",
                        "टाइमर ${t.durationMin}:00 से शुरू होगा; 0 होते ही टेस्ट अपने-आप सबमिट हो जाएगा।",
                        "किसी प्रश्न को बाद में देखने के लिए ☆ (Mark for review) दबाएँ। प्रश्न पैलेट से किसी भी प्रश्न पर जा सकते हैं।",
                        "इलेक्ट्रॉनिक उपकरण, स्मार्ट वॉच और कैलकुलेटर का प्रयोग वर्जित है।",
                    )
                    lines.forEach { Text("•  $it", fontSize = 13.sp, modifier = Modifier.padding(vertical = 3.dp)) }
                }
                past.data?.takeIf { it.isNotEmpty() }?.let { list ->
                    Spacer(Modifier.height(10.dp))
                    SCard {
                        Text("आपके प्रयास", fontWeight = FontWeight.Bold)
                        val last = list.first()
                        val pct = if (Scorer.maxMarks(b) > 0) last.score * 100 / Scorer.maxMarks(b) else 0.0
                        Text("${fmtNum(Math.round(pct * 10) / 10.0)}%  •  ${fmtNum(last.score)}/${fmtNum(Scorer.maxMarks(b))}",
                            color = if (last.score >= 0) Brand.Green else Brand.Red, fontWeight = FontWeight.Black, fontSize = 22.sp)
                        Text("कुल प्रयास: ${list.size}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { nav.push(Route.Result(b, last)) }, Modifier.weight(1f)) { Text("रिज़ल्ट देखें") }
                            OutlinedButton(onClick = { nav.push(Route.Analysis(b, last)) }, Modifier.weight(1f)) { Text("विश्लेषण") }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            Box(Modifier.padding(16.dp)) {
                BigButton(if (qs.isEmpty()) "इस टेस्ट में अभी प्रश्न नहीं हैं" else "टेस्ट शुरू करें  →", enabled = qs.isNotEmpty(), color = Brand.Navy) {
                    nav.push(Route.TakeTest(b, useHindi))
                }
            }
        }
    }
}

// ═════════════════════════ Test (exam hall) ═════════════════════════

@Composable
fun TestScreen(bundle: TestBundle, startHindi: Boolean) {
    val repo = LocalRepo.current
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    DisposableEffect(Unit) { view.keepScreenOn = true; onDispose { view.keepScreenOn = false } }

    val t = bundle.test
    val qs = remember(bundle) { bundle.ordered() }
    val total = t.durationMin.coerceAtLeast(1) * 60
    var index by remember { mutableIntStateOf(0) }
    val answers = remember { mutableStateMapOf<String, Int>() }
    val marked = remember { mutableStateMapOf<String, Boolean>() }
    val visited = remember { mutableStateMapOf<String, Boolean>() }
    var hindi by remember { mutableStateOf(startHindi) }
    var remaining by remember { mutableIntStateOf(total) }
    var showPalette by remember { mutableStateOf(false) }
    var showSubmit by remember { mutableStateOf(false) }
    var showExit by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }

    val q = qs[index]
    LaunchedEffect(index) { visited[q.id] = true }

    fun submit() {
        if (submitting) return
        submitting = true
        scope.launch {
            val a = repo.submit(bundle, answers.toMap(), total - remaining)
            nav.replace(Route.Result(bundle, a))
        }
    }

    LaunchedEffect(Unit) {
        while (remaining > 0) { delay(1000); if (!submitting) remaining-- }
        submit()
    }

    BackHandler { if (showPalette) showPalette = false else showExit = true }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // top bar
        Row(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).statusBarsPadding().padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { showExit = true }) { Icon(Icons.Default.Close, "बाहर निकलें") }
            Text(t.title, Modifier.weight(1f), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val low = remaining <= 60
            Text("⏱ " + fmtClock(remaining), color = if (low) Color.White else Brand.Red, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(if (low) Brand.Red else Brand.RedSoft).padding(horizontal = 10.dp, vertical = 5.dp))
            Spacer(Modifier.width(6.dp))
            if (t.hasEnglish && t.hasHindi) {
                Text(if (hindi) "हि" else "EN", fontWeight = FontWeight.Bold, color = Brand.Navy,
                    modifier = Modifier.clip(CircleShape).border(1.5.dp, Brand.Navy, CircleShape).clickable { hindi = !hindi }.padding(horizontal = 9.dp, vertical = 5.dp))
            }
            IconButton(onClick = { showPalette = true }) { Icon(Icons.Default.Menu, "प्रश्न पैलेट") }
        }
        LinearProgressIndicator(
            progress = { (index + 1f) / qs.size }, modifier = Modifier.fillMaxWidth().height(3.dp),
            color = Brand.Orange, trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        // section chips
        if (bundle.sections.size > 1) {
            val secs = bundle.sections.sortedBy { it.sort }
            val cur = secs.indexOfFirst { it.id == q.sectionId }.coerceAtLeast(0)
            Spacer(Modifier.height(8.dp))
            ChipRow(secs.map { it.name }, cur) { i ->
                val first = qs.indexOfFirst { it.sectionId == secs[i].id }
                if (first >= 0) index = first
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Q ${index + 1}/${qs.size}", fontWeight = FontWeight.Black, fontSize = 16.sp)
            Spacer(Modifier.width(10.dp))
            Tag("+${fmtNum(t.marksCorrect)}", Brand.GreenSoft, Brand.Green)
            Spacer(Modifier.width(6.dp))
            Tag("−${fmtNum(t.marksWrong)}", Brand.RedSoft, Brand.Red)
            Spacer(Modifier.weight(1f))
            Text(bundle.sectionName(q.sectionId), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        // question body
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            SCard {
                Text("Q${index + 1}. " + q.text(hindi), fontSize = 17.sp, lineHeight = 25.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(12.dp))
            q.options(hindi).forEachIndexed { i, opt ->
                val sel = answers[q.id] == i
                OptionRow(i, opt, if (sel) OptState.SELECTED else OptState.NONE) {
                    if (sel) answers.remove(q.id) else answers[q.id] = i
                }
                Spacer(Modifier.height(10.dp))
            }
            if (answers.containsKey(q.id)) {
                TextButton(onClick = { answers.remove(q.id) }) { Text("उत्तर हटाएँ (Clear response)") }
            }
            Spacer(Modifier.height(16.dp))
        }
        // bottom bar
        Row(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = { if (index > 0) index-- }, enabled = index > 0, shape = RoundedCornerShape(12.dp)) { Text("← पिछला") }
            val isMarked = marked[q.id] == true
            OutlinedButton(
                onClick = { if (isMarked) marked.remove(q.id) else marked[q.id] = true },
                shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 12.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = if (isMarked) Brand.PurpleSoft else Color.Transparent)
            ) { Icon(Icons.Default.Star, "रिव्यू के लिए मार्क करें", tint = if (isMarked) Brand.Purple else MaterialTheme.colorScheme.onSurfaceVariant) }
            Button(
                onClick = { if (index < qs.size - 1) index++ else showSubmit = true },
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Brand.Teal)
            ) { Text(if (index < qs.size - 1) "सेव करें और आगे →" else "सेव करें और सबमिट", fontWeight = FontWeight.Bold, maxLines = 1) }
        }
        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(start = 12.dp, end = 12.dp, bottom = 10.dp)) {
            Button(onClick = { showSubmit = true }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Brand.OrangeDark)) { Text("टेस्ट सबमिट करें", fontWeight = FontWeight.Bold) }
        }
    }

    if (showPalette) {
        Dialog(onDismissRequest = { showPalette = false }) {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.padding(16.dp)) {
                    Text("प्रश्न पैलेट", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    PaletteLegend()
                    Spacer(Modifier.height(10.dp))
                    LazyVerticalGrid(GridCells.Fixed(5), Modifier.heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(qs.indices.toList()) { i ->
                            val id = qs[i].id
                            val (bg, fg) = paletteColor(answers.containsKey(id), marked[id] == true, visited[id] == true)
                            Box(
                                Modifier.size(44.dp).clip(CircleShape).background(bg)
                                    .border(if (i == index) 2.dp else 0.dp, if (i == index) Brand.Orange else Color.Transparent, CircleShape)
                                    .clickable { index = i; showPalette = false },
                                contentAlignment = Alignment.Center
                            ) { Text("${i + 1}", color = fg, fontWeight = FontWeight.Bold) }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    BigButton("टेस्ट सबमिट करें", color = Brand.OrangeDark) { showPalette = false; showSubmit = true }
                }
            }
        }
    }

    if (showSubmit) {
        val answered = answers.size
        val review = marked.keys.count { it in answers.keys }
        val reviewOnly = marked.keys.count { it !in answers.keys }
        AlertDialog(
            onDismissRequest = { if (!submitting) showSubmit = false },
            title = { Text("टेस्ट सबमिट करें?") },
            text = {
                Column {
                    Text("सबमिट करने से पहले अपना प्रयास देख लें। जिन प्रश्नों का उत्तर नहीं दिया, वे छोड़े गए (skipped) माने जाएँगे।", fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    SummaryLine("उत्तर दिए", "$answered", Brand.Green)
                    SummaryLine("उत्तर + रिव्यू", "$review", Brand.Purple)
                    SummaryLine("सिर्फ़ रिव्यू (बिना उत्तर)", "$reviewOnly", Brand.Purple)
                    SummaryLine("छोड़े गए", "${qs.size - answered}", Brand.Red)
                    SummaryLine("बचा समय", fmtClock(remaining), MaterialTheme.colorScheme.onSurface)
                }
            },
            confirmButton = {
                Button(onClick = { submit() }, enabled = !submitting, colors = ButtonDefaults.buttonColors(containerColor = Brand.OrangeDark)) {
                    Text(if (submitting) "सबमिट हो रहा है…" else "सबमिट करें")
                }
            },
            dismissButton = { OutlinedButton(onClick = { showSubmit = false }, enabled = !submitting) { Text("रद्द करें") } }
        )
    }

    if (showExit) {
        AlertDialog(
            onDismissRequest = { showExit = false },
            title = { Text("टेस्ट छोड़ें?") },
            text = { Text("अभी बाहर निकलने पर यह प्रयास सेव नहीं होगा। क्या आप सच में बाहर निकलना चाहते हैं?") },
            confirmButton = { TextButton(onClick = { showExit = false; nav.pop() }) { Text("बाहर निकलें", color = Brand.Red) } },
            dismissButton = { Button(onClick = { showExit = false }) { Text("टेस्ट जारी रखें") } }
        )
    }

    if (submitting && !showSubmit) {
        Box(Modifier.fillMaxSize().background(Color(0x88000000)), contentAlignment = Alignment.Center) {
            Surface(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Brand.Orange)
                    Spacer(Modifier.height(10.dp))
                    Text(if (remaining <= 0) "समय समाप्त — टेस्ट सबमिट हो रहा है…" else "टेस्ट सबमिट हो रहा है…")
                }
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String, color: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, Modifier.weight(1f), fontSize = 14.sp)
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

fun paletteColor(answered: Boolean, review: Boolean, visited: Boolean): Pair<Color, Color> = when {
    review && answered -> Brand.Purple to Color.White
    review -> Brand.PurpleSoft to Brand.Purple
    answered -> Brand.Green to Color.White
    visited -> Brand.RedSoft to Brand.Red
    else -> Color(0xFFE6E9EF) to Color(0xFF555F70)
}

@Composable
private fun PaletteLegend() {
    val items = listOf(
        "उत्तर दिया" to paletteColor(true, false, true),
        "नहीं दिया" to paletteColor(false, false, true),
        "रिव्यू" to paletteColor(false, true, true),
        "उत्तर+रिव्यू" to paletteColor(true, true, true),
        "नहीं देखा" to paletteColor(false, false, false),
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (label, c) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(12.dp).clip(CircleShape).background(c.first).border(1.dp, c.second.copy(alpha = .4f), CircleShape))
                        Spacer(Modifier.width(4.dp))
                        Text(label, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

enum class OptState { NONE, SELECTED, CORRECT, WRONG_PICK }

@Composable
fun OptionRow(i: Int, text: String, state: OptState, note: String? = null, onClick: (() -> Unit)? = null) {
    val letter = ('A' + i).toString()
    val (bg, border, badgeBg) = when (state) {
        OptState.SELECTED -> Triple(MaterialTheme.colorScheme.secondaryContainer, Brand.Navy, Brand.Navy)
        OptState.CORRECT -> Triple(Brand.GreenSoft, Brand.Green, Brand.Green)
        OptState.WRONG_PICK -> Triple(Brand.RedSoft, Brand.Red, Brand.Red)
        OptState.NONE -> Triple(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.surfaceVariant)
    }
    val onLight = state == OptState.CORRECT || state == OptState.WRONG_PICK
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(bg)
            .border(1.5.dp, border, RoundedCornerShape(14.dp))
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(30.dp).clip(CircleShape).background(badgeBg), contentAlignment = Alignment.Center) {
            Text(letter, fontWeight = FontWeight.Bold,
                color = if (state == OptState.NONE) MaterialTheme.colorScheme.onSurfaceVariant else Color.White)
        }
        Spacer(Modifier.width(12.dp))
        Text(text, Modifier.weight(1f), fontSize = 15.sp, color = if (onLight) Color(0xFF14181F) else MaterialTheme.colorScheme.onSurface)
        if (note != null) Text(note, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = border, textAlign = TextAlign.End)
    }
}
