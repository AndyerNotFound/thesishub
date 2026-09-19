package com.gay.wetherflar.android.ui

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gay.wetherflar.android.data.Paper
import com.gay.wetherflar.android.data.SourceId
import com.gay.wetherflar.android.util.badge
import com.gay.wetherflar.android.util.dp
import com.gay.wetherflar.android.util.hbox
import com.gay.wetherflar.android.util.rounded
import com.gay.wetherflar.android.util.tv
import com.gay.wetherflar.android.util.vbox
import com.google.android.material.color.MaterialColors


class ResultAdapter(
    private val onOpen: (Paper) -> Unit,
    private val onLongClick: ((Paper) -> Unit)? = null
) : RecyclerView.Adapter<ResultAdapter.VH>() {

    private val items = mutableListOf<Paper>()

    fun submit(list: List<Paper>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    class VH(val root: View) : RecyclerView.ViewHolder(root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ctx = parent.context
        val on = MaterialColors.getColor(parent, com.google.android.material.R.attr.colorOnSurface, 0)
        val dim = MaterialColors.getColor(parent, com.google.android.material.R.attr.colorOnSurfaceVariant, 0)

        val title = tv(ctx, "", 16f, on, true, 3)
        val authors = tv(ctx, "", 13f, dim, false, 2)
        val metaRow = hbox(ctx)

        val body = vbox(ctx, title, authors, metaRow).apply {
            val pad = ctx.dp(14f)
            setPadding(pad, ctx.dp(12f), pad, ctx.dp(12f))
        }
        
        body.background = rounded(ctx.dp(16f), MaterialColors.getColor(
            parent, com.google.android.material.R.attr.colorSurfaceContainerHigh, 0))

        val lp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.setMargins(ctx.dp(12f), ctx.dp(6f), ctx.dp(12f), ctx.dp(6f))
        body.layoutParams = lp

        val holder = VH(body)
        
        body.tag = arrayOf(title, authors, metaRow)
        return holder
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = items[position]
        val ctx = holder.root.context
        val (title, authors, metaRow) = holder.root.tag as Array<*>
        (title as TextView).text = p.title
        (authors as TextView).text = p.authors.joinToString(", ").ifBlank { "未知作者" }

        val row = metaRow as LinearLayout
        row.removeAllViews()
        val dim = MaterialColors.getColor(row, com.google.android.material.R.attr.colorOnSurfaceVariant, 0)
        val onSurf = MaterialColors.getColor(row, com.google.android.material.R.attr.colorOnSurface, 0)

        val metaParts = mutableListOf<String>()
        p.year?.let { metaParts.add(it.toString()) }
        if (!p.venue.isNullOrBlank()) metaParts.add(p.venue)
        if (metaParts.isNotEmpty()) {
            row.addView(tv(ctx, metaParts.joinToString(" · "), 12f, dim, false, 1))
        }

        
        p.sources.sortedBy { SourceId.label(it) }.forEach { src ->
            row.addView(badge(ctx, SourceId.short(src), srcColor(ctx, src), 0xFFFFFFFF.toInt(), 10f).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { marginStart = ctx.dp(6f) }
            })
        }

        
        p.citationCount?.let { c ->
            row.addView(tv(ctx, "被引 $c", 12f, dim, false, 1).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { marginStart = ctx.dp(6f) }
            })
        }

        
        if (p.openAccess || p.pdfUrl != null) {
            row.addView(badge(ctx, "OA", 0xFF2E7D32.toInt(), 0xFFFFFFFF.toInt(), 10f).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { marginStart = ctx.dp(6f) }
            })
        }

        holder.root.setOnClickListener { onOpen(p) }
        holder.root.setOnLongClickListener {
            if (onLongClick != null) {
                onLongClick(p)
                true
            } else false
        }
    }

    override fun getItemCount(): Int = items.size

    private fun srcColor(ctx: android.content.Context, id: String): Int = when (id) {
        SourceId.OPENALEX -> 0xFF1565C0.toInt()      
        SourceId.SEMANTIC_SCHOLAR -> 0xFF6A1B9A.toInt() 
        SourceId.ARXIV -> 0xFFB71C1C.toInt()          
        SourceId.CROSSREF -> 0xFF00838F.toInt()       
        SourceId.PUBMED -> 0xFF2E7D32.toInt()         
        else -> 0xFF616161.toInt()
    }
}
