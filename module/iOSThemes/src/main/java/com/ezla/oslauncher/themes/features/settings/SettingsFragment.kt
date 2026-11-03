package com.ezla.oslauncher.themes.features.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import com.ezla.oslauncher.themes.base.BaseFragment
import com.ezla.oslauncher.themes.databinding.ThemesViewSettingsContentBinding

// Tab Setting ở Home (Figma "Settings"): cùng nội dung SettingsActivity.
class SettingsFragment : BaseFragment<ThemesViewSettingsContentBinding>() {

    private var controller: SettingsController? = null

    override fun setViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        ThemesViewSettingsContentBinding.inflate(inflater, container, false)

    override fun initView() {
        controller = SettingsController(requireActivity(), binding).also { it.setup() }
    }

    override fun viewListener() {}

    override fun dataObservable() {}

    override fun onResume() {
        super.onResume()
        if (!isHidden) controller?.onResume()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) controller?.onResume()
    }

    override fun onDestroyView() {
        controller = null
        super.onDestroyView()
    }
}
