package com.example.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NudgeEntity
import com.example.ui.theme.AcidYellow
import com.example.ui.theme.DarkGrey
import com.example.ui.theme.MidGrey
import com.example.ui.theme.NudgeTheme
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NudgeOverlayContent(
    nudge: NudgeEntity?,
    onDone: () -> Unit,
    onSnoozeMinutes: (Int) -> Unit
) {
    NudgeTheme {
        var isCustomSnoozeSelected by remember { mutableStateOf(false) }
        var customMinutesInput by remember { mutableStateOf("") }
        val focusRequester = remember { FocusRequester() }

        val timeFormatted = remember(nudge?.scheduledTimeMillis) {
            val millis = nudge?.scheduledTimeMillis ?: System.currentTimeMillis()
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            sdf.format(Date(millis))
        }

        val scrollState = rememberScrollState()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PureBlack)
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                // Top Badge
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "/// NUDGE ACTIVE ///",
                        color = DarkGrey,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Big Scheduled Time
                    Text(
                        text = timeFormatted,
                        color = PureWhite,
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        lineHeight = 56.sp,
                        letterSpacing = (-1.5).sp,
                        modifier = Modifier.testTag("overlay_scheduled_time")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Big Task Name
                    Text(
                        text = nudge?.taskName ?: "SCHEDULED NUDGE",
                        color = PureWhite,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        lineHeight = 36.sp,
                        modifier = Modifier.testTag("overlay_task_name")
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Action Area
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(
                        color = MidGrey,
                        thickness = 2.dp,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // Big Acid Yellow Button: [ ✓ DONE ]
                    Button(
                        onClick = onDone,
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AcidYellow,
                            contentColor = PureBlack
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .testTag("overlay_done_button")
                    ) {
                        Text(
                            text = "[ ✓ DONE ]",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Snooze Options
                    Text(
                        text = "SNOOZE NUDGE:",
                        color = DarkGrey,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Snooze Chips: 15m, 30m, 1h, 2h, Custom
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SnoozeChip(
                            label = "15m",
                            isSelected = false,
                            modifier = Modifier.weight(1f),
                            onClick = { onSnoozeMinutes(15) }
                        )
                        SnoozeChip(
                            label = "30m",
                            isSelected = false,
                            modifier = Modifier.weight(1f),
                            onClick = { onSnoozeMinutes(30) }
                        )
                        SnoozeChip(
                            label = "1h",
                            isSelected = false,
                            modifier = Modifier.weight(1f),
                            onClick = { onSnoozeMinutes(60) }
                        )
                        SnoozeChip(
                            label = "2h",
                            isSelected = false,
                            modifier = Modifier.weight(1f),
                            onClick = { onSnoozeMinutes(120) }
                        )
                        SnoozeChip(
                            label = "Custom",
                            isSelected = isCustomSnoozeSelected,
                            modifier = Modifier.weight(1.3f),
                            onClick = {
                                isCustomSnoozeSelected = !isCustomSnoozeSelected
                            }
                        )
                    }

                    // Custom Snooze Input Field
                    if (isCustomSnoozeSelected) {
                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, AcidYellow, RectangleShape)
                                .background(MidGrey)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = customMinutesInput,
                                onValueChange = { input ->
                                    if (input.all { it.isDigit() } && input.length <= 4) {
                                        customMinutesInput = input
                                    }
                                },
                                textStyle = TextStyle(
                                    color = PureWhite,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                cursorBrush = SolidColor(AcidYellow),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        val mins = customMinutesInput.toIntOrNull()
                                        if (mins != null && mins > 0) {
                                            onSnoozeMinutes(mins)
                                        }
                                    }
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester)
                                    .testTag("custom_snooze_input"),
                                decorationBox = { innerTextField ->
                                    if (customMinutesInput.isEmpty()) {
                                        Text(
                                            text = "Minutes (e.g. 45)",
                                            color = DarkGrey,
                                            fontSize = 16.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    innerTextField()
                                }
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    val mins = customMinutesInput.toIntOrNull()
                                    if (mins != null && mins > 0) {
                                        onSnoozeMinutes(mins)
                                    }
                                },
                                shape = RectangleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AcidYellow,
                                    contentColor = PureBlack
                                ),
                                modifier = Modifier.testTag("custom_snooze_confirm")
                            ) {
                                Text(
                                    text = "SNOOZE",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SnoozeChip(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) AcidYellow else DarkGrey
    val backgroundColor = if (isSelected) AcidYellow else PureBlack
    val textColor = if (isSelected) PureBlack else PureWhite

    Box(
        modifier = modifier
            .height(44.dp)
            .border(1.dp, borderColor, RectangleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .testTag("snooze_chip_$label"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )
    }
}
