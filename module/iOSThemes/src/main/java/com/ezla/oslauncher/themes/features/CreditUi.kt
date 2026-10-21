package com.ezla.oslauncher.themes.features

import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.data.CreditRepository
import kotlinx.coroutines.launch

// Nạp số dư từ server vào badge xu; lỗi (mất mạng...) thì báo lỗi, giữ nguyên số đang hiện.
// Fragment phải gọi qua viewLifecycleOwner để không cập nhật view đã huỷ.
fun LifecycleOwner.loadCreditInto(badge: TextView) {
    lifecycleScope.launch {
        CreditRepository.getCredit()
            .onSuccess { badge.text = it.toString() }
            .onFailure {
                Toast.makeText(badge.context, R.string.themes_network_error, Toast.LENGTH_SHORT).show()
            }
    }
}
