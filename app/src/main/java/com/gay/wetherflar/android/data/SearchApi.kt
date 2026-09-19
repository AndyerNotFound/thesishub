package com.gay.wetherflar.android.data

import com.gay.wetherflar.android.data.sources.ArxivApi
import com.gay.wetherflar.android.data.sources.CrossrefApi
import com.gay.wetherflar.android.data.sources.OpenAlexApi
import com.gay.wetherflar.android.data.sources.PubMedApi
import com.gay.wetherflar.android.data.sources.SemanticScholarApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope


object SearchApi {

    
    private val sourceOrder = listOf(
        SourceId.OPENALEX,
        SourceId.SEMANTIC_SCHOLAR,
        SourceId.ARXIV,
        SourceId.CROSSREF,
        SourceId.PUBMED
    )

    private fun allSources(): List<PaperSource> =
        listOf(OpenAlexApi, SemanticScholarApi, ArxivApi, CrossrefApi, PubMedApi)

    
    private fun activeSources(filters: SearchFilters): List<PaperSource> {
        val selected = if (filters.sources.isEmpty()) SourceId.ALL else filters.sources
        return allSources().filter { src ->
            if (src.id !in selected) return@filter false
            
            if (src.id == SourceId.ARXIV && filters.type != PaperType.ANY && filters.type != PaperType.PREPRINT) {
                return@filter false
            }
            true
        }
    }

    suspend fun search(query: String, filters: SearchFilters, sort: SortBy): List<Paper> {
        if (query.isBlank()) return emptyList()
        val sources = activeSources(filters)

        
        val perSource = coroutineScope {
            sources.map { src ->
                async {
                    try {
                        src.search(query, filters, sort)
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
            }.awaitAll()
        }

        
        val merged = LinkedHashMap<String, Paper>()
        for (list in perSource) {
            for (p in list) {
                val key = p.doiKey ?: ("title:" + p.titleKey)
                if (key.isBlank()) continue
                val existing = merged[key]
                if (existing == null) {
                    merged[key] = p
                } else {
                    merged[key] = merge(existing, p)
                }
            }
        }

        val result = merged.values.toList()
        return when (sort) {
            SortBy.RELEVANCE -> result
            SortBy.YEAR_DESC -> result.sortedWith(compareByDescending<Paper> { it.year ?: -1 }.thenBy { it.titleKey })
            SortBy.CITED_DESC -> result.sortedWith(compareByDescending<Paper> { it.citationCount ?: -1 }.thenBy { it.titleKey })
        }
    }

    
    private fun merge(a: Paper, b: Paper): Paper {
        fun firstNonBlank(vararg xs: String?): String? = xs.firstOrNull { !it.isNullOrBlank() }
        fun longer(x: String?, y: String?): String? =
            if ((x?.length ?: 0) >= (y?.length ?: 0)) x else y
        return Paper(
            id = firstNonBlank(a.id, b.id) ?: "",
            title = longer(a.title, b.title) ?: "",
            authors = (a.authors + b.authors).distinct(),
            year = a.year ?: b.year,
            venue = firstNonBlank(a.venue, b.venue),
            abstractText = firstNonBlank(a.abstractText, b.abstractText) ?: longer(a.abstractText, b.abstractText),
            doi = firstNonBlank(a.doi, b.doi),
            url = firstNonBlank(a.url, b.url),
            pdfUrl = firstNonBlank(a.pdfUrl, b.pdfUrl),
            citationCount = (a.citationCount ?: 0).coerceAtLeast(b.citationCount ?: 0).takeIf { it > 0 } ?: (a.citationCount ?: b.citationCount),
            sources = a.sources + b.sources,
            openAccess = a.openAccess || b.openAccess
        )
    }
}
