package com.oslauncher.applauncher.themelauncher.Features.general.transitionpage

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.widget.TextView
import androidx.activity.addCallback
import com.oslauncher.applauncher.themelauncher.Base.BaseActivity
import com.oslauncher.applauncher.themelauncher.R
import com.oslauncher.applauncher.themelauncher.databinding.ActivityPageTransitionBinding
import com.oslauncher.applauncher.themelauncher.extensions.tap
import com.amz.ios.launcher.PagedView.PageAnimationType
import com.amz.ios.utils.LauncherInteractor


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
            ivBack.tap {
                onBackPressedDispatcher.onBackPressed()
            }

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
            unselectAnim(llAnimDefalt, tvAnimDefault)
            unselectAnim(llAnimRotate, tvAnimRotate)
            unselectAnim(llAnimFlip, tvAnimFlip)
            unselectAnim(llAnimZoom, tvAnimZoom)

            when (post) {
                0 -> {
                    selectAnim(llAnimDefalt, tvAnimDefault)
                    animType = PageAnimationType.NONE
                }

                1 -> {
                    selectAnim(llAnimRotate, tvAnimRotate)
                    animType = PageAnimationType.ROTATE
                }

                2 -> {
                    selectAnim(llAnimFlip, tvAnimFlip)
                    animType = PageAnimationType.FLIP
                }

                3 -> {
                    selectAnim(llAnimZoom, tvAnimZoom)
                    animType = PageAnimationType.ZOOM_IN
                }
            }
        }
    }

    /** Nút đang chọn: pill trắng (pt_seg_selected) + chữ tối (pt_seg_text_selected). */
    fun selectAnim(cell: View, tv: TextView) {
        cell.setBackgroundResource(R.drawable.pt_seg_selected)
        tv.setTextColor(getColorStateList(R.color.pt_seg_text_selected))
    }

    /** Nút thường: không nền, chữ theo token tiêu đề (đổi theo light/dark). */
    fun unselectAnim(cell: View, tv: TextView) {
        cell.background = null
        tv.setTextColor(getColorStateList(R.color.onb_text_primary))
    }


    /**
     * Reset toàn bộ transform về mặc định: anim1 = trang hiện tại (giữa), anim2 = trang mới (nằm bên
     * phải, ngoài màn). Gọi TRƯỚC mỗi lần chạy để click lại nhiều lần đều thấy chuyển động, không bị
     * kẹt trạng thái của lần trước. width lấy tại thời điểm chạy (đã đo xong nhờ post{}).
     */
    private fun ActivityPageTransitionBinding.resetPreview(width: Float) {
        ivPageAnim1.translationX = 0f
        ivPageAnim2.translationX = width + previewGapPx()
        for (v in arrayOf(ivPageAnim1, ivPageAnim2)) {
            v.visibility = View.VISIBLE // bật lại để replay được sau khi lần trước đã ẩn page1
            v.translationY = 0f
            v.rotation = 0f
            v.rotationY = 0f
            v.scaleX = 1f
            v.scaleY = 1f
        }
    }

    /** Khoảng cách giữa 2 page khi trượt (24dp). */
    private fun previewGapPx(): Float = 24f * resources.displayMetrics.density

    /** Bọc post{} để chắc chắn view đã đo xong (width thật) rồi mới chạy animation. */
    private fun playPreview(block: ActivityPageTransitionBinding.(Float) -> Unit) {
        binding.ivPageAnim1.post {
            binding.apply {
                val width = ivPageAnim1.width.toFloat()
                resetPreview(width)
                block(width)
            }
        }
    }

    /**
     * Trượt trang: anim1 (giữa) ra trái, anim2 (phải, cách anim1 một khoảng gap) vào giữa.
     * Nền cho mọi hiệu ứng. Kết thúc: ẩn page1 để chỉ còn trang mới (giống chuyển trang thật).
     */
    private fun ActivityPageTransitionBinding.slidePages(width: Float) {
        val gap = previewGapPx()
        ObjectAnimator.ofFloat(ivPageAnim1, "translationX", 0f, -(width + gap)).apply {
            duration = 1000
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    ivPageAnim1.visibility = View.INVISIBLE
                }
            })
            start()
        }
        ObjectAnimator.ofFloat(ivPageAnim2, "translationX", width + gap, 0f).apply {
            duration = 1000
            start()
        }
    }

    private fun startDefaultTransition() = playPreview { width ->
        slidePages(width)
    }

    private fun startRotateAnimation() = playPreview { width ->
        slidePages(width)
        ObjectAnimator.ofFloat(ivPageAnim1, "rotation", 0f, 360f).apply { duration = 1000; start() }
        ObjectAnimator.ofFloat(ivPageAnim2, "rotation", 0f, 360f).apply { duration = 1000; start() }
    }

    private fun startFlipAnim() = playPreview { width ->
        slidePages(width)
        ObjectAnimator.ofFloat(ivPageAnim1, "rotationY", 0f, 180f).apply { duration = 1000; start() }
        ObjectAnimator.ofFloat(ivPageAnim2, "rotationY", -180f, 0f).apply { duration = 1000; start() }
    }

    private fun startZoomInAnim() = playPreview { width ->
        slidePages(width)

        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(ivPageAnim1, "scaleX", 1f, 1.5f),
                ObjectAnimator.ofFloat(ivPageAnim1, "scaleY", 1f, 1.5f),
            )
            duration = 1000
            start()
        }
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(ivPageAnim2, "scaleX", 1.5f, 1f),
                ObjectAnimator.ofFloat(ivPageAnim2, "scaleY", 1.5f, 1f),
            )
            duration = 1000
            start()
        }
    }
}