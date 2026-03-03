package com.example.eventglow.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.eventglow.common.support.SupportScreenContent

@Composable
fun AdminSupportScreen(
    onBackClick: () -> Unit = {},
    onCallClick: () -> Unit = {},
    onEmailClick: () -> Unit = {},
    onHelpCenterClick: () -> Unit = {}
) {
    SupportScreenContent(
        onBackClick = onBackClick,
        onCallClick = onCallClick,
        onEmailClick = onEmailClick,
        onHelpCenterClick = onHelpCenterClick,
        navigationIcon = {
            Box {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        }
    )
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun AdminSupportScreenPreview() {
    AdminSupportScreen()
}
