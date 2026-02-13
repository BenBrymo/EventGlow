package com.example.eventglow.user_HelpCenter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountHelpScreen(navController: NavController) {
    FAQScreen(
        navController = navController,
        title = "Account",
        faqList = listOf(
            "How to create an account?",
            "How to update my profile?",
            "How to change my password?"
        )
    )
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun FAQScreen(navController: NavController, title: String, faqList: List<String>) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    var selectedFaq by remember { mutableStateOf<String?>(null) }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetContent = {
            selectedFaq?.let { faq -> FAQDetailBottomSheet(faq) }
        },
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        sheetBackgroundColor = MaterialTheme.colorScheme.surface,
        scrimColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title, style = MaterialTheme.typography.titleMedium) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp)
            ) {
                item {
                    Text(
                        text = "Frequently Asked Questions",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(faqList) { faq ->
                    HelpFaqItem(faq, onClick = {
                        selectedFaq = faq
                        scope.launch { sheetState.show() }
                    })
                }
            }
        }
    }
}


@Composable
fun HelpFaqItem(faq: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "FAQ")
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = faq, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun FAQDetailBottomSheet(faq: String) {
    val details = when (faq) {
        "How to create an account?" -> listOf(
            "Step 1: Go to the sign-up page",
            "Step 2: Enter your details",
            "Step 3: Confirm your email"
        )

        "How to update my profile?" -> listOf(
            "Step 1: Go to 'My Profile'",
            "Step 2: Tap on 'Edit Profile'",
            "Step 3: Save changes"
        )

        "How to change my password?" -> listOf(
            "Step 1: Go to 'Settings'",
            "Step 2: Tap on 'Change Password'",
            "Step 3: Enter new password"
        )

        else -> listOf("No details available for this FAQ.")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Text(
            text = faq,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        details.forEach { step ->
            Text(text = "• $step", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
