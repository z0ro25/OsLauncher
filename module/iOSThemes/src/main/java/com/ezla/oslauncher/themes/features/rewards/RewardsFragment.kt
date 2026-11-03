package com.ezla.oslauncher.themes.features.rewards

import android.view.LayoutInflater
import android.view.ViewGroup
import com.ezla.oslauncher.themes.base.BaseFragment
import com.ezla.oslauncher.themes.databinding.ThemesViewRewardsContentBinding

// Tab Rewards ở Home (Figma "Rewards"): cùng nội dung RewardsActivity; nhận xu xong báo [onBalanceChanged]
// để Home cập nhật badge xu trên topbar.
class RewardsFragment : BaseFragment<ThemesViewRewardsContentBinding>() {

    var onBalanceChanged: (Int) -> Unit = {}

    private var controller: RewardsController? = null

    override fun setViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        ThemesViewRewardsContentBinding.inflate(inflater, container, false)

    override fun initView() {
        controller = RewardsController(requireActivity(), viewLifecycleOwner, binding) { onBalanceChanged(it) }
            .also { it.setup() }
    }

    override fun viewListener() {}

    override fun dataObservable() {}

    // Tab ẩn/hiện qua hide/show nên phải nghe cả onHiddenChanged, không chỉ onResume.
    override fun onResume() {
        super.onResume()
        if (!isHidden) controller?.onResume()
    }

    override fun onPause() {
        controller?.onPause()
        super.onPause()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) controller?.onPause() else controller?.onResume()
    }

    override fun onDestroyView() {
        controller?.onPause()
        controller = null
        super.onDestroyView()
    }
}
