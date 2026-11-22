// Smart Exam Academy — "AI गुरु" coach.
// POST { message: string, history?: [{role:"user"|"model", text:string}] } with the user's JWT.
// Loads the caller's context through the ai_context() RPC (RLS + is_admin aware), then asks Gemini.
const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const ANON = Deno.env.get("SUPABASE_ANON_KEY")!;
const GEMINI_KEY = Deno.env.get("GEMINI_API_KEY") ?? "";
const MODELS = [...new Set([Deno.env.get("GEMINI_MODEL") ?? "gemini-flash-latest", "gemini-2.5-flash", "gemini-flash-lite-latest", "gemini-2.5-flash-lite"])];

const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};
const json = (b: unknown, status = 200) =>
  new Response(JSON.stringify(b), { status, headers: { ...cors, "Content-Type": "application/json" } });

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: cors });
  if (req.method !== "POST") return json({ error: "POST only" }, 405);
  const auth = req.headers.get("Authorization") ?? "";
  if (!auth.startsWith("Bearer ")) return json({ error: "लॉग इन ज़रूरी है" }, 401);

  let body: { message?: string; history?: { role: string; text: string }[] } = {};
  try { body = await req.json(); } catch { /* empty */ }
  const message = (body.message ?? "").toString().trim().slice(0, 4000);
  if (!message) return json({ error: "message खाली है" }, 400);

  // Caller context (also validates the JWT: PostgREST rejects a bad token)
  const ctxRes = await fetch(`${SUPABASE_URL}/rest/v1/rpc/ai_context`, {
    method: "POST",
    headers: { apikey: ANON, Authorization: auth, "Content-Type": "application/json" },
    body: "{}",
  });
  if (!ctxRes.ok) return json({ error: "सेशन अमान्य है, फिर से लॉग इन करें" }, 401);
  const ctx = await ctxRes.json();

  if (!GEMINI_KEY) return json({ reply: "AI जल्द ही चालू होगा 🙏", ready: false, is_admin: !!ctx?.is_admin });

  const role = ctx?.is_admin
    ? `तुम Smart Exam Academy के मालिक/एडमिन के सहायक हो। नीचे "students" में सभी छात्रों और उनके टेस्ट/अंकों का डेटा है। ` +
      `एडमिन पूछे कि किस छात्र ने कौन सा टेस्ट दिया, कितने अंक आए, कौन कमज़ोर है — तो इसी डेटा से सटीक जवाब दो (तालिका/सूची में)। डेटा में जो नहीं है, उसे मत गढ़ो।`
    : `तुम "AI गुरु" हो — Smart Exam Academy ऐप के छात्र के निजी प्रतियोगी-परीक्षा कोच (SSC, Railway, Banking, Police, UPSC, State exams आदि)। ` +
      `छात्र के टेस्ट परिणाम ("my_attempts") देखकर कमज़ोरियाँ बताओ, पढ़ाई की योजना, टिप्स, विषय समझाओ और प्रश्न हल करो। ` +
      `किसी दूसरे छात्र की जानकारी कभी मत बताओ।`;
  const system =
    `${role}\nहमेशा सरल हिंदी (देवनागरी) में जवाब दो; परीक्षा/विषय के नाम English में रह सकते हैं। ` +
    `जवाब छोटा, साफ़ और उपयोगी रखो, ज़रूरत हो तो बुलेट पॉइंट। गर्मजोशी से प्रोत्साहित करो।\n\n` +
    `संदर्भ डेटा (JSON):\n${JSON.stringify(ctx).slice(0, 60000)}`;

  const history = (body.history ?? []).slice(-12).map((h) => ({
    role: h.role === "model" ? "model" : "user",
    parts: [{ text: String(h.text ?? "").slice(0, 4000) }],
  }));
  const payload = {
    systemInstruction: { parts: [{ text: system }] },
    contents: [...history, { role: "user", parts: [{ text: message }] }],
    generationConfig: { temperature: 0.6, maxOutputTokens: 2048 },
  };

  let lastErr = "";
  const errs: string[] = [];
  for (const model of MODELS) for (let attempt = 0; attempt < 2; attempt++) {
    if (attempt > 0) await new Promise((res) => setTimeout(res, 1200));
    const r = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent`, {
      method: "POST",
      headers: { "Content-Type": "application/json", "x-goog-api-key": GEMINI_KEY },
      body: JSON.stringify(payload),
    });
    const t = await r.text();
    if (!r.ok) {
      lastErr = `${model}: ${r.status} ${t.slice(0, 200)}`; errs.push(lastErr);
      if (r.status >= 500 || r.status === 429) continue;   // retry, then next model
      break;                                              // 4xx: next model
    }
    try {
      const d = JSON.parse(t);
      const reply = (d.candidates?.[0]?.content?.parts ?? []).map((p: { text?: string }) => p.text ?? "").join("").trim();
      if (reply) return json({ reply, ready: true, model, is_admin: !!ctx?.is_admin });
      lastErr = `${model}: empty reply ${t.slice(0, 200)}`; errs.push(lastErr);
      break;
    } catch (e) { lastErr = `${model}: ${e}`; errs.push(lastErr); break; }
  }
  console.error("gemini failed", errs.join(" | "));
  return json({ reply: "माफ़ कीजिए, AI गुरु अभी जवाब नहीं दे पा रहे। थोड़ी देर बाद फिर पूछें।", ready: true, error: errs.map((e) => e.slice(0, 120)).join(" | ").slice(0, 600) });
});
