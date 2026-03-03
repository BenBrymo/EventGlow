package com.example.eventglow.common.help

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class HelpArticle(
    val title: String,
    val steps: List<String>
)

data class HelpTopic(
    val title: String,
    val icon: ImageVector,
    val article: HelpArticle
)

data class HelpFaq(
    val question: String,
    val article: HelpArticle
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpCenterContent(
    title: String,
    topics: List<HelpTopic>,
    faqs: List<HelpFaq>,
    onBack: () -> Unit,
    backIcon: ImageVector
) {
    var query by remember { mutableStateOf("") }
    var selectedArticle by remember { mutableStateOf<HelpArticle?>(null) }

    val normalizedQuery = query.trim()
    val filteredTopics = if (normalizedQuery.isBlank()) {
        topics
    } else {
        topics.filter {
            it.title.contains(normalizedQuery, ignoreCase = true) ||
                    it.article.steps.any { step -> step.contains(normalizedQuery, ignoreCase = true) }
        }
    }
    val filteredFaqs = if (normalizedQuery.isBlank()) {
        faqs
    } else {
        faqs.filter {
            it.question.contains(normalizedQuery, ignoreCase = true) ||
                    it.article.steps.any { step -> step.contains(normalizedQuery, ignoreCase = true) }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(backIcon, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search for help...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true
                )
            }

            item {
                Text(
                    text = "Topics",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            items(filteredTopics.size) { index ->
                val topic = filteredTopics[index]
                Card(
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable { selectedArticle = topic.article }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = topic.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.padding(6.dp))
                        Text(
                            text = topic.title,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Frequently Asked Questions",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            items(filteredFaqs.size) { index ->
                val faq = filteredFaqs[index]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedArticle = faq.article }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text(
                        text = faq.question,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            item { Spacer(modifier = Modifier.padding(8.dp)) }
        }
    }

    selectedArticle?.let { article ->
        ModalBottomSheet(
            onDismissRequest = { selectedArticle = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                article.steps.forEach { step ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = "- ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(modifier = Modifier.padding(8.dp))
            }
        }
    }
}
