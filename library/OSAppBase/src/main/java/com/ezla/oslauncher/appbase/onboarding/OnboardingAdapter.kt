package com.ezla.oslauncher.appbase.onboarding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ezla.oslauncher.appbase.databinding.AppbaseItemOnboardingBinding

class OnboardingAdapter(private val pages: List<OnboardingPage>) :
    RecyclerView.Adapter<OnboardingAdapter.PageHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = PageHolder(
        AppbaseItemOnboardingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount(): Int = pages.size

    override fun onBindViewHolder(holder: PageHolder, position: Int) {
        val page = pages[position]
        holder.binding.imgOnboarding.setImageResource(page.imageRes)
        holder.binding.tvTitle.setText(page.titleRes)
        holder.binding.tvDesc.setText(page.descRes)
    }

    class PageHolder(val binding: AppbaseItemOnboardingBinding) : RecyclerView.ViewHolder(binding.root)
}
