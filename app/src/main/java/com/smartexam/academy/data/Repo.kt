package com.smartexam.academy.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
private fun inList(ids: Collection<String>) = "in.(" + ids.joinToString(",") + ")"

fun nowIso(): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    .apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())

/** All server reads/writes plus the offline cache. */
class Repo(ctx: Context) {
    val supa = Supa(ctx)
    val local = LocalStore(ctx)
    val prefs = AppPrefs(ctx)

    // ───────── Content ─────────

    suspend fun categories(): List<Category> =
        supa.get("categories?select=*&order=sort.asc,name.asc").map { Category.from(it) }

    suspend fun banners(): List<Banner> =
        supa.get("banners?select=*&active=eq.true&order=sort.asc").map { Banner.from(it) }

    suspend fun exams(categoryId: String? = null): List<Exam> {
        val f = if (categoryId != null) "&category_id=eq.$categoryId" else ""
        val list = supa.get("exams?select=*&is_active=eq.true$f&order=sort.asc,name.asc").map { Exam.from(it) }
        if (list.isNotEmpty()) {
            val stats = supa.get("exam_stats?select=*&exam_id=${inList(list.map { it.id })}")
            val byId = (0 until stats.length()).associate { stats.getJSONObject(it).let { s -> s.getString("exam_id") to s } }
            list.forEach { e -> byId[e.id]?.let { e.packCount = it.optInt("pack_count"); e.testCount = it.optInt("test_count") } }
        }
        return list
    }

    suspend fun exam(id: String): Exam? =
        supa.get("exams?select=*&id=eq.$id").map { Exam.from(it) }.firstOrNull()?.also { e ->
            supa.get("exam_stats?select=*&exam_id=eq.$id").map { it }.firstOrNull()?.let {
                e.packCount = it.optInt("pack_count"); e.testCount = it.optInt("test_count")
            }
        }

    suspend fun packs(examId: String): List<Pack> {
        val list = supa.get("packs?select=*&exam_id=eq.$examId&order=sort.asc").map { Pack.from(it) }
        if (list.isNotEmpty()) {
            val stats = supa.get("pack_stats?select=*&exam_id=eq.$examId").map { it }
            list.forEach { p -> stats.firstOrNull { it.optString("pack_id") == p.id }?.let { p.testCount = it.optInt("test_count") } }
        }
        return list
    }

    suspend fun pack(id: String): Pack? = supa.get("packs?select=*&id=eq.$id").map { Pack.from(it) }.firstOrNull()

    suspend fun tests(packId: String): List<Test> {
        val list = supa.get("tests?select=*&pack_id=eq.$packId&is_published=eq.true&order=sort.asc").map { Test.from(it) }
        if (list.isNotEmpty()) {
            val stats = supa.get("test_stats?select=*&pack_id=eq.$packId").map { it }
            list.forEach { t ->
                stats.firstOrNull { it.optString("test_id") == t.id }?.let {
                    t.questionCount = it.optInt("question_count"); t.sectionCount = it.optInt("section_count")
                }
            }
        }
        return list
    }

    /** Full test with questions: from the offline copy if saved, otherwise the server. */
    suspend fun bundle(testId: String, preferOffline: Boolean = true): TestBundle {
        if (preferOffline) local.offlineBundle(testId)?.let { return it }
        val test = supa.get("tests?select=*&id=eq.$testId").map { Test.from(it) }.firstOrNull()
            ?: throw ApiException(404, "टेस्ट नहीं मिला")
        val sections = supa.get("sections?select=*&test_id=eq.$testId&order=sort.asc").map { Section.from(it) }
        val questions = supa.get("questions?select=*&test_id=eq.$testId&order=sort.asc").map { Question.from(it) }
        val packTitle = try { pack(test.packId)?.title ?: "" } catch (_: Exception) { "" }
        test.questionCount = questions.size; test.sectionCount = sections.size
        return TestBundle(test, sections, questions, packTitle)
    }

    suspend fun downloadForOffline(testId: String): TestBundle {
        val b = bundle(testId, preferOffline = false)
        local.saveOffline(b)
        return b
    }

    // ───────── Profile ─────────

    suspend fun profile(): Profile? {
        val uid = supa.session.userId ?: return null
        return supa.get("profiles?select=*&id=eq.$uid").map { Profile.from(it) }.firstOrNull()
            ?.also { prefs.cachedName = it.fullName }
    }

    suspend fun saveProfile(p: Profile) {
        supa.insert("profiles?on_conflict=id", p.toJson(), upsert = true)
        prefs.cachedName = p.fullName
    }

    // ───────── Attempts ─────────

    /** Saves locally first, then tries the server; returns the attempt with its sync state. */
    suspend fun submit(bundle: TestBundle, answers: Map<String, Int>, timeSec: Int): Attempt {
        val graded = Scorer.grade(bundle, answers, timeSec, nowIso())
        return try {
            val synced = upload(graded)
            local.addHistory(synced); synced
        } catch (e: Exception) {
            local.addPending(graded); local.addHistory(graded); graded
        }
    }

    private suspend fun upload(a: Attempt): Attempt {
        val res = supa.rpc(
            "submit_attempt",
            JSONObject().put("p_test_id", a.testId).put("p_answers", a.answersJson()).put("p_time_sec", a.timeTakenSec)
        )
        val row = if (res.trim().startsWith("[")) JSONArray(res).getJSONObject(0) else JSONObject(res)
        val server = Attempt.from(row)
        return a.copy(id = server.id, score = server.score, correct = server.correct, wrong = server.wrong,
            skipped = server.skipped, createdAt = server.createdAt.ifBlank { a.createdAt }, synced = true)
    }

    /** Push attempts made offline. Returns how many were uploaded. */
    suspend fun syncPending(): Int {
        var n = 0
        for (a in local.pending()) {
            try {
                val up = upload(a)
                local.removePending(a.createdAt, a.testId)
                local.markSynced(a.createdAt, a.testId, up.id)
                n++
            } catch (_: Exception) { break }
        }
        return n
    }

    suspend fun attempts(): List<Attempt> {
        val uid = supa.session.userId ?: return local.history()
        val server = supa.get(
            "attempts?select=*,tests(title,total_marks)&user_id=eq.$uid&order=created_at.desc&limit=200"
        ).map { Attempt.from(it) }
        val pend = local.pending()
        return (pend + server).sortedByDescending { it.createdAt }
    }

    suspend fun attemptsForTest(testId: String): List<Attempt> = try {
        val uid = supa.session.userId ?: ""
        supa.get("attempts?select=*,tests(title,total_marks)&user_id=eq.$uid&test_id=eq.$testId&order=created_at.desc")
            .map { Attempt.from(it) } + local.pending().filter { it.testId == testId }
    } catch (_: Exception) {
        local.history().filter { it.testId == testId }
    }

    suspend fun leaderboard(testId: String): List<LeaderRow> {
        val res = JSONArray(supa.rpc("test_leaderboard", JSONObject().put("p_test_id", testId).put("p_limit", 50)))
        return res.map {
            LeaderRow(it.optInt("rank"), it.optString("full_name", "Student"), it.optDouble("score"),
                it.optInt("time_taken_sec"), it.optBoolean("is_me"))
        }
    }

    suspend fun search(q: String): List<Exam> {
        val p = enc("*${q.replace(",", " ").replace("(", " ").replace(")", " ").trim()}*")
        return supa.get("exams?select=*&is_active=eq.true&or=(name.ilike.$p,name_hi.ilike.$p)&order=sort.asc&limit=30")
            .map { Exam.from(it) }
    }
}

/** Files under filesDir: offline tests, attempts waiting to sync, recent local history. */
class LocalStore(ctx: Context) {
    private val dir = File(ctx.filesDir, "offline").apply { mkdirs() }
    private val pendingFile = File(ctx.filesDir, "pending_attempts.json")
    private val historyFile = File(ctx.filesDir, "local_history.json")

    fun saveOffline(b: TestBundle) = File(dir, "${b.test.id}.json").writeText(b.toJson().toString())
    fun offlineBundle(id: String): TestBundle? =
        File(dir, "$id.json").takeIf { it.exists() }?.let { runCatching { TestBundle.from(JSONObject(it.readText())) }.getOrNull() }
    fun isOffline(id: String) = File(dir, "$id.json").exists()
    fun deleteOffline(id: String) { File(dir, "$id.json").delete() }
    fun offlineList(): List<TestBundle> = (dir.listFiles() ?: emptyArray())
        .sortedByDescending { it.lastModified() }
        .mapNotNull { runCatching { TestBundle.from(JSONObject(it.readText())) }.getOrNull() }

    @Synchronized private fun readArr(f: File): List<Attempt> =
        if (!f.exists()) emptyList() else runCatching { JSONArray(f.readText()).map { Attempt.from(it) } }.getOrDefault(emptyList())
    @Synchronized private fun writeArr(f: File, list: List<Attempt>) =
        f.writeText(JSONArray(list.map { it.toJson() }).toString())

    fun pending(): List<Attempt> = readArr(pendingFile)
    fun addPending(a: Attempt) = writeArr(pendingFile, pending() + a)
    fun removePending(createdAt: String, testId: String) =
        writeArr(pendingFile, pending().filterNot { it.createdAt == createdAt && it.testId == testId })

    fun history(): List<Attempt> = readArr(historyFile)
    fun addHistory(a: Attempt) = writeArr(historyFile, (listOf(a) + history()).take(100))
    fun markSynced(createdAt: String, testId: String, id: String?) = writeArr(historyFile,
        history().map { if (it.createdAt == createdAt && it.testId == testId) it.copy(synced = true, id = id) else it })

    fun clearAll() {
        pendingFile.delete(); historyFile.delete()
    }
}

/** Small UI preferences. */
class AppPrefs(ctx: Context) {
    private val p = ctx.getSharedPreferences("sea_prefs", Context.MODE_PRIVATE)

    /** 0 = system, 1 = day, 2 = night */
    var theme: Int
        get() = p.getInt("theme", 0)
        set(v) = p.edit().putInt("theme", v).apply()
    var hindiDefault: Boolean
        get() = p.getBoolean("hindi", false)
        set(v) = p.edit().putBoolean("hindi", v).apply()
    var cachedName: String
        get() = p.getString("name", "") ?: ""
        set(v) = p.edit().putString("name", v).apply()

    /** Exams the student added to "My exams" (id → display name). */
    fun myExams(): List<Pair<String, String>> = (p.getString("my_exams", "") ?: "")
        .split("\n").filter { it.contains("|") }.map { it.substringBefore("|") to it.substringAfter("|") }
    fun isMyExam(id: String) = myExams().any { it.first == id }
    fun toggleMyExam(id: String, name: String) {
        val cur = myExams()
        val next = if (cur.any { it.first == id }) cur.filterNot { it.first == id } else listOf(id to name) + cur
        p.edit().putString("my_exams", next.joinToString("\n") { "${it.first}|${it.second.replace("\n", " ")}" }).apply()
    }

    fun clear() = p.edit().clear().apply()
}
