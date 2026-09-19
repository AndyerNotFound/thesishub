package com.gay.wetherflar.android.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.gay.wetherflar.android.MainActivity
import com.gay.wetherflar.android.R
import com.gay.wetherflar.android.data.Citation
import com.gay.wetherflar.android.data.Http
import com.gay.wetherflar.android.data.Paper
import com.gay.wetherflar.android.data.SourceId
import com.gay.wetherflar.android.data.Store
import com.gay.wetherflar.android.data.TranslateApi
import com.gay.wetherflar.android.util.badge
import com.gay.wetherflar.android.util.dp
import com.gay.wetherflar.android.util.hbox
import com.gay.wetherflar.android.util.rounded
import com.gay.wetherflar.android.util.tv
import com.gay.wetherflar.android.util.vbox
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch


class DetailFragment : Fragment() {

    private lateinit var paper: Paper
    private var favButton: MaterialButton? = null

    companion object {
        fun newInstance(paper: Paper): DetailFragment {
            val f = DetailFragment()
            f.arguments = Bundle().apply { putString("paper", Http.gson.toJson(paper)) }
            return f
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val json = arguments?.getString("paper")
        paper = if (json != null) Http.gson.fromJson(json, Paper::class.java) else Paper()
    }

    override fun onCreateView(inflater: android.view.LayoutInflater, c: ViewGroup?, b: Bundle?): View {
        val ctx = requireContext()
        val on = MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorOnSurface, 0)
        val dim = MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorOnSurfaceVariant, 0)
        val surface = MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorSurface, 0)

        
        val back = tv(ctx, "←", 22f, on, true, 1).apply {
            gravity = Gravity.CENTER
            setPadding(0, ctx.dp(4f), ctx.dp(16f), ctx.dp(4f))
            setOnClickListener { (activity as? MainActivity)?.closeSecondary() }
        }
        val barTitle = tv(ctx, "论文详情", 16f, on, true, 1)
        val topBar = hbox(ctx, back, barTitle).apply {
            setPadding(ctx.dp(8f), ctx.dp(8f), ctx.dp(16f), ctx.dp(4f))
        }

        
        val content = vbox(ctx)

        content.addView(tv(ctx, paper.title, 20f, on, true, 6))

        if (paper.authors.isNotEmpty()) {
            content.addView(tv(ctx, paper.authors.joinToString(", "), 14f, dim, false, 10).apply {
                setPadding(0, ctx.dp(8f), 0, 0)
            })
        }

        
        val metaRow = hbox(ctx).apply { setPadding(0, ctx.dp(8f), 0, 0) }
        val metaParts = mutableListOf<String>()
        paper.year?.let { metaParts.add(it.toString()) }
        val venue = paper.venue
        if (!venue.isNullOrBlank()) metaParts.add(venue)
        if (metaParts.isNotEmpty()) {
            metaRow.addView(tv(ctx, metaParts.joinToString(" · "), 13f, dim, false, 3))
        }
        paper.sources.sortedBy { SourceId.label(it) }.forEach { src ->
            metaRow.addView(badge(ctx, SourceId.label(src), srcColor(src), 0xFFFFFFFF.toInt(), 11f).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { marginStart = ctx.dp(6f) }
            })
        }
        paper.citationCount?.let { c ->
            metaRow.addView(badge(ctx, "被引 $c", 0xFF546E7A.toInt(), 0xFFFFFFFF.toInt(), 11f).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { marginStart = ctx.dp(6f) }
            })
        }
        content.addView(metaRow)

        
        content.addView(tv(ctx, "摘要", 15f, on, true, 1).apply {
            setPadding(0, ctx.dp(16f), 0, ctx.dp(4f))
        })
        val abstract = paper.abstractText?.takeIf { it.isNotBlank() } ?: "（该来源未提供摘要）"
        content.addView(tv(ctx, abstract, 14f, on, false, Int.MAX_VALUE).apply {
            setLineSpacing(0f, 1.3f)
        })

        
        val hasEnglish = abstract.any { it in 'a'..'z' || it in 'A'..'Z' }
        if (hasEnglish) {
            val translateResult = tv(ctx, "", 14f, 0xFF1B5E20.toInt(), false, Int.MAX_VALUE).apply {
                setLineSpacing(0f, 1.3f)
                visibility = View.GONE
                setPadding(0, ctx.dp(8f), 0, 0)
            }
            content.addView(translateResult)
            var translateBtn: MaterialButton? = null
            translateBtn = actionButton(ctx, "翻译摘要成中文", primary = false, topMargin = ctx.dp(8f)) {
                if (translateResult.visibility == View.VISIBLE) {
                    translateResult.visibility = View.GONE
                    translateBtn?.text = "翻译摘要成中文"
                } else {
                    translateBtn?.isEnabled = false
                    translateBtn?.text = "翻译中…"
                    viewLifecycleOwner.lifecycleScope.launch {
                        try {
                            val zh = TranslateApi.translate(abstract)
                            translateResult.text = zh
                            translateResult.visibility = View.VISIBLE
                            translateBtn?.text = "收起翻译"
                        } catch (e: Exception) {
                            toast("翻译失败：${e.message?.take(40) ?: "请稍后重试"}")
                            translateBtn?.text = "翻译摘要成中文"
                        } finally {
                            translateBtn?.isEnabled = true
                        }
                    }
                }
            }
            content.addView(translateBtn!!)
        }

        
        if (!paper.doi.isNullOrBlank()) {
            content.addView(tv(ctx, "DOI", 15f, on, true, 1).apply {
                setPadding(0, ctx.dp(16f), 0, ctx.dp(4f))
            })
            val doiTv = tv(ctx, paper.doi ?: "", 14f, dim, false, Int.MAX_VALUE).apply {
                setOnClickListener { copyToClipboard(paper.doi ?: "", "已复制 DOI") }
            }
            content.addView(doiTv)
        }

        
        content.addView(tv(ctx, "操作", 15f, on, true, 1).apply {
            setPadding(0, ctx.dp(20f), 0, ctx.dp(6f))
        })

        
        content.addView(actionButton(ctx, "打开原文", primary = true) {
            val url = paper.url ?: (paper.doi?.let { "https://doi.org/$it" })
            if (url != null) openUrl(url) else toast("无可用链接")
        })

        
        val pdfUrl = paper.pdfUrl
        if (!pdfUrl.isNullOrBlank()) {
            content.addView(actionButton(ctx, "打开 / 下载 PDF", primary = false, topMargin = ctx.dp(8f)) {
                showPdfDialog(pdfUrl)
            })
        }

        
        content.addView(actionButton(ctx, "复制引用（BibTeX / APA）", primary = false, topMargin = ctx.dp(8f)) {
            showCitationDialog()
        })

        
        favButton = actionButton(ctx, favText(), primary = false, topMargin = ctx.dp(8f)) {
            val fav = Store.toggleFavorite(paper)
            favButton?.text = favText()
            toast(if (fav) "已收藏" else "已取消收藏")
        }
        content.addView(favButton!!)

        
        content.addView(actionButton(ctx, "联系发布者", primary = false, topMargin = ctx.dp(8f)) {
            contactPublisher()
        })

        val scroll = ScrollView(ctx).apply { addView(content) }
        val pad = ctx.dp(16f)
        scroll.setPadding(pad, 0, pad, ctx.dp(24f))

        val root = vbox(ctx, topBar, scroll)
        root.setBackgroundColor(surface)
        root.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        return root
    }

    private fun favText(): String = if (Store.isFavorite(paper.id)) "取消收藏" else "收藏"

    private fun actionButton(ctx: Context, text: String, primary: Boolean, topMargin: Int = 0, onClick: () -> Unit): MaterialButton {
        val onPrimary = MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorOnPrimaryContainer, 0)
        val onSecondary = MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorOnSecondaryContainer, 0)
        return MaterialButton(ctx).apply {
            this.text = text
            textSize = 14f
            isAllCaps = false
            gravity = Gravity.CENTER
            if (primary) {
                setBackgroundColor(MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorPrimary, 0))
                setTextColor(onPrimary)
            } else {
                setBackgroundColor(MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorSurfaceContainerHigh, 0))
                setTextColor(onSecondary)
            }
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { this.topMargin = topMargin }
        }
    }

    private fun showCitationDialog() {
        val ctx = requireContext()
        val items = arrayOf("BibTeX", "APA")
        MaterialAlertDialogBuilder(ctx)
            .setTitle("复制引用")
            .setItems(items) { _, which ->
                val text = if (which == 0) Citation.bibtex(paper) else Citation.apa(paper)
                copyToClipboard(text, "已复制 ${items[which]} 引用")
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showPdfDialog(pdfUrl: String) {
        val ctx = requireContext()
        val items = arrayOf("打开 PDF", "下载 PDF")
        MaterialAlertDialogBuilder(ctx)
            .setTitle("PDF")
            .setItems(items) { _, which ->
                if (which == 0) openUrl(pdfUrl) else downloadPdf(pdfUrl)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            toast("无法打开链接")
        }
    }

    private fun downloadPdf(url: String) {
        try {
            val req = android.app.DownloadManager.Request(Uri.parse(url))
                .setTitle(paper.title.take(80) + ".pdf")
                .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setMimeType("application/pdf")
            val dm = requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
            dm.enqueue(req)
            toast("已开始下载 PDF")
        } catch (e: Exception) {
            toast("下载失败：${e.message}")
        }
    }

    private fun contactPublisher() {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_SUBJECT, "关于论文：${paper.title}")
                putExtra(Intent.EXTRA_TEXT,
                    "论文：${paper.title}\n" +
                    "作者：${paper.authors.joinToString(", ")}\n" +
                    "DOI：${paper.doi ?: "无"}\n" +
                    "链接：${paper.url ?: ""}\n\n")
            }
            startActivity(Intent.createChooser(intent, "联系发布者"))
        } catch (e: Exception) {
            toast("未找到邮箱应用")
        }
    }

    private fun copyToClipboard(text: String, tip: String) {
        val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("citation", text))
        toast(tip)
    }

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }

    private fun srcColor(id: String): Int = when (id) {
        SourceId.OPENALEX -> 0xFF1565C0.toInt()
        SourceId.SEMANTIC_SCHOLAR -> 0xFF6A1B9A.toInt()
        SourceId.ARXIV -> 0xFFB71C1C.toInt()
        SourceId.CROSSREF -> 0xFF00838F.toInt()
        SourceId.PUBMED -> 0xFF2E7D32.toInt()
        else -> 0xFF616161.toInt()
    }
}
