package com.example.fridgetracker.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fridgetracker.model.ShoppingListItemEntity
import com.example.fridgetracker.model.ShoppingListWithItems
import com.example.fridgetracker.repository.ShoppingListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
open class ShoppingListViewModel @Inject constructor(private val repo: ShoppingListRepository) : ViewModel() {

    val listsState: StateFlow<List<ShoppingListWithItems>> =
        repo.observeAllLists()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Error/Success state
    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    private val _deleteState = MutableStateFlow<DeleteState>(DeleteState.Idle)
    val deleteState: StateFlow<DeleteState> = _deleteState.asStateFlow()

   fun saveList(name: String, uiItems: List<ShoppingListItemEntity>, existingListId: Long? = null) {
        if (name.isBlank()) {
            _saveState.value = SaveState.Error("List name cannot be empty")
            return
        }

        _saveState.value = SaveState.Loading

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
            try {
                val savedId = repo.saveList(name, entities, existingListId)
                _saveState.value = SaveState.Success(
                    message = if (existingListId != null)
                        "List '$name' updated successfully!"
                    else
                        "List '$name' created successfully!",
                    listId = savedId
                )
            } catch (e: Exception) {
                _saveState.value = SaveState.Error("Failed to save list: ${e.message}")
            }
        }
    }

    fun deleteList(listId: Long) {
        _deleteState.value = DeleteState.Loading

        viewModelScope.launch {
            try {
                repo.deleteList(listId)
                _deleteState.value = DeleteState.Success("List deleted successfully!")
            } catch (e: Exception) {
                _deleteState.value = DeleteState.Error("Failed to delete list: ${e.message}")
            }
        }
    }

    fun resetSaveState() {
        _saveState.value = SaveState.Idle
    }

    fun resetDeleteState() {
        _deleteState.value = DeleteState.Idle
    }
}

// State sealing classes
sealed class SaveState {
    object Idle : SaveState()
    object Loading : SaveState()
    data class Success(val message: String, val listId: Long) : SaveState()
    data class Error(val message: String) : SaveState()
}

sealed class DeleteState {
    object Idle : DeleteState()
    object Loading : DeleteState()
    data class Success(val message: String) : DeleteState()
    data class Error(val message: String) : DeleteState()
}