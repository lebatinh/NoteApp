package com.grownapp.noteapp.ui

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converter {
    // từ chữ định dạng sang string
    @TypeConverter
    fun fromFormattedText(value: FormattedText?): String = Gson().toJson(value)

    // từ json sang chữ định dạng
    @TypeConverter
    fun toFormattedText(value: String?): FormattedText {
        val type = object : TypeToken<FormattedText>() {}.type
        return Gson().fromJson(value, type)
    }
}