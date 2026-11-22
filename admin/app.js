/* Smart Exam Academy — admin panel (static, supabase-js v2 from CDN) */
(() => {
  const cfg = window.SEA_CONFIG || {};
  const $ = (s, r = document) => r.querySelector(s);
  const esc = (v) => String(v ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
  const toast = (m) => { const t = $("#toast"); t.textContent = m; t.classList.add("show"); clearTimeout(t._h); t._h = setTimeout(() => t.classList.remove("show"), 2600); };
  const show = (id) => ["login", "denied", "app"].forEach((x) => $("#" + x).classList.toggle("hidden", x !== id));
  const LETTERS = "ABCDEFGHIJ";

  if (!cfg.SUPABASE_URL || !cfg.SUPABASE_ANON_KEY) {
    document.body.insertAdjacentHTML("beforeend", '<div class="card narrow"><h2>कॉन्फ़िगर नहीं है</h2><p>admin/config.js में SUPABASE_URL और SUPABASE_ANON_KEY भरें।</p></div>');
    return;
  }
  const sb = supabase.createClient(cfg.SUPABASE_URL, cfg.SUPABASE_ANON_KEY, {
    auth: { persistSession: true, autoRefreshToken: true, detectSessionInUrl: true, flowType: "implicit" },
  });

  // ───────── Auth ─────────
  const redirect = location.origin + location.pathname;
  $("#sendOtp").onclick = async () => {
    const email = $("#email").value.trim();
    if (!email) return ($("#loginMsg").textContent = "ईमेल लिखें");
    $("#sendOtp").disabled = true;
    const { error } = await sb.auth.signInWithOtp({ email, options: { shouldCreateUser: true } });
    $("#sendOtp").disabled = false;
    if (error) return ($("#loginMsg").textContent = /signup|not allowed/i.test(error.message) ? "यह ईमेल पंजीकृत नहीं है। पहले ऐप में साइन अप करें, फिर एडमिन सूची में जुड़वाएँ।" : error.message);
    $("#otpBox").classList.remove("hidden");
    $("#loginMsg").textContent = "OTP भेजा गया। ईमेल (या स्पैम) में आया 6 अंकों का कोड नीचे डालें।";
  };
  $("#verifyOtp").onclick = async () => {
    const { error } = await sb.auth.verifyOtp({ email: $("#email").value.trim(), token: $("#otp").value.trim(), type: "email" });
    if (error) $("#loginMsg").textContent = error.message;
  };
  document.addEventListener("click", async (e) => {
    if (e.target.classList.contains("logout")) { await sb.auth.signOut(); location.hash = ""; show("login"); $("#who").innerHTML = ""; }
  });

  let booted = false;
  async function onSession(session) {
    if (!session) { show("login"); $("#who").innerHTML = ""; booted = false; return; }
    const email = session.user.email;
    $("#who").innerHTML = `<span>${esc(email)}</span><button class="small logout">लॉग आउट</button>`;
    const { data, error } = await sb.rpc("is_admin");
    if (error || data !== true) return show("denied");
    show("app");
    if (!booted) { booted = true; if (location.hash.includes("access_token")) history.replaceState(null, "", redirect); renderTabs(); openTab("dashboard"); }
  }
  sb.auth.onAuthStateChange((_ev, session) => { setTimeout(() => onSession(session), 0); });

  // ───────── Entity definitions ─────────
  const F = (k, l, t = "text", extra = {}) => ({ k, l, t, ...extra });
  const ENT = {
    categories: { title: "श्रेणियाँ", table: "categories", cols: ["sort", "name", "name_hi"],
      fields: [F("name", "नाम (English)", "text", { req: 1 }), F("name_hi", "नाम (हिंदी)"), F("icon_url", "आइकन इमेज URL"), F("sort", "क्रम (sort)", "number")] },
    exams: { title: "परीक्षाएँ", table: "exams", parent: { k: "category_id", ent: "categories", l: "श्रेणी" }, cols: ["sort", "name", "name_hi", "is_active"],
      fields: [F("name", "नाम (English)", "text", { req: 1 }), F("name_hi", "नाम (हिंदी)"), F("description", "विवरण", "textarea"), F("logo_url", "लोगो इमेज URL"), F("is_active", "सक्रिय (ऐप में दिखे)", "bool", { def: true }), F("sort", "क्रम", "number")] },
    packs: { title: "टेस्ट पैक", table: "packs", parent: { k: "exam_id", ent: "exams", l: "परीक्षा" }, cols: ["sort", "title", "title_hi", "is_free", "price"],
      fields: [F("title", "शीर्षक (English)", "text", { req: 1 }), F("title_hi", "शीर्षक (हिंदी)"), F("is_free", "फ्री पैक", "bool", { def: true }), F("price", "कीमत (₹)", "number"), F("sort", "क्रम", "number")] },
    tests: { title: "टेस्ट", table: "tests", parent: { k: "pack_id", ent: "packs", l: "पैक" }, cols: ["sort", "title", "duration_min", "total_marks", "is_free", "is_published"],
      fields: [F("title", "शीर्षक", "text", { req: 1 }), F("duration_min", "समय (मिनट)", "number", { def: 15 }), F("total_marks", "कुल अंक (0 = प्रश्न × सही-अंक)", "number", { def: 0 }),
        F("marks_correct", "सही उत्तर पर अंक", "number", { def: 1 }), F("marks_wrong", "ग़लत उत्तर पर कटौती", "number", { def: 0.25 }),
        F("language", "भाषा", "select", { opts: [["both", "हिंदी + English"], ["en", "केवल English"], ["hi", "केवल हिंदी"]], def: "both" }),
        F("is_free", "फ्री टेस्ट", "bool", { def: true }), F("is_published", "प्रकाशित (ऐप में दिखे)", "bool", { def: false }), F("sort", "क्रम", "number"), F("instructions", "अतिरिक्त निर्देश", "textarea")] },
    sections: { title: "सेक्शन", table: "sections", parent: { k: "test_id", ent: "tests", l: "टेस्ट" }, cols: ["sort", "name"],
      fields: [F("name", "सेक्शन / विषय का नाम", "text", { req: 1 }), F("sort", "क्रम", "number")] },
    questions: { title: "प्रश्न", table: "questions", parent: { k: "test_id", ent: "tests", l: "टेस्ट" }, cols: ["sort", "q", "correct_index", "difficulty", "section_id"], custom: true },
    banners: { title: "बैनर", table: "banners", cols: ["sort", "image_url", "link", "active"],
      fields: [F("image_url", "इमेज URL (लगभग 1200×500)", "text", { req: 1 }), F("link", "क्लिक पर लिंक"), F("active", "सक्रिय", "bool", { def: true }), F("sort", "क्रम", "number")] },
    admins: { title: "एडमिन", table: "admins", key: "email", cols: ["email"], noEdit: true, fields: [F("email", "एडमिन ईमेल", "text", { req: 1 })] },
  };
  const TABS = [["dashboard", "डैशबोर्ड"], ["students", "छात्र"], ["notify", "सूचनाएँ"], ["ai", "AI गुरु"], ["categories", "श्रेणियाँ"], ["exams", "परीक्षाएँ"], ["packs", "पैक"], ["tests", "टेस्ट"],
    ["sections", "सेक्शन"], ["questions", "प्रश्न"], ["banners", "बैनर"], ["users", "उपयोगकर्ता"], ["admins", "एडमिन"]];
  const parentSel = {}; // ent -> selected parent id
  let current = "dashboard";

  function renderTabs() {
    $("#tabs").innerHTML = TABS.map(([k, l]) => `<button data-tab="${k}">${l}</button>`).join("");
    $("#tabs").onclick = (e) => { const k = e.target.dataset.tab; if (k) openTab(k); };
  }
  function openTab(k) {
    current = k;
    document.querySelectorAll("#tabs button").forEach((b) => b.classList.toggle("on", b.dataset.tab === k));
    if (k === "dashboard") return renderDashboard();
    if (k === "users") return renderUsers();
    if (k === "students") return renderStudents();
    if (k === "notify") return renderNotify();
    if (k === "ai") return renderAi();
    return renderList(k);
  }
  const fail = (error) => { if (error) { toast("त्रुटि: " + error.message); throw error; } };

  // ───────── Parent option lists ─────────
  async function optionsFor(ent) {
    if (ent === "categories") { const { data, error } = await sb.from("categories").select("id,name").order("sort"); fail(error); return data.map((r) => [r.id, r.name]); }
    if (ent === "exams") { const { data, error } = await sb.from("exams").select("id,name").order("sort"); fail(error); return data.map((r) => [r.id, r.name]); }
    if (ent === "packs") { const { data, error } = await sb.from("packs").select("id,title,exams(name)").order("sort"); fail(error); return data.map((r) => [r.id, `${r.exams?.name ?? "?"} › ${r.title}`]); }
    if (ent === "tests") { const { data, error } = await sb.from("tests").select("id,title,packs(title,exams(name))").order("sort"); fail(error);
      return data.map((r) => [r.id, `${r.packs?.exams?.name ?? "?"} › ${r.packs?.title ?? "?"} › ${r.title}`]); }
    return [];
  }
  const selectHtml = (opts, val, id, blank) =>
    `<select id="${id}">${blank ? `<option value="">${blank}</option>` : ""}${opts.map(([v, l]) => `<option value="${esc(v)}" ${v === val ? "selected" : ""}>${esc(l)}</option>`).join("")}</select>`;

  // ───────── Dashboard ─────────
  async function renderDashboard() {
    $("#view").innerHTML = `<div class="card"><h2>डैशबोर्ड</h2><div class="stats" id="stats">लोड हो रहा है…</div>
      <p class="muted" style="margin-top:14px">क्रम: श्रेणी → परीक्षा → पैक → टेस्ट → सेक्शन → प्रश्न। टेस्ट तभी ऐप में दिखेगा जब वह “प्रकाशित” हो।</p></div>`;
    const { data, error } = await sb.rpc("admin_stats");
    if (error) return ($("#stats").textContent = error.message);
    const L = { users: "उपयोगकर्ता (लॉग इन)", profiles: "प्रोफ़ाइल बनी", attempts: "कुल प्रयास", tests: "टेस्ट", questions: "प्रश्न" };
    $("#stats").innerHTML = Object.entries(L).map(([k, l]) => `<div class="stat"><b>${esc(data[k])}</b>${l}</div>`).join("");
    const r = await sb.rpc("admin_recent_submissions", { p_limit: 8 });
    if (!r.error && r.data.length) $("#view").insertAdjacentHTML("beforeend", `<div class="card"><h2>हाल के सबमिशन</h2><div class="feed">${r.data.map((a) => `<div class="it"><b>${esc(a.student)}</b> — ${esc(a.test_title || "")}: <span class="score">${num(a.score)}/${num(a.max_marks)}</span><small>${esc(fmtT(a.created_at))}</small></div>`).join("")}</div></div>`);
  }

  // ───────── Students ─────────
  const fmtT = (v) => (v ? new Date(v).toLocaleString("en-IN", { dateStyle: "medium", timeStyle: "short" }) : "—");
  const num = (v) => (v === null || v === undefined ? "—" : Number(v).toLocaleString("en-IN", { maximumFractionDigits: 2 }));
  async function renderStudents() {
    $("#view").innerHTML = `<div class="card"><h2>छात्र (Students)</h2><input id="stuQ" placeholder="नाम / ईमेल / राज्य से खोजें" style="max-width:360px"><div id="slist" style="margin-top:12px">लोड हो रहा है…</div></div><div id="sdetail"></div>`;
    const { data, error } = await sb.rpc("admin_students");
    if (error) return ($("#slist").textContent = error.message);
    const draw = (q) => {
      const rows = data.filter((u) => !q || [u.full_name, u.email, u.state, u.exam_interest].join(" ").toLowerCase().includes(q));
      $("#slist").innerHTML = `<p class="muted">कुल ${rows.length} छात्र • किसी पंक्ति पर क्लिक करें</p><div style="overflow:auto"><table><tr><th>नाम</th><th>ईमेल</th><th>राज्य</th><th>तैयारी</th><th>टेस्ट दिए</th><th>औसत अंक</th><th>औसत %</th><th>आख़िरी सक्रियता</th></tr>
        ${rows.map((u) => `<tr class="click" data-u="${esc(u.id)}" data-n="${esc(u.full_name || u.email)}"><td><b>${esc(u.full_name || "—")}</b></td><td>${esc(u.email)}</td><td>${esc(u.state || "")}</td><td>${esc(u.exam_interest || "")}</td>
        <td class="score">${esc(u.tests_taken)}</td><td>${num(u.avg_score)}</td><td>${u.avg_pct === null ? "—" : num(u.avg_pct) + "%"}</td><td>${esc(fmtT(u.last_active))}</td></tr>`).join("")}</table></div>`;
    };
    draw("");
    $("#stuQ").oninput = (e) => draw(e.target.value.trim().toLowerCase());
    $("#slist").onclick = async (e) => {
      const tr = e.target.closest("tr[data-u]"); if (!tr) return;
      $("#sdetail").innerHTML = `<div class="card"><h2>${esc(tr.dataset.n)} — प्रयास</h2><div id="sat">लोड हो रहा है…</div></div>`;
      const r = await sb.rpc("admin_student_attempts", { p_user: tr.dataset.u });
      if (r.error) return ($("#sat").textContent = r.error.message);
      $("#sat").innerHTML = r.data.length ? `<div style="overflow:auto"><table><tr><th>टेस्ट</th><th>अंक</th><th>सही</th><th>ग़लत</th><th>छोड़े</th><th>समय</th><th>कब</th></tr>
        ${r.data.map((a) => `<tr><td>${esc(a.test_title || "—")}</td><td class="score">${num(a.score)} / ${num(a.max_marks)}</td><td>${a.correct}</td><td>${a.wrong}</td><td>${a.skipped}</td><td>${Math.round(a.time_taken_sec / 60)} मि</td><td>${esc(fmtT(a.created_at))}</td></tr>`).join("")}</table></div>`
        : `<p class="muted">इस छात्र ने अभी कोई टेस्ट नहीं दिया।</p>`;
      $("#sdetail").scrollIntoView({ behavior: "smooth" });
    };
  }

  // ───────── Notifications ─────────
  async function renderNotify() {
    $("#view").innerHTML = `<div class="grid-2">
      <div class="card"><h2>सूचना भेजें</h2>
        <label>किसे<select id="nAud"><option value="all">सभी छात्र (Broadcast)</option><option value="user">एक छात्र</option></select></label>
        <label id="nUserL" class="hidden">छात्र<select id="nUser"></select></label>
        <label>शीर्षक<input id="nTitle" maxlength="120" placeholder="जैसे: कल नया मॉक टेस्ट आएगा"></label>
        <label>संदेश<textarea id="nBody" maxlength="1000"></textarea></label>
        <button class="primary" id="nSend">भेजें</button>
        <p class="muted">ऐप में बेल पर दिखेगी और लगभग 15 मिनट में फ़ोन पर नोटिफ़िकेशन आएगा।</p>
        <h3 style="margin-top:22px">हाल में भेजी गई</h3><div class="feed" id="nSent">…</div>
      </div>
      <div class="card"><h2>छात्रों के सबमिशन (Live feed)</h2><button class="small" id="nRef">↻ रीफ़्रेश</button><div class="feed" id="nFeed" style="margin-top:10px">लोड हो रहा है…</div></div>
    </div>`;
    const studs = await sb.rpc("admin_students");
    $("#nUser").innerHTML = (studs.data || []).map((u) => `<option value="${esc(u.id)}">${esc((u.full_name || "—") + " • " + u.email)}</option>`).join("");
    $("#nAud").onchange = (e) => $("#nUserL").classList.toggle("hidden", e.target.value !== "user");
    $("#nSend").onclick = async () => {
      const title = $("#nTitle").value.trim(), body = $("#nBody").value.trim(), aud = $("#nAud").value;
      if (!title) return toast("शीर्षक लिखें");
      const row = { title, body, type: "announcement", audience: aud, user_id: aud === "user" ? $("#nUser").value : null };
      const { error } = await sb.from("notifications").insert(row); fail(error);
      toast("सूचना भेज दी गई ✓"); $("#nTitle").value = ""; $("#nBody").value = ""; loadSent();
    };
    const loadSent = async () => {
      const { data, error } = await sb.from("notifications").select("*").neq("audience", "admins").order("created_at", { ascending: false }).limit(25);
      $("#nSent").innerHTML = error ? esc(error.message) : data.length ? data.map((n) => `<div class="it"><b>${esc(n.title)}</b> <span class="pill ${n.audience === "all" ? "on" : "off"}">${n.audience === "all" ? "सभी" : n.audience === "user" ? "एक छात्र" : n.audience}</span><div>${esc(n.body || "")}</div><small>${esc(fmtT(n.created_at))} • ${esc(n.type)}</small></div>`).join("") : `<p class="muted">अभी कुछ नहीं।</p>`;
    };
    const loadFeed = async () => {
      const { data, error } = await sb.rpc("admin_recent_submissions", { p_limit: 60 });
      $("#nFeed").innerHTML = error ? esc(error.message) : data.length ? data.map((a) => `<div class="it"><b>${esc(a.student)}</b> ने <b>${esc(a.test_title || "टेस्ट")}</b> दिया — <span class="score">${num(a.score)}/${num(a.max_marks)}</span> <small>सही ${a.correct} • ग़लत ${a.wrong} • ${Math.round(a.time_taken_sec / 60)} मि • ${esc(a.email || "")} • ${esc(fmtT(a.created_at))}</small></div>`).join("") : `<p class="muted">अभी कोई सबमिशन नहीं।</p>`;
    };
    $("#nRef").onclick = loadFeed;
    loadSent(); loadFeed();
  }

  // ───────── AI गुरु (admin) ─────────
  const aiHistory = [];
  async function renderAi() {
    $("#view").innerHTML = `<div class="card"><h2>AI गुरु (एडमिन)</h2><p class="muted">सभी छात्रों के टेस्ट और अंकों के बारे में हिंदी में पूछें।</p>
      <div class="sugg">${["किस छात्र ने कौन सा टेस्ट दिया, कितने अंक?", "सबसे अच्छा प्रदर्शन किसका है?", "किन छात्रों को मदद की ज़रूरत है?", "इस हफ़्ते कितने टेस्ट दिए गए?"].map((q) => `<button data-q="${esc(q)}">${esc(q)}</button>`).join("")}</div>
      <div class="chat" id="aiChat"></div>
      <div class="row" style="margin-top:10px"><input id="aiIn" placeholder="AI गुरु से पूछें…" style="flex:1;margin:0"><button class="primary" id="aiSend">भेजें</button><button id="aiClear">नई बातचीत</button></div></div>`;
    const draw = () => { $("#aiChat").innerHTML = aiHistory.length ? aiHistory.map((m) => `<div class="bub ${m.role === "user" ? "me" : "ai"}">${esc(m.text.replace(/\*\*/g, ""))}</div>`).join("") : `<p class="muted">नमस्ते! 🙏 नीचे सवाल लिखें या ऊपर का सुझाव चुनें।</p>`; $("#aiChat").scrollTop = 1e9; };
    const ask = async (q) => {
      q = (q || "").trim(); if (!q) return;
      const history = aiHistory.slice(-12);
      aiHistory.push({ role: "user", text: q }); $("#aiIn").value = ""; draw();
      $("#aiChat").insertAdjacentHTML("beforeend", `<div class="bub ai" id="aiWait">सोच रहे हैं…</div>`);
      $("#aiSend").disabled = true;
      const { data, error } = await sb.functions.invoke("ai-coach", { body: { message: q, history } });
      $("#aiSend").disabled = false;
      aiHistory.push({ role: "model", text: error ? "⚠ " + error.message : data.reply || data.error || "कोई जवाब नहीं" });
      draw();
    };
    draw();
    $("#aiSend").onclick = () => ask($("#aiIn").value);
    $("#aiIn").onkeydown = (e) => { if (e.key === "Enter") ask($("#aiIn").value); };
    $("#aiClear").onclick = () => { aiHistory.length = 0; draw(); };
    document.querySelector(".sugg").onclick = (e) => { const b = e.target.closest("button[data-q]"); if (b) ask(b.dataset.q); };
  }

  // ───────── Users ─────────
  async function renderUsers() {
    $("#view").innerHTML = `<div class="card"><h2>उपयोगकर्ता</h2><div id="ulist">लोड हो रहा है…</div></div>`;
    const { data, error } = await sb.rpc("admin_users");
    if (error) return ($("#ulist").textContent = error.message);
    $("#ulist").innerHTML = `<p class="muted">कुल ${data.length} उपयोगकर्ता</p><div style="overflow:auto"><table><tr><th>नाम</th><th>ईमेल</th><th>फ़ोन</th><th>राज्य / ज़िला</th><th>श्रेणी</th><th>प्रयास</th><th>जुड़े</th></tr>
      ${data.map((u) => `<tr><td>${esc(u.full_name || "—")}</td><td>${esc(u.email)}</td><td>${esc(u.phone || "")}</td><td>${esc([u.state, u.district].filter(Boolean).join(" / "))}</td>
      <td>${esc(u.category || "")}</td><td><b>${esc(u.attempts)}</b></td><td>${esc((u.created_at || "").slice(0, 10))}</td></tr>`).join("")}</table></div>`;
  }

  // ───────── Generic list ─────────
  async function renderList(k) {
    const E = ENT[k];
    let toolbar = "";
    if (E.parent) {
      const opts = await optionsFor(E.parent.ent);
      if (parentSel[k] === undefined || !opts.some(([v]) => v === parentSel[k])) parentSel[k] = k === "exams" ? "" : opts[0]?.[0] ?? "";
      toolbar += `<label>${E.parent.l}${selectHtml(opts, parentSel[k], "parentSel", k === "exams" ? "— सभी —" : null)}</label>`;
    }
    toolbar += `<button class="primary" id="addBtn">+ नया जोड़ें</button>`;
    if (k === "questions") toolbar += `<button id="importBtn">⬆ CSV / JSON इम्पोर्ट</button>`;
    $("#view").innerHTML = `<div class="card"><h2>${E.title}</h2><div class="toolbar">${toolbar}</div><div id="list">लोड हो रहा है…</div></div>`;
    if ($("#parentSel")) $("#parentSel").onchange = (e) => { parentSel[k] = e.target.value; loadRows(k); };
    $("#addBtn").onclick = () => (E.custom ? questionForm(null) : entityForm(k, null));
    if ($("#importBtn")) $("#importBtn").onclick = importDialog;
    loadRows(k);
  }

  let sectionsCache = {};
  async function loadRows(k) {
    const E = ENT[k];
    if (E.parent && E.parent.ent !== "categories" && !parentSel[k]) return ($("#list").innerHTML = `<p class="muted">पहले ऊपर से ${E.parent.l} चुनें (या बनाएँ)।</p>`);
    let q = sb.from(E.table).select("*");
    if (E.parent && parentSel[k]) q = q.eq(E.parent.k, parentSel[k]);
    q = E.key ? q.order(E.key) : q.order("sort").order("created_at");
    const { data, error } = await q;
    if (error) return ($("#list").textContent = error.message);
    let counts = {};
    if (k === "tests" && data.length) {
      const r = await sb.from("test_stats").select("test_id,question_count").in("test_id", data.map((d) => d.id));
      (r.data || []).forEach((s) => (counts[s.test_id] = s.question_count));
    }
    if (k === "questions") {
      const r = await sb.from("sections").select("id,name").eq("test_id", parentSel[k]);
      sectionsCache = Object.fromEntries((r.data || []).map((s) => [s.id, s.name]));
    }
    if (!data.length) return ($("#list").innerHTML = `<p class="muted">अभी कुछ नहीं है। “+ नया जोड़ें” दबाएँ${k === "questions" ? " या CSV इम्पोर्ट करें" : ""}।</p>`);
    const head = E.cols.map((c) => `<th>${colLabel(c)}</th>`).join("") + (k === "tests" ? "<th>प्रश्न</th>" : "") + "<th></th>";
    const rows = data.map((r) => {
      const id = r[E.key || "id"];
      const cells = E.cols.map((c) => `<td class="${c === "q" ? "qtext" : ""}">${cell(c, r)}</td>`).join("");
      const extra = k === "tests" ? `<td>${counts[r.id] ?? 0}</td>` : "";
      let actions = "";
      if (k === "tests") actions += `<button class="small" data-pub="${esc(id)}" data-val="${r.is_published}">${r.is_published ? "अप्रकाशित करें" : "प्रकाशित करें"}</button> <button class="small" data-goq="${esc(id)}">प्रश्न</button> `;
      if (!E.noEdit) actions += `<button class="small" data-edit="${esc(id)}">बदलें</button> `;
      actions += `<button class="small danger" data-del="${esc(id)}">हटाएँ</button>`;
      return `<tr>${cells}${extra}<td class="actions">${actions}</td></tr>`;
    }).join("");
    $("#list").innerHTML = `<div style="overflow:auto"><table><tr>${head}</tr>${rows}</table></div>`;
    $("#list").onclick = async (e) => {
      const b = e.target.closest("button"); if (!b) return;
      const keyCol = E.key || "id";
      if (b.dataset.edit) { const row = data.find((r) => r[keyCol] === b.dataset.edit); return E.custom ? questionForm(row) : entityForm(k, row); }
      if (b.dataset.del) {
        if (!confirm("पक्का हटाएँ? इससे जुड़ी नीचे की चीज़ें (पैक/टेस्ट/प्रश्न) भी हट सकती हैं।")) return;
        const { error } = await sb.from(E.table).delete().eq(keyCol, b.dataset.del); fail(error); toast("हटा दिया"); return loadRows(k);
      }
      if (b.dataset.pub) {
        const { error } = await sb.from("tests").update({ is_published: b.dataset.val !== "true" }).eq("id", b.dataset.pub); fail(error);
        toast(b.dataset.val !== "true" ? "प्रकाशित हुआ" : "अप्रकाशित किया"); return loadRows(k);
      }
      if (b.dataset.goq) { parentSel.questions = b.dataset.goq; parentSel.sections = b.dataset.goq; return openTab("questions"); }
    };
  }
  const LBL = { sort: "क्रम", name: "नाम", name_hi: "हिंदी नाम", title: "शीर्षक", title_hi: "हिंदी शीर्षक", is_active: "सक्रिय", is_free: "फ्री", price: "₹",
    duration_min: "मिनट", total_marks: "अंक", is_published: "प्रकाशित", q: "प्रश्न", correct_index: "सही", difficulty: "कठिनाई", section_id: "सेक्शन",
    image_url: "इमेज", link: "लिंक", active: "सक्रिय", email: "ईमेल" };
  const colLabel = (c) => LBL[c] || c;
  function cell(c, r) {
    const v = r[c];
    if (typeof v === "boolean") return `<span class="pill ${v ? "on" : "off"}">${v ? "हाँ" : "नहीं"}</span>`;
    if (c === "q") return esc((r.q_en || "").slice(0, 120)) + (r.q_hi ? `<br><span class="muted">${esc(r.q_hi.slice(0, 120))}</span>` : "");
    if (c === "correct_index") return LETTERS[v] ?? v;
    if (c === "section_id") return esc(sectionsCache[v] || "—");
    if (c === "image_url" && v) return `<img src="${esc(v)}" alt="" style="height:40px;border-radius:6px">`;
    return esc(v ?? "");
  }

  // ───────── Modal helpers ─────────
  function modal(title, html, onSave) {
    $("#modalTitle").textContent = title;
    $("#modalBody").innerHTML = html;
    const dlg = $("#modal");
    $("#modalForm").onsubmit = async (e) => {
      if (e.submitter && e.submitter.value === "cancel") return;
      e.preventDefault();
      $("#modalSave").disabled = true;
      try { if ((await onSave()) !== false) dlg.close(); } catch (err) { console.error(err); }
      $("#modalSave").disabled = false;
    };
    dlg.showModal();
  }
  function fieldHtml(f, v) {
    const val = v ?? f.def ?? "";
    const id = "f_" + f.k;
    if (f.t === "bool") return `<label><input type="checkbox" id="${id}" ${val === true ? "checked" : ""}>${esc(f.l)}</label>`;
    if (f.t === "textarea") return `<label>${esc(f.l)}<textarea id="${id}">${esc(val)}</textarea></label>`;
    if (f.t === "select") return `<label>${esc(f.l)}${selectHtml(f.opts, val, id)}</label>`;
    return `<label>${esc(f.l)}<input id="${id}" type="${f.t === "number" ? "number" : "text"}" step="any" value="${esc(val)}" ${f.req ? "required" : ""}></label>`;
  }
  function readField(f) {
    const el = $("#f_" + f.k);
    if (f.t === "bool") return el.checked;
    if (f.t === "number") return el.value === "" ? 0 : Number(el.value);
    const s = el.value.trim();
    return s === "" ? null : s;
  }

  async function entityForm(k, row) {
    const E = ENT[k];
    let parentHtml = "";
    if (E.parent) {
      const opts = await optionsFor(E.parent.ent);
      parentHtml = `<label>${E.parent.l}${selectHtml(opts, row ? row[E.parent.k] : parentSel[k] || opts[0]?.[0], "f_parent", k === "exams" ? "— कोई नहीं —" : null)}</label>`;
    }
    modal(`${E.title} — ${row ? "बदलें" : "नया"}`, parentHtml + E.fields.map((f) => fieldHtml(f, row?.[f.k])).join(""), async () => {
      const obj = {};
      E.fields.forEach((f) => (obj[f.k] = readField(f)));
      if (E.parent) obj[E.parent.k] = $("#f_parent").value || null;
      if (E.parent && E.parent.ent !== "categories" && !obj[E.parent.k]) { toast(E.parent.l + " चुनें"); return false; }
      const { error } = row ? await sb.from(E.table).update(obj).eq(E.key || "id", row[E.key || "id"]) : await sb.from(E.table).insert(obj);
      fail(error); toast("सेव हो गया"); loadRows(k);
    });
  }

  // ───────── Question form ─────────
  async function questionForm(row) {
    const testId = row?.test_id || parentSel.questions;
    if (!testId) return toast("पहले टेस्ट चुनें");
    const { data: secs } = await sb.from("sections").select("id,name").eq("test_id", testId).order("sort");
    const oe = row?.options_en || ["", "", "", ""], oh = row?.options_hi || ["", "", "", ""];
    const n = Math.max(4, oe.length, oh.length);
    let opts = "";
    for (let i = 0; i < n; i++)
      opts += `<div class="grid2"><label>विकल्प ${LETTERS[i]} (English)<input id="oe${i}" value="${esc(oe[i] || "")}"></label><label>विकल्प ${LETTERS[i]} (हिंदी)<input id="oh${i}" value="${esc(oh[i] || "")}"></label></div>`;
    const html = `
      <div class="grid2">
        <label>सेक्शन${selectHtml((secs || []).map((s) => [s.id, s.name]), row?.section_id || "", "q_sec", "— कोई नहीं —")}</label>
        <label>कठिनाई${selectHtml([["easy", "आसान"], ["medium", "मध्यम"], ["hard", "कठिन"]], row?.difficulty || "medium", "q_diff")}</label>
      </div>
      <label>प्रश्न (English)<textarea id="q_en">${esc(row?.q_en || "")}</textarea></label>
      <label>प्रश्न (हिंदी)<textarea id="q_hi">${esc(row?.q_hi || "")}</textarea></label>
      ${opts}
      <div class="grid2">
        <label>सही उत्तर${selectHtml([...Array(n).keys()].map((i) => [String(i), LETTERS[i]]), String(row?.correct_index ?? 0), "q_correct")}</label>
        <label>क्रम (sort)<input id="q_sort" type="number" value="${esc(row?.sort ?? "")}"></label>
      </div>
      <label>व्याख्या (English)<textarea id="q_exen">${esc(row?.explanation_en || "")}</textarea></label>
      <label>व्याख्या (हिंदी)<textarea id="q_exhi">${esc(row?.explanation_hi || "")}</textarea></label>`;
    modal(row ? "प्रश्न बदलें" : "नया प्रश्न", html, async () => {
      const options_en = [], options_hi = [];
      for (let i = 0; i < n; i++) { options_en.push($("#oe" + i).value.trim()); options_hi.push($("#oh" + i).value.trim()); }
      while (options_en.length > 2 && !options_en.at(-1) && !options_hi.at(-1)) { options_en.pop(); options_hi.pop(); }
      const obj = {
        test_id: testId, section_id: $("#q_sec").value || null, difficulty: $("#q_diff").value,
        q_en: $("#q_en").value.trim() || null, q_hi: $("#q_hi").value.trim() || null,
        options_en, options_hi: options_hi.some(Boolean) ? options_hi : [],
        correct_index: Number($("#q_correct").value), explanation_en: $("#q_exen").value.trim() || null, explanation_hi: $("#q_exhi").value.trim() || null,
      };
      if (!obj.q_en && !obj.q_hi) { toast("प्रश्न लिखें"); return false; }
      if (obj.correct_index >= options_en.length) { toast("सही उत्तर का विकल्प ख़ाली है"); return false; }
      const s = $("#q_sort").value;
      if (s !== "") obj.sort = Number(s);
      else if (!row) { const { data } = await sb.from("questions").select("sort").eq("test_id", testId).order("sort", { ascending: false }).limit(1); obj.sort = (data?.[0]?.sort ?? 0) + 1; }
      const { error } = row ? await sb.from("questions").update(obj).eq("id", row.id) : await sb.from("questions").insert(obj);
      fail(error); toast("प्रश्न सेव हुआ"); loadRows("questions");
    });
  }

  // ───────── Bulk import ─────────
  function parseCorrect(v, nOpts) {
    const s = String(v ?? "").trim().toUpperCase().replace(/[().\s]/g, "");
    if (/^[A-J]$/.test(s)) return s.charCodeAt(0) - 65;
    if (/^\d+$/.test(s)) { const n = Number(s); if (n >= 1 && n <= nOpts) return n - 1; }
    return -1;
  }
  function normalize(rows) {
    const out = [], errs = [];
    rows.forEach((r, i) => {
      const g = (k) => String(r[k] ?? r[k.toUpperCase()] ?? "").trim();
      const en = ["a", "b", "c", "d", "e"].map(g); const hi = ["a_hi", "b_hi", "c_hi", "d_hi", "e_hi"].map(g);
      while (en.length > 2 && !en.at(-1) && !hi.at(-1)) { en.pop(); hi.pop(); }
      const q = { q_en: g("q_en") || null, q_hi: g("q_hi") || null, options_en: en, options_hi: hi.some(Boolean) ? hi : [],
        correct_index: parseCorrect(r.correct ?? r.CORRECT, en.length), explanation_en: g("explanation_en") || null,
        explanation_hi: g("explanation_hi") || null, difficulty: (g("difficulty") || "medium").toLowerCase(), section: g("section") };
      if (!["easy", "medium", "hard"].includes(q.difficulty)) q.difficulty = "medium";
      const line = i + 2;
      if (!q.q_en && !q.q_hi) errs.push(`पंक्ति ${line}: प्रश्न ख़ाली`);
      else if (en.filter(Boolean).length < 2 && hi.filter(Boolean).length < 2) errs.push(`पंक्ति ${line}: कम से कम 2 विकल्प चाहिए`);
      else if (q.correct_index < 0) errs.push(`पंक्ति ${line}: "correct" A/B/C/D या 1-4 होना चाहिए`);
      else out.push(q);
    });
    return { out, errs };
  }
  function importDialog() {
    const testId = parentSel.questions;
    if (!testId) return toast("पहले टेस्ट चुनें");
    const sample = "q_en,q_hi,a,b,c,d,a_hi,b_hi,c_hi,d_hi,correct,explanation_en,explanation_hi,difficulty,section";
    const html = `<p class="muted">कॉलम: <code>${sample}</code><br>“correct” में A/B/C/D (या 1-4)। “section” में सेक्शन का नाम — न हो तो अपने-आप बन जाएगा। JSON में इन्हीं keys वाली array दें।</p>
      <label>फ़ाइल (.csv / .json)<input type="file" id="impFile" accept=".csv,.json,.txt"></label>
      <label>या यहाँ पेस्ट करें<textarea id="impText" style="min-height:120px" placeholder="${sample}"></textarea></label>
      <button type="button" id="impPreview">जाँचें</button><pre class="preview" id="impOut">—</pre>`;
    let parsed = null;
    modal("प्रश्न इम्पोर्ट करें", html, async () => {
      if (!parsed || !parsed.out.length) { toast("पहले “जाँचें” दबाएँ"); return false; }
      const { data: secs } = await sb.from("sections").select("id,name,sort").eq("test_id", testId);
      const byName = Object.fromEntries((secs || []).map((s) => [s.name.toLowerCase(), s.id]));
      let nextSecSort = Math.max(0, ...(secs || []).map((s) => s.sort)) + 1;
      for (const name of [...new Set(parsed.out.map((q) => q.section).filter(Boolean))]) {
        if (byName[name.toLowerCase()]) continue;
        const { data, error } = await sb.from("sections").insert({ test_id: testId, name, sort: nextSecSort++ }).select("id").single(); fail(error);
        byName[name.toLowerCase()] = data.id;
      }
      const { data: last } = await sb.from("questions").select("sort").eq("test_id", testId).order("sort", { ascending: false }).limit(1);
      let sort = (last?.[0]?.sort ?? 0) + 1;
      const rows = parsed.out.map(({ section, ...q }) => ({ ...q, test_id: testId, section_id: section ? byName[section.toLowerCase()] : null, sort: sort++ }));
      for (let i = 0; i < rows.length; i += 200) { const { error } = await sb.from("questions").insert(rows.slice(i, i + 200)); fail(error); }
      toast(`${rows.length} प्रश्न जोड़े गए`); loadRows("questions");
    });
    $("#impFile").onchange = async (e) => { const f = e.target.files[0]; if (f) $("#impText").value = await f.text(); };
    $("#impPreview").onclick = () => {
      const txt = $("#impText").value.trim();
      if (!txt) return;
      let rows;
      try {
        rows = txt.startsWith("[") ? JSON.parse(txt) : Papa.parse(txt, { header: true, skipEmptyLines: true, transformHeader: (h) => h.trim().toLowerCase() }).data;
      } catch (err) { $("#impOut").textContent = "पढ़ नहीं पाए: " + err.message; return; }
      parsed = normalize(rows);
      $("#impOut").textContent = `✔ ${parsed.out.length} प्रश्न तैयार` + (parsed.errs.length ? `\n✘ ${parsed.errs.length} में गड़बड़ (छोड़े जाएँगे):\n` + parsed.errs.slice(0, 30).join("\n") : "") +
        (parsed.out[0] ? `\n\nपहला: ${parsed.out[0].q_en || parsed.out[0].q_hi} → ${LETTERS[parsed.out[0].correct_index]}` : "");
    };
  }
})();
