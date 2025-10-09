package com.example.fridgetracker.view.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.example.fridgetracker.view_model.ProductViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class CategoryItem(
    val name: String,
    val color: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Category
)

@Composable
fun CategoryScreen(
    navController: NavController,
    vm: ProductViewModel,
    onMenuClick: () -> Unit
) {
    val products by vm.products.collectAsState()
    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()

    // initial categories (you can change order or items)
    val initialCategories = listOf(
        CategoryItem("No category", Color(0xFF9E9E9E)),
        CategoryItem("Fruits", Color(0xFFFFA726)),
        CategoryItem("Vegetables", Color(0xFF66BB6A)),
        CategoryItem("Legumes", Color(0xFF8BC34A)),
        CategoryItem("Meat", Color(0xFFEF5350)),
        CategoryItem("Fish", Color(0xFF42A5F5)),
        CategoryItem("Seafood", Color(0xFF26C6DA)),
        CategoryItem("Bread and cereals", Color(0xFF8D6E63)),
        CategoryItem("Dairy products", Color(0xFFBDBDBD)),
        CategoryItem("Desserts and sugary foods", Color(0xFFEC407A)),
        CategoryItem("Prepared foods and snack foods", Color(0xFFFFEE58)),
        CategoryItem("Spices and condiments", Color(0xFFFFCA28)),
        CategoryItem("Drinks", Color(0xFF5C6BC0)),
        CategoryItem("Alcohol", Color(0xFF7E57C2)),
        CategoryItem("Household and cleaning", Color(0xFF78909C))
    )

    // mutable list for reordering
    var categories by remember { mutableStateOf(initialCategories) }

    // counts per category from products
    val categoryCounts = remember(products) {
        categories.associate { cat ->
            cat.name to products.count { p -> (p.category ?: "No category") == cat.name }
        }
    }

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar(
                backgroundColor = Color(0xFF6A1B9A),
                contentColor = Color.White,
                elevation = 4.dp
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { coroutineScope.launch { scaffoldState.drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                    }
                    Text("Categories", style = MaterialTheme.typography.h6, modifier = Modifier.weight(1f))
                }
            }
        },
        drawerContent = {
            AppDrawer(
                navController = navController,
                currentRoute = "category",
                closeDrawer = {
                    coroutineScope.launch {
                        scaffoldState.drawerState.close()
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* TODO add category */ }, backgroundColor = Color(0xFFFFA726)) {
                Icon(Icons.Default.Add, contentDescription = "Add category", tint = Color.White)
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Draggable list with placeholder and overlay; commit only on drop
            DraggableCategoryList(
                categories = categories,
                categoryCounts = categoryCounts,
                onMoveCommit = { fromIndex, toInsertIndex ->
                    // commit reorder: remove at 'fromIndex' and insert at computed 'to'
                    categories = categories.toMutableList().apply {
                        val item = removeAt(fromIndex)
                        // when inserting, the index semantics are insertion position in list after removal
                        val adjusted = if (toInsertIndex > fromIndex) toInsertIndex - 1 else toInsertIndex
                        add(adjusted.coerceIn(0, size), item)
                    }
                    // If you want to persist ordering, call VM here: vm.saveCategoryOrder(categories)
                }
            )
        }
    }
}

@Composable
fun DraggableCategoryList(
    categories: List<CategoryItem>,
    categoryCounts: Map<String, Int>,
    onMoveCommit: (Int, Int) -> Unit
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Drag state
    var draggedIndex by remember { mutableStateOf<Int?>(null) }    // original index of dragged item
    var dragging by remember { mutableStateOf(false) }
    var dragY by remember { mutableStateOf(0f) }                   // pointer Y relative to LazyColumn
    var draggedItemHeight by remember { mutableStateOf(0) }       // px
    var insertIndex by remember { mutableStateOf<Int?>(null) }    // insertion index (0..n)

    // compute insertion index based on y
    fun computeInsertIndex(y: Float): Int {
        val visible = listState.layoutInfo.visibleItemsInfo
        if (visible.isEmpty()) return 0
        for (info in visible) {
            val top = info.offset.toFloat()
            val mid = top + info.size / 2f
            if (y < mid) {
                return info.index
            }
        }
        return categories.size
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(categories) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            // find index under the pointer
                            val visible = listState.layoutInfo.visibleItemsInfo
                            var found: Int? = null
                            for (info in visible) {
                                val top = info.offset.toFloat()
                                val bottom = top + info.size
                                if (offset.y >= top && offset.y <= bottom) {
                                    found = info.index
                                    draggedItemHeight = info.size
                                    break
                                }
                            }
                            draggedIndex = found
                            insertIndex = found
                            dragY = offset.y
                            dragging = found != null
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            dragY = change.position.y
                            val computed = computeInsertIndex(dragY)
                            insertIndex = computed.coerceIn(0, categories.size)
                            // auto-scroll when near edges
                            val viewportEnd = listState.layoutInfo.viewportEndOffset
                            val threshold = 80
                            if (change.position.y.toInt() < threshold) {
                                coroutineScope.launch {
                                    val first = listState.firstVisibleItemIndex
                                    if (first > 0) listState.animateScrollToItem(first - 1)
                                }
                            } else if (change.position.y.toInt() > viewportEnd - threshold) {
                                coroutineScope.launch {
                                    val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                                    if (last < categories.lastIndex) listState.animateScrollToItem(last + 1)
                                }
                            }
                        },
                        onDragEnd = {
                            val from = draggedIndex
                            val to = insertIndex
                            if (from != null && to != null) {
                                onMoveCommit(from, to)
                            }
                            // reset
                            draggedIndex = null
                            insertIndex = null
                            dragging = false
                            draggedItemHeight = 0
                        },
                        onDragCancel = {
                            draggedIndex = null
                            insertIndex = null
                            dragging = false
                            draggedItemHeight = 0
                        }
                    )
                },
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            itemsIndexed(items = categories, key = { index, item -> item.name }) { index, cat ->
                // if we have insertion index and it equals this index, render placeholder before this item
                if (insertIndex != null && insertIndex == index && dragging && draggedIndex != null) {
                    PlaceholderRow(heightPx = draggedItemHeight)
                }

                val isDragged = draggedIndex == index
                val elevation = if (isDragged) 12.dp else 1.dp

                CategoryItemCard(
                    category = cat,
                    count = categoryCounts[cat.name] ?: 0,
                    isDragged = isDragged,
                    elevation = elevation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .zIndex(if (isDragged) 2f else 0f),
                    alpha = if (isDragged) 0.25f else 1f
                )
            }

            // placeholder at end when insertIndex == categories.size
            if (insertIndex != null && insertIndex == categories.size && dragging) {
                item { PlaceholderRow(heightPx = draggedItemHeight) }
            }
        }

        // Overlay (floating copy) that follows pointer and shows dragged item
        if (dragging && draggedIndex != null) {
            val localYdp = with(density) { (dragY - draggedItemHeight / 2f).toDp() }
            val cat = categories.getOrNull(draggedIndex!!)
            if (cat != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .offset { IntOffset(0, with(density) { localYdp.toPx().roundToInt() }) }
                        .zIndex(10f)
                ) {
                    Surface(elevation = 12.dp, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier
                            .background(Color.White)
                            .padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier
                                .width(6.dp)
                                .height(44.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(cat.color)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = cat.name, style = MaterialTheme.typography.body1.copy(fontWeight = FontWeight.Medium))
                            Spacer(modifier = Modifier.weight(1f))
                            Text(text = (categoryCounts[cat.name] ?: 0).toString(), fontWeight = FontWeight.Bold, color = Color.Gray)
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(imageVector = Icons.Default.Restaurant, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceholderRow(heightPx: Int) {
    val density = LocalDensity.current
    val h = with(density) { heightPx.toDp().coerceAtLeast(52.dp) }
    Box(modifier = Modifier
        .fillMaxWidth()
        .height(h)
        .padding(horizontal = 16.dp, vertical = 6.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(Color(0xFFEDE7F6).copy(alpha = 0.6f))
    ) {
        Box(modifier = Modifier
            .height(2.dp)
            .fillMaxWidth(0.6f)
            .align(Alignment.Center)
            .background(Color(0xFF6A1B9A))
        )
    }
}

@Composable
fun CategoryItemCard(
    category: CategoryItem,
    count: Int,
    isDragged: Boolean = false,
    elevation: androidx.compose.ui.unit.Dp = 1.dp,
    modifier: Modifier = Modifier,
    alpha: Float = 1f
) {
    Card(
        modifier = modifier.shadow(elevation),
        elevation = elevation,
        shape = RoundedCornerShape(8.dp),
        backgroundColor = Color.White
    ) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .alpha(alpha),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier
                .width(6.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(category.color)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = category.name, style = MaterialTheme.typography.body1, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Restaurant, contentDescription = "Products count", tint = Color.Gray, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = count.toString(), fontWeight = FontWeight.Bold, color = Color.Gray)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Icon(imageVector = Icons.Default.Menu, contentDescription = "Drag handle", tint = Color.Gray, modifier = Modifier.size(20.dp))
        }
    }
}
