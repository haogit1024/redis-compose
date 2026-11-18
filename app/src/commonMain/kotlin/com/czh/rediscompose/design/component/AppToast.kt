package com.czh.rediscompose.design.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import kotlinx.coroutines.delay

enum class ToastKind { Info, Success, Warning, Danger }

data class ToastData(
    val id: Long,
    val message: String,
    val kind: ToastKind = ToastKind.Info,
)

/**
 * Bottom-right toast stack. Callers own the list; the host removes each toast after
 * three seconds through [onDismiss] and shows at most three entries at a time.
 */
@Composable
fun AppToastHost(
    toasts: List<ToastData>,
    onDismiss: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalAppDimens.current
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Column(
            modifier =
                Modifier.padding(
                    end = dimens.spaceLg,
                    bottom = dimens.toastBottomPadding,
                ),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            toasts.takeLast(3).forEach { toast ->
                key(toast.id) {
                    AppToastItem(toast = toast, onDismiss = onDismiss)
                }
            }
        }
    }
}

@Composable
private fun AppToastItem(
    toast: ToastData,
    onDismiss: (Long) -> Unit,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val (icon, color) =
        when (toast.kind) {
            ToastKind.Info -> AppIcons.Info to colors.info
            ToastKind.Success -> AppIcons.CheckCircle to colors.success
            ToastKind.Warning -> AppIcons.Warning to colors.warning
            ToastKind.Danger -> AppIcons.ErrorCircle to colors.danger
        }
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(toast.id) {
        appeared = true
        delay(3000)
        onDismiss(toast.id)
    }
    val progress by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "appToastAppear",
    )
    val shape = RoundedCornerShape(dimens.radiusMd)
    Row(
        modifier =
            Modifier
                .graphicsLayer {
                    alpha = progress
                    translationY = (1f - progress) * 8.dp.toPx()
                }
                .shadow(elevation = 12.dp, shape = shape)
                .background(colors.surface3, shape)
                .border(1.dp, colors.borderStrong, shape)
                .padding(horizontal = dimens.spaceMd, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(dimens.iconSm),
        )
        Text(
            text = toast.message,
            style = typography.bodySmall,
            color = colors.text,
            maxLines = 2,
        )
    }
}

@Preview
@Composable
private fun AppToastPreview() {
    AppTheme(dark = true) {
        Box(Modifier.background(LocalAppColors.current.background)) {
            AppToastHost(
                toasts =
                    listOf(
                        ToastData(1, "已保存", ToastKind.Success),
                        ToastData(2, "TTL 将在 30 秒后到期", ToastKind.Warning),
                        ToastData(3, "连接失败：Connection refused", ToastKind.Danger),
                    ),
                onDismiss = {},
            )
        }
    }
}
