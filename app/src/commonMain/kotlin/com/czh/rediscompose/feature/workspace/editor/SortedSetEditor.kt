package com.czh.rediscompose.feature.workspace.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.domain.key.KeyValue
import com.czh.rediscompose.feature.workspace.EditableSortedSetEntry
import com.czh.rediscompose.feature.workspace.WorkspaceState
import com.czh.rediscompose.feature.workspace.previewWorkspaceState

/** Editable sorted set; displayed from highest score to lowest. */
@Composable
internal fun SortedSetEditor(
    state: WorkspaceState,
    value: KeyValue.SortedSetValue,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalAppDimens.current
    val rows =
        remember(state.selectedKey, value) {
            mutableStateListOf<EditableSortedSetEntry>().apply {
                value.entries.forEach { entry ->
                    add(
                        EditableSortedSetEntry(
                            originalMember = entry.member,
                            member = entry.member,
                            score = formatScore(entry.score),
                        ),
                    )
                }
            }
        }
    var query by remember(state.selectedKey) { mutableStateOf("") }
    val visibleRows by remember(rows, query) {
        derivedStateOf {
            if (query.isBlank()) {
                rows.toList()
            } else {
                rows.filter { it.member.contains(query, ignoreCase = true) }
            }
        }
    }
    val dirty by remember(rows) { derivedStateOf { rows.any { it.isDirty } } }
    val maxScore by remember(rows) {
        derivedStateOf { rows.mapNotNull { it.score.toDoubleOrNull() }.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0 }
    }
    val editable = !state.isReadOnly && !state.isSaving && !state.detailLoading
    LaunchedEffect(dirty) { state.hasUnsavedEdits = dirty }
    DisposableEffect(Unit) {
        onDispose { state.hasUnsavedEdits = false }
    }
    LaunchedEffect(state.saveRequestId) {
        if (state.consumeSaveRequest() && dirty && editable) state.saveSortedSetEntries(rows)
    }

    EditorCard(modifier) {
        CollectionToolbar(
            title = "Sorted set",
            icon = AppIcons.Sort,
            badge = "${visibleRows.size} members",
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
                            rows.add(
                                EditableSortedSetEntry(
                                    originalMember = null,
                                    member = "",
                                    score = "0",
                                ),
                            )
                        },
                        enabled = editable,
                        primary = false,
                        loading = false,
                    ),
                    ToolbarAction(
                        label = "保存",
                        icon = AppIcons.Save,
                        onClick = { state.saveSortedSetEntries(rows) },
                        enabled = dirty && editable,
                        primary = true,
                        loading = state.isSaving,
                    ),
                ),
        )
        CollectionHeader(
            cells =
                listOf(
                    CollectionHeaderCell("#", width = dimens.collectionRankWidth),
                    CollectionHeaderCell("Member", weight = 1f),
                    CollectionHeaderCell("Score", width = dimens.collectionScoreWidth, alignEnd = true),
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
                itemsIndexed(
                    items = visibleRows,
                    key = { index, row -> row.originalMember ?: "new:$index" },
                ) { index, row ->
                    SortedSetRow(
                        rank = index + 1,
                        row = row,
                        maxScore = maxScore,
                        editable = editable,
                        saving = state.isSaving,
                        onSave = { state.saveSortedSetEntry(row) },
                        onDelete = {
                            if (row.isNew) {
                                rows.remove(row)
                            } else {
                                state.deleteSortedSetEntry(row)
                            }
                        },
                    )
                }
            }
        }
        CollectionFooter(
            left = "ZADD · ZREM",
            middle = "从高到低排序",
            dirty = dirty,
        )
    }
}

@Composable
private fun SortedSetRow(
    rank: Int,
    row: EditableSortedSetEntry,
    maxScore: Double,
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
    val scoreValue = row.score.toDoubleOrNull()
    val invalidScore = scoreValue == null
    val fraction = ((scoreValue ?: 0.0) / maxScore).coerceIn(0.0, 1.0).toFloat()

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
        CollectionCell(Modifier.width(dimens.collectionRankWidth)) {
            Text(
                text = rank.toString(),
                style = typography.collectionIndex,
                color = colors.textQuaternary,
            )
        }
        CollectionCell(Modifier.weight(1f)) {
            CellTextField(
                value = row.member,
                onValueChange = { row.member = it },
                enabled = editable,
                placeholder = "member",
            )
        }
        CollectionCell(Modifier.width(dimens.collectionScoreWidth)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                Box(
                    Modifier
                        .padding(end = dimens.spaceXs + 2.dp)
                        .width((dimens.scoreBarMaxWidth * fraction).coerceAtLeast(dimens.spaceSm))
                        .height(dimens.scoreBarHeight)
                        .background(colors.typeZset.copy(alpha = 0.60f), RoundedCornerShape(dimens.scoreBarHeight)),
                )
                CellTextField(
                    value = row.score,
                    onValueChange = { row.score = it },
                    enabled = editable,
                    alignEnd = true,
                    isError = invalidScore,
                )
            }
        }
        CollectionCell(Modifier.width(dimens.collectionActionsWidth)) {
            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                RowActionButton(
                    icon = AppIcons.Check,
                    contentDescription = "保存成员",
                    onClick = onSave,
                    visible = hovered || row.isDirty,
                    enabled = editable && !saving && row.isDirty && !invalidScore && row.member.isNotBlank(),
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

private fun formatScore(score: Double): String = if (score % 1.0 == 0.0) score.toLong().toString() else score.toString()

@Preview(widthDp = 820, heightDp = 420)
@Composable
private fun SortedSetEditorPreview() {
    AppTheme(dark = true) {
        val state = remember { previewWorkspaceState().also { it.selectKey("metrics:histogram:latency") } }
        SortedSetEditor(
            state = state,
            value =
                KeyValue.SortedSetValue(
                    entries =
                        listOf(
                            com.czh.rediscompose.domain.key.SortedSetEntry("p99", 982.0),
                            com.czh.rediscompose.domain.key.SortedSetEntry("p95", 644.0),
                            com.czh.rediscompose.domain.key.SortedSetEntry("p50", 411.0),
                        ),
                ),
            modifier = Modifier.padding(LocalAppDimens.current.spaceLg),
        )
    }
}
