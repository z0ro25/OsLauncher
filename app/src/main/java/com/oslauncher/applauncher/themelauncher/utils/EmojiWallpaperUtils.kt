package com.oslauncher.applauncher.themelauncher.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.util.Log
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

object EmojiWallpaperUtils {

    enum class EmojiWallpaperType {
        SMALL_GRID, MEDIUM_GRID, LARGE_GRID, RING, SPIRAL
    }

    fun generateGridWallpaper(emojis: List<String>, width: Int, height: Int): Bitmap? {
        if (emojis.isEmpty()) return null

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            textSize = 70f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

//        paint.setColor(Color.TRANSPARENT)

        val rect = Rect()
        paint.getTextBounds(emojis[0], 0, emojis[0].length, rect)
        val emojiWidth = rect.width()
        val emojiHeight = rect.height()

        val spacingFactor = 1.01f  // Increase this to make the grid larger
        val horizontalSpacing = emojiWidth * spacingFactor
        val verticalSpacing = emojiHeight * spacingFactor

        val horizontalCount = (width / horizontalSpacing).toInt() + 1
        val verticalCount = (height / verticalSpacing).toInt() + 1

        var emojiIndex = 0

        for (i in 0 until verticalCount) {
            for (j in 0 until horizontalCount) {
                val x = j * horizontalSpacing
                val y = i * verticalSpacing + rect.height()
                canvas.drawText(emojis[(emojiIndex + i) % emojis.size], x, y, paint)
                emojiIndex++
            }
        }

        return bitmap
    }

    fun generateMediumGridPattern(emojis: List<String>, width: Int, height: Int): Bitmap? {
        if (emojis.isEmpty()) return null

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            textSize = 100f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

//        paint.setColor(Color.TRANSPARENT)

        val rect = Rect()
        paint.getTextBounds(emojis[0], 0, emojis[0].length, rect)
        val emojiWidth = rect.width()
        val emojiHeight = rect.height()

        val spacingFactor = 1.2f  // Increase this to make the grid larger
        val horizontalSpacing = emojiWidth * spacingFactor
        val verticalSpacing = emojiHeight * spacingFactor

        val horizontalCount = (width / horizontalSpacing).toInt() + 1
        val verticalCount = (height / verticalSpacing).toInt() + 1

        var emojiIndex = 0

        for (i in 0 until verticalCount) {
            for (j in 0 until horizontalCount) {
                val x = j * horizontalSpacing
                val y = i * verticalSpacing + rect.height()
                canvas.drawText(emojis[(emojiIndex + i) % emojis.size], x, y, paint)
                emojiIndex++
            }
        }

        return bitmap
    }

    fun generateLargeGridPattern(emojis: List<String>, width: Int, height: Int): Bitmap? {
        if (emojis.isEmpty()) return null

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            textSize = 150f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

//        paint.setColor(Color.TRANSPARENT)

        val rect = Rect()
        paint.getTextBounds(emojis[0], 0, emojis[0].length, rect)
        val emojiWidth = rect.width()
        val emojiHeight = rect.height()

        val spacingFactor = 1.5f  // Increase this to make the grid larger
        val horizontalSpacing = emojiWidth * spacingFactor
        val verticalSpacing = emojiHeight * spacingFactor

        val horizontalCount = (width / horizontalSpacing).toInt() + 1
        val verticalCount = (height / verticalSpacing).toInt() + 1

        var emojiIndex = 0

        for (i in 0 until verticalCount) {
            for (j in 0 until horizontalCount) {
                val x = j * horizontalSpacing
                val y = i * verticalSpacing + rect.height()
                canvas.drawText(emojis[(emojiIndex + j) % emojis.size], x, y, paint)
                emojiIndex++
            }
        }

        return bitmap
    }

    fun generateMultiSpiralPattern(emojis: List<String>, width: Int, height: Int): Bitmap? {
        if (emojis.isEmpty()) return null

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            textSize = 10f  // Start with a very small text size
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

//        paint.setColor(Color.TRANSPARENT)

        val centerX = width / 2f
        val centerY = height / 2f
        val maxRadius = kotlin.math.max(width, height) / 2f
        val emojiCount = emojis.size

        val spiralSpacing = 2 * Math.PI / 1
        var emojiIndex = 0

        for (spiral in 0 until 1) {
            var radius = 0f
            var angle = spiral * spiralSpacing
            val angleIncrement = 15f
            val radiusIncrement = 5f

            while (radius < maxRadius * 2) {
                val emoji = emojis[emojiIndex % emojiCount]
                paint.textSize =
                    1f + (radius / maxRadius) * 190f // Increase emoji size from very small to large
                Log.e("ahsdfasjfksad", paint.textSize.toString())
                val rect = Rect()
                paint.getTextBounds(emoji, 0, emoji.length, rect)
                val emojiWidth = rect.width()
                val emojiHeight = rect.height()
                emojiIndex++
                val x = centerX + (radius * cos(angle)).toFloat() - emojiWidth / 2
                val y = centerY + (radius * sin(angle)).toFloat() + emojiHeight / 2
                if (paint.textSize >= 20f) {
                    canvas.drawText(emoji, x, y, paint)
                }
                angle += angleIncrement
                radius += radiusIncrement + paint.textSize / 100  // Increase radius increment with text size
            }
        }

        return bitmap
    }

    fun generateRingPattern(emojis: List<String>, width: Int, height: Int): Bitmap? {
        if (emojis.isEmpty()) return null

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            textSize = 100f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

//        paint.setColor(Color.TRANSPARENT)

        val centerX = width / 2f
        val centerY = height / 2f
        val maxRadius = min(width, height) / 2f
        val emojiCount = emojis.size

        val rect = Rect()
        paint.getTextBounds(emojis[0], 0, emojis[0].length, rect)
        val emojiWidth = rect.width()
        val emojiHeight = rect.height()

        var radiusStep = emojiHeight * 1.5f  // Adjusted step for better spacing
        var radius = radiusStep
        var emojiIndex = 0

        // Draw the first emoji at the center
        canvas.drawText(emojis[0], centerX - emojiWidth / 2, centerY + emojiHeight / 2, paint)
        emojiIndex++

        while (radius < maxRadius * 2) {  // Ensure we cover the entire canvas
            val circumference = 2 * Math.PI * radius
            val emojisInCircle = (circumference / (paint.measureText(emojis[0]) * 1.5)).toInt()
            val angleStep = 360f / emojisInCircle

            for (i in 0 until emojisInCircle) {
                val angle = Math.toRadians((i * angleStep).toDouble())
                val x = centerX + (radius * cos(angle)).toFloat()
                val y = centerY + (radius * sin(angle)).toFloat()
                canvas.drawText(
                    emojis[emojiIndex % emojiCount],
                    x - emojiWidth / 2,
                    y + emojiHeight / 2,
                    paint
                )
            }
            emojiIndex++
            radius += radiusStep
        }

        return bitmap
    }
}