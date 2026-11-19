package com.czh.rediscompose.feature.connections.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.czh.rediscompose.design.component.AppTextField
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.domain.connection.ConnectionField
import com.czh.rediscompose.feature.connections.ConnectionDraft
import com.czh.rediscompose.feature.connections.FormField
import com.czh.rediscompose.feature.connections.FormSection
import com.czh.rediscompose.feature.connections.ToggleRow
import com.czh.rediscompose.feature.connections.error

@Composable
fun ConnectionAdvancedTab(
    draft: ConnectionDraft,
    errors: Map<ConnectionField, String>,
    onDraftChange: ((ConnectionDraft) -> ConnectionDraft) -> Unit,
) {
    val dimens = LocalAppDimens.current
    Column(Modifier.fillMaxWidth()) {
        FormSection("超时")
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm)) {
            Box(Modifier.weight(1f)) {
                FormField(label = "连接超时（ms）", error = errors.error(ConnectionField.CONNECT_TIMEOUT)) {
                    AppTextField(
                        value = draft.connectTimeout,
                        onValueChange = { value -> onDraftChange { it.copy(connectTimeout = value) } },
                        placeholder = "5000",
                        mono = true,
                        isError = errors.error(ConnectionField.CONNECT_TIMEOUT) != null,
                    )
                }
            }
            Box(Modifier.weight(1f)) {
                FormField(label = "读取超时（ms）", error = errors.error(ConnectionField.READ_TIMEOUT)) {
                    AppTextField(
                        value = draft.readTimeout,
                        onValueChange = { value -> onDraftChange { it.copy(readTimeout = value) } },
                        placeholder = "5000",
                        mono = true,
                        isError = errors.error(ConnectionField.READ_TIMEOUT) != null,
                    )
                }
            }
        }
        FormSection("命名空间")
        FormField(label = "Key 分隔符", hint = "影响 Key 树的目录分组，留空表示平铺") {
            AppTextField(
                value = draft.keySeparator,
                onValueChange = { value -> onDraftChange { it.copy(keySeparator = value) } },
                placeholder = ":",
                mono = true,
            )
        }
        FormSection("高级拓扑")
        ToggleRow(
            title = "Sentinel / Cluster",
            hint = "当前版本仅支持单节点；Sentinel 与 Cluster 将在后续版本提供",
            checked = false,
            onCheckedChange = {},
            enabled = false,
        )
    }
}
