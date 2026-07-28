package com.oslauncher.applauncher.themelauncher.dialog

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.oslauncher.applauncher.themelauncher.R
import com.oslauncher.applauncher.themelauncher.databinding.DialogGradientPickerBinding
import com.oslauncher.applauncher.themelauncher.databinding.ViewholderGradientBinding
import com.oslauncher.applauncher.themelauncher.extensions.serializable
import com.oslauncher.applauncher.themelauncher.model.GradientColorModel
import com.oslauncher.applauncher.themelauncher.utils.YourWallpaperDataManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class GradientColorPickerDialog : BottomSheetDialogFragment() {
    var binding: DialogGradientPickerBinding? = null
    var listGradientColors: ArrayList<GradientColorModel> = arrayListOf()
    val adapter: GradientAdapter by lazy { GradientAdapter(requireContext(), listGradientColors) }
    var onColorPicked: ((GradientColorModel, Bitmap?) -> Unit)? = null

    companion object {
        fun newInstance(color: GradientColorModel): GradientColorPickerDialog {
            val args = Bundle()
            args.putSerializable("CURRENT_COLOR", color)
            val fragment = GradientColorPickerDialog()
            fragment.arguments = args
            return fragment
        }
    }

    override fun getTheme(): Int {
        return R.style.bottomSheetFragment
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogGradientPickerBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.apply {
            listGradientColors.add(GradientColorModel("#94EDFA", "#6B57F5"))
            listGradientColors.add(GradientColorModel("#FA4545", "#F57073"))
            listGradientColors.add(GradientColorModel("#FF0845", "#FFB099"))
            listGradientColors.add(GradientColorModel("#FF8C21", "#FAC74D"))
            listGradientColors.add(GradientColorModel("#FF8C21", "#F5576E"))
            listGradientColors.add(GradientColorModel("#F5ED47", "#80F5E8"))
            listGradientColors.add(GradientColorModel("#EBC43D", "#FFD18F"))
            listGradientColors.add(GradientColorModel("#D4FC79", "#96E6A1"))
            listGradientColors.add(GradientColorModel("#2AF598", "#009EFD"))
            listGradientColors.add(GradientColorModel("#0538FF", "#70E3F5"))
            listGradientColors.add(GradientColorModel("#0538FF", "#40FFC7"))
            listGradientColors.add(GradientColorModel("#A6E8FF", "#B280F5"))
            listGradientColors.add(GradientColorModel("#CCFFA6", "#B280F5"))
            listGradientColors.add(GradientColorModel("#3D73EB", "#DE8FFF"))
            listGradientColors.add(GradientColorModel("#A6BFFF", "#F68084"))
            listGradientColors.add(GradientColorModel("#D9A6FF", "#F68084"))
            listGradientColors.add(GradientColorModel("#F3F3F3", "#FAEBC2"))
            listGradientColors.add(GradientColorModel("#F3F3F3", "#FAD0C4"))

            val currentcolor = arguments?.serializable<GradientColorModel>("CURRENT_COLOR")
            listGradientColors.forEach {
                it.isSelected =
                    it.startColor.equals(currentcolor?.startColor) && it.endColor.equals(
                        currentcolor?.endColor
                    )
            }

            rcvGradient.adapter = adapter

            adapter.onGradientSelect = { color, bm ->
                listGradientColors.forEach {
                    it.isSelected =
                        it.startColor.equals(color.startColor) && it.endColor.equals(color.endColor)
                }
                adapter.notifyDataSetChanged()

                onColorPicked?.invoke(color, bm)
                dismiss()
            }

            ivClose.setOnClickListener {
                dismiss()
            }
        }

    }


    class GradientAdapter(val context: Context, val colors: List<GradientColorModel>) :
        Adapter<ViewHolder>() {
        var onGradientSelect: ((GradientColorModel, Bitmap?) -> Unit)? = null

        override fun onCreateViewHolder(parent: ViewGroup, type: Int): ViewHolder {
            return GradientViewHolder(
                ViewholderGradientBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }

        override fun getItemCount(): Int = colors.size

        override fun onBindViewHolder(viewholder: ViewHolder, post: Int) {
            val data = colors[post]
            val vhd = viewholder as GradientViewHolder
            vhd.binding.apply {
                val startColor = Color.parseColor(data.startColor)
                val endColor = Color.parseColor(data.endColor)
                var bm: Bitmap? = null
                CoroutineScope(Dispatchers.IO).launch {

                    bm = YourWallpaperDataManager.createGradientBitmap(40, 40, startColor, endColor)
                    MainScope().launch {
                        ivColor.setImageBitmap(bm)
                    }
                }

                ivSelect.isVisible = data.isSelected
                if (data.isSelected) {
                    vhd.scaleView()
                } else vhd.unScaleView()

                root.setOnClickListener {
                    onGradientSelect?.invoke(data, bm)
                }

            }
        }

        class GradientViewHolder(val binding: ViewholderGradientBinding) :
            ViewHolder(binding.root) {

            fun scaleView() {
                val scaleX = ObjectAnimator.ofFloat(binding.ivColor, View.SCALE_X, 1.0f, 0.7f)
                val scaleY = ObjectAnimator.ofFloat(binding.ivColor, View.SCALE_Y, 1.0f, 0.7f)
                scaleX.duration = 100
                scaleY.duration = 100

                scaleX.start()
                scaleY.start()
            }

            fun unScaleView() {
                val scaleX = ObjectAnimator.ofFloat(binding.ivColor, View.SCALE_X, 1.0f, 1.0f)
                val scaleY = ObjectAnimator.ofFloat(binding.ivColor, View.SCALE_Y, 1.0f, 1.0f)
                scaleX.duration = 100
                scaleY.duration = 100

                scaleX.start()
                scaleY.start()
            }


        }

    }
}