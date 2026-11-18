package com.czh.rediscompose.feature.workspace.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.component.AppButton
import com.czh.rediscompose.design.component.AppButtonSize
import com.czh.rediscompose.design.component.AppButtonVariant
import com.czh.rediscompose.design.component.AppDialog
import com.czh.rediscompose.design.component.AppSegmented
import com.czh.rediscompose.design.component.AppSegmentedSize
import com.czh.rediscompose.design.component.AppTextField
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.domain.key.KeyType
import com.czh.rediscompose.feature.workspace.NewKeyDraft
import com.czh.rediscompose.feature.workspace.WorkspaceState
import com.czh.rediscompose.feature.workspace.displayColor
import com.czh.rediscompose.feature.workspace.displayLabel
import com.czh.rediscompose.feature.workspace.previewWorkspaceState

private val CREATABLE_TYPES =
    listOf(KeyType.STRING, KeyType.HASH, KeyType.LIST, KeyType.SET, KeyType.ZSET)

/** Create a key of one of five collection/string types, with optional TTL. */
@Composable
internal fun NewKeyDialog(state: WorkspaceState) {
    if (!state.newKeyDialogVisible) return
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(KeyType.STRING) }
    var ttl by remember { mutableStateOf("-1") }
    var stringValue by remember { mutableStateOf("") }
    var hashField by remember { mutableStateOf("") }
    var hashValue by remember { mutableStateOf("") }
    var listValue by remember { mutableStateOf("") }
    var listHead by remember { mutableStateOf(false) }
    var setMember by remember { mutableStateOf("") }
    var zsetMember by remember { mutableStateOf("") }
    var zsetScore by remember { mutableStateOf("0") }

    AppDialog(
        visible = true,
        onDismissRequest = state::dismissNewKeyDialog,
        title = "新建 Key",
        subtitle = "写入 db${state.databaseIndex}",
        width = dimens.dialogWideWidth,
        footer = {
            AppButton(
                text = "取消",
                onClick = state::dismissNewKeyDialog,
                variant = AppButtonVariant.Ghost,
                size = AppButtonSize.Small,
            )
            AppButton(
                text = "创建",
                onClick = {
                    state.createKey(
                        NewKeyDraft(
                            name = name,
                            type = type,
                            ttlSeconds = ttl.trim().toLongOrNull() ?: Long.MIN_VALUE,
                            stringValue = stringValue,
                            hashField = hashField,
                            hashValue = hashValue,
                            listValue = listValue,
                            listHead = listHead,
                            setMember = setMember,
                            zsetMember = zsetMember,
                            zsetScore = zsetScore,
                        ),
                    )
                },
                variant = AppButtonVariant.Primary,
                size = AppButtonSize.Small,
                icon = AppIcons.Add,
                enabled = name.isNotBlank() && !state.isSaving,
                loading = state.isSaving,
            )
        },
    ) {
        Text(text = "类型", style = typography.microLabel, color = colors.textQuaternary)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = dimens.spaceXs + 1.dp),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceXs + 2.dp),
        ) {
            CREATABLE_TYPES.forEach { option ->
                NewKeyTypeCard(
                    type = option,
                    selected = type == option,
                    onClick = { type = option },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        AppTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.padding(top = dimens.spaceMd),
            placeholder = "Key 名，例如 user:1001:profile",
            leadingIcon = AppIcons.Key,
            mono = true,
        )
        AppTextField(
            value = ttl,
            onValueChange = { ttl = it },
            modifier = Modifier.padding(top = dimens.spaceSm),
            placeholder = "TTL 秒数；-1 表示永久",
            leadingIcon = AppIcons.Clock,
            mono = true,
        )
        when (type) {
            KeyType.STRING ->
                AppTextField(
                    value = stringValue,
                    onValueChange = { stringValue = it },
                    modifier = Modifier.padding(top = dimens.spaceSm),
                    placeholder = "字符串值（可以为空）",
                    singleLine = false,
                    mono = true,
                )
            KeyType.HASH -> {
                AppTextField(
                    value = hashField,
                    onValueChange = { hashField = it },
                    modifier = Modifier.padding(top = dimens.spaceSm),
                    placeholder = "Field",
                    leadingIcon = AppIcons.Hash,
                    mono = true,
                )
                AppTextField(
                    value = hashValue,
                    onValueChange = { hashValue = it },
                    modifier = Modifier.padding(top = dimens.spaceSm),
                    placeholder = "Value",
                    mono = true,
                )
            }
            KeyType.LIST -> {
                AppTextField(
                    value = listValue,
                    onValueChange = { listValue = it },
                    modifier = Modifier.padding(top = dimens.spaceSm),
                    placeholder = "第一个成员",
                    leadingIcon = AppIcons.List,
                    mono = true,
                )
                Row(
                    modifier = Modifier.padding(top = dimens.spaceSm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
                ) {
                    Text(text = "插入位置", style = typography.caption, color = colors.textTertiary)
                    AppSegmented(
                        options = listOf(true, false),
                        selected = listHead,
                        onSelect = { listHead = it },
                        size = AppSegmentedSize.Small,
                        label = { head -> if (head) "头部 LPUSH" else "尾部 RPUSH" },
                    )
                }
            }
            KeyType.SET ->
                AppTextField(
                    value = setMember,
                    onValueChange = { setMember = it },
                    modifier = Modifier.padding(top = dimens.spaceSm),
                    placeholder = "第一个成员",
                    leadingIcon = AppIcons.Layers,
                    mono = true,
                )
            KeyType.ZSET -> {
                AppTextField(
                    value = zsetMember,
                    onValueChange = { zsetMember = it },
                    modifier = Modifier.padding(top = dimens.spaceSm),
                    placeholder = "Member",
                    leadingIcon = AppIcons.Sort,
                    mono = true,
                )
                AppTextField(
                    value = zsetScore,
                    onValueChange = { zsetScore = it },
                    modifier = Modifier.padding(top = dimens.spaceSm),
                    placeholder = "Score（数字）",
                    mono = true,
                )
            }
            else -> Unit
        }
        Text(
            text = "Redis 不能创建空集合：Hash / List / Set / ZSet 至少需要一个成员；TTL 为 -1 或正整数。",
            style = typography.monoMicro,
            color = colors.textQuaternary,
            modifier = Modifier.padding(top = dimens.spaceMd),
        )
    }
}

@Composable
private fun NewKeyTypeCard(
    type: KeyType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val typeColor = type.displayColor(colors)
    val shape = RoundedCornerShape(dimens.radiusControlSmall)
    Column(
        modifier =
            modifier
                .height(dimens.newKeyTypeCardHeight)
                .clip(shape)
                .background(if (selected) typeColor.copy(alpha = 0.12f) else colors.surface2)
                .border(
                    width = 1.dp,
                    color = if (selected) typeColor.copy(alpha = 0.40f) else colors.border,
                    shape = shape,
                )
                .clickable(onClick = onClick)
                .padding(dimens.spaceXs + 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = type.icon(),
            contentDescription = null,
            tint = if (selected) typeColor else colors.textTertiary,
            modifier = Modifier.size(dimens.iconLg),
        )
        Text(
            text = type.displayLabel(),
            style = typography.caption,
            color = if (selected) colors.text else colors.textSecondary,
            modifier = Modifier.padding(top = dimens.spaceXs),
        )
    }
}

private fun KeyType.icon(): ImageVector =
    when (this) {
        KeyType.STRING -> AppIcons.Code
        KeyType.HASH -> AppIcons.Hash
        KeyType.LIST -> AppIcons.List
        KeyType.SET -> AppIcons.Layers
        KeyType.ZSET -> AppIcons.Sort
        KeyType.STREAM -> AppIcons.Timeline
        KeyType.NONE -> AppIcons.Info
    }

@Preview(widthDp = 620, heightDp = 700)
@Composable
private fun NewKeyDialogPreview() {
    AppTheme(dark = true) {
        val state = remember { previewWorkspaceState().also { it.openNewKeyDialog() } }
        NewKeyDialog(state = state)
    }
}
