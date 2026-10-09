package com.ezla.oslauncher.themes.features.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.ezla.oslauncher.themes.Models.ThemeTab
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.base.BaseFragment
import com.ezla.oslauncher.themes.data.ThemeRepositoryProvider
import com.ezla.oslauncher.themes.databinding.ThemesFragmentWallpapersBinding
import com.ezla.oslauncher.themes.extensions.makeInVisible
import com.ezla.oslauncher.themes.extensions.makeVisible
import com.ezla.oslauncher.themes.features.detail.WallpaperDetailActivity
import com.ezla.oslauncher.themes.features.mine.PreviewCardAdapter
import kotlinx.coroutines.launch

// Tab Wallpapers (Figma Wallpapers/Featured): tab Featured/Top + chip + lưới thẻ nền.
// Nguồn dữ liệu = backgrounds của theme. Bấm thẻ -> màn wallpaper detail.
class WallpapersFragment : BaseFragment<ThemesFragmentWallpapersBinding>() {

    private val repository = ThemeRepositoryProvider.repository
    private var tab = ThemeTab.FEATURED
    private var categoryId: String? = null

    private val categoryAdapter = CategoryAdapter { category ->
        categoryId = category.id
        loadWallpapers()
    }

    private val wallpaperAdapter = PreviewCardAdapter(small = false) { card ->
        WallpaperDetailActivity.start(requireContext(), card.image.toString(), card.name)
    }

    override fun setViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        ThemesFragmentWallpapersBinding.inflate(inflater, container, false)

    override fun initView() {
        binding.rvCategory.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvCategory.adapter = categoryAdapter

        binding.rvWallpaper.layoutManager = GridLayoutManager(requireContext(), SPAN)
        binding.rvWallpaper.addItemDecoration(
            GridSpacingDecoration(SPAN, resources.getDimensionPixelSize(R.dimen.themes_grid_gap))
        )
        binding.rvWallpaper.adapter = wallpaperAdapter
        renderTab()
    }

    override fun viewListener() {
        binding.tabFeatured.setOnClickListener { selectTab(ThemeTab.FEATURED) }
        binding.tabTop.setOnClickListener { selectTab(ThemeTab.TOP) }
    }

    override fun dataObservable() {
        viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)
            val categories = repository.getCategories()
            if (categoryId == null) categoryId = categories.firstOrNull()?.id
            categoryAdapter.submit(categories, categoryId)
            submitWallpapers()
            setLoading(false)
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.pbLoading.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun selectTab(newTab: ThemeTab) {
        if (tab == newTab) return
        tab = newTab
        renderTab()
        loadWallpapers()
    }

    private fun renderTab() {
        val featured = tab == ThemeTab.FEATURED
        val ctx = requireContext()
        binding.tvTabFeatured.setTextColor(ContextCompat.getColor(ctx, if (featured) R.color.themes_text_title else R.color.themes_text_hint))
        binding.tvTabTop.setTextColor(ContextCompat.getColor(ctx, if (featured) R.color.themes_text_hint else R.color.themes_text_title))
        if (featured) {
            binding.lineFeatured.makeVisible(); binding.lineTop.makeInVisible()
        } else {
            binding.lineFeatured.makeInVisible(); binding.lineTop.makeVisible()
        }
    }

    private fun loadWallpapers() {
        viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)
            submitWallpapers()
            setLoading(false)
        }
    }

    private suspend fun submitWallpapers() {
        val category = categoryId ?: return
        wallpaperAdapter.submit(repository.getWallpapers(tab, category))
    }

    companion object {
        private const val SPAN = 2
    }
}
