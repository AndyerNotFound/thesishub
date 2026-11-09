package com.gay.wetherflar.android.ui

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gay.wetherflar.android.MainActivity
import com.gay.wetherflar.android.data.Paper
import com.gay.wetherflar.android.data.Store
import com.gay.wetherflar.android.util.dp
import com.gay.wetherflar.android.util.tv
import com.gay.wetherflar.android.util.vbox
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class FavoritesFragment : Fragment() {

    private lateinit var list: RecyclerView
    private lateinit var adapter: ResultAdapter
    private lateinit var status: TextView

    override fun onCreateView(inflater: android.view.LayoutInflater, c: ViewGroup?, b: Bundle?): View {
        val ctx = requireContext()
        list = RecyclerView(ctx).apply {
            layoutManager = LinearLayoutManager(ctx)
            adapter = ResultAdapter(
                onOpen = { p -> (activity as? MainActivity)?.openDetail(p) },
                onLongClick = { p -> confirmRemove(p) }
            )
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
        root.setPadding(ctx.dp(12f), ctx.dp(4f), ctx.dp(12f), 0)
        root.addView(status)
        root.addView(list, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        return root
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val favs = Store.favorites()
        adapter.submit(favs)
        status.visibility = if (favs.isEmpty()) View.VISIBLE else View.GONE
        if (favs.isEmpty()) {
            status.text = "暂无收藏\n在论文详情页点「收藏」保存到这里"
        }
    }

    private fun confirmRemove(p: Paper) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("取消收藏")
            .setMessage("确定从收藏中移除「${p.title.take(40)}」吗？")
            .setPositiveButton("移除") { _, _ ->
                Store.removeFavorite(p.id)
                refresh()
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
