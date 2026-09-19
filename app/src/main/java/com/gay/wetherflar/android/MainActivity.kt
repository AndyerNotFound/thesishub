package com.gay.wetherflar.android

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.gay.wetherflar.android.data.Paper
import com.gay.wetherflar.android.ui.DetailFragment
import com.gay.wetherflar.android.ui.FavoritesFragment
import com.gay.wetherflar.android.ui.HistoryFragment
import com.gay.wetherflar.android.ui.SearchFragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.color.MaterialColors


class MainActivity : AppCompatActivity() {

    private data class Tab(val menuId: Int, val create: () -> Fragment)

    private val tabs = listOf(
        Tab(R.id.nav_search) { SearchFragment() },
        Tab(R.id.nav_favorites) { FavoritesFragment() },
        Tab(R.id.nav_history) { HistoryFragment() }
    )

    private lateinit var pager: ViewPager2
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var mainContent: View
    private lateinit var secondaryContainer: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val root = findViewById<View>(R.id.root)
        pager = findViewById(R.id.pager)
        bottomNav = findViewById(R.id.bottomNav)
        mainContent = findViewById(R.id.mainContent)
        secondaryContainer = findViewById(R.id.secondaryContainer)

        
        root.setBackgroundColor(MaterialColors.getColor(root, com.google.android.material.R.attr.colorSurface, 0))
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            v.setPadding(0, top, 0, 0)
            insets
        }

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.title = getString(R.string.app_name)

        pager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = tabs.size
            override fun createFragment(position: Int): Fragment = tabs[position].create()
        }
        pager.offscreenPageLimit = 2
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val id = tabs[position].menuId
                if (bottomNav.selectedItemId != id) bottomNav.selectedItemId = id
            }
        })

        bottomNav.setOnItemSelectedListener { item ->
            val index = tabs.indexOfFirst { it.menuId == item.itemId }
            if (index >= 0) {
                pager.setCurrentItem(index, true)
                true
            } else false
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (secondaryContainer.visibility == View.VISIBLE) {
                    closeSecondary()
                    return
                }
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        })
    }

    
    fun openDetail(paper: Paper) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in_right, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out_right)
            .replace(R.id.secondaryContainer, DetailFragment.newInstance(paper))
            .commitAllowingStateLoss()
        mainContent.visibility = View.GONE
        secondaryContainer.visibility = View.VISIBLE
    }

    fun closeSecondary() {
        mainContent.visibility = View.VISIBLE
        secondaryContainer.visibility = View.GONE
        val f = supportFragmentManager.findFragmentById(R.id.secondaryContainer)
        if (f != null) {
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(0, R.anim.slide_out_right)
                .remove(f)
                .commitAllowingStateLoss()
        }
    }

    
    fun searchFromHistory(query: String) {
        pager.setCurrentItem(0, false)
        trySearch(query, 0)
    }

    private fun trySearch(query: String, attempt: Int) {
        val f = supportFragmentManager.fragments.firstOrNull { it is SearchFragment } as? SearchFragment
        if (f != null) {
            f.searchFor(query)
        } else if (attempt < 6) {
            pager.postDelayed({ trySearch(query, attempt + 1) }, 200)
        }
    }
}
