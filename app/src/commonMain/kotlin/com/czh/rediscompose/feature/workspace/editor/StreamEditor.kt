package com.czh.rediscompose.feature.workspace.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.tooling.preview.Preview
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.domain.key.KeyValue
import com.czh.rediscompose.domain.key.StreamEntry
import com.czh.rediscompose.feature.workspace.WorkspaceState
import com.czh.rediscompose.feature.workspace.previewWorkspaceState

/** Read-only stream browser: arbitrary field names, click a row to expand. */
@Composable
internal fun StreamEditor(
    state: WorkspaceState,
    value: KeyValue.StreamValue,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalAppDimens.current
    var query by remember(state.selectedKey) { mutableStateOf("") }
    var expandedId by remember(state.selectedKey) { mutableStateOf<String?>(null) }
    val visibleEntries by remember(value, query) {
        derivedStateOf {
            if (query.isBlank()) {
                value.entries
            } else {
                value.entries.filter { entry ->
                    entry.id.contains(query, ignoreCase = true) ||
                        entry.fields.any { (field, fieldValue) ->
                            field.contains(query, ignoreCase = true) ||
                                fieldValue.contains(query, ignoreCase = true)
                        }
                }
            }
        }
    }
    val groups = value.groupCount

    EditorCard(modifier) {
        CollectionToolbar(
            title = "Stream entries",
            icon = AppIcons.Timeline,
            badge = "${visibleEntries.size} shown" + if (groups != null) " · $groups groups" else "",
            searchQuery = query,
            onSearchChange = { query = it },
            searchPlaceholder = "筛选条目…",
        )
        CollectionHeader(
            cells =
                listOf(
                    CollectionHeaderCell("Entry ID", width = dimens.streamIdWidth),
                    CollectionHeaderCell("Fields", weight = 1f),
                ),
        )
        if (visibleEntries.isEmpty()) {
            CollectionEmpty(
                text = if (value.entries.isEmpty()) "暂无条目" else "没有匹配的条目",
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                items(items = visibleEntries, key = { entry -> entry.id }) { entry ->
                    StreamRow(
                        entry = entry,
                        expanded = expandedId == entry.id,
                        onToggle = { expandedId = if (expandedId == entry.id) null else entry.id },
                    )
                }
            }
        }
        CollectionFooter(
            left = "XRANGE · XINFO GROUPS",
            middle = "仅展示最近 ${value.entries.size} 条",
            dirty = false,
        )
    }
}

@Composable
private fun StreamRow(
    entry: StreamEntry,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(if (hovered || expanded) colors.surface2 else Color.Transparent)
                .hoverable(interaction),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(dimens.collectionRowHeight)
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onToggle,
                    )
                    .pointerHoverIcon(PointerIcon.Hand),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CollectionCell(Modifier.width(dimens.streamIdWidth)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dimens.spaceXs),
                ) {
                    Icon(
                        imageVector = if (expanded) AppIcons.ChevronDown else AppIcons.ChevronRight,
                        contentDescription = null,
                        tint = colors.textQuaternary,
                        modifier = Modifier.size(dimens.iconXs),
                    )
                    Text(
                        text = entry.id,
                        style = typography.monoKeyRow,
                        color = colors.text,
                        maxLines = 1,
                    )
                }
            }
            CollectionCell(Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
                ) {
                    entry.fields.take(2).forEach { (field, fieldValue) ->
                        Text(
                            text = "$field=$fieldValue",
                            style = typography.monoKeyRow,
                            color = colors.textSecondary,
                            maxLines = 1,
                        )
                    }
                    if (entry.fields.size > 2) {
                        Text(
                            text = "+${entry.fields.size - 2}",
                            style = typography.monoMicro,
                            color = colors.textQuaternary,
                        )
                    }
                }
            }
        }
        if (expanded) {
            SelectionContainer {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(colors.codeBackground)
                            .padding(
                                start = dimens.space2Xl,
                                end = dimens.spaceLg,
                                top = dimens.spaceSm,
                                bottom = dimens.spaceSm,
                            ),
                    verticalArrangement = Arrangement.spacedBy(dimens.spaceXs),
                ) {
                    entry.fields.forEach { (field, fieldValue) ->
                        Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm)) {
                            Text(
                                text = field,
                                style = typography.monoKeyRow,
                                color = colors.jsonKey,
                            )
                            Text(
                                text = fieldValue,
                                style = typography.monoKeyRow,
                                color = colors.text,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(widthDp = 820, heightDp = 440)
@Composable
private fun StreamEditorPreview() {
    AppTheme(dark = true) {
        val state = remember { previewWorkspaceState().also { it.selectKey("stream:events:audit") } }
        StreamEditor(
            state = state,
            value =
                KeyValue.StreamValue(
                    entries =
                        listOf(
                            StreamEntry("1731910000000-0", listOf("event" to "login", "actor" to "ada", "ip" to "203.0.113.42")),
                            StreamEntry("1731910060000-0", listOf("event" to "logout", "actor" to "lin", "ip" to "198.51.100.24")),
                        ),
                    groupCount = 2,
                ),
            modifier = Modifier.padding(LocalAppDimens.current.spaceLg),
        )
    }
}
