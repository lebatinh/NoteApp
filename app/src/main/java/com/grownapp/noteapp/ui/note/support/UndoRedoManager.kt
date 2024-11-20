package com.grownapp.noteapp.ui.note.support

import android.text.SpannableStringBuilder

class UndoRedoManager {
    private val history: MutableList<SpannableStringBuilder> = mutableListOf()
    private var currentPosition = -1

    fun addState(newText: SpannableStringBuilder) {
        if (currentPosition < history.size - 1) {
            history.subList(currentPosition + 1, history.size).clear()
        }

        val snapshot = SpannableStringBuilder(newText)
        history.add(snapshot)
        currentPosition++
    }

    fun undo(): SpannableStringBuilder? {
        return if (currentPosition > 0) {
            currentPosition--
            SpannableStringBuilder(history[currentPosition])
        } else {
            null
        }
    }

    fun redo(): SpannableStringBuilder? {
        return if (currentPosition < history.size - 1) {
            currentPosition++
            SpannableStringBuilder(history[currentPosition])
        } else {
            null
        }
    }

    fun undoAll(): SpannableStringBuilder? {
        return if (history.isNotEmpty()) {
            currentPosition = 0
            SpannableStringBuilder(history[currentPosition])
        } else {
            null
        }
    }
}