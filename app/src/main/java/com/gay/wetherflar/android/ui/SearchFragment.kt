package com.gay.wetherflar.android.ui

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gay.wetherflar.android.MainActivity
import com.gay.wetherflar.android.R
import com.gay.wetherflar.android.data.Paper
import com.gay.wetherflar.android.data.PaperType
import com.gay.wetherflar.android.data.SearchApi
import com.gay.wetherflar.android.data.SearchFilters
import com.gay.wetherflar.android.data.SortBy
import com.gay.wetherflar.android.data.SourceId
import com.gay.wetherflar.android.data.Store
import com.gay.wetherflar.android.util.dp
import com.gay.wetherflar.android.util.hbox
import com.gay.wetherflar.android.util.rounded
import com.gay.wetherflar.android.util.tv
import com.gay.wetherflar.android.util.vbox
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch


class SearchFragment : Fragment() {

    private lateinit var searchBox: EditText
    private lateinit var sortGroup: ChipGroup
    private lateinit var filterChip: Chip
    private lateinit var list: RecyclerView
    private lateinit var status: TextView
    private lateinit var adapter: ResultAdapter

    private val items = mutableListOf<Paper>()
    private var sort = SortBy.RELEVANCE
    private var filters = SearchFilters()
    private var lastQuery = ""
    private var searchJob: Job? = null

    override fun onCreateView(inflater: android.view.LayoutInflater, c: ViewGroup?, b: Bundle?): View {
        val ctx = requireContext()

        searchBox = EditText(ctx).apply {
            hint = "搜索论文（标题 / 关键词 / DOI）"
            setTextSize(15f)
            maxLines = 1
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            setSingleLine(true)
            val p = ctx.dp(14f)
            setPadding(p, ctx.dp(12f), p, ctx.dp(12f))
            background = rounded(ctx.dp(24f), MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorSurfaceContainerHigh, 0))
            AppCompatResources.getDrawable(ctx, R.drawable.ic_search)?.let { icon ->
                icon.setBounds(0, 0, ctx.dp(18f), ctx.dp(18f))
                setCompoundDrawables(icon, null, null, null)
            }
            compoundDrawablePadding = ctx.dp(8f)
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    doSearch(text.toString())
                    true
                } else false
            }
        }

        
        sortGroup = ChipGroup(ctx).apply {
            isSingleSelection = true
            isSingleLine = true
            chipSpacingHorizontal = ctx.dp(8f)
        }
        rebuildSortChips()

        
        filterChip = Chip(ctx).apply {
            text = filterLabel()
            isCheckable = true
            isChecked = hasActiveFilters()
            chipIcon = AppCompatResources.getDrawable(ctx, R.drawable.ic_filter)
            chipIconSize = ctx.dp(16f).toFloat()
            setOnClickListener { showFilterDialog() }
        }

        
        val chipRow = HorizontalScrollView(ctx).apply {
            isHorizontalScrollBarEnabled = false
            addView(hbox(ctx, sortGroup, filterChip))
        }

        list = RecyclerView(ctx).apply {
            layoutManager = LinearLayoutManager(ctx)
            adapter = ResultAdapter(onOpen = { p -> (activity as? MainActivity)?.openDetail(p) })
            clipToPadding = false
            setPadding(0, 0, 0, ctx.dp(12f))
        }
        adapter = list.adapter as ResultAdapter

        status = tv(ctx, "", 14f, MaterialColors.getColor(
            list, com.google.android.material.R.attr.colorOnSurfaceVariant, 0)).apply {
            gravity = Gravity.CENTER
            setPadding(0, ctx.dp(24f), 0, ctx.dp(24f))
        }

        val root = vbox(ctx)
        root.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        val pad = ctx.dp(12f)
        root.setPadding(pad, ctx.dp(6f), pad, 0)
        root.addView(searchBox, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(chipRow, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = ctx.dp(8f)
        })
        root.addView(status, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(list, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        status.text = "输入关键词搜索，聚合 OpenAlex / SemanticScholar / arXiv / Crossref / PubMed"
    }

    private fun rebuildSortChips() {
        sortGroup.removeAllViews()
        val ctx = requireContext()
        SortBy.values().forEach { s ->
            sortGroup.addView(Chip(ctx).apply {
                text = s.label
                isCheckable = true
                isChecked = sort == s
                setOnClickListener {
                    if (sort != s) {
                        sort = s
                        rebuildSortChips()
                        if (lastQuery.isNotBlank()) doSearch(lastQuery)
                    }
                }
            })
        }
    }

    private fun filterLabel(): String {
        val n = activeFilterCount()
        return if (n > 0) "筛选 ($n)" else "筛选"
    }

    private fun activeFilterCount(): Int {
        var n = 0
        if (filters.type != PaperType.ANY) n++
        if (filters.sources.isNotEmpty()) n++
        if (filters.yearFrom != null || filters.yearTo != null) n++
        if (filters.onlyOpenAccess) n++
        return n
    }

    private fun hasActiveFilters(): Boolean = activeFilterCount() > 0

    
    fun searchFor(q: String) {
        searchBox.setText(q)
        searchBox.setSelection(q.length)
        doSearch(q)
    }

    private fun doSearch(q: String) {
        val query = q.trim()
        if (query.isEmpty()) return
        lastQuery = query
        Store.addHistory(query)
        searchJob?.cancel()
        status.text = "搜索中…"
        status.visibility = View.VISIBLE
        searchJob = lifecycleScope.launch {
            val result = SearchApi.search(query, filters, sort)
            items.clear()
            items.addAll(result)
            adapter.submit(result)
            status.visibility = if (result.isEmpty()) View.VISIBLE else View.GONE
            if (result.isEmpty()) {
                status.text = "没有找到结果\n（可尝试换关键词或放宽筛选）"
            }
        }
    }

    private fun showFilterDialog() {
        val ctx = requireContext()
        val cur = filters
        val scroll = ScrollView(ctx)
        val box = vbox(ctx)

        
        box.addView(sectionTitle(ctx, "类型"))
        val typeGroup = ChipGroup(ctx).apply {
            chipSpacingHorizontal = ctx.dp(6f)
            isSingleSelection = true
        }
        val typeIndex = PaperType.values().indexOf(filters.type)
        PaperType.values().forEachIndexed { i, t ->
            typeGroup.addView(Chip(ctx).apply {
                text = t.label
                isCheckable = true
                isChecked = i == typeIndex
            })
        }
        box.addView(typeGroup)

        
        box.addView(sectionTitle(ctx, "来源（全不选 = 全部）"))
        val srcGroup = ChipGroup(ctx).apply { chipSpacingHorizontal = ctx.dp(6f) }
        val srcChips = mutableMapOf<String, Chip>()
        SourceId.ALL.forEach { id ->
            val chip = Chip(ctx).apply {
                text = SourceId.label(id)
                isCheckable = true
                isChecked = cur.sources.isEmpty() || cur.sources.contains(id)
            }
            srcChips[id] = chip
            srcGroup.addView(chip)
        }
        box.addView(srcGroup)

        
        box.addView(sectionTitle(ctx, "年份范围（留空 = 不限）"))
        val yearFrom = TextInputEditText(ctx).apply {
            hint = "起始年，如 2020"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(cur.yearFrom?.toString() ?: "")
        }
        val yearTo = TextInputEditText(ctx).apply {
            hint = "结束年，如 2024"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(cur.yearTo?.toString() ?: "")
        }
        val yearRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        yearRow.addView(yearFrom, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        yearRow.addView(yearTo, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = ctx.dp(8f)
        })
        box.addView(yearRow)

        
        box.addView(sectionTitle(ctx, "仅开放获取"))
        val oaSwitch = androidx.appcompat.widget.SwitchCompat(ctx).apply {
            isChecked = cur.onlyOpenAccess
        }
        box.addView(oaSwitch)

        scroll.addView(box)
        val pad = ctx.dp(20f)
        scroll.setPadding(pad, pad / 2, pad, 0)

        MaterialAlertDialogBuilder(ctx)
            .setTitle("筛选")
            .setView(scroll)
            .setNegativeButton("取消", null)
            .setNeutralButton("重置") { _, _ ->
                filters = SearchFilters()
                filterChip.text = filterLabel()
                filterChip.isChecked = false
                if (lastQuery.isNotBlank()) doSearch(lastQuery)
            }
            .setPositiveButton("应用") { _, _ ->
                var selType = PaperType.ANY
                PaperType.values().forEachIndexed { i, t ->
                    val chip = typeGroup.getChildAt(i) as? Chip
                    if (chip != null && chip.isChecked) selType = t
                }
                val selSources = srcChips.filterValues { it.isChecked }.keys.toSet()
                val yf = yearFrom.text?.toString()?.trim()?.toIntOrNull()
                val yt = yearTo.text?.toString()?.trim()?.toIntOrNull()
                filters = SearchFilters(
                    yearFrom = yf, yearTo = yt,
                    sources = selSources,
                    onlyOpenAccess = oaSwitch.isChecked,
                    type = selType
                )
                filterChip.text = filterLabel()
                filterChip.isChecked = hasActiveFilters()
                if (lastQuery.isNotBlank()) doSearch(lastQuery)
            }
            .show()
    }

    private fun sectionTitle(ctx: android.content.Context, t: String): TextView =
        tv(ctx, t, 13f, MaterialColors.getColor(
            ctx, com.google.android.material.R.attr.colorPrimary, 0xFF6750A4.toInt()), true, 1).apply {
            setPadding(0, ctx.dp(12f), 0, ctx.dp(4f))
        }
}
