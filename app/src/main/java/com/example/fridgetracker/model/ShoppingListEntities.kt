package com.example.fridgetracker.model

import androidx.room.*

@Entity(tableName = "shopping_lists")
data class ShoppingListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "shopping_list_items",
    foreignKeys = [
        ForeignKey(
            entity = ShoppingListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("listId")]
)
data class ShoppingListItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val listId: Long,
    val name: String,
    val quantity: Int = 1,
    val checked: Boolean = false,
    val orderIndex: Int = 0
)

// relation wrapper
data class ShoppingListWithItems(
@Embedded val list: ShoppingListEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "listId",
        entity = ShoppingListItemEntity::class
    )
    var items: List<ShoppingListItemEntity>
)
