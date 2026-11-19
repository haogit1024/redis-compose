package com.czh.rediscompose.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.component.AppTextField
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography

@Composable
internal fun SettingsSectionHeader(section: SettingsSection) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val dimens = LocalAppDimens.current
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = section.title,
            style = typography.settingsSectionTitle,
            color = colors.text,
        )
        Text(
            text = section.description,
            style = typography.settingsSectionDesc,
            color = colors.textTertiary,
            modifier = Modifier.padding(top = 2.dp),
        )
        Box(Modifier.padding(top = dimens.settings.sectionHeadGap))
    }
}

@Composable
internal fun SettingsPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val shape = RoundedCornerShape(dimens.settings.panelRadius)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.surface)
                .border(1.dp, colors.border, shape),
        content = content,
    )
}

@Composable
internal fun SettingsDivider() {
    val colors = LocalAppColors.current
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(colors.border),
    )
}

@Composable
internal fun SettingRow(
    label: String,
    hint: String,
    last: Boolean = false,
    modifier: Modifier = Modifier,
    control: @Composable RowScope.() -> Unit,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .drawBehind {
                    if (!last) {
                        drawRect(
                            color = colors.border,
                            topLeft = Offset(0f, size.height - 1.dp.toPx()),
                            size = Size(size.width, 1.dp.toPx()),
                        )
                    }
                }
                .defaultMinSize(minHeight = dimens.settings.rowMinHeight)
                .padding(horizontal = dimens.settings.rowPaddingH, vertical = dimens.settings.rowPaddingV),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.settings.rowGap),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = typography.settingsLabel,
                color = colors.text,
            )
            Text(
                text = hint,
                style = typography.settingsHint,
                color = colors.textTertiary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.settings.controlGap),
            content = control,
        )
    }
}

/**
 * Numeric setting field that keeps a text draft while the user types.
 *
 * Invalid/partial values stay in the field without touching settings; leaving the field
 * (or a valid in-range value) commits a clamped number.
 */
@Composable
internal fun SettingsNumberField(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 90.dp,
) {
    val dimens = LocalAppDimens.current
    var focused by remember { mutableStateOf(false) }
    var draft by remember(value) { mutableStateOf(value.toString()) }

    LaunchedEffect(value, focused) {
        if (!focused) draft = value.toString()
    }

    AppTextField(
        value = draft,
        onValueChange = { input ->
            val digits = input.filter { it.isDigit() }.take(6)
            draft = digits
            digits.toIntOrNull()?.let { parsed ->
                if (parsed in range) onValueChange(parsed)
            }
        },
        modifier = modifier.width(width),
        mono = true,
        height = dimens.controlHeightSmall,
        keyboardType = KeyboardType.Number,
        onFocusChanged = { isFocused ->
            focused = isFocused
            if (!isFocused) {
                val committed = (draft.toIntOrNull() ?: value).coerceIn(range)
                draft = committed.toString()
                if (committed != value) onValueChange(committed)
            }
        },
    )
}

@Composable
internal fun SettingsStepper(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val shape = RoundedCornerShape(dimens.settings.navItemRadius)
    Row(
        modifier =
            modifier
                .height(dimens.settings.stepperButtonWidth)
                .background(colors.surface2, shape)
                .border(1.dp, colors.border, shape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton(
            icon = AppIcons.Remove,
            contentDescription = "减小字号",
            enabled = value > range.first,
            onClick = { onValueChange((value - 1).coerceIn(range)) },
        )
        Box(
            modifier =
                Modifier
                    .width(dimens.settings.stepperValueWidth)
                    .height(dimens.settings.stepperButtonWidth)
                    .drawBehind {
                        drawRect(
                            color = colors.border,
                            topLeft = Offset.Zero,
                            size = Size(1.dp.toPx(), size.height),
                        )
                        drawRect(
                            color = colors.border,
                            topLeft = Offset(size.width - 1.dp.toPx(), 0f),
                            size = Size(1.dp.toPx(), size.height),
                        )
                    },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = value.toString(),
                style = typography.monoValue,
                color = colors.text,
            )
        }
        StepperButton(
            icon = AppIcons.Add,
            contentDescription = "增大字号",
            enabled = value < range.last,
            onClick = { onValueChange((value + 1).coerceIn(range)) },
        )
    }
}

@Composable
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Box(
        modifier =
            Modifier
                .width(dimens.settings.stepperButtonWidth)
                .height(dimens.settings.stepperButtonWidth)
                .background(if (hovered && enabled) colors.surface3 else Color.Transparent)
                .hoverable(interaction)
                .clickable(enabled = enabled, onClick = onClick)
                .then(if (enabled) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint =
                when {
                    !enabled -> colors.textQuaternary
                    hovered -> colors.text
                    else -> colors.textTertiary
                },
            modifier = Modifier.size(dimens.iconXs),
        )
    }
}

@Composable
internal fun SettingsStaticValue(text: String) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val shape = RoundedCornerShape(dimens.radiusControlSmall)
    Box(
        modifier =
            Modifier
                .height(dimens.controlHeightSmall)
                .background(colors.surface2, shape)
                .border(1.dp, colors.border, shape)
                .padding(horizontal = dimens.fieldPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = typography.monoValue,
            color = colors.textSecondary,
            maxLines = 1,
        )
    }
}

@Preview(widthDp = 720, heightDp = 320)
@Composable
private fun SettingsControlsPreview() {
    AppTheme(dark = true) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            SettingsSectionHeader(SettingsSection.EDITOR)
            SettingsPanel {
                SettingRow("字号", "值编辑器的代码字号") {
                    SettingsStepper(value = 13, range = 11..18, onValueChange = {})
                }
                SettingRow("大值截断阈值", "超过该大小时仅展示前段（KB）", last = true) {
                    SettingsNumberField(value = 10, range = 1..1024, onValueChange = {})
                }
            }
        }
    }
}

@Preview(widthDp = 720, heightDp = 320)
@Composable
private fun SettingsControlsLightPreview() {
    AppTheme(dark = false) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            SettingsSectionHeader(SettingsSection.BROWSER)
            SettingsPanel {
                SettingRow("SCAN 批量大小", "单次 SCAN 的 COUNT 值", last = true) {
                    SettingsNumberField(value = 1000, range = 100..10_000, onValueChange = {}, width = 96.dp)
                }
            }
        }
    }
}
