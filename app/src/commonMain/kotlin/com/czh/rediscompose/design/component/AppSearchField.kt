package com.czh.rediscompose.design.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors

@Composable
fun AppSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "搜索…",
    kbdText: String? = "⌘F",
    enabled: Boolean = true,
    mono: Boolean = false,
) {
    AppTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = placeholder,
        leadingIcon = AppIcons.Search,
        trailingContent =
            kbdText?.let { hint ->
                @Composable { AppKbd(hint) }
            },
        enabled = enabled,
        mono = mono,
    )
}

@Preview
@Composable
private fun AppSearchFieldPreview() {
    AppTheme(dark = true) {
        Column(
            modifier =
                Modifier
                    .background(LocalAppColors.current.background)
                    .padding(16.dp)
                    .width(320.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppSearchField(value = "", onValueChange = {}, placeholder = "搜索 Key、命令或连接…")
            AppSearchField(
                value = "user:*",
                onValueChange = {},
                placeholder = "匹配模式",
                kbdText = null,
                mono = true,
            )
        }
    }
}
