package com.gay.wetherflar.android.data


enum class SortBy(val label: String) {
    RELEVANCE("相关度"),
    YEAR_DESC("最新"),
    CITED_DESC("引用最多")
}


enum class PaperType(val label: String) {
    ANY("全部类型"),
    ARTICLE("期刊文章"),
    REVIEW("综述"),
    CONFERENCE("会议论文"),
    PREPRINT("预印本"),
    BOOK("书籍")
}


data class SearchFilters(
    val yearFrom: Int? = null,
    val yearTo: Int? = null,
    val sources: Set<String> = emptySet(),   
    val onlyOpenAccess: Boolean = false,
    val type: PaperType = PaperType.ANY
)


object SourceId {
    const val OPENALEX = "openalex"
    const val SEMANTIC_SCHOLAR = "s2"
    const val ARXIV = "arxiv"
    const val CROSSREF = "crossref"
    const val PUBMED = "pubmed"

    val ALL = setOf(OPENALEX, SEMANTIC_SCHOLAR, ARXIV, CROSSREF, PUBMED)

    fun label(id: String): String = when (id) {
        OPENALEX -> "OpenAlex"
        SEMANTIC_SCHOLAR -> "SemanticScholar"
        ARXIV -> "arXiv"
        CROSSREF -> "Crossref"
        PUBMED -> "PubMed"
        else -> id
    }

    
    fun short(id: String): String = when (id) {
        OPENALEX -> "OA"
        SEMANTIC_SCHOLAR -> "S2"
        ARXIV -> "arXiv"
        CROSSREF -> "CR"
        PUBMED -> "PM"
        else -> id
    }
}


interface PaperSource {
    val id: String
    suspend fun search(query: String, filters: SearchFilters, sort: SortBy): List<Paper>
}
