package com.oslauncher.applauncher.themelauncher.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.oslauncher.applauncher.themelauncher.R
import com.oslauncher.applauncher.themelauncher.databinding.DialogWallpaperSelectOptionBinding
import com.oslauncher.applauncher.themelauncher.utils.YourWallpaperDataManager
import java.io.InputStream

class WallpaperSelectOptionDialog(context: Context,bm : Bitmap?) :
    Dialog(context) {
    var binding: DialogWallpaperSelectOptionBinding? = null
    var onSetWallpaper :((Bitmap) -> Unit)  ? = null
    var onCustomHome :((Bitmap) -> Unit)  ? = null
    var wallpaperBM = bm

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogWallpaperSelectOptionBinding.inflate(LayoutInflater.from(context))

        binding?.apply {
            setContentView(root)

            window?.apply {
                setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setGravity(Gravity.CENTER)
            }

            ivBgHome.setImageBitmap(wallpaperBM)
            ivBgLock.setImageBitmap(wallpaperBM)


            btnSetWallpaper.setOnClickListener {
                wallpaperBM?.let { it1 -> onSetWallpaper?.invoke(it1) }
            }

            btnCustomHome.setOnClickListener {
                wallpaperBM?.let { it1 ->  onCustomHome?.invoke(it1) }

            }
        }

    }

}