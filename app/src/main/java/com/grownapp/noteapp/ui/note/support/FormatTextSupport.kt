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
}