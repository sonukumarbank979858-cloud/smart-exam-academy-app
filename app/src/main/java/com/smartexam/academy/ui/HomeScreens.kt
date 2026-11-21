package com.smartexam.academy.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartexam.academy.data.Banner
import com.smartexam.academy.data.Category
import com.smartexam.academy.data.Exam
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ═════════════════════════ Home ═════════════════════════

@Composable
fun HomeScreen() {
    val repo = LocalRepo.current
    val nav = LocalNav.current
    var name by remember { mutableStateOf(repo.prefs.cachedName) }
    LaunchedEffect(Unit) { try { repo.profile()?.let { name = it.fullName } } catch (_: Exception) {} }

    val cats = rememberLoad("cats") { repo.categories() }
    val banners = rememberLoad("banners") { repo.banners() }
    val exams = rememberLoad("exams") { repo.exams() }
    var query by remember { mutableStateOf("") }
    val results = rememberLoad(query) {
        if (query.trim().length < 2) emptyList() else { delay(300); repo.search(query) }
    }
    val myExams = remember { repo.prefs.myExams() }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Column(Modifier.fillMaxWidth().background(Brand.Navy).statusBarsPadding().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Welcome back 👋", color = Color.White.copy(alpha = .75f), fontSize = 12.sp)
                        Text("नमस्ते ${name.ifBlank { "विद्यार्थी" }.substringBefore(" ")}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(Brand.Orange).clickable { nav.tab(Route.Profile) },
                        contentAlignment = Alignment.Center
                    ) { Text(name.take(1).uppercase().ifBlank { "S" }, color = Color.White, fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.height(12.dp))
                TextField(
                    value = query, onValueChange = { query = it },
                    placeholder = { Text("परीक्षा खोजें (Search exam)") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Clear, "साफ़ करें") } },
                    singleLine = true, shape = RoundedCornerShape(14.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (query.trim().length >= 2) {
            item { SectionTitle("खोज परिणाम") }
            val r = results.data
            when {
                r == null -> item { LoadingBox() }
                r.isEmpty() -> item { EmptyBox("“$query” से कोई परीक्षा नहीं मिली") }
                else -> items(r) { e -> ExamRow(e) { nav.push(Route.ExamDetail(e.id)) } }
            }
            return@LazyColumn
        }

        item { Spacer(Modifier.height(12.dp)); BannerCarousel(banners.data ?: emptyList()) }

        item {
            SectionTitle("अपनी परीक्षा चुनें", Modifier.padding(top = 8.dp), "सभी देखें") { nav.tab(Route.Exams()) }
            LoadBox(cats) { list -> CategoryCircles(list) { nav.tab(Route.Exams(it)) } }
        }

        item {
            SectionTitle("मेरी परीक्षाएँ (My exams)", Modifier.padding(top = 8.dp))
            if (myExams.isEmpty()) {
                SCard(Modifier.padding(horizontal = 16.dp), onClick = { nav.tab(Route.Exams()) }) {
                    Text("अभी कोई परीक्षा नहीं जोड़ी गई", fontWeight = FontWeight.SemiBold)
                    Text("परीक्षा खोलकर “मेरी परीक्षा में जोड़ें” दबाएँ — वह यहाँ दिखेगी।", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(myExams) { (id, title) ->
                        val e = exams.data?.firstOrNull { it.id == id }
                        SCard(Modifier.width(150.dp), onClick = { nav.push(Route.ExamDetail(id)) }) {
                            NetImage(e?.logoUrl, title, 44.dp)
                            Spacer(Modifier.height(8.dp))
                            Text(title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(e?.let { "${it.testCount} टेस्ट" } ?: "", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Text("▶ तैयारी शुरू करें", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold,
                                modifier = Modifier.clip(RoundedCornerShape(50)).background(Brand.Navy).padding(horizontal = 10.dp, vertical = 5.dp))
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(12.dp))
            SCard(Modifier.padding(horizontal = 16.dp), onClick = { nav.push(Route.History) }, color = MaterialTheme.colorScheme.secondaryContainer) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Brand.Navy), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.DateRange, null, tint = Color.White)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("मेरा प्रदर्शन (Performance)", fontWeight = FontWeight.Bold)
                        Text("स्कोर, पिछले टेस्ट और दोबारा प्रयास", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.KeyboardArrowRight, null)
                }
            }
        }

        item { SectionTitle("सभी परीक्षाएँ", Modifier.padding(top = 12.dp), "सभी देखें") { nav.tab(Route.Exams()) } }
        val ex = exams.data
        when {
            ex == null && exams.loading -> item { LoadingBox() }
            ex == null -> item { ErrorBox(exams.error ?: "", retry = exams.reload) }
            ex.isEmpty() -> item { EmptyBox("अभी कोई परीक्षा उपलब्ध नहीं है") }
            else -> items(ex.take(8)) { e -> ExamRow(e) { nav.push(Route.ExamDetail(e.id)) } }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BannerCarousel(banners: List<Banner>) {
    val ctx = LocalContext.current
    val count = banners.size.coerceAtLeast(1)
    val pager = rememberPagerState { count }
    LaunchedEffect(count) {
        while (count > 1) { delay(4000); pager.animateScrollToPage((pager.currentPage + 1) % count) }
    }
    Column {
        HorizontalPager(pager, contentPadding = PaddingValues(horizontal = 16.dp), pageSpacing = 10.dp) { i ->
            val shape = RoundedCornerShape(18.dp)
            if (banners.isEmpty()) {
                Box(
                    Modifier.fillMaxWidth().height(150.dp).clip(shape)
                        .background(Brush.linearGradient(listOf(Brand.Navy, Brand.NavyLight, Brand.Orange))).padding(18.dp)
                ) {
                    Column(Modifier.align(Alignment.CenterStart)) {
                        Tag("NEW", Brand.Orange, Color.White)
                        Spacer(Modifier.height(8.dp))
                        Text("Smart Exam Academy", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
                        Text("फ्री मॉक टेस्ट • हिंदी + English • पूरा विश्लेषण", color = Color.White.copy(alpha = .9f), fontSize = 13.sp)
                    }
                }
            } else {
                val b = banners[i]
                BannerImage(
                    b.imageUrl,
                    Modifier.fillMaxWidth().height(150.dp).clip(shape).clickable(enabled = !b.link.isNullOrBlank()) {
                        try { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(b.link))) } catch (_: Exception) {}
                    }
                )
            }
        }
        if (count > 1) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center) {
                repeat(count) { i ->
                    Box(
                        Modifier.padding(3.dp).height(6.dp).width(if (i == pager.currentPage) 18.dp else 6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (i == pager.currentPage) Brand.Orange else MaterialTheme.colorScheme.outline)
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryCircles(list: List<Category>, onPick: (String?) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column(Modifier.width(72.dp).clickable { onPick(null) }, horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(60.dp).clip(CircleShape).background(Brand.Navy), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Menu, null, tint = Color.White)
                }
                Text("सभी", fontSize = 12.sp, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
            }
        }
        items(list) { c ->
            Column(Modifier.width(72.dp).clickable { onPick(c.id) }, horizontalAlignment = Alignment.CenterHorizontally) {
                NetImage(c.iconUrl, c.name, 60.dp, modifier = Modifier.border(2.dp, Brand.Orange.copy(alpha = .4f), CircleShape))
                Text(c.nameHi ?: c.name, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp), lineHeight = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

@Composable
fun ExamRow(e: Exam, onClick: () -> Unit) {
    SCard(Modifier.padding(horizontal = 16.dp, vertical = 5.dp), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NetImage(e.logoUrl, e.name, 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(e.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(listOfNotNull(e.nameHi, e.description).joinToString(" • "), fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (e.testCount > 0 || e.packCount > 0)
                    Text("${e.packCount} पैक • ${e.testCount} टेस्ट", fontSize = 12.sp, color = Brand.Orange, fontWeight = FontWeight.SemiBold)
            }
            Icon(Icons.Default.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ═════════════════════════ Exams (by category) ═════════════════════════

@Composable
fun ExamsScreen(categoryId: String?) {
    val repo = LocalRepo.current
    val nav = LocalNav.current
    val cats = rememberLoad("cats") { repo.categories() }
    var selected by remember(categoryId) { mutableStateOf(categoryId) }
    val exams = rememberLoad(selected) { repo.exams(selected) }

    Column(Modifier.fillMaxSize()) {
        PlainHeader("परीक्षाएँ", "अपनी परीक्षा चुनें और तैयारी शुरू करें")
        val list = cats.data ?: emptyList()
        val labels = listOf("सभी") + list.map { it.nameHi ?: it.name }
        val idx = if (selected == null) 0 else list.indexOfFirst { it.id == selected } + 1
        ChipRow(labels, idx.coerceAtLeast(0)) { i -> selected = if (i == 0) null else list[i - 1].id }
        Spacer(Modifier.height(8.dp))
        LoadBox(exams, Modifier.weight(1f)) { ex ->
            if (ex.isEmpty()) EmptyBox("इस श्रेणी में अभी कोई परीक्षा नहीं है")
            else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
                items(ex) { e -> ExamRow(e) { nav.push(Route.ExamDetail(e.id)) } }
            }
        }
    }
}

// ═════════════════════════ Exam detail (packs) ═════════════════════════

@Composable
fun ExamDetailScreen(examId: String) {
    val repo = LocalRepo.current
    val nav = LocalNav.current
    val exam = rememberLoad(examId) { repo.exam(examId) ?: throw Exception("परीक्षा नहीं मिली") }
    val packs = rememberLoad(examId, "packs") { repo.packs(examId) }
    var mine by remember { mutableStateOf(repo.prefs.isMyExam(examId)) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                    .background(Brush.verticalGradient(listOf(Brand.Navy, Brand.NavyLight)))
                    .statusBarsPadding().padding(bottom = 18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { nav.pop() }) { Icon(Icons.Default.ArrowBack, "पीछे", tint = Color.White) }
                    Spacer(Modifier.weight(1f))
                    exam.data?.let { e ->
                        TextButton(onClick = { repo.prefs.toggleMyExam(e.id, e.name); mine = !mine }) {
                            Icon(if (mine) Icons.Default.Check else Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (mine) "मेरी परीक्षा में है" else "मेरी परीक्षा में जोड़ें", color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
                val e = exam.data
                if (e == null) {
                    if (exam.loading) LoadingBox() else ErrorBox(exam.error ?: "", retry = exam.reload)
                } else {
                    Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        NetImage(e.logoUrl, e.name, 64.dp, RoundedCornerShape(16.dp), bg = Color.White)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(e.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp)
                            Text(listOfNotNull(e.nameHi, e.description).joinToString(" • "), color = Color.White.copy(alpha = .8f), fontSize = 13.sp)
                            Text("एक टेस्ट पैक चुनें", color = Color.White.copy(alpha = .6f), fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HeaderPill("${e.packCount} पैक", Modifier.weight(1f))
                        HeaderPill("${e.testCount} टेस्ट", Modifier.weight(1f))
                        HeaderPill("हिंदी + English", Modifier.weight(1f))
                    }
                }
            }
        }
        item { SectionTitle("टेस्ट पैक (Test packs)", Modifier.padding(top = 10.dp)) }
        val p = packs.data
        when {
            p == null && packs.loading -> item { LoadingBox() }
            p == null -> item { ErrorBox(packs.error ?: "", retry = packs.reload) }
            p.isEmpty() -> item { EmptyBox("इस परीक्षा के टेस्ट पैक जल्द आ रहे हैं") }
            else -> items(p) { pk ->
                SCard(Modifier.padding(horizontal = 16.dp, vertical = 5.dp), onClick = { nav.push(Route.PackTests(pk.id, pk.title, pk.isFree)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(46.dp).clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF7C4DFF), Color(0xFF536DFE)))),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.List, null, tint = Color.White) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(pk.title, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            pk.titleHi?.let { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            Text("${pk.testCount} टेस्ट" + if (!pk.isFree && pk.price > 0) " • ₹${pk.price}" else "",
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        FreeTag(pk.isFree)
                        Spacer(Modifier.width(6.dp))
                        Box(Modifier.size(30.dp).clip(CircleShape).background(Brand.Navy), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.KeyboardArrowRight, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderPill(text: String, modifier: Modifier) {
    Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = modifier.clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = .14f)).padding(vertical = 8.dp))
}

// ═════════════════════════ Pack → tests ═════════════════════════

@Composable
fun PackTestsScreen(packId: String, title: String, packFree: Boolean) {
    val repo = LocalRepo.current
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    val tests = rememberLoad(packId) { repo.tests(packId) }
    var locked by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf<String?>(null) }
    var offlineTick by remember { mutableIntStateOf(0) }
    var toast by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        NavyHeader(title, if (packFree) "फ्री पैक" else "पेड पैक", onBack = { nav.pop() })
        LoadBox(tests, Modifier.weight(1f)) { list ->
            if (list.isEmpty()) EmptyBox("इस पैक में अभी कोई टेस्ट प्रकाशित नहीं हुआ है")
            else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 10.dp)) {
                item {
                    Text("उपलब्ध टेस्ट • ${list.size}", fontWeight = FontWeight.Bold, fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                }
                items(list, key = { it.id }) { t ->
                    val free = t.isFree || packFree
                    val saved = remember(offlineTick) { repo.local.isOffline(t.id) }
                    SCard(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), border = Brand.Orange.copy(alpha = .35f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Tag("MOCK", Brand.Navy, Color.White); Spacer(Modifier.width(6.dp)); FreeTag(free)
                            Spacer(Modifier.weight(1f))
                            if (free) {
                                if (downloading == t.id) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                else IconButton(onClick = {
                                    if (saved) { repo.local.deleteOffline(t.id); offlineTick++; toast = "ऑफ़लाइन कॉपी हटाई गई" }
                                    else {
                                        downloading = t.id
                                        scope.launchCatching({ toast = it }) {
                                            repo.downloadForOffline(t.id); offlineTick++; toast = "ऑफ़लाइन के लिए सेव हो गया"
                                        }.invokeOnCompletion { downloading = null }
                                    }
                                }) {
                                    Icon(if (saved) Icons.Default.CheckCircle else Icons.Default.KeyboardArrowDown,
                                        if (saved) "ऑफ़लाइन सेव है" else "ऑफ़लाइन के लिए सेव करें",
                                        tint = if (saved) Brand.Green else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(t.title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("⏱ ${t.durationMin} मिनट   •   ${t.questionCount} प्रश्न   •   ${fmtNum(t.totalMarks.takeIf { it > 0 } ?: (t.questionCount * t.marksCorrect))} अंक",
                            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("सेक्शन: ${t.sectionCount.coerceAtLeast(1)} विषय", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                            Button(
                                onClick = { if (free) nav.push(Route.Instructions(t.id)) else locked = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = if (free) Brand.Teal else Brand.Grey)
                            ) { Text(if (free) "▶ टेस्ट शुरू करें" else "🔒 अनलॉक करें", fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
    }
    if (locked) {
        AlertDialog(
            onDismissRequest = { locked = false },
            title = { Text("पेड टेस्ट") },
            text = { Text("यह टेस्ट पेड पैक का हिस्सा है। पेड पैक जल्द ही ऐप में खरीदे जा सकेंगे। तब तक फ्री टेस्ट से तैयारी जारी रखें।") },
            confirmButton = { TextButton(onClick = { locked = false }) { Text("ठीक है") } }
        )
    }
    toast?.let { msg ->
        LaunchedEffect(msg) { delay(2200); toast = null }
        Box(Modifier.fillMaxSize().padding(bottom = 30.dp), contentAlignment = Alignment.BottomCenter) {
            Text(msg, color = Color.White, modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0xE6222222)).padding(horizontal = 16.dp, vertical = 10.dp))
        }
    }
}

/** launch that reports errors through [onError] instead of crashing. */
fun CoroutineScope.launchCatching(onError: (String) -> Unit, block: suspend () -> Unit): Job =
    launch {
        try { block() } catch (e: CancellationException) { throw e } catch (e: Exception) { onError(friendly(e)) }
    }
