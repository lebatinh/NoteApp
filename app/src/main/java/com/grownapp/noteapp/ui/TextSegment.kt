package com.grownapp.noteapp.ui

data class TextSegment(
    val text: String,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val isStrikethrough: Boolean = false,
    val backgroundColor: String? = null,
    val textColor: String? = null,
    val fontSize: Float? = null
)