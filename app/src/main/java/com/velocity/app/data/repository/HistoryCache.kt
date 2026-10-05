package com.velocity.app.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.velocity.app.data.model.ChatMessage
import com.velocity.app.data.model.MessagePage
import com.velocity.app.data.model.ThreadItem
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest

class HistoryCache(context: Context, config: ServerConfig) : SQLiteOpenHelper(context.applicationContext, "history.db", null, 2) {
    private val scope = MessageDigest.getInstance("SHA-256").digest("${config.normalizedUrl}|${config.cfClientId}|${config.cfClientSecret}".toByteArray()).joinToString("") { "%02x".format(it) }
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    override fun onCreate(database: SQLiteDatabase) {
        database.execSQL("CREATE TABLE messages(scope TEXT, session TEXT, id TEXT, position INTEGER, payload TEXT, PRIMARY KEY(scope,session,id))")
        database.execSQL("CREATE INDEX message_order ON messages(scope,session,position)")
        database.execSQL("CREATE TABLE pages(scope TEXT, session TEXT, revision INTEGER, has_more INTEGER, PRIMARY KEY(scope,session))")
        database.execSQL("CREATE TABLE metadata(scope TEXT, name TEXT, payload TEXT, PRIMARY KEY(scope,name))")
    }

    override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) database.execSQL("CREATE TABLE metadata(scope TEXT, name TEXT, payload TEXT, PRIMARY KEY(scope,name))")
    }

    @Synchronized
    fun readThreads(): List<ThreadItem> = readableDatabase.rawQuery("SELECT payload FROM metadata WHERE scope=? AND name='threads'", arrayOf(scope)).use { cursor ->
        if (cursor.moveToFirst()) json.decodeFromString<List<ThreadItem>>(cursor.getString(0)) else emptyList()
    }

    @Synchronized
    fun saveThreads(threads: List<ThreadItem>) {
        val values = ContentValues().apply { put("scope", scope); put("name", "threads"); put("payload", json.encodeToString(threads)) }
        writableDatabase.insertWithOnConflict("metadata", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    @Synchronized
    fun registerVisitedThread(id: String, title: String) {
        if (id == "main") return
        val threads = readThreads()
        if (threads.none { it.id == id }) saveThreads(threads + ThreadItem(id = id, name = title))
    }

    @Synchronized
    fun read(session: String): MessagePage? {
        val database = readableDatabase
        val metadata = database.rawQuery("SELECT revision,has_more FROM pages WHERE scope=? AND session=?", arrayOf(scope, session)).use { cursor ->
            if (!cursor.moveToFirst()) return null
            cursor.getLong(0) to (cursor.getInt(1) == 1)
        }
        val messages = mutableListOf<ChatMessage>()
        database.rawQuery("SELECT payload FROM messages WHERE scope=? AND session=? ORDER BY position", arrayOf(scope, session)).use { cursor ->
            while (cursor.moveToNext()) messages.add(json.decodeFromString<ChatMessage>(cursor.getString(0)).copy(isStreaming = false))
        }
        return MessagePage(messages, metadata.second, messages.firstOrNull()?.id, metadata.first)
    }

    @Synchronized
    fun writePage(session: String, page: MessagePage, latest: Boolean) {
        val previous = read(session)
        val database = writableDatabase
        database.beginTransaction()
        try {
            val sameRevision = previous?.historyRevision == page.historyRevision
            if (!sameRevision || (latest && page.messages.isEmpty())) database.delete("messages", "scope=? AND session=?", arrayOf(scope, session))
            val firstPosition = page.messages.firstOrNull()?.historyIndex
            val overlapsBoundary = previous?.messages?.any { it.id == page.messages.firstOrNull()?.id } == true
            val keepsOlder = sameRevision && latest && overlapsBoundary && firstPosition != null && previous?.messages?.any { it.historyIndex < firstPosition } == true
            if (latest && !overlapsBoundary && firstPosition != null) database.delete("messages", "scope=? AND session=? AND position<?", arrayOf(scope, session, firstPosition.toString()))
            if (latest && firstPosition != null) database.delete("messages", "scope=? AND session=? AND position>=?", arrayOf(scope, session, firstPosition.toString()))
            insert(database, session, page.messages)
            val metadata = ContentValues().apply {
                put("scope", scope); put("session", session); put("revision", page.historyRevision)
                put("has_more", if (if (keepsOlder) previous?.hasMore == true else page.hasMore) 1 else 0)
            }
            database.insertWithOnConflict("pages", null, metadata, SQLiteDatabase.CONFLICT_REPLACE)
            database.setTransactionSuccessful()
        } finally { database.endTransaction() }
    }

    @Synchronized
    fun saveMessages(session: String, messages: List<ChatMessage>) {
        val database = writableDatabase
        database.beginTransaction()
        try {
            insert(database, session, messages)
            database.setTransactionSuccessful()
        } finally { database.endTransaction() }
    }

    private fun insert(database: SQLiteDatabase, session: String, messages: List<ChatMessage>) {
        var nextPosition = database.rawQuery("SELECT COALESCE(MAX(position),0) FROM messages WHERE scope=? AND session=?", arrayOf(scope, session)).use { it.moveToFirst(); it.getLong(0) }
        messages.forEach { message ->
            val existing = database.rawQuery("SELECT position FROM messages WHERE scope=? AND session=? AND id=?", arrayOf(scope, session, message.id)).use { if (it.moveToFirst()) it.getLong(0) else null }
            val position = message.historyIndex.takeIf { it > 0 } ?: existing ?: ++nextPosition
            val values = ContentValues().apply {
                put("scope", scope); put("session", session); put("id", message.id); put("position", position)
                put("payload", json.encodeToString(message.copy(historyIndex = position)))
            }
            database.insertWithOnConflict("messages", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    @Synchronized
    fun invalidate(session: String) {
        writableDatabase.delete("messages", "scope=? AND session=?", arrayOf(scope, session))
        writableDatabase.delete("pages", "scope=? AND session=?", arrayOf(scope, session))
    }
}
