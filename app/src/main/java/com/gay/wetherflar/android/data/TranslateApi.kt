package com.gay.wetherflar.android.data

import com.google.gson.JsonObject
import java.io.IOException






object TranslateApi {

    private const val BASE = "https://api.mymemory.translated.net/get"
    private const val CHUNK = 450   

    suspend fun translate(text: String, from: String = "en", to: String = "zh-CN"): String {
        val src = text.trim()
        if (src.isEmpty()) return ""
        val chunks = splitChunks(src, CHUNK)
        val parts = chunks.map { chunk ->
            val url = "$BASE?q=${Http.enc(chunk)}&langpair=$from|$to"
            val body = Http.get(url)
            val obj = Http.gson.fromJson(body, JsonObject::class.java)
            val status = obj.get("responseStatus")?.asInt ?: -1
            val data = obj.getAsJsonObject("responseData")
            val translated = data?.get("translatedText")?.asString
            if (status != 200 || translated.isNullOrBlank()) {
                val detail = obj.get("responseDetails")?.asString ?: "未知错误"
                throw IOException("翻译失败：$detail")
            }
            translated
        }
        return parts.joinToString("")
    }

    
    private fun splitChunks(text: String, max: Int): List<String> {
        if (text.length <= max) return listOf(text)
        val chunks = mutableListOf<String>()
        var rest = text
        while (rest.length > max) {
            val head = rest.substring(0, max)
            val period = head.lastIndexOfAny(charArrayOf('.', '!', '?', '\n'))
            val space = head.lastIndexOf(' ')
            val cut = when {
                period > max / 2 -> period + 1
                space > max / 2 -> space
                else -> max
            }
            chunks.add(rest.substring(0, cut))
            rest = rest.substring(cut).trimStart()
        }
        if (rest.isNotEmpty()) chunks.add(rest)
        return chunks
    }
}
