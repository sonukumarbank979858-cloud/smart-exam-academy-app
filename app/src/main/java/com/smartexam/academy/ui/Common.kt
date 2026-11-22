package com.smartexam.academy.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartexam.academy.data.Repo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val LocalRepo = staticCompositionLocalOf<Repo> { error("Repo not provided") }

// ───────── Async loading helper ─────────

class Loadable<T>(val data: T?, val error: String?, val loading: Boolean, val reload: () -> Unit)

@Composable
fun <T> rememberLoad(vararg keys: Any?, block: suspend () -> T): Loadable<T> {
    var data by remember(*keys) { mutableStateOf<T?>(null) }
    var error by remember(*keys) { mutableStateOf<String?>(null) }
    var loading by remember(*keys) { mutableStateOf(true) }
    var tick by remember(*keys) { mutableIntStateOf(0) }
    LaunchedEffect(*keys, tick) {
        loading = true; error = null
        try { data = block() } catch (e: Exception) { error = friendly(e) }
        loading = false
    }
    return Loadable(data, error, loading) { tick++ }
}

fun friendly(e: Throwable): String = when (e) {
    is java.net.UnknownHostException, is java.net.ConnectException, is java.net.SocketTimeoutException ->
        "इंटरनेट कनेक्शन नहीं है। कनेक्शन जाँचकर फिर कोशिश करें।"
    else -> e.message ?: "कुछ गड़बड़ हो गई"
}

@Composable
fun <T> LoadBox(l: Loadable<T>, modifier: Modifier = Modifier, content: @Composable (T) -> Unit) {
    val state = when { l.data != null -> 0; l.loading -> 1; else -> 2 }
    androidx.compose.animation.Crossfade(targetState = state, modifier = modifier, label = "load") { st ->
        when (st) {
            0 -> l.data?.let { content(it) }
            1 -> LoadingBox()
            else -> ErrorBox(l.error ?: "कुछ गड़बड़ हो गई", Modifier, l.reload)
        }
    }
}

/** Shimmering placeholder (skeleton) shown while content loads. */
@Composable
fun LoadingBox(modifier: Modifier = Modifier, rows: Int = 3) {
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(rows) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surface).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).shimmer())
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Box(Modifier.fillMaxWidth(.7f).height(14.dp).clip(RoundedCornerShape(6.dp)).shimmer())
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth(.45f).height(11.dp).clip(RoundedCornerShape(6.dp)).shimmer())
                }
            }
        }
    }
}

/** Animated shimmer background for skeletons. */
fun Modifier.shimmer(): Modifier = composed {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val hi = MaterialTheme.colorScheme.surface
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(
        initialValue = -400f, targetValue = 1200f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "shimmerX"
    )
    background(Brush.linearGradient(listOf(base, hi, base), start = Offset(x, 0f), end = Offset(x + 400f, 200f)))
}

@Composable
fun ErrorBox(msg: String, modifier: Modifier = Modifier, retry: (() -> Unit)? = null) {
    Column(modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Warning, null, tint = Brand.Orange, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(8.dp))
        Text(msg, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (retry != null) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = retry) { Text("फिर कोशिश करें") }
        }
    }
}

@Composable
fun EmptyBox(msg: String, modifier: Modifier = Modifier, icon: ImageVector = Icons.Default.Info) {
    Column(modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp)) }
        Spacer(Modifier.height(12.dp))
        Text(msg, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ───────── Headers & surfaces ─────────

@Composable
fun NavyHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp))
            .background(Brand.HeaderGradient).statusBarsPadding().padding(horizontal = 6.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "पीछे", tint = Color.White) }
        } else Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, color = Color.White.copy(alpha = .75f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        actions()
    }
}

@Composable
fun PlainHeader(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "पीछे") }
        else Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = MaterialTheme.colorScheme.onBackground)
            if (subtitle != null) Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        actions()
    }
}

@Composable
fun SCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    border: Color? = null,
    color: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    val m = modifier.fillMaxWidth().shadow(4.dp, shape, ambientColor = Brand.Navy.copy(alpha = .25f), spotColor = Brand.Navy.copy(alpha = .25f))
        .clip(shape)
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }
    Surface(
        modifier = m, shape = shape, color = color, tonalElevation = 0.dp, shadowElevation = 0.dp,
        border = BorderStroke(1.dp, border ?: MaterialTheme.colorScheme.outline.copy(alpha = .35f))
    ) { Column(Modifier.padding(14.dp), content = content) }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground)
        if (action != null && onAction != null) {
            Text(action, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                modifier = Modifier.clickable(onClick = onAction).padding(4.dp))
        }
    }
}

@Composable
fun Tag(text: String, bg: Color, fg: Color, modifier: Modifier = Modifier) {
    Text(
        text, color = fg, fontSize = 10.sp, fontWeight = FontWeight.Bold,
        modifier = modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
fun FreeTag(free: Boolean) =
    if (free) Tag("✓ FREE", Brand.Green, Color.White) else Tag("🔒 PAID", Color(0xFFFFB300), Color(0xFF3E2700))

@Composable
fun StatTile(value: String, label: String, modifier: Modifier = Modifier, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = valueColor, maxLines = 1)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
fun BigButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, color: Color = Brand.Orange, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val bg = if (!enabled) Brush.linearGradient(listOf(Brand.Grey, Brand.Grey))
        else if (color == Brand.Orange) Brand.GoldGradient else Brush.linearGradient(listOf(color, color))
    Box(
        modifier.fillMaxWidth().height(54.dp).shadow(if (enabled) 6.dp else 0.dp, shape).clip(shape).background(bg)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White) }
}

@Composable
fun SegmentedToggle(options: List<String>, selected: Int, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    Row(
        modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceVariant).padding(3.dp)
    ) {
        options.forEachIndexed { i, s ->
            val on = i == selected
            Text(
                s, fontSize = 13.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                color = if (on) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.clip(RoundedCornerShape(50))
                    .background(if (on) Brand.Navy else Color.Transparent)
                    .clickable { onSelect(i) }.padding(horizontal = 16.dp, vertical = 7.dp)
            )
        }
    }
}

@Composable
fun ChipRow(items: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    androidx.compose.foundation.lazy.LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items.size) { i ->
            val on = i == selected
            Text(
                items[i], fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                color = if (on) Color.White else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.clip(RoundedCornerShape(50))
                    .background(if (on) Brand.Navy else MaterialTheme.colorScheme.surface)
                    .border(1.dp, if (on) Brand.Navy else MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
                    .clickable { onSelect(i) }.padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}

// ───────── Images ─────────

private object ImgCache : LruCache<String, Bitmap>(12 * 1024 * 1024) {
    override fun sizeOf(key: String, value: Bitmap) = value.byteCount
}

@Composable
fun NetImage(
    url: String?, fallback: String, size: Dp, shape: Shape = CircleShape,
    modifier: Modifier = Modifier, bg: Color = Brand.OrangeSoft, fg: Color = Brand.OrangeDark
) {
    val repo = LocalRepo.current
    val bmp by produceState<Bitmap?>(initialValue = url?.let { ImgCache.get(it) }, url) {
        if (url.isNullOrBlank() || value != null) return@produceState
        value = withContext(Dispatchers.IO) {
            repo.supa.fetchBytes(url)?.let { b ->
                val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(b, 0, b.size, o)
                var s = 1; while (o.outWidth / (s * 2) >= 512) s *= 2
                BitmapFactory.decodeByteArray(b, 0, b.size, BitmapFactory.Options().apply { inSampleSize = s })
            }?.also { ImgCache.put(url, it) }
        }
    }
    Box(modifier.size(size).clip(shape).background(bg), contentAlignment = Alignment.Center) {
        val b = bmp
        if (b != null) Image(b.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Text(fallback.trim().take(1).uppercase().ifBlank { "S" }, color = fg, fontWeight = FontWeight.Bold, fontSize = (size.value * .4f).sp)
    }
}

/** Full-width banner image (no fallback letter). */
@Composable
fun BannerImage(url: String, modifier: Modifier = Modifier) {
    val repo = LocalRepo.current
    val bmp by produceState<Bitmap?>(initialValue = ImgCache.get(url), url) {
        if (value != null) return@produceState
        value = withContext(Dispatchers.IO) {
            repo.supa.fetchBytes(url)?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }?.also { ImgCache.put(url, it) }
        }
    }
    Box(modifier.background(Brush.linearGradient(listOf(Brand.Navy, Brand.NavyLight)))) {
        bmp?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
    }
}

// ───────── Form helpers ─────────

@Composable
fun PickerField(label: String, value: String, options: List<String>, modifier: Modifier = Modifier, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value, onValueChange = {}, readOnly = true, label = { Text(label) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Box(Modifier.matchParentSize().clickable { open = true })
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(label) },
            text = {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(options) { o ->
                        Text(o, Modifier.fillMaxWidth().clickable { onPick(o); open = false }.padding(vertical = 12.dp),
                            fontWeight = if (o == value) FontWeight.Bold else FontWeight.Normal,
                            color = if (o == value) Brand.Orange else MaterialTheme.colorScheme.onSurface)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { open = false }) { Text("बंद करें") } }
        )
    }
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().let { if (onClick != null) it.clickable(onClick = onClick) else it }.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value.ifBlank { "—" }, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

// ───────── Formatting ─────────

fun fmtNum(d: Double): String = if (d == Math.floor(d) && !d.isInfinite()) d.toLong().toString() else String.format(java.util.Locale.US, "%.2f", d).trimEnd('0').trimEnd('.')

fun fmtClock(sec: Int): String {
    val s = sec.coerceAtLeast(0)
    return if (s >= 3600) String.format(java.util.Locale.US, "%d:%02d:%02d", s / 3600, s / 60 % 60, s % 60)
    else String.format(java.util.Locale.US, "%02d:%02d", s / 60, s % 60)
}

fun fmtDuration(sec: Int): String = when {
    sec < 60 -> "${sec}s"
    sec < 3600 -> "${sec / 60}m ${sec % 60}s"
    else -> "${sec / 3600}h ${sec / 60 % 60}m"
}

fun fmtDate(iso: String): String = try {
    val p = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
    val d = p.parse(iso.take(19))!!
    java.text.SimpleDateFormat("d MMM yyyy, h:mm a", java.util.Locale("en", "IN")).format(d)
} catch (_: Exception) { iso.take(10) }
