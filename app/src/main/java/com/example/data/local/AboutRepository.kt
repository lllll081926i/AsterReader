package com.example.data.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

data class OpenSourceNotice(val id: String, val name: String, val version: String,
    val license: String, val url: String, val notice: String)

data class OpenSourceState(val loading: Boolean = false, val entries: List<OpenSourceNotice> = emptyList(),
    val error: String? = null)

/** 读取构建产物中的许可元数据，不在页面中访问文件。 */
class AboutRepository(context: Context) {
    private val assets = context.applicationContext.assets
    suspend fun notices(): List<OpenSourceNotice> = withContext(Dispatchers.IO) {
        val json = assets.open("open_source_notices.json").bufferedReader().use { it.readText() }
        val array = JSONArray(json)
        List(array.length()) { index ->
            val item = array.getJSONObject(index)
            OpenSourceNotice(item.getString("id"), item.getString("name"), item.getString("version"),
                item.getString("license"), item.getString("url"), item.getString("notice"))
        }.also { require(it.isNotEmpty() && it.map(OpenSourceNotice::id).distinct().size == it.size) }
    }
}
