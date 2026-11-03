package ir.mosafir.recovery.data

import android.content.Context
import org.json.JSONObject

/** یک سؤال تستی بانک — گزینه‌ها همیشه با ترتیب ثابت الف/ب/ج/د */
data class BankMcq(
    val id: String,
    val q: String,
    val keys: List<String>,      // برچسب گزینه‌ها (الف/ب/…)
    val opts: List<String>,      // متن گزینه‌ها هم‌تراز با keys
    val ansIndex: Int,           // ایندکس گزینهٔ درست
    val cat: String,
    val ref: String
)

/** یک سؤال تشریحی با پاسخ کوتاه منابع */
data class BankQa(
    val id: String,
    val q: String,
    val a: String,
    val cat: String,
    val ref: String
)

data class QuestionBankData(
    val mcq: List<BankMcq>,
    val qa: List<BankQa>,
    val mcqCats: List<String>,
    val qaCats: List<String>
)

/**
 * بانک سؤالات آزمون — از assets/questionbank.json خوانده می‌شود.
 * منبع ساخت: پوشهٔ upload (ده‌ها آزمون واقعی کنگره ۶۰) با اسکریپت‌های
 * extract_questions.py / decode_answer_keys.py / build_bank.py / export_app_bank.py.
 */
object QuestionBank {

    @Volatile
    private var cache: QuestionBankData? = null

    fun load(context: Context): QuestionBankData {
        cache?.let { return it }
        synchronized(this) {
            cache?.let { return it }
            val data = parse(context.assets.open("questionbank.json").bufferedReader().use { it.readText() })
            cache = data
            return data
        }
    }

    internal fun parse(json: String): QuestionBankData {
        val root = JSONObject(json)
        val mcq = ArrayList<BankMcq>()
        val qa = ArrayList<BankQa>()
        val mcqCats = LinkedHashSet<String>()
        val qaCats = LinkedHashSet<String>()

        val mcqArr = root.optJSONArray("mcq")
        if (mcqArr != null) {
            for (i in 0 until mcqArr.length()) {
                val o = mcqArr.getJSONObject(i)
                val optsObj = o.optJSONObject("opts") ?: continue
                val ans = o.optString("ans", "")
                if (ans.isEmpty()) continue
                // ترتیب ثابت الف/ب/ج/د — فقط گزینه‌های موجود
                val keys = ArrayList<String>()
                val texts = ArrayList<String>()
                for (k in LETTER_ORDER) {
                    val v = optsObj.optString(k, "")
                    if (v.isNotBlank()) {
                        keys.add(k)
                        texts.add(v.trim())
                    }
                }
                val ansIndex = keys.indexOf(ans)
                if (ansIndex < 0 || texts.size < 2) continue
                val cat = o.optString("cat", "آزمون عمومی").ifBlank { "آزمون عمومی" }
                mcq.add(
                    BankMcq(
                        id = o.optString("id", ""),
                        q = o.optString("q", "").trim(),
                        keys = keys,
                        opts = texts,
                        ansIndex = ansIndex,
                        cat = cat,
                        ref = o.optString("ref", "")
                    )
                )
                mcqCats.add(cat)
            }
        }

        val qaArr = root.optJSONArray("qa")
        if (qaArr != null) {
            for (i in 0 until qaArr.length()) {
                val o = qaArr.getJSONObject(i)
                val q = o.optString("q", "").trim()
                val a = o.optString("a", "").trim()
                if (q.length < 12 || a.length < 10) continue
                val cat = o.optString("cat", "آزمون عمومی").ifBlank { "آزمون عمومی" }
                qa.add(
                    BankQa(
                        id = o.optString("id", ""),
                        q = q,
                        a = a,
                        cat = cat,
                        ref = o.optString("ref", "")
                    )
                )
                qaCats.add(cat)
            }
        }

        return QuestionBankData(
            mcq = mcq,
            qa = qa,
            mcqCats = mcqCats.toList(),
            qaCats = qaCats.toList()
        )
    }

    private val LETTER_ORDER = listOf("الف", "ب", "ج", "د")
}
