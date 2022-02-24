package com.lifwear.testtemp.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/**
 * @author WillXia
 * @date 2022/2/23.
 */
@Dao
interface TempDao {
    @Insert
    fun insertTemp(temp: Temp)

    @Insert
    fun insertTemps(temps: List<Temp>)

    @Query("SELECT * FROM `Temp` WHERE mac =:mac")
    fun getDeviceTemp(mac: String): List<Temp>
}