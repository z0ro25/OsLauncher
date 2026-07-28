package com.oslauncher.applauncher.themelauncher.dialog

import android.animation.ObjectAnimator
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.oslauncher.applauncher.themelauncher.R
import com.oslauncher.applauncher.themelauncher.databinding.DialogColorPickerBinding
import com.oslauncher.applauncher.themelauncher.databinding.ViewholderGradientBinding
import com.oslauncher.applauncher.themelauncher.model.ColorModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ColorPickerDialog : BottomSheetDialogFragment() {
    var binding: DialogColorPickerBinding? = null
    var listColor: ArrayList<ColorModel> = arrayListOf()
    val adapter: ColorAdapter by lazy { ColorAdapter(listColor) }
    var onColorPick: ((Int) -> Unit)? = null
    var selectedColor: Int = Color.parseColor("#FFEBA8")
    var currentOpacity = 255

    companion object {
        fun newInstance(color: String): ColorPickerDialog {
            val args = Bundle()
            args.putString("CurrentColor", color)
            val fragment = ColorPickerDialog()
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
        binding = DialogColorPickerBinding.inflate(layoutInflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.apply {
            listColor.clear()
            listColor.add(ColorModel("#FFEBA8"))
            listColor.add(ColorModel("#8BD9ED"))
            listColor.add(ColorModel("#4A9DE1"))
            listColor.add(ColorModel("#7C8FDE"))
            listColor.add(ColorModel("#7E62AD"))
            listColor.add(ColorModel("#C761A3"))
            listColor.add(ColorModel("#EB5B41"))
            listColor.add(ColorModel("#F1A23B"))
            listColor.add(ColorModel("#FFD833"))
            listColor.add(ColorModel("#A4B469"))
            listColor.add(ColorModel("#69A366"))
            listColor.add(ColorModel("#61A29C"))
            listColor.add(ColorModel("#DED8D8"))
            listColor.add(ColorModel("#95A0A4"))
            listColor.add(ColorModel("#858176"))
            listColor.add(ColorModel("#D1B48A"))
            listColor.add(ColorModel("#785128"))
            listColor.add(ColorModel("#000000"))

            val current = arguments?.getString("CurrentColor")
            selectedColor = Color.parseColor(current)
            listColor.forEach {
                val colorPath = Color.parseColor(it.color)

                it.isSelected =
                    colorPath.red == selectedColor.red && colorPath.green == selectedColor.green && colorPath.blue == selectedColor.blue
            }

            rcvColor.adapter = adapter
            ivClose.setOnClickListener {
                dismiss()
                onColorPick?.invoke(selectedColor)
            }
            adapter.onColorPicked = { model ->
                listColor.forEachIndexed { index, colorModel ->
                    colorModel.isSelected = colorModel.color == model.color
                    adapter.notifyItemChanged(index)
                }
                val newColor = Color.parseColor(model.color)
                selectedColor = Color.argb(
                    currentOpacity,
                    newColor.red,
                    newColor.green,
                    newColor.blue
                )

            }

            currentOpacity = selectedColor.alpha
            sbOpacity.progress = currentOpacity

            sbOpacity.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    currentOpacity = progress
                    selectedColor = Color.argb(
                        currentOpacity,
                        selectedColor.red,
                        selectedColor.green,
                        selectedColor.blue
                    )
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }

    }


    class ColorAdapter(val listColors: List<ColorModel>) : Adapter<ColorViewHolder>() {

        var onColorPicked: ((ColorModel) -> Unit)? = null

        override fun onCreateViewHolder(parent: ViewGroup, p1: Int): ColorViewHolder {
            return ColorViewHolder(
                ViewholderGradientBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }

        override fun getItemCount(): Int = listColors.size

        override fun onBindViewHolder(viewholder: ColorViewHolder, post: Int) {
            val data = listColors[post]
            viewholder.binding.apply {
                ivColor.setImageDrawable(ColorDrawable(Color.parseColor(data.color)))

                ivSelect.isVisible = data.isSelected
                if (data.isSelected) {
                    viewholder.scaleView()
                } else viewholder.unScaleView()

                root.setOnClickListener {
                    onColorPicked?.invoke(data)
                }
            }
        }

    }


    class ColorViewHolder(val binding: ViewholderGradientBinding) :
        RecyclerView.ViewHolder(binding.root) {

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