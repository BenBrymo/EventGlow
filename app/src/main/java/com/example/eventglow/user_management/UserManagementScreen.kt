package com.example.eventglow.user_management

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.eventglow.dataClass.User
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
    viewModel: UserManagementViewModel = viewModel(),
    onUserClick: (User) -> Unit = {},
    navController: NavController,
) {
    val users by viewModel.filteredUsers.collectAsState()
    val searchQuery by viewModel.searchQueryUser.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val vmErrorMessage by viewModel.errorMessage.collectAsState()
    val isAddingUser by viewModel.isAddingUser.collectAsState()
    val addUserError by viewModel.addUserError.collectAsState()
    val addUserSuccess by viewModel.addUserSuccess.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddUser by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.fetchUsers()
    }

    LaunchedEffect(addUserError) {
        val message = addUserError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearAddUserFeedback()
    }

    LaunchedEffect(addUserSuccess) {
        val message = addUserSuccess ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        showAddUser = false
        viewModel.clearAddUserFeedback()
    }

    LaunchedEffect(vmErrorMessage) {
        val message = vmErrorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearError()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text("Manage Users") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
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
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChangeUser
            )

            Spacer(Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn {
                    items(users) { user ->
                        UserRow(
                            user = user,
                            onClick = { onUserClick(user) }
                        )
                    }
                }
            }
        }
    }

    if (showAddUser) {
        AddUserBottomSheet(
            onDismiss = { showAddUser = false },
            isAdding = isAddingUser,
            onAdd = { u, e, p, r ->
                viewModel.addUser(
                    email = e,
                    password = p,
                    username = u,
                    role = r
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddUserBottomSheet(
    onDismiss: () -> Unit,
    isAdding: Boolean,
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
                    enabled = !isAdding,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandPrimary
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        if (isAdding) "Adding..." else "Add",
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
    user: User,
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
                    text = user.userName,
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


@Preview(showBackground = true, apiLevel = 34)
@Composable
fun ManageUsersScreenPreview() {
    ManageUsersScreen(
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


