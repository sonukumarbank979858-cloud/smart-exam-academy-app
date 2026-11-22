package com.smartexam.academy.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartexam.academy.data.AppNotification
import com.smartexam.academy.data.ChatMsg
import com.smartexam.academy.data.StoreTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ═════════════════════════ Splash ═════════════════════════

@Composable
fun SplashScreen() {
    val t = rememberInfiniteTransition(label = "splash")
    val s by t.animateFloat(0.92f, 1.06f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "pulse")
    Box(Modifier.fillMaxSize().background(Brand.HeaderGradient), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BrandMark(Modifier.scale(s))
            Spacer(Modifier.height(28.dp))
            CircularProgressIndicator(color = Brand.Gold, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
        }
    }
}

// ═════════════════════════ Bell with unread badge ═════════════════════════

/** Unread count, refreshed every 60 s while the screen is open. */
@Composable
fun rememberUnreadCount(): State<Int> {
    val repo = LocalRepo.current
    val count = remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            try { count.intValue = repo.notifications(50).count { !it.isRead } } catch (_: Exception) {}
            delay(60_000)
        }
    }
    return count
}

@Composable
fun NotificationBell(tint: Color = Color.White) {
    val nav = LocalNav.current
    val unread by rememberUnreadCount()
    Box(Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = .14f)).clickable { nav.push(Route.Notifications) },
        contentAlignment = Alignment.Center) {
        Icon(Icons.Default.Notifications, "सूचनाएँ", tint = tint)
        if (unread > 0) {
            Text(
                if (unread > 9) "9+" else "$unread", color = Brand.Navy, fontSize = 10.sp, fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 4.dp)
                    .clip(CircleShape).background(Brand.Gold).defaultMinSize(minWidth = 17.dp).padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }
    }
}

// ═════════════════════════ Notifications ═════════════════════════

@Composable
fun NotificationsScreen() {
    val repo = LocalRepo.current
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    val list = rememberLoad("notifs") { repo.notifications(100) }
    var readAll by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        NavyHeader("सूचनाएँ", "नए टेस्ट, परिणाम और घोषणाएँ", onBack = { nav.pop() }) {
            TextButton(onClick = {
                scope.launch { try { repo.markAllRead(); readAll = true } catch (_: Exception) {} }
            }) { Text("सब पढ़ा ✓", color = Brand.Gold, fontWeight = FontWeight.Bold) }
        }
        LoadBox(list, Modifier.weight(1f)) { ns ->
            LaunchedEffect(ns) {
                // opening the screen marks everything read (after a moment, so the dots are seen)
                if (ns.any { !it.isRead }) { delay(1500); try { repo.markAllRead() } catch (_: Exception) {} }
            }
            if (ns.isEmpty()) EmptyBox("अभी कोई सूचना नहीं है।\nनया टेस्ट आने या परिणाम मिलने पर यहाँ दिखेगा।", icon = Icons.Default.Notifications)
            else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 10.dp)) {
                itemsIndexed(ns, key = { _, n -> n.id }) { i, n ->
                    var shown by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { delay(30L * i.coerceAtMost(10)); shown = true }
                    AnimatedVisibility(shown, enter = fadeIn() + slideInVertically { it / 3 }) {
                        NotificationRow(n, unread = !n.isRead && !readAll) {
                            if (n.testId != null && n.type == "new_test") nav.push(Route.Instructions(n.testId))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(n: AppNotification, unread: Boolean, onClick: () -> Unit) {
    val (icon, tint) = when (n.type) {
        "new_test" -> Icons.Default.PlayArrow to Brand.Teal
        "result" -> Icons.Default.CheckCircle to Brand.Green
        "submission" -> Icons.Default.Person to Brand.Violet
        else -> Icons.Default.Notifications to Brand.Orange
    }
    SCard(Modifier.padding(horizontal = 16.dp, vertical = 5.dp), onClick = onClick,
        color = if (unread) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .55f) else MaterialTheme.colorScheme.surface) {
        Row(verticalAlignment = Alignment.Top) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = .15f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = tint)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(n.title, fontWeight = if (unread) FontWeight.ExtraBold else FontWeight.SemiBold, fontSize = 15.sp)
                if (n.body.isNotBlank()) Text(n.body, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(fmtDate(n.createdAt), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .8f), modifier = Modifier.padding(top = 4.dp))
            }
            if (unread) Box(Modifier.padding(top = 4.dp).size(9.dp).clip(CircleShape).background(Brand.Orange))
        }
    }
}

// ═════════════════════════ AI गुरु ═════════════════════════

private val SUGGESTIONS = listOf(
    "मेरे पिछले टेस्ट का विश्लेषण करो",
    "मेरी कमज़ोरियाँ क्या हैं?",
    "SSC CGL की 30 दिन की स्टडी प्लान बनाओ",
    "Reasoning में स्पीड कैसे बढ़ाऊँ?",
    "आज का एक GK प्रश्न पूछो",
    "Maths के शॉर्टकट बताओ",
)

@Composable
fun AiGuruScreen() {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    val msgs = remember { mutableStateListOf<ChatMsg>() }
    var input by remember { mutableStateOf("") }
    var thinking by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    fun ask(text: String) {
        val q = text.trim()
        if (q.isEmpty() || thinking) return
        val history = msgs.toList()
        msgs.add(ChatMsg(true, q)); input = ""; thinking = true
        scope.launch {
            val reply = try { repo.askAi(q, history) } catch (e: Exception) { "⚠ " + friendly(e) }
            msgs.add(ChatMsg(false, reply)); thinking = false
        }
    }
    LaunchedEffect(msgs.size, thinking) {
        val total = 1 + (if (msgs.isEmpty()) 1 else 0) + msgs.size + (if (thinking) 1 else 0)
        if (msgs.isNotEmpty()) listState.animateScrollToItem(total - 1)
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp))
                .background(Brand.AiGradient).statusBarsPadding().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(46.dp).clip(CircleShape).background(Color.White.copy(alpha = .2f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Star, null, tint = Brand.Gold, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("AI गुरु", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                Text("आपका निजी परीक्षा कोच • आपके स्कोर देखकर सलाह", color = Color.White.copy(alpha = .85f), fontSize = 12.sp)
            }
            if (msgs.isNotEmpty()) IconButton(onClick = { msgs.clear() }) { Icon(Icons.Default.Refresh, "नई बातचीत", tint = Color.White) }
        }

        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState, contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Bubble(ChatMsg(false, "नमस्ते! 🙏 मैं आपका AI गुरु हूँ। अपने टेस्ट, पढ़ाई की योजना या किसी भी प्रश्न के बारे में पूछिए।"))
            }
            if (msgs.isEmpty()) {
                item {
                    Text("सुझाए गए सवाल", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SUGGESTIONS.forEach { s ->
                            Text("✨ $s", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surface).clickable { ask(s) }.padding(12.dp))
                        }
                    }
                }
            }
            items(msgs) { m -> Bubble(m) }
            if (thinking) item { TypingBubble() }
        }

        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input, onValueChange = { input = it.take(2000) },
                placeholder = { Text("AI गुरु से पूछें…") }, modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp), maxLines = 4
            )
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.size(50.dp).clip(CircleShape).background(if (input.isBlank() || thinking) Brand.Grey else Brand.Navy)
                    .clickable(enabled = input.isNotBlank() && !thinking) { ask(input) },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Send, "भेजें", tint = Color.White) }
        }
    }
}

@Composable
private fun Bubble(m: ChatMsg) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.fromUser) Arrangement.End else Arrangement.Start) {
        val shape = if (m.fromUser) RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp) else RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp)
        Text(
            m.text.replace("**", ""), fontSize = 14.sp, lineHeight = 21.sp,
            color = if (m.fromUser) Color.White else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.widthIn(max = 310.dp).clip(shape)
                .background(if (m.fromUser) Brand.Navy else MaterialTheme.colorScheme.surface)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun TypingBubble() {
    val t = rememberInfiniteTransition(label = "typing")
    Row(Modifier.clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surface).padding(horizontal = 16.dp, vertical = 14.dp)) {
        repeat(3) { i ->
            val a by t.animateFloat(0.25f, 1f, infiniteRepeatable(tween(500, delayMillis = i * 150), RepeatMode.Reverse), label = "dot$i")
            Box(Modifier.padding(horizontal = 3.dp).size(8.dp).alpha(a).clip(CircleShape).background(Brand.NavyLight))
        }
    }
}

// ═════════════════════════ Store test card (Home) ═════════════════════════

@Composable
fun StoreTestCard(t: StoreTest, modifier: Modifier = Modifier, onLocked: () -> Unit) {
    val nav = LocalNav.current
    SCard(modifier, onClick = { if (t.isFree) nav.push(Route.Instructions(t.id)) else onLocked() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NetImage(t.examLogo, t.examName, 44.dp, RoundedCornerShape(12.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(t.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${t.examName} • ${t.packTitle}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            FreeTag(t.isFree)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            MiniStat(Icons.Default.DateRange, "${t.durationMin} मिनट")
            Spacer(Modifier.width(12.dp))
            MiniStat(Icons.Default.List, "${t.questionCount} प्रश्न")
            Spacer(Modifier.width(12.dp))
            MiniStat(Icons.Default.Star, "${fmtNum(t.maxMarks)} अंक")
            Spacer(Modifier.weight(1f))
            Text(if (t.isFree) "▶ शुरू करें" else "🔒", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(if (t.isFree) Brand.Navy else Brand.Grey).padding(horizontal = 12.dp, vertical = 6.dp))
        }
    }
}

@Composable
private fun MiniStat(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Brand.Orange, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(3.dp))
        Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun PaidDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("पेड टेस्ट") },
        text = { Text("यह टेस्ट पेड पैक का हिस्सा है। पेड पैक जल्द ही ऐप में खरीदे जा सकेंगे। तब तक फ्री टेस्ट से तैयारी जारी रखें।") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("ठीक है") } }
    )
}

/** Gradient promo card for AI गुरु on Home. */
@Composable
fun AiPromoCard(modifier: Modifier = Modifier) {
    val nav = LocalNav.current
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Brand.AiGradient)
            .clickable { nav.tab(Route.AiGuru) }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = .2f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Star, null, tint = Brand.Gold, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("AI गुरु से पूछें ✨", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
            Text("कमज़ोरी, स्टडी प्लान, कठिन प्रश्न — तुरंत हिंदी में जवाब", color = Color.White.copy(alpha = .88f), fontSize = 12.sp)
        }
        Icon(Icons.Default.KeyboardArrowRight, null, tint = Color.White)
    }
}

