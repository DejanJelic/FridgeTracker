package com.example.fridgetracker.view.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fridgetracker.model.ProductDraft
import com.example.fridgetracker.model.Suggestion
import com.example.fridgetracker.view_model.ProductViewModel

@Composable
fun SuggestionsScreen(
    navController: NavController,
    vm: ProductViewModel,
    onMenuClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val suggestions = remember {
        listOf(
            Suggestion("Apple", "No unit", "Fruits", "Larder", "APP", Color(0xFF8BC34A)),
            Suggestion("Apricot", "No unit", "Fruits", "Not stored", "APR", Color(0xFFFF5252)),
            Suggestion("Banana", "No unit", "Fruits", "Larder", "BAN", Color(0xFFFFEB3B)),
            Suggestion("Blackberry", "No unit", "Fruits", "Not stored", "BLA", Color(0xFF9C27B0)),
            Suggestion("Blueberry", "No unit", "Fruits", "Not stored", "BLU", Color(0xFF2196F3)),
            Suggestion("Cherry", "No unit", "Fruits", "Not stored", "CHE", Color(0xFFE91E63)),
            Suggestion("Clementine", "No unit", "Fruits", "Not stored", "CLE", Color(0xFF03A9F4)),
            Suggestion("Coconut", "No unit", "Fruits", "Not stored", "COC", Color(0xFF9C27B0)),
            Suggestion("Cranberry", "No unit", "Fruits", "Not stored", "CRA", Color(0xFF9C27B0)),
            Suggestion("Date", "No unit", "Fruits", "Not stored", "DAT", Color(0xFFCDDC39)),
            Suggestion("Dragon fruit", "No unit", "Fruits", "Not stored", "DRA", Color(0xFF8BC34A))
        )
    }

    val filteredSuggestions = suggestions.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
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
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                    }
                    Text(
                        "Suggestion",
                        style = MaterialTheme.typography.h6,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { /* Search functionality */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                    }
                    IconButton(onClick = { /* Filter functionality */ }) {
                        Icon(
                            imageVector = Icons.Default.Menu, // Koristimo kao placeholder za filter
                            contentDescription = "Filter",
                            tint = Color.White
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add") },
                backgroundColor = Color(0xFFFFA726)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFFFA726),
                elevation = 2.dp
            ) {
                Text(
                    "Many presets",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.h5.copy(
                        fontWeight = FontWeight.Bold,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    ),
                    color = Color.White
                )
            }

            // Search bar (optional)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search suggestions...") },
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFF6A1B9A),
                    cursorColor = Color(0xFF6A1B9A)
                )
            )

            // Suggestions list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(filteredSuggestions) { suggestion ->
                    SuggestionItem(
                        suggestion = suggestion,
                        onClick = {
                            // Postavi prefill i idi na add screen
                            vm.setPrefill(
                                ProductDraft(
                                    name = suggestion.name,
                                    unit = if (suggestion.unit == "No unit") "pcs" else suggestion.unit,
                                    quantity = 1.0,
                                    daysUntilExpiry = 7,
                                    location = suggestion.location,
                                    category = suggestion.category
                                )
                            )
                            navController.navigate("add")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SuggestionItem(
    suggestion: Suggestion,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = 2.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Colored abbreviation box
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(suggestion.color),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = suggestion.abbreviation,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = suggestion.name,
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${suggestion.unit}\n${suggestion.category}\n${suggestion.location}",
                    style = MaterialTheme.typography.caption,
                    color = Color.Gray
                )
            }
        }
    }
}