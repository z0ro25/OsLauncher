package com.oslauncher.applauncher.themelauncher.Base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.oslauncher.applauncher.themelauncher.tool.languageTool.LanguageUtil

abstract class BaseFragment<b : ViewBinding> : Fragment() {
    lateinit var binding: b

    abstract fun setViewBinding(inflater: LayoutInflater, container: ViewGroup?): b

    //setupView here
    abstract fun initView()

    //listen to user action here
    abstract fun viewListener()

    //listen to database change here
    abstract fun dataObservable()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        LanguageUtil.setLocale(requireActivity())
        binding = setViewBinding(inflater, container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        viewListener()
        dataObservable()
    }
}