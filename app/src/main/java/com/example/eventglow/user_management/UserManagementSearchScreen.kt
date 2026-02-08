package com.example.eventts.user_management


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.eventglow.dataClass.User

@Composable
fun UserManagementSearchScreen(
    modifier: Modifier = Modifier,
    userViewModel: UserManagementViewModel = viewModel(),
    navController: NavController
) {
    val searchQuery by userViewModel.searchQueryUser.collectAsState()
    val users by userViewModel.filteredUsers.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TextField(
            value = searchQuery,
            placeholder = { Text(text = "Search Users") },
            onValueChange = { query -> userViewModel.onSearchQueryChangeUser(query) },
            leadingIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate Back")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn {
            items(users) { user ->
                UserRow(user = user, onClick = { })
            }
        }
    }
}

@Composable
fun UserRow(user: User, onClick: (User) -> Unit) {
    Row(
        modifier = Modifier
            .clickable { onClick(user) }
            .padding(8.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Event Image
        Card(
            modifier = Modifier
                .size(105.dp)
                .clip(RoundedCornerShape(8.dp)),
            elevation = CardDefaults.cardElevation(4.dp),
        ) {
            val userImage =
                "https://www.google.com/url?sa=i&url=https%3A%2F%2Fwww.flaticon.com%2Ffree-icon%2Fuser_9187604&psig=AOvVaw2lIyKDNbyksdmmlpGdackI&ust=1722595159762000&source=images&cd=vfe&opi=89978449&ved=0CBEQjRxqFwoTCOielaGZ1IcDFQAAAAAdAAAAABAE"
            AsyncImage(
                model = userImage,
                contentDescription = "Event Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Event Details Column
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Text(
                text = user.userName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = user.role,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = user.email,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Text(
                text = if (user.isSuspended) "Suspended" else " Active",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

