package com.example.eventglow.user

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.eventglow.dataClass.Transaction
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val SuccessGreen = Color(0xFF39D353)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    navController: NavController,
    transactionViewModel: TransactionViewModel = viewModel()
) {
    val transactions by transactionViewModel.transactions.collectAsState()
    val isLoading by transactionViewModel.isLoading.collectAsState()
    val errorMessage by transactionViewModel.errorMessage.collectAsState()

    LaunchedEffect(Unit) {
        transactionViewModel.fetchTransactions()
    }

    TransactionsScreenContent(
        transactions = transactions,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onRetry = {
            transactionViewModel.clearError()
            transactionViewModel.fetchTransactions()
        },
        onRefresh = { transactionViewModel.fetchTransactions() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionsScreenContent(
    transactions: List<Transaction>,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    onRefresh: () -> Unit
) {
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        "Transactions",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            TransactionSummaryRow(
                transactions = transactions,
                modifier = Modifier.padding(top = 8.dp)
            )

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                !errorMessage.isNullOrBlank() -> {
                    ErrorState(
                        message = errorMessage.orEmpty(),
                        onRetry = onRetry
                    )
                }

                transactions.isEmpty() -> {
                    EmptyState()
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(transactions) { transaction ->
                            TransactionCard(
                                transaction = transaction,
                                onClick = { selectedTransaction = transaction }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                }
            }
        }
    }

    selectedTransaction?.let { tx ->
        ModalBottomSheet(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = { selectedTransaction = null }
        ) {
            TransactionDetails(transaction = tx)
        }
    }
}

@Composable
private fun TransactionSummaryRow(
    transactions: List<Transaction>,
    modifier: Modifier = Modifier
) {
    val successCount = transactions.count { it.status.equals("success", ignoreCase = true) }
    val failedCount = transactions.count { !it.status.equals("success", ignoreCase = true) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryChip(title = "Total", value = transactions.size.toString(), modifier = Modifier.weight(1f))
        SummaryChip(
            title = "Success",
            value = successCount.toString(),
            accent = SuccessGreen,
            modifier = Modifier.weight(1f)
        )
        SummaryChip(
            title = "Failed",
            value = failedCount.toString(),
            accent = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryChip(
    title: String,
    value: String,
    accent: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = value,
                color = accent,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TransactionCard(
    transaction: Transaction,
    onClick: () -> Unit
) {
    val isSuccess = transaction.status.equals("success", ignoreCase = true)
    val statusColor = if (isSuccess) SuccessGreen else MaterialTheme.colorScheme.error
    val displayDate = formatDisplayDateTime(transaction.paidAt.ifBlank { transaction.createdAt.ifBlank { "" } })

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .width(42.dp)
                    .height(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.AttachMoney,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.reference.ifBlank { transaction.id.ifBlank { "Transaction" } },
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${transaction.currency.ifBlank { "GHS" }} ${formatAmountDisplay(transaction.amount)}",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = displayDate.ifBlank { "No date" },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(shape = RoundedCornerShape(50), color = statusColor.copy(alpha = 0.16f)) {
                Text(
                    text = transaction.status.ifBlank { "unknown" }.uppercase(),
                    color = statusColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Tap to retry",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.clickable(onClick = onRetry)
        )
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No transactions yet",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun TransactionDetails(transaction: Transaction) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Transaction Details",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge
        )
        TransactionDetailRow("Transaction ID", transaction.id)
        TransactionDetailRow("Status", transaction.status)
        TransactionDetailRow(
            "Amount",
            "${transaction.currency.ifBlank { "GHS" }} ${formatAmountDisplay(transaction.amount)}"
        )
        TransactionDetailRow("Paid At", formatDisplayDateTime(transaction.paidAt).ifBlank { "N/A" })
        TransactionDetailRow("Created At", formatDisplayDateTime(transaction.createdAt).ifBlank { "N/A" })
        TransactionDetailRow("Channel", transaction.channel.ifBlank { "N/A" })
        TransactionDetailRow("Reference", transaction.reference.ifBlank { "N/A" })
        TransactionDetailRow("Gateway", transaction.gatewayResponse.ifBlank { "N/A" })
    }
}

@Composable
fun TransactionDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun formatDisplayDateTime(raw: String): String {
    if (raw.isBlank()) return ""
    val parsers = listOf(
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        },
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        },
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    )
    val parsed: Date = parsers.firstNotNullOfOrNull { parser ->
        runCatching { parser.parse(raw) }.getOrNull()
    } ?: return raw

    return SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(parsed)
}

private fun formatAmountDisplay(rawAmount: String): String {
    val value = rawAmount.trim()
    if (value.isBlank()) return "0.00"

    val formatted = runCatching {
        val bd = BigDecimal(value)
        if (!value.contains(".")) {
            bd.divide(BigDecimal(100), 2, RoundingMode.HALF_UP).toPlainString()
        } else {
            bd.setScale(2, RoundingMode.HALF_UP).toPlainString()
        }
    }.getOrNull()

    return formatted ?: value
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun TransactionItemPreview() {
    TransactionCard(
        transaction = Transaction(
            id = "tx_1",
            status = "success",
            amount = "5000",
            currency = "GHS",
            paidAt = "2026-02-16T12:00:00Z",
            reference = "REF123",
            channel = "card"
        ),
        onClick = {}
    )
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun TransactionDetailsPreview() {
    TransactionDetails(
        transaction = Transaction(
            id = "tx_2",
            status = "success",
            amount = "7500",
            currency = "GHS",
            paidAt = "2026-02-16T10:30:00Z",
            createdAt = "2026-02-16T10:10:00Z",
            reference = "REF456",
            channel = "mobile_money"
        )
    )
}
