package com.grownapp.noteapp.ui.note

import android.text.SpannableStringBuilder

class UndoRedoManager {
    val history: MutableList<SpannableStringBuilder> = mutableListOf()
    private var currentPosition = -1 // Vị trí hiện tại trong list lịch sử

    // Hàm thêm trạng thái mới vào list lịch sử
    fun addState(newText: SpannableStringBuilder) {
        // Loại bỏ các trạng thái "redo" nếu có sau vị trí hiện tại
        if (currentPosition < history.size - 1) {
            history.subList(currentPosition + 1, history.size).clear()
        }

        // Sao chép `SpannableStringBuilder` để lưu độc lập
        val snapshot = SpannableStringBuilder(newText)
        history.add(snapshot)
        currentPosition++
    }

    // Undo: Trở về trạng thái trước đó
    fun undo(): SpannableStringBuilder? {
        return if (currentPosition > 0) {
            currentPosition--
            SpannableStringBuilder(history[currentPosition]) // Trả về một bản sao
        } else {
            null // Không có trạng thái để undo
        }
    }

    // Redo: Chuyển đến trạng thái kế tiếp
    fun redo(): SpannableStringBuilder? {
        return if (currentPosition < history.size - 1) {
            currentPosition++
            SpannableStringBuilder(history[currentPosition]) // Trả về một bản sao
        } else {
            null // Không có trạng thái để redo
        }
    }

    // Undo All: Trở về trạng thái đầu tiên
    fun undoAll(): SpannableStringBuilder? {
        return if (history.isNotEmpty()) {
            currentPosition = 0
            SpannableStringBuilder(history[currentPosition]) // Trả về trạng thái đầu tiên
        } else {
            null
        }
    }

    // Kiểm tra có thể undo không
    fun canUndo(): Boolean = currentPosition > 0

    // Kiểm tra có thể redo không
    fun canRedo(): Boolean = currentPosition < history.size - 1
}