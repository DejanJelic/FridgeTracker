package com.example.fridgetracker.view.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.google.accompanist.pager.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.layout.Arrangement
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalPagerApi::class)
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
    val pagerState = rememberPagerState(initialPage = 0)

    // Filter / Sort state
    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedSort by remember { mutableStateOf(SortOption.BEST_BEFORE) }
    var ascending by remember { mutableStateOf(true) }

    // Page loader state: shown during applying sort / while pager scrolls
    var isLoading by remember { mutableStateOf(false) }

    // Helper status functions (product-level)
    fun Product.isExpired(): Boolean = daysUntil(this.bestBeforeEpochDay) <= 0L
    fun Product.isOpened(): Boolean = this.openedAtEpochDay != null
    fun Product.isReady(): Boolean = !isOpened() && !isExpired() && this.quantity > 0.0

    // Search applied across whole dataset (memoized)
    val searched = remember(products, query) {
        if (query.isBlank()) products
        else products.filter { p ->
            p.name.contains(query, ignoreCase = true) ||
                    (p.barcode?.contains(query, ignoreCase = true) ?: false)
        }
    }

    // Counts for tabs (based on current search)
    val counts = remember(searched) {
        listOf(
            searched.size,
            searched.count { it.isReady() },
            searched.count { it.isOpened() },
            searched.count { it.isExpired() }
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

    // Lists by tab with current sort
    val listsByTab = remember(searched, selectedSort, ascending) {
        listOf(
            sortList(searched, selectedSort, ascending), // All
            sortList(searched.filter { it.isReady() }, selectedSort, ascending),
            sortList(searched.filter { it.isOpened() }, selectedSort, ascending),
            sortList(searched.filter { it.isExpired() }, selectedSort, ascending)
        )
    }

    // when pager scrolls show a small loader overlay (visual nicety)
    LaunchedEffect(pagerState.isScrollInProgress) {
        if (pagerState.isScrollInProgress) {
            isLoading = true
        } else {
            // small delay to smooth flicker
            delay(80)
            isLoading = false
        }
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
                                placeholder = { Text("Search by name or barcode") },
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
                        IconButton(onClick = { showFilterDialog = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter/Sort")
                        }
                    }
                )

                // TabRow with badge (tab label bold); badge moved slightly right and up so it doesn't cover label
                TabRow(selectedTabIndex = pagerState.currentPage, backgroundColor = MaterialTheme.colors.surface) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } }
                        ) {
                            Box(modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)) {
                                Text(
                                    text = title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.Bold
                                )
                                val count = counts.getOrElse(index) { 0 }
                                if (count > 0) {
                                    // place badge to the right, slightly above text baseline
                                    Badge(
                                        count = count,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 12.dp, y = (-6).dp)
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
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd, backgroundColor = Color(0xFFFFC107)) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            // HorizontalPager (swipe + animation)
            HorizontalPager(count = tabTitles.size, state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                val pageList = listsByTab.getOrElse(page) { emptyList() }

                // Group by location in desired order: Larder, Fridge, Freezer, Pantry, Not stored (others after)
                val orderedLocations = listOf("Larder", "Fridge", "Freezer", "Pantry", "Not stored")
                val groupedMap = pageList.groupBy { it.location ?: "Not stored" }
                // Convert to list of pairs and sort by index in orderedLocations
                val groupedSorted = groupedMap.toList().sortedWith(compareBy { (loc, _) ->
                    val idx = orderedLocations.indexOfFirst { it.equals(loc, ignoreCase = true) }
                    if (idx >= 0) idx else orderedLocations.size
                })

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (pageList.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text(text = "No products", style = MaterialTheme.typography.body1)
                            }
                        }
                    } else {
                        groupedSorted.forEach { (loc, list) ->
                            item { GroupHeader(title = loc, count = list.size) }
                            items(list) { p -> ProductCard(product = p, onClick = { onOpen(p.id) }) }
                        }
                    }
                }
            }

            // Page loader overlay: centered spinner with translucent background
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Working...", color = Color.Black)
                    }
                }
            }

            // Filter dialog UI (stylized right-panel)
            if (showFilterDialog) {
                RightStyleFilterDialog(
                    initial = selectedSort,
                    ascendingInitial = ascending,
                    onApply = { newSort, asc ->
                        // show loader briefly while applying sort so UI feels responsive
                        coroutineScope.launch {
                            isLoading = true
                            // small delay to let user see loader; in real app this would be the actual sort/filter operation
                            delay(300)
                            selectedSort = newSort
                            ascending = asc
                            isLoading = false
                            showFilterDialog = false
                        }
                    },
                    onCancel = { showFilterDialog = false }
                )
            }
        }
    }
}

/** Badge: small red top-notch */
@Composable
fun Badge(count: Int, modifier: Modifier = Modifier, size: Dp = 24.dp, textSize: TextUnit = 12.sp) {
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

/** Right-panel style filter dialog (custom Dialog) */
@Composable
fun RightStyleFilterDialog(
    initial: SortOption,
    ascendingInitial: Boolean,
    onApply: (SortOption, Boolean) -> Unit,
    onCancel: () -> Unit
) {
    var current by remember { mutableStateOf(initial) }
    var asc by remember { mutableStateOf(ascendingInitial) }

    Dialog(onDismissRequest = onCancel) {
        // Card styled panel (narrower, nicer buttons)
        Card(
            modifier = Modifier
                .fillMaxHeight()
                .width(280.dp)
                .padding(end = 8.dp),
            backgroundColor = Color(0xFF4A148C),
            shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp),
            elevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Header row with title + close
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Sort / Filter", color = Color.White, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onCancel) { Icon(Icons.Default.Close, tint = Color.White, contentDescription = "Close") }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Options (radio) - styled: white text, gold accent
                val options = listOf(
                    Pair(SortOption.NAME, "Name"),
                    Pair(SortOption.PURCHASE_DATE, "Purchase date"),
                    Pair(SortOption.BEST_BEFORE, "Best before"),
                    Pair(SortOption.REMAINING_QTY, "Remaining quantity"),
                    Pair(SortOption.CATEGORY, "Category")
                )

                options.forEach { (opt, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { current = opt }
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(
                            selected = (current == opt),
                            onClick = { current = opt },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFFFC107), unselectedColor = Color(0x80FFC107))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = label, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Ascending", color = Color.White)
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(checked = asc, onCheckedChange = { asc = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFC107)))
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onCancel) {
                        Text("CANCEL", color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onApply(current, asc) },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFFC107)),
                        elevation = ButtonDefaults.elevation(defaultElevation = 6.dp)
                    ) {
                        Text("APPLY", color = Color.Black)
                    }
                }
            }
        }
    }
}

enum class SortOption {
    NAME,
    PURCHASE_DATE,
    BEST_BEFORE,
    REMAINING_QTY,
    CATEGORY
}

/* -------------------------
   Reused UI pieces (GroupHeader, ProductCard, helpers)
   ------------------------- */

@Composable
fun GroupHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFFC107)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, modifier = Modifier.weight(1f).padding(12.dp), fontWeight = FontWeight.Bold)
        Text(count.toString(), modifier = Modifier.padding(end = 12.dp))
    }
}

@Composable
fun ProductCard(
    product: Product,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

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
