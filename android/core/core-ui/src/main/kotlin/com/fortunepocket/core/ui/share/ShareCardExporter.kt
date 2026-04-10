package com.fortunepocket.core.ui.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

data class OracleShareCardPayload(
    val eyebrow: String,
    val title: String,
    val subtitle: String,
    val headline: String,
    val summary: String,
    val guidance: String,
    val footer: String
)

object ShareCardExporter {

    fun export(
        context: Context,
        payload: OracleShareCardPayload,
        fileName: String
    ): android.net.Uri? {
        return runCatching {
            val density = context.resources.displayMetrics.density
            val width = (360f * density).roundToInt()
            val outerPadding = 22f * density
            val cardPadding = 24f * density
            val textWidth = (width - outerPadding * 2 - cardPadding * 2).roundToInt()

            val eyebrowPaint = textPaint(12f * density, 0xFFC9A84C.toInt(), true)
            val titlePaint = textPaint(34f * density, 0xFFF0EBE3.toInt(), true)
            val subtitlePaint = textPaint(13f * density, 0xFF9E96B8.toInt())
            val headlinePaint = textPaint(18f * density, 0xFFF0EBE3.toInt(), true)
            val summaryPaint = textPaint(15f * density, 0xFFE5DDF4.toInt())
            val guidancePaint = textPaint(14f * density, 0xFFD8C7A0.toInt())
            val footerPaint = textPaint(12f * density, 0xFF9E96B8.toInt())

            val eyebrowLayout = singleLineLayout(payload.eyebrow, eyebrowPaint, textWidth)
            val titleLayout = multiLineLayout(payload.title, titlePaint, textWidth)
            val subtitleLayout = multiLineLayout(payload.subtitle, subtitlePaint, textWidth)
            val headlineLayout = multiLineLayout(payload.headline, headlinePaint, textWidth)
            val summaryLayout = multiLineLayout(payload.summary, summaryPaint, textWidth)
            val guidanceLayout = multiLineLayout(payload.guidance, guidancePaint, textWidth)
            val footerLayout = multiLineLayout(payload.footer, footerPaint, textWidth)

            val contentHeight = eyebrowLayout.height +
                18f * density +
                titleLayout.height +
                10f * density +
                subtitleLayout.height +
                26f * density +
                headlineLayout.height +
                14f * density +
                summaryLayout.height +
                24f * density +
                guidanceLayout.height +
                28f * density +
                footerLayout.height

            val height = (contentHeight + outerPadding * 2 + cardPadding * 2).roundToInt()
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            drawBackground(canvas, width.toFloat(), height.toFloat())

            val cardLeft = outerPadding
            val cardTop = outerPadding
            val cardRight = width - outerPadding
            val cardBottom = height - outerPadding
            val cardRect = RectF(cardLeft, cardTop, cardRight, cardBottom)
            val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF251848.toInt() }
            canvas.drawRoundRect(cardRect, 36f, 36f, cardPaint)

            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2f * density
                color = 0x40C9A84C
            }
            canvas.drawRoundRect(cardRect, 36f, 36f, borderPaint)

            var cursorY = cardTop + cardPadding
            val textX = cardLeft + cardPadding

            cursorY = drawLayout(canvas, eyebrowLayout, textX, cursorY)
            cursorY += 18f * density

            cursorY = drawLayout(canvas, titleLayout, textX, cursorY)
            cursorY += 10f * density

            cursorY = drawLayout(canvas, subtitleLayout, textX, cursorY)
            cursorY += 26f * density

            val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0x30C9A84C
                strokeWidth = 1.5f * density
            }
            canvas.drawLine(textX, cursorY, cardRight - cardPadding, cursorY, dividerPaint)
            cursorY += 20f * density

            cursorY = drawLayout(canvas, headlineLayout, textX, cursorY)
            cursorY += 14f * density
            cursorY = drawLayout(canvas, summaryLayout, textX, cursorY)
            cursorY += 24f * density
            drawLayout(canvas, guidanceLayout, textX, cursorY)

            val footerY = cardBottom - cardPadding - footerLayout.height
            drawLayout(canvas, footerLayout, textX, footerY)

            val directory = File(context.cacheDir, "share-cards").apply { mkdirs() }
            val file = File(directory, "$fileName.png")
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        }.getOrNull()
    }

    fun shareIntent(
        uri: android.net.Uri?,
        chooserTitle: String,
        shareText: String
    ): Intent {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            if (uri != null) {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                type = "text/plain"
            }
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        return Intent.createChooser(shareIntent, chooserTitle)
    }

    private fun drawBackground(canvas: Canvas, width: Float, height: Float) {
        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                width,
                height,
                intArrayOf(0xFF0E0A1F.toInt(), 0xFF1A1035.toInt(), 0xFF251848.toInt()),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width, height, backgroundPaint)
    }

    private fun textPaint(sizePx: Float, color: Int, isBold: Boolean = false): TextPaint {
        return TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = sizePx
            this.color = color
            isFakeBoldText = isBold
        }
    }

    private fun multiLineLayout(text: String, paint: TextPaint, width: Int): StaticLayout {
        return StaticLayout.Builder
            .obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.08f)
            .build()
    }

    private fun singleLineLayout(text: String, paint: TextPaint, width: Int): StaticLayout {
        return StaticLayout.Builder
            .obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(1)
            .build()
    }

    private fun drawLayout(
        canvas: Canvas,
        layout: StaticLayout,
        x: Float,
        y: Float
    ): Float {
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
        return y + layout.height
    }
}
