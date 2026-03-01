package com.example.eventglow.reportingandanalytics

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.Locale

object AnalyticsExportHelper {

    fun exportAsExcelLikeXlsToUri(
        context: Context,
        targetUri: Uri,
        rows: List<ReportingEventRow>,
        periodLabel: String
    ): Result<Unit> = runCatching {
        val bytes = buildExcelBytes(rows, periodLabel)
        writeBytesToUri(context, targetUri, bytes)
    }

    fun exportAsPdfToUri(
        context: Context,
        targetUri: Uri,
        rows: List<ReportingEventRow>,
        periodLabel: String
    ): Result<Unit> = runCatching {
        data class PdfColumn(val title: String, val width: Float)

        val document = PdfDocument()
        try {
            val pageWidth = 595
            val pageHeight = 842
            val margin = 24f
            val tableTopGap = 14f
            val rowHeight = 24f
            val headerHeight = 26f
            val cellTextPadding = 6f
            val columns = listOf(
                PdfColumn("Event", 240f),
                PdfColumn("Date", 120f),
                PdfColumn("Sold", 70f),
                PdfColumn("Revenue (GHS)", 117f)
            )

            val titlePaint = Paint().apply {
                textSize = 16f
                isFakeBoldText = true
                color = Color.BLACK
            }
            val metaPaint = Paint().apply {
                textSize = 11f
                color = Color.DKGRAY
            }
            val headerTextPaint = Paint().apply {
                textSize = 11f
                isFakeBoldText = true
                color = Color.BLACK
            }
            val bodyTextPaint = Paint().apply {
                textSize = 10.5f
                color = Color.BLACK
            }
            val borderPaint = Paint().apply {
                style = Paint.Style.STROKE
                strokeWidth = 1f
                color = Color.rgb(190, 190, 190)
            }
            val headerFillPaint = Paint().apply {
                style = Paint.Style.FILL
                color = Color.rgb(228, 236, 246)
            }
            val zebraFillPaint = Paint().apply {
                style = Paint.Style.FILL
                color = Color.rgb(246, 248, 251)
            }
            val summaryFillPaint = Paint().apply {
                style = Paint.Style.FILL
                color = Color.rgb(235, 244, 234)
            }

            fun drawHeaderBlock(canvas: android.graphics.Canvas, pageNumber: Int): Float {
                var y = 36f
                canvas.drawText("EventGlow Analytics Report", margin, y, titlePaint)
                y += 18f
                canvas.drawText("Period: $periodLabel", margin, y, metaPaint)
                y += 14f
                canvas.drawText("Rows: ${rows.size}", margin, y, metaPaint)
                canvas.drawText("Page: $pageNumber", pageWidth - 90f, y, metaPaint)
                y += tableTopGap

                val headerTop = y
                val headerBottom = y + headerHeight
                var x = margin
                columns.forEach { col ->
                    val rect = RectF(x, headerTop, x + col.width, headerBottom)
                    canvas.drawRect(rect, headerFillPaint)
                    canvas.drawRect(rect, borderPaint)
                    canvas.drawText(
                        col.title,
                        x + cellTextPadding,
                        headerTop + 16f,
                        headerTextPaint
                    )
                    x += col.width
                }
                return headerBottom
            }

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas
            var y = drawHeaderBlock(canvas, pageNumber)

            rows.forEachIndexed { index, row ->
                if (y + rowHeight > pageHeight - 58f) {
                    document.finishPage(page)
                    pageNumber += 1
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    y = drawHeaderBlock(canvas, pageNumber)
                }

                val rowTop = y
                val rowBottom = y + rowHeight
                if (index % 2 == 1) {
                    canvas.drawRect(RectF(margin, rowTop, pageWidth - margin, rowBottom), zebraFillPaint)
                }

                val values = listOf(
                    row.title,
                    row.date,
                    row.sold.toString(),
                    String.format(Locale.getDefault(), "%.2f", row.revenue)
                )

                var x = margin
                columns.forEachIndexed { colIndex, col ->
                    val rect = RectF(x, rowTop, x + col.width, rowBottom)
                    canvas.drawRect(rect, borderPaint)
                    val rawText = values[colIndex]
                    val trimmed = trimToFit(rawText, col.width - (cellTextPadding * 2), bodyTextPaint)
                    canvas.drawText(
                        trimmed,
                        x + cellTextPadding,
                        rowTop + 16f,
                        bodyTextPaint
                    )
                    x += col.width
                }

                y = rowBottom
            }

            val totalRevenue = rows.sumOf { it.revenue }
            val totalSold = rows.sumOf { it.sold }
            val summaryTop = (y + 14f).coerceAtMost(pageHeight - 62f)
            val summaryRect = RectF(margin, summaryTop, pageWidth - margin, summaryTop + 34f)
            canvas.drawRect(summaryRect, summaryFillPaint)
            canvas.drawRect(summaryRect, borderPaint)
            val summaryText = "Summary: Tickets Sold = $totalSold    Total Revenue = ${
                String.format(Locale.getDefault(), "%.2f", totalRevenue)
            } GHS"
            canvas.drawText(summaryText, margin + 8f, summaryTop + 21f, headerTextPaint)

            document.finishPage(page)
            val stream = context.contentResolver.openOutputStream(targetUri, "wt")
            if (stream != null) {
                stream.use { output ->
                    document.writeTo(output)
                    output.flush()
                }
            } else {
                val parcelDescriptor: ParcelFileDescriptor = context.contentResolver
                    .openFileDescriptor(targetUri, "w")
                    ?: throw IllegalStateException("Unable to open file descriptor for selected location.")
                var fallbackOutput: FileOutputStream? = null
                try {
                    fallbackOutput = FileOutputStream(parcelDescriptor.fileDescriptor)
                    document.writeTo(fallbackOutput)
                    fallbackOutput.flush()
                    fallbackOutput.fd.sync()
                } finally {
                    try {
                        fallbackOutput?.close()
                    } catch (_: Exception) {
                    }
                    try {
                        parcelDescriptor.close()
                    } catch (_: Exception) {
                    }
                }
            }
        } finally {
            try {
                document.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun trimToFit(text: String, maxWidth: Float, paint: Paint): String {
        if (paint.measureText(text) <= maxWidth) return text
        val ellipsis = "..."
        var value = text
        while (value.isNotEmpty() && paint.measureText(value + ellipsis) > maxWidth) {
            value = value.dropLast(1)
        }
        return if (value.isBlank()) ellipsis else value + ellipsis
    }

    private fun buildExcelBytes(
        rows: List<ReportingEventRow>,
        periodLabel: String
    ): ByteArray {
        val builder = StringBuilder()
        builder.append("Period\t").append(periodLabel).append('\n')
        builder.append("Event\tDate\tSold\tRevenue\n")
        rows.forEach { row ->
            builder.append(sanitize(row.title)).append('\t')
                .append(sanitize(row.date)).append('\t')
                .append(row.sold).append('\t')
                .append(String.format(Locale.getDefault(), "%.2f", row.revenue))
                .append('\n')
        }
        val textBytes = builder.toString().toByteArray(StandardCharsets.UTF_8)
        return byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + textBytes
    }

    private fun writeBytesToUri(
        context: Context,
        targetUri: Uri,
        bytes: ByteArray
    ) {
        val parcelDescriptor: ParcelFileDescriptor = context.contentResolver
            .openFileDescriptor(targetUri, "w")
            ?: throw IllegalStateException("Unable to open file descriptor for selected location.")
        var output: FileOutputStream? = null
        try {
            output = FileOutputStream(parcelDescriptor.fileDescriptor)
            output.write(bytes)
            output.flush()
            output.fd.sync()
        } finally {
            try {
                output?.close()
            } catch (_: Exception) {
            }
            try {
                parcelDescriptor.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun sanitize(value: String): String {
        return value.replace('\t', ' ').replace('\n', ' ')
    }
}
