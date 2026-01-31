package com.example.fridgetracker.view.screens.shopping_list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fridgetracker.model.ShoppingListEntity
import com.example.fridgetracker.model.ShoppingListItemEntity
import com.example.fridgetracker.model.ShoppingListWithItems
import com.example.fridgetracker.utilities.CustomSnackbar
import com.example.fridgetracker.utilities.SnackbarType
import com.example.fridgetracker.view_model.ShoppingListViewModel
import com.example.fridgetracker.ui.theme.AccentYellow
import com.example.fridgetracker.ui.theme.ErrorRed
import com.example.fridgetracker.ui.theme.PrimaryPurple
import kotlinx.coroutines.CoroutineScope

// Top Bar
@Composable
fun ShoppingListTopBar(
    state: ShoppingListState,
    onMenuClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 4.dp,
        color = PrimaryPurple,
        contentColor = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .height(56.dp), // Typical TopAppBar height
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, contentDescription = "Menu")
            }

            // List selector
            ListSelectorDropdown(state = state)

            IconButton(onClick = onSaveClick) {
                Icon(Icons.Default.Check, contentDescription = "Save list")
            }
        }
    }
}

// List Selector Dropdown
@Composable
fun RowScope.ListSelectorDropdown(state: ShoppingListState) {
    Box(modifier = Modifier.weight(1f)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { state.dropdownExpanded = true }
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = state.selectedListName ?: "Shopping list",
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
            expanded = state.dropdownExpanded,
            onDismissRequest = { state.dropdownExpanded = false }
        ) {
            if (state.savedLists.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No saved lists", color = Color.Gray) },
                    onClick = { state.dropdownExpanded = false }
                )
            } else {
                state.savedLists.forEach { listWith ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    listWith.list.name,
                                    modifier = Modifier.weight(1f)
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (state.selectedListName == listWith.list.name) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = PrimaryPurple,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                    }

                                    IconButton(
                                        onClick = {
                                            state.listToDeleteId = listWith.list.id
                                            state.listToDeleteName = listWith.list.name
                                            state.showDeleteDialog = true
                                            state.dropdownExpanded = false
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete list",
                                            tint = ErrorRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        },
                        onClick = {
                            state.loadList(listWith)
                            state.dropdownExpanded = false
                        }
                    )
                }
            }

            HorizontalDivider()

            DropdownMenuItem(
                text = { Text("Create new list") },
                onClick = {
                    state.dropdownExpanded = false
                    state.showCreateListDialog = true
                    state.createNameText = ""
                },
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
            )
        }
    }
}

// Search Bar
@Composable
fun SearchBar(state: ShoppingListState) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 4.dp,
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
                value = state.searchQuery,
                onValueChange = {
                    state.searchQuery = it
                    state.showSuggestions = it.isNotBlank()
                },
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    color = Color.Black
                ),
                decorationBox = { inner ->
                    if (state.searchQuery.isEmpty()) {
                        Text(
                            "What to buy?",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                    }
                    inner()
                }
            )

            if (state.searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = {
                        state.searchQuery = ""
                        state.showSuggestions = false
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
}

// Suggestions Card
@Composable
fun SuggestionsCard(state: ShoppingListState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .heightIn(max = 250.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        LazyColumn {
            items(state.filteredSuggestions) { suggestion ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            state.addItem(suggestion)
                            state.searchQuery = ""
                            state.showSuggestions = false
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(suggestion, fontSize = 16.sp)
                }
                if (suggestion != state.filteredSuggestions.last()) {
                    HorizontalDivider()
                }
            }
        }
    }
}

// Empty State
@Composable
fun EmptyShoppingListState() {
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

// Custom Snackbar Overlay
@Composable
fun CustomSnackbarOverlay(
    message: String,
    type: SnackbarType,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        CustomSnackbar(
            message = message,
            type = type,
            onDismiss = onDismiss
        )
    }
}

// Mock ViewModel za preview
class MockShoppingListViewModel : ShoppingListViewModel(
    repo = object : com.example.fridgetracker.repository.ShoppingListRepository(
        dao = object : com.example.fridgetracker.data.ShoppingListDao {
            override fun getAllListsWithItemsFlow(): kotlinx.coroutines.flow.Flow<List<ShoppingListWithItems>> {
                return kotlinx.coroutines.flow.flowOf(emptyList())
            }
            override suspend fun getListWithItemsById(id: Long): ShoppingListWithItems? = null
            override suspend fun insertList(list: ShoppingListEntity): Long = 1L
            override suspend fun insertItems(items: List<ShoppingListItemEntity>): List<Long> = emptyList()
            override suspend fun deleteItemsForList(listId: Long): Int = 0
            override suspend fun deleteList(list: ShoppingListEntity): Int = 0
            override suspend fun deleteItemById(itemId: Long): Int = 0
            override suspend fun updateItem(item: ShoppingListItemEntity): Int = 0
            override suspend fun updateList(list: ShoppingListEntity): Int = 0
        }
    ) {}
)

// Default suggestions for previews
private val defaultSuggestions = listOf(
    "Apple", "Banana", "Bread", "Butter", "Cheese", "Eggs", "Milk", "Yogurt"
)

// Dummy ShoppingListState za preview
fun createMockShoppingListState(coroutineScope: CoroutineScope): ShoppingListState {
    return ShoppingListState(
        vm = MockShoppingListViewModel(),
        coroutineScope = coroutineScope,
        allSuggestions = defaultSuggestions
    ).apply {
        selectedListName = "Weekly Shopping"

        // Dodaj test stavke
        shoppingItems.addAll(
            listOf(
                ShoppingListItemEntity(1L, 1L, "Milk", 2, false, 0),
                ShoppingListItemEntity(2L, 1L, "Bread", 1, true, 1),
                ShoppingListItemEntity(3L, 1L, "Eggs", 12, false, 2)
            )
        )
        displayList.addAll(shoppingItems)

        // Dodaj sačuvane liste
        savedLists = listOf(
            ShoppingListWithItems(
                ShoppingListEntity(1L, "Weekly Shopping", System.currentTimeMillis()),
                shoppingItems.toList()
            ),
            ShoppingListWithItems(
                ShoppingListEntity(2L, "Party Supplies", System.currentTimeMillis()),
                emptyList()
            )
        )
    }
}

// Preview za Top Bar
@Preview(showBackground = true, widthDp = 360, heightDp = 56)
@Composable
fun ShoppingListTopBarPreview() {
    val coroutineScope = rememberCoroutineScope()
    val mockState = createMockShoppingListState(coroutineScope)
    ShoppingListTopBar(
        state = mockState,
        onMenuClick = {},
        onSaveClick = {}
    )
}

// Preview za Search Bar
@Preview(showBackground = true, widthDp = 360, heightDp = 120)
@Composable
fun SearchBarPreview() {
    val coroutineScope = rememberCoroutineScope()
    val mockState = createMockShoppingListState(coroutineScope)
    Surface(color = Color.White) {
        SearchBar(state = mockState)
    }
}

// Preview za Suggestions Card
@Preview(showBackground = true, widthDp = 360, heightDp = 250)
@Composable
fun SuggestionsCardPreview() {
    val coroutineScope = rememberCoroutineScope()
    val mockState = createMockShoppingListState(coroutineScope).apply {
        searchQuery = "Mil"
    }
    Surface(color = Color.White) {
        SuggestionsCard(state = mockState)
    }
}

// Preview za Empty State
@Preview(showBackground = true, widthDp = 360, heightDp = 500)
@Composable
fun EmptyShoppingListStatePreview() {
    Surface(color = Color.White) {
        EmptyShoppingListState()
    }
}

// Preview za Shopping Item Card
@Preview(showBackground = true, widthDp = 360, heightDp = 100)
@Composable
fun ShoppingItemCardPreview() {
    val coroutineScope = rememberCoroutineScope()
    val mockState = createMockShoppingListState(coroutineScope)
    val item = ShoppingListItemEntity(1L, 1L, "Milk", 2, false, 0)

    Surface(color = Color.White) {
        ShoppingItemCard(
            item = item,
            index = 0,
            state = mockState,
            itemHeightPx = 85f,
            onDelete = {}
        )
    }
}

// Preview za Quantity Controls
@Preview(showBackground = true, widthDp = 150, heightDp = 50)
@Composable
fun QuantityControlsPreview() {
    Surface(color = Color.White) {
        QuantityControls(
            quantity = 2,
            onDecrease = {},
            onIncrease = {}
        )
    }
}

// Preview za Success Snackbar
@Preview(showBackground = true, widthDp = 360, heightDp = 200)
@Composable
fun CustomSnackbarSuccessPreview() {
    Surface(color = Color.White) {
        CustomSnackbarOverlay(
            message = "List 'Weekly Shopping' updated successfully!",
            type = SnackbarType.SUCCESS,
            onDismiss = {}
        )
    }
}

// Preview za Error Snackbar
@Preview(showBackground = true, widthDp = 360, heightDp = 200)
@Composable
fun CustomSnackbarErrorPreview() {
    Surface(color = Color.White) {
        CustomSnackbarOverlay(
            message = "Failed to save list: Database error",
            type = SnackbarType.ERROR,
            onDismiss = {}
        )
    }
}

// Preview za Info Snackbar
@Preview(showBackground = true, widthDp = 360, heightDp = 200)
@Composable
fun CustomSnackbarInfoPreview() {
    Surface(color = Color.White) {
        CustomSnackbarOverlay(
            message = "Saving...",
            type = SnackbarType.INFO,
            onDismiss = {}
        )
    }
}

// Preview za Save Dialog
@Preview(showBackground = true, widthDp = 360, heightDp = 500)
@Composable
fun SaveListDialogPreview() {
    val coroutineScope = rememberCoroutineScope()
    val mockState = createMockShoppingListState(coroutineScope)
    SaveListDialog(
        state = mockState,
        onSave = {},
        onDismiss = {}
    )
}

// Preview za Create Dialog
@Preview(showBackground = true, widthDp = 360, heightDp = 500)
@Composable
fun CreateListDialogPreview() {
    val coroutineScope = rememberCoroutineScope()
    val mockState = createMockShoppingListState(coroutineScope)
    CreateListDialog(
        state = mockState,
        onCreate = {},
        onDismiss = {}
    )
}

// Preview za Delete Dialog
@Preview(showBackground = true, widthDp = 360, heightDp = 400)
@Composable
fun DeleteListDialogPreview() {
    DeleteListDialog(
        listName = "Weekly Shopping",
        onConfirm = {},
        onDismiss = {}
    )
}
