package com.czh.rediscompose.feature.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.component.AppBadge
import com.czh.rediscompose.design.component.AppBadgeTone
import com.czh.rediscompose.design.component.AppTypeChip
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.domain.key.KeyDetail
import com.czh.rediscompose.domain.key.KeyMetadata
import com.czh.rediscompose.domain.key.KeySummary
import com.czh.rediscompose.domain.key.KeyType
import com.czh.rediscompose.domain.key.KeyValue
import com.czh.rediscompose.util.formatEpochSeconds

/** Key identity, badges and header actions above the metadata grid. */
@Composable
internal fun ValueHeader(
    state: WorkspaceState,
    detail: KeyDetail?,
    loading: Boolean,
    onCopyKey: (String) -> Unit,
    onCopyCommand: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val key = state.selectedKey ?: return
    val type = detail?.summary?.type ?: state.typeOf(key)
    val stringValue = detail?.value as? KeyValue.StringValue
    val ttl = detail?.summary?.ttlSeconds ?: state.ttlOf(key)
    val pinned = state.isPinned(key)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimens.valueHeaderGap),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            Row(
                modifier = Modifier.padding(bottom = dimens.spaceXs + 1.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimens.valueTaglineGap),
            ) {
                AppTypeChip(
                    text = type?.displayLabel()?.uppercase() ?: "…",
                    color = type?.displayColor(colors) ?: colors.typeNone,
                )
                if (stringValue?.isJson == true) {
                    AppBadge(text = "JSON", tone = AppBadgeTone.Accent)
                }
                if (stringValue?.isBinary == true) {
                    AppBadge(text = "BINARY", tone = AppBadgeTone.Warning)
                }
                if (pinned) {
                    AppBadge(text = "已置顶", tone = AppBadgeTone.Warning, icon = AppIcons.StarFilled)
                }
                if (ttl != null && ttl in 0..60) {
                    AppBadge(text = "即将过期", tone = AppBadgeTone.Danger, icon = AppIcons.Clock)
                }
                if (state.isReadOnly) {
                    AppBadge(text = "只读", tone = AppBadgeTone.Warning, icon = AppIcons.Shield)
                }
                if (loading) {
                    Text(
                        text = "同步中…",
                        style = typography.monoMicro,
                        color = colors.textTertiary,
                    )
                }
            }
            SelectionContainer {
                Text(
                    text = key,
                    style = typography.keyTitle,
                    color = colors.text,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                modifier = Modifier.padding(top = dimens.spaceXs + 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm - 1.dp),
            ) {
                HeaderMeta("db${state.databaseIndex}")
                Dot()
                HeaderMeta("${state.session.profile.host}:${state.session.profile.port}")
                detail?.metadata?.encoding?.let { encoding ->
                    Dot()
                    HeaderMeta("OBJECT ENCODING $encoding")
                }
                if (state.detailLoadedAtEpochSeconds > 0) {
                    Dot()
                    HeaderMeta("读取于 ${formatEpochSeconds(state.detailLoadedAtEpochSeconds)}")
                }
            }
        }
        ValueHeaderActions(
            state = state,
            type = type,
            onCopyKey = onCopyKey,
            onCopyCommand = onCopyCommand,
        )
    }
}

@Composable
private fun HeaderMeta(text: String) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    Text(
        text = text,
        style = typography.monoMeta,
        color = colors.textTertiary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun Dot() {
    Box(
        Modifier
            .size(LocalAppDimens.current.metaTrackHeight)
            .background(LocalAppColors.current.textQuaternary, CircleShape),
    )
}

@Preview(widthDp = 760)
@Composable
private fun ValueHeaderPreview() {
    AppTheme(dark = true) {
        val state = remember { previewWorkspaceState().also { it.selectKey("cache:user:1001:profile") } }
        ValueHeader(
            state = state,
            detail =
                KeyDetail(
                    summary = KeySummary("cache:user:1001:profile", KeyType.HASH, 1_694),
                    metadata = KeyMetadata(ttlSeconds = 1_694, memoryBytes = 1_433, encoding = "listpack", elementCount = 8),
                    value = KeyValue.HashValue(emptyList()),
                ),
            loading = false,
            onCopyKey = {},
            onCopyCommand = {},
            modifier = Modifier.padding(LocalAppDimens.current.spaceLg),
        )
    }
}
