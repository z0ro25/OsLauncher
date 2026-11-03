package com.ezla.oslauncher.controlcenter.record

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import com.ezla.oslauncher.controlcenter.system.CcActions

// Activity trong suốt chỉ để xin consent MediaProjection (bắt buộc qua Activity), rồi đóng ngay.
class ScreenRecordStarter : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) return
        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        try {
            @Suppress("DEPRECATION")
            startActivityForResult(mpm.createScreenCaptureIntent(), REQ)
        } catch (t: Throwable) {
            finish()
        }
    }

    @Deprecated("Activity thường, không dùng ActivityResult API cho màn trong suốt 1 lần")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ && resultCode == RESULT_OK && data != null) {
            ScreenRecordService.start(this, resultCode, data)
        }
        finish()
        overridePendingTransition(0, 0)
    }

    companion object {
        private const val REQ = 7101

        fun start(context: Context) {
            CcActions.start(context, Intent(context, ScreenRecordStarter::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION))
        }
    }
}
