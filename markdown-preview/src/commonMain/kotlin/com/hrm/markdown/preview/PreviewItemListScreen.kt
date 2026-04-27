package com.hrm.markdown.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PreviewItemListScreen(
    title: String,
    items: List<PreviewItem>,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        if (items.size == 1) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                PreviewCard(
                    item = items.first(),
                    modifier = Modifier.fillMaxSize(),
                    expandContent = true
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    PreviewCard(item = item)
                }
            }
        }
    }
}

@Composable
internal fun PreviewCard(
    item: PreviewItem,
    modifier: Modifier = Modifier,
    expandContent: Boolean = false,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = if (expandContent) {
                Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            } else {
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            }
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Box(
                modifier = if (expandContent) {
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                } else {
                    // In LazyColumn item scope, children are measured with infinite maxHeight.
                    // If the content is scrollable (e.g. Markdown uses verticalScroll),
                    // it must be constrained to avoid "infinity max height" crash.
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                }
            ) {
                item.content()
            }
        }
    }
}
