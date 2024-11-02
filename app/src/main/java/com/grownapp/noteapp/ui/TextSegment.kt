package com.grownapp.noteapp.ui

import android.graphics.Color

data class TextSegment(
    var text: String? = null,
    val isBold: Boolean? = false,
    val isItalic: Boolean? = false,
    val isUnderline: Boolean? = false,
    val isStrikethrough: Boolean? = false,
    val backgroundColor: Int? = Color.TRANSPARENT,
    val textColor: Int? = Color.BLACK,
    val fontSize: Float = 18f
)