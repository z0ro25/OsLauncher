package com.oslauncher.applauncher.themelauncher.views.wallpaperTool

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.oslauncher.applauncher.themelauncher.R
import com.oslauncher.applauncher.themelauncher.databinding.ViewWallpaperToolBinding
import com.oslauncher.applauncher.themelauncher.utils.YourWallpaperDataManager

class WallpaperToolView(context: Context, attrs: AttributeSet?) :
    ConstraintLayout(context, attrs) {
    var binding: ViewWallpaperToolBinding? = null
    var type = ToolType.PAIR
    var isAddGrad = false

    var onViewClick: (() -> Unit)? = null
    var onAddGradientClick: (() -> Unit)? = null
    var onAddColorClick: (() -> Unit)? = null
    var onAddPhotoClick: (() -> Unit)? = null

    var photoBm : Bitmap ? = null
    var gradientBm : Bitmap ? = null

    fun setPair(value: Bitmap) {
        photoBm = value
        binding?.ivTool?.imageTintList = null
        binding?.ivTool?.setImageBitmap(value)
    }

    fun setGradient(value: Bitmap){
        gradientBm = value
        binding?.ivTool?.setImageBitmap(value)
    }

    init {
        binding = ViewWallpaperToolBinding.inflate(LayoutInflater.from(context), this, true)
        val defstyle = context.obtainStyledAttributes(attrs, R.styleable.WallpaperToolView)
        val t = defstyle.getInt(R.styleable.WallpaperToolView_toolType, 1)

        type = ToolType.values().lastOrNull { it.ordinal == t } ?: ToolType.PAIR

        binding?.apply {
            when (type) {
                ToolType.BLUR -> {
                    ivTool.setImageResource(R.drawable.ic_blur)
                    tvName.text = context.getString(R.string.blur)
                }

                ToolType.PAIR -> {
                    ivTool.setImageResource(R.drawable.bg_hello)
                    tvName.text = context.getString(R.string.pair)
                }

                ToolType.COLOR -> {
                    ivTool.setImageDrawable(ColorDrawable(Color.parseColor("#1BADF8")))
                    tvName.text = context.getString(R.string.color)
                }

                ToolType.GRADIENT -> {
                    val bm = YourWallpaperDataManager.createGradientBitmap(
                        40,
                        40,
                        Color.parseColor("#94EDFA"),
                        Color.parseColor("#6B57F5")
                    )
                    ivTool.setImageBitmap(bm)
                    tvName.text = context.getString(R.string.gradient)
                }

                ToolType.PHOTO -> {
                    ivTool.setImageResource(R.drawable.ic_tool_photos)
                    tvName.text = context.getString(R.string.photos)
                }
            }


            ivTool.setOnClickListener {
                setlectView()
                onViewClick?.invoke()
            }
        }

        defstyle.recycle()
    }

    fun setlectView() {
        binding?.apply {
            when (type) {
                ToolType.BLUR -> {
                    ivSelect.isVisible = false
                    unScaleView()
                    ivTool.imageTintList = context.getColorStateList(R.color.color_FF8C21)
                }

                ToolType.PAIR -> {
                    scaleView()
                    ivSelect.isVisible = true
                }

                ToolType.COLOR -> {
                    scaleView()
                    ivSelect.isVisible = true
                    ivAddGrad.isVisible = true
                    if (!isAddGrad) {
                        isAddGrad = true
                    }else{
                        onAddColorClick?.invoke()
                    }
                }

                ToolType.GRADIENT -> {
                    scaleView()
                    ivSelect.isVisible = true
                    ivAddGrad.isVisible = true
                    if (!isAddGrad) {
                        isAddGrad = true
                    }else{
                        onAddGradientClick?.invoke()
                    }
                }

                ToolType.PHOTO -> {
                    if (photoBm == null){
                        ivSelect.isVisible = false
                        ivTool.imageTintList = context.getColorStateList(R.color.color_FF8C21)
                        unScaleView()
                        onAddPhotoClick?.invoke()
                    }else{
                        ivAddGrad.isVisible = true
                        scaleView()
                        ivTool.imageTintList = null
                        ivTool.setImageBitmap(photoBm)
                        ivSelect.isVisible = true
                        if (!isAddGrad) {
                            isAddGrad = true
                        }else{
                            onAddPhotoClick?.invoke()
                        }
                    }
                }
            }
        }

    }


    fun scaleView() {
        val scaleX = ObjectAnimator.ofFloat(binding?.ivTool, View.SCALE_X, 1.0f, 0.7f)
        val scaleY = ObjectAnimator.ofFloat(binding?.ivTool, View.SCALE_Y, 1.0f, 0.7f)
        scaleX.duration = 100
        scaleY.duration = 100

        scaleX.start()
        scaleY.start()
    }

    fun unScaleView() {
        val scaleX = ObjectAnimator.ofFloat(binding?.ivTool, View.SCALE_X, 1.0f, 1.0f)
        val scaleY = ObjectAnimator.ofFloat(binding?.ivTool, View.SCALE_Y, 1.0f, 1.0f)
        scaleX.duration = 100
        scaleY.duration = 100

        scaleX.start()
        scaleY.start()
    }

    fun unSelectView() {
        binding?.apply {
            when (type) {
                ToolType.BLUR -> {
                    ivSelect.isVisible = false
                    unScaleView()
                    ivTool.imageTintList = context.getColorStateList(R.color.white)
                }

                ToolType.PHOTO -> {
                    ivSelect.isVisible = false
                    ivAddGrad.isVisible = false
                    if (photoBm == null){
                        ivTool.imageTintList = context.getColorStateList(R.color.white)
                    }else{
                        isAddGrad = false
                    }
                    unScaleView()
                }

                ToolType.GRADIENT, ToolType.COLOR ->{
                    unScaleView()
                    ivSelect.isVisible = false
                    ivAddGrad.isVisible = false
                    isAddGrad = false
                }

                else -> {
                    unScaleView()
                    ivSelect.isVisible = false
                }
            }
        }
    }

    enum class ToolType() {
        BLUR, PAIR, COLOR, GRADIENT, PHOTO
    }
}