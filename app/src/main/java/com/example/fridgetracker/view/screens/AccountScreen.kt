package com.example.fridgetracker.view.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fridgetracker.model.UserProfile
import com.example.fridgetracker.view_model.UserProfileViewModel
import kotlinx.coroutines.launch
import com.example.fridgetracker.utilities.CustomSnackbar
import com.example.fridgetracker.utilities.SnackbarType

@Composable
fun AccountScreen(
    navController: NavController,
    vm: UserProfileViewModel,
    onMenuClick: () -> Unit
) {
    val profile by vm.profileState.collectAsState()
    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()

    // Local state for form fields
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf("") }

    // Preferences
    var isVegan by remember { mutableStateOf(false) }
    var isVegetarian by remember { mutableStateOf(false) }
    var isPorkFree by remember { mutableStateOf(false) }
    var isMeatFree by remember { mutableStateOf(false) }
    var isNoBeef by remember { mutableStateOf(false) }

    // Restrictions
    var isGlutenFree by remember { mutableStateOf(false) }
    var isNoLactose by remember { mutableStateOf(false) }
    var isNoAlcohol by remember { mutableStateOf(false) }
    var isNoShellfish by remember { mutableStateOf(false) }
    var isNoNuts by remember { mutableStateOf(false) }

    var showSaveDialog by remember { mutableStateOf(false) }
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    var snackbarType by remember { mutableStateOf(SnackbarType.SUCCESS) }

    // Load profile data when available
    LaunchedEffect(profile) {
        profile?.let { p ->
            firstName = p.firstName
            lastName = p.lastName
            dateOfBirth = p.dateOfBirth
            weight = p.weight?.toString() ?: ""
            selectedGender = p.gender

            isVegan = p.isVegan
            isVegetarian = p.isVegetarian
            isPorkFree = p.isPorkFree
            isMeatFree = p.isMeatFree
            isNoBeef = p.isNoBeef

            isGlutenFree = p.isGlutenFree
            isNoLactose = p.isNoLactose
            isNoAlcohol = p.isNoAlcohol
            isNoShellfish = p.isNoShellfish
            isNoNuts = p.isNoNuts
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
                    Text("Account",  color = Color.White, style = MaterialTheme.typography.h6, modifier = Modifier.weight(1f))
                }
            }
        },
        drawerContent = {
            AppDrawer(
                navController = navController,
                currentRoute = "account",
                closeDrawer = {
                    coroutineScope.launch {
                        scaffoldState.drawerState.close()
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Personal Information Section
            SectionHeader(title = "Personal Information", icon = Icons.Default.Person)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text("First Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("Last Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = dateOfBirth,
                        onValueChange = { dateOfBirth = it },
                        label = { Text("Date of Birth (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Cake, contentDescription = null) },
                        placeholder = { Text("1990-01-01") }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Weight (kg)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        leadingIcon = { Icon(Icons.Default.MonitorWeight, contentDescription = null) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Gender", style = MaterialTheme.typography.caption, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))

                    val genderOptions = listOf("Male", "Female", "Other", "Prefer not to say")
                    var expandedGender by remember { mutableStateOf(false) }

                    Box {
                        OutlinedButton(
                            onClick = { expandedGender = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Wc, contentDescription = null, tint = Color(0xFFFFC107))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(selectedGender.ifBlank { "Select Gender" }, color = Color.Black)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }

                        DropdownMenu(
                            expanded = expandedGender,
                            onDismissRequest = { expandedGender = false }
                        ) {
                            genderOptions.forEach { gender ->
                                DropdownMenuItem(onClick = {
                                    selectedGender = gender
                                    expandedGender = false
                                }) {
                                    Text(gender)
                                }
                            }
                        }
                    }
                }
            }

            // Dietary Preferences Section
            SectionHeader(title = "Dietary Preferences", icon = Icons.Default.Restaurant)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    PreferenceCheckbox(
                        label = "Vegan",
                        checked = isVegan,
                        onCheckedChange = { isVegan = it }
                    )

                    PreferenceCheckbox(
                        label = "Vegetarian",
                        checked = isVegetarian,
                        onCheckedChange = { isVegetarian = it }
                    )

                    PreferenceCheckbox(
                        label = "Pork Free",
                        checked = isPorkFree,
                        onCheckedChange = { isPorkFree = it }
                    )

                    PreferenceCheckbox(
                        label = "Meat Free",
                        checked = isMeatFree,
                        onCheckedChange = { isMeatFree = it }
                    )

                    PreferenceCheckbox(
                        label = "No Beef",
                        checked = isNoBeef,
                        onCheckedChange = { isNoBeef = it }
                    )
                }
            }

            // Dietary Restrictions Section
            SectionHeader(title = "Dietary Restrictions", icon = Icons.Default.Warning)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    PreferenceCheckbox(
                        label = "Gluten Free",
                        checked = isGlutenFree,
                        onCheckedChange = { isGlutenFree = it }
                    )

                    PreferenceCheckbox(
                        label = "No Lactose",
                        checked = isNoLactose,
                        onCheckedChange = { isNoLactose = it }
                    )

                    PreferenceCheckbox(
                        label = "No Alcohol",
                        checked = isNoAlcohol,
                        onCheckedChange = { isNoAlcohol = it }
                    )

                    PreferenceCheckbox(
                        label = "No Shellfish",
                        checked = isNoShellfish,
                        onCheckedChange = { isNoShellfish = it }
                    )

                    PreferenceCheckbox(
                        label = "No Nuts",
                        checked = isNoNuts,
                        onCheckedChange = { isNoNuts = it }
                    )
                }
            }

            // Save Button
            Button(
                onClick = { showSaveDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFFC107)),
                shape = RoundedCornerShape(28.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("SAVE PROFILE", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
        if (showSnackbar) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                CustomSnackbar(
                    message = snackbarMessage,
                    type = snackbarType,
                    onDismiss = { showSnackbar = false }
                )
            }
        }
    }

    // Save Confirmation Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            backgroundColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = Color(0xFFFFA726),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Save Profile",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            },
            text = { Text("Are you sure you want to save your profile information?",fontSize = 14.sp,color = Color.Gray) },
            confirmButton = {
                Button(onClick = {
                    val newProfile = UserProfile(
                        id = 1L,
                        firstName = firstName,
                        lastName = lastName,
                        dateOfBirth = dateOfBirth,
                        weight = weight.toDoubleOrNull(),
                        gender = selectedGender,
                        isVegan = isVegan,
                        isVegetarian = isVegetarian,
                        isPorkFree = isPorkFree,
                        isMeatFree = isMeatFree,
                        isNoBeef = isNoBeef,
                        isGlutenFree = isGlutenFree,
                        isNoLactose = isNoLactose,
                        isNoAlcohol = isNoAlcohol,
                        isNoShellfish = isNoShellfish,
                        isNoNuts = isNoNuts
                    )
                    vm.saveProfile(newProfile)
                    showSaveDialog = false
                    snackbarMessage = "Profile saved successfully!"
                    snackbarType = SnackbarType.SUCCESS
                    showSnackbar = true

                    coroutineScope.launch {
                        kotlinx.coroutines.delay(3000)
                        showSnackbar = false
                    }
                },
                    colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFFFFA726)
                ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("SAVE", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("CANCEL",color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFFFFC107),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFC107)
        )
    }
}

@Composable
fun PreferenceCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFFFFC107),
                uncheckedColor = Color.Gray
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, fontSize = 16.sp)
    }
}