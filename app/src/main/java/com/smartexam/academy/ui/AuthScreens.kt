package com.smartexam.academy.ui

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartexam.academy.data.Profile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun NotConfiguredScreen() {
    Column(
        Modifier.fillMaxSize().background(Brand.Navy).statusBarsPadding().padding(28.dp),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Warning, null, tint = Brand.Orange, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text("ऐप अभी कॉन्फ़िगर नहीं है", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            "इस APK में सर्वर (Supabase) का पता नहीं डाला गया है।\n\n" +
                "This build has no SUPABASE_URL / SUPABASE_ANON_KEY. Add them as GitHub Actions secrets " +
                "(or gradle -PSUPABASE_URL=… -PSUPABASE_ANON_KEY=…) and build again.",
            color = Color.White.copy(alpha = .85f), textAlign = TextAlign.Center, fontSize = 14.sp
        )
    }
}

@Composable
fun BrandMark(modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(84.dp).clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(Brand.Orange, Brand.OrangeDark))),
            contentAlignment = Alignment.Center
        ) { Text("SEA", color = Color.White, fontWeight = FontWeight.Black, fontSize = 28.sp) }
        Spacer(Modifier.height(12.dp))
        Text("Smart Exam Academy", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Text("स्मार्ट तैयारी, पक्की सफलता", color = Color.White.copy(alpha = .8f), fontSize = 14.sp)
    }
}

@Composable
fun LoginScreen(linkError: String? = null, onDone: () -> Unit) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf(repo.supa.session.email ?: "") }
    var code by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember(linkError) { mutableStateOf(linkError) }
    var info by remember { mutableStateOf<String?>(null) }
    var cooldown by remember { mutableIntStateOf(0) }

    LaunchedEffect(cooldown) { if (cooldown > 0) { delay(1000); cooldown-- } }

    fun send() {
        val e = email.trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(e).matches()) { error = "सही ईमेल पता लिखें"; return }
        busy = true; error = null
        scope.launch {
            try {
                repo.supa.sendOtp(e)
                sent = true; cooldown = 60
                info = "$e पर ईमेल भेजा गया है (स्पैम फ़ोल्डर भी देखें)। ईमेल में “Sign in” लिंक को इसी फ़ोन पर दबाएँ — ऐप अपने-आप खुलकर लॉग इन हो जाएगा। ईमेल में 6 अंकों का कोड हो तो नीचे लिखें।"
            } catch (ex: Exception) { error = friendly(ex) }
            busy = false
        }
    }

    fun verify() {
        if (code.trim().length != 6) { error = "ईमेल में आया 6 अंकों का कोड लिखें"; return }
        busy = true; error = null
        scope.launch {
            try { repo.supa.verifyOtp(email, code); onDone() } catch (ex: Exception) { error = friendly(ex) }
            busy = false
        }
    }

    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Brand.Navy, Brand.NavyLight)))
            .statusBarsPadding().navigationBarsPadding().imePadding().verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))
        BrandMark()
        Spacer(Modifier.height(36.dp))
        Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text(if (!sent) "लॉग इन / साइन अप" else "ईमेल देखें", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(
                    if (!sent) "अपना ईमेल डालें, हम उस पर लॉग इन लिंक / OTP भेजेंगे।" else "ईमेल के लिंक से या कोड से लॉग इन करें।",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = email, onValueChange = { email = it; if (sent) { sent = false; code = "" } },
                    label = { Text("ईमेल (Email)") }, leadingIcon = { Icon(Icons.Default.Email, null) },
                    singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                if (sent) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = code, onValueChange = { v -> code = v.filter { it.isDigit() }.take(6) },
                        label = { Text("6 अंकों का कोड (अगर ईमेल में हो)") }, leadingIcon = { Icon(Icons.Default.Lock, null) },
                        singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                }
                info?.takeIf { sent }?.let { Spacer(Modifier.height(8.dp)); Text(it, fontSize = 12.sp, color = Brand.Green) }
                error?.let { Spacer(Modifier.height(8.dp)); Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.height(16.dp))
                if (!sent) {
                    BigButton(if (busy) "भेजा जा रहा है…" else "लॉग इन ईमेल भेजें", enabled = !busy) { send() }
                } else {
                    BigButton(if (busy) "जाँच हो रही है…" else "कोड से लॉग इन करें", enabled = !busy) { verify() }
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = { send() }, enabled = !busy && cooldown == 0, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text(if (cooldown > 0) "ईमेल दोबारा भेजें (${cooldown}s)" else "ईमेल दोबारा भेजें")
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("लॉग इन करके आप हमारी शर्तों और गोपनीयता नीति से सहमत होते हैं।", color = Color.White.copy(alpha = .7f), fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

val INDIAN_STATES = listOf(
    "Andaman and Nicobar Islands", "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar", "Chandigarh",
    "Chhattisgarh", "Dadra and Nagar Haveli and Daman and Diu", "Delhi", "Goa", "Gujarat", "Haryana",
    "Himachal Pradesh", "Jammu and Kashmir", "Jharkhand", "Karnataka", "Kerala", "Ladakh", "Lakshadweep",
    "Madhya Pradesh", "Maharashtra", "Manipur", "Meghalaya", "Mizoram", "Nagaland", "Odisha", "Puducherry",
    "Punjab", "Rajasthan", "Sikkim", "Tamil Nadu", "Telangana", "Tripura", "Uttar Pradesh", "Uttarakhand", "West Bengal"
)
val CATEGORIES = listOf("General", "OBC", "EWS", "SC (Scheduled Caste)", "ST (Scheduled Tribe)")

@Composable
fun ProfileFormScreen(first: Boolean, onDone: () -> Unit, onBack: (() -> Unit)?) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            repo.profile()?.let { p ->
                name = p.fullName; phone = p.phone; state = p.state; district = p.district; category = p.category
            }
        } catch (_: Exception) { name = repo.prefs.cachedName }
    }

    Column(Modifier.fillMaxSize()) {
        NavyHeader(
            if (first) "अपनी प्रोफ़ाइल बनाएँ" else "प्रोफ़ाइल बदलें",
            if (first) "बस एक बार — ताकि हम आपके लिए सही टेस्ट दिखा सकें" else repo.supa.session.email,
            onBack = onBack
        )
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).imePadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(name, { name = it.take(60) }, label = { Text("पूरा नाम *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                phone, { v -> phone = v.filter { it.isDigit() }.take(10) }, label = { Text("मोबाइल नंबर") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
            PickerField("राज्य (State)", state, INDIAN_STATES) { state = it }
            OutlinedTextField(district, { district = it.take(40) }, label = { Text("ज़िला (District)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            PickerField("श्रेणी (Category)", category, CATEGORIES) { category = it }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
        }
        Box(Modifier.padding(16.dp).navigationBarsPadding()) {
            BigButton(if (busy) "सेव हो रहा है…" else "सेव करें", enabled = !busy) {
                when {
                    name.trim().length < 2 -> error = "अपना नाम लिखें"
                    phone.isNotEmpty() && phone.length != 10 -> error = "मोबाइल नंबर 10 अंकों का होना चाहिए"
                    else -> {
                        busy = true; error = null
                        scope.launch {
                            try {
                                repo.saveProfile(Profile(repo.supa.session.userId ?: "", name.trim(), phone, state, district.trim(), category))
                                onDone()
                            } catch (e: Exception) { error = friendly(e) }
                            busy = false
                        }
                    }
                }
            }
        }
    }
}
