package com.smartexam.academy.data

import org.json.JSONArray
import org.json.JSONObject

private fun JSONObject.s(k: String): String = if (isNull(k)) "" else optString(k, "")
private fun JSONObject.sn(k: String): String? = if (isNull(k)) null else optString(k, "").ifBlank { null }
private fun JSONArray.strings(): List<String> = (0 until length()).map { optString(it, "") }
private fun JSONArray.objs(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

fun <T> JSONArray.map(f: (JSONObject) -> T): List<T> = objs().map(f)

data class Category(val id: String, val name: String, val nameHi: String?, val iconUrl: String?) {
    companion object {
        fun from(j: JSONObject) = Category(j.s("id"), j.s("name"), j.sn("name_hi"), j.sn("icon_url"))
    }
}

data class Exam(
    val id: String, val categoryId: String?, val name: String, val nameHi: String?,
    val description: String?, val logoUrl: String?,
    var packCount: Int = 0, var testCount: Int = 0
) {
    companion object {
        fun from(j: JSONObject) = Exam(
            j.s("id"), j.sn("category_id"), j.s("name"), j.sn("name_hi"), j.sn("description"), j.sn("logo_url")
        )
    }
}

data class Pack(
    val id: String, val examId: String, val title: String, val titleHi: String?,
    val isFree: Boolean, val price: Int, var testCount: Int = 0
) {
    companion object {
        fun from(j: JSONObject) = Pack(
            j.s("id"), j.s("exam_id"), j.s("title"), j.sn("title_hi"),
            j.optBoolean("is_free", true), j.optInt("price", 0)
        )
    }
}

data class Test(
    val id: String, val packId: String, val title: String, val durationMin: Int,
    val totalMarks: Double, val marksCorrect: Double, val marksWrong: Double,
    val language: String, val isFree: Boolean, val instructions: String?,
    var questionCount: Int = 0, var sectionCount: Int = 0
) {
    val hasEnglish get() = language != "hi"
    val hasHindi get() = language != "en"

    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("pack_id", packId).put("title", title).put("duration_min", durationMin)
        .put("total_marks", totalMarks).put("marks_correct", marksCorrect).put("marks_wrong", marksWrong)
        .put("language", language).put("is_free", isFree).put("instructions", instructions ?: JSONObject.NULL)
        .put("question_count", questionCount).put("section_count", sectionCount)

    companion object {
        fun from(j: JSONObject) = Test(
            j.s("id"), j.s("pack_id"), j.s("title"), j.optInt("duration_min", 15),
            j.optDouble("total_marks", 0.0), j.optDouble("marks_correct", 1.0), j.optDouble("marks_wrong", 0.25),
            j.optString("language", "both"), j.optBoolean("is_free", true), j.sn("instructions"),
            j.optInt("question_count", 0), j.optInt("section_count", 0)
        )
    }
}

data class Section(val id: String, val name: String, val sort: Int) {
    fun toJson(): JSONObject = JSONObject().put("id", id).put("name", name).put("sort", sort)
    companion object {
        fun from(j: JSONObject) = Section(j.s("id"), j.s("name"), j.optInt("sort", 0))
    }
}

data class Question(
    val id: String, val sectionId: String?, val qEn: String, val qHi: String,
    val optionsEn: List<String>, val optionsHi: List<String>, val correct: Int,
    val explanationEn: String?, val explanationHi: String?, val difficulty: String
) {
    fun text(hindi: Boolean) = if (hindi && qHi.isNotBlank()) qHi else qEn.ifBlank { qHi }
    fun options(hindi: Boolean): List<String> =
        if (hindi && optionsHi.any { it.isNotBlank() }) optionsHi else optionsEn.ifEmpty { optionsHi }
    fun explanation(hindi: Boolean): String? =
        (if (hindi) explanationHi ?: explanationEn else explanationEn ?: explanationHi)?.ifBlank { null }

    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("section_id", sectionId ?: JSONObject.NULL).put("q_en", qEn).put("q_hi", qHi)
        .put("options_en", JSONArray(optionsEn)).put("options_hi", JSONArray(optionsHi))
        .put("correct_index", correct)
        .put("explanation_en", explanationEn ?: JSONObject.NULL)
        .put("explanation_hi", explanationHi ?: JSONObject.NULL)
        .put("difficulty", difficulty)

    companion object {
        fun from(j: JSONObject) = Question(
            j.s("id"), j.sn("section_id"), j.s("q_en"), j.s("q_hi"),
            j.optJSONArray("options_en")?.strings() ?: emptyList(),
            j.optJSONArray("options_hi")?.strings() ?: emptyList(),
            j.optInt("correct_index", 0), j.sn("explanation_en"), j.sn("explanation_hi"),
            j.optString("difficulty", "medium").ifBlank { "medium" }
        )
    }
}

/** Everything needed to take a test, also what is saved for offline use. */
data class TestBundle(val test: Test, val sections: List<Section>, val questions: List<Question>, val packTitle: String = "") {
    fun toJson(): JSONObject = JSONObject()
        .put("test", test.toJson()).put("pack_title", packTitle)
        .put("sections", JSONArray(sections.map { it.toJson() }))
        .put("questions", JSONArray(questions.map { it.toJson() }))

    fun sectionName(id: String?): String = sections.firstOrNull { it.id == id }?.name ?: "General"

    companion object {
        fun from(j: JSONObject) = TestBundle(
            Test.from(j.getJSONObject("test")),
            j.getJSONArray("sections").map { Section.from(it) },
            j.getJSONArray("questions").map { Question.from(it) },
            j.optString("pack_title", "")
        )
    }
}

data class Banner(val id: String, val imageUrl: String, val link: String?) {
    companion object {
        fun from(j: JSONObject) = Banner(j.s("id"), j.s("image_url"), j.sn("link"))
    }
}

data class Profile(
    val id: String, val fullName: String, val phone: String, val state: String,
    val district: String, val category: String
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("full_name", fullName).put("phone", phone).put("state", state)
        .put("district", district).put("category", category)

    companion object {
        fun from(j: JSONObject) = Profile(
            j.s("id"), j.s("full_name"), j.s("phone"), j.s("state"), j.s("district"), j.s("category")
        )
    }
}

/** One finished attempt. [answers] maps question id → chosen option index. */
data class Attempt(
    val id: String?, val testId: String, val testTitle: String,
    val answers: Map<String, Int>, val score: Double, val correct: Int, val wrong: Int,
    val skipped: Int, val timeTakenSec: Int, val createdAt: String, val synced: Boolean,
    val totalMarks: Double = 0.0
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id ?: JSONObject.NULL).put("test_id", testId).put("test_title", testTitle)
        .put("answers", answersJson()).put("score", score).put("correct", correct).put("wrong", wrong)
        .put("skipped", skipped).put("time_taken_sec", timeTakenSec).put("created_at", createdAt)
        .put("synced", synced).put("total_marks", totalMarks)

    fun answersJson(): JSONObject = JSONObject().also { o -> answers.forEach { (k, v) -> o.put(k, v) } }

    companion object {
        fun from(j: JSONObject): Attempt {
            val a = j.optJSONObject("answers") ?: JSONObject()
            val map = a.keys().asSequence().mapNotNull { k ->
                val v = a.opt(k); if (v is Number) k to v.toInt() else null
            }.toMap()
            val t = j.optJSONObject("tests")
            return Attempt(
                j.sn("id"), j.s("test_id"),
                t?.optString("title") ?: j.optString("test_title", "Test"),
                map, j.optDouble("score", 0.0), j.optInt("correct"), j.optInt("wrong"), j.optInt("skipped"),
                j.optInt("time_taken_sec"), j.s("created_at"), j.optBoolean("synced", true),
                t?.optDouble("total_marks", 0.0) ?: j.optDouble("total_marks", 0.0)
            )
        }
    }
}

data class LeaderRow(val rank: Int, val name: String, val score: Double, val timeSec: Int, val isMe: Boolean)

/** Pure scoring, same formula as the submit_attempt RPC. */
object Scorer {
    fun grade(bundle: TestBundle, answers: Map<String, Int>, timeSec: Int, createdAt: String): Attempt {
        var c = 0; var w = 0; var s = 0
        bundle.questions.forEach { q ->
            val a = answers[q.id]
            when {
                a == null -> s++
                a == q.correct -> c++
                else -> w++
            }
        }
        val t = bundle.test
        val score = Math.round((c * t.marksCorrect - w * t.marksWrong) * 100.0) / 100.0
        return Attempt(null, t.id, t.title, answers, score, c, w, s, timeSec, createdAt, false, maxMarks(bundle))
    }

    fun maxMarks(b: TestBundle): Double =
        if (b.test.totalMarks > 0) b.test.totalMarks else b.questions.size * b.test.marksCorrect
}
