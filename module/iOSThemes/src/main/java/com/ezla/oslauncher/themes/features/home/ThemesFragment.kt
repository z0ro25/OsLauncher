package com.ezla.oslauncher.themes.features.home

import com.ezla.oslauncher.themes.Models.ThemeTab
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.base.BaseFragment
import com.ezla.oslauncher.themes.data.ThemeRepositoryProvider
import com.ezla.oslauncher.themes.databinding.ThemesFragmentThemesBinding
import com.ezla.oslauncher.themes.features.ThemesInter
import com.ezla.oslauncher.themes.features.detail.ThemeViewActivity
import kotlinx.coroutines.launch

// Tab Themes (Figma Main): chip danh mục + lưới theme. Figma mới bỏ tab Featured/Top nên luôn
// lấy danh sách FEATURED. Lúc tải hiện thẻ placeholder thay spinner.
class ThemesFragment : BaseFragment<ThemesFragmentThemesBinding>() {

    private val repository = ThemeRepositoryProvider.repository
    private var categoryId: String? = null

    private val categoryAdapter = CategoryAdapter { category ->
        categoryId = category.id
        loadThemes()
    }

    // Bấm theme -> inter (nếu có) rồi màn View theme (xem trước rồi mới Install).
    private val themeAdapter = ThemeAdapter { theme ->
        ThemesInter.run(activity) {
            if (isAdded) ThemeViewActivity.start(requireContext(), theme.id, categoryId)
        }
    }

    private val placeholderAdapter = ThemePlaceholderAdapter(PLACEHOLDER_COUNT)

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
    }

    override fun viewListener() {}

    override fun dataObservable() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Categories + themes đều là API -> giữ placeholder liền mạch suốt lần tải đầu.
            setLoading(true)
            val categories = repository.getCategories()
            if (categoryId == null) categoryId = categories.firstOrNull()?.id
            categoryAdapter.submit(categories, categoryId)
            submitThemes()
            setLoading(false)
        }
    }

    // Đang tải: lưới hiện thẻ placeholder; xong thì gắn lại adapter theme thật.
    private fun setLoading(loading: Boolean) {
        val target = if (loading) placeholderAdapter else themeAdapter
        if (binding.rvTheme.adapter !== target) binding.rvTheme.adapter = target
    }

    // Đổi chip -> tải lại lưới theme (API).
    private fun loadThemes() {
        viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)
            submitThemes()
            setLoading(false)
        }
    }

    private suspend fun submitThemes() {
        val category = categoryId ?: return
        themeAdapter.submit(repository.getThemes(ThemeTab.FEATURED, category))
    }

    companion object {
        private const val SPAN = 2
        private const val PLACEHOLDER_COUNT = 4
    }
}
