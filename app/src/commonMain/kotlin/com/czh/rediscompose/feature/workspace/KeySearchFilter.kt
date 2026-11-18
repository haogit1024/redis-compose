package com.czh.rediscompose.feature.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.component.AppIconButton
import com.czh.rediscompose.design.component.AppIconButtonSize
import com.czh.rediscompose.design.component.AppTextField
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.domain.key.KeyType

private val TYPE_FILTERS: List<KeyType?> =
    listOf(null, KeyType.STRING, KeyType.HASH, KeyType.LIST, KeyType.SET, KeyType.ZSET, KeyType.STREAM)

/** SCAN pattern input plus client-side type filter chips. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun KeySearchFilter(
    state: WorkspaceState,
    focusRequester: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalAppDimens.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dimens.spaceSm),
    ) {
        AppTextField(
            value = state.searchInput,
            onValueChange = { value -> state.searchInput = value },
            modifier =
                Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { onFocusChanged(it.isFocused) }
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (event.key) {
                            Key.Enter, Key.NumPadEnter -> {
                                state.applySearch(state.searchInput)
                                true
                            }
                            Key.Escape -> {
                                if (state.searchInput.isNotEmpty()) {
                                    state.clearSearch()
                                    true
                                } else {
                                    false
                                }
                            }
                            else -> false
                        }
                    },
            placeholder = "匹配模式  user:*  ·  ⌘F",
            leadingIcon = AppIcons.Search,
            trailingContent =
                if (state.searchInput.isNotEmpty()) {
                    @Composable {
                        AppIconButton(
                            icon = AppIcons.Close,
                            contentDescription = "清除搜索",
                            onClick = state::clearSearch,
                            size = AppIconButtonSize.Small,
                        )
                    }
                } else {
                    null
                },
            mono = true,
            height = dimens.keySearchHeight,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceXs - 1.dp),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceXs - 1.dp),
        ) {
            TYPE_FILTERS.forEach { type ->
                TypeFilterChip(
                    type = type,
                    active = state.typeFilter == type,
                    onClick = { state.updateTypeFilter(type) },
                )
            }
        }
    }
}

@Composable
private fun TypeFilterChip(
    type: KeyType?,
    active: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val typeColor = type?.displayColor(colors)
    val shape = RoundedCornerShape(dimens.radiusChip)
    val background =
        when {
            active && typeColor != null -> typeColor.copy(alpha = 0.12f)
            active -> colors.surface3
            hovered -> colors.surface2
            else -> Color.Transparent
        }
    val borderColor =
        when {
            active && typeColor != null -> typeColor.copy(alpha = 0.24f)
            active -> colors.border
            else -> Color.Transparent
        }
    val textColor =
        when {
            active && typeColor != null -> typeColor
            active -> colors.text
            hovered -> colors.textSecondary
            else -> colors.textTertiary
        }

    Text(
        text = type?.displayLabel() ?: "全部",
        style = typography.filterChip,
        color = textColor,
        maxLines = 1,
        modifier =
            Modifier
                .height(dimens.filterChipHeight)
                .clip(shape)
                .background(background)
                .border(1.dp, borderColor, shape)
                .hoverable(interaction)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick,
                )
                .pointerHoverIcon(PointerIcon.Hand)
                .padding(horizontal = dimens.filterChipPadding),
    )
}

@Preview(widthDp = 320)
@Composable
private fun KeySearchFilterPreview() {
    AppTheme(dark = true) {
        val dimens = LocalAppDimens.current
        val state = remember { previewWorkspaceState().also { it.searchInput = "user:*" } }
        KeySearchFilter(
            state = state,
            focusRequester = remember { FocusRequester() },
            onFocusChanged = {},
            modifier = Modifier.padding(dimens.spaceMd),
        )
    }
}

@Preview(widthDp = 320)
@Composable
private fun KeySearchFilterLightPreview() {
    AppTheme(dark = false) {
        val dimens = LocalAppDimens.current
        val state = remember { previewWorkspaceState().also { it.updateTypeFilter(KeyType.HASH) } }
        KeySearchFilter(
            state = state,
            focusRequester = remember { FocusRequester() },
            onFocusChanged = {},
            modifier = Modifier.padding(dimens.spaceMd),
        )
    }
}
