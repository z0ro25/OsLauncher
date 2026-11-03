package com.ezla.oslauncher.controlcenter.features.onboarding

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ezla.oslauncher.appbase.onboarding.BaseOnboardingActivity
import com.ezla.oslauncher.appbase.onboarding.OnboardingPage
import com.ezla.oslauncher.controlcenter.CcPrefs
import com.ezla.oslauncher.controlcenter.R
import com.ezla.oslauncher.controlcenter.features.main.CcMainActivity

/** Onboarding lần đầu mở Control Center (Figma "Onboarding 4..6"). */
class CcOnboardingActivity : BaseOnboardingActivity() {

    override val pages = listOf(
        OnboardingPage(R.drawable.cc_img_onboarding_1, R.string.cc_onboarding_title_1, R.string.cc_onboarding_desc_1),
        OnboardingPage(R.drawable.cc_img_onboarding_2, R.string.cc_onboarding_title_2, R.string.cc_onboarding_desc_2),
        OnboardingPage(R.drawable.cc_img_onboarding_3, R.string.cc_onboarding_title_3, R.string.cc_onboarding_desc_3)
    )

    // Figma trang này nền đen chữ trắng, khác base (nền trắng): đổi màu từng trang khi gắn vào pager,
    // không sửa layout dùng chung của OSAppBase.
    override fun initView() {
        super.initView()
        binding.root.setBackgroundColor(0xFF000000.toInt())
        val recycler = binding.vpOnboarding.getChildAt(0) as? RecyclerView
        recycler?.addOnChildAttachStateChangeListener(object : RecyclerView.OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(view: View) = darken(view)
            override fun onChildViewDetachedFromWindow(view: View) = Unit
        })
    }

    private fun darken(page: View) {
        val group = page as? ViewGroup ?: return
        for (i in 0 until group.childCount) {
            val child = group.getChildAt(i)
            when {
                child is TextView && child.id == com.ezla.oslauncher.appbase.R.id.tvTitle ->
                    child.setTextColor(0xFFFFFFFF.toInt())
                child is TextView && child.id == com.ezla.oslauncher.appbase.R.id.tvDesc ->
                    child.setTextColor(0xB3FFFFFF.toInt())
                child !is TextView && child.id == View.NO_ID ->
                    child.setBackgroundResource(R.drawable.cc_bg_onboarding_fade)
            }
        }
    }

    override fun onOnboardingDone() {
        CcPrefs.markOnboardingDone(this)
        startActivity(Intent(this, CcMainActivity::class.java))
    }
}
