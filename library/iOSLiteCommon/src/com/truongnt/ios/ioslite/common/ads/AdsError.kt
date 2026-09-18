package com.truongnt.ios.ioslite.common.ads

/**
 * Lỗi trả về từ mọi callback trong package `ads`.
 *
 * Thay cho IOSAdError cũ, giữ nguyên hai trường code/message để call-site migrate
 * không phải đổi cách đọc lỗi.
 */
class AdsError(val code: Int, description: String?) {

    /** Thông điệp lỗi — không bao giờ rỗng. */
    val message: String = if (description.isNullOrBlank()) DEFAULT_MESSAGE else description

    override fun toString(): String = "AdsError(code=$code, message=$message)"

    companion object {
        private const val DEFAULT_MESSAGE = "unknown error"

        // Mã lỗi do chính tầng `ads` sinh ra (không phải từ SDK).
        /** Slot bị tắt, user đã mua bản không quảng cáo, hoặc policy tần suất chặn. */
        const val CODE_BLOCKED = -1

        /** Chưa có quảng cáo nào được tải sẵn. */
        const val CODE_NOT_READY = -2

        /** Không tìm được Activity từ Context để hiển thị. */
        const val CODE_NO_ACTIVITY = -3

        /** SDK báo hiển thị thất bại. */
        const val CODE_SHOW_FAILED = -4
    }
}
