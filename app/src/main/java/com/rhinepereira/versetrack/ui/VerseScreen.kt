package com.rhinepereira.versetrack.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rhinepereira.versetrack.data.BibleData
import com.rhinepereira.versetrack.data.BibleDatabaseHelper
import com.rhinepereira.versetrack.data.Note
import com.rhinepereira.versetrack.data.NoteWithVerses
import com.rhinepereira.versetrack.data.Verse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerseScreen(
    viewModel: VerseViewModel = viewModel(),
    sharedText: String? = null,
    onSharedTextConsumed: () -> Unit = {}
) {
    var selectedNoteWithVerses by remember { mutableStateOf<NoteWithVerses?>(null) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var showAddVerseDialog by remember { mutableStateOf(false) }
    var showSharedTextDialog by remember { mutableStateOf(false) }
    var verseToEdit by remember { mutableStateOf<Verse?>(null) }
    
    // Deletion confirmation states
    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var verseToDelete by remember { mutableStateOf<Verse?>(null) }

    val notesWithVerses by viewModel.allNotesWithVerses.collectAsState()

    LaunchedEffect(sharedText) {
        if (sharedText != null) {
            showSharedTextDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedNoteWithVerses?.note?.theme ?: "Verse Track") },
                navigationIcon = {
                    if (selectedNoteWithVerses != null) {
                        IconButton(onClick = { selectedNoteWithVerses = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (selectedNoteWithVerses == null) showAddNoteDialog = true else showAddVerseDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (selectedNoteWithVerses == null) {
                // Themes Grid
                if (notesWithVerses.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No themes yet. Add one to start!", color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(notesWithVerses, key = { it.note.id }) { noteWithVerses ->
                            ThemeCard(
                                noteWithVerses = noteWithVerses,
                                onClick = { selectedNoteWithVerses = noteWithVerses },
                                onDelete = { noteToDelete = noteWithVerses.note }
                            )
                        }
                    }
                }
            } else {
                // Verses List for selected Theme
                val verses by viewModel.getVersesForNote(selectedNoteWithVerses!!.note.id).collectAsState(initial = emptyList())
                BackHandler { selectedNoteWithVerses = null }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(verses, key = { it.id }) { verse ->
                        VerseItem(
                            verse = verse,
                            onDelete = { verseToDelete = verse },
                            onEdit = { verseToEdit = verse }
                        )
                    }
                }
            }
        }

        if (showAddNoteDialog) {
            AddNoteDialog(
                onDismiss = { showAddNoteDialog = false },
                onConfirm = { theme ->
                    viewModel.addNote(theme)
                    showAddNoteDialog = false
                }
            )
        }

        if (showAddVerseDialog && selectedNoteWithVerses != null) {
            AddVerseDialog(
                onDismiss = { showAddVerseDialog = false },
                onConfirm = { reference, content ->
                    viewModel.addVerse(selectedNoteWithVerses!!.note.id, reference, content)
                    showAddVerseDialog = false
                }
            )
        }

        if (verseToEdit != null) {
            EditVerseDialog(
                verse = verseToEdit!!,
                onDismiss = { verseToEdit = null },
                onConfirm = { updatedReference, updatedContent ->
                    viewModel.updateVerse(verseToEdit!!.copy(reference = updatedReference, content = updatedContent))
                    verseToEdit = null
                }
            )
        }

        if (showSharedTextDialog && sharedText != null) {
            SharedTextDialog(
                sharedText = sharedText,
                themes = notesWithVerses.map { it.note },
                onDismiss = {
                    showSharedTextDialog = false
                    onSharedTextConsumed()
                },
                onConfirm = { noteId, themeName, reference, content ->
                    if (noteId != null) {
                        viewModel.addVerse(noteId, reference, content)
                    } else if (themeName != null) {
                        viewModel.addNoteAndVerse(themeName, reference, content)
                    }
                    showSharedTextDialog = false
                    onSharedTextConsumed()
                }
            )
        }

        // Delete Confirmation Dialogs
        noteToDelete?.let { note ->
            DeleteConfirmationDialog(
                title = "Delete Theme",
                message = "Are you sure you want to delete the theme \"${note.theme}\" and all its verses?",
                onConfirm = {
                    viewModel.deleteNote(note)
                    noteToDelete = null
                },
                onDismiss = { noteToDelete = null }
            )
        }

        verseToDelete?.let { verse ->
            DeleteConfirmationDialog(
                title = "Delete Verse",
                message = "Are you sure you want to delete this verse (${verse.reference})?",
                onConfirm = {
                    viewModel.deleteVerse(verse)
                    verseToDelete = null
                },
                onDismiss = { verseToDelete = null }
            )
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditVerseDialog(
    verse: Verse,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var reference by remember { mutableStateOf(verse.reference) }
    var content by remember { mutableStateOf(verse.content) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Verse") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text("Reference") },
                    modifier = Modifier.fillMaxWidth()
                )
                TextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(onClick = { if (reference.isNotBlank() && content.isNotBlank()) onConfirm(reference, content) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SharedTextDialog(
    sharedText: String,
    themes: List<Note>,
    onDismiss: () -> Unit,
    onConfirm: (String?, String?, String, String) -> Unit
) {
    var selectedNoteId by remember { mutableStateOf<String?>(themes.firstOrNull()?.id) }
    var newThemeName by remember { mutableStateOf("") }
    var isNewTheme by remember { mutableStateOf(themes.isEmpty()) }

    // Parsing logic
    val lines = sharedText.lines().filter { it.isNotBlank() }
    var reference = lines.firstOrNull() ?: ""
    
    // Remove Bible version (e.g., "RSV-C", "NIV", "KJV") from the end of the first line
    // This matches common 3-5 letter abbreviations at the end of the reference
    reference = reference.replace(Regex("\\s+[A-Z0-9-]{2,}$"), "").trim()
    
    val content = lines.drop(1).filter { !it.startsWith("http") }.joinToString("\n").trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Shared Verse") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Reference: $reference", fontWeight = FontWeight.Bold)
                Text(content, maxLines = 3, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                
                HorizontalDivider()
                
                Text("Choose Theme", style = MaterialTheme.typography.titleSmall)
                
                if (themes.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = !isNewTheme, onClick = { isNewTheme = false })
                        Text("Existing Theme", modifier = Modifier.clickable { isNewTheme = false })
                    }
                    
                    if (!isNewTheme) {
                        var expanded by remember { mutableStateOf(false) }
                        val selectedNote = themes.find { it.id == selectedNoteId }
                        
                        Box {
                            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(selectedNote?.theme ?: "Select Theme")
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                themes.forEach { note ->
                                    DropdownMenuItem(
                                        text = { Text(note.theme) },
                                        onClick = {
                                            selectedNoteId = note.id
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = isNewTheme, onClick = { isNewTheme = true })
                    Text("Create New Theme", modifier = Modifier.clickable { isNewTheme = true })
                }
                
                if (isNewTheme) {
                    TextField(
                        value = newThemeName,
                        onValueChange = { newThemeName = it },
                        label = { Text("New Theme Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (isNewTheme) {
                    if (newThemeName.isNotBlank()) onConfirm(null, newThemeName, reference, content)
                } else {
                    onConfirm(selectedNoteId, null, reference, content)
                }
            }) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ThemeCard(noteWithVerses: NoteWithVerses, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = noteWithVerses.note.theme,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Theme",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val recentVerses = noteWithVerses.verses.sortedByDescending { it.createdAt }.take(2)
            if (recentVerses.isEmpty()) {
                Text(
                    "No verses yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            } else {
                recentVerses.forEach { verse ->
                    Column(modifier = Modifier.padding(bottom = 4.dp)) {
                        Text(
                            text = verse.reference,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = verse.content,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VerseItem(verse: Verse, onDelete: () -> Unit, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = verse.reference,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Verse")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Verse")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = verse.content,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun AddNoteDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var theme by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Theme") },
        text = {
            TextField(value = theme, onValueChange = { theme = it }, label = { Text("Theme (e.g. Faith, Hope)") })
        },
        confirmButton = {
            Button(onClick = { if (theme.isNotBlank()) onConfirm(theme) }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddVerseDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    val context = LocalContext.current
    val bibleHelper = remember { BibleDatabaseHelper(context) }
    
    var bookInput by remember { mutableStateOf("") }
    var filteredBooks by remember { mutableStateOf(emptyList<String>()) }
    var expanded by remember { mutableStateOf(false) }
    
    var chapter by remember { mutableStateOf("") }
    var verseStart by remember { mutableStateOf("") }
    var verseEnd by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    
    var showVerseFetchConfirmation by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(16.dp),
        title = { Text("Add Bible Verse") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                // Book Selection with Search Suggestion
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = bookInput,
                        onValueChange = {
                            bookInput = it
                            filteredBooks = if (it.isEmpty()) {
                                emptyList()
                            } else {
                                BibleData.catholicBooks.filter { book ->
                                    book.contains(it, ignoreCase = true)
                                }
                            }
                            expanded = filteredBooks.isNotEmpty()
                        },
                        label = { Text("Book") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )
                    
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        properties = PopupProperties(focusable = false),
                        modifier = Modifier.fillMaxWidth(0.8f) // Avoid covering whole screen
                    ) {
                        filteredBooks.take(5).forEach { book ->
                            DropdownMenuItem(
                                text = { Text(book) },
                                onClick = {
                                    bookInput = book
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = chapter,
                        onValueChange = { if (it.all { char -> char.isDigit() }) chapter = it },
                        label = { Text("Ch.") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = verseStart,
                        onValueChange = { if (it.all { char -> char.isDigit() }) verseStart = it },
                        label = { Text("Ver.") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = verseEnd,
                        onValueChange = { if (it.all { char -> char.isDigit() }) verseEnd = it },
                        label = { Text("End") },
                        placeholder = { Text("-") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Verse Content") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Button(
                    onClick = {
                        val c = chapter.toIntOrNull()
                        val v = verseStart.toIntOrNull()
                        val ve = verseEnd.toIntOrNull()
                        if (bookInput.isNotBlank() && c != null && v != null) {
                            val fetched = bibleHelper.getVerseRange(bookInput, c, v, ve)
                            if (fetched != null) {
                                showVerseFetchConfirmation = fetched
                            } else {
                                // Handle not found (optional toast)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = bookInput.isNotBlank() && chapter.isNotBlank() && verseStart.isNotBlank()
                ) {
                    Text("Fetch Verse from Bible.db")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (bookInput.isNotBlank() && chapter.isNotBlank() && verseStart.isNotBlank() && content.isNotBlank()) {
                    val ref = if (verseEnd.isBlank()) {
                        "$bookInput $chapter:$verseStart"
                    } else {
                        "$bookInput $chapter:$verseStart-$verseEnd"
                    }
                    onConfirm(ref, content)
                }
            }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
    
    showVerseFetchConfirmation?.let { fetchedContent ->
        AlertDialog(
            onDismissRequest = { showVerseFetchConfirmation = null },
            title = { Text("Use this verse content?") },
            text = { Text(fetchedContent) },
            confirmButton = {
                Button(onClick = {
                    content = fetchedContent
                    showVerseFetchConfirmation = null
                }) { Text("Yes, use it") }
            },
            dismissButton = {
                TextButton(onClick = { showVerseFetchConfirmation = null }) { Text("No") }
            }
        )
    }
}
