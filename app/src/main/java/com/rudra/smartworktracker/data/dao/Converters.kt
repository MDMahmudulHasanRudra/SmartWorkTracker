package com.rudra.smartworktracker.data.dao

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rudra.smartworktracker.data.entity.DayOfWeek
import java.time.LocalDate

class Converters {
    private val gson = Gson()
    
    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDate? {
        return value?.let { LocalDate.ofEpochDay(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDate?): Long? {
        return date?.toEpochDay()
    }

    @TypeConverter
    fun fromString(value: String?): List<String> {
        // "".split(",") yields [""], which turned empty lists into a single blank entry
        return if (value.isNullOrEmpty()) emptyList() else value.split(",")
    }

    @TypeConverter
    fun fromList(list: List<String>?): String {
        return list?.joinToString(",") ?: ""
    }

    @TypeConverter
    fun fromDoubleList(value: String?): List<Double> {
        if (value == null) return emptyList()
        val type = object : TypeToken<List<Double>>() {}.type
        return try {
            gson.fromJson(value, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun toDoubleList(list: List<Double>?): String = gson.toJson(list ?: emptyList<Double>())

    @TypeConverter
    fun fromDayOfWeekList(value: String?): List<DayOfWeek> {
        if (value == null) return emptyList()
        val type = object : TypeToken<List<DayOfWeek>>() {}.type
        return try {
            gson.fromJson(value, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    @TypeConverter
    fun toDayOfWeekList(list: List<DayOfWeek>?): String = gson.toJson(list ?: emptyList<DayOfWeek>())
}
