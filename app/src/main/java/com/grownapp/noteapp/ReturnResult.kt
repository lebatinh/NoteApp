package com.grownapp.noteapp

sealed class ReturnResult {
    object Success : ReturnResult()
    data class Error(val message: String) : ReturnResult()
}