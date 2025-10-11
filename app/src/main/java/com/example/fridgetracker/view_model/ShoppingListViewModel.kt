package com.example.fridgetracker.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fridgetracker.model.ShoppingListItemEntity
import com.example.fridgetracker.model.ShoppingListWithItems
import com.example.fridgetracker.repository.ShoppingListRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShoppingListViewModel(private val repo: ShoppingListRepository) : ViewModel() {

    val listsState: StateFlow<List<ShoppingListWithItems>> =
        repo.observeAllLists()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun saveList(name: String, uiItems: List<ShoppingListItemEntity>, existingListId: Long? = null) {
        // map UI items -> entity (listId will be fixed in repo)
        val entities = uiItems.mapIndexed { idx, ui ->
            ShoppingListItemEntity(
                id = 0L,
                listId = existingListId ?: 0L,
                name = ui.name,
                quantity = ui.quantity,
                checked = ui.checked,
                orderIndex = idx
            )
        }
        viewModelScope.launch {
            repo.saveList(name, entities, existingListId)
        }
    }

    fun deleteList(listId: Long) = viewModelScope.launch { repo.deleteList(listId) }
}
