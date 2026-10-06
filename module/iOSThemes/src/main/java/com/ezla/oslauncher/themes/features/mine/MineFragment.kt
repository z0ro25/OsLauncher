package com.ezla.oslauncher.themes.features.mine

import com.ezla.oslauncher.themes.Models.PreviewCard
import com.ezla.oslauncher.themes.Models.MineTab
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.base.BaseFragment
import com.ezla.oslauncher.themes.data.DefaultThemeProvider
import com.ezla.oslauncher.themes.data.MineRepositoryProvider
import com.ezla.oslauncher.themes.databinding.ThemesFragmentMineBinding
import com.ezla.oslauncher.themes.features.home.GridSpacingDecoration
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// Màn Mine (Figma Profile/*): My Library = theme mặc định của launcher đứng đầu + đồ user đã có;
// Suggest = gợi ý. Favorites dùng cùng dạng thẻ với Icon theo Figma.
class MineFragment : BaseFragment<ThemesFragmentMineBinding>() {

    private val repository = MineRepositoryProvider.repository
    private var tab = MineTab.THEME
    private var loadJob: Job? = null

    // Chưa nối hành động; gắn implementation thật khi có API / logic áp theme.
    var callback: MineCallback = object : MineCallback {}

    private val libraryCards = PreviewCardAdapter(small = false) { onCardClick(it) }
    private val suggestCards = PreviewCardAdapter(small = true) { onCardClick(it) }
    private val libraryPacks = IconPackAdapter { callback.onIconPackClick(it, tab == MineTab.FAVORITES) }
    private val suggestPacks = IconPackAdapter { callback.onIconPackClick(it, tab == MineTab.FAVORITES) }

    override fun setViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        ThemesFragmentMineBinding.inflate(inflater, container, false)

    override fun initView() {
        binding.rvMineTab.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvMineTab.adapter = MineTabAdapter(tab) { selectTab(it) }

        binding.headerLibrary.tvSectionTitle.setText(R.string.themes_mine_my_library)
        binding.headerSuggest.tvSectionTitle.setText(R.string.themes_mine_suggest)
        binding.headerSuggest.tvSectionTitle.textSize = 14f

        binding.rvLibrary.addItemDecoration(GridSpacingDecoration(LIBRARY_SPAN, gapPx()))
        applyLayouts()
    }

    override fun viewListener() {
        binding.headerLibrary.tvSectionMore.setOnClickListener { callback.onMoreClick(tab, true) }
        binding.headerSuggest.tvSectionMore.setOnClickListener { callback.onMoreClick(tab, false) }
    }

    override fun dataObservable() = load()

    private fun selectTab(newTab: MineTab) {
        tab = newTab
        applyLayouts()
        load()
    }

    // Theme/Wallpapers: thẻ preview (Suggest 3 cột nhỏ); Icon/Favorites: thẻ icon pack 2 cột.
    private fun applyLayouts() {
        val isPack = tab == MineTab.ICON || tab == MineTab.FAVORITES
        val suggestSpan = if (isPack) LIBRARY_SPAN else SUGGEST_CARD_SPAN
        binding.rvLibrary.layoutManager = noScrollGrid(LIBRARY_SPAN)
        binding.rvLibrary.adapter = if (isPack) libraryPacks else libraryCards

        // Số cột Suggest đổi theo tab nên decoration phải thay theo.
        while (binding.rvSuggest.itemDecorationCount > 0) binding.rvSuggest.removeItemDecorationAt(0)
        binding.rvSuggest.addItemDecoration(GridSpacingDecoration(suggestSpan, gapPx()))
        binding.rvSuggest.layoutManager = noScrollGrid(suggestSpan)
        binding.rvSuggest.adapter = if (isPack) suggestPacks else suggestCards
    }

    private fun gapPx() = resources.getDimensionPixelSize(R.dimen.themes_mine_gap)

    private fun noScrollGrid(span: Int) = object : GridLayoutManager(requireContext(), span) {
        override fun canScrollVertically() = false
    }

    private fun load() {
        loadJob?.cancel()
        val current = tab
        loadJob = viewLifecycleOwner.lifecycleScope.launch {
            val ctx = requireContext()
            when (current) {
                MineTab.THEME -> {
                    libraryCards.submit(listOf(DefaultThemeProvider.theme(ctx)) + repository.getOwnedThemes())
                    suggestCards.submit(repository.getSuggestThemes().take(SUGGEST_CARD_SPAN))
                }
                MineTab.WALLPAPERS -> {
                    libraryCards.submit(listOf(DefaultThemeProvider.wallpaper(ctx)) + repository.getOwnedWallpapers())
                    suggestCards.submit(repository.getSuggestWallpapers().take(SUGGEST_CARD_SPAN))
                }
                MineTab.ICON -> {
                    libraryPacks.submit(listOf(DefaultThemeProvider.iconPack(ctx)) + repository.getOwnedIconPacks())
                    suggestPacks.submit(repository.getSuggestIconPacks())
                }
                MineTab.FAVORITES -> {
                    libraryPacks.submit(repository.getFavorites())
                    suggestPacks.submit(repository.getSuggestIconPacks())
                }
            }
        }
    }

    private fun onCardClick(card: PreviewCard) {
        if (tab == MineTab.WALLPAPERS) callback.onWallpaperClick(card) else callback.onThemeClick(card)
    }

    companion object {
        private const val LIBRARY_SPAN = 2
        private const val SUGGEST_CARD_SPAN = 3
    }
}
