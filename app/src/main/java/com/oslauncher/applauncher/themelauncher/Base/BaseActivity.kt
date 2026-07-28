package com.oslauncher.applauncher.themelauncher.Base

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.WindowManager.BadTokenException
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.amz.ios.rate.LauncherSharePrefUtils
import com.oslauncher.applauncher.themelauncher.Features.no_internet.NetworkReceiver
import com.oslauncher.applauncher.themelauncher.Features.no_internet.NoInternetActivity
import com.oslauncher.applauncher.themelauncher.Features.splash.SplashActivity
import com.oslauncher.applauncher.themelauncher.R
import com.oslauncher.applauncher.themelauncher.dialog.RatingDialog
import com.oslauncher.applauncher.themelauncher.extensions.haveNetworkConnection
import com.oslauncher.applauncher.themelauncher.extensions.hideNavigation
import com.oslauncher.applauncher.themelauncher.extensions.showNav
import com.oslauncher.applauncher.themelauncher.tool.languageTool.LanguageUtil
import com.oslauncher.applauncher.themelauncher.tool.sharePreferenceTool.SharePrefUtils
import com.oslauncher.applauncher.themelauncher.utils.Constant
import com.oslauncher.applauncher.themelauncher.utils.Constant.SCREEN
import com.oslauncher.applauncher.themelauncher.utils.Constant.SPLASH_ACTIVITY
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.oslauncher.applauncher.themelauncher.Features.home.HomeActivity
import com.oslauncher.applauncher.themelauncher.utils.CommonAds


abstract class BaseActivity<b : ViewBinding> : AppCompatActivity() {
    lateinit var binding: b
    lateinit var manager: ReviewManager
    lateinit var reviewInfo: ReviewInfo
    abstract val setViewBinding: b
    private var networkReceiver: NetworkReceiver? = null

    //setupView here
    abstract fun initView()

    //listen to user action here
    abstract fun viewListener()

    //listen to database change here
    abstract fun dataObservable()

    val internetBroadcast = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "android.net.conn.CONNECTIVITY_CHANGE" || intent?.action == "android.net.wifi.WIFI_STATE_CHANGED") {
                Log.e("No_internet_start", "nointernet")

                val isNoInternetOn = SharePrefUtils.getBoolean(context, "NO_INTER_NET_ON", false)

                if (!haveNetworkConnection() && !isNoInternetOn) {
                    Log.e("No_internet_start", "show")
                    SharePrefUtils.putBoolean(context, "NO_INTER_NET_ON", true)
//                    launchActivity<NoInternetActivity> { }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
//        if (this !is NoInternetActivity && this !is SplashActivity){
//            unregisterReceiver(internetBroadcast)
//        }
        unregisterReceiver(networkReceiver)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        LanguageUtil.setLocale(this)
        super.onCreate(savedInstanceState)
        binding = setViewBinding
        setContentView(binding.root)
        initView()
        viewListener()
        dataObservable()

//        if (this !is NoInternetActivity && this !is SplashActivity){
//            val intent = IntentFilter()
//            intent.addAction("android.net.conn.CONNECTIVITY_CHANGE")
//            intent.addAction("android.net.wifi.WIFI_STATE_CHANGED")
//            registerReceiver(internetBroadcast, intent)
//        }


        networkReceiver = NetworkReceiver {
            if (!it) {
                showActivityCustom(NoInternetActivity::class.java, null)
            } else {
                if (this is NoInternetActivity && intent?.extras?.getString(SCREEN) != SPLASH_ACTIVITY) {
                    finish()
                } else if (this is NoInternetActivity && intent?.extras?.getString(SCREEN) == SPLASH_ACTIVITY) {
                    val myIntent = Intent(this, SplashActivity::class.java)
                    myIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(myIntent)
                }
            }
        }
        registerReceiver(networkReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }
    fun showActivity(activity: Class<*>, bundle: Bundle?) {
        val intent = Intent(this, activity)
        intent.putExtras(bundle ?: Bundle())
        startActivity(intent)
    }
    fun showActivityCustom(activity: Class<*>, bundle: Bundle?) {
        val intent = Intent(this, activity)
        intent.putExtras(bundle ?: Bundle())
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
    }
    override fun onResume() {
        super.onResume()

        if (SharePrefUtils.getBoolean(this, Constant.IS_HIDE_NAV,true)){
            window?.hideNavigation()
        }else window?.showNav()
    }



    fun showDialogExit() {
//        val dialogExitApp = DialogQuestion(this, object : IBaseListener {
//            override fun onExit() {
////                dialogExitApp.dismiss()
//                finishAffinity()
//            }
//
//            override fun onCancel() {
////                if (dialogExitApp.isShowing()) {
////                    dialogExitApp.dismiss()
////                }
//            }
//        }, DialogQuestion.QuestionType.QUIT_APP)
//        dialogExitApp.getWindow()?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
//        dialogExitApp.show()
    }


    fun showRateDialog(isFinish: Boolean, reateSuccess: (() -> Unit)) {
        val ratingDialog =
            RatingDialog(
                this
            )
        ratingDialog.init(object : RatingDialog.OnPress {
            override fun send(star: Float) {
                ratingDialog.dismiss()
//                val uriText =
//                    """
//                 mailto:${SharePrefUtils.email},${SharePrefUtils.email1}?subject=${SharePrefUtils.subject}&body=Rate : ${ratingDialog.rating}
//                 Content:
//                 """.trimIndent()
//                val uri = Uri.parse(uriText)
//                val sendIntent = Intent(Intent.ACTION_SENDTO)
//                sendIntent.data = uri
//                try {
                if (isFinish) finishAffinity()
//                    SharePrefUtils.forceRated(this@BaseActivity)
//                    startActivity(Intent.createChooser(sendIntent, getString(R.string.Send_Email)))
//                } catch (ex: ActivityNotFoundException) {
//                    Toast.makeText(
//                        this@BaseActivity,
//                        getString(R.string.There_is_no),
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
                Toast.makeText(
                    this@BaseActivity,
                    getString(R.string.rate_success),
                    Toast.LENGTH_SHORT
                ).show()
                SharePrefUtils.forceRated(this@BaseActivity)
                LauncherSharePrefUtils.forceRated(this@BaseActivity)
                reateSuccess.invoke()
            }

            override fun rating(star: Float) {
                manager = ReviewManagerFactory.create(this@BaseActivity)
                val request: Task<ReviewInfo> = manager.requestReviewFlow()
                request.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        reviewInfo = task.result
                        val flow: Task<Void> =
                            manager.launchReviewFlow(this@BaseActivity, reviewInfo)
                        flow.addOnSuccessListener {
                            SharePrefUtils.forceRated(this@BaseActivity)
                            LauncherSharePrefUtils.forceRated(this@BaseActivity)
                            ratingDialog.dismiss()
                            reateSuccess.invoke()
                            if (isFinish) finishAffinity()
                        }
                    } else {
                        ratingDialog.dismiss()
                        if (isFinish) finishAffinity()
                    }
                }
            }

            override fun cancel() {}
            override fun later() {
                ratingDialog.dismiss()
                if (isFinish) finishAffinity()
            }
        })
        try {
            ratingDialog.show()
        } catch (e: BadTokenException) {
            e.printStackTrace()
        }
    }


    fun showInterSave(action: () -> Unit) {
        action.invoke()
    }

}