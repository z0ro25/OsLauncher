package com.ezla.oslauncher.themes.features.icons

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ezla.oslauncher.themes.base.BaseFragment
import com.ezla.oslauncher.themes.data.ThemeRepositoryProvider
import com.ezla.oslauncher.themes.databinding.ThemesFragmentIconsBinding
import com.ezla.oslauncher.themes.features.detail.IconDetailActivity
import com.ezla.oslauncher.themes.features.home.CategoryAdapter
import kotlinx.coroutines.launch

// Tab Icons (Figma "Icon"): chip danh mục + mỗi danh mục 1 section thẻ icon (data /themes).
// Bấm chip -> cuộn tới section đó; bấm ">" -> màn xem tất cả; bấm thẻ -> Install tab Icon.
class IconsFragment : BaseFragment<ThemesFragmentIconsBinding>() {

    private val repository = ThemeRepositoryProvider.repository
    private var categoryId: String? = null
    private val sectionLayout by lazy { LinearLayoutManager(requireContext()) }

    private val categoryAdapter = CategoryAdapter { category ->
        categoryId = category.id
        scrollToSection(category.id)
    }

    private val sectionAdapter = IconSectionAdapter(
        onMore = { category -> IconCategoryActivity.start(requireContext(), category) },
        onCard = { card -> IconDetailActivity.start(requireContext(), card.id) }
    )

    override fun setViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        ThemesFragmentIconsBinding.inflate(inflater, container, false)

    override fun initView() {
        binding.rvCategory.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvCategory.adapter = categoryAdapter
        binding.rvSection.layoutManager = sectionLayout
        binding.rvSection.adapter = sectionAdapter
    }

    override fun viewListener() {}

    override fun dataObservable() {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.pbLoading.visibility = View.VISIBLE
            val categories = repository.getCategories()
            if (categoryId == null) categoryId = categories.firstOrNull()?.id
            categoryAdapter.submit(categories, categoryId)
            sectionAdapter.submit(repository.getIconSections(categories))
            binding.pbLoading.visibility = View.GONE
        }
    }

    // Đưa tiêu đề section lên đầu danh sách (không chỉ "thấy được") để giống nhảy tới mục.
    private fun scrollToSection(id: String) {
        val index = sectionAdapter.indexOf(id)
        if (index >= 0) sectionLayout.scrollToPositionWithOffset(index, 0)
    }
}
