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
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.czh.rediscompose.domain.key.HashField
import com.czh.rediscompose.domain.key.KeyValue
import com.czh.rediscompose.feature.workspace.EditableHashField
import com.czh.rediscompose.feature.workspace.WorkspaceState
import com.czh.rediscompose.feature.workspace.previewWorkspaceState

/** Editable hash table; new rows stay local until the user saves. */
@Composable
internal fun HashEditor(
    state: WorkspaceState,
    value: KeyValue.HashValue,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalAppDimens.current
    val rows =
        remember(state.selectedKey, value) {
            mutableStateListOf<EditableHashField>().apply {
                addAll(value.fields.map { toHashRow(it) })
            }
        }
    var query by remember(state.selectedKey) { mutableStateOf("") }
    val visibleRows by remember(rows, query) {
        derivedStateOf {
            if (query.isBlank()) {
                rows.toList()
            } else {
                rows.filter {
                    it.field.contains(query, ignoreCase = true) ||
                        it.value.contains(query, ignoreCase = true)
                }
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
        if (state.consumeSaveRequest() && dirty && editable) state.saveHashFields(rows)
    }

    EditorCard(modifier) {
        CollectionToolbar(
            title = "Hash fields",
            icon = AppIcons.Hash,
            badge = "${visibleRows.size} fields",
            searchQuery = query,
            onSearchChange = { query = it },
            searchPlaceholder = "筛选字段…",
            actions =
                listOf(
                    ToolbarAction(
                        label = "新增字段",
                        icon = AppIcons.Add,
                        onClick = {
                            query = ""
                            rows.add(EditableHashField(originalField = null, field = "", value = ""))
                        },
                        enabled = editable,
                        primary = false,
                        loading = false,
                    ),
                    ToolbarAction(
                        label = "保存",
                        icon = AppIcons.Save,
                        onClick = { state.saveHashFields(rows) },
                        enabled = dirty && editable,
                        primary = true,
                        loading = state.isSaving,
                    ),
                ),
        )
        CollectionHeader(
            cells =
                listOf(
                    CollectionHeaderCell("Field", weight = 0.32f),
                    CollectionHeaderCell("Value", weight = 1f),
                    CollectionHeaderCell("", width = dimens.collectionActionsWidth),
                ),
        )
        if (visibleRows.isEmpty()) {
            CollectionEmpty(
                text = if (rows.isEmpty()) "暂无字段" else "没有匹配的字段",
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                itemsIndexed(
                    items = visibleRows,
                    key = { index, row -> row.originalField ?: "new:$index" },
                ) { _, row ->
                    HashRow(
                        row = row,
                        editable = editable,
                        saving = state.isSaving,
                        onSave = { state.saveHashField(row) },
                        onDelete = {
                            if (row.isNew) {
                                rows.remove(row)
                            } else {
                                state.deleteHashField(row)
                            }
                        },
                    )
                }
            }
        }
        CollectionFooter(
            left = "HSET · HDEL",
            middle = "${rows.size} fields",
            dirty = dirty,
        )
    }
}

@Composable
private fun HashRow(
    row: EditableHashField,
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
        CollectionCell(Modifier.weight(0.32f)) {
            CellTextField(
                value = row.field,
                onValueChange = { row.field = it },
                enabled = editable,
                placeholder = "field",
            )
        }
        CollectionCell(Modifier.weight(1f)) {
            CellTextField(
                value = row.value,
                onValueChange = { row.value = it },
                enabled = editable,
                placeholder = "value",
            )
        }
        CollectionCell(Modifier.width(dimens.collectionActionsWidth)) {
            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                RowActionButton(
                    icon = AppIcons.Check,
                    contentDescription = "保存字段",
                    onClick = onSave,
                    visible = hovered || row.isDirty,
                    enabled = editable && !saving && row.isDirty && row.field.isNotBlank(),
                )
                RowActionButton(
                    icon = AppIcons.Delete,
                    contentDescription = "删除字段",
                    onClick = onDelete,
                    visible = hovered || row.isDirty,
                    enabled = editable && !saving,
                    danger = true,
                )
            }
        }
    }
}

private fun toHashRow(field: HashField): EditableHashField =
    EditableHashField(
        originalField = field.field,
        field = field.field,
        value = field.value,
        originalValue = field.value,
    )

@Preview(widthDp = 820, heightDp = 420)
@Composable
private fun HashEditorPreview() {
    AppTheme(dark = true) {
        val state =
            remember {
                previewWorkspaceState().also { it.selectKey("cache:user:1001:profile") }
            }
        HashEditor(
            state = state,
            value =
                KeyValue.HashValue(
                    fields =
                        listOf(
                            HashField("name", "Ada Lovelace"),
                            HashField("email", "ada@acme.io"),
                            HashField("plan", "scale"),
                        ),
                ),
            modifier = Modifier.padding(LocalAppDimens.current.spaceLg),
        )
    }
}
