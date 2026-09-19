package com.gay.wetherflar.android.util

import java.text.DecimalFormat


fun fmtNum(n: Long): String {
    if (n < 1000) return n.toString()
    val units = arrayOf("k", "M", "B", "T")
    var v = n.toDouble()
    var i = -1
    while (v >= 1000 && i < units.size - 1) { v /= 1000; i++ }
    val s = if (v >= 100) "%.0f" else if (v >= 10) "%.1f" else "%.2f"
    return String.format(java.util.Locale.US, s, v) + units[i]
}


fun fmtBytes(n: Long): String {
    if (n < 1024) return "$n B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var v = n.toDouble()
    var i = -1
    while (v >= 1024 && i < units.size - 1) { v /= 1024; i++ }
    val s = if (v >= 100) "%.0f" else if (v >= 10) "%.1f" else "%.2f"
    return String.format(java.util.Locale.US, s, v) + " " + units[i]
}


fun fmtDate(iso: String?): String =
    if (iso.isNullOrBlank()) "" else iso.take(10)


fun fmtParams(total: Long): String = fmtNum(total)
