package com.rhinepereira.versetrack.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rhinepereira.versetrack.data.PersonalNote
import com.rhinepereira.versetrack.data.PersonalNoteCategory
import com.rhinepereira.versetrack.data.BibleData
import com.rhinepereira.versetrack.data.BibleDatabaseHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(viewModel: NotesViewModel = viewModel()) {
    val categories by viewModel.categories.collectAsState()
    var noteToEdit by remember { mutableStateOf<PersonalNote?>(null) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var noteToDelete by remember { mutableStateOf<PersonalNote?>(null) }

    val pagerState = rememberPagerState(pageCount = { categories.size })
    val coroutineScope = rememberCoroutineScope()

    if (noteToEdit != null) {
        Dialog(
            onDismissRequest = { noteToEdit = null },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            FullScreenNoteEditor(
                note = noteToEdit!!,
                onDismiss = { noteToEdit = null },
                onSave = { title, content ->
                    viewModel.updateNote(noteToEdit!!.copy(title = title, content = content))
                }
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (categories.isNotEmpty()) {
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                edgePadding = 16.dp,
                divider = {}
            ) {
                categories.forEachIndexed { index, category ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = { Text(category.name) }
                    )
                }
                Tab(
                    selected = false,
                    onClick = { showAddCategoryDialog = true },
                    text = { Icon(Icons.Default.Add, contentDescription = "Add Category") }
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                val category = categories.getOrNull(pageIndex) ?: return@HorizontalPager
                val notes by viewModel.getNotesForCategory(category.id).collectAsState(initial = emptyList())
                
                if (notes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No notes here yet.", color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 80.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalItemSpacing = 12.dp
                    ) {
                        items(notes, key = { it.id }) { note ->
                            KeepNoteItem(
                                note = note,
                                onClick = { noteToEdit = note },
                                onDelete = { noteToDelete = note }
                            )
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = { 
                    val calendar = Calendar.getInstance()
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    
                    val currentCategoryId = categories.getOrNull(pagerState.currentPage)?.id ?: ""
                    
                    noteToEdit = PersonalNote(
                        categoryId = currentCategoryId, 
                        title = "", 
                        content = "",
                        date = calendar.timeInMillis
                    ) 
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Note")
            }
        }
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = { name ->
                viewModel.addCategory(name)
                showAddCategoryDialog = false
            }
        )
    }

    noteToDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("Delete Note") },
            text = { Text("Are you sure you want to delete this note?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteNote(note)
                        noteToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun KeepNoteItem(note: PersonalNote, onClick: () -> Unit, onDelete: () -> Unit) {
    val previewHighlight = MaterialTheme.colorScheme.primary
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                if (note.title.isNotBlank()) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                }
            }
            if (note.title.isNotBlank()) Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = parseMarkdown(note.content, hideMarkers = true, highlightColor = previewHighlight),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 8,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            val df = SimpleDateFormat("dd MMM", Locale.getDefault())
            Text(
                text = df.format(Date(note.date)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenNoteEditor(
    note: PersonalNote,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var title by remember { mutableStateOf(note.title) }
    var contentValue by remember { mutableStateOf(TextFieldValue(note.content)) }
    val context = LocalContext.current
    val bibleHelper = remember { BibleDatabaseHelper(context) }
    val boldColor = MaterialTheme.colorScheme.primary
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var lastContentValue by remember { mutableStateOf<TextFieldValue?>(null) }
    var detectedReference by remember { mutableStateOf<BibleRef?>(null) }

    LaunchedEffect(title, contentValue.text) {
        if (title != note.title || contentValue.text != note.content) {
            delay(1000)
            onSave(title, contentValue.text)
        }
    }

    LaunchedEffect(contentValue) {
        val text = contentValue.text
        val selection = contentValue.selection
        if (selection.collapsed && text.isNotEmpty()) {
            val textBeforeCursor = text.take(selection.start)
            val lastLine = textBeforeCursor.split("\n").lastOrNull() ?: ""
            
            if (lastLine.isNotBlank() && !lastLine.contains(" - ") && !lastLine.contains("**")) {
                detectedReference = findBibleReference(lastLine)
            } else {
                detectedReference = null
            }
        } else {
            detectedReference = null
        }
    }

    val dismissAndSave = {
        onSave(title, contentValue.text)
        onDismiss()
    }

    BackHandler { dismissAndSave() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = dismissAndSave) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column {
                AnimatedVisibility(
                    visible = detectedReference != null,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    detectedReference?.let { ref ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            tonalElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val refLabel = ref.originalText
                                    Text(
                                        text = "Add $refLabel",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Row {
                                    TextButton(onClick = { detectedReference = null }) {
                                        Text("Ignore")
                                    }
                                    Button(onClick = {
                                        val fetched = bibleHelper.getVerses(ref.book, ref.chapter, ref.verses)
                                        if (fetched != null) {
                                            val referenceText = ref.originalText
                                            
                                            val text = contentValue.text
                                            val selection = contentValue.selection
                                            val textBeforeCursor = text.take(selection.start)
                                            val textAfterCursor = text.substring(selection.end)
                                            val lineStart = textBeforeCursor.lastIndexOf('\n') + 1
                                            val currentLine = textBeforeCursor.substring(lineStart)
                                            
                                            val booksList = BibleData.catholicBooks.toMutableList()
                                            val abbrevList = BibleData.abbreviations.keys.toList()
                                            val allPatterns = (booksList + abbrevList).sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) }
                                            val regex = Regex("""\b(${allPatterns})\s+(\d+)(?::(\d+(?:-\d+)?(?:\s*,\s*\d+(?:-\d+)?)*))?\b""", RegexOption.IGNORE_CASE)
                                            val match = regex.find(currentLine)
                                            
                                            val newText: String
                                            val newSelection: TextRange
                                            
                                            if (match != null) {
                                                val lineBeforeMatch = currentLine.take(match.range.first)
                                                val lineAfterMatch = currentLine.substring(match.range.last + 1)
                                                val replacement = "$referenceText\n$fetched"
                                                val newLine = lineBeforeMatch + replacement + lineAfterMatch
                                                newText = text.take(lineStart) + newLine + "\n" + textAfterCursor
                                                newSelection = TextRange(lineStart + lineBeforeMatch.length + replacement.length + 1)
                                            } else {
                                                val appendText = "\n$referenceText\n$fetched\n"
                                                newText = text.take(selection.start) + appendText + textAfterCursor
                                                newSelection = TextRange(selection.start + appendText.length)
                                            }
                                            
                                            val oldContent = contentValue
                                            contentValue = contentValue.copy(text = newText, selection = newSelection)
                                            
                                            scope.launch {
                                                lastContentValue = oldContent
                                                val result = snackbarHostState.showSnackbar(
                                                    message = "Verse added",
                                                    actionLabel = "Undo",
                                                    duration = SnackbarDuration.Short
                                                )
                                                if (result == SnackbarResult.ActionPerformed) {
                                                    lastContentValue?.let {
                                                        contentValue = it
                                                    }
                                                }
                                            }
                                        }
                                        detectedReference = null 
                                    }) {
                                        Text("Add")
                                    }
                                }
                            }
                        }
                    }
                }

                Surface(tonalElevation = 3.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.ime)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = { contentValue = applyFormat(contentValue, "**") }) {
                            Icon(Icons.Default.FormatBold, contentDescription = "Bold")
                        }
                        IconButton(onClick = { contentValue = applyFormat(contentValue, "_") }) {
                            Icon(Icons.Default.FormatItalic, contentDescription = "Italic")
                        }
                        IconButton(onClick = { 
                            val newText = if (contentValue.text.endsWith("\n") || contentValue.text.isEmpty()) {
                                contentValue.text + "1. "
                            } else {
                                contentValue.text + "\n1. "
                            }
                            contentValue = contentValue.copy(text = newText, selection = TextRange(newText.length))
                        }) {
                            Icon(Icons.Default.FormatListNumbered, contentDescription = "Numbered List")
                        }
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets.statusBars
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Title", style = MaterialTheme.typography.headlineSmall) },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = MaterialTheme.typography.headlineSmall,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )
            TextField(
                value = contentValue,
                onValueChange = { newValue ->
                    contentValue = handleAutoList(contentValue, newValue)
                },
                placeholder = { Text("Note") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                visualTransformation = MarkdownVisualTransformation(boldColor)
            )
        }
    }
}

class MarkdownVisualTransformation(private val boldColor: Color) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        // Identity mapping requires original and transformed lengths to be identical.
        return TransformedText(
            text = parseMarkdown(text.text, hideMarkers = false, highlightColor = boldColor),
            offsetMapping = OffsetMapping.Identity
        )
    }
}

fun parseMarkdown(text: String, hideMarkers: Boolean, highlightColor: Color): AnnotatedString = buildAnnotatedString {
    if (text.isEmpty()) return@buildAnnotatedString
    
    val markerStyle = SpanStyle(color = Color.Gray.copy(alpha = 0.2f))
    val refHighlightColor = Color(0xFFFFD700) // Gold
    
    // Pre-calculate all Bible matches once for the entire text
    val bibleMatches = BibleData.bibleRefRegex.findAll(text).toList()
    
    var i = 0
    while (i < text.length) {
        // Check if current index is start of a Bible reference
        val bibleMatch = bibleMatches.find { it.range.first == i }
        
        when {
            bibleMatch != null -> {
                withStyle(SpanStyle(color = refHighlightColor, fontWeight = FontWeight.Medium)) {
                    append(bibleMatch.value)
                }
                i += bibleMatch.value.length
            }
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    if (hideMarkers) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = highlightColor)) {
                            append(text.substring(i + 2, end))
                        }
                    } else {
                        withStyle(markerStyle) { append("**") }
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = highlightColor)) {
                            append(text.substring(i + 2, end))
                        }
                        withStyle(markerStyle) { append("**") }
                    }
                    i = end + 2
                } else {
                    append(text[i])
                    i++
                }
            }
            text.startsWith("_", i) -> {
                val end = text.indexOf("_", i + 1)
                if (end != -1) {
                    if (hideMarkers) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(text.substring(i + 1, end))
                        }
                    } else {
                        withStyle(markerStyle) { append("_") }
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(text.substring(i + 1, end))
                        }
                        withStyle(markerStyle) { append("_") }
                    }
                    i = end + 1
                } else {
                    append(text[i])
                    i++
                }
            }
            else -> {
                append(text[i])
                i++
            }
        }
    }
}

data class BibleRef(val book: String, val chapter: Int, val verses: List<Pair<Int, Int?>>, val originalText: String)

fun findBibleReference(line: String): BibleRef? {
    val booksList = BibleData.catholicBooks.toMutableList()
    val abbrevList = BibleData.abbreviations.keys.toList()
    
    val allPatterns = (booksList + abbrevList).sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) }
    
    // Matches: Book Chapter[:VerseRange[, VerseRange]*]
    // VerseRange is StartVerse[-EndVerse]
    val regex = Regex("""\b(${allPatterns})\s+(\d+)(?::(\d+(?:-\d+)?(?:\s*,\s*\d+(?:-\d+)?)*))?\b""", RegexOption.IGNORE_CASE)
    
    val match = regex.find(line)
    if (match != null) {
        val matchedName = match.groupValues[1]
        val chapter = match.groupValues[2].toInt()
        val versesStr = match.groupValues[3]
        
        // Map abbreviation back to full name if necessary
        val book = BibleData.abbreviations.entries.find { it.key.equals(matchedName, ignoreCase = true) }?.value 
                  ?: matchedName
        
        val verseRanges = if (versesStr.isEmpty()) {
            emptyList()
        } else {
            versesStr.split(",").map { rangeStr ->
                val parts = rangeStr.trim().split("-")
                val start = parts[0].toInt()
                val end = if (parts.size > 1) parts[1].toInt() else null
                start to end
            }
        }
        
        return BibleRef(
            book = book,
            chapter = chapter,
            verses = verseRanges,
            originalText = match.value
        )
    }
    return null
}

fun handleAutoList(oldValue: TextFieldValue, newValue: TextFieldValue): TextFieldValue {
    if (newValue.text.length != oldValue.text.length + 1) return newValue
    if (newValue.text[newValue.selection.start - 1] != '\n') return newValue

    val textBeforeNewline = newValue.text.take(newValue.selection.start - 1)
    val lastLineStart = textBeforeNewline.lastIndexOf('\n') + 1
    val lastLine = textBeforeNewline.substring(lastLineStart)

    val orderedListRegex = Regex("""^(\d+)\.\s+(.*)$""")
    val orderedMatch = orderedListRegex.find(lastLine)
    if (orderedMatch != null) {
        val number = orderedMatch.groupValues[1].toInt()
        val content = orderedMatch.groupValues[2]
        
        if (content.isEmpty()) {
            val newText = newValue.text.take(lastLineStart) + newValue.text.substring(newValue.selection.start)
            return newValue.copy(text = newText, selection = TextRange(lastLineStart))
        }
        
        val prefix = "${number + 1}. "
        val newText = newValue.text.take(newValue.selection.start) + prefix + newValue.text.substring(newValue.selection.start)
        return newValue.copy(text = newText, selection = TextRange(newValue.selection.start + prefix.length))
    }

    val bulletListRegex = Regex("""^([-*])\s+(.*)$""")
    val bulletMatch = bulletListRegex.find(lastLine)
    if (bulletMatch != null) {
        val bullet = bulletMatch.groupValues[1]
        val content = bulletMatch.groupValues[2]
        
        if (content.isEmpty()) {
            val newText = newValue.text.take(lastLineStart) + newValue.text.substring(newValue.selection.start)
            return newValue.copy(text = newText, selection = TextRange(lastLineStart))
        }
        
        val prefix = "$bullet "
        val newText = newValue.text.take(newValue.selection.start) + prefix + newValue.text.substring(newValue.selection.start)
        return newValue.copy(text = newText, selection = TextRange(newValue.selection.start + prefix.length))
    }
    
    val checklistRegex = Regex("""^(-\s\[\s]\s)(.*)$""")
    val checklistMatch = checklistRegex.find(lastLine)
    if (checklistMatch != null) {
        val prefix = checklistMatch.groupValues[1]
        val content = checklistMatch.groupValues[2]
        
        if (content.isEmpty()) {
            val newText = newValue.text.take(lastLineStart) + newValue.text.substring(newValue.selection.start)
            return newValue.copy(text = newText, selection = TextRange(lastLineStart))
        }
        
        val newText = newValue.text.take(newValue.selection.start) + prefix + newValue.text.substring(newValue.selection.start)
        return newValue.copy(text = newText, selection = TextRange(newValue.selection.start + prefix.length))
    }

    return newValue
}

fun applyFormat(value: TextFieldValue, symbol: String): TextFieldValue {
    val selection = value.selection
    val text = value.text
    
    val formatted = if (selection.collapsed) {
        text.take(selection.start) + symbol + symbol + text.substring(selection.end)
    } else {
        text.take(selection.start) + symbol + text.substring(selection.start, selection.end) + symbol + text.substring(selection.end)
    }
    
    val newCursorPos = if (selection.collapsed) selection.start + symbol.length else selection.end + symbol.length * 2
    return value.copy(text = formatted, selection = TextRange(newCursorPos))
}

@Composable
fun AddCategoryDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Category") },
        text = {
            TextField(
                value = name, 
                onValueChange = { name = it }, 
                label = { Text("Category Name") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name) }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
