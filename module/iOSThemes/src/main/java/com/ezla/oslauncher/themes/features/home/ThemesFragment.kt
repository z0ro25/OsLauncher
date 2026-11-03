package com.ezla.oslauncher.themes.features.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ezla.oslauncher.themes.Models.ControlCenterCard
import com.ezla.oslauncher.themes.Models.HomeKind
import com.ezla.oslauncher.themes.Models.IconThemeCard
import com.ezla.oslauncher.themes.Models.PreviewCard
import com.ezla.oslauncher.themes.Models.ThemeCategory
import com.ezla.oslauncher.themes.Models.ThemeItem
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.base.BaseFragment
import com.ezla.oslauncher.themes.data.ThemeRepositoryProvider
import com.ezla.oslauncher.themes.databinding.ThemesFragmentThemesBinding
import com.ezla.oslauncher.themes.features.ThemesInter
import com.ezla.oslauncher.themes.features.controlcenter.ControlCenterDetailActivity
import com.ezla.oslauncher.themes.features.detail.IconDetailActivity
import com.ezla.oslauncher.themes.features.detail.ThemeViewActivity
import com.ezla.oslauncher.themes.features.detail.WallpaperDetailActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// Tab Themes (Figma Main): chip loại Theme / Icons / Wallpaper / Control Center + mỗi danh mục 1 section.
// Lấy danh mục 1 lần rồi gọi /themes theo TỪNG category_id song song: section nào về trước hiện trước,
// không chờ cả màn. Bấm thẻ đi đúng luồng cũ của loại đó (View theme / Icon / Wallpaper detail).
class ThemesFragment : BaseFragment<ThemesFragmentThemesBinding>() {

    private val repository = ThemeRepositoryProvider.repository
    private var kind = HomeKind.THEME
    private var categories: List<ThemeCategory>? = null

    // Đổi chip giữa chừng -> huỷ lượt tải cũ để section của chip trước không đè lên chip mới.
    private var loadJob: Job? = null

    // Đã mở màn chi tiết -> quay về thì tải lại để thẻ lấy is_unlocked mới (vừa mở khoá).
    private var reloadOnResume = false

    // Home đổi tiêu đề topbar theo chip (Control Center có tiêu đề riêng như Figma).
    var onTitleChanged: (Int) -> Unit = {}

    private val sectionAdapter = HomeSectionAdapter(
        onMore = { category -> CategoryMoreActivity.start(requireContext(), kind, category) },
        onCard = { category, card -> openCard(category.id, card) }
    )

    override fun setViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        ThemesFragmentThemesBinding.inflate(inflater, container, false)

    override fun initView() {
        binding.rvKind.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvKind.adapter = HomeKindAdapter(kind) { selectKind(it) }
        binding.rvSection.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSection.adapter = sectionAdapter
    }

    override fun viewListener() {
        binding.btnRetry.setOnClickListener {
            categories = null
            load()
        }
    }

    override fun dataObservable() = load()

    override fun onResume() {
        super.onResume()
        if (reloadOnResume) {
            reloadOnResume = false
            load()
        }
    }

    @StringRes
    fun currentTitle(): Int =
        if (kind == HomeKind.CONTROL_CENTER) R.string.themes_kind_control_center else R.string.themes_nav_themes

    private fun selectKind(newKind: HomeKind) {
        kind = newKind
        onTitleChanged(currentTitle())
        load()
    }

    private fun load() {
        loadJob?.cancel()
        val current = kind
        binding.layoutError.visibility = View.GONE
        loadJob = viewLifecycleOwner.lifecycleScope.launch {
            val list = categories ?: repository.getCategories().also { if (it.isNotEmpty()) categories = it }
            if (list.isEmpty()) {
                sectionAdapter.reset(current, emptyList())
                binding.layoutError.visibility = View.VISIBLE
                return@launch
            }
            sectionAdapter.reset(current, list)
            binding.rvSection.scrollToPosition(0)
            // launch con theo loadJob: huỷ lượt cũ là huỷ hết request của từng danh mục.
            list.forEach { category ->
                launch {
                    sectionAdapter.update(category.id, repository.getHomeItems(current, category.id))
                    if (!sectionAdapter.hasContent()) binding.layoutError.visibility = View.VISIBLE
                }
            }
        }
    }

    // Inter (nếu có) rồi mở màn chi tiết theo loại thẻ.
    private fun openCard(categoryId: String, card: Any) {
        ThemesInter.run(activity) {
            if (!isAdded) return@run
            reloadOnResume = true
            val ctx = requireContext()
            when (card) {
                is ThemeItem -> ThemeViewActivity.start(ctx, card.id, categoryId)
                is IconThemeCard -> IconDetailActivity.start(ctx, card.id)
                is PreviewCard -> WallpaperDetailActivity.start(
                    ctx, card.image.toString(), card.name, card.backgroundId, card.coins, card.isUnlocked
                )
                is ControlCenterCard -> ControlCenterDetailActivity.start(ctx, card.themeId)
            }
        }
    }
}
