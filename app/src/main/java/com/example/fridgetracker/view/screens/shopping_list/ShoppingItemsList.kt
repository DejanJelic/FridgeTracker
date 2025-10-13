package com.example.fridgetracker.view.screens.shopping_list

import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fridgetracker.model.ShoppingListItemEntity
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ShoppingItemsList(
    state: ShoppingListState,
    scaffoldState: ScaffoldState
) {
    val coroutineScope = rememberCoroutineScope()
    val itemHeightDp = 85.dp
    val density = LocalDensity.current
    val itemHeightPx = with(density) { itemHeightDp.toPx() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(
            items = state.displayList,
            key = { _, item -> item.id }
        ) { index, item ->
            ShoppingItemCard(
                item = item,
                index = index,
                state = state,
                itemHeightPx = itemHeightPx,
                onDelete = {
                    val itemName = state.deleteItem(item.id)
                    coroutineScope.launch {
                        scaffoldState.snackbarHostState.showSnackbar(
                            "$itemName removed",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            )
        }

        // Empty state
        if (state.displayList.isEmpty()) {
            item {
                EmptyShoppingListState()
            }
        }
    }
}

@Composable
fun ShoppingItemCard(
    item: ShoppingListItemEntity,
    index: Int,
    state: ShoppingListState,
    itemHeightPx: Float,
    onDelete: () -> Unit
) {
    val isDragging = state.draggedIndex == index
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
                        state.draggedIndex = index
                        state.accumulatedDrag = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consumePositionChange()
                        state.accumulatedDrag += dragAmount.y

                        val cur = state.draggedIndex
                        if (cur != null) {
                            val steps = (state.accumulatedDrag / itemHeightPx).roundToInt()
                            if (steps != 0) {
                                val newIndex = (cur + steps).coerceIn(
                                    0,
                                    state.displayList.size - 1
                                )
                                if (newIndex != cur) {
                                    val moved = state.displayList.removeAt(cur)
                                    state.displayList.add(newIndex, moved)
                                    state.draggedIndex = newIndex
                                    state.accumulatedDrag -= steps * itemHeightPx
                                }
                            }
                        }
                    },
                    onDragEnd = { state.endDrag() },
                    onDragCancel = { state.cancelDrag() }
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
                    state.toggleItemChecked(item.id, checked)
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
            QuantityControls(
                quantity = item.quantity,
                onDecrease = { state.updateItemQuantity(item.id, -1) },
                onIncrease = { state.updateItemQuantity(item.id, 1) }
            )

            // Delete button
            IconButton(
                onClick = onDelete,
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

@Composable
fun QuantityControls(
    quantity: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
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
                onClick = onDecrease,
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
                text = quantity.toString(),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.widthIn(min = 24.dp),
                textAlign = TextAlign.Center
            )

            IconButton(
                onClick = onIncrease,
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
}