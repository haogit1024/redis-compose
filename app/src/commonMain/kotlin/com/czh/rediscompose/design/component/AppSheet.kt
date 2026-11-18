package com.czh.rediscompose.design.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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

@Composable
fun AppSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    tabs: (@Composable () -> Unit)? = null,
    footer: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!visible) return
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val scrimAlpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = 160),
        label = "appSheetScrim",
    )
    val slide by animateFloatAsState(
        targetValue = if (appeared) 0f else 24f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "appSheetSlide",
    )
    val focusRequester = remember { FocusRequester() }
    val scrimInteraction = remember { MutableInteractionSource() }
    val sheetInteraction = remember { MutableInteractionSource() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.scrim.copy(alpha = colors.scrim.alpha * scrimAlpha))
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
    ) {
        Column(
            modifier =
                modifier
                    .align(Alignment.CenterEnd)
                    .width(dimens.sheetWidth)
                    .fillMaxHeight()
                    .graphicsLayer { translationX = slide.dp.toPx() }
                    .shadow(elevation = 24.dp)
                    .background(colors.surface)
                    .drawBehind {
                        drawRect(
                            color = colors.borderStrong,
                            topLeft = Offset.Zero,
                            size = Size(1.dp.toPx(), size.height),
                        )
                    }
                    .clickable(
                        interactionSource = sheetInteraction,
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
            if (tabs != null) {
                Box(Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
                    tabs()
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.border),
                )
            }
            Column(
                modifier =
                    Modifier
                        .weight(1f)
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
private fun AppSheetPreview() {
    AppTheme(dark = true) {
        Box(Modifier.background(LocalAppColors.current.background)) {
            AppSheet(
                visible = true,
                onDismissRequest = {},
                title = "新建连接",
                subtitle = "连接到 Redis 6 及以上版本",
                tabs = {
                    AppSegmented(
                        options = listOf("基础", "TLS", "SSH"),
                        selected = "基础",
                        onSelect = {},
                    )
                },
                footer = {
                    AppButton("取消", {})
                    AppButton("测试连接", {}, variant = AppButtonVariant.Primary)
                },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppTextField(value = "", onValueChange = {}, placeholder = "连接名称")
                    AppTextField(value = "", onValueChange = {}, placeholder = "主机 127.0.0.1", mono = true)
                    AppTextField(value = "6379", onValueChange = {}, placeholder = "端口", mono = true)
                }
            }
        }
    }
}
