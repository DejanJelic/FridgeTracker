package com.example.fridgetracker.view.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Image
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

data class LocationItem(
    val name: String,
    val color: Color
)

@Composable
fun LocationScreen(
    navController: NavController,
    vm: ProductViewModel,
    onMenuClick: () -> Unit
) {
    val products by vm.products.collectAsState()
    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()

    // Predefined locations with colors (order matters)
    val defaultLocations = listOf(
        LocationItem("Larder", Color(0xFFFFA726)),
        LocationItem("Fridge", Color(0xFF42A5F5)),
        LocationItem("Freezer", Color(0xFF29B6F6)),
        LocationItem("Pantry", Color(0xFFFFCA28)),
        LocationItem("Not stored", Color(0xFF9E9E9E))
    )

    var locations by remember { mutableStateOf(defaultLocations) }

    // Counts per location
    val locationCounts = remember(products) {
        locations.associate { location ->
            location.name to products.count { product ->
                (product.location ?: "Not stored").equals(location.name, ignoreCase = true)
            }
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { coroutineScope.launch { scaffoldState.drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                    }
                    Text("Locations", style = MaterialTheme.typography.h6, modifier = Modifier.weight(1f))
                }
            }
        },
        drawerContent = {
            AppDrawer(
                navController = navController,
                currentRoute = "location",
                closeDrawer = {
                    coroutineScope.launch {
                        scaffoldState.drawerState.close()
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* add location */ }, backgroundColor = Color(0xFFFFA726)) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            DraggableLocationList(
                locations = locations,
                locationCounts = locationCounts,
                onMoveCommit = { from, to ->
                    // commit final move on drop
                    locations = locations.toMutableList().apply {
                        val item = removeAt(from)
                        // when inserting after removal we need correct index:
                        // if inserting after original index, the target shifts left by 1
                        val adjusted = if (to > from) to - 1 else to
                        add(adjusted.coerceIn(0, size), item)
                    }
                }
            )
        }
    }
}

@Composable
fun DraggableLocationList(
    locations: List<LocationItem>,
    locationCounts: Map<String, Int>,
    onMoveCommit: (Int, Int) -> Unit
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    // drag state
    var draggedIndex by remember { mutableStateOf<Int?>(null) }      // index of the item being dragged (original list index)
    var dragY by remember { mutableStateOf(0f) }                    // current Y (local to LazyColumn) of pointer
    var draggedItemHeight by remember { mutableStateOf(0) }         // px height of dragged item
    var insertIndex by remember { mutableStateOf<Int?>(null) }      // current insertion index (0..n)
    var dragging by remember { mutableStateOf(false) }

    // helper: compute insertion index based on y coordinate
    fun computeInsertIndex(y: Float): Int? {
        val visible = listState.layoutInfo.visibleItemsInfo
        if (visible.isEmpty()) return null
        // find the item whose middle the pointer is over; default to nearest visible
        for (info in visible) {
            val top = info.offset.toFloat()
            val bottom = top + info.size
            val mid = top + info.size / 2f
            if (y < mid) {
                return info.index
            }
        }
        // pointer is after last visible -> insert at end (index = locations.size)
        return locations.size
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(locations) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            val idx = run {
                                // find visible item at offset.y
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
                                found
                            }
                            draggedIndex = idx
                            insertIndex = idx
                            dragY = offset.y
                            dragging = idx != null
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            dragY = change.position.y
                            // compute insert index (0..n)
                            val computed = computeInsertIndex(dragY)
                            if (computed != null) {
                                // keep bounds [0..locations.size]
                                insertIndex = computed.coerceIn(0, locations.size)
                                // try to auto-scroll if dragged near edges
                                val viewportHeight = listState.layoutInfo.viewportEndOffset
                                val threshold = 80
                                if (change.position.y.toInt() < threshold) {
                                    coroutineScope.launch {
                                        val first = listState.firstVisibleItemIndex
                                        listState.animateScrollToItem(first.coerceAtLeast(0) - 1)
                                    }
                                } else if (change.position.y.toInt() > viewportHeight - threshold) {
                                    coroutineScope.launch {
                                        val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                                        listState.animateScrollToItem((last + 1).coerceAtMost(locations.lastIndex))
                                    }
                                }
                            }
                        },
                        onDragEnd = {
                            // commit move only if we have draggedIndex and insertIndex
                            val from = draggedIndex
                            val to = insertIndex
                            if (from != null && to != null) {
                                // convert insertion index to target index for commit
                                // insertion index is position *after* which the item should be inserted in the list-without-item
                                // onMoveCommit expects target index in the original-list indexing semantics: we will pass 'to' as insertion index
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
            // we will render items in original order, but:
            // - the dragged item will be rendered semi-transparent (or as spacer) so space remains
            // - we will render an insertion placeholder between items at insertIndex
            itemsIndexed(items = locations, key = { index, item -> item.name }) { index, location ->
                // if current insertion index equals this index, render placeholder before the item
                if (insertIndex != null && insertIndex == index && dragging && draggedIndex != null) {
                    PlaceholderRow(heightPx = draggedItemHeight)
                }

                val isDragged = draggedIndex == index
                val elevation by animateDpAsState(if (isDragged) 12.dp else 1.dp)

                // keep space for the dragged item by rendering it with reduced alpha
                LocationItemCard(
                    location = location,
                    count = locationCounts[location.name] ?: 0,
                    isDragging = isDragged,
                    elevation = elevation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .zIndex(if (isDragged) 2f else 0f),
                    alpha = if (isDragged) 0.25f else 1f
                )
            }

            // placeholder at end (if insert at locations.size)
            if (insertIndex != null && insertIndex == locations.size && dragging) {
                item {
                    PlaceholderRow(heightPx = draggedItemHeight)
                }
            }
        }

        // Overlay: floating representation of the dragged item that follows pointer
        if (dragging && draggedIndex != null) {
            val localYdp = with(density) { (dragY - draggedItemHeight / 2f).toDp() }
            // place overlay with some horizontal padding
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .offset { IntOffset(0, with(density) { localYdp.toPx().roundToInt() }) }
                    .zIndex(10f)
            ) {
                // show a copy (elevated)
                val loc = locations.getOrNull(draggedIndex!!)
                if (loc != null) {
                    Surface(elevation = 12.dp, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier
                            .background(Color.White)
                            .padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .width(6.dp)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(loc.color)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = loc.name, style = MaterialTheme.typography.body1.copy(fontWeight = FontWeight.Medium))
                                Spacer(modifier = Modifier.weight(1f))
                                Text(text = (locationCounts[loc.name] ?: 0).toString(), fontWeight = FontWeight.Bold, color = Color.Gray)
                                Spacer(modifier = Modifier.width(12.dp))
                                Icon(imageVector = Icons.Default.Place, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceholderRow(heightPx: Int) {
    // convert px to dp and show a dashed-ish placeholder box (simple shaded rectangle)
    val density = LocalDensity.current
    val h = with(density) { heightPx.toDp().coerceAtLeast(48.dp) }
    Box(modifier = Modifier
        .fillMaxWidth()
        .height(h)
        .padding(horizontal = 16.dp, vertical = 6.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(Color(0xFFEDE7F6).copy(alpha = 0.6f))
    ) {
        // a small indicator line in the middle
        Box(modifier = Modifier
            .height(2.dp)
            .fillMaxWidth(0.6f)
            .align(Alignment.Center)
            .background(Color(0xFF6A1B9A))
        )
    }
}

@Composable
fun LocationItemCard(
    location: LocationItem,
    count: Int,
    isDragging: Boolean = false,
    elevation: androidx.compose.ui.unit.Dp = 1.dp,
    modifier: Modifier = Modifier,
    alpha: Float = 1f
) {
    Card(
        modifier = modifier.shadow(elevation),
        elevation = elevation,
        shape = RoundedCornerShape(8.dp),
        backgroundColor = if (isDragging) Color(0xFFF5F5F5) else Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .alpha(alpha),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier
                .width(6.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(location.color)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(text = location.name, style = MaterialTheme.typography.body1, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = count.toString(), fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 16.sp)
                Icon(imageVector = Icons.Default.Place, contentDescription = "Products count", tint = Color.Gray, modifier = Modifier.size(18.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Icon(imageVector = Icons.Default.Menu, contentDescription = "Drag handle", tint = Color.Gray, modifier = Modifier.size(20.dp))
        }
    }
}
