package com.ezla.oslauncher.beautylauncher.ads

import com.ezla.oslauncher.beautylauncher.BuildConfig
import com.google.android.gms.tasks.Task
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Phần Firebase của AppAds: ở :app vì iOSLiteCommon không được phụ thuộc Firebase.
object AppAdsRemote {
    // Parameter String trên Remote Config chứa toàn bộ JSON placement.
    const val REMOTE_KEY = "ad_placements"

    // Truyền vào AppAds.initialize; timeout toàn luồng do StartupAdsConfig giới hạn.
    suspend fun fetchPlacements(): String {
        val remote = FirebaseRemoteConfig.getInstance()
        remote.ensureInitialized().awaitResult()
        remote.setConfigSettingsAsync(FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0 else 3_600)
            .setFetchTimeoutInSeconds(4)
            .build()).awaitResult()
        // false không phải lỗi: Firebase có thể đang dùng dữ liệu đã activate trước đó.
        remote.fetchAndActivate().awaitResult()
        return remote.getString(REMOTE_KEY)
    }
}

// Bỏ callback đến muộn khi tác vụ chờ đã bị huỷ/timeout.
private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (!continuation.isActive) return@addOnCompleteListener
        when {
            task.isCanceled -> continuation.resumeWithException(IllegalStateException("Firebase task was cancelled"))
            task.isSuccessful -> continuation.resume(task.result)
            else -> continuation.resumeWithException(task.exception ?: IllegalStateException("Firebase task failed"))
        }
    }
}
