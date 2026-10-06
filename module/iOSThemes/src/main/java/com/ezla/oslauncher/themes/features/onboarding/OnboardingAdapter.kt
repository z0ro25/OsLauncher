package com.ezla.oslauncher.themes.features.onboarding

import com.ezla.oslauncher.themes.Models.OnboardingPage
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ezla.oslauncher.themes.databinding.ThemesItemOnboardingBinding

class OnboardingAdapter(private val pages: List<OnboardingPage>) :
    RecyclerView.Adapter<OnboardingAdapter.PageHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = PageHolder(
        ThemesItemOnboardingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount(): Int = pages.size

    override fun onBindViewHolder(holder: PageHolder, position: Int) {
        val page = pages[position]
        holder.binding.imgOnboarding.setImageResource(page.imageRes)
        holder.binding.tvTitle.setText(page.titleRes)
    }

    class PageHolder(val binding: ThemesItemOnboardingBinding) : RecyclerView.ViewHolder(binding.root)
}
