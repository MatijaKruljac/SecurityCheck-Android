package com.securitycheck.demoapp.services.database

import android.content.ContentValues
import android.content.Context
import net.sqlcipher.Cursor
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SQLiteOpenHelper
import com.securitycheck.demoapp.services.helpers.User

class DBCipherHandler(context: Context, passphrase: String) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {

        private const val DATABASE_NAME = "security-check-cipher.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_NAME = "users"
        private const val COLUMN_ID = "id"
        private const val COLUMN_FIRST_NAME = "first_name"
        private const val COLUMN_LAST_NAME = "last_name"
    }

    private val passphrase: String

    init {
        this.passphrase = passphrase // Master key -> should be stored in KeyStore

        SQLiteDatabase.loadLibs(context)
        val database = SQLiteDatabase.openOrCreateDatabase(":memory:", passphrase, null)
        database.execSQL("PRAGMA cipher_memory_security = ON;") // Disable for performance improvement
        database.close()
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createUserTableQuery = ("CREATE TABLE $TABLE_NAME (" +
                "$COLUMN_ID INTEGER PRIMARY KEY, " +
                "$COLUMN_FIRST_NAME TEXT, " +
                "$COLUMN_LAST_NAME TEXT)")

        db.execSQL(createUserTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Drop the table if it exists
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun addUser(firstName: String, lastName: String) {
        val db = getWritableDatabase(passphrase)
        val values = ContentValues()
        values.put(COLUMN_FIRST_NAME, firstName)
        values.put(COLUMN_LAST_NAME, lastName)
        db.insert(TABLE_NAME, null, values)
        db.close()
    }

    fun getAllUsers(): ArrayList<User> {
        val db = getReadableDatabase(passphrase)
        val userList = ArrayList<User>()
        val selectAllQuery = "SELECT * FROM $TABLE_NAME"
        val cursor: Cursor = db.rawQuery(selectAllQuery, null)
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndex(COLUMN_ID))
                val firstName = cursor.getString(cursor.getColumnIndex(COLUMN_FIRST_NAME))
                val lastName = cursor.getString(cursor.getColumnIndex(COLUMN_LAST_NAME))
                val user = User(id, firstName, lastName)
                userList.add(user)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()

        return userList
    }

    fun deleteAllUsers() {
        val db = getWritableDatabase(passphrase)
        db.delete(TABLE_NAME, null, null)
        db.close()
    }
}