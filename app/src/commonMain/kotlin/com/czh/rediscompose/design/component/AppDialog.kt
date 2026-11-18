package com.czh.rediscompose.design.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography

/**
 * Modal dialog drawn inside the app window.
 *
 * Using an in-composition overlay (instead of a separate `DialogWindow`) keeps the
 * scrim, focus handling and theming identical across the app and the gallery.
 */
@Composable
fun AppDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    footer: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!visible) return
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val alpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "appDialogAlpha",
    )
    val scale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.985f,
        animationSpec = tween(durationMillis = 150),
        label = "appDialogScale",
    )
    val focusRequester = remember { FocusRequester() }
    val scrimInteraction = remember { MutableInteractionSource() }
    val cardInteraction = remember { MutableInteractionSource() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val shape = RoundedCornerShape(dimens.radiusXl)

    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.scrim.copy(alpha = colors.scrim.alpha * alpha))
                .clickable(
                    interactionSource = scrimInteraction,
                    indication = null,
                    onClick = onDismissRequest,
                )
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                        onDismissRequest()
                        true
                    } else {
                        false
                    }
                }
                .focusRequester(focusRequester)
                .focusable(),
        contentAlignment = Alignment.Center,
    ) {
        val cardMaxHeight = minOf(dimens.dialogMaxHeight, this.maxHeight - 80.dp)
        Column(
            modifier =
                modifier
                    .width(dimens.dialogWidth)
                    .heightIn(max = cardMaxHeight)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .shadow(elevation = 24.dp, shape = shape)
                    .background(colors.surface, shape)
                    .border(1.dp, colors.borderStrong, shape)
                    .clickable(
                        interactionSource = cardInteraction,
                        indication = null,
                        onClick = {},
                    ),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, top = 17.dp, end = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceMd),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = typography.sectionTitle,
                        color = colors.text,
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = typography.caption,
                            color = colors.textTertiary,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                }
                AppIconButton(
                    icon = AppIcons.Close,
                    contentDescription = "关闭",
                    onClick = onDismissRequest,
                    size = AppIconButtonSize.Small,
                )
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.border),
            )
            Column(
                modifier =
                    Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                content = content,
            )
            if (footer != null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.border),
                )
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(colors.surface2)
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
                ) {
                    Spacer(Modifier.weight(1f))
                    footer()
                }
            }
        }
    }
}

@Preview
@Composable
private fun AppDialogPreview() {
    AppTheme(dark = true) {
        Box(Modifier.background(LocalAppColors.current.background)) {
            AppDialog(
                visible = true,
                onDismissRequest = {},
                title = "设置 TTL",
                subtitle = "为 cache:user:1001:profile 设置过期时间",
                footer = {
                    AppButton("取消", {})
                    AppButton("保存", {}, variant = AppButtonVariant.Primary)
                },
            ) {
                AppTextField(
                    value = "3600",
                    onValueChange = {},
                    placeholder = "秒",
                    mono = true,
                )
                Text(
                    text = "留空表示永久保存。",
                    style = LocalAppTypography.current.caption,
                    color = LocalAppColors.current.textTertiary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
