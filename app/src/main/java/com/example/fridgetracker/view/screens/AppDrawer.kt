package com.example.fridgetracker.view.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun AppDrawer(
    navController: NavController,
    currentRoute: String,
    closeDrawer: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFFFA726),
            elevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF6A1B9A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "App icon",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "Best Before",
                    style = MaterialTheme.typography.h5.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6A1B9A)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Menu items
        DrawerMenuItem(
            icon = Icons.Default.ShoppingCart,
            title = "Shopping list",
            isSelected = currentRoute == "shopping",
            onClick = {
                navController.navigate("shopping")
                closeDrawer()
            }
        )

        DrawerMenuItem(
            icon = Icons.Default.Restaurant,
            title = "Products",
            isSelected = currentRoute == "home",
            onClick = {
                navController.navigate("home") {
                    popUpTo("home") { inclusive = true }
                }
                closeDrawer()
            }
        )

//        DrawerMenuItem(
//            icon = Icons.Default.DateRange,
//            title = "Consumption",
//            isSelected = currentRoute == "consumption",
//            onClick = {
//                // TODO: Navigate to consumption
//                closeDrawer()
//            }
//        )

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        DrawerMenuItem(
            icon = Icons.Default.Refresh,
            title = "Account",
            isSelected = false,
            onClick = {
                // TODO: Navigate to account
                closeDrawer()
            }
        )

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        DrawerMenuItem(
            icon = Icons.Default.LocalOffer,
            title = "Category",
            isSelected = currentRoute == "category",
            onClick = {
                navController.navigate("category")
                closeDrawer()
            }
        )

        DrawerMenuItem(
            icon = Icons.Default.Place,
            title = "Location",
            isSelected = currentRoute == "location",
            onClick = {
                navController.navigate("location")
                closeDrawer()
            }
        )

        DrawerMenuItem(
            icon = Icons.Default.Lightbulb,
            title = "Suggestion",
            isSelected = currentRoute == "suggestions",
            onClick = {
                navController.navigate("suggestions")
                closeDrawer()
            }
        )

        Divider(modifier = Modifier.padding(vertical = 8.dp))

//        DrawerMenuItem(
//            icon = Icons.Default.Settings,
//            title = "Settings",
//            isSelected = false,
//            onClick = {
//                // TODO: Navigate to settings
//                closeDrawer()
//            }
//        )

        DrawerMenuItem(
            icon = Icons.Default.Info,
            title = "Help",
            isSelected = false,
            onClick = {
                // TODO: Navigate to help
                closeDrawer()
            }
        )
    }
}

@Composable
fun DrawerMenuItem(
    icon: ImageVector,
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) Color(0xFFE1BEE7) else Color.Transparent
    val textColor = if (isSelected) Color(0xFF6A1B9A) else Color.Black

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = textColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.body1,
                color = textColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}