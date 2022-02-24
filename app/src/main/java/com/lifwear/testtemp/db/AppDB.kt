package com.lifwear.testtemp.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.lifwear.App

/**
 * @author WillXia
 * @date 2022/2/23.
 */
@Database(entities = [Temp::class], version = 1)
abstract class AppDB : RoomDatabase() {

    abstract fun tempDao(): TempDao

    companion object {
        private var db: AppDB? = null

        fun getInstance(): AppDB {
            if (db == null) {
                db = Room.databaseBuilder(
                    App.getContext(),
                    AppDB::class.java, "database-name"
                ).build()
            }

            return db as AppDB
        }
    }
}