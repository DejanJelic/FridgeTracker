package com.example.fridgetracker.view.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fridgetracker.model.ShoppingListItemEntity
import com.example.fridgetracker.view_model.ShoppingListViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ShoppingListScreen(
    navController: NavController,
    vm: ShoppingListViewModel,
    onMenuClick: () -> Unit
) {
    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()

    // Primary data
    val shoppingItems = remember { mutableStateListOf<ShoppingListItemEntity>() }
    val savedLists by vm.listsState.collectAsState()

    // UI state
    var searchQuery by remember { mutableStateOf("") }
    var showSuggestions by remember { mutableStateOf(false) }
    var selectedListName by remember { mutableStateOf<String?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    // Dialogs
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveNameText by remember { mutableStateOf("") }
    var showCreateListDialog by remember { mutableStateOf(false) }
    var createNameText by remember { mutableStateOf("") }

    // Drag state
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }

    val itemHeightDp = 85.dp
    val density = LocalDensity.current
    val itemHeightPx = with(density) { itemHeightDp.toPx() }

    val displayList = remember { mutableStateListOf<ShoppingListItemEntity>() }

    LaunchedEffect(shoppingItems.size, shoppingItems.toList()) {
        if (draggedIndex == null) {
            displayList.clear()
            displayList.addAll(shoppingItems)
        }
    }

    // Suggestions
    val allSuggestions = remember {
        listOf(
            "Apple", "Apricot", "Banana", "Blackberry", "Blueberry",
            "Cherry", "Clementine", "Coconut", "Cranberry", "Date",
            "Dragon fruit", "Ham", "Carrot", "Milk", "Bread",
            "Eggs", "Cheese", "Yogurt", "Butter", "Chicken",
            "Tomato", "Potato", "Onion", "Garlic", "Pasta"
        )
    }

    val filteredSuggestions = remember(searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else allSuggestions.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    fun addItemNow(name: String) {
        if (name.isBlank()) return
        val tempId = -System.currentTimeMillis()
        val item = ShoppingListItemEntity(
            name = name.trim(),
            id = tempId,
            listId = 0L,
            quantity = 1,
            checked = false,
            orderIndex = 0
        )
        shoppingItems.add(0, item)
        displayList.add(0, item)
    }

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar(
                backgroundColor = Color(0xFF6A1B9A),
                contentColor = Color.White,
                elevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { coroutineScope.launch { scaffoldState.drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                    // List selector
                    Box(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { dropdownExpanded = true }
                                .padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = selectedListName ?: "Shopping list",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                maxLines = 1
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Select list"
                            )
                        }

                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            if (savedLists.isEmpty()) {
                                DropdownMenuItem(onClick = { dropdownExpanded = false }) {
                                    Text("No saved lists", color = Color.Gray)
                                }
                            } else {
                                savedLists.forEach { listWith  ->
                                    DropdownMenuItem(
                                        onClick = {
                                            shoppingItems.clear()
                                            shoppingItems.addAll(listWith.items)
                                            displayList.clear()
                                            displayList.addAll(listWith.items)
                                            selectedListName = listWith.list.name
                                            dropdownExpanded = false
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(listWith.list.name)
                                            if (selectedListName == listWith.list.name) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color(0xFF6A1B9A)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Divider()
                            DropdownMenuItem(
                                onClick = {
                                    dropdownExpanded = false
                                    showCreateListDialog = true
                                    createNameText = ""
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Create new list")
                            }
                        }
                    }
                    IconButton(
                        onClick = {
                            if (selectedListName != null) {
                                savedLists.forEach { listWith ->
                                    if(listWith.list.name == selectedListName!!)
                                        listWith.items = shoppingItems.toList()
                                }
                                coroutineScope.launch {
                                    scaffoldState.snackbarHostState.showSnackbar(
                                        "List '${selectedListName}' updated"
                                    )
                                }
                            } else {
                                showSaveDialog = true
                                saveNameText = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save list")
                    }
                }
            }
        },
        drawerContent = {
            AppDrawer(
                navController = navController,
                currentRoute = "shopping",
                closeDrawer = {
                    coroutineScope.launch {
                        scaffoldState.drawerState.close()
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (searchQuery.isNotBlank()) {
                        addItemNow(searchQuery)
                        searchQuery = ""
                        showSuggestions = false
                    } else {
                        showCreateListDialog = true
                        createNameText = ""
                    }
                },
                backgroundColor = Color(0xFFFFC107)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add",
                    tint = Color.Black
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Search bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = 4.dp,
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(Modifier.width(12.dp))

                    BasicTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            showSuggestions = it.isNotBlank()
                        },
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            color = Color.Black
                        ),
                        decorationBox = { inner ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    "What to buy?",
                                    color = Color.Gray,
                                    fontSize = 16.sp
                                )
                            }
                            inner()
                        }
                    )

                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                searchQuery = ""
                                showSuggestions = false
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = Color.Gray
                            )
                        }
                    }
                }
            }

            // Suggestions dropdown
            if (showSuggestions && filteredSuggestions.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .heightIn(max = 250.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = 6.dp
                ) {
                    LazyColumn {
                        items(filteredSuggestions) { suggestion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        addItemNow(suggestion)
                                        searchQuery = ""
                                        showSuggestions = false
                                    }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color(0xFF6A1B9A),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(suggestion, fontSize = 16.sp)
                            }
                            if (suggestion != filteredSuggestions.last()) {
                                Divider()
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Shopping items list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(
                    items = displayList,
                    key = { _, item -> item.id }
                ) { index, item ->
                    val isDragging = draggedIndex == index
                    val scale by animateFloatAsState(if (isDragging) 1.05f else 1f)
                    val elevation by animateDpAsState(if (isDragging) 12.dp else 3.dp)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(scale)
                            .shadow(elevation, RoundedCornerShape(16.dp))
                            .pointerInput(item.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggedIndex = index
                                        accumulatedDrag = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consumePositionChange()
                                        accumulatedDrag += dragAmount.y

                                        val cur = draggedIndex
                                        if (cur != null) {
                                            val steps = (accumulatedDrag / itemHeightPx).roundToInt()
                                            if (steps != 0) {
                                                val newIndex = (cur + steps).coerceIn(0, displayList.size - 1)
                                                if (newIndex != cur) {
                                                    val moved = displayList.removeAt(cur)
                                                    displayList.add(newIndex, moved)
                                                    draggedIndex = newIndex
                                                    accumulatedDrag -= steps * itemHeightPx
                                                }
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        draggedIndex = null
                                        accumulatedDrag = 0f
                                        if (!shoppingItems.contentEquals(displayList)) {
                                            shoppingItems.clear()
                                            shoppingItems.addAll(displayList)
                                        }
                                    },
                                    onDragCancel = {
                                        draggedIndex = null
                                        accumulatedDrag = 0f
                                        displayList.clear()
                                        displayList.addAll(shoppingItems)
                                    }
                                )
                            },
                        shape = RoundedCornerShape(16.dp),
                        elevation = elevation,
                        backgroundColor = if (item.checked) Color(0xFFF5F5F5) else Color.White
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Checkbox
                            Checkbox(
                                checked = item.checked,
                                onCheckedChange = { checked ->
                                    val pos = displayList.indexOfFirst { it.id == item.id }
                                    if (pos >= 0) displayList[pos] = displayList[pos].copy(checked = checked)
                                    val idx = shoppingItems.indexOfFirst { it.id == item.id }
                                    if (idx >= 0) shoppingItems[idx] = shoppingItems[idx].copy(checked = checked)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF6A1B9A),
                                    uncheckedColor = Color.Gray
                                )
                            )

                            Spacer(Modifier.width(12.dp))

                            // Item name
                            Text(
                                text = item.name,
                                modifier = Modifier.weight(1f),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                                color = if (item.checked) Color.Gray else Color.Black
                            )

                            // Quantity controls
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFFF5F5F5),
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            val pos = displayList.indexOfFirst { it.id == item.id }
                                            if (pos >= 0 && displayList[pos].quantity > 1) {
                                                displayList[pos] = displayList[pos].copy(quantity = displayList[pos].quantity - 1)
                                            }
                                            val idx = shoppingItems.indexOfFirst { it.id == item.id }
                                            if (idx >= 0 && shoppingItems[idx].quantity > 1) {
                                                shoppingItems[idx] = shoppingItems[idx].copy(quantity = shoppingItems[idx].quantity - 1)
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Remove,
                                            contentDescription = "Decrease",
                                            tint = Color(0xFF6A1B9A),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Text(
                                        text = item.quantity.toString(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        modifier = Modifier.widthIn(min = 24.dp),
                                        textAlign = TextAlign.Center
                                    )

                                    IconButton(
                                        onClick = {
                                            val pos = displayList.indexOfFirst { it.id == item.id }
                                            if (pos >= 0) {
                                                displayList[pos] = displayList[pos].copy(quantity = displayList[pos].quantity + 1)
                                            }
                                            val idx = shoppingItems.indexOfFirst { it.id == item.id }
                                            if (idx >= 0) {
                                                shoppingItems[idx] = shoppingItems[idx].copy(quantity = shoppingItems[idx].quantity + 1)
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Increase",
                                            tint = Color(0xFF6A1B9A),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            // Delete button - ADDED
                            IconButton(
                                onClick = {
                                    displayList.removeAll { it.id == item.id }
                                    shoppingItems.removeAll { it.id == item.id }
                                    coroutineScope.launch {
                                        scaffoldState.snackbarHostState.showSnackbar(
                                            "${item.name} removed",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color(0xFFEF5350),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Drag handle
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Drag",
                                tint = Color.Gray,
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(start = 4.dp)
                            )
                        }
                    }
                }

                // Empty state
                if (displayList.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 64.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Your shopping list is empty",
                                fontSize = 18.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Add items to get started",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }

    // Save Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            backgroundColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = null,
                        tint = Color(0xFF6A1B9A),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Save Shopping List",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            },
            text = {
                Column {
                    Text(
                        "Enter a name for this list:",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = saveNameText,
                        onValueChange = { saveNameText = it },
                        label = { Text("List name") },
                        placeholder = { Text("e.g., Weekly Shopping") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFF6A1B9A),
                            cursorColor = Color(0xFF6A1B9A)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = saveNameText.ifBlank { "List ${savedLists.size + 1}" }
                        savedLists.forEach { listWith ->
                            if(listWith.list.name == name)
                                listWith.items = shoppingItems.toList()
                        }
                        selectedListName = name
                        val itemsForDb = displayList.mapIndexed { idx, it ->
                            it.copy(
                                id = 0L,          // -> Room će auto-generisati
                                listId = 0L,
                                orderIndex = idx
                            )
                        }
                        vm.saveList(name, itemsForDb.map { uiItem ->
                            uiItem
                        }, existingListId = null /* ili current saved id ako update */)
                        showSaveDialog = false
                        coroutineScope.launch {
                            scaffoldState.snackbarHostState.showSnackbar("Saved as '$name'")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF6A1B9A)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("SAVE", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("CANCEL", color = Color.Gray)
                }
            }
        )
    }

    // Create List Dialog
    if (showCreateListDialog) {
        AlertDialog(
            onDismissRequest = { showCreateListDialog = false },
            backgroundColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = Color(0xFFFFA726),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Create New List",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            },
            text = {
                Column {
                    Text(
                        "Enter a name for the new shopping list:",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = createNameText,
                        onValueChange = { createNameText = it },
                        label = { Text("List name") },
                        placeholder = { Text("e.g., Weekly Shopping") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFFFFA726),
                            cursorColor = Color(0xFFFFA726)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = createNameText.ifBlank { "New List ${savedLists.size + 1}" }
                        savedLists.forEach { listWith ->
                            if(listWith.list.name == name)
                                listWith.items = emptyList()
                        }
                        shoppingItems.clear()
                        displayList.clear()
                        selectedListName = name
                        val itemsForDb = displayList.mapIndexed { idx, it ->
                            it.copy(
                                id = 0L,          // -> Room će auto-generisati
                                listId = 0L,
                                orderIndex = idx
                            )
                        }
                        vm.saveList(name, itemsForDb.map { uiItem ->
                            uiItem
                        }, existingListId = null /* ili current saved id ako update */)
                        showCreateListDialog = false
                        coroutineScope.launch {
                            scaffoldState.snackbarHostState.showSnackbar("Created '$name'")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFFFFA726)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("CREATE", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateListDialog = false }) {
                    Text("CANCEL", color = Color.Gray)
                }
            }
        )
    }
}

// Helper extension
private fun <T> MutableList<T>.contentEquals(other: List<T>): Boolean {
    if (this.size != other.size) return false
    for (i in indices) {
        if (this[i] != other[i]) return false
    }
    return true
}