package com.example.t1dtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.compose.material3.FilterChip
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.LaunchedEffect
import org.json.JSONArray
import androidx.compose.runtime.rememberCoroutineScope
import com.example.t1dtracker.ui.theme.T1DTrackerTheme
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            T1DTrackerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun MainApp(modifier: Modifier = Modifier) {
    var selectedButton by remember { mutableStateOf<String?>(null) }
    var editingEntry by remember { mutableStateOf<TimelineEntry?>(null) }
    var historyFilter by remember { mutableStateOf<String?>(null) }

    when {
        editingEntry != null -> {
            // Show edit screen
            EditEntryScreen(
                entry = editingEntry!!,
                onBack = {
                    editingEntry = null
                    selectedButton = "History" // Go back to history, not main menu
                },
                onSave = {
                    editingEntry = null
                    selectedButton = "History" // Go back to history after save/delete
                }
            )
        }
        selectedButton == "History" -> {
            // Show timeline screen
            TimelineScreen(
                onBack = {
                    selectedButton = null
                    historyFilter = null // Reset filter when leaving history
                },
                onEditEntry = { entry -> editingEntry = entry },
                selectedFilter = historyFilter,
                onFilterChange = { filter -> historyFilter = filter }
            )
        }
        selectedButton != null -> {
            // Show date picker screen for new entry
            DatePickerScreen(
                title = selectedButton!!,
                onBack = { selectedButton = null }
            )
        }
        else -> {
            // Show main screen with buttons
            MainScreen(
                modifier = modifier,
                onButtonClick = { buttonName -> selectedButton = buttonName }
            )
        }
    }
}

@Composable
fun MainScreen(modifier: Modifier = Modifier, onButtonClick: (String) -> Unit) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        BigButton(text = "Short Acting", onClick = { onButtonClick("Short Acting") })
        BigButton(text = "Long Acting", onClick = { onButtonClick("Long Acting") })
        BigButton(text = "Glucose", onClick = { onButtonClick("Glucose") })
        BigButton(text = "Food", onClick = { onButtonClick("Food") })
        BigButton(text = "Exercise", onClick = { onButtonClick("Exercise") })
        BigButton(text = "History", onClick = { onButtonClick("History") })
    }
}

@Composable
fun BigButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(100.dp)
    ) {
        Text(
            text = text,
            fontSize = 24.sp
        )
    }
}

@Composable
fun TimelineScreen(
    onBack: () -> Unit,
    onEditEntry: (TimelineEntry) -> Unit,
    selectedFilter: String?,
    onFilterChange: (String?) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }
    var allEntries by remember { mutableStateOf<List<TimelineEntry>>(emptyList()) }

    // Load entries when screen opens
    LaunchedEffect(Unit) {
        val jsonString = dataStoreManager.getAllEntries()
        val jsonArray = JSONArray(jsonString)
        val loadedEntries = mutableListOf<TimelineEntry>()

        for (i in 0 until jsonArray.length()) {
            val json = jsonArray.getJSONObject(i)
            loadedEntries.add(
                TimelineEntry(
                    id = json.getLong("id"),
                    type = json.getString("type"),
                    dateMillis = json.getLong("date"),
                    hour = json.getInt("hour"),
                    minute = json.getInt("minute"),
                    notes = json.getString("notes")
                )
            )
        }

        // Sort by date and time (most recent first)
        allEntries = loadedEntries.sortedWith(
            compareByDescending<TimelineEntry> { it.dateMillis }
                .thenByDescending { it.hour * 60 + it.minute }
        )
    }

    // Filter entries based on selected filter
    val displayedEntries = if (selectedFilter == null) {
        allEntries
    } else {
        allEntries.filter { it.type == selectedFilter }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 0.dp)
    ) {
        // Title
        Text(
            text = "History",
            fontSize = 32.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == null,
                onClick = { onFilterChange(null) },
                label = { Text("All") }
            )
            FilterChip(
                selected = selectedFilter == "Short Acting",
                onClick = { onFilterChange("Short Acting") },
                label = { Text("Short") }
            )
            FilterChip(
                selected = selectedFilter == "Long Acting",
                onClick = { onFilterChange("Long Acting") },
                label = { Text("Long") }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == "Glucose",
                onClick = { onFilterChange("Glucose") },
                label = { Text("Glucose") }
            )
            FilterChip(
                selected = selectedFilter == "Food",
                onClick = { onFilterChange("Food") },
                label = { Text("Food") }
            )
            FilterChip(
                selected = selectedFilter == "Exercise",
                onClick = { onFilterChange("Exercise") },
                label = { Text("Exercise") }
            )
        }

        // List of entries (scrollable, takes remaining space)
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(displayedEntries.size) { index ->
                EntryCard(
                    entry = displayedEntries[index],
                    onClick = { onEditEntry(displayedEntries[index]) }
                )
            }

            // Add spacing at the bottom so last item is fully visible
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Back and Export buttons side by side
        Spacer(modifier = Modifier.height(16.dp))

        val createDocumentLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("text/csv")
        ) { uri ->
            uri?.let {
                try {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        val csv = dataStoreManager.generateCSV(displayedEntries)
                        outputStream.write(csv.toByteArray())
                    }
                    Toast.makeText(context, "Export successful!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 80.dp)
                    .height(60.dp)
            ) {
                Text("Back", fontSize = 20.sp)
            }

            Button(
                onClick = {
                    createDocumentLauncher.launch("t1d_tracker_export.csv")
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 80.dp)
                    .height(60.dp)
            ) {
                Text("Export CSV", fontSize = 20.sp)
            }
        }
    }
}

@Composable
fun EntryCard(entry: TimelineEntry, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Entry type
            Text(
                text = entry.type,
                fontSize = 20.sp,
                style = MaterialTheme.typography.titleMedium
            )

            // Date and time
            Text(
                text = "${formatDate(entry.dateMillis)} at ${formatTime(entry.hour, entry.minute)}",                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            // Notes (if any)
            if (entry.notes.isNotEmpty()) {
                Text(
                    text = "Notes: ${entry.notes}",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

data class TimelineEntry(
    val id: Long,
    val type: String,
    val dateMillis: Long,
    val hour: Int,
    val minute: Int,
    val notes: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerScreen(title: String, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val dataStoreManager = remember { DataStoreManager(context) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    val currentTime = Calendar.getInstance()
    val timePickerState = rememberTimePickerState(
        initialHour = currentTime.get(Calendar.HOUR_OF_DAY),
        initialMinute = currentTime.get(Calendar.MINUTE)
    )

    var notes by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var isSubmitted by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title at top
        Text(
            text = title,
            fontSize = 32.sp
        )

        // Date button
        OutlinedButton(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Date: ${formatDate(datePickerState.selectedDateMillis)}",
                fontSize = 18.sp
            )
        }

        // Time button
        OutlinedButton(
            onClick = { showTimePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Time: ${String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)}",
                fontSize = 18.sp
            )
        }

        // Notes input box
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes") },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            maxLines = 5
        )

        // Back and Submit buttons side by side
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
            ) {
                Text("Back", fontSize = 20.sp)
            }

            Button(
                onClick = {
                    if (!isSubmitted) {
                        isSubmitted = true
                        scope.launch {
                            dataStoreManager.saveEntry(
                                entryType = title,
                                dateMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis(),
                                hour = timePickerState.hour,
                                minute = timePickerState.minute,
                                notes = notes
                            )
                            Toast.makeText(context, "Entry saved!", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = !isSubmitted,
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
            ) {
                Text("Submit", fontSize = 20.sp)
            }
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time picker dialog
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }
}

fun formatDate(millis: Long?): String {
    if (millis == null) return "Select date"
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = millis
    return "${calendar.get(Calendar.MONTH) + 1}/${calendar.get(Calendar.DAY_OF_MONTH)}/${calendar.get(Calendar.YEAR)}"
}

fun formatTime(hour: Int, minute: Int): String {
    val amPm = if (hour < 12) "AM" else "PM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return String.format("%d:%02d %s", displayHour, minute, amPm)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEntryScreen(entry: TimelineEntry, onBack: () -> Unit, onSave: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val dataStoreManager = remember { DataStoreManager(context) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = entry.dateMillis
    )

    val timePickerState = rememberTimePickerState(
        initialHour = entry.hour,
        initialMinute = entry.minute
    )

    var notes by remember { mutableStateOf(entry.notes) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var isSaved by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title at top
        Text(
            text = "Edit ${entry.type}",
            fontSize = 32.sp
        )

        // Date button
        OutlinedButton(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Date: ${formatDate(datePickerState.selectedDateMillis)}",
                fontSize = 18.sp
            )
        }

        // Time button
        OutlinedButton(
            onClick = { showTimePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Time: ${String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)}",
                fontSize = 18.sp
            )
        }

        // Notes input box
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes") },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            maxLines = 5
        )

        //Spacer(modifier = Modifier.weight(1f))

        // Cancel and Save buttons side by side
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
            ) {
                Text("Cancel", fontSize = 20.sp)
            }

            Button(
                onClick = {
                    if (!isSaved) {
                        isSaved = true
                        scope.launch {
                            dataStoreManager.updateEntry(
                                entryId = entry.id,
                                entryType = entry.type,
                                dateMillis = datePickerState.selectedDateMillis ?: entry.dateMillis,
                                hour = timePickerState.hour,
                                minute = timePickerState.minute,
                                notes = notes
                            )
                            Toast.makeText(context, "Entry updated!", Toast.LENGTH_SHORT).show()
                            onSave()
                        }
                    }
                },
                enabled = !isSaved,
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
            ) {
                Text("Save", fontSize = 20.sp)
            }
        }

        // Delete button
        Button(
            onClick = { showDeleteDialog = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Delete Entry", fontSize = 18.sp)
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Entry") },
            text = { Text("Are you sure you want to delete this entry? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            dataStoreManager.deleteEntry(entry.id)
                            Toast.makeText(context, "Entry deleted", Toast.LENGTH_SHORT).show()
                            onSave() // Go back to timeline
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Date picker dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time picker dialog
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    T1DTrackerTheme {
        MainScreen(onButtonClick = {})
    }
}