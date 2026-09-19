package com.gay.wetherflar.android.data.sources

import com.gay.wetherflar.android.data.Http
import com.gay.wetherflar.android.data.Paper
import com.gay.wetherflar.android.data.PaperSource
import com.gay.wetherflar.android.data.S2Paper
import com.gay.wetherflar.android.data.S2Search
import com.gay.wetherflar.android.data.SearchFilters
import com.gay.wetherflar.android.data.SortBy
import com.gay.wetherflar.android.data.SourceId





object SemanticScholarApi : PaperSource {

    override val id = SourceId.SEMANTIC_SCHOLAR
    private const val BASE = "https://api.semanticscholar.org/graph/v1/paper/search"

    override suspend fun search(query: String, filters: SearchFilters, sort: SortBy): List<Paper> {
        val p = StringBuilder("?limit=20")
        p.append("&query=").append(Http.enc(query))
        p.append("&fields=title,authors,year,citationCount,externalIds,abstract,openAccessPdf,venue,url,isOpenAccess")

        if (filters.yearFrom != null || filters.yearTo != null) {
            val from = filters.yearFrom ?: 1
            val to = filters.yearTo ?: 9999
            p.append("&year=$from-$to")
        }

        val body = Http.get(BASE + p)
        val s = Http.gson.fromJson(body, S2Search::class.java)
        val list = s.data.mapNotNull { it.toPaper() }
        
        return if (filters.onlyOpenAccess) list.filter { it.pdfUrl != null || it.openAccess } else list
    }
}

private fun S2Paper.toPaper(): Paper {
    val doi = externalIds?.doi
    val arxiv = externalIds?.arxiv
    val corpus = externalIds?.corpusId
    val cleanDoi = doi?.lowercase()?.removePrefix("https://doi.org/")?.removePrefix("http://doi.org/")?.removePrefix("doi:")
    return Paper(
        id = cleanDoi ?: corpus?.toString() ?: arxiv ?: paperId ?: "",
        title = title ?: "",
        authors = authors.mapNotNull { it.name },
        year = year,
        venue = venue,
        abstractText = abstract,
        doi = doi,
        url = url ?: (paperId?.let { "https://www.semanticscholar.org/paper/$it" }),
        pdfUrl = openAccessPdf?.url,
        citationCount = citationCount,
        sources = setOf(SourceId.SEMANTIC_SCHOLAR),
        openAccess = openAccessPdf?.url != null
    )
}
