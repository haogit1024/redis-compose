package com.czh.rediscompose.feature.workspace.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.domain.key.KeyValue
import com.czh.rediscompose.feature.workspace.EditableSetMember
import com.czh.rediscompose.feature.workspace.WorkspaceState
import com.czh.rediscompose.feature.workspace.previewWorkspaceState

/** Editable set table; renaming is SADD new + SREM old. */
@Composable
internal fun SetEditor(
    state: WorkspaceState,
    value: KeyValue.SetValue,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalAppDimens.current
    val rows =
        remember(state.selectedKey, value) {
            mutableStateListOf<EditableSetMember>().apply {
                addAll(value.members.map { EditableSetMember(original = it, value = it) })
            }
        }
    var query by remember(state.selectedKey) { mutableStateOf("") }
    val visibleRows by remember(rows, query) {
        derivedStateOf {
            if (query.isBlank()) {
                rows.toList()
            } else {
                rows.filter { it.value.contains(query, ignoreCase = true) }
            }
        }
    }
    val dirty by remember(rows) { derivedStateOf { rows.any { it.isDirty } } }
    val editable = !state.isReadOnly && !state.isSaving && !state.detailLoading
    LaunchedEffect(dirty) { state.hasUnsavedEdits = dirty }
    DisposableEffect(Unit) {
        onDispose { state.hasUnsavedEdits = false }
    }
    LaunchedEffect(state.saveRequestId) {
        if (state.consumeSaveRequest() && dirty && editable) state.saveSetMembers(rows)
    }

    EditorCard(modifier) {
        CollectionToolbar(
            title = "Set members",
            icon = AppIcons.Layers,
            badge = "${visibleRows.size} unique",
            searchQuery = query,
            onSearchChange = { query = it },
            searchPlaceholder = "筛选成员…",
            actions =
                listOf(
                    ToolbarAction(
                        label = "添加成员",
                        icon = AppIcons.Add,
                        onClick = {
                            query = ""
                            rows.add(EditableSetMember(original = null, value = ""))
                        },
                        enabled = editable,
                        primary = false,
                        loading = false,
                    ),
                    ToolbarAction(
                        label = "保存",
                        icon = AppIcons.Save,
                        onClick = { state.saveSetMembers(rows) },
                        enabled = dirty && editable,
                        primary = true,
                        loading = state.isSaving,
                    ),
                ),
        )
        CollectionHeader(
            cells =
                listOf(
                    CollectionHeaderCell("Member", weight = 1f),
                    CollectionHeaderCell("", width = dimens.collectionActionsWidth),
                ),
        )
        if (visibleRows.isEmpty()) {
            CollectionEmpty(
                text = if (rows.isEmpty()) "暂无成员" else "没有匹配的成员",
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                items(
                    items = visibleRows,
                    key = { row -> "set:${row.original ?: "new"}:${row.value}" },
                ) { row ->
                    SetRow(
                        row = row,
                        editable = editable,
                        saving = state.isSaving,
                        onSave = { state.saveSetMember(row) },
                        onDelete = {
                            if (row.isNew) {
                                rows.remove(row)
                            } else {
                                state.deleteSetMember(row)
                            }
                        },
                    )
                }
            }
        }
        CollectionFooter(
            left = "SADD · SREM",
            middle = "${rows.size} members",
            dirty = dirty,
        )
    }
}

@Composable
private fun SetRow(
    row: EditableSetMember,
    editable: Boolean,
    saving: Boolean,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(dimens.collectionRowHeight)
                .background(if (hovered) colors.surface2 else Color.Transparent)
                .drawBehind {
                    drawRect(
                        color = colors.border,
                        topLeft = Offset(0f, size.height - 1.dp.toPx()),
                        size = Size(size.width, 1.dp.toPx()),
                    )
                }
                .hoverable(interaction),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CollectionCell(Modifier.weight(1f)) {
            CellTextField(
                value = row.value,
                onValueChange = { row.value = it },
                enabled = editable,
                placeholder = "member",
            )
        }
        CollectionCell(Modifier.width(dimens.collectionActionsWidth)) {
            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                RowActionButton(
                    icon = AppIcons.Check,
                    contentDescription = "保存成员",
                    onClick = onSave,
                    visible = hovered || row.isDirty,
                    enabled = editable && !saving && row.isDirty,
                )
                RowActionButton(
                    icon = AppIcons.Delete,
                    contentDescription = "删除成员",
                    onClick = onDelete,
                    visible = hovered || row.isDirty,
                    enabled = editable && !saving,
                    danger = true,
                )
            }
        }
    }
}

@Preview(widthDp = 820, heightDp = 380)
@Composable
private fun SetEditorPreview() {
    AppTheme(dark = true) {
        val state = remember { previewWorkspaceState().also { it.selectKey("tags:article:204") } }
        SetEditor(
            state = state,
            value = KeyValue.SetValue(members = listOf("redis", "compose", "desktop", "kotlin")),
            modifier = Modifier.padding(LocalAppDimens.current.spaceLg),
        )
    }
}
