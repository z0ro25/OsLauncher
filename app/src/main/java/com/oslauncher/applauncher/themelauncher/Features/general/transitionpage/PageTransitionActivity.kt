package com.oslauncher.applauncher.themelauncher.Features.general.transitionpage

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Handler
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.addCallback
import com.oslauncher.applauncher.themelauncher.Base.BaseActivity
import com.oslauncher.applauncher.themelauncher.R
import com.oslauncher.applauncher.themelauncher.databinding.ActivityPageTransitionBinding
import com.oslauncher.applauncher.themelauncher.extensions.tap
import com.amz.ios.launcher.PagedView.PageAnimationType
import com.amz.ios.utils.LauncherInteractor
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class PageTransitionActivity : BaseActivity<ActivityPageTransitionBinding>() {
    override val setViewBinding: ActivityPageTransitionBinding
        get() = ActivityPageTransitionBinding.inflate(layoutInflater)
    var animType: PageAnimationType = PageAnimationType.NONE



    override fun initView() {
        onBackPressedDispatcher.addCallback {
            finish()
        }

        animType = LauncherInteractor.mPageAnimationType
        setCurrentAnim(animType.ordinal)
    }

    override fun viewListener() {
        binding.apply {
            llAnimDefalt.tap {
                setCurrentAnim(0)
                startDefaultTransition()
            }

            llAnimRotate.tap {
                setCurrentAnim(1)
                startRotateAnimation()
            }

            llAnimFlip.tap {
                setCurrentAnim(2)
                startFlipAnim()
            }

            llAnimZoom.tap {
                setCurrentAnim(3)
                startZoomInAnim()
            }

            btnSetPageTransition.tap {
                onBackPressedDispatcher.onBackPressed()
                LauncherInteractor.setPageTransitionAnim(animType)

            }
        }
    }


    override fun dataObservable() {

    }

    fun setCurrentAnim(post: Int) {
        binding.apply {
            unselectAnim(ivAnimDefault, tvAnimDefault)
            unselectAnim(ivAnimRotate, tvAnimRotate)
            unselectAnim(ivAnimFlip, tvAnimFlip)
            unselectAnim(ivAnimZoom, tvAnimZoom)

            when (post) {
                0 -> {
                    selectAnim(ivAnimDefault, tvAnimDefault)
                    animType = PageAnimationType.NONE
                }

                1 -> {
                    selectAnim(ivAnimRotate, tvAnimRotate)
                    animType = PageAnimationType.ROTATE
                }

                2 -> {
                    selectAnim(ivAnimFlip, tvAnimFlip)
                    animType = PageAnimationType.FLIP
                }

                3 -> {
                    selectAnim(ivAnimZoom, tvAnimZoom)
                    animType = PageAnimationType.ZOOM_IN
                }
            }
        }
    }

    fun selectAnim(iv: ImageView, tv: TextView) {
        iv.imageTintList = getColorStateList(R.color.color_FF8C21)
        tv.setTextColor(getColorStateList(R.color.color_FF8C21))
    }

    fun unselectAnim(iv: ImageView, tv: TextView) {
        iv.imageTintList = getColorStateList(R.color.color_4B4F58)
        tv.setTextColor(getColorStateList(R.color.color_4B4F58))
    }


    private fun ActivityPageTransitionBinding.startDefaultTransition() {
        val rotateAnimator1 = ObjectAnimator.ofFloat(ivPageAnim1, "translationX", -100f, -(ivPageAnim1.width).toFloat() - 100f)
        rotateAnimator1.duration = 1000 // duration in milliseconds
        rotateAnimator1.start()

        val rotateAnimator2 = ObjectAnimator.ofFloat(ivPageAnim2, "translationX", (ivPageAnim2.width).toFloat(), 0f)
        rotateAnimator2.duration = 1000 // duration in milliseconds
        rotateAnimator2.start()
    }


    private fun ActivityPageTransitionBinding.startRotateAnimation() {

        startDefaultTransition()

        val rotateAnimator1 = ObjectAnimator.ofFloat(ivPageAnim1, "rotation", 0f, 360f)
        rotateAnimator1.duration = 1000 // duration in milliseconds
        rotateAnimator1.start()

        val rotateAnimator2 = ObjectAnimator.ofFloat(binding.ivPageAnim2, "rotation", 0f, 360f)
        rotateAnimator2.duration = 1000 // duration in milliseconds
        rotateAnimator2.start()
    }

    private fun ActivityPageTransitionBinding.startFlipAnim() {
        startDefaultTransition()

        val set1 = ObjectAnimator.ofFloat(ivPageAnim1, "rotationY", 0f, 180f);
        set1.duration = 1000
        set1.start()

        val set2 = ObjectAnimator.ofFloat(ivPageAnim2, "rotationY", 0f, 180f);
        set2.duration = 1000
        set2.start()
    }

    private fun ActivityPageTransitionBinding.startZoomInAnim() {
        startDefaultTransition()

        val scaleXAnimator1 = ObjectAnimator.ofFloat(ivPageAnim1, "scaleX", 1f, 1.5f)
        val scaleYAnimator1 = ObjectAnimator.ofFloat(ivPageAnim1, "scaleY", 1f, 1.5f)

        val animatorSet1 = AnimatorSet()
        animatorSet1.playTogether(scaleXAnimator1, scaleYAnimator1)
        animatorSet1.setDuration(1000) // duration in milliseconds
        animatorSet1.start()

        val scaleXAnimator2 = ObjectAnimator.ofFloat(ivPageAnim2, "scaleX", 1.5f, 1f)
        val scaleYAnimator2 = ObjectAnimator.ofFloat(ivPageAnim2, "scaleY", 1.5f, 1f)

        val animatorSet2 = AnimatorSet()
        animatorSet2.playTogether(scaleXAnimator2, scaleYAnimator2)
        animatorSet2.setDuration(1000) // duration in milliseconds
        animatorSet2.start()

        MainScope().launch {
            delay(1000)
            val scaleXAnimator1 = ObjectAnimator.ofFloat(ivPageAnim1, "scaleX", 1f, 1f)
            val scaleYAnimator1 = ObjectAnimator.ofFloat(ivPageAnim1, "scaleY", 1f, 1f)

            val animatorSet1 = AnimatorSet()
            animatorSet1.playTogether(scaleXAnimator1, scaleYAnimator1)
            animatorSet1.setDuration(1) // duration in milliseconds
            animatorSet1.start()
        }
    }
}