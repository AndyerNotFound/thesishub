package com.gay.wetherflar.android.data


object Citation {

    
    private fun bibAuthors(authors: List<String>): String =
        authors.joinToString(" and ") { it.trim() }

    
    private fun apaAuthor(name: String): String {
        val n = name.trim()
        if (n.isBlank()) return n
        
        val parts = n.split(Regex("\\s+"))
        return if (parts.size >= 2 && parts.all { it.matches(Regex("[A-Za-z.]+")) }) {
            val last = parts.last()
            val initials = parts.dropLast(1).joinToString(" ") { it.take(1).uppercase() + "." }
            "$last, $initials"
        } else n
    }

    private fun apaAuthors(authors: List<String>): String {
        val formatted = authors.map { apaAuthor(it) }
        return when (formatted.size) {
            0 -> ""
            1 -> formatted[0]
            2 -> "${formatted[0]} & ${formatted[1]}"
            else -> formatted.joinToString(", ")
        }
    }

    
    private fun bibKey(p: Paper): String {
        val family = p.authors.firstOrNull()?.trim()?.split(Regex("\\s+"))?.lastOrNull()?.lowercase() ?: "paper"
        val word = p.title.split(Regex("\\s+")).firstOrNull()?.lowercase() ?: "paper"
        return "${family}${p.year ?: "n.d."}$word"
    }

    private fun entryType(p: Paper): String = when {
        p.venue.equals("arXiv", ignoreCase = true) -> "misc"
        else -> "article"
    }

    fun bibtex(p: Paper): String {
        val sb = StringBuilder()
        sb.append("@").append(entryType(p)).append("{").append(bibKey(p)).append(",\n")
        sb.append("  title = {").append(clean(p.title)).append("},\n")
        if (p.authors.isNotEmpty()) sb.append("  author = {").append(clean(bibAuthors(p.authors))).append("},\n")
        if (!p.venue.isNullOrBlank()) sb.append("  journal = {").append(clean(p.venue)).append("},\n")
        if (p.year != null) sb.append("  year = {").append(p.year).append("},\n")
        if (!p.doi.isNullOrBlank()) sb.append("  doi = {").append(cleanDoi(p.doi)).append("},\n")
        if (!p.url.isNullOrBlank()) sb.append("  url = {").append(p.url).append("},\n")
        sb.append("}")
        return sb.toString()
    }

    fun apa(p: Paper): String {
        val sb = StringBuilder()
        val authors = apaAuthors(p.authors)
        if (authors.isNotBlank()) sb.append(authors).append(" ")
        sb.append("(").append(p.year ?: "n.d.").append("). ")
        sb.append(p.title).append(". ")
        if (!p.venue.isNullOrBlank()) sb.append(p.venue).append(". ")
        if (!p.doi.isNullOrBlank()) sb.append("https://doi.org/").append(cleanDoi(p.doi))
        else if (!p.url.isNullOrBlank()) sb.append(p.url)
        return sb.toString().trim()
    }

    
    private fun cleanDoi(doi: String): String = doi
        .removePrefix("https://doi.org/")
        .removePrefix("http://doi.org/")
        .removePrefix("https://dx.doi.org/")
        .removePrefix("http://dx.doi.org/")
        .removePrefix("doi:")
        .trim()

    
    private fun clean(s: String): String = s
        .replace("\\", "")
        .replace("{", "")
        .replace("}", "")
        .replace(Regex("\\s+"), " ")
        .trim()
}
