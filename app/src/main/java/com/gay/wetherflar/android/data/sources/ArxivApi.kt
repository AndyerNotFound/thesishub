package com.gay.wetherflar.android.data.sources

import com.gay.wetherflar.android.data.Http
import com.gay.wetherflar.android.data.Paper
import com.gay.wetherflar.android.data.PaperSource
import com.gay.wetherflar.android.data.SearchFilters
import com.gay.wetherflar.android.data.SortBy
import com.gay.wetherflar.android.data.SourceId
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory





object ArxivApi : PaperSource {

    override val id = SourceId.ARXIV
    private const val BASE = "https://export.arxiv.org/api/query"

    override suspend fun search(query: String, filters: SearchFilters, sort: SortBy): List<Paper> {
        val sortParam = when (sort) {
            SortBy.YEAR_DESC -> "submittedDate"
            else -> "relevance"
        }
        val p = StringBuilder()
            .append("?search_query=all:").append(Http.enc("\"$query\""))
            .append("&start=0&max_results=20")
            .append("&sortBy=").append(sortParam)
            .append("&sortOrder=descending")

        val xml = Http.get(BASE + p)
        val list = parse(xml)
        
        val yf = filters.yearFrom
        val yt = filters.yearTo
        if (yf == null && yt == null) return list
        return list.filter { pp ->
            val y = pp.year ?: return@filter false
            (yf == null || y >= yf) && (yt == null || y <= yt)
        }
    }

    private fun parse(xml: String): List<Paper> {
        val papers = mutableListOf<Paper>()
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        
        runCatching { factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        val doc = factory.newDocumentBuilder().parse(InputSource(StringReader(xml)))
        val entries = doc.getElementsByTagNameNS("*", "entry")
        for (i in 0 until entries.length) {
            val e = entries.item(i) as Element
            val title = firstText(e, "title")
            val summary = firstText(e, "summary")?.replace(Regex("\\s+"), " ")?.trim()
            val published = firstText(e, "published")
            val doi = firstText(e, "doi")

            val authors = mutableListOf<String>()
            val authorNodes = e.getElementsByTagNameNS("*", "author")
            for (j in 0 until authorNodes.length) {
                val name = firstText(authorNodes.item(j) as Element, "name")
                if (!name.isNullOrBlank()) authors.add(name)
            }

            var absUrl: String? = null
            var pdfUrl: String? = null
            val linkNodes = e.getElementsByTagNameNS("*", "link")
            for (j in 0 until linkNodes.length) {
                val l = linkNodes.item(j) as Element
                val rel = l.getAttribute("rel")
                val href = l.getAttribute("href")
                val t = l.getAttribute("title")
                if (rel == "alternate") absUrl = href
                if (t == "pdf" || (href != null && href.contains("/pdf/"))) pdfUrl = href
            }
            val pdf = pdfUrl ?: absUrl?.let { it.replace("arxiv.org/abs/", "arxiv.org/pdf/") }
            val cleanDoi = doi?.lowercase()?.removePrefix("https://doi.org/")?.removePrefix("http://doi.org/")
            papers.add(
                Paper(
                    id = cleanDoi ?: absUrl ?: "",
                    title = title ?: "",
                    authors = authors,
                    year = published?.take(4)?.toIntOrNull(),
                    venue = "arXiv",
                    abstractText = summary,
                    doi = doi,
                    url = absUrl,
                    pdfUrl = pdf,
                    citationCount = null,
                    sources = setOf(SourceId.ARXIV),
                    openAccess = true
                )
            )
        }
        return papers.filter { it.title.isNotBlank() }
    }

    private fun firstText(e: Element, tag: String): String? {
        val nodes = e.getElementsByTagNameNS("*", tag)
        return if (nodes.length > 0) nodes.item(0).textContent?.trim() else null
    }
}
