package com.gay.wetherflar.android.data.sources

import com.gay.wetherflar.android.data.CrossrefItem
import com.gay.wetherflar.android.data.CrossrefWorks
import com.gay.wetherflar.android.data.Http
import com.gay.wetherflar.android.data.Paper
import com.gay.wetherflar.android.data.PaperSource
import com.gay.wetherflar.android.data.PaperType
import com.gay.wetherflar.android.data.SearchFilters
import com.gay.wetherflar.android.data.SortBy
import com.gay.wetherflar.android.data.SourceId


object CrossrefApi : PaperSource {

    override val id = SourceId.CROSSREF
    private const val BASE = "https://api.crossref.org/works"

    private fun typeOf(t: PaperType): String? = when (t) {
        PaperType.ARTICLE -> "journal-article"
        PaperType.REVIEW -> "journal-article"
        PaperType.CONFERENCE -> "proceedings-article"
        PaperType.PREPRINT -> "posted-content"
        PaperType.BOOK -> "book"
        PaperType.ANY -> null
    }

    override suspend fun search(query: String, filters: SearchFilters, sort: SortBy): List<Paper> {
        val p = StringBuilder("?rows=30")
        p.append("&query=").append(Http.enc(query))
        p.append("&select=DOI,title,author,container-title,published-print,published-online,is-referenced-by-count,abstract,resource,link,type")
        p.append("&mailto=contact@example.com")

        val fs = mutableListOf<String>()
        if (filters.yearFrom != null || filters.yearTo != null) {
            val from = filters.yearFrom ?: 1
            val to = filters.yearTo ?: 9999
            fs.add("from-pub-date:${yearPad(from)}-01-01,until-pub-date:${yearPad(to)}-12-31")
        }
        typeOf(filters.type)?.let { fs.add("type:$it") }
        if (fs.isNotEmpty()) p.append("&filter=").append(Http.enc(fs.joinToString(",")))

        p.append("&sort=").append(if (sort == SortBy.RELEVANCE) "relevance" else "score")
        p.append("&order=").append(if (sort == SortBy.YEAR_DESC) "desc" else "desc")

        val body = Http.get(BASE + p)
        val w = Http.gson.fromJson(body, CrossrefWorks::class.java)
        val list = w.message.items.mapNotNull { it.toPaper() }
        
        return if (filters.onlyOpenAccess) list.filter { it.pdfUrl != null } else list
    }

    private fun yearPad(y: Int): String = y.toString().padStart(4, '0')
}

private fun CrossrefItem.toPaper(): Paper {
    val doi = this.doi
    val cleanDoi = doi?.lowercase()?.removePrefix("https://doi.org/")?.removePrefix("http://doi.org/")
    val year = (publishedPrint?.dateParts?.firstOrNull()?.getOrNull(0))
        ?: (publishedOnline?.dateParts?.firstOrNull()?.getOrNull(0))
    val venue = containerTitle.firstOrNull()
    val authors = author.map { a ->
        listOfNotNull(a.given, a.family).joinToString(" ").trim()
    }.filter { it.isNotBlank() }
    
    val pdf = link.firstOrNull { it.contentType?.contains("pdf") == true }?.url
    val cleanAbstract = abstract?.let { it.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim() }
    return Paper(
        id = cleanDoi ?: doi ?: "",
        title = title.firstOrNull() ?: "",
        authors = authors,
        year = year,
        venue = venue,
        abstractText = cleanAbstract,
        doi = doi,
        url = resource?.primary?.url ?: (doi?.let { "https://doi.org/$it" }),
        pdfUrl = pdf,
        citationCount = citedByCount,
        sources = setOf(SourceId.CROSSREF),
        openAccess = pdf != null
    )
}
