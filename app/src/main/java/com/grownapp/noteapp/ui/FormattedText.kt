package com.grownapp.noteapp.ui

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class FormattedText {
    private val segments: MutableList<TextSegment> = mutableListOf()

    // Thêm đoạn mới với định dạng
    fun addSegment(text: String, format: Format) {
        segments.add(TextSegment(text, format))
    }

    // Chuyển đổi các đoạn sang JSON để lưu
    fun toJSON(): String {
        val gson = Gson()
        return gson.toJson(segments)
    }

    // Khôi phục lại từ JSON
    fun fromJSON(json: String) {
        if (json.isEmpty()) return

        val gson = Gson()
        val type = object : TypeToken<List<TextSegment>>() {}.type
        val loadedSegments: List<TextSegment> = gson.fromJson(json, type)
        segments.clear()
        segments.addAll(loadedSegments)
    }

    // Chuyển đổi các đoạn sang SpannableStringBuilder để hiển thị trong edtNote
    fun toSpannable(): SpannableStringBuilder {
        val spannable = SpannableStringBuilder()

        if (segments.size != 0) {
            segments.forEach { segment ->
                val start = spannable.length
                spannable.append(segment.text)
                applyFormat(spannable, segment.format, start, spannable.length)
            }
        }
        return spannable
    }

    fun applyFormat(
        spannable: SpannableStringBuilder,
        format: Format,
        start: Int,
        end: Int
    ) {
        if (format.isBold == true) spannable.setSpan(
            StyleSpan(Typeface.BOLD),
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        if (format.isItalic == true) spannable.setSpan(
            StyleSpan(Typeface.ITALIC),
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        if (format.isUnderline == true) spannable.setSpan(
            UnderlineSpan(),
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        if (format.isStrikethrough == true) spannable.setSpan(
            StrikethroughSpan(),
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            format.textColor?.let { ForegroundColorSpan(it) },
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            format.backgroundColor?.let { BackgroundColorSpan(it) },
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            format.fontSize?.let { AbsoluteSizeSpan(it.toInt(), true) },
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
}