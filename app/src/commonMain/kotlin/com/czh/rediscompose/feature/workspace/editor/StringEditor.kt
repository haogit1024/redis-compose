package com.czh.rediscompose.feature.workspace.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.data.redis.ValueCodec
import com.czh.rediscompose.design.component.AppBadge
import com.czh.rediscompose.design.component.AppBadgeTone
import com.czh.rediscompose.design.component.AppButton
import com.czh.rediscompose.design.component.AppButtonSize
import com.czh.rediscompose.design.component.AppButtonVariant
import com.czh.rediscompose.design.component.AppSegmented
import com.czh.rediscompose.design.component.AppSegmentedSize
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.domain.key.KeyValue
import com.czh.rediscompose.domain.settings.ValueFormat
import com.czh.rediscompose.feature.workspace.WorkspaceState
import com.czh.rediscompose.feature.workspace.previewWorkspaceState
import com.czh.rediscompose.util.formatBytes
import com.czh.rediscompose.util.truncateUtf8

private val FORMAT_OPTIONS = listOf(ValueFormat.AUTO, ValueFormat.JSON, ValueFormat.TEXT, ValueFormat.HEX)

/** String/JSON editor with format switch, truncation guard and save semantics. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StringEditor(
    state: WorkspaceState,
    value: KeyValue.StringValue,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val clipboard = LocalClipboardManager.current
    val savedText = value.text
    var text by remember(state.selectedKey, savedText) { mutableStateOf(savedText) }
    var format by remember(state.selectedKey, value.isBinary) {
        mutableStateOf(if (value.isBinary) ValueFormat.HEX else ValueFormat.AUTO)
    }
    var showFull by remember(state.selectedKey) { mutableStateOf(false) }

    val limitBytes = (state.truncateValueKb.coerceAtLeast(1)) * 1024
    val limitKb = (limitBytes / 1024)
    val truncated = !value.isBinary && !showFull && value.byteLength > limitBytes
    val isHex = value.isBinary || format == ValueFormat.HEX
    val isDirty = text != savedText
    val isJson = ValueCodec.isJson(text)
    val highlighted = !isHex && (format == ValueFormat.AUTO || format == ValueFormat.JSON) && isJson
    val displayText = if (truncated) truncateUtf8(text, limitBytes) else text
    val hexText =
        remember(text, value.isBinary, displayText, truncated) {
            when {
                value.isBinary -> value.text
                truncated -> ValueCodec.hex(displayText.encodeToByteArray())
                else -> ValueCodec.hex(text.encodeToByteArray())
            }
        }
    val editorText = if (isHex) hexText else displayText
    val readOnly = state.isReadOnly || value.isBinary || truncated || state.detailLoading || state.isSaving
    val canSave = isDirty && !readOnly && !isHex && state.canWrite
    val formatLabel = if (value.isBinary) "HEX" else "UTF-8"
    val byteSize =
        if (isDirty) {
            text.encodeToByteArray().size.toLong()
        } else {
            value.byteLength
        }

    LaunchedEffect(isDirty) { state.hasUnsavedEdits = isDirty }
    DisposableEffect(Unit) {
        onDispose { state.hasUnsavedEdits = false }
    }
    LaunchedEffect(state.saveRequestId) {
        if (state.consumeSaveRequest() && canSave) state.saveString(text)
    }

    EditorCard(modifier) {
        FlowRow(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(colors.surface2.copy(alpha = 0.70f))
                    .padding(
                        horizontal = dimens.editorToolbarPaddingH,
                        vertical = dimens.editorToolbarPaddingV,
                    ),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm - 1.dp),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceXs + 1.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimens.iconTextGap),
            ) {
                Icon(
                    imageVector = AppIcons.Code,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(dimens.treeFolderIconSize),
                )
                Text(text = "Value", style = typography.bodyStrong, color = colors.text)
                if (isJson && !isHex && (format == ValueFormat.AUTO || format == ValueFormat.JSON)) {
                    AppBadge(text = "JSON detected", tone = AppBadgeTone.Success)
                }
                if (value.isBinary) {
                    AppBadge(text = "BINARY · HEX", tone = AppBadgeTone.Warning)
                }
            }
            AppSegmented(
                options = FORMAT_OPTIONS,
                selected = format,
                onSelect = { selected -> if (!value.isBinary) format = selected },
                size = AppSegmentedSize.Small,
                enabled = !value.isBinary,
                label = { formatName(it) },
            )
            if (!isHex && (isJson || format == ValueFormat.JSON)) {
                AppButton(
                    text = "格式化",
                    onClick = {
                        val pretty = ValueCodec.prettyJson(text)
                        if (pretty != null) {
                            text = pretty
                        } else {
                            state.reportInvalidJson()
                        }
                    },
                    variant = AppButtonVariant.Ghost,
                    size = AppButtonSize.Small,
                    icon = AppIcons.Sparkles,
                    enabled = !state.isReadOnly && !truncated,
                )
            }
            if (value.isBinary) {
                AppButton(
                    text = "复制 HEX",
                    onClick = {
                        clipboard.setText(AnnotatedString(value.text))
                        state.reportValueCopied()
                    },
                    variant = AppButtonVariant.Ghost,
                    size = AppButtonSize.Small,
                    icon = AppIcons.Copy,
                )
            }
            if (!isHex) {
                AppButton(
                    text = "保存",
                    onClick = { state.saveString(text) },
                    variant = AppButtonVariant.Primary,
                    size = AppButtonSize.Small,
                    icon = AppIcons.Save,
                    enabled = canSave && !state.isSaving,
                    loading = state.isSaving && isDirty,
                )
            }
        }
        if (truncated) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(colors.warning.copy(alpha = 0.10f))
                        .padding(horizontal = dimens.editorToolbarPaddingH, vertical = dimens.spaceXs + 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
            ) {
                Icon(
                    imageVector = AppIcons.Warning,
                    contentDescription = null,
                    tint = colors.warning,
                    modifier = Modifier.size(dimens.iconSm),
                )
                Text(
                    text = "已截断：仅显示前 $limitKb KB（共 ${formatBytes(value.byteLength)}）",
                    style = typography.caption,
                    color = colors.warning,
                    modifier = Modifier.weight(1f),
                )
                AppButton(
                    text = "复制完整值",
                    onClick = {
                        clipboard.setText(AnnotatedString(value.text))
                        state.reportValueCopied()
                    },
                    variant = AppButtonVariant.Ghost,
                    size = AppButtonSize.Small,
                    icon = AppIcons.Copy,
                )
                AppButton(
                    text = "仍然编辑",
                    onClick = { showFull = true },
                    variant = AppButtonVariant.Secondary,
                    size = AppButtonSize.Small,
                    icon = AppIcons.Edit,
                )
            }
        }
        CodeEditor(
            value = editorText,
            onValueChange = { updated -> if (!isHex && !truncated) text = updated },
            highlighted = highlighted,
            readOnly = readOnly,
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
        )
        CollectionFooter(
            left = if (isHex) "HEX · LF" else "$formatLabel · LF",
            middle = formatBytes(byteSize),
            dirty = isDirty,
        )
    }
}

private fun formatName(format: ValueFormat): String =
    when (format) {
        ValueFormat.AUTO -> "Auto"
        ValueFormat.JSON -> "JSON"
        ValueFormat.TEXT -> "TEXT"
        ValueFormat.HEX -> "HEX"
    }

@Preview(widthDp = 820, heightDp = 520)
@Composable
private fun StringEditorPreview() {
    AppTheme(dark = true) {
        val state = remember { previewWorkspaceState().also { it.selectKey("cache:user:1001:profile") } }
        StringEditor(
            state = state,
            value =
                KeyValue.StringValue(
                    text = """{"theme":"dark","locale":"zh-CN","notifications":{"email":true}}""",
                    isJson = true,
                ),
            modifier = Modifier.padding(LocalAppDimens.current.spaceLg),
        )
    }
}
