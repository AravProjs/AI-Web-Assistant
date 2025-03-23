package com.example.aiwebsummarizer.ui.summary

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.aiwebsummarizer.data.model.Summary
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    navController: NavController,
    onNavigateToQuery: () -> Unit = {}
) {
    // Get the context to pass to the Factory
    val context = LocalContext.current

    // Create ViewModel using Factory
    val viewModel = viewModel<SummaryViewModel>(
        factory = SummaryViewModel.Factory(context)
    )

    var url by remember { mutableStateOf("") }
    var showHistory by remember { mutableStateOf(false) }

    val summaryState by viewModel.summaryState.collectAsState()
    val historyState by viewModel.historyState.collectAsState()

    LaunchedEffect(key1 = true) {
        viewModel.loadSummaryHistory()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Web Summarizer") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // New section for switching between summarizer and search features
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Try our new search feature!",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(end = 16.dp)
                )

                Button(onClick = onNavigateToQuery) {
                    Text("Go to Search")
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Enter URL to summarize") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            Button(
                onClick = {
                    if (url.isNotEmpty()) {
                        viewModel.summarizeUrl(url)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                enabled = url.isNotEmpty() && summaryState !is SummaryState.Loading
            ) {
                if (summaryState is SummaryState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Summarize")
                }
            }

            when (summaryState) {
                is SummaryState.Success -> {
                    val summary = (summaryState as SummaryState.Success).summary

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Summary",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Text(
                                text = summary.summarizedText,
                                style = MaterialTheme.typography.bodyLarge
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Source: ${summary.url}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                is SummaryState.Error -> {
                    Text(
                        text = (summaryState as SummaryState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
                else -> {
                    // Show nothing or a placeholder
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Previous Summaries",
                    style = MaterialTheme.typography.titleMedium
                )

                Switch(
                    checked = showHistory,
                    onCheckedChange = { showHistory = it }
                )
            }

            if (showHistory) {
                when (historyState) {
                    is HistoryState.Success -> {
                        val summaries = (historyState as HistoryState.Success).summaries

                        if (summaries.isEmpty()) {
                            Text(
                                text = "No summary history found",
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            LazyColumn {
                                items(summaries) { summary ->
                                    SummaryHistoryItem(summary = summary)
                                }
                            }
                        }
                    }
                    is HistoryState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(16.dp)
                                .align(Alignment.CenterHorizontally)
                        )
                    }
                    is HistoryState.Error -> {
                        Text(
                            text = (historyState as HistoryState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    else -> {
                        // Show nothing
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryHistoryItem(summary: Summary) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(summary.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = summary.summarizedText.take(100) + if (summary.summarizedText.length > 100) "..." else "",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "URL: ${summary.url}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Text(
                text = "Date: $formattedDate",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}