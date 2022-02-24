package com.lifwear.testtemp.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * @author WillXia
 * @date 2022/2/23.
 */
@Entity
data class Temp(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo val aimTemp: String?,
    @ColumnInfo val enTemp: String?,
    @ColumnInfo val timestamp: String?,
    @ColumnInfo val mac: String?
)