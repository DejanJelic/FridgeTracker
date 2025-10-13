package com.example.fridgetracker.view.screens.shopping_list

import androidx.compose.runtime.*
import com.example.fridgetracker.model.ShoppingListItemEntity
import com.example.fridgetracker.model.ShoppingListWithItems
import com.example.fridgetracker.utilities.SnackbarType
import com.example.fridgetracker.view_model.ShoppingListViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Stable
class ShoppingListState(
    private val vm: ShoppingListViewModel,
    private val coroutineScope: CoroutineScope
) {
    // Data
    val shoppingItems = mutableStateListOf<ShoppingListItemEntity>()
    val displayList = mutableStateListOf<ShoppingListItemEntity>()
    var savedLists by mutableStateOf<List<ShoppingListWithItems>>(emptyList())

    // Search & Suggestions
    var searchQuery by mutableStateOf("")
    var showSuggestions by mutableStateOf(false)

    val allSuggestions = listOf(
        "Apple", "Apricot", "Banana", "Blackberry", "Blueberry",
        "Cherry", "Clementine", "Coconut", "Cranberry", "Date",
        "Dragon fruit", "Ham", "Carrot", "Milk", "Bread",
        "Eggs", "Cheese", "Yogurt", "Butter", "Chicken",
        "Tomato", "Potato", "Onion", "Garlic", "Pasta"
    )

    val filteredSuggestions: List<String>
        get() = if (searchQuery.isBlank()) emptyList()
        else allSuggestions.filter { it.contains(searchQuery, ignoreCase = true) }

    // List selection
    var selectedListName by mutableStateOf<String?>(null)
    var dropdownExpanded by mutableStateOf(false)

    // Dialogs
    var showSaveDialog by mutableStateOf(false)
    var saveNameText by mutableStateOf("")
    var showCreateListDialog by mutableStateOf(false)
    var createNameText by mutableStateOf("")
    var showDeleteDialog by mutableStateOf(false)
    var listToDeleteId by mutableStateOf<Long?>(null)
    var listToDeleteName by mutableStateOf<String?>(null)

    // Drag state
    var draggedIndex by mutableStateOf<Int?>(null)
    var accumulatedDrag by mutableFloatStateOf(0f)

    // Snackbar
    var snackbarVisible by mutableStateOf(false)
    var snackbarMessage by mutableStateOf("")
    var snackbarType by mutableStateOf(SnackbarType.SUCCESS)

    // Add item
    fun addItem(name: String) {
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

        // Auto-save ako je lista već selektovana
        if (selectedListName != null) {
            autoSaveCurrentList()
        }
    }

    // Toggle checkbox
    fun toggleItemChecked(itemId: Long, checked: Boolean) {
        val pos = displayList.indexOfFirst { it.id == itemId }
        if (pos >= 0) displayList[pos] = displayList[pos].copy(checked = checked)

        val idx = shoppingItems.indexOfFirst { it.id == itemId }
        if (idx >= 0) shoppingItems[idx] = shoppingItems[idx].copy(checked = checked)

        // Auto-save
        if (selectedListName != null) {
            autoSaveCurrentList()
        }
    }

    // Update quantity
    fun updateItemQuantity(itemId: Long, delta: Int) {
        val pos = displayList.indexOfFirst { it.id == itemId }
        if (pos >= 0) {
            val current = displayList[pos].quantity
            val newQty = (current + delta).coerceAtLeast(1)
            displayList[pos] = displayList[pos].copy(quantity = newQty)
        }

        val idx = shoppingItems.indexOfFirst { it.id == itemId }
        if (idx >= 0) {
            val current = shoppingItems[idx].quantity
            val newQty = (current + delta).coerceAtLeast(1)
            shoppingItems[idx] = shoppingItems[idx].copy(quantity = newQty)
        }

        // Auto-save
        if (selectedListName != null) {
            autoSaveCurrentList()
        }
    }

    // Delete item
    fun deleteItem(itemId: Long): String {
        val item = displayList.firstOrNull { it.id == itemId }
        displayList.removeAll { it.id == itemId }
        shoppingItems.removeAll { it.id == itemId }

        // Auto-save
        if (selectedListName != null) {
            autoSaveCurrentList()
        }

        return item?.name ?: "Item"
    }

    // Auto-save helper (bez snackbar poruke)
    private fun autoSaveCurrentList() {
        selectedListName?.let { listName ->
            val currentList = savedLists.firstOrNull { it.list.name == listName }
            if (currentList != null) {
                val itemsForDb = shoppingItems.mapIndexed { idx, it ->
                    ShoppingListItemEntity(
                        id = 0L,
                        listId = currentList.list.id,
                        name = it.name,
                        quantity = it.quantity,
                        checked = it.checked,
                        orderIndex = idx
                    )
                }
                vm.saveList(listName, itemsForDb, currentList.list.id)
            }
        }
    }

    // Load list
    fun loadList(listWith: ShoppingListWithItems) {
        shoppingItems.clear()
        shoppingItems.addAll(listWith.items)
        displayList.clear()
        displayList.addAll(listWith.items)
        selectedListName = listWith.list.name
    }

    // Save list
    fun saveList(name: String, existingListId: Long? = null) {
        val finalName = name.ifBlank { "List ${savedLists.size + 1}" }

        // Pronađi postojeću listu ako update-ujemo
        val currentListId = existingListId ?: savedLists.firstOrNull { it.list.name == finalName }?.list?.id

        val itemsForDb = shoppingItems.mapIndexed { idx, it ->
            ShoppingListItemEntity(
                id = 0L,  // Room će generisati novi ID
                listId = 0L,  // Biće setovan u repository
                name = it.name,
                quantity = it.quantity,
                checked = it.checked,
                orderIndex = idx
            )
        }

        vm.saveList(finalName, itemsForDb, currentListId)
        selectedListName = finalName
        showSnackbar("Saved as '$finalName'", SnackbarType.SUCCESS)
    }

    // Create list
    fun createList(name: String) {
        val finalName = name.ifBlank { "New List ${savedLists.size + 1}" }

        shoppingItems.clear()
        displayList.clear()
        selectedListName = finalName

        // Kreiraj praznu listu odmah u bazi
        val emptyItems = emptyList<ShoppingListItemEntity>()
        vm.saveList(finalName, emptyItems, null)

        showSnackbar("Created '$finalName'", SnackbarType.SUCCESS)
    }

    // Update current list - NOVO
    fun updateCurrentList() {
        selectedListName?.let { listName ->
            val currentList = savedLists.firstOrNull { it.list.name == listName }
            if (currentList != null) {
                val itemsForDb = shoppingItems.mapIndexed { idx, it ->
                    ShoppingListItemEntity(
                        id = 0L,
                        listId = currentList.list.id,
                        name = it.name,
                        quantity = it.quantity,
                        checked = it.checked,
                        orderIndex = idx
                    )
                }
                vm.saveList(listName, itemsForDb, currentList.list.id)
            }
        }
    }

    // Delete list
    fun deleteList(listId: Long, listName: String) {
        vm.deleteList(listId)

        if (selectedListName == listName) {
            selectedListName = null
            shoppingItems.clear()
            displayList.clear()
        }

        showSnackbar("Deleted '$listName'", SnackbarType.SUCCESS)
    }

    // Snackbar helper
    fun showSnackbar(message: String, type: SnackbarType) {
        snackbarMessage = message
        snackbarType = type
        snackbarVisible = true

        coroutineScope.launch {
            delay(3000)
            snackbarVisible = false
        }
    }

    // Sync display list
    fun syncDisplayList() {
        if (draggedIndex == null) {
            displayList.clear()
            displayList.addAll(shoppingItems)
        }
    }

    // End drag
    fun endDrag() {
        draggedIndex = null
        accumulatedDrag = 0f
        if (!shoppingItems.contentEquals(displayList)) {
            shoppingItems.clear()
            shoppingItems.addAll(displayList)

            // Auto-save nakon reorder-a
            if (selectedListName != null) {
                autoSaveCurrentList()
            }
        }
    }

    // Cancel drag
    fun cancelDrag() {
        draggedIndex = null
        accumulatedDrag = 0f
        displayList.clear()
        displayList.addAll(shoppingItems)
    }
}

@Composable
fun rememberShoppingListState(vm: ShoppingListViewModel): ShoppingListState {
    val coroutineScope = rememberCoroutineScope()
    val state = remember { ShoppingListState(vm, coroutineScope) }

    // Observe saved lists
    val savedLists by vm.listsState.collectAsState()
    LaunchedEffect(savedLists) {
        state.savedLists = savedLists
    }

    // Sync display list
    LaunchedEffect(state.shoppingItems.size, state.shoppingItems.toList()) {
        state.syncDisplayList()
    }

    return state
}

// Helper extension
private fun <T> MutableList<T>.contentEquals(other: List<T>): Boolean {
    if (this.size != other.size) return false
    for (i in indices) {
        if (this[i] != other[i]) return false
    }
    return true
}