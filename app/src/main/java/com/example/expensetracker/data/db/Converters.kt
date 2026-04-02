package com.example.expensetracker.data.db

import androidx.room.TypeConverter
import com.example.expensetracker.domain.model.Interval
import java.time.Instant
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromLocalDate(date: LocalDate): String = date.toString()

    @TypeConverter
    fun toLocalDate(value: String): LocalDate = LocalDate.parse(value)

    @TypeConverter
    fun fromInstant(instant: Instant): String = instant.toString()

    @TypeConverter
    fun toInstant(value: String): Instant = Instant.parse(value)

    @TypeConverter
    fun fromInterval(interval: Interval): String = interval.name

    @TypeConverter
    fun toInterval(value: String): Interval = Interval.valueOf(value)
}
