package com.ezla.oslauncher.themes.features.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ezla.oslauncher.themes.Models.ThemeCategory
import com.ezla.oslauncher.themes.Models.ThemeTab
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.base.BaseFragment
import com.ezla.oslauncher.themes.data.ThemeRepositoryProvider
import com.ezla.oslauncher.themes.databinding.ThemesFragmentWallpapersBinding
import com.ezla.oslauncher.themes.features.ThemesInter
import com.ezla.oslauncher.themes.features.detail.WallpaperDetailActivity
import com.ezla.oslauncher.themes.features.mine.PreviewCardAdapter
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// Tab Wallpapers: chip "All" (mặc định, /backgrounds có loadmore) + chip danh mục + lưới thẻ nền.
// Bấm thẻ -> màn wallpaper detail.
class WallpapersFragment : BaseFragment<ThemesFragmentWallpapersBinding>() {

    private val repository = ThemeRepositoryProvider.repository
    private var categoryId: String? = null

    // Chip đổi giữa chừng -> huỷ lượt tải cũ để thẻ của chip trước không đè lên chip mới.
    private var loadJob: Job? = null

    // Loadmore chỉ dùng cho chip All: trang đã tải xong + còn trang sau không + đang tải trang kế.
    private var allPage = 0
    private var allHasMore = false
    private var loadingMore = false

    private val categoryAdapter = CategoryAdapter { category ->
        categoryId = category.id
        loadWallpapers()
    }

    // Đã mở màn chi tiết -> quay về thì tải lại để thẻ lấy is_unlocked mới (vừa mở khoá).
    private var reloadOnResume = false

    private val wallpaperAdapter = PreviewCardAdapter(small = false, showNewBadge = true) { card ->
        ThemesInter.run(activity) {
            if (!isAdded) return@run
            reloadOnResume = true
            WallpaperDetailActivity.start(
                requireContext(), card.image.toString(), card.name,
                card.backgroundId, card.coins, card.isUnlocked
            )
        }
    }

    override fun setViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        ThemesFragmentWallpapersBinding.inflate(inflater, container, false)

    override fun initView() {
        binding.rvCategory.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvCategory.adapter = categoryAdapter

        val grid = GridLayoutManager(requireContext(), SPAN)
        binding.rvWallpaper.layoutManager = grid
        binding.rvWallpaper.addItemDecoration(
            GridSpacingDecoration(SPAN, resources.getDimensionPixelSize(R.dimen.themes_grid_gap))
        )
        binding.rvWallpaper.adapter = wallpaperAdapter
        binding.rvWallpaper.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
                if (grid.findLastVisibleItemPosition() >= wallpaperAdapter.itemCount - LOAD_MORE_THRESHOLD) {
                    loadMoreAll()
                }
            }
        })
    }

    override fun viewListener() {}

    override fun dataObservable() {
        loadJob?.cancel()
        loadJob = viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)
            // All chỉ có chữ (iconUrl null) và chỉ có ở tab này, không thêm vào repository dùng chung.
            val all = ThemeCategory(ALL_ID, titleRes = R.string.themes_category_all)
            val categories = listOf(all) + repository.getCategories()
            if (categoryId == null) categoryId = ALL_ID
            categoryAdapter.submit(categories, categoryId)
            submitWallpapers()
            setLoading(false)
        }
    }

    // Chip All giữ nguyên danh sách + vị trí cuộn khi quay về (tải lại sẽ mất các trang đã loadmore).
    override fun onResume() {
        super.onResume()
        if (reloadOnResume) {
            reloadOnResume = false
            if (categoryId != ALL_ID) loadWallpapers()
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.pbLoading.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun loadWallpapers() {
        loadJob?.cancel()
        loadJob = viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)
            submitWallpapers()
            setLoading(false)
        }
    }

    private suspend fun submitWallpapers() {
        val category = categoryId ?: return
        resetLoadMore()
        if (category == ALL_ID) {
            val page = repository.getAllWallpapers(1)
            wallpaperAdapter.submit(page?.cards.orEmpty())
            allPage = 1
            allHasMore = page?.hasMore ?: false
        } else {
            // Repository vẫn nhận tab; bỏ thanh Featured/Top nên luôn lấy FEATURED.
            wallpaperAdapter.submit(repository.getWallpapers(ThemeTab.FEATURED, category))
        }
    }

    // Lỗi mạng -> toast, giữ allPage để lần cuộn sau thử lại đúng trang đó.
    private fun loadMoreAll() {
        if (categoryId != ALL_ID || !allHasMore || loadingMore || loadJob?.isActive == true) return
        loadingMore = true
        binding.pbLoadMore.visibility = View.VISIBLE
        val nextPage = allPage + 1
        loadJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                val page = repository.getAllWallpapers(nextPage)
                if (page == null) {
                    Toast.makeText(requireContext(), R.string.themes_network_error, Toast.LENGTH_SHORT).show()
                } else {
                    wallpaperAdapter.append(page.cards)
                    allPage = nextPage
                    allHasMore = page.hasMore
                }
            } finally {
                loadingMore = false
                binding.pbLoadMore.visibility = View.GONE
            }
        }
    }

    private fun resetLoadMore() {
        allPage = 0
        allHasMore = false
        loadingMore = false
        binding.pbLoadMore.visibility = View.GONE
    }

    companion object {
        private const val SPAN = 2
        // Không trùng id danh mục của server (số nguyên).
        private const val ALL_ID = "all"
        // Còn ≤ 4 thẻ (2 hàng) chưa hiện thì tải trang kế.
        private const val LOAD_MORE_THRESHOLD = 4
    }
}
