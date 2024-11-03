package com.grownapp.noteapp

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.grownapp.noteapp.ui.TextSegment

class FormatConverter {
    val gson = Gson()

    // Chuyển List<TextSegment> thành chuỗi JSON
    fun convertSegmentsToJson(segments: List<TextSegment>): String {
        return gson.toJson(segments)
    }

    // Chuyển chuỗi JSON thành List<TextSegment>
    fun convertJsonToSegments(json: String): List<TextSegment> {
        val type = object : TypeToken<List<TextSegment>>() {}.type
        return gson.fromJson(json, type)
    }

    fun extractTextSegments(): List<TextSegment> {
        val formattedTextSegments = SpannableStringBuilder()
        val segments = mutableListOf<TextSegment>()
        var start = 0

        while (start < formattedTextSegments.length) {
            val end = formattedTextSegments.nextSpanTransition(start, formattedTextSegments.length, Any::class.java)
            val text = formattedTextSegments.subSequence(start, end).toString()

            // Xác định các định dạng
            val isBold = formattedTextSegments.getSpans(start, end, StyleSpan::class.java).any { it.style == Typeface.BOLD }
            val isItalic = formattedTextSegments.getSpans(start, end, StyleSpan::class.java).any { it.style == Typeface.ITALIC }
            val isUnderline = formattedTextSegments.getSpans(start, end, UnderlineSpan::class.java).isNotEmpty()
            val isStrikethrough = formattedTextSegments.getSpans(start, end, StrikethroughSpan::class.java).isNotEmpty()
            val backgroundColor = formattedTextSegments.getSpans(start, end, BackgroundColorSpan::class.java).firstOrNull()?.backgroundColor
            val textColor  = formattedTextSegments.getSpans(start, end, ForegroundColorSpan::class.java).firstOrNull()?.foregroundColor
            val textSize = formattedTextSegments.getSpans(start, end, AbsoluteSizeSpan::class.java).firstOrNull()?.size?.toFloat() ?: 18f

            // Thêm TextSegment vào danh sách
            segments.add(TextSegment(text, isBold, isItalic, isUnderline, isStrikethrough, backgroundColor, textColor, textSize))
            start = end
        }
        return segments
    }

}