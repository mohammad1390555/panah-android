package ir.panahannet.panah

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class Store(ctx: Context) {
    private val sp = ctx.getSharedPreferences("panah", Context.MODE_PRIVATE)

    fun all(): MutableList<Sub> {
        val arr = JSONArray(sp.getString("subs", "[]"))
        val out = mutableListOf<Sub>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out += Sub(
                id = o.getString("id"),
                name = o.getString("name"),
                url = o.getString("url"),
                cache = o.optString("cache"),
                updatedAt = o.optLong("updatedAt"),
                lastError = o.optString("lastError"),
                nodeCount = o.optInt("nodeCount")
            )
        }
        return out
    }

    fun save(list: List<Sub>) {
        val arr = JSONArray()
        list.forEach { s ->
            arr.put(
                JSONObject().apply {
                    put("id", s.id)
                    put("name", s.name)
                    put("url", s.url)
                    put("cache", s.cache)
                    put("updatedAt", s.updatedAt)
                    put("lastError", s.lastError)
                    put("nodeCount", s.nodeCount)
                }
            )
        }
        sp.edit().putString("subs", arr.toString()).apply()
    }
}
