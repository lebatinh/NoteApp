package com.grownapp.noteapp.ui.note

import android.graphics.Color

data class NoteContent(
    val segments: List<TextSegment> // Danh sách các đoạn văn bản đã định dạng
)

data class TextSegment(
    var text: String? = null,
    val isBold: Boolean? = false,
    val isItalic: Boolean? = false,
    val isUnderline: Boolean? = false,
    val isStrikethrough: Boolean? = false,
    val backgroundColor: Int? = Color.TRANSPARENT,
    val textColor: Int? = Color.BLACK,
    val textSize: Float? = 18f
)

data class TextFormat(
    var isBold: Boolean = false,
    var isItalic: Boolean = false,
    var isUnderline: Boolean = false,
    var isStrikethrough: Boolean = false,
    var backgroundColor: Int = 0,
    var textColor: Int = 0,
    var textSize: Float = 18f
)