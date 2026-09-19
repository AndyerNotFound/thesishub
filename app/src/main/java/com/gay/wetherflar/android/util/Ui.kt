package com.gay.wetherflar.android.util

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import com.google.android.material.color.MaterialColors
import com.google.android.material.R as MR


fun Context.dp(v: Float): Int = (v * resources.displayMetrics.density + 0.5f).toInt()


fun Context.dp(v: Int): Int = dp(v.toFloat())


fun View.colorAttr(resId: Int, fallback: Int = 0xFF808080.toInt()): Int =
    MaterialColors.getColor(this, resId, fallback)


fun rounded(radiusPx: Int, fill: Int, stroke: Int = 0, strokePx: Int = 0): GradientDrawable {
    val d = GradientDrawable()
    d.cornerRadius = radiusPx.toFloat()
    d.setColor(fill)
    if (stroke != 0 && strokePx > 0) d.setStroke(strokePx, stroke)
    return d
}


fun tv(
    ctx: Context, text: String = "", sizeSp: Float = 14f,
    color: Int = 0xFF444444.toInt(), bold: Boolean = false,
    maxLines: Int = Int.MAX_VALUE
): TextView = TextView(ctx).apply {
    this.text = text
    textSize = sizeSp
    setTextColor(color)
    if (bold) setTypeface(typeface, Typeface.BOLD)
    if (maxLines != Int.MAX_VALUE) {
        this.maxLines = maxLines
        ellipsize = android.text.TextUtils.TruncateAt.END
    }
}


fun vbox(ctx: Context, vararg views: View): LinearLayout =
    LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        views.forEach { addView(it, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)) }
    }


fun hbox(ctx: Context, vararg views: View): LinearLayout =
    LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        views.forEach { addView(it) }
    }


fun badge(ctx: Context, text: String, fill: Int, textColor: Int, sizeSp: Float = 11f): TextView =
    tv(ctx, text, sizeSp, textColor, true, 1).apply {
        val p = ctx.dp(6f)
        setPadding(p, ctx.dp(2f), p, ctx.dp(2f))
        background = rounded(ctx.dp(10f), fill)
    }


fun TextView.spaceAround() {
    val p = (context as Context).dp(8f)
    setPadding(p, 0, p, 0)
}


fun View.margin(l: Int = 0, t: Int = 0, r: Int = 0, b: Int = 0) {
    val lp = layoutParams as? ViewGroup.MarginLayoutParams ?: return
    lp.setMargins(l, t, r, b)
    layoutParams = lp
}


fun View.circleBg(fill: Int) {
    background = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(fill)
    }
}


fun frame(ctx: Context, child: View? = null): FrameLayout =
    FrameLayout(ctx).apply {
        if (child != null) addView(child, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

fun tintList(color: Int): ColorStateList = ColorStateList.valueOf(color)


fun Context.themeAttrColor(resId: Int, fallback: Int = 0xFF808080.toInt()): Int {
    val tv = TypedValue()
    return if (theme.resolveAttribute(resId, tv, true)) tv.data else fallback
}
