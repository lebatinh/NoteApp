package com.grownapp.noteapp.ui

data class TextSegment(
    val text: String,
    val format: Format
)

data class Format(
    val isBold: Boolean? = false,
    val isItalic: Boolean? = false,
    val isUnderline: Boolean? = false,
    val isStrikethrough: Boolean? =false,
    val backgroundColor: Int?,
    val textColor: Int?,
    val fontSize: Float? = 18f
)