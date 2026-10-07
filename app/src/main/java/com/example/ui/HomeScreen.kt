package com.example.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NudgeEntity
import com.example.ui.theme.AcidYellow
import com.example.ui.theme.DarkGrey
import com.example.ui.theme.MidGrey
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: NudgeViewModel,
    onNavigateToHistory: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val upcomingNudges by viewModel.upcomingNudges.collectAsState()
    val editingNudgeId by viewModel.editingNudgeId.collectAsState()
    val taskNameInput by viewModel.taskNameInput.collectAsState()
    val hourInput by viewModel.hourInput.collectAsState()
    val minuteInput by viewModel.minuteInput.collectAsState()
    val isPm by viewModel.isPm.collectAsState()
    val isTomorrow by viewModel.isTomorrow.collectAsState()

    var canDrawOverlays by remember {
        mutableStateOf(Settings.canDrawOverlays(context))
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canDrawOverlays = Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val minuteFocusRequester = remember { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // App Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(10.dp)
                        .height(24.dp)
                        .background(AcidYellow)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "NUDGE",
                    color = PureWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 2.sp
                )
            }

            // History Button
            Box(
                modifier = Modifier
                    .border(1.dp, DarkGrey, RectangleShape)
                    .clickable(onClick = onNavigateToHistory)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("history_nav_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "History",
                        tint = PureWhite,
                        modifier = Modifier.width(16.dp).height(16.dp)
                    )
                    Text(
                        text = "HISTORY",
                        color = PureWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Overlay Permission Banner if needed
        if (!canDrawOverlays) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .background(MidGrey)
                    .border(1.dp, AcidYellow, RectangleShape)
                    .clickable {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                    .padding(12.dp)
                    .testTag("permission_banner")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = AcidYellow,
                        modifier = Modifier.width(20.dp).height(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "GRANT OVERLAY PERMISSION",
                            color = AcidYellow,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Tap here to enable full-screen hijack over other apps",
                            color = PureWhite,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MidGrey, thickness = 1.dp)

        // ==========================================
        // TOP SECTION: OPEN INPUT BOX & SCHEDULING
        // ==========================================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Label & Mode indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editingNudgeId != null) "/// EDITING NUDGE ///" else "/// NEW NUDGE ///",
                    color = if (editingNudgeId != null) AcidYellow else DarkGrey,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )

                if (editingNudgeId != null) {
                    Text(
                        text = "CANCEL",
                        color = DarkGrey,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .clickable { viewModel.cancelEditing() }
                            .padding(4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Task Name Input
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (editingNudgeId != null) AcidYellow else DarkGrey, RectangleShape)
                    .background(MidGrey)
                    .padding(14.dp)
            ) {
                BasicTextField(
                    value = taskNameInput,
                    onValueChange = { viewModel.onTaskNameChange(it) },
                    textStyle = TextStyle(
                        color = PureWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    ),
                    cursorBrush = SolidColor(AcidYellow),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_input_field"),
                    decorationBox = { innerTextField ->
                        if (taskNameInput.isEmpty()) {
                            Text(
                                text = "Type here...",
                                color = DarkGrey,
                                fontSize = 18.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                        innerTextField()
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Time Input Row (Manual Keyboard Type, Hour & Minute)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Hour Box
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(48.dp)
                        .border(1.dp, DarkGrey, RectangleShape)
                        .background(MidGrey)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicTextField(
                        value = hourInput,
                        onValueChange = { input ->
                            viewModel.onHourChange(input)
                            if (input.length >= 2) {
                                minuteFocusRequester.requestFocus()
                            }
                        },
                        textStyle = TextStyle(
                            color = PureWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        ),
                        cursorBrush = SolidColor(AcidYellow),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hour_input_field"),
                        decorationBox = { innerTextField ->
                            if (hourInput.isEmpty()) {
                                Text(
                                    text = "08",
                                    color = DarkGrey,
                                    fontSize = 20.sp,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center
                                )
                            }
                            innerTextField()
                        }
                    )
                }

                Text(
                    text = ":",
                    color = PureWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                // Minute Box
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(48.dp)
                        .border(1.dp, DarkGrey, RectangleShape)
                        .background(MidGrey)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicTextField(
                        value = minuteInput,
                        onValueChange = { input ->
                            viewModel.onMinuteChange(input)
                        },
                        textStyle = TextStyle(
                            color = PureWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        ),
                        cursorBrush = SolidColor(AcidYellow),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                            }
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(minuteFocusRequester)
                            .testTag("minute_input_field"),
                        decorationBox = { innerTextField ->
                            if (minuteInput.isEmpty()) {
                                Text(
                                    text = "30",
                                    color = DarkGrey,
                                    fontSize = 20.sp,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center
                                )
                            }
                            innerTextField()
                        }
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // AM / PM Chips
                BrutalistChip(
                    text = "AM",
                    isSelected = !isPm,
                    onClick = { viewModel.toggleAmPm(false) },
                    modifier = Modifier.width(48.dp)
                )

                BrutalistChip(
                    text = "PM",
                    isSelected = isPm,
                    onClick = { viewModel.toggleAmPm(true) },
                    modifier = Modifier.width(48.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                // Date Chip: TODAY / TOMORROW
                BrutalistChip(
                    text = if (isTomorrow) "TMRW" else "TODAY",
                    isSelected = isTomorrow,
                    onClick = { viewModel.toggleTomorrow(!isTomorrow) },
                    modifier = Modifier.width(70.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Save / Update Button
            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.saveNudge()
                },
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AcidYellow,
                    contentColor = PureBlack
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_nudge_button")
            ) {
                Text(
                    text = if (editingNudgeId != null) "[ UPDATE NUDGE ]" else "[ SAVE NUDGE ]",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 1.sp
                )
            }
        }

        HorizontalDivider(color = MidGrey, thickness = 2.dp)

        // ==========================================
        // BOTTOM SECTION: CHRONOLOGICAL UPCOMING LIST
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "UPCOMING NUDGES",
                color = DarkGrey,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Text(
                text = "[ ${upcomingNudges.size} ]",
                color = PureWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        if (upcomingNudges.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NO PENDING NUDGES",
                        color = PureWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Type a task above and hit save to schedule a screen hijack.",
                        color = DarkGrey,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = upcomingNudges,
                    key = { it.id }
                ) { nudge ->
                    UpcomingNudgeCard(
                        nudge = nudge,
                        isEditing = nudge.id == editingNudgeId,
                        onEdit = { viewModel.startEditing(nudge) },
                        onDelete = { viewModel.deleteNudge(nudge) },
                        onTestTrigger = { viewModel.testTriggerNow(nudge) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun UpcomingNudgeCard(
    nudge: NudgeEntity,
    isEditing: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTestTrigger: () -> Unit
) {
    val timeFormatted = remember(nudge.scheduledTimeMillis) {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        sdf.format(Date(nudge.scheduledTimeMillis))
    }

    val dateFormatted = remember(nudge.scheduledTimeMillis) {
        val sdf = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        sdf.format(Date(nudge.scheduledTimeMillis))
    }

    val borderColor = if (isEditing) AcidYellow else DarkGrey

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RectangleShape)
            .background(MidGrey)
            .clickable(onClick = onEdit)
            .padding(14.dp)
            .testTag("task_item_${nudge.id}")
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
                        text = timeFormatted,
                        color = PureWhite,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = dateFormatted.uppercase(),
                        color = DarkGrey,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    if (nudge.status == "SNOOZED") {
                        Box(
                            modifier = Modifier
                                .border(1.dp, AcidYellow, RectangleShape)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "SNOOZED",
                                color = AcidYellow,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
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

            // Actions: Test Popup & Delete
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Test Trigger button
                Box(
                    modifier = Modifier
                        .border(1.dp, DarkGrey, RectangleShape)
                        .clickable(onClick = onTestTrigger)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("test_trigger_${nudge.id}")
                ) {
                    Text(
                        text = "TEST",
                        color = AcidYellow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Delete Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_nudge_${nudge.id}")
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
}

@Composable
fun BrutalistChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) AcidYellow else DarkGrey
    val backgroundColor = if (isSelected) AcidYellow else PureBlack
    val textColor = if (isSelected) PureBlack else PureWhite

    Box(
        modifier = modifier
            .height(48.dp)
            .border(1.dp, borderColor, RectangleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .testTag("chip_$text"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
