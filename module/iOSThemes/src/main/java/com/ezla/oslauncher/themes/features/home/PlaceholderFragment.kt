package com.ezla.oslauncher.themes.features.home

import android.view.LayoutInflater
import android.view.ViewGroup
import com.ezla.oslauncher.themes.base.BaseFragment
import com.ezla.oslauncher.themes.databinding.ThemesFragmentPlaceholderBinding

// Tab Icons / Wallpapers chưa dựng; thay bằng Fragment thật khi làm các màn đó.
class PlaceholderFragment : BaseFragment<ThemesFragmentPlaceholderBinding>() {
    override fun setViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        ThemesFragmentPlaceholderBinding.inflate(inflater, container, false)

    override fun initView() {}
    override fun viewListener() {}
    override fun dataObservable() {}
}
