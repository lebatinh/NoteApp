package com.grownapp.noteapp.ui.note

import android.graphics.Color

data class NoteContent(
    val segments: List<TextSegment> // Danh sách các đoạn văn bản đã định dạng
)

data class TextSegment(
    var text: String? = null,
    var isBold: Boolean? = false,
    var isItalic: Boolean? = false,
    var isUnderline: Boolean? = false,
    var isStrikethrough: Boolean? = false,
    var backgroundColor: Int? = Color.TRANSPARENT,
    var textColor: Int? = Color.BLACK,
    var textSize: Int? = 18
)

data class TextFormat(
    var isBold: Boolean = false,
    var isItalic: Boolean = false,
    var isUnderline: Boolean = false,
    var isStrikethrough: Boolean = false,
    var backgroundColor: Int = 0,
    var textColor: Int = 0,
    var textSize: Int = 18
)