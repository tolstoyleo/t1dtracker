package com.example.t1dtracker

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

private val Context.dataStore by preferencesDataStore(name = "tracker_data")

class DataStoreManager(private val context: Context) {

    companion object {
        private val ENTRIES_KEY = stringPreferencesKey("entries")
    }

    suspend fun saveEntry(entryType: String, dateMillis: Long, hour: Int, minute: Int, notes: String) {
        // Create a new entry as JSON
        val newEntry = JSONObject().apply {
            put("id", System.currentTimeMillis()) // Use timestamp as unique ID
            put("type", entryType)
            put("date", dateMillis)
            put("hour", hour)
            put("minute", minute)
            put("notes", notes)
        }

        // Get existing entries
        val existingEntries = getAllEntries()

        // Add new entry
        val updatedArray = JSONArray(existingEntries)
        updatedArray.put(newEntry)

        // Save back to DataStore
        context.dataStore.edit { preferences ->
            preferences[ENTRIES_KEY] = updatedArray.toString()
        }
    }

    suspend fun getAllEntries(): String {
        return context.dataStore.data.map { preferences ->
            preferences[ENTRIES_KEY] ?: "[]"
        }.first()
    }

    suspend fun updateEntry(entryId: Long, entryType: String, dateMillis: Long, hour: Int, minute: Int, notes: String) {
        val existingEntries = getAllEntries()
        val jsonArray = JSONArray(existingEntries)

        // Find and update the entry
        for (i in 0 until jsonArray.length()) {
            val json = jsonArray.getJSONObject(i)
            if (json.getLong("id") == entryId) {
                json.put("type", entryType)
                json.put("date", dateMillis)
                json.put("hour", hour)
                json.put("minute", minute)
                json.put("notes", notes)
                break
            }
        }

        // Save back to DataStore
        context.dataStore.edit { preferences ->
            preferences[ENTRIES_KEY] = jsonArray.toString()
        }
    }

    suspend fun deleteEntry(entryId: Long) {
        val existingEntries = getAllEntries()
        val jsonArray = JSONArray(existingEntries)
        val updatedArray = JSONArray()

        // Copy all entries except the one to delete
        for (i in 0 until jsonArray.length()) {
            val json = jsonArray.getJSONObject(i)
            if (json.getLong("id") != entryId) {
                updatedArray.put(json)
            }
        }

        // Save back to DataStore
        context.dataStore.edit { preferences ->
            preferences[ENTRIES_KEY] = updatedArray.toString()
        }
    }

    fun generateCSV(entries: List<TimelineEntry>): String {
        val header = "Type,Date,Time,Notes\n"
        val rows = entries.joinToString("\n") { entry ->
            val calendar = Calendar.getInstance().apply { timeInMillis = entry.dateMillis }
            val date = "${calendar.get(Calendar.MONTH) + 1}/${calendar.get(Calendar.DAY_OF_MONTH)}/${calendar.get(Calendar.YEAR)}"
            val amPm = if (entry.hour < 12) "AM" else "PM"
            val displayHour = when {
                entry.hour == 0 -> 12
                entry.hour > 12 -> entry.hour - 12
                else -> entry.hour
            }
            val time = String.format("%d:%02d %s", displayHour, entry.minute, amPm)
            val escapedNotes = entry.notes.replace("\"", "\"\"") // Escape quotes
            "\"${entry.type}\",\"$date\",\"$time\",\"$escapedNotes\""
        }
        return header + rows
    }
}