package com.czh.rediscompose.feature.connections.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.component.AppSegmented
import com.czh.rediscompose.design.component.AppSegmentedSize
import com.czh.rediscompose.design.component.AppTextField
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.domain.connection.ConnectionColor
import com.czh.rediscompose.domain.connection.ConnectionField
import com.czh.rediscompose.feature.connections.ConnectionDraft
import com.czh.rediscompose.feature.connections.FormField
import com.czh.rediscompose.feature.connections.FormSection
import com.czh.rediscompose.feature.connections.PasswordField
import com.czh.rediscompose.feature.connections.ToggleRow
import com.czh.rediscompose.feature.connections.displayName
import com.czh.rediscompose.feature.connections.error
import com.czh.rediscompose.feature.connections.toColor

@Composable
fun ConnectionBasicTab(
    draft: ConnectionDraft,
    errors: Map<ConnectionField, String>,
    onDraftChange: ((ConnectionDraft) -> ConnectionDraft) -> Unit,
    nameFocusRequester: FocusRequester,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    Column(Modifier.fillMaxWidth()) {
        FormSection("身份")
        FormField(label = "连接名称", required = true, error = errors.error(ConnectionField.NAME)) {
            AppTextField(
                value = draft.name,
                onValueChange = { value -> onDraftChange { it.copy(name = value) } },
                placeholder = "例如 Production Cache",
                isError = errors.error(ConnectionField.NAME) != null,
                modifier = Modifier.focusRequester(nameFocusRequester),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm)) {
            Box(Modifier.weight(1f)) {
                FormField(label = "Host", required = true, error = errors.error(ConnectionField.HOST)) {
                    AppTextField(
                        value = draft.host,
                        onValueChange = { value -> onDraftChange { it.copy(host = value) } },
                        placeholder = "127.0.0.1",
                        mono = true,
                        isError = errors.error(ConnectionField.HOST) != null,
                    )
                }
            }
            Box(Modifier.width(100.dp)) {
                FormField(label = "Port", required = true, error = errors.error(ConnectionField.PORT)) {
                    AppTextField(
                        value = draft.port,
                        onValueChange = { value -> onDraftChange { it.copy(port = value) } },
                        placeholder = "6379",
                        mono = true,
                        isError = errors.error(ConnectionField.PORT) != null,
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm)) {
            Box(Modifier.weight(1f)) {
                FormField(label = "用户名（Redis 6+ ACL）", error = errors.error(ConnectionField.USERNAME)) {
                    AppTextField(
                        value = draft.username,
                        onValueChange = { value -> onDraftChange { it.copy(username = value) } },
                        placeholder = "default",
                    )
                }
            }
            Box(Modifier.weight(1f)) {
                FormField(label = "密码", error = errors.error(ConnectionField.PASSWORD)) {
                    PasswordField(
                        value = draft.password,
                        onValueChange = { value -> onDraftChange { it.copy(password = value) } },
                        placeholder = "••••••••",
                    )
                }
            }
        }

        FormSection("默认与分组")
        FormField(label = "环境") {
            AppSegmented(
                options = com.czh.rediscompose.domain.connection.ConnectionEnvironment.entries,
                selected = draft.environment,
                onSelect = { value -> onDraftChange { it.copy(environment = value) } },
                size = AppSegmentedSize.Small,
                label = { it.displayName() },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm)) {
            Box(Modifier.weight(1f)) {
                FormField(label = "默认 DB", error = errors.error(ConnectionField.DATABASE)) {
                    AppTextField(
                        value = draft.defaultDatabase,
                        onValueChange = { value -> onDraftChange { it.copy(defaultDatabase = value) } },
                        placeholder = "0",
                        mono = true,
                        isError = errors.error(ConnectionField.DATABASE) != null,
                    )
                }
            }
            Box(Modifier.weight(1f)) {
                FormField(label = "Key 分隔符", hint = "留空表示不分组") {
                    AppTextField(
                        value = draft.keySeparator,
                        onValueChange = { value -> onDraftChange { it.copy(keySeparator = value) } },
                        placeholder = ":",
                        mono = true,
                    )
                }
            }
        }
        FormField(label = "分组", hint = "用于连接库筛选，可留空") {
            AppTextField(
                value = draft.group,
                onValueChange = { value -> onDraftChange { it.copy(group = value) } },
                placeholder = "Production",
            )
        }

        FormSection("外观与安全")
        FormField(label = "颜色标签") {
            Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm - 1.dp)) {
                ConnectionColor.entries.forEach { color ->
                    val swatch = color.toColor(colors)
                    Box(
                        modifier =
                            Modifier
                                .size(17.dp)
                                .clip(CircleShape)
                                .background(swatch)
                                .border(
                                    width = if (draft.color == color) 2.dp else 1.dp,
                                    color = if (draft.color == color) colors.text else colors.borderStrong,
                                    shape = CircleShape,
                                )
                                .clickable { onDraftChange { it.copy(color = color) } }
                                .pointerHoverIcon(PointerIcon.Hand),
                    )
                }
            }
        }
        ToggleRow(
            title = "只读模式",
            hint = "连接后拦截 SET / DEL / FLUSHDB 等写命令",
            checked = draft.readOnly,
            onCheckedChange = { value -> onDraftChange { it.copy(readOnly = value) } },
        )
    }
}
