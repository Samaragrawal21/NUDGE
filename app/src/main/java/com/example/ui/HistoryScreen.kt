package com.example.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NudgeEntity
import com.example.ui.theme.AcidYellow
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DarkGrey
import com.example.ui.theme.MidGrey
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    viewModel: NudgeViewModel,
    onBack: () -> Unit
) {
    val historyNudges by viewModel.historyNudges.collectAsState()
    val activeFilter by viewModel.historyFilter.collectAsState()

    var itemToDelete by remember { mutableStateOf<NudgeEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("history_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = PureWhite
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "HISTORY LOG",
                color = PureWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "[ ${historyNudges.size} ]",
                color = DarkGrey,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        HorizontalDivider(color = MidGrey, thickness = 1.dp)

        // Filter Chips Row
        val filters = listOf(
            HistoryFilter.LAST_1_DAY,
            HistoryFilter.LAST_7_DAYS,
            HistoryFilter.LAST_30_DAYS,
            HistoryFilter.LAST_6_MONTHS
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEach { filter ->
                val isSelected = filter == activeFilter
                val borderColor = if (isSelected) AcidYellow else DarkGrey
                val bgColor = if (isSelected) AcidYellow else PureBlack
                val textColor = if (isSelected) PureBlack else PureWhite

                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .border(1.dp, borderColor, RectangleShape)
                        .background(bgColor)
                        .combinedClickable(onClick = { viewModel.setHistoryFilter(filter) })
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = filter.label.uppercase(),
                        color = textColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        HorizontalDivider(color = MidGrey, thickness = 1.dp)

        // Swipe / Long Press Hint
        Text(
            text = "SWIPE OR LONG-PRESS AN ITEM TO DELETE",
            color = DarkGrey,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // History Items List
        if (historyNudges.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "NO NUDGES IN THIS TIMEFRAME",
                    color = DarkGrey,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = historyNudges,
                    key = { it.id }
                ) { nudge ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart || value == SwipeToDismissBoxValue.StartToEnd) {
                                itemToDelete = nudge
                                false // Handled via dialog
                            } else {
                                false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(DangerRed)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = PureWhite
                                )
                            }
                        }
                    ) {
                        HistoryNudgeCard(
                            nudge = nudge,
                            onLongClick = { itemToDelete = nudge },
                            onDeleteClick = { itemToDelete = nudge }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Single item deletion dialog
    itemToDelete?.let { nudge ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            shape = RectangleShape,
            containerColor = MidGrey,
            title = {
                Text(
                    text = "DELETE NUDGE?",
                    color = PureWhite,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Permanently remove \"${nudge.taskName}\" from history?",
                    color = PureWhite
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteNudge(nudge)
                        itemToDelete = null
                    },
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DangerRed,
                        contentColor = PureWhite
                    )
                ) {
                    Text("DELETE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { itemToDelete = null },
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PureBlack,
                        contentColor = PureWhite
                    )
                ) {
                    Text("CANCEL")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryNudgeCard(
    nudge: NudgeEntity,
    onLongClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val timeFormatted = remember(nudge.scheduledTimeMillis) {
        val sdf = SimpleDateFormat("MMM d, yyyy • hh:mm a", Locale.getDefault())
        sdf.format(Date(nudge.scheduledTimeMillis))
    }

    val (statusLabel, statusColor) = when (nudge.status) {
        "COMPLETED" -> "✓ COMPLETED" to AcidYellow
        "MISSED" -> "✕ MISSED" to DangerRed
        "SNOOZED" -> "⟳ SNOOZED" to PureWhite
        else -> "PENDING" to DarkGrey
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MidGrey, RectangleShape)
            .background(PureBlack)
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            )
            .padding(14.dp)
            .testTag("history_item_${nudge.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = statusLabel,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "•",
                        color = DarkGrey,
                        fontSize = 11.sp
                    )
                    Text(
                        text = timeFormatted,
                        color = DarkGrey,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = nudge.taskName,
                    color = PureWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif
                )
            }

            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.testTag("delete_history_${nudge.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = DarkGrey
                )
            }
        }
    }
}
