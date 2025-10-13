package com.example.fridgetracker.repository

import com.example.fridgetracker.data.ShoppingListDao
import com.example.fridgetracker.model.ShoppingListEntity
import com.example.fridgetracker.model.ShoppingListItemEntity
import com.example.fridgetracker.model.ShoppingListWithItems
import kotlinx.coroutines.flow.Flow

class ShoppingListRepository(private val dao: ShoppingListDao) {

    fun observeAllLists(): Flow<List<ShoppingListWithItems>> = dao.getAllListsWithItemsFlow()

    suspend fun getList(id: Long): ShoppingListWithItems? = dao.getListWithItemsById(id)

    suspend fun saveList(name: String, items: List<ShoppingListItemEntity>, oldListId: Long? = null): Long {
        // if updating existing list
        val listId = if (oldListId != null && oldListId > 0L) {
            // update list (replace)
            val listEntity = ShoppingListEntity(id = oldListId, name = name)
            dao.updateList(listEntity)
            // replace items: delete existing and insert new
            dao.deleteItemsForList(oldListId)
            // fix items' listId
            val itemsWithListId = items.map { it.copy(listId = oldListId) }
            dao.insertItems(itemsWithListId)
            oldListId
        } else {
            val listEntity = ShoppingListEntity(name = name)
            val newId = dao.insertList(listEntity)
            val itemsWithListId = items.map { it.copy(listId = newId) }
            dao.insertItems(itemsWithListId)
            newId
        }
        return listId
    }

    suspend fun deleteList(listId: Long) {
        dao.getListWithItemsById(listId)?.let {
            dao.deleteList(it.list)
        }
    }
}
