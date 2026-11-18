package com.czh.rediscompose.feature.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.domain.key.KeyType

/**
 * Pinned keys shown above the tree.
 *
 * Pinned entries use full key names and stay visible while searching, so a saved key does
 * not disappear when the SCAN pattern changes.
 */
@Composable
internal fun PinnedKeyList(
    keys: List<String>,
    selectedKey: String?,
    typeOf: (String) -> KeyType?,
    ttlOf: (String) -> Long?,
    showTtl: Boolean,
    onSelect: (String) -> Unit,
    onUnpin: (String) -> Unit,
    onCopy: (String) -> Unit,
    onEnsurePreview: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (keys.isEmpty()) return
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        keys.forEach { key ->
            KeyRow(
                key = key,
                displayName = key,
                depth = 0,
                type = typeOf(key),
                ttlSeconds = ttlOf(key),
                showTtl = showTtl,
                pinned = true,
                selected = key == selectedKey,
                onSelect = { onSelect(key) },
                onTogglePinned = { onUnpin(key) },
                onCopy = { onCopy(key) },
                onEnsurePreview = { onEnsurePreview(key) },
            )
        }
    }
}

@Preview(widthDp = 320)
@Composable
private fun PinnedKeyListPreview() {
    AppTheme(dark = true) {
        val colors = LocalAppColors.current
        val dimens = LocalAppDimens.current
        val typography = LocalAppTypography.current
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(dimens.spaceSm),
        ) {
            Text(
                text = "置顶",
                style = typography.microLabel,
                color = colors.textQuaternary,
            )
            PinnedKeyList(
                keys = listOf("app:config:feature_flags", "temp:otp:13800001111"),
                selectedKey = "temp:otp:13800001111",
                typeOf = { key -> if (key.contains("otp")) KeyType.STRING else KeyType.HASH },
                ttlOf = { key -> if (key.contains("otp")) 42 else null },
                showTtl = true,
                onSelect = {},
                onUnpin = {},
                onCopy = {},
                onEnsurePreview = {},
            )
        }
    }
}
