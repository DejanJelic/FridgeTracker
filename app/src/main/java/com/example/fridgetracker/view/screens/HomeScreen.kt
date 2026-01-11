package com.example.fridgetracker.view.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.fridgetracker.model.Product
import com.example.fridgetracker.view_model.ProductViewModel
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    vm: ProductViewModel = viewModel()
) {
    val products by vm.products.collectAsState()
    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()

    // Search state
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    // Pager / Tabs
    val tabTitles = listOf("All", "Ready", "Opened", "Expired")
    val pagerState = rememberPagerState(initialPage = 0) { tabTitles.size }

    // Filter / Sort / Panel state
    var panelOpen by remember { mutableStateOf(false) } // slide-in panel
    var selectedSort by remember { mutableStateOf(SortOption.BEST_BEFORE) }
    var ascending by remember { mutableStateOf(true) }

    // Multi-field filter state (UI values)
    var selectedCategoryUI by remember { mutableStateOf("All") } // "All" means no filter
    var minQtyText by remember { mutableStateOf("") } // numeric string
    var maxQtyText by remember { mutableStateOf("") }

    // Applied filter values (used to compute lists)
    var appliedCategory by remember { mutableStateOf<String?>(null) } // null = all
    var appliedMinQty by remember { mutableStateOf<Double?>(null) }
    var appliedMaxQty by remember { mutableStateOf<Double?>(null) }

    // Page loader (only initial)
    var initialLoading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        // show loader briefly on app start (adjust delay if needed)
        delay(700)
        initialLoading = false
    }

    // Panel-local state (reinitialized on open)
    var currentSortLocal by remember { mutableStateOf(selectedSort) }
    var ascLocal by remember { mutableStateOf(ascending) }

    // Helper status functions (product-level)
    fun Product.isExpired(): Boolean = daysUntil(this.bestBeforeEpochDay) <= 0L
    fun Product.isOpened(): Boolean = this.openedAtEpochDay != null
    fun Product.isReady(): Boolean = !isOpened() && !isExpired() && this.quantity > 0.0

    // location colors - use same palette as your Location screen if present
    val locationColors = mapOf(
        "Fridge" to Color(0xFF42A5F5),
        "Freezer" to Color(0xFFFF6026),
        "Pantry" to Color(0xFFFFCA28),
        "Larder" to Color(0xFFFFA726),
        "Not stored" to Color(0xFF9E9E9E)
    )
    val defaultLocationColor = Color(0xFFFFC107)

    // Derived list of categories (from products) for filter dropdown, include "All" first
    val categories = remember(products) {
        val fromProducts = products.mapNotNull { it.category?.takeIf { c -> c.isNotBlank() } }.distinct().sorted()
        listOf("All") + fromProducts
    }

    // When panel opens, initialize panel-local controls from applied/selected state
    LaunchedEffect(panelOpen) {
        if (panelOpen) {
            currentSortLocal = selectedSort
            ascLocal = ascending
            selectedCategoryUI = appliedCategory ?: "All"
            minQtyText = appliedMinQty?.toString() ?: ""
            maxQtyText = appliedMaxQty?.toString() ?: ""
        }
    }

    // Search + applied filters: memoized
    val filtered = remember(products, query, appliedCategory, appliedMinQty, appliedMaxQty) {
        products.filter { p ->
            // search match
            val matchesQuery = query.isBlank() ||
                    p.name.contains(query, ignoreCase = true) ||
                    (p.barcode?.contains(query, ignoreCase = true) ?: false)

            // category match
            val matchesCategory = appliedCategory == null || appliedCategory == "All" || (p.category ?: "").equals(appliedCategory ?: "", ignoreCase = true)

            // qty match
            val matchesMin = appliedMinQty?.let { p.quantity >= it } ?: true
            val matchesMax = appliedMaxQty?.let { p.quantity <= it } ?: true

            matchesQuery && matchesCategory && matchesMin && matchesMax
        }
    }

    // Counts for tabs (based on filtered)
    val counts = remember(filtered) {
        listOf(
            filtered.size,
            filtered.count { it.isReady() },
            filtered.count { it.isOpened() },
            filtered.count { it.isExpired() }
        )
    }

    // Sorting helper (stable)
    fun sortList(list: List<Product>, sort: SortOption, asc: Boolean): List<Product> {
        val result = when (sort) {
            SortOption.NAME -> list.sortedBy { it.name.lowercase() }
            SortOption.PURCHASE_DATE -> list.sortedBy { it.addedAtEpochDay }
            SortOption.BEST_BEFORE -> list.sortedBy { it.bestBeforeEpochDay }
            SortOption.REMAINING_QTY -> list.sortedBy { it.quantity }
            SortOption.CATEGORY -> list.sortedBy { (it.category ?: "").lowercase() }
        }
        return if (asc) result else result.reversed()
    }

    // Lists by tab with current sort applied to the filtered dataset
    val listsByTab = remember(filtered, selectedSort, ascending) {
        listOf(
            sortList(filtered, selectedSort, ascending), // All
            sortList(filtered.filter { it.isReady() }, selectedSort, ascending),
            sortList(filtered.filter { it.isOpened() }, selectedSort, ascending),
            sortList(filtered.filter { it.isExpired() }, selectedSort, ascending)
        )
    }

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        if (!searchOpen) {
                            Text("Products")
                        } else {
                            TextField(
                                value = query,
                                onValueChange = { query = it },
                                placeholder = { Text("Search by name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.textFieldColors(
                                    backgroundColor = Color(0xFF6A1B9A),
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    textColor = Color.White,
                                    placeholderColor = Color.White.copy(alpha = 0.7f)
                                )
                            )
                        }
                    },
                    backgroundColor = Color(0xFF6A1B9A),
                    contentColor = Color.White,
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { scaffoldState.drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        if (!searchOpen) {
                            IconButton(onClick = { searchOpen = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                        } else {
                            IconButton(onClick = { searchOpen = false; query = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Close search")
                            }
                        }
                        IconButton(onClick = { panelOpen = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter/Sort")
                        }
                    }
                )

                // TabRow with badge; badge positioned to the right/top so it doesn't overlap label
                TabRow(selectedTabIndex = pagerState.currentPage, backgroundColor = MaterialTheme.colors.surface) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } }
                        ) {
                            Box(modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp)) {
                                Text(
                                    text = title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(end = 18.dp) // reserve space for badge
                                )
                                val count = counts.getOrElse(index) { 0 }
                                if (count > 0) {
                                    SmallBadge(
                                        count = count,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 6.dp, y = (-6).dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        drawerContent = {
            AppDrawer(
                navController = navController,
                currentRoute = "home",
                closeDrawer = {
                    coroutineScope.launch {
                        scaffoldState.drawerState.close()
                    }
                }
            )
        },
        // HIDE FAB while panel is open to avoid overlap with APPLY
        floatingActionButton = {
            if (!panelOpen) {
                FloatingActionButton(onClick = onAdd, backgroundColor = Color(0xFFFFC107)) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // HorizontalPager (swipe + animation)
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                val pageList = listsByTab.getOrElse(page) { emptyList() }

                // Group by location in desired order
                val orderedLocations = listOf("Larder", "Fridge", "Freezer", "Pantry", "Not stored")
                val groupedMap = pageList.groupBy { it.location ?: "Not stored" }
                val groupedSorted = groupedMap.toList().sortedWith(compareBy { (loc, _) ->
                    val idx = orderedLocations.indexOfFirst { it.equals(loc, ignoreCase = true) }
                    if (idx >= 0) idx else orderedLocations.size
                })

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (pageList.isEmpty()) {
                        item {
                            EmptyStateCard(page = page)
                        }
                    } else {
                        groupedSorted.forEach { (loc, list) ->
                            val color = locationColors.entries.firstOrNull { it.key.equals(loc, ignoreCase = true) }?.value ?: defaultLocationColor
                            item { GroupHeaderColored(title = loc, count = list.size, color = color) }
                            items(list) { p -> ProductCard(product = p, onClick = { onOpen(p.id) }) }
                        }
                    }
                }
            }

            // Initial Page loader overlay (only on app start)
            if (initialLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Loading...", color = Color.Black)
                    }
                }
            }

            // Slide-in panel from right for filter/sort
            AnimatedVisibility(
                visible = panelOpen,
                enter = slideInHorizontally(animationSpec = tween(durationMillis = 300), initialOffsetX = { fullWidth -> fullWidth }) + fadeIn(),
                exit = slideOutHorizontally(animationSpec = tween(durationMillis = 260), targetOffsetX = { fullWidth -> fullWidth }) + fadeOut(),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                // Panel card - with scrollable body and fixed bottom buttons
                Card(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(320.dp)
                        .padding(12.dp),
                    shape = RoundedCornerShape(8.dp),
                    elevation = 12.dp
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text("Filters & Sort", fontWeight = FontWeight.Bold)
                            IconButton(onClick = { panelOpen = false }) { Icon(Icons.Default.Close, contentDescription = "Close") }
                        }

                        // Scrollable content
                        Column(modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp)
                        ) {
                            Spacer(modifier = Modifier.height(6.dp))

                            // Category dropdown
                            Text("Category", style = MaterialTheme.typography.caption)
                            var expandedCat by remember { mutableStateOf(false) }
                            Box {
                                OutlinedButton(onClick = { expandedCat = true }, modifier = Modifier.fillMaxWidth()) {
                                    Text(selectedCategoryUI)
                                    Spacer(Modifier.weight(1f))
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                                DropdownMenu(expanded = expandedCat, onDismissRequest = { expandedCat = false }) {
                                    categories.forEach { c ->
                                        DropdownMenuItem(onClick = { selectedCategoryUI = c; expandedCat = false }) {
                                            Text(c)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Min/Max Quantity inputs
                            Text("Remaining quantity (min / max)", style = MaterialTheme.typography.caption)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = minQtyText,
                                    onValueChange = { minQtyText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("min") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                )
                                OutlinedTextField(
                                    value = maxQtyText,
                                    onValueChange = { maxQtyText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("max") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Sort options (compact)
                            Text("Sort by", style = MaterialTheme.typography.caption)
                            val options = listOf(
                                Pair(SortOption.NAME, "Name"),
                                Pair(SortOption.PURCHASE_DATE, "Purchase date"),
                                Pair(SortOption.BEST_BEFORE, "Best before"),
                                Pair(SortOption.REMAINING_QTY, "Remaining quantity"),
                                Pair(SortOption.CATEGORY, "Category")
                            )
                            Column {
                                options.forEach { (opt, label) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { currentSortLocal = opt }
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(selected = currentSortLocal == opt, onClick = { currentSortLocal = opt })
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(label)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Ascending")
                                Spacer(modifier = Modifier.width(12.dp))
                                Switch(checked = ascLocal, onCheckedChange = { ascLocal = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFC107)))
                                Spacer(modifier = Modifier.weight(1f))
                                TextButton(onClick = {
                                    // reset UI state
                                    selectedCategoryUI = "All"
                                    minQtyText = ""
                                    maxQtyText = ""
                                    currentSortLocal = SortOption.BEST_BEFORE
                                    ascLocal = true
                                }) {
                                    Text("RESET")
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Bottom action row - fixed & always visible, with extra bottom padding so FAB (if shown) won't overlap
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .padding(bottom = 24.dp), // extra bottom space for safety (gestures / soft nav)
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(onClick = { panelOpen = false }, modifier = Modifier.weight(1f)) {
                                Text("CANCEL")
                            }
                            Button(onClick = {
                                // parse numeric fields
                                val min = minQtyText.toDoubleOrNull()
                                val max = maxQtyText.toDoubleOrNull()
                                // apply (update applied state)
                                appliedCategory = if (selectedCategoryUI == "All") null else selectedCategoryUI
                                appliedMinQty = min
                                appliedMaxQty = max
                                selectedSort = currentSortLocal
                                ascending = ascLocal
                                // close panel
                                panelOpen = false
                            }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFFC107))) {
                                Text("APPLY", color = Color.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Small top badge (not overlapping text) */
@Composable
fun SmallBadge(count: Int, modifier: Modifier = Modifier, size: Dp = 18.dp, textSize: TextUnit = 10.sp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(50))
            .background(Color.Red),
        contentAlignment = Alignment.Center
    ) {
        Text(text = count.toString(), color = Color.White, fontSize = textSize, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
    }
}

enum class SortOption {
    NAME,
    PURCHASE_DATE,
    BEST_BEFORE,
    REMAINING_QTY,
    CATEGORY
}

@Composable
fun EmptyStateCard(
    page: Int,
    modifier: Modifier = Modifier
) {
    when (page) {
        0 -> { // All tab
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Inventory,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("No products yet", fontSize = 18.sp, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    Text("Add your first product", fontSize = 14.sp, color = Color.Gray)
                }
            }
        }
        1 -> { // Ready tab
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("No ready products", fontSize = 18.sp, color = Color.Gray)
                }
            }
        }
        2 -> { // Opened tab
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.NoEncryption,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("No opened products", fontSize = 18.sp, color = Color.Gray)
                }
            }
        }
        3 -> { // Expired tab - SPECIAL CARD
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(2.dp, Color(0xFF6A1B9A)),
                backgroundColor = Color.White,
                elevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No expired products",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(24.dp))

                    Text(
                        text = "A product is \"Expired\" when best before date has passed.",
                        fontSize = 16.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "Click on it, you can consume or trash it.",
                        fontSize = 16.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                }
            }
        }
    }
}
@Composable
fun GroupHeaderColored(title: String, count: Int, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = color,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Spacer(modifier = Modifier.weight(1f))

            // Count
            Text(
                text = count.toString(),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color.Black,
                textAlign = TextAlign.End,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}
@Composable
fun ProductCard(
    product: Product,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(2.dp, Color(0xFF6A1B9A)),
        elevation = 2.dp
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            // IMAGE
            val imageModifier = Modifier
                .size(width = 64.dp, height = 64.dp)
                .clip(RoundedCornerShape(8.dp))

            Surface(modifier = imageModifier, color = Color(0xFFEFEFEF)) {
                if (!product.photoUri.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(product.photoUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Product image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.Image, contentDescription = "placeholder", tint = Color.LightGray)
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.h6.copy(fontSize = 20.sp),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!product.category.isNullOrBlank()) {
                        Text(text = product.category, color = Color(0xFFFB8C00), maxLines = 1, style = MaterialTheme.typography.body2)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    if (!product.location.isNullOrBlank()) {
                        Text(text = product.location, color = Color(0xFF90A4AE), maxLines = 1, style = MaterialTheme.typography.body2)
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(text = formatQuantity(product), style = MaterialTheme.typography.body1, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                val days = daysUntil(product.bestBeforeEpochDay)
                Text(text = "${days}d", style = MaterialTheme.typography.h6.copy(fontSize = 24.sp), fontWeight = FontWeight.Bold)
                product.openedAtEpochDay?.let { openedEpoch ->
                    val openedDays = daysSince(openedEpoch)
                    Text(text = "Opened ${openedDays}d", style = MaterialTheme.typography.caption, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
private fun formatQuantity(product: Product): String {
    val qtyInt = product.quantity.toInt()
    val unitLabel = when (product.unit) {
        "pcs", "piece" -> "piece"
        else -> product.unit
    }
    return "$qtyInt $unitLabel"
}

private fun daysUntil(epochDay: Long): Long {
    val today = LocalDate.now()
    val best = LocalDate.ofEpochDay(epochDay)
    return ChronoUnit.DAYS.between(today, best).coerceAtLeast(0L)
}

private fun daysSince(epochDay: Long): Long {
    val today = LocalDate.now()
    val opened = LocalDate.ofEpochDay(epochDay)
    return ChronoUnit.DAYS.between(opened, today).coerceAtLeast(0L)
}
