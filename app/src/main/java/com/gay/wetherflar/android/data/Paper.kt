package com.gay.wetherflar.android.data

import com.google.gson.annotations.SerializedName






data class Paper(
    val id: String = "",
    val title: String = "",
    val authors: List<String> = emptyList(),
    val year: Int? = null,
    val venue: String? = null,
    val abstractText: String? = null,
    val doi: String? = null,
    val url: String? = null,
    val pdfUrl: String? = null,
    val citationCount: Int? = null,
    val sources: Set<String> = emptySet(),
    val openAccess: Boolean = false
) {
    
    val titleKey: String
        get() = title.lowercase()
            .replace(Regex("[^a-z0-9\\u4e00-\\u9fff]"), "")
            .trim()

    
    val doiKey: String?
        get() = doi?.lowercase()?.removePrefix("https://doi.org/")?.removePrefix("http://doi.org/")?.removePrefix("doi:")
}





data class OpenAlexWorks(
    val meta: OpenAlexMeta = OpenAlexMeta(),
    val results: List<OpenAlexWork> = emptyList()
)

data class OpenAlexMeta(val count: Long = 0)

data class OpenAlexWork(
    val id: String? = null,
    val doi: String? = null,
    val title: String? = null,
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("publication_year") val year: Int? = null,
    @SerializedName("publication_date") val pubDate: String? = null,
    val type: String? = null,
    @SerializedName("cited_by_count") val citedByCount: Int? = null,
    @SerializedName("primary_location") val primaryLocation: OpenAlexLocation? = null,
    @SerializedName("open_access") val openAccess: OpenAlexOA? = null,
    val authorships: List<OpenAlexAuthorship> = emptyList(),
    @SerializedName("abstract_inverted_index") val abstractInvertedIndex: Map<String, List<Int>>? = null,
    val concepts: List<OpenAlexConcept> = emptyList()
)

data class OpenAlexLocation(
    @SerializedName("landing_page_url") val landingPageUrl: String? = null,
    @SerializedName("pdf_url") val pdfUrl: String? = null,
    val source: OpenAlexSource? = null
)

data class OpenAlexSource(
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("issn_l") val issnL: String? = null,
    val type: String? = null
)

data class OpenAlexOA(
    @SerializedName("is_oa") val isOa: Boolean = false,
    @SerializedName("oa_status") val oaStatus: String? = null,
    @SerializedName("oa_url") val oaUrl: String? = null
)

data class OpenAlexAuthorship(
    @SerializedName("author_position") val authorPosition: String? = null,
    val author: OpenAlexAuthor? = null,
    @SerializedName("is_corresponding") val isCorresponding: Boolean = false,
    val institutions: List<OpenAlexInstitution> = emptyList(),
    @SerializedName("raw_author_name") val rawAuthorName: String? = null
)

data class OpenAlexAuthor(
    @SerializedName("display_name") val displayName: String? = null,
    val orcid: String? = null
)

data class OpenAlexInstitution(
    @SerializedName("display_name") val displayName: String? = null
)

data class OpenAlexConcept(
    @SerializedName("display_name") val displayName: String? = null
)


fun rebuildAbstract(inverted: Map<String, List<Int>>?): String? {
    if (inverted.isNullOrEmpty()) return null
    
    val pairs = mutableListOf<Pair<Int, String>>()
    for ((word, positions) in inverted) {
        for (p in positions) pairs.add(p to word)
    }
    pairs.sortBy { it.first }
    return pairs.joinToString(" ") { it.second }
}



data class S2Search(
    val total: Long = 0,
    val data: List<S2Paper> = emptyList()
)

data class S2Paper(
    val paperId: String? = null,
    val title: String? = null,
    val abstract: String? = null,
    val venue: String? = null,
    val year: Int? = null,
    val citationCount: Int? = null,
    val url: String? = null,
    val externalIds: S2ExternalIds? = null,
    val openAccessPdf: S2Pdf? = null,
    val authors: List<S2Author> = emptyList()
)

data class S2ExternalIds(
    @SerializedName("DOI") val doi: String? = null,
    @SerializedName("ArXiv") val arxiv: String? = null,
    @SerializedName("CorpusId") val corpusId: Long? = null,
    @SerializedName("PubMed") val pubmed: String? = null
)

data class S2Pdf(val url: String? = null, val status: String? = null)

data class S2Author(val authorId: String? = null, val name: String? = null)



data class CrossrefWorks(
    val message: CrossrefMessage = CrossrefMessage()
)

data class CrossrefMessage(
    @SerializedName("total-results") val totalResults: Long = 0,
    val items: List<CrossrefItem> = emptyList()
)

data class CrossrefItem(
    @SerializedName("DOI") val doi: String? = null,
    val type: String? = null,
    val title: List<String> = emptyList(),
    val author: List<CrossrefAuthor> = emptyList(),
    @SerializedName("container-title") val containerTitle: List<String> = emptyList(),
    @SerializedName("published-print") val publishedPrint: CrossrefDate? = null,
    @SerializedName("published-online") val publishedOnline: CrossrefDate? = null,
    @SerializedName("is-referenced-by-count") val citedByCount: Int? = null,
    val abstract: String? = null,
    val resource: CrossrefResource? = null,
    val link: List<CrossrefLink> = emptyList()
)

data class CrossrefAuthor(
    val given: String? = null,
    val family: String? = null,
    val affiliation: List<Map<String, String>> = emptyList()
)

data class CrossrefDate(
    @SerializedName("date-parts") val dateParts: List<List<Int>> = emptyList()
)

data class CrossrefResource(val primary: CrossrefPrimary? = null)

data class CrossrefPrimary(
    @SerializedName("URL") val url: String? = null
)

data class CrossrefLink(
    @SerializedName("URL") val url: String? = null,
    @SerializedName("content-type") val contentType: String? = null
)



data class PubMedSearch(
    val esearchresult: PubMedSearchResult = PubMedSearchResult()
)

data class PubMedSearchResult(
    val count: String? = null,
    val idlist: List<String> = emptyList()
)

data class PubMedSummary(
    val result: PubMedSummaryResult = PubMedSummaryResult()
)

data class PubMedSummaryResult(
    val uids: List<String> = emptyList(),
    
)

data class PubMedDoc(
    val uid: String? = null,
    val title: String? = null,
    @SerializedName("sortfirstauthor") val sortFirstAuthor: String? = null,
    val authors: List<PubMedAuthor> = emptyList(),
    val pubdate: String? = null,
    val epubdate: String? = null,
    val source: String? = null,
    val articleids: List<PubMedArticleId> = emptyList()
)

data class PubMedAuthor(val name: String? = null)

data class PubMedArticleId(
    val idtype: String? = null,
    val value: String? = null
)
