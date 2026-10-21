package com.ezla.oslauncher.beautylauncher.dialog

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.ezla.oslauncher.beautylauncher.Features.subs.SubsAct
import com.ezla.oslauncher.beautylauncher.R
import com.ezla.oslauncher.beautylauncher.databinding.DialogSubsOfferBinding
import com.ezla.oslauncher.beautylauncher.utils.BillingUtils
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.Locale

/**
 * Dialog ưu đãi sub: chỉ bán gói NĂM, giá gốc gạch ngang = giá bán × 2.
 * Đếm ngược 5 phút rồi tự đóng. Chỉ mở từ [SubsOfferPromptActivity] (desktop launcher, 1 lần/ngày).
 */
class SubsOfferDialog : BottomSheetDialogFragment() {

    private var binding: DialogSubsOfferBinding? = null
    private var countDown: CountDownTimer? = null

    /** Gọi khi dialog đóng (nút X, tap ngoài, hết giờ) — host dùng để finish. */
    var onDismissed: (() -> Unit)? = null

    // Giữ tham chiếu lambda để gỡ ĐÚNG callback mình đã đặt — không xoá callback của màn khác (SubsAct).
    private val onProductsLoaded: () -> Unit = { view?.post { bindPrices() } }
    private val onSubsChanged: (Boolean) -> Unit = { isSubs -> if (isSubs) dismissAllowingStateLoss() }

    override fun getTheme(): Int = R.style.bottomSheetFragment

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogSubsOfferBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onStart() {
        super.onStart()
        // Bỏ nền + bo góc mặc định của BottomSheet để lộ đúng nền vẽ trong layout (giống RatingDialog).
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.setBackgroundColor(Color.TRANSPARENT)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.apply {
            btnClose.setOnClickListener { dismiss() }
            btnSubscribe.setOnClickListener { subscribe() }
            tvTc.setOnClickListener { openWeb(SubsAct.URL_TERMS) }
            tvPrivacy.setOnClickListener { openWeb(SubsAct.URL_PRIVACY) }
            // Gạch ngang giá gốc: không đặt được bằng thuộc tính XML nên set ở đây.
            tvOriginalPrice.paintFlags = tvOriginalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        }

        applyPercentGradient()
        bindPrices()
        startCountdown()

        BillingUtils.onProductsLoaded = onProductsLoaded
        BillingUtils.onSubsChanged = onSubsChanged
    }

    /**
     * "50%" tô gradient ngang. Shader tính theo bề rộng CHỮ nên phải bù offset căn giữa,
     * không thì dải màu lệch hẳn về một bên.
     */
    private fun applyPercentGradient() {
        val tv = binding?.tvPercent ?: return
        tv.post {
            val textWidth = tv.paint.measureText(tv.text.toString())
            val start = (tv.width - textWidth) / 2f
            tv.paint.shader = LinearGradient(
                start, 0f, start + textWidth, 0f,
                ContextCompat.getColor(tv.context, R.color.subs_offer_gradient_start),
                ContextCompat.getColor(tv.context, R.color.subs_offer_gradient_end),
                Shader.TileMode.CLAMP
            )
        }
    }

    // Giá bán = gói NĂM (dialog chỉ bán gói năm); giá gốc = giá bán × 2.
    // Chưa có ProductDetails (productId còn rỗng / billing chưa xong) thì GIỮ placeholder trong layout.
    private fun bindPrices() {
        val b = binding ?: return
        BillingUtils.formattedPrice(BillingUtils.Yearly)?.let { b.tvSalePrice.text = it }
        BillingUtils.formattedOriginalPrice(BillingUtils.Yearly)?.let { b.tvOriginalPrice.text = it }
    }

    private fun startCountdown() {
        countDown = object : CountDownTimer(OFFER_DURATION_MS, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                binding?.apply {
                    tvMinute.text = String.format(Locale.US, "%02d", seconds / 60)
                    tvSecond.text = String.format(Locale.US, "%02d", seconds % 60)
                }
            }

            // Hết giờ tự đóng; lần mở sau lại đếm từ đầu vì dialog dựng mới mỗi lần.
            override fun onFinish() {
                dismissAllowingStateLoss()
            }
        }.start()
    }

    private fun subscribe() {
        val product = BillingUtils.Yearly
        if (product == null) {
            // Chưa có ProductDetails: chưa điền productId, hoặc billing chưa query xong.
            Toast.makeText(requireContext(), R.string.subs_product_not_ready, Toast.LENGTH_SHORT).show()
        } else {
            BillingUtils.launchPurchaseSubs(requireActivity(), product)
        }
    }

    private fun openWeb(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissed?.invoke()
    }

    override fun onDestroyView() {
        countDown?.cancel()
        countDown = null
        // Gỡ callback global kẻo giữ tham chiếu dialog đã chết — chỉ gỡ khi đúng callback của mình.
        if (BillingUtils.onProductsLoaded == onProductsLoaded) BillingUtils.onProductsLoaded = null
        if (BillingUtils.onSubsChanged == onSubsChanged) BillingUtils.onSubsChanged = null
        binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "SubsOfferDialog"

        /** Thời gian đếm ngược của offer, khớp mockup 05:00. */
        private const val OFFER_DURATION_MS = 5 * 60 * 1000L
    }
}
