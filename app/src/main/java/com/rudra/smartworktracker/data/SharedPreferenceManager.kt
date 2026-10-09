package com.rudra.smartworktracker.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.rudra.smartworktracker.model.WorkLog
import com.rudra.smartworktracker.ui.screens.team.DutySwap
import com.rudra.smartworktracker.ui.screens.team.Team
import java.time.LocalDate
import java.time.LocalTime

class SharedPreferenceManager(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("WorkLogs", Context.MODE_PRIVATE)

    /**
     * java.time and Uri get explicit adapters: Gson cannot construct the abstract android.net.Uri
     * (loading teams crashed once a contact photo was saved), and reflecting into platform
     * java.time internals is fragile. Readers also accept the object form older versions wrote.
     */
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter().nullSafe())
        .registerTypeAdapter(LocalTime::class.java, LocalTimeAdapter().nullSafe())
        .registerTypeHierarchyAdapter(Uri::class.java, UriAdapter())
        .create()

    fun saveWorkLogs(workLogs: List<WorkLog>) {
        val json = gson.toJson(workLogs)
        sharedPreferences.edit().putString("work_logs", json).apply()
    }

    fun getWorkLogs(): List<WorkLog> {
        val json = sharedPreferences.getString("work_logs", null) ?: return emptyList()
        return readList(json, object : TypeToken<List<WorkLog>>() {})
    }

    fun saveTeams(teams: List<Team>) {
        val json = gson.toJson(teams)
        sharedPreferences.edit().putString("teams", json).apply()
    }

    fun getTeams(): List<Team> {
        val json = sharedPreferences.getString("teams", null) ?: return emptyList()
        return readList(json, object : TypeToken<List<Team>>() {})
    }

    fun saveDutySwaps(swaps: List<DutySwap>) {
        sharedPreferences.edit().putString("duty_swaps", gson.toJson(swaps)).apply()
    }

    fun getDutySwaps(): List<DutySwap> {
        val json = sharedPreferences.getString("duty_swaps", null) ?: return emptyList()
        return readList(json, object : TypeToken<List<DutySwap>>() {})
    }

    private fun <T> readList(json: String, token: TypeToken<List<T>>): List<T> {
        return try {
            gson.fromJson<List<T>>(json, token.type)?.filterNotNull() ?: emptyList()
        } catch (e: Exception) {
            // Corrupt or incompatible data must not crash the screen
            Log.e("SharedPreferenceManager", "Could not read stored list", e)
            emptyList()
        }
    }

    private class LocalDateAdapter : TypeAdapter<LocalDate>() {
        override fun write(out: JsonWriter, value: LocalDate) {
            out.value(value.toString())
        }

        override fun read(reader: JsonReader): LocalDate {
            if (reader.peek() == JsonToken.STRING) return LocalDate.parse(reader.nextString())
            val obj = JsonParser.parseReader(reader).asJsonObject
            return LocalDate.of(obj["year"].asInt, obj["month"].asInt, obj["day"].asInt)
        }
    }

    private class LocalTimeAdapter : TypeAdapter<LocalTime>() {
        override fun write(out: JsonWriter, value: LocalTime) {
            out.value(value.toString())
        }

        override fun read(reader: JsonReader): LocalTime {
            if (reader.peek() == JsonToken.STRING) return LocalTime.parse(reader.nextString())
            val obj = JsonParser.parseReader(reader).asJsonObject
            return LocalTime.of(
                obj["hour"]?.asInt ?: 0,
                obj["minute"]?.asInt ?: 0,
                obj["second"]?.asInt ?: 0
            )
        }
    }

    private class UriAdapter : TypeAdapter<Uri?>() {
        override fun write(out: JsonWriter, value: Uri?) {
            if (value == null) out.nullValue() else out.value(value.toString())
        }

        override fun read(reader: JsonReader): Uri? {
            return when (reader.peek()) {
                JsonToken.NULL -> {
                    reader.nextNull()
                    null
                }
                JsonToken.STRING -> Uri.parse(reader.nextString())
                else -> {
                    // Older versions reflected Uri internals; recover the string if present
                    val obj = JsonParser.parseReader(reader).asJsonObject
                    obj["uriString"]?.takeIf { it.isJsonPrimitive }?.asString?.let(Uri::parse)
                }
            }
        }
    }
}
