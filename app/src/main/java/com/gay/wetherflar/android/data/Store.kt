package com.gay.wetherflar.android.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.reflect.TypeToken


object Store {

    private const val PREFS = "gayhub_store"
    private const val KEY_HISTORY = "history"
    private const val KEY_FAVORITES = "favorites"
    private const val MAX_HISTORY = 30

    private lateinit var sp: SharedPreferences

    fun init(ctx: Context) {
        sp = ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    

    fun history(): List<String> {
        val json = sp.getString(KEY_HISTORY, null) ?: return emptyList()
        return runCatching {
            val t = object : TypeToken<List<String>>() {}.type
            Http.gson.fromJson<List<String>>(json, t)
        }.getOrDefault(emptyList())
    }

    fun addHistory(q: String) {
        val v = q.trim()
        if (v.isBlank()) return
        val cur = history().toMutableList()
        cur.removeAll { it.equals(v, ignoreCase = true) }
        cur.add(0, v)
        if (cur.size > MAX_HISTORY) cur.subList(MAX_HISTORY, cur.size).clear()
        sp.edit().putString(KEY_HISTORY, Http.gson.toJson(cur)).apply()
    }

    fun clearHistory() {
        sp.edit().remove(KEY_HISTORY).apply()
    }

    

    fun favorites(): List<Paper> {
        val json = sp.getString(KEY_FAVORITES, null) ?: return emptyList()
        return runCatching {
            val t = object : TypeToken<List<Paper>>() {}.type
            Http.gson.fromJson<List<Paper>>(json, t)
        }.getOrDefault(emptyList())
    }

    fun isFavorite(id: String): Boolean = favorites().any { it.id == id }

    
    fun toggleFavorite(p: Paper): Boolean {
        val cur = favorites().toMutableList()
        val idx = cur.indexOfFirst { it.id == p.id }
        return if (idx >= 0) {
            cur.removeAt(idx)
            saveFavorites(cur)
            false
        } else {
            cur.add(0, p)
            saveFavorites(cur)
            true
        }
    }

    fun removeFavorite(id: String) {
        val cur = favorites().filter { it.id != id }
        saveFavorites(cur)
    }

    private fun saveFavorites(list: List<Paper>) {
        sp.edit().putString(KEY_FAVORITES, Http.gson.toJson(list)).apply()
    }
}
