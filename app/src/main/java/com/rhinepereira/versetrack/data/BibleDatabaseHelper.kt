package com.rhinepereira.versetrack.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class BibleDatabaseHelper(private val context: Context) {

    private val dbName = "bible.db"

    /**
     * Fetches a range of verses. If endVerse is null or same as startVerse, fetches one verse.
     * Uses a JOIN with the books table to find the book by name.
     */
    fun getVerseRange(bookName: String, chapter: Int, startVerse: Int, endVerse: Int?): String? {
        val db = openDatabase()
        val results = mutableListOf<String>()
        
        val query: String
        val params: Array<String>
        
        if (endVerse == null || endVerse <= startVerse) {
            query = """
                SELECT v.text 
                FROM verses v 
                JOIN books b ON v.book_id = b.id 
                WHERE b.name = ? AND v.chapter = ? AND v.verse = ?
            """.trimIndent()
            params = arrayOf(bookName, chapter.toString(), startVerse.toString())
        } else {
            query = """
                SELECT v.text 
                FROM verses v 
                JOIN books b ON v.book_id = b.id 
                WHERE b.name = ? AND v.chapter = ? AND v.verse >= ? AND v.verse <= ? 
                ORDER BY v.verse ASC
            """.trimIndent()
            params = arrayOf(bookName, chapter.toString(), startVerse.toString(), endVerse.toString())
        }

        try {
            val cursor = db.rawQuery(query, params)
            while (cursor.moveToNext()) {
                results.add(cursor.getString(0))
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.close()
        }
        
        return if (results.isNotEmpty()) results.joinToString(" ") else null
    }

    private fun openDatabase(): SQLiteDatabase {
        val dbFile = context.getDatabasePath(dbName)
        if (!dbFile.exists()) {
            dbFile.parentFile?.mkdirs()
            copyDatabase()
        }
        return SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
    }

    private fun copyDatabase() {
        val input: InputStream = context.assets.open(dbName)
        val outFileName = context.getDatabasePath(dbName).path
        val output: OutputStream = FileOutputStream(outFileName)
        val buffer = ByteArray(1024)
        var length: Int
        while (input.read(buffer).also { length = it } > 0) {
            output.write(buffer, 0, length)
        }
        output.flush()
        output.close()
        input.close()
    }
}
