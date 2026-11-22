package com.smartexam.academy.ui

import android.util.Patterns
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import com.smartexam.academy.data.Supa
import org.json.JSONObject
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
                .background(Brand.GoldGradient),
            contentAlignment = Alignment.Center
        ) { Text("SEA", color = Brand.Navy, fontWeight = FontWeight.Black, fontSize = 28.sp) }
        Spacer(Modifier.height(12.dp))
        Text("Smart Exam Academy", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Text("स्मार्ट तैयारी, पक्की सफलता", color = Color.White.copy(alpha = .8f), fontSize = 14.sp)
    }
}

private sealed class AuthStep {
    data object Login : AuthStep()
    data object Signup : AuthStep()
    data class Otp(val email: String, val signup: Boolean, val meta: SignupData?) : AuthStep()
}

data class SignupData(val name: String, val email: String, val phone: String, val state: String, val examInterest: String) {
    fun meta(): JSONObject = JSONObject().put("full_name", name).put("phone", phone).put("state", state).put("exam_interest", examInterest)
}

val EXAM_INTERESTS = listOf(
    "SSC (CGL / CHSL / MTS / GD)", "Railway (NTPC / Group D / ALP)", "Banking (IBPS / SBI)", "UPSC / IAS",
    "State PSC (BPSC / UPPSC / RPSC …)", "Police / Constable / SI", "Defence (NDA / CDS / Agniveer)",
    "Teaching (CTET / TET / KVS)", "State exams (Patwari, VDO …)", "अन्य (Other)"
)

/** Login → OTP → Home, or Sign-up → OTP → Home. Only 6-digit email codes. */
@Composable
fun AuthFlow(onDone: () -> Unit) {
    var step by remember { mutableStateOf<AuthStep>(AuthStep.Login) }
    var prefillEmail by remember { mutableStateOf("") }
    AuthBackground {
        AnimatedContent(
            targetState = step,
            transitionSpec = { (slideInHorizontally { it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 3 } + fadeOut()) },
            label = "auth"
        ) { st ->
            when (st) {
                AuthStep.Login -> LoginCard(prefillEmail,
                    onSent = { e -> prefillEmail = e; step = AuthStep.Otp(e, false, null) },
                    onSignup = { e -> prefillEmail = e; step = AuthStep.Signup })
                AuthStep.Signup -> SignupCard(prefillEmail,
                    onSent = { d -> prefillEmail = d.email; step = AuthStep.Otp(d.email, true, d) },
                    onLogin = { step = AuthStep.Login })
                is AuthStep.Otp -> OtpCard(st.email, st.signup, st.meta,
                    onChangeEmail = { step = if (st.signup) AuthStep.Signup else AuthStep.Login },
                    onDone = onDone)
            }
        }
    }
}

@Composable
private fun AuthBackground(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Brand.HeaderGradient)
            .statusBarsPadding().navigationBarsPadding().imePadding().verticalScroll(rememberScrollState())
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(36.dp))
        BrandMark()
        Spacer(Modifier.height(28.dp))
        content()
        Spacer(Modifier.height(16.dp))
        Text("आगे बढ़कर आप हमारी शर्तों और गोपनीयता नीति से सहमत होते हैं।", color = Color.White.copy(alpha = .65f), fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun AuthCard(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 12.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(22.dp)) {
            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(18.dp))
            content()
        }
    }
}

@Composable
private fun ErrorText(msg: String?) {
    AnimatedVisibility(visible = msg != null) {
        Text(msg ?: "", fontSize = 13.sp, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
    }
}

private fun validEmail(e: String) = Patterns.EMAIL_ADDRESS.matcher(e.trim()).matches()

@Composable
private fun LoginCard(prefill: String, onSent: (String) -> Unit, onSignup: (String) -> Unit) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf(prefill.ifBlank { repo.supa.session.email ?: "" }) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notFound by remember { mutableStateOf(false) }

    fun send() {
        val e = email.trim().lowercase()
        if (!validEmail(e)) { error = "सही ईमेल पता लिखें"; return }
        busy = true; error = null; notFound = false
        scope.launch {
            try { repo.supa.sendOtp(e, createUser = false); onSent(e) }
            catch (ex: Supa.UserNotFound) { notFound = true; error = "खाता नहीं मिला, साइन अप करें" }
            catch (ex: Exception) { error = friendly(ex) }
            busy = false
        }
    }

    AuthCard("लॉग इन करें", "अपना रजिस्टर्ड ईमेल डालें — हम 6 अंकों का OTP भेजेंगे।") {
        OutlinedTextField(
            value = email, onValueChange = { email = it; error = null; notFound = false },
            label = { Text("ईमेल (Email)") }, leadingIcon = { Icon(Icons.Default.Email, null) },
            singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { send() })
        )
        ErrorText(error)
        if (notFound) {
            TextButton(onClick = { onSignup(email.trim()) }) { Text("➜ इसी ईमेल से साइन अप करें", fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(16.dp))
        BigButton(if (busy) "भेजा जा रहा है…" else "OTP भेजें", enabled = !busy) { send() }
        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .5f))
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("नए हैं?", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            TextButton(onClick = { onSignup(email.trim()) }) { Text("साइन अप करें", fontWeight = FontWeight.ExtraBold, color = Brand.Orange) }
        }
    }
}

@Composable
private fun SignupCard(prefill: String, onSent: (SignupData) -> Unit, onLogin: () -> Unit) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(prefill) }
    var phone by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var interest by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun send() {
        val d = SignupData(name.trim(), email.trim().lowercase(), phone, state, interest)
        error = when {
            d.name.length < 2 -> "अपना पूरा नाम लिखें"
            !validEmail(d.email) -> "सही ईमेल पता लिखें"
            phone.isNotEmpty() && phone.length != 10 -> "मोबाइल नंबर 10 अंकों का होना चाहिए"
            state.isBlank() -> "अपना राज्य चुनें"
            interest.isBlank() -> "किस परीक्षा की तैयारी कर रहे हैं, चुनें"
            else -> null
        }
        if (error != null) return
        busy = true
        scope.launch {
            try { repo.supa.sendOtp(d.email, createUser = true, meta = d.meta()); onSent(d) }
            catch (ex: Exception) { error = friendly(ex) }
            busy = false
        }
    }

    AuthCard("नया खाता बनाएँ", "बस कुछ जानकारी — फिर ईमेल पर OTP आएगा।") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(name, { name = it.take(60); error = null }, label = { Text("पूरा नाम *") },
                leadingIcon = { Icon(Icons.Default.Person, null) }, singleLine = true, enabled = !busy,
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
            OutlinedTextField(email, { email = it; error = null }, label = { Text("ईमेल (Email) *") },
                leadingIcon = { Icon(Icons.Default.Email, null) }, singleLine = true, enabled = !busy,
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
            OutlinedTextField(phone, { v -> phone = v.filter { it.isDigit() }.take(10); error = null }, label = { Text("मोबाइल नंबर (वैकल्पिक)") },
                leadingIcon = { Icon(Icons.Default.Phone, null) }, singleLine = true, enabled = !busy,
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
            PickerField("राज्य (State) *", state, INDIAN_STATES) { state = it; error = null }
            PickerField("परीक्षा की तैयारी (Exam interest) *", interest, EXAM_INTERESTS) { interest = it; error = null }
        }
        ErrorText(error)
        Spacer(Modifier.height(16.dp))
        BigButton(if (busy) "भेजा जा रहा है…" else "OTP भेजें", enabled = !busy) { send() }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("पहले से खाता है?", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            TextButton(onClick = onLogin) { Text("लॉग इन करें", fontWeight = FontWeight.ExtraBold, color = Brand.Orange) }
        }
    }
}

@Composable
private fun OtpCard(email: String, signup: Boolean, meta: SignupData?, onChangeEmail: () -> Unit, onDone: () -> Unit) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var cooldown by remember { mutableIntStateOf(60) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(cooldown) { if (cooldown > 0) { delay(1000); cooldown-- } }
    LaunchedEffect(Unit) { try { focus.requestFocus() } catch (_: Exception) {} }

    fun verify(c: String) {
        if (c.length != 6 || busy) return
        busy = true; error = null
        scope.launch {
            try {
                repo.supa.verifyOtp(email, c)
                if (signup && meta != null) {
                    try {
                        repo.saveProfile(Profile(repo.supa.session.userId ?: "", meta.name, meta.phone, meta.state, "", "",
                            email = meta.email, examInterest = meta.examInterest))
                    } catch (_: Exception) { repo.prefs.cachedName = meta.name }
                }
                onDone()
            } catch (ex: Exception) { error = friendly(ex); code = "" }
            busy = false
        }
    }

    fun resend() {
        busy = true; error = null
        scope.launch {
            try {
                repo.supa.sendOtp(email, createUser = signup, meta = meta?.meta())
                cooldown = 60; info = "नया OTP भेज दिया गया ✓"
            } catch (ex: Exception) { error = friendly(ex) }
            busy = false
        }
    }

    AuthCard("OTP डालें", "$email पर 6 अंकों का कोड भेजा गया है। ईमेल का स्पैम फ़ोल्डर भी देखें।") {
        Box(Modifier.fillMaxWidth()) {
            // Invisible field receives typing/paste; six boxes show the digits.
            BasicTextField(
                value = code,
                onValueChange = { v -> val d = v.filter { it.isDigit() }.take(6); code = d; error = null; if (d.length == 6) verify(d) },
                enabled = !busy,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                modifier = Modifier.matchParentSize().alpha(0.01f).focusRequester(focus),
                singleLine = true
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(6) { i ->
                    val ch = code.getOrNull(i)?.toString() ?: ""
                    val active = i == code.length && !busy
                    val borderColor by animateColorAsState(
                        when { error != null -> MaterialTheme.colorScheme.error; active -> Brand.Orange; ch.isNotEmpty() -> Brand.NavyLight; else -> MaterialTheme.colorScheme.outline },
                        label = "otpBorder"
                    )
                    Box(
                        Modifier.weight(1f).aspectRatio(.85f).clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .6f))
                            .border(if (active) 2.dp else 1.5.dp, borderColor, RoundedCornerShape(14.dp))
                            .clickable { try { focus.requestFocus() } catch (_: Exception) {} },
                        contentAlignment = Alignment.Center
                    ) { Text(ch, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface) }
                }
            }
        }
        ErrorText(error)
        info?.let { Text(it, fontSize = 12.sp, color = Brand.Green, modifier = Modifier.padding(top = 8.dp)) }
        Spacer(Modifier.height(16.dp))
        BigButton(if (busy) "जाँच हो रही है…" else if (signup) "खाता बनाएँ" else "लॉग इन करें", enabled = !busy && code.length == 6) { verify(code) }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onChangeEmail, enabled = !busy) { Text("✎ ईमेल बदलें") }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { resend() }, enabled = !busy && cooldown == 0) {
                Text(if (cooldown > 0) "OTP दोबारा भेजें (${cooldown}s)" else "OTP दोबारा भेजें")
            }
        }
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
    var interest by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            repo.profile()?.let { p ->
                name = p.fullName; phone = p.phone; state = p.state; district = p.district; category = p.category; interest = p.examInterest
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
            PickerField("परीक्षा की तैयारी (Exam interest)", interest, EXAM_INTERESTS) { interest = it }
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
                                repo.saveProfile(Profile(repo.supa.session.userId ?: "", name.trim(), phone, state, district.trim(), category,
                                    email = repo.supa.session.email ?: "", examInterest = interest))
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
