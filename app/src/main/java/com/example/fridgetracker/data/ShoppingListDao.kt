package com.example.fridgetracker.data

import androidx.room.*
import com.example.fridgetracker.model.ShoppingListEntity
import com.example.fridgetracker.model.ShoppingListItemEntity
import com.example.fridgetracker.model.ShoppingListWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingListDao {

    @Transaction
    @Query("SELECT * FROM shopping_lists ORDER BY createdAtEpochMs DESC")
    fun getAllListsWithItemsFlow(): Flow<List<ShoppingListWithItems>>

    @Transaction
    @Query("SELECT * FROM shopping_lists WHERE id = :id LIMIT 1")
    suspend fun getListWithItemsById(id: Long): ShoppingListWithItems?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: ShoppingListEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ShoppingListItemEntity>)

    @Query("DELETE FROM shopping_list_items WHERE listId = :listId")
    suspend fun deleteItemsForList(listId: Long)

    @Delete
    suspend fun deleteList(list: ShoppingListEntity)

    @Query("DELETE FROM shopping_list_items WHERE id = :itemId")
    suspend fun deleteItemById(itemId: Long)

    @Update
    suspend fun updateItem(item: ShoppingListItemEntity)

    @Update
    suspend fun updateList(list: ShoppingListEntity)
}
