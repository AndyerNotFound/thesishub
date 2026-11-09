package com.gay.wetherflar.android.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.ParcelFileDescriptor
import android.util.LruCache
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.gay.wetherflar.android.data.Http
import com.gay.wetherflar.android.util.dp
import com.gay.wetherflar.android.util.hbox
import com.gay.wetherflar.android.util.tv
import com.gay.wetherflar.android.util.vbox
import androidx.lifecycle.lifecycleScope
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.security.MessageDigest


class PdfViewerActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_URL = "url"
        private const val EXTRA_TITLE = "title"

        fun start(ctx: Context, url: String, title: String) {
            ctx.startActivity(Intent(ctx, PdfViewerActivity::class.java)
                .putExtra(EXTRA_URL, url)
                .putExtra(EXTRA_TITLE, title))
        }
    }

    private var renderer: PdfRenderer? = null
    private var pfd: ParcelFileDescriptor? = null
    private var renderThread: HandlerThread? = null
    private var renderHandler: Handler? = null

    private lateinit var pager: ViewPager2
    private lateinit var pageInfo: TextView
    private var loadingView: View? = null

    private val pageCache: LruCache<Int, Bitmap> = object :
        LruCache<Int, Bitmap>((Runtime.getRuntime().maxMemory() / 1024 / 1024 / 8).toInt()) {
        override fun sizeOf(key: Int, value: Bitmap): Int =
            (value.byteCount / 1024 / 1024).coerceAtLeast(1)
    }

    private fun cacheGet(pos: Int): Bitmap? = synchronized(pageCache) { pageCache.get(pos) }

    private fun cachePut(pos: Int, bmp: Bitmap) = synchronized(pageCache) { pageCache.put(pos, bmp) }

    private val pageAdapter = object : RecyclerView.Adapter<PageHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder {
            val v = ZoomImageView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            }
            return PageHolder(v)
        }

        override fun getItemCount(): Int = renderer?.pageCount ?: 0

        override fun onBindViewHolder(holder: PageHolder, position: Int) {
            renderPage(holder.view, position)
        }

        override fun onViewRecycled(holder: PageHolder) {
            holder.view.renderToken = -1
        }
    }

    private class PageHolder(val view: ZoomImageView) : RecyclerView.ViewHolder(view)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = intent.getStringExtra(EXTRA_URL)
        if (url.isNullOrBlank()) {
            finish()
            return
        }
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "论文 PDF"

        val ctx = this
        val on = MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorOnSurface, 0)
        val dim = MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorOnSurfaceVariant, 0)
        val surface = MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorSurface, 0)

        val back = tv(ctx, "←", 22f, on, true, 1).apply {
            gravity = Gravity.CENTER
            setPadding(0, ctx.dp(4f), ctx.dp(12f), ctx.dp(4f))
            setOnClickListener { finish() }
        }
        pageInfo = tv(ctx, "", 13f, dim, false, 1).apply {
            gravity = Gravity.END
        }
        val titleTv = tv(ctx, title, 15f, on, true, 1).apply {
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        val topBar = hbox(ctx, back, titleTv, pageInfo).apply {
            setPadding(ctx.dp(8f), ctx.dp(8f), ctx.dp(16f), ctx.dp(4f))
            (getChildAt(1).layoutParams as LinearLayout.LayoutParams).weight = 1f
        }

        pager = ViewPager2(ctx).apply {
            adapter = this@PdfViewerActivity.pageAdapter
            offscreenPageLimit = 1
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    pageInfo.text = "${position + 1} / ${renderer?.pageCount ?: 0}"
                }
            })
        }

        loadingView = vbox(ctx).apply {
            gravity = Gravity.CENTER
            val pb = ProgressBar(ctx)
            addView(pb)
            addView(tv(ctx, "正在下载 PDF…", 14f, dim).apply {
                setPadding(0, ctx.dp(12f), 0, 0)
            })
        }

        val content = FrameLayout(ctx).apply {
            addView(pager, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            addView(loadingView!!, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT).apply {
                gravity = Gravity.CENTER
            })
        }

        val root = vbox(ctx)
        root.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        root.setBackgroundColor(surface)
        root.addView(topBar)
        root.addView(content, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)

        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            v.setPadding(0, top, 0, 0)
            insets
        }

        renderThread = HandlerThread("pdf-render").apply { start() }
        renderHandler = Handler(renderThread!!.looper)

        downloadAndOpen(url)
    }

    private fun downloadAndOpen(url: String) {
        lifecycleScope.launch {
            val file = try {
                withContext(Dispatchers.IO) { downloadToCache(url) }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                if (isFinishing || isDestroyed) return@launch
                toast("PDF 下载失败：${e.message?.take(40) ?: "请稍后重试"}")
                finish()
                return@launch
            }
            val r = try {
                pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                PdfRenderer(pfd!!)
            } catch (e: Exception) {
                if (isFinishing || isDestroyed) return@launch
                toast("无法打开 PDF：${e.message?.take(40) ?: "文件可能已损坏"}")
                finish()
                return@launch
            }
            renderer = r
            loadingView?.visibility = View.GONE
            pageAdapter.notifyDataSetChanged()
            if (r.pageCount > 0) pageInfo.text = "1 / ${r.pageCount}"
        }
    }

    private suspend fun downloadToCache(url: String): File {
        val dir = File(cacheDir, "pdfs").apply { mkdirs() }
        val name = "pdf_" + sha1(url).take(12) + ".pdf"
        val f = File(dir, name)
        if (f.exists() && f.length() > 0) return f
        val tmp = File(dir, name + ".tmp")
        Http.download(url, tmp)
        if (tmp.length() < 5 || !tmp.startsWithPdfHeader()) {
            tmp.delete()
            throw IOException("不是有效的 PDF 文件")
        }
        tmp.renameTo(f)
        return f
    }

    private fun renderPage(view: ZoomImageView, position: Int) {
        val cached = cacheGet(position)
        if (cached != null) {
            view.renderToken = position
            view.setPageBitmap(cached)
            return
        }
        val r = renderer ?: return
        val h = renderHandler ?: return
        view.renderToken = position
        h.post {
            val page = try { r.openPage(position) } catch (e: Exception) { null } ?: return@post
            val w = renderWidth()
            var hh = (page.height.toFloat() / page.width.toFloat() * w).toInt().coerceAtLeast(1)
            hh = hh.coerceAtMost(4096)
            var bmp: Bitmap? = null
            try {
                bmp = Bitmap.createBitmap(w, hh, Bitmap.Config.ARGB_8888)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            } catch (e: Throwable) {
                bmp = null
                val w2 = (w / 2).coerceAtLeast(512)
                val h2 = (hh / 2).coerceAtLeast(1)
                bmp = try {
                    Bitmap.createBitmap(w2, h2, Bitmap.Config.ARGB_8888).also {
                        page.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    }
                } catch (e2: Throwable) {
                    null
                }
            } finally {
                page.close()
            }
            if (bmp != null) {
                cachePut(position, bmp)
                runOnUiThread {
                    if (view.renderToken == position) view.setPageBitmap(bmp)
                }
            }
        }
    }

    private fun renderWidth(): Int {
        val screen = resources.displayMetrics.widthPixels
        return (screen * 1.6f).toInt().coerceIn(1024, 2048)
    }

    private fun sha1(s: String): String =
        MessageDigest.getInstance("SHA-1").digest(s.toByteArray())
            .joinToString("") { "%02x".format(it) }

    private fun File.startsWithPdfHeader(): Boolean {
        if (length() < 5) return false
        val head = ByteArray(4)
        inputStream().use { it.read(head) }
        return head[0] == '%'.code.toByte() && head[1] == 'P'.code.toByte() &&
            head[2] == 'D'.code.toByte() && head[3] == 'F'.code.toByte()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        val r = renderer
        val f = pfd
        renderer = null
        pfd = null
        val h = renderHandler
        if (r != null && h != null) {
            h.post {
                runCatching { r.close() }
                runCatching { f?.close() }
            }
        } else {
            runCatching { r?.close() }
            runCatching { f?.close() }
        }
        renderThread?.quitSafely()
        renderThread?.join(500)
        super.onDestroy()
    }
}


class ZoomImageView(ctx: Context) : AppCompatImageView(ctx) {

    private val baseMatrix = Matrix()
    private val drawMatrix = Matrix()
    private val down = PointF()
    private var maxScaleRel = 5f
    private var fitScale = 1f
    internal var renderToken = -1
    private var fitPending = false

    private val scaleDetector = ScaleGestureDetector(ctx, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(d: ScaleGestureDetector): Boolean {
            val f = d.scaleFactor
            val next = relScale() * f
            if (next in 0.8f..maxScaleRel) {
                drawMatrix.postScale(f, f, d.focusX, d.focusY)
                apply()
            }
            return true
        }
    })

    private val tapDetector = GestureDetector(ctx, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            toggleZoom(e.x, e.y)
            return true
        }
    })

    init {
        scaleType = ScaleType.MATRIX
    }

    fun setPageBitmap(bmp: Bitmap) {
        setImageBitmap(bmp)
        fitPending = true
        fitCenter()
        post { if (fitPending) fitCenter() }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (fitPending) fitCenter()
    }

    private fun fitCenter() {
        baseMatrix.reset()
        val d = drawable ?: return
        val vw = width.toFloat()
        val vh = height.toFloat()
        if (vw <= 0 || vh <= 0) return
        val dw = d.intrinsicWidth.toFloat()
        val dh = d.intrinsicHeight.toFloat()
        if (dw <= 0 || dh <= 0) return
        val s = minOf(vw / dw, vh / dh).coerceAtLeast(0.05f)
        baseMatrix.postScale(s, s)
        val dx = (vw - dw * s) / 2f
        val dy = (vh - dh * s) / 2f
        baseMatrix.postTranslate(dx, dy)
        drawMatrix.set(baseMatrix)
        imageMatrix = drawMatrix
        fitScale = s
        fitPending = false
    }

    private fun scale(): Float {
        val v = FloatArray(9)
        drawMatrix.getValues(v)
        return v[Matrix.MSCALE_X]
    }

    private fun relScale(): Float = scale() / fitScale

    private fun toggleZoom(fx: Float, fy: Float) {
        if (relScale() > 1.2f) {
            drawMatrix.set(baseMatrix)
        } else {
            val target = 3f
            val f = target / relScale()
            drawMatrix.postScale(f, f, fx, fy)
        }
        apply()
    }

    private fun apply() {
        clamp()
        imageMatrix = drawMatrix
    }

    private fun clamp() {
        val v = FloatArray(9)
        drawMatrix.getValues(v)
        val sx = v[Matrix.MSCALE_X]
        val sy = v[Matrix.MSCALE_Y]
        var tx = v[Matrix.MTRANS_X]
        var ty = v[Matrix.MTRANS_Y]
        val d = drawable ?: return
        val vw = width.toFloat()
        val vh = height.toFloat()
        val dw = d.intrinsicWidth * sx
        val dh = d.intrinsicHeight * sy

        if (dw <= vw) {
            val b = FloatArray(9)
            baseMatrix.getValues(b)
            tx = b[Matrix.MTRANS_X]
        } else {
            tx = tx.coerceIn(vw - dw, 0f)
        }
        if (dh <= vh) {
            val b = FloatArray(9)
            baseMatrix.getValues(b)
            ty = b[Matrix.MTRANS_Y]
        } else {
            ty = ty.coerceIn(vh - dh, 0f)
        }
        v[Matrix.MTRANS_X] = tx
        v[Matrix.MTRANS_Y] = ty
        drawMatrix.setValues(v)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(ev)
        tapDetector.onTouchEvent(ev)
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                down.set(ev.x, ev.y)
                if (relScale() > 1.01f) parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                down.set(ev.x, ev.y)
            }
            MotionEvent.ACTION_MOVE -> {
                if (ev.pointerCount == 1 && relScale() > 1.01f) {
                    parent?.requestDisallowInterceptTouchEvent(true)
                    drawMatrix.postTranslate(ev.x - down.x, ev.y - down.y)
                    apply()
                    down.set(ev.x, ev.y)
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (ev.pointerCount == 2) down.set(ev.x, ev.y)
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
