package com.obsidianpay.mobile.storage

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Simple local SQLite store for offline cache of receipts/cards plus a debug
 * event log.
 *
 * NOTE (instructor): rows store the raw API JSON in cleartext on purpose. The
 * database lives under databases/obsidianpay_local.db and is a deliberate seam
 * for a future "insecure local SQLite" study. No encryption (no SQLCipher).
 */
class ObsidianLocalDb(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    data class CachedReceipt(
        val id: String,
        val ownerUserId: String?,
        val ownerRole: String?,
        val reference: String?,
        val amount: String?,
        val merchant: String?,
        val rawJson: String?,
        val cachedAt: String?,
    )

    data class CachedCard(
        val id: String,
        val ownerUserId: String?,
        val ownerRole: String?,
        val maskedNumber: String?,
        val cardType: String?,
        val rawJson: String?,
        val cachedAt: String?,
    )

    data class DebugEvent(
        val id: Long,
        val eventType: String,
        val details: String?,
        val createdAt: String?,
    )

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE cached_receipts (
                id TEXT PRIMARY KEY,
                ownerUserId TEXT,
                ownerRole TEXT,
                reference TEXT,
                amount TEXT,
                merchant TEXT,
                rawJson TEXT,
                cachedAt TEXT
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE cached_cards (
                id TEXT PRIMARY KEY,
                ownerUserId TEXT,
                ownerRole TEXT,
                maskedNumber TEXT,
                cardType TEXT,
                rawJson TEXT,
                cachedAt TEXT
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE debug_events (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                eventType TEXT,
                details TEXT,
                createdAt TEXT
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS cached_receipts")
        db.execSQL("DROP TABLE IF EXISTS cached_cards")
        db.execSQL("DROP TABLE IF EXISTS debug_events")
        onCreate(db)
    }

    private fun now(): String = System.currentTimeMillis().toString()

    fun cacheReceipt(
        id: String,
        ownerUserId: String?,
        ownerRole: String?,
        reference: String?,
        amount: String?,
        merchant: String?,
        rawJson: String?,
    ) {
        val values = ContentValues().apply {
            put("id", id)
            put("ownerUserId", ownerUserId)
            put("ownerRole", ownerRole)
            put("reference", reference)
            put("amount", amount)
            put("merchant", merchant)
            put("rawJson", rawJson)
            put("cachedAt", now())
        }
        writableDatabase.insertWithOnConflict(
            "cached_receipts", null, values, SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun cacheCard(
        id: String,
        ownerUserId: String?,
        ownerRole: String?,
        maskedNumber: String?,
        cardType: String?,
        rawJson: String?,
    ) {
        val values = ContentValues().apply {
            put("id", id)
            put("ownerUserId", ownerUserId)
            put("ownerRole", ownerRole)
            put("maskedNumber", maskedNumber)
            put("cardType", cardType)
            put("rawJson", rawJson)
            put("cachedAt", now())
        }
        writableDatabase.insertWithOnConflict(
            "cached_cards", null, values, SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun addDebugEvent(eventType: String, details: String?) {
        val values = ContentValues().apply {
            put("eventType", eventType)
            put("details", details)
            put("createdAt", now())
        }
        writableDatabase.insert("debug_events", null, values)
    }

    fun listCachedReceipts(): List<CachedReceipt> {
        val out = mutableListOf<CachedReceipt>()
        readableDatabase.rawQuery(
            "SELECT id, ownerUserId, ownerRole, reference, amount, merchant, rawJson, cachedAt FROM cached_receipts ORDER BY cachedAt DESC",
            null,
        ).use { c ->
            while (c.moveToNext()) {
                out.add(
                    CachedReceipt(
                        id = c.getString(0),
                        ownerUserId = c.getString(1),
                        ownerRole = c.getString(2),
                        reference = c.getString(3),
                        amount = c.getString(4),
                        merchant = c.getString(5),
                        rawJson = c.getString(6),
                        cachedAt = c.getString(7),
                    )
                )
            }
        }
        return out
    }

    fun listCachedCards(): List<CachedCard> {
        val out = mutableListOf<CachedCard>()
        readableDatabase.rawQuery(
            "SELECT id, ownerUserId, ownerRole, maskedNumber, cardType, rawJson, cachedAt FROM cached_cards ORDER BY cachedAt DESC",
            null,
        ).use { c ->
            while (c.moveToNext()) {
                out.add(
                    CachedCard(
                        id = c.getString(0),
                        ownerUserId = c.getString(1),
                        ownerRole = c.getString(2),
                        maskedNumber = c.getString(3),
                        cardType = c.getString(4),
                        rawJson = c.getString(5),
                        cachedAt = c.getString(6),
                    )
                )
            }
        }
        return out
    }

    fun listDebugEvents(limit: Int): List<DebugEvent> {
        val out = mutableListOf<DebugEvent>()
        readableDatabase.rawQuery(
            "SELECT id, eventType, details, createdAt FROM debug_events ORDER BY id DESC LIMIT ?",
            arrayOf(limit.toString()),
        ).use { c ->
            while (c.moveToNext()) {
                out.add(
                    DebugEvent(
                        id = c.getLong(0),
                        eventType = c.getString(1),
                        details = c.getString(2),
                        createdAt = c.getString(3),
                    )
                )
            }
        }
        return out
    }

    fun clearAll() {
        writableDatabase.apply {
            delete("cached_receipts", null, null)
            delete("cached_cards", null, null)
            delete("debug_events", null, null)
        }
    }

    companion object {
        const val DB_NAME = "obsidianpay_local.db"
        const val DB_VERSION = 1
    }
}
