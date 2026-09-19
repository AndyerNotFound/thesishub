package com.gay.wetherflar.android.data.sources

import com.gay.wetherflar.android.data.Http
import com.gay.wetherflar.android.data.OpenAlexWork
import com.gay.wetherflar.android.data.OpenAlexWorks
import com.gay.wetherflar.android.data.Paper
import com.gay.wetherflar.android.data.PaperSource
import com.gay.wetherflar.android.data.PaperType
import com.gay.wetherflar.android.data.SearchFilters
import com.gay.wetherflar.android.data.SortBy
import com.gay.wetherflar.android.data.SourceId
import com.gay.wetherflar.android.data.rebuildAbstract


object OpenAlexApi : PaperSource {

    override val id = SourceId.OPENALEX
    private const val BASE = "https://api.openalex.org/works"

    private fun typeOf(t: PaperType): String = when (t) {
        PaperType.ARTICLE -> "article"
        PaperType.REVIEW -> "review"
        PaperType.CONFERENCE -> "proceedings-article"
        PaperType.PREPRINT -> "preprint"
        PaperType.BOOK -> "book"
        PaperType.ANY -> ""
    }

    override suspend fun search(query: String, filters: SearchFilters, sort: SortBy): List<Paper> {
        val p = StringBuilder("?per-page=40")
        p.append("&search=").append(Http.enc(query))

        p.append("&sort=").append(
            when (sort) {
                SortBy.YEAR_DESC -> "publication_date:desc"
                SortBy.CITED_DESC -> "cited_by_count:desc"
                else -> "relevance_score:desc"
            }
        )

        val fs = mutableListOf<String>()
        if (filters.yearFrom != null || filters.yearTo != null) {
            val from = filters.yearFrom ?: 1
            val to = filters.yearTo ?: 9999
            fs.add("publication_year:$from-$to")
        }
        val t = typeOf(filters.type)
        if (t.isNotEmpty()) fs.add("type:$t")
        if (filters.onlyOpenAccess) fs.add("is_oa:true")
        if (fs.isNotEmpty()) p.append("&filter=").append(Http.enc(fs.joinToString(",")))

        p.append("&select=id,doi,title,display_name,publication_year,publication_date,type,cited_by_count,primary_location,open_access,authorships,abstract_inverted_index")

        val body = Http.get(BASE + p)
        val works = Http.gson.fromJson(body, OpenAlexWorks::class.java)
        return works.results.mapNotNull { it.toPaper() }
    }
}

private fun OpenAlexWork.toPaper(): Paper {
    val doi = this.doi
    val authors = authorships.mapNotNull { a -> a.author?.displayName ?: a.rawAuthorName }.distinct()
    val venue = primaryLocation?.source?.displayName
    val pdf = primaryLocation?.pdfUrl ?: openAccess?.oaUrl
    val cleanDoi = doi?.lowercase()?.removePrefix("https://doi.org/")?.removePrefix("http://doi.org/")
    return Paper(
        id = cleanDoi ?: (id ?: ""),
        title = title ?: displayName ?: "",
        authors = authors,
        year = year,
        venue = venue,
        abstractText = rebuildAbstract(abstractInvertedIndex),
        doi = doi,
        url = primaryLocation?.landingPageUrl ?: doi,
        pdfUrl = pdf,
        citationCount = citedByCount,
        sources = setOf(SourceId.OPENALEX),
        openAccess = openAccess?.isOa == true
    )
}
