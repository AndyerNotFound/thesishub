package com.gay.wetherflar.android.ui

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.gay.wetherflar.android.MainActivity
import com.gay.wetherflar.android.data.Store
import com.gay.wetherflar.android.util.dp
import com.gay.wetherflar.android.util.hbox
import com.gay.wetherflar.android.util.tv
import com.gay.wetherflar.android.util.vbox
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class HistoryFragment : Fragment() {

    private lateinit var container: LinearLayout
    private lateinit var status: TextView

    override fun onCreateView(inflater: android.view.LayoutInflater, c: ViewGroup?, b: Bundle?): View {
        val ctx = requireContext()
        val on = MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorOnSurface, 0)
        val dim = MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorOnSurfaceVariant, 0)

        val title = tv(ctx, "搜索历史", 18f, on, true, 1)
        val clearBtn = MaterialButton(ctx).apply {
            text = "清空"
            textSize = 13f
            isAllCaps = false
            setOnClickListener {
                if (Store.history().isNotEmpty()) {
                    MaterialAlertDialogBuilder(ctx)
                        .setTitle("清空历史")
                        .setMessage("确定清空全部搜索历史吗？")
                        .setPositiveButton("清空") { _, _ ->
                            Store.clearHistory()
                            refresh()
                        }
                        .setNegativeButton("取消", null)
                        .show()
                }
            }
        }
        val header = hbox(ctx, title, clearBtn).apply {
            (getChildAt(1).layoutParams as LinearLayout.LayoutParams).apply {
                width = ViewGroup.LayoutParams.WRAP_CONTENT
            }
            
            (getChildAt(1).layoutParams as LinearLayout.LayoutParams).gravity = Gravity.END
        }

        container = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }

        status = tv(ctx, "", 14f, dim).apply {
            gravity = Gravity.CENTER
            setPadding(0, ctx.dp(24f), 0, ctx.dp(24f))
        }

        val scroll = ScrollView(ctx).apply { addView(container) }

        val root = vbox(ctx)
        root.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        val pad = ctx.dp(16f)
        root.setPadding(pad, ctx.dp(8f), pad, 0)
        root.addView(header)
        root.addView(status)
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        return root
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val ctx = requireContext()
        val hist = Store.history()
        container.removeAllViews()
        if (hist.isEmpty()) {
            status.text = "暂无搜索历史"
            status.visibility = View.VISIBLE
            return
        }
        status.visibility = View.GONE
        val on = MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorOnSurface, 0)
        hist.forEach { q ->
            val item = tv(ctx, q, 15f, on, false, 1).apply {
                setPadding(ctx.dp(4f), ctx.dp(14f), ctx.dp(4f), ctx.dp(14f))
                setOnClickListener {
                    (activity as? MainActivity)?.searchFromHistory(q)
                }
            }
            container.addView(item)
            
            val divider = View(ctx).apply {
                setBackgroundColor(MaterialColors.getColor(
                    ctx, com.google.android.material.R.attr.colorOutlineVariant, 0))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ctx.dp(1f))
            }
            container.addView(divider)
        }
    }
}
