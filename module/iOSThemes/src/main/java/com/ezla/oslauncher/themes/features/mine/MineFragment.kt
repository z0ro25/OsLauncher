package com.ezla.oslauncher.themes.features.mine

import com.ezla.oslauncher.themes.Models.IconPack
import com.ezla.oslauncher.themes.Models.PreviewCard
import com.ezla.oslauncher.themes.Models.MineTab
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.base.BaseFragment
import com.ezla.oslauncher.themes.data.DefaultThemeProvider
import com.ezla.oslauncher.themes.data.MineRepositoryProvider
import com.ezla.oslauncher.themes.data.favorite.FavoriteRepository
import com.ezla.oslauncher.themes.databinding.ThemesFragmentMineBinding
import com.ezla.oslauncher.themes.features.detail.IconDetailActivity
import com.ezla.oslauncher.themes.features.detail.ThemeViewActivity
import com.ezla.oslauncher.themes.features.detail.WallpaperDetailActivity
import com.ezla.oslauncher.themes.features.home.GridSpacingDecoration
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// Màn Mine (Figma Profile/*): My Library = theme mặc định của launcher đứng đầu + đồ user đã có;
// Suggest = gợi ý. Favorites (local) có chip con Wallpapers / Icon, không có theme và Suggest.
class MineFragment : BaseFragment<ThemesFragmentMineBinding>() {

    private val repository = MineRepositoryProvider.repository
    private var tab = MineTab.THEME
    private var favoriteTab = MineTab.WALLPAPERS
    private var loadJob: Job? = null

    // Bấm thẻ theme / wallpaper / bộ icon xử lý tại chỗ; callback chỉ còn dùng cho nút "more".
    var callback: MineCallback = object : MineCallback {}

    private val libraryCards = PreviewCardAdapter(small = false) { onCardClick(it) }
    private val suggestCards = PreviewCardAdapter(small = true) { onCardClick(it) }
    private val libraryPacks = IconPackAdapter { onIconPackClick(it) }
    private val suggestPacks = IconPackAdapter { onIconPackClick(it) }

    override fun setViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        ThemesFragmentMineBinding.inflate(inflater, container, false)

    override fun initView() {
        binding.rvMineTab.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvMineTab.adapter = MineTabAdapter(tab) { selectTab(it) }
        // Chip con chia đều bề ngang (trong lề màn), cách nhau themes_mine_chip_gap.
        binding.rvFavoriteTab.layoutManager = noScrollGrid(FAVORITE_TABS.size)
        binding.rvFavoriteTab.addItemDecoration(
            GridSpacingDecoration(FAVORITE_TABS.size, resources.getDimensionPixelSize(R.dimen.themes_mine_chip_gap))
        )
        binding.rvFavoriteTab.adapter =
            MineTabAdapter(favoriteTab, FAVORITE_TABS, fillWidth = true) { selectFavoriteTab(it) }

        binding.headerLibrary.tvSectionTitle.setText(R.string.themes_mine_my_library)
        binding.headerSuggest.tvSectionTitle.setText(R.string.themes_mine_suggest)
        binding.headerSuggest.tvSectionTitle.textSize = 14f

        binding.rvLibrary.addItemDecoration(GridSpacingDecoration(LIBRARY_SPAN, gapPx()))
        applyLayouts()
    }

    override fun viewListener() {
        binding.headerLibrary.tvSectionMore.setOnClickListener { openMore(true) }
        binding.headerSuggest.tvSectionMore.setOnClickListener { openMore(false) }
    }

    // Vẫn báo callback cũ (nếu nơi khác có gắn) rồi mở màn xem tất cả của tab đang chọn.
    private fun openMore(isLibrary: Boolean) {
        callback.onMoreClick(tab, isLibrary)
        MineMoreActivity.start(requireContext(), tab, isLibrary)
    }

    override fun dataObservable() = load()

    // Yêu thích đổi ở màn detail -> quay về (hoặc chuyển lại tab Mine) thì nạp lại tab Favorites.
    override fun onResume() {
        super.onResume()
        if (!isHidden && tab == MineTab.FAVORITES) load()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && tab == MineTab.FAVORITES) load()
    }

    private fun selectTab(newTab: MineTab) {
        tab = newTab
        applyLayouts()
        load()
    }

    private fun selectFavoriteTab(newTab: MineTab) {
        favoriteTab = newTab
        applyLayouts()
        load()
    }

    // Theme/Wallpapers: thẻ preview (Suggest 3 cột nhỏ); Icon: thẻ icon pack 2 cột.
    // Favorites theo chip con: Wallpapers = thẻ preview, Icon = thẻ icon pack.
    private fun applyLayouts() {
        val isPack = tab == MineTab.ICON || (tab == MineTab.FAVORITES && favoriteTab == MineTab.ICON)
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
        val isFavorites = current == MineTab.FAVORITES
        // Favorites: chip con thay tiêu đề My Library, không có khối Suggest.
        binding.rvFavoriteTab.visibility = if (isFavorites) View.VISIBLE else View.GONE
        binding.headerLibrary.root.visibility = if (isFavorites) View.GONE else View.VISIBLE
        binding.headerSuggest.root.visibility = if (isFavorites) View.GONE else View.VISIBLE
        binding.rvSuggest.visibility = if (isFavorites) View.GONE else View.VISIBLE
        binding.tvFavoriteEmpty.visibility = View.GONE
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
                    suggestPacks.submit(repository.getSuggestIconPacks().take(SUGGEST_PACK_COUNT))
                }
                MineTab.FAVORITES -> {
                    val count = if (favoriteTab == MineTab.ICON) {
                        FavoriteRepository.getIconPacks(ctx).also { libraryPacks.submit(it) }.size
                    } else {
                        favoriteWallpapers().also { libraryCards.submit(it) }.size
                    }
                    binding.tvFavoriteEmpty.visibility = if (count == 0) View.VISIBLE else View.GONE
                }
            }
        }
    }

    // Trạng thái mở khoá lấy từ my-backgrounds (mở khoá ở màn khác vẫn đúng); mất mạng -> coi như khoá.
    private suspend fun favoriteWallpapers(): List<PreviewCard> {
        val cards = FavoriteRepository.getWallpapers(requireContext())
        if (cards.none { !it.isDefault }) return cards
        val unlockedIds = repository.getOwnedWallpapers().map { it.backgroundId }.toSet()
        return cards.map { if (it.isDefault) it else it.copy(isUnlocked = it.backgroundId in unlockedIds) }
    }

    // Thẻ Default đi chung luồng với theme thường (màn tự dựng dữ liệu mặc định, Apply = khôi phục).
    private fun onIconPackClick(pack: IconPack) {
        IconDetailActivity.start(requireContext(), pack.id)
    }

    // Theme -> màn View theme; Wallpaper (kể cả thẻ ở Favorites) -> màn wallpaper detail.
    private fun onCardClick(card: PreviewCard) {
        if (tab == MineTab.WALLPAPERS || tab == MineTab.FAVORITES) onWallpaperClick(card) else onThemeClick(card)
    }

    private fun onThemeClick(card: PreviewCard) {
        ThemeViewActivity.start(requireContext(), card.id, null)
    }

    private fun onWallpaperClick(card: PreviewCard) {
        if (card.isDefault) {
            WallpaperDetailActivity.startDefault(requireContext(), card.name)
            return
        }
        val url = card.image as? String ?: return
        WallpaperDetailActivity.start(
            requireContext(), url, card.name, card.backgroundId, card.coins, card.isUnlocked
        )
    }

    companion object {
        private const val LIBRARY_SPAN = 2
        private const val SUGGEST_CARD_SPAN = 3
        private const val SUGGEST_PACK_COUNT = 4
        private val FAVORITE_TABS = listOf(MineTab.WALLPAPERS, MineTab.ICON)
    }
}
