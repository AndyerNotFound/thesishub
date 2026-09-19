package com.gay.wetherflar.android.data.sources

import com.gay.wetherflar.android.data.Http
import com.gay.wetherflar.android.data.Paper
import com.gay.wetherflar.android.data.PaperSource
import com.gay.wetherflar.android.data.PaperType
import com.gay.wetherflar.android.data.SearchFilters
import com.gay.wetherflar.android.data.SortBy
import com.gay.wetherflar.android.data.SourceId
import com.google.gson.JsonObject





object PubMedApi : PaperSource {

    override val id = SourceId.PUBMED
    private const val ESEARCH = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi"
    private const val ESUMMARY = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi"

    override suspend fun search(query: String, filters: SearchFilters, sort: SortBy): List<Paper> {
        val term = buildString {
            append(query)
            if (filters.yearFrom != null || filters.yearTo != null) {
                val from = filters.yearFrom ?: 1800
                val to = filters.yearTo ?: 9999
                append(" AND $from:$to[dp]")
            }
            if (filters.type == PaperType.REVIEW) append(" AND review[pt]")
        }
        val sortParam = if (sort == SortBy.YEAR_DESC) "pub_date" else "relevance"

        val searchBody = Http.get(
            "$ESEARCH?db=pubmed&term=${Http.enc(term)}&retmax=30&retmode=json&sort=$sortParam"
        )
        val searchObj = Http.gson.fromJson(searchBody, JsonObject::class.java)
        val idlist = searchObj.getAsJsonObject("esearchresult")
            ?.getAsJsonArray("idlist")?.map { it.asString } ?: emptyList()
        if (idlist.isEmpty()) return emptyList()

        val summaryBody = Http.get(
            "$ESUMMARY?db=pubmed&id=${idlist.joinToString(",")}&retmode=json"
        )
        val summaryObj = Http.gson.fromJson(summaryBody, JsonObject::class.java)
        val result = summaryObj.getAsJsonObject("result") ?: return emptyList()

        val papers = mutableListOf<Paper>()
        for (uid in idlist) {
            val doc = result.getAsJsonObject(uid) ?: continue
            papers.add(doc.toPaper())
        }
        
        val yf = filters.yearFrom
        val yt = filters.yearTo
        if (yf == null && yt == null) return papers
        return papers.filter { p ->
            val y = p.year ?: return@filter false
            (yf == null || y >= yf) && (yt == null || y <= yt)
        }
    }
}

private fun JsonObject.toPaper(): Paper {
    val title = get("title")?.asString ?: ""
    val authors = getAsJsonArray("authors")?.mapNotNull { a ->
        (a as? JsonObject)?.get("name")?.asString
    } ?: emptyList()
    val pubdate = get("pubdate")?.asString ?: get("epubdate")?.asString
    val year = pubdate?.take(4)?.toIntOrNull()
    val venue = get("source")?.asString

    
    var doi: String? = null
    getAsJsonArray("articleids")?.forEach { a ->
        val o = a as? JsonObject ?: return@forEach
        if (o.get("idtype")?.asString == "doi") doi = o.get("value")?.asString
    }
    if (doi == null) {
        get("elocationid")?.asString?.let { e ->
            if (e.lowercase().contains("doi")) {
                doi = e.substringAfter("doi", "").trim().trim(':').trim()
            }
        }
    }
    val cleanDoi = doi?.lowercase()?.removePrefix("https://doi.org/")?.removePrefix("http://doi.org/")
    val uid = get("uid")?.asString ?: ""
    return Paper(
        id = cleanDoi ?: "pmid:$uid",
        title = title,
        authors = authors,
        year = year,
        venue = venue,
        abstractText = null,
        doi = doi,
        url = if (cleanDoi != null) "https://doi.org/$cleanDoi" else "https://pubmed.ncbi.nlm.nih.gov/$uid/",
        pdfUrl = null,
        citationCount = null,
        sources = setOf(SourceId.PUBMED),
        openAccess = false
    )
}
