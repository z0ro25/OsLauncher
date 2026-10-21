package com.ezla.oslauncher.beautylauncher.Features.general.changeicon

import ads_mobile_sdk.fa
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.ezla.oslauncher.beautylauncher.Base.BaseActivity
import com.ezla.oslauncher.beautylauncher.R
import com.ezla.oslauncher.beautylauncher.Features.general.changeicon.data.ChangeAppIconRepository
import com.ezla.oslauncher.beautylauncher.extensions.tap
import com.ezla.oslauncher.beautylauncher.databinding.ActivityChangeIconDetailBinding

class ChangeIconDetailActivity : BaseActivity<ActivityChangeIconDetailBinding>() {

    override val setViewBinding: ActivityChangeIconDetailBinding
        get() = ActivityChangeIconDetailBinding.inflate(layoutInflater)

    private val repo by lazy { ChangeAppIconRepository(this) }

    private var component: String = ""
    private var packageName: String = ""
    private var label: String = ""

    /** Mở gallery (ảnh) qua Photo Picker (GetContent) — không cần quyền READ_MEDIA. */
    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) applyGalleryIcon(uri)
        }

    override fun onResume() {
        super.onResume()
        showBannerIfEnabled(binding.frBanner, false)
    }

    override fun initView() {
        component = intent.getStringExtra(EXTRA_COMPONENT).orEmpty()
        packageName = intent.getStringExtra(EXTRA_PACKAGE).orEmpty()
        label = intent.getStringExtra(EXTRA_LABEL).orEmpty()

        binding.tvTitle.text = label
        binding.tvAppLabel.text = label
        binding.ivAppIcon.setImageDrawable(loadCurrentIcon())
    }

    override fun viewListener() {
        binding.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.btnGallery.tap { pickImage.launch("image/*") }

        binding.tvReset.tap {
            repo.resetIcon(component)
            binding.ivAppIcon.setImageDrawable(loadSystemIcon())
        }
    }

    override fun dataObservable() {}

    /** Áp icon từ ảnh gallery: copy + broadcast engine, rồi cập nhật preview từ chính ảnh đã chọn. */
    private fun applyGalleryIcon(uri: Uri) {
        val ok = repo.setIconFromGallery(component, uri)
        if (ok) {
            try {
                contentResolver.openInputStream(uri).use { input ->
                    binding.ivAppIcon.setImageDrawable(Drawable.createFromStream(input, uri.toString()))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            Toast.makeText(this, getString(R.string.change_icon_failed), Toast.LENGTH_SHORT).show()
        }
    }

    /** Icon hiện tại để preview: ưu tiên ảnh custom đã lưu, ngược lại icon gốc hệ thống. */
    private fun loadCurrentIcon(): Drawable? =
        repo.customIconFile(component)?.let { Drawable.createFromPath(it.absolutePath) } ?: loadSystemIcon()

    /** Icon gốc hệ thống của app (dùng khi chưa có custom + sau Reset). */
    private fun loadSystemIcon(): Drawable? = try {
        packageManager.getApplicationIcon(packageName)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    companion object {
        const val EXTRA_COMPONENT = "extra_component"
        const val EXTRA_PACKAGE = "extra_package"
        const val EXTRA_LABEL = "extra_label"
    }
}
