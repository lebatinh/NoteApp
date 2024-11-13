package com.grownapp.noteapp.ui.note.support

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.widget.Toast
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.grownapp.noteapp.R
import com.grownapp.noteapp.ui.note.dao.Note

class FileProcess {
    fun checkAndRequestPermissions(action: () -> Unit) {
        action()
    }

    fun exportNotesToDirectory(
        directoryUri: Uri?,
        context: Context,
        listNoteSelected: MutableList<Note>
    ) {
        if (directoryUri == null) {
            Toast.makeText(
                context,
                context.getString(R.string.select_directory), Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (listNoteSelected.isNotEmpty()) {
            listNoteSelected.forEach { note ->
                val noteText = note.note
                val noteContent = extractText(noteText?:"")

                val fileTitle = note.title ?: context.getString(R.string.untitled)
                val fileName = context.getString(R.string.txt, fileTitle)

                val fileUri = createFileInDirectory(directoryUri, fileName, context)
                fileUri?.let {
                    saveToFile(it, noteContent, context)
                }
            }
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.select_none_note), Toast.LENGTH_SHORT
            ).show()
        }
        listNoteSelected.clear()
    }

    private fun createFileInDirectory(directoryUri: Uri, fileName: String, context: Context): Uri? {
        val documentUri = DocumentsContract.buildDocumentUriUsingTree(
            directoryUri,
            DocumentsContract.getTreeDocumentId(directoryUri)
        )
        return try {
            DocumentsContract.createDocument(
                context.contentResolver,
                documentUri,
                context.getString(R.string.text_plain),
                fileName
            )
        } catch (e: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.error_log_create_file, e.message), Toast.LENGTH_SHORT
            ).show()
            null
        }
    }

    private fun saveToFile(uri: Uri, content: String, context: Context) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(content.toByteArray())
                Toast.makeText(
                    context,
                    context.getString(R.string.saved_note, uri.lastPathSegment),
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.error_log_save_file, e.message), Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun extractText(noteContent: String): String{
        if (noteContent.isNotEmpty()){
            val jsonObject = JsonParser.parseString(noteContent).asJsonObject

            val segmentArray: JsonArray = jsonObject.getAsJsonArray("segments")

            return buildString {
                for (s in segmentArray){
                    val segmentObject = s.asJsonObject
                    append(segmentObject["text"].asString)
                }
            }
        }else{
            return ""
        }

    }
}