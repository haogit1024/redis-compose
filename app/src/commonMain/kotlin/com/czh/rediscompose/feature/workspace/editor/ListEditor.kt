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
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.domain.key.KeyValue
import com.czh.rediscompose.feature.workspace.EditableListMember
import com.czh.rediscompose.feature.workspace.WorkspaceState
import com.czh.rediscompose.feature.workspace.previewWorkspaceState

/** Editable list table with LPUSH/RPUSH drafts and index-aware saves. */
@Composable
internal fun ListEditor(
    state: WorkspaceState,
    value: KeyValue.ListValue,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalAppDimens.current
    val rows =
        remember(state.selectedKey, value) {
            mutableStateListOf<EditableListMember>().apply {
                value.items.forEachIndexed { index, item ->
                    add(EditableListMember(index = index.toLong(), value = item))
                }
            }
        }
    var query by remember(state.selectedKey) { mutableStateOf("") }
    var addingHead by remember(state.selectedKey) { mutableStateOf(false) }
    var addingTail by remember(state.selectedKey) { mutableStateOf(false) }
    var draft by remember(state.selectedKey) { mutableStateOf("") }
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
    val hasPendingDraft = (addingHead || addingTail) && draft.isNotEmpty()
    val editable = !state.isReadOnly && !state.isSaving && !state.detailLoading
    LaunchedEffect(dirty || hasPendingDraft) { state.hasUnsavedEdits = dirty || hasPendingDraft }
    DisposableEffect(Unit) {
        onDispose { state.hasUnsavedEdits = false }
    }
    LaunchedEffect(state.saveRequestId) {
        if (state.consumeSaveRequest() && dirty && editable) state.saveListMembers(rows)
    }

    fun resetDraft() {
        addingHead = false
        addingTail = false
        draft = ""
    }

    EditorCard(modifier) {
        CollectionToolbar(
            title = "List members",
            icon = AppIcons.List,
            badge = "${visibleRows.size} items",
            searchQuery = query,
            onSearchChange = { query = it },
            searchPlaceholder = "筛选成员…",
            actions =
                listOf(
                    ToolbarAction(
                        label = "LPUSH",
                        icon = AppIcons.ChevronLeft,
                        onClick = {
                            addingHead = true
                            addingTail = false
                            draft = ""
                        },
                        enabled = editable,
                        primary = false,
                        loading = false,
                    ),
                    ToolbarAction(
                        label = "RPUSH",
                        icon = AppIcons.ChevronRight,
                        onClick = {
                            addingHead = false
                            addingTail = true
                            draft = ""
                        },
                        enabled = editable,
                        primary = false,
                        loading = false,
                    ),
                    ToolbarAction(
                        label = "保存",
                        icon = AppIcons.Save,
                        onClick = { state.saveListMembers(rows) },
                        enabled = dirty && editable,
                        primary = true,
                        loading = state.isSaving,
                    ),
                ),
        )
        CollectionHeader(
            cells =
                listOf(
                    CollectionHeaderCell("Index", width = dimens.collectionIndexWidth, alignEnd = true),
                    CollectionHeaderCell("Value", weight = 1f),
                    CollectionHeaderCell("", width = dimens.collectionActionsWidth),
                ),
        )
        if (visibleRows.isEmpty() && !addingHead && !addingTail) {
            CollectionEmpty(
                text = if (rows.isEmpty()) "暂无成员" else "没有匹配的成员",
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                if (addingHead) {
                    item(key = "draft-head") {
                        ListDraftRow(
                            label = "LPUSH",
                            value = draft,
                            onValueChange = { draft = it },
                            onSave = {
                                state.addListMember(draft, head = true)
                                resetDraft()
                            },
                            onCancel = { resetDraft() },
                            saving = state.isSaving,
                        )
                    }
                }
                items(
                    items = visibleRows,
                    key = { row -> "list:${row.index}:${row.originalValue}" },
                ) { row ->
                    ListRow(
                        row = row,
                        editable = editable,
                        saving = state.isSaving,
                        onSave = { state.saveListMember(row.index, row.value) },
                        onDelete = { state.deleteListMember(row.index, row.originalValue) },
                    )
                }
                if (addingTail) {
                    item(key = "draft-tail") {
                        ListDraftRow(
                            label = "RPUSH",
                            value = draft,
                            onValueChange = { draft = it },
                            onSave = {
                                state.addListMember(draft, head = false)
                                resetDraft()
                            },
                            onCancel = { resetDraft() },
                            saving = state.isSaving,
                        )
                    }
                }
            }
        }
        CollectionFooter(
            left = "LPUSH · RPUSH · LSET",
            middle = "删除同值时移除首个匹配项",
            dirty = dirty,
        )
    }
}

@Composable
private fun ListRow(
    row: EditableListMember,
    editable: Boolean,
    saving: Boolean,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
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
        CollectionCell(Modifier.width(dimens.collectionIndexWidth)) {
            Text(
                text = row.index.toString(),
                style = typography.collectionIndex,
                color = colors.textQuaternary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
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

@Composable
private fun ListDraftRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    saving: Boolean,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(dimens.collectionRowHeight)
                .background(colors.accentSofter)
                .drawBehind {
                    drawRect(
                        color = colors.accentLine,
                        topLeft = Offset(0f, size.height - 1.dp.toPx()),
                        size = Size(size.width, 1.dp.toPx()),
                    )
                },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CollectionCell(Modifier.width(dimens.collectionIndexWidth)) {
            Text(
                text = label,
                style = typography.collectionIndex,
                color = colors.accent,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
            )
        }
        CollectionCell(Modifier.weight(1f)) {
            CellTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = !saving,
                placeholder = "输入新成员…",
            )
        }
        CollectionCell(Modifier.width(dimens.collectionActionsWidth)) {
            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                RowActionButton(
                    icon = AppIcons.Check,
                    contentDescription = "插入成员",
                    onClick = onSave,
                    enabled = !saving,
                )
                RowActionButton(
                    icon = AppIcons.Close,
                    contentDescription = "取消",
                    onClick = onCancel,
                    enabled = !saving,
                )
            }
        }
    }
}

@Preview(widthDp = 820, heightDp = 420)
@Composable
private fun ListEditorPreview() {
    AppTheme(dark = true) {
        val state = remember { previewWorkspaceState().also { it.selectKey("queue:emails") } }
        ListEditor(
            state = state,
            value =
                KeyValue.ListValue(
                    items =
                        listOf(
                            """{"to":"ada@acme.io","template":"welcome"}""",
                            """{"to":"lin@acme.io","template":"invoice_ready"}""",
                            """{"to":"ops@acme.io","template":"daily_digest"}""",
                        ),
                ),
            modifier = Modifier.padding(LocalAppDimens.current.spaceLg),
        )
    }
}
