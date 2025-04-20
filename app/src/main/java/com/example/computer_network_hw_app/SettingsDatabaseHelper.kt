package com.example.computer_network_hw_app

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {
    @Provides
    @Singleton
    fun provideSettingsModel(@ApplicationContext appContext: Context): SettingsModel {
        return SettingsModel(appContext)
    }
}



class SettingsModel(context: Context) : SQLiteOpenHelper(context.applicationContext, "settings.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE if not exists "settings" (
                "key" TEXT PRIMARY KEY,
                "value" TEXT
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {

    }

    fun getSetting(key: String): String? {
        val cursor = readableDatabase.query("settings", arrayOf("value"), "key = ?", arrayOf(key), null, null, null)
        cursor.use {
            if (it.moveToNext()) {
                return it.getString(0)
            }
        }
        return null;
    }

    fun setSetting(key: String, value: String) {
        writableDatabase.insertWithOnConflict("settings", null, ContentValues().apply {
            put("key", key)
            put("value", value)
        }, SQLiteDatabase.CONFLICT_REPLACE)
    }
}