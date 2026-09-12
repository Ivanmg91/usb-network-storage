package com.usblocal.app.ui.browser.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun BreadcrumbBar(
    shareName: String,
    currentPath: String,
    onNavigateToPath: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val segments = buildBreadcrumbSegments(shareName, currentPath)

    // Auto-scroll to the end when path changes
    LaunchedEffect(currentPath) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Row(
        modifier = modifier
            .horizontalScroll(scrollState)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        segments.forEachIndexed { index, segment ->
            if (index > 0) {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val isLast = index == segments.lastIndex
            val textColor = if (isLast) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

            if (index == 0) {
                Icon(
                    imageVector = Icons.Filled.Storage,
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(end = 4.dp),
                    tint = textColor
                )
            }

            Text(
                text = segment.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Normal,
                color = textColor,
                modifier = Modifier
                    .clickable(enabled = !isLast) { onNavigateToPath(segment.path) }
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            )
        }
    }
}

private data class BreadcrumbSegment(val name: String, val path: String)

private fun buildBreadcrumbSegments(shareName: String, currentPath: String): List<BreadcrumbSegment> {
    val segments = mutableListOf(BreadcrumbSegment(shareName, ""))

    if (currentPath.isNotEmpty()) {
        val parts = currentPath.replace('/', '\\').trimStart('\\').split('\\').filter { it.isNotEmpty() }
        var accumulated = ""
        for (part in parts) {
            accumulated = if (accumulated.isEmpty()) part else "$accumulated\\$part"
            segments.add(BreadcrumbSegment(part, accumulated))
        }
    }

    return segments
}
