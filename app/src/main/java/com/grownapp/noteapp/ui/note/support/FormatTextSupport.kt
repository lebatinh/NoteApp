package com.grownapp.noteapp.ui.note.support

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import androidx.core.content.ContextCompat
import com.grownapp.noteapp.R

class FormatTextSupport {
    fun applyCurrentFormat(
        context: Context,
        currentFormat: TextFormat,
        text: SpannableStringBuilder,
        start: Int,
        end: Int
    ) {
        val defaultBackgroundColor =
            ContextCompat.getColor(context, R.color.transparent)
        val defaultTextColor = ContextCompat.getColor(context, R.color.text)

        if (currentFormat.isBold) text.setSpan(
            StyleSpan(Typeface.BOLD),
            start,
            end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        if (currentFormat.isItalic) text.setSpan(
            StyleSpan(Typeface.ITALIC),
            start,
            end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        if (currentFormat.isUnderline) text.setSpan(
            UnderlineSpan(),
            start,
            end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        if (currentFormat.isStrikethrough) text.setSpan(
            StrikethroughSpan(),
            start,
            end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        if (currentFormat.backgroundColor != defaultBackgroundColor) text.setSpan(
            BackgroundColorSpan(
                currentFormat.backgroundColor
            ), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        if (currentFormat.textColor != defaultTextColor) text.setSpan(
            ForegroundColorSpan(
                currentFormat.textColor
            ), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        text.setSpan(
            AbsoluteSizeSpan(currentFormat.textSize, true),
            start,
            end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    fun spannableToNoteContent(context: Context, spannable: SpannableStringBuilder): NoteContent {
        val defaultBackgroundColor =
            ContextCompat.getColor(context, R.color.transparent)
        val defaultTextColor = ContextCompat.getColor(context, R.color.text)
        val segments = mutableListOf<TextSegment>()
        var start = 0

        while (start < spannable.length) {
            val end = spannable.nextSpanTransition(start, spannable.length, Any::class.java)
            val text = spannable.subSequence(start, end).toString()

            val isBold = spannable.getSpans(start, end, StyleSpan::class.java)
                .any { it.style == Typeface.BOLD }
            val isItalic = spannable.getSpans(start, end, StyleSpan::class.java)
                .any { it.style == Typeface.ITALIC }
            val isUnderline =
                spannable.getSpans(start, end, UnderlineSpan::class.java).isNotEmpty()
            val isStrikethrough =
                spannable.getSpans(start, end, StrikethroughSpan::class.java).isNotEmpty()

            val backgroundColor =
                spannable.getSpans(start, end, BackgroundColorSpan::class.java)
                    .firstOrNull()?.backgroundColor ?: defaultBackgroundColor
            val textColor = spannable.getSpans(start, end, ForegroundColorSpan::class.java)
                .firstOrNull()?.foregroundColor ?: defaultTextColor
            val textSize = spannable.getSpans(start, end, AbsoluteSizeSpan::class.java)
                .firstOrNull()?.size ?: 18

            segments.add(
                TextSegment(
                    text,
                    isBold,
                    isItalic,
                    isUnderline,
                    isStrikethrough,
                    backgroundColor,
                    textColor,
                    textSize
                )
            )

            start = end
        }
        return NoteContent(segments)
    }

    fun noteContentToSpannable(context: Context, noteContent: NoteContent): SpannableStringBuilder {
        val defaultBackgroundColor =
            ContextCompat.getColor(context, R.color.transparent)
        val defaultTextColor = ContextCompat.getColor(context, R.color.text)
        val spannable = SpannableStringBuilder()

        for (segment in noteContent.segments) {
            val start = spannable.length
            spannable.append(segment.text)
            val end = spannable.length

            segment.apply {
                if (isBold == true) spannable.setSpan(
                    StyleSpan(Typeface.BOLD),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                if (isItalic == true) spannable.setSpan(
                    StyleSpan(Typeface.ITALIC),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                if (isUnderline == true) spannable.setSpan(
                    UnderlineSpan(),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                if (isStrikethrough == true) spannable.setSpan(
                    StrikethroughSpan(),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                backgroundColor.let {
                    spannable.setSpan(
                        BackgroundColorSpan(it ?: defaultBackgroundColor),
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                textColor.let {
                    spannable.setSpan(
                        ForegroundColorSpan(it ?: defaultTextColor),
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                textSize.let {
                    spannable.setSpan(
                        AbsoluteSizeSpan(it ?: 18, true),
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        }
        return spannable
    }

//    fun updateCurrentFormatFromCursorPosition() {
//        val selectionStart = binding.edtNote.selectionStart
//        val selectionEnd = binding.edtNote.selectionEnd
//        if (selectionStart in 0..<selectionEnd) {
//            val spannable = binding.edtNote.text as SpannableStringBuilder
//
//            // Lấy các spans trong phạm vi vùng chọn
//            val boldSpans = spannable.getSpans(selectionStart, selectionEnd, StyleSpan::class.java)
//            val colorSpans =
//                spannable.getSpans(selectionStart, selectionEnd, ForegroundColorSpan::class.java)
//            val backgroundColorSpans =
//                spannable.getSpans(selectionStart, selectionEnd, BackgroundColorSpan::class.java)
//            val underlineSpans =
//                spannable.getSpans(selectionStart, selectionEnd, UnderlineSpan::class.java)
//            val strikethroughSpans =
//                spannable.getSpans(selectionStart, selectionEnd, StrikethroughSpan::class.java)
//            val sizeSpans =
//                spannable.getSpans(selectionStart, selectionEnd, AbsoluteSizeSpan::class.java)
//
//            // Kiểm tra định dạng chung trong toàn bộ vùng chọn
//            val isBold = boldSpans.all { it.style == Typeface.BOLD }
//            val isItalic = boldSpans.all { it.style == Typeface.ITALIC }
//            val isUnderline =
//                underlineSpans.isNotEmpty() && underlineSpans.size == spannable.getSpans(
//                    selectionStart,
//                    selectionEnd,
//                    UnderlineSpan::class.java
//                ).size
//            val isStrikethrough =
//                strikethroughSpans.isNotEmpty() && strikethroughSpans.size == spannable.getSpans(
//                    selectionStart,
//                    selectionEnd,
//                    StrikethroughSpan::class.java
//                ).size
//
//            val textColor = colorSpans.firstOrNull()?.foregroundColor ?: currentFormat.textColor
//            val backgroundColor =
//                backgroundColorSpans.firstOrNull()?.backgroundColor ?: currentFormat.backgroundColor
//            val textSize = sizeSpans.firstOrNull()?.size ?: currentFormat.textSize
//
//            // Cập nhật currentFormat với các thuộc tính lấy được
//            currentFormat = currentFormat.copy(
//                isBold = isBold,
//                isItalic = isItalic,
//                isUnderline = isUnderline,
//                isStrikethrough = isStrikethrough,
//                textColor = textColor,
//                backgroundColor = backgroundColor,
//                textSize = textSize
//            )
//        } else {
//            val spannable = binding.edtNote.text as SpannableStringBuilder
//
//            // Lấy định dạng ngay tại vị trí con trỏ
//            val boldSpans =
//                spannable.getSpans(selectionStart, selectionStart, StyleSpan::class.java)
//            val colorSpans =
//                spannable.getSpans(selectionStart, selectionStart, ForegroundColorSpan::class.java)
//            val backgroundColorSpans =
//                spannable.getSpans(selectionStart, selectionStart, BackgroundColorSpan::class.java)
//            val underlineSpans =
//                spannable.getSpans(selectionStart, selectionStart, UnderlineSpan::class.java)
//            val strikethroughSpans =
//                spannable.getSpans(selectionStart, selectionStart, StrikethroughSpan::class.java)
//            val sizeSpans =
//                spannable.getSpans(selectionStart, selectionStart, AbsoluteSizeSpan::class.java)
//
//            // Cập nhật currentFormat với các thuộc tính tìm thấy
//            currentFormat = currentFormat.copy(
//                isBold = boldSpans.isNotEmpty() && boldSpans[0].style == Typeface.BOLD,
//                isItalic = boldSpans.isNotEmpty() && boldSpans[0].style == Typeface.ITALIC,
//                isUnderline = underlineSpans.isNotEmpty(),
//                isStrikethrough = strikethroughSpans.isNotEmpty(),
//                textColor = colorSpans.firstOrNull()?.foregroundColor ?: currentFormat.textColor,
//                backgroundColor = backgroundColorSpans.firstOrNull()?.backgroundColor
//                    ?: currentFormat.backgroundColor,
//                textSize = sizeSpans.firstOrNull()?.size ?: currentFormat.textSize
//            )
//        }
//
//        Log.d("currentFormat", currentFormat.toString())
//    }
//
//    // Hàm kiểm tra định dạng của đoạn mới với đoạn cuối trong stack
//    fun isSameFormat(newFormat: TextFormat, lastFormat: TextFormat): Boolean {
//        return newFormat.isBold == lastFormat.isBold &&
//                newFormat.isItalic == lastFormat.isItalic &&
//                newFormat.isUnderline == lastFormat.isUnderline &&
//                newFormat.isStrikethrough == lastFormat.isStrikethrough &&
//                newFormat.backgroundColor == lastFormat.backgroundColor &&
//                newFormat.textColor == lastFormat.textColor &&
//                newFormat.textSize == lastFormat.textSize
//    }
//
//    fun getCurrentFormat(): TextFormat {
//        // Lấy và trả về định dạng hiện tại từ trạng thái của người dùng
//        return currentFormat
//    }
}