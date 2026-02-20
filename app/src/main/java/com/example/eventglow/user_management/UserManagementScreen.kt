package com.example.eventglow.user_management

import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.eventglow.dataClass.User
import com.example.eventglow.navigation.Routes
import com.example.eventglow.ui.theme.Background
import com.example.eventglow.ui.theme.BorderDefault
import com.example.eventglow.ui.theme.BorderStrong
import com.example.eventglow.ui.theme.BrandPrimary
import com.example.eventglow.ui.theme.CardGray
import com.example.eventglow.ui.theme.Divider
import com.example.eventglow.ui.theme.TextHint
import com.example.eventglow.ui.theme.TextPrimary
import com.example.eventglow.ui.theme.TextSecondary


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageUsersScreen(
    users: List<UserUi> = listOf(
        UserUi(
            name = "BenBrymo",
            role = "admin"
        )
    ),
    onBack: () -> Unit = {},
    onAddUser: (username: String, email: String, password: String, role: String) -> Unit = { _, _, _, _ -> },
    onUserClick: (UserUi) -> Unit = {},
    navController: NavController,
) {

    var query by remember { mutableStateOf("") }
    var showAddUser by remember { mutableStateOf(false) }

    // ---- existing scaffold stays the same ----

    Scaffold(
        containerColor = Background,
        topBar = { /* unchanged */ },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddUser = true },
                containerColor = BrandPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = TextPrimary
                )
            }
        }
    ) { padding ->

        // ---- your existing content ----
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {

            Spacer(Modifier.height(12.dp))

            SearchUsersField(
                value = query,
                onValueChange = { query = it }
            )

            Spacer(Modifier.height(16.dp))

            val filtered = users.filter {
                it.name.contains(query, true)
            }

            LazyColumn {
                items(filtered) { user ->
                    UserRow(
                        user = user,
                        onClick = { onUserClick(user) }
                    )
                }
            }
        }
    }

    if (showAddUser) {
        AddUserBottomSheet(
            onDismiss = { showAddUser = false },
            onAdd = { u, e, p, r ->
                showAddUser = false
                onAddUser(u, e, p, r)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddUserBottomSheet(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String) -> Unit
) {

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var role by remember { mutableStateOf("user") }
    var roleExpanded by remember { mutableStateOf(false) }

    var showPassword by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardGray,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {

            Text(
                text = "Add User",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )

            Spacer(Modifier.height(16.dp))

            AddUserTextField(
                value = username,
                onValueChange = { username = it },
                placeholder = "Username",
                leadingIcon = Icons.Default.Person
            )

            Spacer(Modifier.height(12.dp))

            AddUserTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = "Email",
                leadingIcon = Icons.Default.Email
            )

            Spacer(Modifier.height(12.dp))

            AddUserPasswordField(
                value = password,
                onValueChange = { password = it },
                visible = showPassword,
                onToggleVisibility = { showPassword = !showPassword }
            )

            Spacer(Modifier.height(12.dp))

            RoleDropdown(
                value = role,
                expanded = roleExpanded,
                onExpandedChange = { roleExpanded = it },
                onSelect = {
                    role = it
                    roleExpanded = false
                }
            )

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {

                TextButton(onClick = onDismiss) {
                    Text(
                        "Cancel",
                        color = BrandPrimary
                    )
                }

                Spacer(Modifier.width(8.dp))

                Button(
                    onClick = {
                        onAdd(username, email, password, role)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandPrimary
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        "Add",
                        color = TextPrimary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}


@Composable
private fun AddUserPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    visible: Boolean,
    onToggleVisibility: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text("Password", color = TextHint)
        },
        leadingIcon = {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = TextHint
            )
        },
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = null,
                    tint = TextHint
                )
            }
        },
        singleLine = true,
        visualTransformation =
        if (visible) VisualTransformation.None
        else PasswordVisualTransformation(),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = CardGray,
            unfocusedContainerColor = CardGray,
            focusedBorderColor = BorderStrong,
            unfocusedBorderColor = BorderDefault,
            cursorColor = BrandPrimary,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        )
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoleDropdown(
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (String) -> Unit
) {

    val roles = listOf("user", "admin")

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {

        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            label = {
                Text("Role")
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
            },
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CardGray,
                unfocusedContainerColor = CardGray,
                focusedBorderColor = BorderStrong,
                unfocusedBorderColor = BorderDefault,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.background(CardGray)
        ) {
            roles.forEach {
                DropdownMenuItem(
                    text = {
                        Text(it, color = TextPrimary)
                    },
                    onClick = { onSelect(it) }
                )
            }
        }
    }
}


@Composable
private fun AddUserTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(placeholder, color = TextHint)
        },
        leadingIcon = {
            Icon(
                leadingIcon,
                contentDescription = null,
                tint = TextHint
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = CardGray,
            unfocusedContainerColor = CardGray,
            focusedBorderColor = BorderStrong,
            unfocusedBorderColor = BorderDefault,
            cursorColor = BrandPrimary,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        )
    )
}


data class UserUi(
    val name: String,
    val role: String
)

@Composable
private fun SearchUsersField(
    value: String,
    onValueChange: (String) -> Unit
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        placeholder = {
            Text(
                "Search users",
                style = MaterialTheme.typography.bodyMedium,
                color = TextHint
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = TextHint
            )
        },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = CardGray,
            unfocusedContainerColor = CardGray,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = BrandPrimary
        ),
        shape = RoundedCornerShape(26.dp),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = TextPrimary
        )
    )
}


@Composable
private fun UserRow(
    user: UserUi,
    onClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(CardGray),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = TextSecondary
                )
            }

            Spacer(Modifier.width(12.dp))

            Column {

                Text(
                    text = user.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = user.role,
                    style = MaterialTheme.typography.labelMedium,
                    color = BrandPrimary
                )
            }
        }

        Divider(
            color = Divider
        )
    }
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserMgmtScreen(
    navController: NavController,
    viewModel: UserManagementViewModel = viewModel()
) {

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
    onAddUserButtonClick: () -> Unit
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

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun ManageUsersScreenPreview() {
    ManageUsersScreen(
        users = listOf(
            UserUi(name = "Alice", role = "admin"),
            UserUi(name = "Brian", role = "user")
        ),
        navController = rememberNavController()
    )
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun SearchUsersFieldPreview() {
    SearchUsersField(
        value = "Ali",
        onValueChange = {}
    )
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun UserRowUiPreview() {
    UserRow(
        user = UserUi(name = "Alice", role = "admin"),
        onClick = {}
    )
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun UsersManagementContentPreview() {
    UsersManagementContent(
        navController = rememberNavController(),
        users = listOf(
            User(
                id = "u1",
                userName = "Alice",
                email = "alice@example.com",
                role = "admin",
                notificationsEnabled = true
            ),
            User(
                id = "u2",
                userName = "Brian",
                email = "brian@example.com",
                role = "user",
                notificationsEnabled = false
            )
        ),
        onAddUserButtonClick = {}
    )
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun SearchBarUserPreview() {
    SearchBarUser(navController = rememberNavController())
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun AddUserButtonPreview() {
    AddUserButton(onClick = {})
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun UserItemPreview() {
    UserItem(
        user = User(
            id = "u3",
            userName = "Claire",
            email = "claire@example.com",
            role = "user",
            notificationsEnabled = true
        )
    )
}
