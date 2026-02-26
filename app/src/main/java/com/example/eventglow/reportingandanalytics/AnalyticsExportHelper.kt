package com.example.eventglow.reportingandanalytics

import android.content.Context
import android.graphics.Paint
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
        val document = PdfDocument()
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas
            val paint = Paint().apply { textSize = 11f }
            val titlePaint = Paint().apply { textSize = 15f; isFakeBoldText = true }

            var y = 36
            canvas.drawText("EventGlow Analytics Report", 24f, y.toFloat(), titlePaint)
            y += 20
            canvas.drawText("Period: $periodLabel", 24f, y.toFloat(), paint)
            y += 20
            canvas.drawText("Rows: ${rows.size}", 24f, y.toFloat(), paint)
            y += 20
            canvas.drawText("Event | Date | Sold | Revenue", 24f, y.toFloat(), paint)
            y += 14

            rows.forEachIndexed { index, row ->
                if (y > 810) {
                    document.finishPage(page)
                    val nextPageInfo = PdfDocument.PageInfo.Builder(595, 842, (index / 40) + 2).create()
                    page = document.startPage(nextPageInfo)
                    canvas = page.canvas
                    y = 36
                }
                val line = "${row.title.take(28)} | ${row.date} | ${row.sold} | ${
                    String.format(
                        Locale.getDefault(),
                        "%.2f",
                        row.revenue
                    )
                }"
                canvas.drawText(line, 24f, y.toFloat(), paint)
                y += 14
            }

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
