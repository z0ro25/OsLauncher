package com.ezla.oslauncher.themes.features.home

import com.ezla.oslauncher.themes.Models.ThemeTab
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.base.BaseFragment
import com.ezla.oslauncher.themes.data.ThemeRepositoryProvider
import com.ezla.oslauncher.themes.databinding.ThemesFragmentThemesBinding
import com.ezla.oslauncher.themes.features.detail.ThemeDetailActivity
import com.ezla.oslauncher.themes.extensions.makeInVisible
import com.ezla.oslauncher.themes.extensions.makeVisible
import kotlinx.coroutines.launch

// Tab Themes: Featured/Top dùng chung layout (Figma chưa có thiết kế riêng cho Top).
class ThemesFragment : BaseFragment<ThemesFragmentThemesBinding>() {

    private val repository = ThemeRepositoryProvider.repository
    private var tab = ThemeTab.FEATURED
    private var categoryId: String? = null

    private val categoryAdapter = CategoryAdapter { category ->
        categoryId = category.id
        loadThemes()
    }

    // Bấm theme -> mở màn Install (chi tiết + áp dụng).
    private val themeAdapter = ThemeAdapter { theme ->
        ThemeDetailActivity.start(requireContext(), theme.id)
    }

    override fun setViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        ThemesFragmentThemesBinding.inflate(inflater, container, false)

    override fun initView() {
        binding.rvCategory.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvCategory.adapter = categoryAdapter

        binding.rvTheme.layoutManager = GridLayoutManager(requireContext(), SPAN)
        binding.rvTheme.addItemDecoration(
            GridSpacingDecoration(SPAN, resources.getDimensionPixelSize(R.dimen.themes_grid_gap))
        )
        binding.rvTheme.adapter = themeAdapter
        renderTab()
    }

    override fun viewListener() {
        binding.tabFeatured.setOnClickListener { selectTab(ThemeTab.FEATURED) }
        binding.tabTop.setOnClickListener { selectTab(ThemeTab.TOP) }
    }

    override fun dataObservable() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Categories + themes đều là API -> giữ loading liền mạch suốt lần tải đầu, không nháy.
            setLoading(true)
            val categories = repository.getCategories()
            if (categoryId == null) categoryId = categories.firstOrNull()?.id
            categoryAdapter.submit(categories, categoryId)
            submitThemes()
            setLoading(false)
        }
    }

    // Bật/tắt spinner đè lên lưới theme (chip + tab vẫn dùng được).
    private fun setLoading(loading: Boolean) {
        binding.pbLoading.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun selectTab(newTab: ThemeTab) {
        if (tab == newTab) return
        tab = newTab
        renderTab()
        loadThemes()
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

    // Đổi tab/chip -> tải lại lưới theme (API), có loading riêng.
    private fun loadThemes() {
        viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)
            submitThemes()
            setLoading(false)
        }
    }

    private suspend fun submitThemes() {
        val category = categoryId ?: return
        themeAdapter.submit(repository.getThemes(tab, category))
    }

    companion object {
        private const val SPAN = 2
    }
}
