package com.gay.wetherflar.android.data

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit


object Http {

    const val UA = "GayHub/0.1 (Android; academic-search; mailto:contact@example.com)"

    val gson: Gson = Gson()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun enc(v: String): String = URLEncoder.encode(v, "UTF-8")

    suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", UA)
            .header("Accept", "*/*")
            .build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IOException("HTTP ${resp.code} for ${url.take(80)}")
            }
            body
        }
    }

    suspend fun download(url: String, dest: java.io.File) = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", UA)
            .header("Accept", "*/*")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IOException("HTTP ${resp.code} for ${url.take(80)}")
            }
            val body = resp.body ?: throw IOException("empty body for ${url.take(80)}")
            body.byteStream().use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
        }
    }
}
