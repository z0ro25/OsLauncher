package com.ezla.oslauncher.beautylauncher.dialog

import android.app.Activity
import android.view.WindowManager.BadTokenException
import android.widget.Toast
import com.ezla.oslauncher.beautylauncher.R
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManagerFactory
import com.truongnt.ios.ioslite.common.config.SharePrefUtils
import com.truongnt.ios.rate.LauncherSharePrefUtils

// Luồng Rate cho Activity KHÔNG kế thừa BaseActivity của :app (vd Settings module Themes, qua
// ThemesEntry.rateOpener). Cùng RatingDialog + In-App Review và cùng hành vi BaseActivity.showRateDialog
// (hàm đó giữ nguyên, không đổi để khỏi ảnh hưởng màn cũ). Rate xong ghi cờ rated rồi gọi [onRated].
object RateFlow {

    fun show(activity: Activity, isFinish: Boolean, onRated: () -> Unit) {
        val ratingDialog = RatingDialog(activity)
        ratingDialog.init(object : RatingDialog.OnPress {
            override fun send(star: Float) {
                ratingDialog.dismiss()
                if (isFinish) activity.finishAffinity()
                Toast.makeText(activity, activity.getString(R.string.rate_success), Toast.LENGTH_SHORT).show()
                markRated(activity)
                onRated()
            }

            override fun rating(star: Float) {
                val manager = ReviewManagerFactory.create(activity)
                val request: Task<ReviewInfo> = manager.requestReviewFlow()
                request.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val flow: Task<Void> = manager.launchReviewFlow(activity, task.result)
                        flow.addOnSuccessListener {
                            markRated(activity)
                            ratingDialog.dismiss()
                            onRated()
                            if (isFinish) activity.finishAffinity()
                        }
                    } else {
                        ratingDialog.dismiss()
                        if (isFinish) activity.finishAffinity()
                    }
                }
            }

            override fun cancel() {}
            override fun later() {
                ratingDialog.dismiss()
                if (isFinish) activity.finishAffinity()
            }
        })
        try {
            ratingDialog.show()
        } catch (e: BadTokenException) {
            e.printStackTrace()
        }
    }

    private fun markRated(activity: Activity) {
        SharePrefUtils.forceRated(activity)
        LauncherSharePrefUtils.forceRated(activity)
    }
}
