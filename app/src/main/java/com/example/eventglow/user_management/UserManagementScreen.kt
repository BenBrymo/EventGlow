package com.example.eventts.user_management

import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.eventglow.dataClass.User
import com.example.eventglow.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserMgmtScreen(
    navController: NavController,
    viewModel: UserManagementViewModel = viewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }

    val users by viewModel.users.collectAsState()
    var showAddUserDialog by remember { mutableStateOf(false) }

    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchUsers()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Manage Users", color = MaterialTheme.colorScheme.primary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate back")
                    }
                },
            )
        }
    ) { paddingValues ->
        Surface(Modifier.padding(paddingValues)) {
            if (isLoading) {
                // Show loading indicator
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                UsersManagementContent(
                    navController = navController,
                    users = users,
                    onAddUserButtonClick = { showAddUserDialog = true }
                )
            }
        }
    }

    if (showAddUserDialog) {
        AddUserDialog(onDismissRequest = { showAddUserDialog = false })
    }
}

@Composable
fun UsersManagementContent(
    navController: NavController,
    users: List<User>,
    onAddUserButtonClick: () -> Unit,
    viewModel: UserManagementViewModel = viewModel()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        SearchBarUser(
            modifier = Modifier
                .fillMaxWidth(),
            navController = navController
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (users.isEmpty()) {
            Text(
                text = "No users available",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        } else {
            // Add User Button
            Row(
                horizontalArrangement = Arrangement.Center
            ) {
                AddUserButton(onClick = onAddUserButtonClick)
            }
            Spacer(modifier = Modifier.height(4.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(users) { user ->
                    UserItem(
                        user = user,
                        //onSuspendUser = { suspend -> viewModel.suspendUser(user.id, suspend) }
                    )
                }
            }
        }
    }
}

@Composable
fun AddUserDialog(onDismissRequest: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var role by remember { mutableStateOf("user") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current
    val viewModel: UserManagementViewModel = viewModel()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Add User") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.trim() },
                    label = { Text("Username") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it.trim()
                        emailError = if (Patterns.EMAIL_ADDRESS.matcher(it).matches()) null else "Invalid email address"
                    },
                    label = { Text("Email") },
                    isError = emailError != null
                )
                if (emailError != null) {
                    Text(
                        text = emailError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it.trim() },
                    label = { Text("Password") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it.trim() },
                    label = { Text("Role") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isEmpty() || email.isEmpty() || password.isEmpty() || role.isEmpty()) {
                        Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (emailError != null) {
                        Toast.makeText(context, "Enter a valid email", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    viewModel.addUser(email, password, name, role, context)
                    onDismissRequest()
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            Button(onClick = onDismissRequest) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBarUser(
    modifier: Modifier = Modifier,
    placeholder: String = "Search users",
    navController: NavController
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
    ) {
        TextField(
            value = "",
            onValueChange = {},
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            },
            colors = TextFieldDefaults.textFieldColors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            placeholder = {
                Text(placeholder, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f))
            },
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .onFocusChanged {
                    if (it.isFocused) {
                        navController.navigate(Routes.USER_MANAGEMENT_SEARCH_SCREEN)
                    }
                }
        )
    }
}

@Composable
fun AddUserButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        contentColor = Color.White,
        containerColor = MaterialTheme.colorScheme.scrim
    ) {
        Icon(Icons.Default.Add, contentDescription = "Add User")
    }
}

@Composable
fun UserItem(
    user: User
) {
    Row(
        modifier = Modifier
            .padding(10.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Circular profile image
        Card(
            elevation = CardDefaults.cardElevation(8.dp),
            shape = RoundedCornerShape(50)
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current).data(
                        data = user.profilePictureUrl
                    ).apply(block = fun ImageRequest.Builder.() {
                        crossfade(true)
                    }).build()
                ),
                contentDescription = "User Profile Image",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape), // Circular shape for the image
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = user.userName,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = user.role,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

}
