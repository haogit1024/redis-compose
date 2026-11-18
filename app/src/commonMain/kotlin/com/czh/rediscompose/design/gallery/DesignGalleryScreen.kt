package com.czh.rediscompose.design.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.component.AppBadge
import com.czh.rediscompose.design.component.AppBadgeTone
import com.czh.rediscompose.design.component.AppButton
import com.czh.rediscompose.design.component.AppButtonSize
import com.czh.rediscompose.design.component.AppButtonVariant
import com.czh.rediscompose.design.component.AppCard
import com.czh.rediscompose.design.component.AppDialog
import com.czh.rediscompose.design.component.AppEmptyState
import com.czh.rediscompose.design.component.AppIconButton
import com.czh.rediscompose.design.component.AppIconButtonSize
import com.czh.rediscompose.design.component.AppKbd
import com.czh.rediscompose.design.component.AppLoading
import com.czh.rediscompose.design.component.AppMenu
import com.czh.rediscompose.design.component.AppMenuItem
import com.czh.rediscompose.design.component.AppSearchField
import com.czh.rediscompose.design.component.AppSegmented
import com.czh.rediscompose.design.component.AppSegmentedSize
import com.czh.rediscompose.design.component.AppSheet
import com.czh.rediscompose.design.component.AppSwitch
import com.czh.rediscompose.design.component.AppTextField
import com.czh.rediscompose.design.component.AppToastHost
import com.czh.rediscompose.design.component.AppTooltip
import com.czh.rediscompose.design.component.AppTypeChip
import com.czh.rediscompose.design.component.AppTypeDot
import com.czh.rediscompose.design.component.AppVerticalScrollbar
import com.czh.rediscompose.design.component.ToastData
import com.czh.rediscompose.design.component.ToastKind
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.icon.BrandMark
import com.czh.rediscompose.design.theme.AppColors
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.domain.settings.Accent
import kotlin.math.roundToInt

/**
 * Temporary Phase 1 gallery: every token and base component on one page.
 *
 * Phase 3 replaces this with the real app shell; the page can then stay as a developer
 * reference until Phase 10 decides whether to keep it.
 */
@Composable
fun DesignGalleryScreen(
    dark: Boolean,
    accent: Accent,
    onDarkChange: (Boolean) -> Unit,
    onAccentChange: (Accent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val scrollState = rememberScrollState()
    var dialogVisible by remember { mutableStateOf(false) }
    var sheetVisible by remember { mutableStateOf(false) }
    var menuVisible by remember { mutableStateOf(false) }
    val toasts = remember { mutableStateListOf<ToastData>() }
    var nextToastId by remember { mutableStateOf(1L) }

    fun pushToast(
        message: String,
        kind: ToastKind = ToastKind.Info,
    ) {
        toasts.add(ToastData(nextToastId, message, kind))
        nextToastId += 1
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.background),
    ) {
        Column(Modifier.fillMaxSize()) {
            GalleryTopBar(
                dark = dark,
                accent = accent,
                onDarkChange = onDarkChange,
                onAccentChange = onAccentChange,
            )
            Row(Modifier.fillMaxSize()) {
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(scrollState)
                            .padding(horizontal = dimens.space2Xl + 4.dp, vertical = dimens.space2Xl),
                    verticalArrangement = Arrangement.spacedBy(dimens.space2Xl + 4.dp),
                ) {
                    GalleryIntro()
                    ColorsSection()
                    TypographySection()
                    ButtonsSection()
                    InputsSection()
                    BadgesSection()
                    ControlsSection()
                    SurfacesSection()
                    IconsSection()
                    OverlaysSection(
                        onOpenDialog = { dialogVisible = true },
                        onOpenSheet = { sheetVisible = true },
                        onToggleMenu = { menuVisible = !menuVisible },
                        menuVisible = menuVisible,
                        onDismissMenu = { menuVisible = false },
                        onShowToast = { kind -> pushToast(toastMessage(kind), kind) },
                    )
                }
                AppVerticalScrollbar(
                    scrollState = scrollState,
                    modifier = Modifier.padding(vertical = dimens.spaceXs),
                )
            }
        }

        AppDialog(
            visible = dialogVisible,
            onDismissRequest = { dialogVisible = false },
            title = "设置 TTL",
            subtitle = "为 cache:user:1001:profile 设置过期时间",
            footer = {
                AppButton("取消", { dialogVisible = false })
                AppButton(
                    text = "保存",
                    onClick = {
                        dialogVisible = false
                        pushToast("已保存", ToastKind.Success)
                    },
                    variant = AppButtonVariant.Primary,
                )
            },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceSm)) {
                AppTextField(value = "3600", onValueChange = {}, mono = true, placeholder = "秒")
                Text(
                    text = "留空表示永久保存。TTL 会在值面板中以倒计时展示。",
                    style = LocalAppTypography.current.caption,
                    color = colors.textTertiary,
                )
            }
        }

        AppSheet(
            visible = sheetVisible,
            onDismissRequest = { sheetVisible = false },
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
                AppButton("取消", { sheetVisible = false })
                AppButton(
                    text = "测试连接",
                    onClick = {
                        sheetVisible = false
                        pushToast("连接成功：12 ms", ToastKind.Success)
                    },
                    variant = AppButtonVariant.Primary,
                )
            },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceMd)) {
                AppTextField(value = "", onValueChange = {}, placeholder = "连接名称")
                AppTextField(value = "127.0.0.1", onValueChange = {}, placeholder = "主机", mono = true)
                AppTextField(value = "6379", onValueChange = {}, placeholder = "端口", mono = true)
                AppTextField(
                    value = "",
                    onValueChange = {},
                    placeholder = "密码（可选）",
                    isPassword = true,
                )
            }
        }

        AppToastHost(
            toasts = toasts,
            onDismiss = { id -> toasts.removeAll { it.id == id } },
        )
    }
}

@Composable
private fun GalleryTopBar(
    dark: Boolean,
    accent: Accent,
    onDarkChange: (Boolean) -> Unit,
    onAccentChange: (Accent) -> Unit,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(dimens.topBarHeight)
                .background(colors.surface)
                .padding(horizontal = dimens.spaceLg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceMd),
    ) {
        BrandMark()
        Text(
            text = "Redis Compose",
            style = typography.bodyStrong,
            color = colors.text,
        )
        Text(
            text = "/",
            style = typography.body,
            color = colors.textQuaternary,
        )
        Text(
            text = "Design system",
            style = typography.body,
            color = colors.textSecondary,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "ACCENT",
            style = typography.microLabel,
            color = colors.textTertiary,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm - 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Accent.entries.forEach { entry ->
                val entryColor = AppColors.forTheme(dark, entry).accent
                val selected = entry == accent
                Box(
                    modifier =
                        Modifier
                            .size(18.dp)
                            .background(entryColor, CircleShape)
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) colors.text else colors.borderStrong,
                                shape = CircleShape,
                            )
                            .clickable { onAccentChange(entry) }
                            .pointerHoverIcon(PointerIcon.Hand),
                )
            }
        }
        AppTooltip(if (dark) "切换到浅色主题" else "切换到深色主题") {
            AppIconButton(
                icon = if (dark) AppIcons.ThemeLight else AppIcons.ThemeDark,
                contentDescription = if (dark) "切换到浅色主题" else "切换到深色主题",
                onClick = { onDarkChange(!dark) },
            )
        }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(colors.border),
    )
}

@Composable
private fun GalleryIntro() {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    Column {
        Text(
            text = "PHASE 1 · DESIGN SYSTEM",
            style = typography.microLabel,
            color = colors.accent,
        )
        Text(
            text = "组件画廊",
            style = typography.pageTitle,
            color = colors.text,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            text =
                "这份页面只用于视觉验收：颜色、字体、图标与基础组件都来自同一套 token。" +
                    "Phase 3 会用真正的应用外壳替换此入口。",
            style = typography.body,
            color = colors.textTertiary,
            modifier =
                Modifier
                    .padding(top = 6.dp)
                    .widthIn(max = 620.dp),
        )
    }
}

@Composable
private fun GallerySection(
    eyebrow: String,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val dimens = LocalAppDimens.current
    Column(modifier) {
        Text(
            text = eyebrow,
            style = typography.microLabel,
            color = colors.textTertiary,
        )
        Text(
            text = title,
            style = typography.pageTitle,
            color = colors.text,
            modifier = Modifier.padding(top = 2.dp, bottom = dimens.spaceMd),
        )
        AppCard(contentPadding = dimens.spaceLg) {
            Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceMd + 2.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun ColorsSection() {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val swatches =
        listOf(
            "Background" to colors.background,
            "Surface" to colors.surface,
            "Surface 2" to colors.surface2,
            "Surface 3" to colors.surface3,
            "Surface 4" to colors.surface4,
            "Border strong" to colors.borderStrong,
            "Text" to colors.text,
            "Text secondary" to colors.textSecondary,
            "Text tertiary" to colors.textTertiary,
            "Text quaternary" to colors.textQuaternary,
            "Accent" to colors.accent,
            "Accent strong" to colors.accentStrong,
            "Success" to colors.success,
            "Warning" to colors.warning,
            "Danger" to colors.danger,
            "Info" to colors.info,
            "Code background" to colors.codeBackground,
            "Terminal" to colors.terminalBackground,
        )
    val typeColors =
        listOf(
            "STRING" to colors.typeString,
            "HASH" to colors.typeHash,
            "LIST" to colors.typeList,
            "SET" to colors.typeSet,
            "ZSET" to colors.typeZset,
            "STREAM" to colors.typeStream,
            "NONE" to colors.typeNone,
        )
    GallerySection(eyebrow = "TOKENS", title = "颜色") {
        Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceMd)) {
            swatches.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceMd)) {
                    row.forEach { (name, color) ->
                        ColorSwatch(name = name, color = color, modifier = Modifier.weight(1f))
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        Text(
            text = "数据类型",
            style = typography.microLabel,
            color = colors.textTertiary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm)) {
            typeColors.forEach { (name, color) ->
                AppTypeChip(text = name, color = color)
            }
        }
        Text(
            text = "JSON 高亮",
            style = typography.microLabel,
            color = colors.textTertiary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceLg)) {
            listOf(
                "Key" to colors.jsonKey,
                "String" to colors.jsonString,
                "Number" to colors.jsonNumber,
                "Boolean" to colors.jsonBoolean,
                "Punctuation" to colors.jsonPunctuation,
            ).forEach { (name, color) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(Modifier.size(10.dp).background(color, CircleShape))
                    Text(text = name, style = typography.monoValue, color = color)
                }
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    name: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
    ) {
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .background(color, RoundedCornerShape(dimens.radiusSm))
                    .border(1.dp, colors.border, RoundedCornerShape(dimens.radiusSm)),
        )
        Column {
            Text(
                text = name,
                style = typography.caption,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = color.hexLabel(),
                style = typography.monoMeta,
                color = colors.textQuaternary,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun TypographySection() {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val samples =
        listOf(
            Triple("display", typography.display, "安静而清晰"),
            Triple("pageTitle", typography.pageTitle, "服务器洞察"),
            Triple("keyTitle", typography.keyTitle, "cache:user:1001:profile"),
            Triple("sectionTitle", typography.sectionTitle, "连接设置"),
            Triple("body", typography.body, "连接已建立，延迟 12 ms"),
            Triple("bodyStrong", typography.bodyStrong, "Prod Cache"),
            Triple("bodySmall", typography.bodySmall, "复制 Key 到剪贴板"),
            Triple("caption", typography.caption, "最近一次采样 2 秒前"),
            Triple("microLabel", typography.microLabel, "CONNECTION LIBRARY"),
            Triple("monoValue", typography.monoValue, "GET cache:user:1001:profile"),
            Triple("monoMeta", typography.monoMeta, "TTL 28m · RTT 12 ms"),
            Triple("button", typography.button, "保存修改"),
            Triple("input", typography.input, "127.0.0.1:6379"),
        )
    GallerySection(eyebrow = "TYPESET", title = "字体") {
        Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceMd)) {
            samples.forEach { (name, style, sample) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = sample,
                        style = style,
                        color = colors.text,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = name,
                        style = typography.monoMeta,
                        color = colors.textQuaternary,
                    )
                }
            }
        }
    }
}

@Composable
private fun ButtonsSection() {
    val dimens = LocalAppDimens.current
    GallerySection(eyebrow = "ACTIONS", title = "按钮") {
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm)) {
            AppButton("主要操作", {}, variant = AppButtonVariant.Primary, icon = AppIcons.Save)
            AppButton("次要操作", {}, variant = AppButtonVariant.Secondary)
            AppButton("幽灵按钮", {}, variant = AppButtonVariant.Ghost)
            AppButton("删除", {}, variant = AppButtonVariant.Danger, icon = AppIcons.Delete)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm)) {
            AppButton("小号", {}, size = AppButtonSize.Small, icon = AppIcons.Add)
            AppButton("小号主操作", {}, size = AppButtonSize.Small, variant = AppButtonVariant.Primary)
            AppButton("禁用", {}, enabled = false)
            AppButton("加载中", {}, loading = true, variant = AppButtonVariant.Primary)
            AppButton("危险禁用", {}, variant = AppButtonVariant.Danger, enabled = false)
        }
    }
}

@Composable
private fun InputsSection() {
    val dimens = LocalAppDimens.current
    var normal by remember { mutableStateOf("prod-cache") }
    var search by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("redis-secret") }
    var passwordVisible by remember { mutableStateOf(false) }
    var multiline by remember { mutableStateOf("{\n  \"user\": \"1001\",\n  \"role\": \"admin\"\n}") }
    GallerySection(eyebrow = "FORMS", title = "输入") {
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceLg)) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(dimens.spaceMd),
            ) {
                AppTextField(
                    value = normal,
                    onValueChange = { normal = it },
                    placeholder = "连接名称",
                    leadingIcon = AppIcons.Database,
                )
                AppSearchField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = "搜索 Key 或命令…",
                )
                AppTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "密码",
                    isPassword = !passwordVisible,
                    trailingContent = {
                        AppIconButton(
                            icon = if (passwordVisible) AppIcons.EyeOff else AppIcons.Eye,
                            contentDescription = if (passwordVisible) "隐藏密码" else "显示密码",
                            onClick = { passwordVisible = !passwordVisible },
                            size = AppIconButtonSize.Small,
                        )
                    },
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(dimens.spaceMd),
            ) {
                AppTextField(
                    value = multiline,
                    onValueChange = { multiline = it },
                    placeholder = "JSON 值",
                    singleLine = false,
                    mono = true,
                )
                AppTextField(
                    value = "invalid",
                    onValueChange = {},
                    placeholder = "必填项",
                    isError = true,
                )
                AppTextField(
                    value = "disabled",
                    onValueChange = {},
                    enabled = false,
                )
            }
        }
    }
}

@Composable
private fun BadgesSection() {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    GallerySection(eyebrow = "LABELS", title = "徽章与快捷键") {
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm)) {
            AppBadge("默认")
            AppBadge("JSON", tone = AppBadgeTone.Accent)
            AppBadge("已同步", tone = AppBadgeTone.Success, icon = AppIcons.Check)
            AppBadge("TTL 42s", tone = AppBadgeTone.Warning)
            AppBadge("即将过期", tone = AppBadgeTone.Danger)
            AppBadge("Redis 7.4.2", tone = AppBadgeTone.Plain)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm)) {
            AppTypeChip("STRING", colors.typeString)
            AppTypeChip("HASH", colors.typeHash)
            AppTypeChip("LIST", colors.typeList)
            AppTypeChip("SET", colors.typeSet)
            AppTypeChip("ZSET", colors.typeZset)
            AppTypeChip("STREAM", colors.typeStream)
            Row(
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceXs + 1.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppTypeDot(colors.typeString)
                AppTypeDot(colors.typeHash)
                AppTypeDot(colors.typeList)
                AppTypeDot(colors.typeSet)
                AppTypeDot(colors.typeZset)
                AppTypeDot(colors.typeStream)
                AppTypeDot(colors.typeNone)
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceLg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                AppKbd("⌘")
                AppKbd("K")
            }
            AppKbd("⌘F")
            AppKbd("Esc")
            AppKbd("↵")
            AppKbd("N")
        }
    }
}

@Composable
private fun ControlsSection() {
    val dimens = LocalAppDimens.current
    var range by remember { mutableStateOf("1h") }
    var layout by remember { mutableStateOf("Auto") }
    var switchOn by remember { mutableStateOf(true) }
    GallerySection(eyebrow = "CONTROLS", title = "分段 / 开关 / 提示") {
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceLg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppSegmented(
                options = listOf("Auto", "JSON", "Text", "HEX"),
                selected = layout,
                onSelect = { layout = it },
            )
            AppSegmented(
                options = listOf("1h", "6h", "24h"),
                selected = range,
                onSelect = { range = it },
                size = AppSegmentedSize.Small,
            )
            AppSwitch(checked = switchOn, onCheckedChange = { switchOn = it })
            AppSwitch(checked = false, onCheckedChange = {}, enabled = false)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppTooltip("刷新 Keyspace") {
                AppIconButton(AppIcons.Refresh, "刷新 Keyspace", {})
            }
            AppTooltip("收藏这个 Key") {
                AppIconButton(AppIcons.Star, "收藏", {}, active = true)
            }
            AppTooltip("更多操作") {
                AppIconButton(AppIcons.More, "更多操作", {}, size = AppIconButtonSize.Small)
            }
            AppTooltip("删除") {
                AppIconButton(AppIcons.Delete, "删除", {}, enabled = false)
            }
        }
    }
}

@Composable
private fun SurfacesSection() {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    GallerySection(eyebrow = "SURFACES", title = "卡片 / 空状态 / 加载") {
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceLg)) {
            AppCard(
                modifier = Modifier.weight(1f),
                title = "服务器健康",
                description = "本次会话采样",
                actions = {
                    AppIconButton(AppIcons.Refresh, "刷新", {})
                },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "内存  1.24 GB / 2 GB",
                        style = typography.monoValue,
                        color = colors.textSecondary,
                    )
                    Text(
                        text = "命中率 96.4% · 24 clients",
                        style = typography.monoMeta,
                        color = colors.textTertiary,
                    )
                }
            }
            AppCard(modifier = Modifier.weight(1f), contentPadding = 0.dp, content = {
                AppEmptyState(
                    icon = AppIcons.Key,
                    title = "这个数据库还没有 Key",
                    description = "连接后可在这里浏览、搜索和编辑键值。",
                    action = {
                        AppButton("新建 Key", {}, variant = AppButtonVariant.Primary, icon = AppIcons.Add)
                    },
                )
            })
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceXl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppLoading(label = "正在加载 Keyspace…")
            AppLoading(label = "同步中", inline = true)
            AppLoading()
        }
    }
}

@Composable
private fun IconsSection() {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val icons =
        listOf(
            "Search" to AppIcons.Search,
            "Add" to AppIcons.Add,
            "Close" to AppIcons.Close,
            "Refresh" to AppIcons.Refresh,
            "Key" to AppIcons.Key,
            "Console" to AppIcons.Console,
            "Insights" to AppIcons.Insights,
            "Settings" to AppIcons.Settings,
            "Star" to AppIcons.Star,
            "Copy" to AppIcons.Copy,
            "Delete" to AppIcons.Delete,
            "Edit" to AppIcons.Edit,
            "More" to AppIcons.More,
            "Clock" to AppIcons.Clock,
            "Memory" to AppIcons.Memory,
            "Clients" to AppIcons.Clients,
            "Shield" to AppIcons.Shield,
            "Lock" to AppIcons.Lock,
            "Folder" to AppIcons.Folder,
            "Check" to AppIcons.Check,
            "Warning" to AppIcons.Warning,
            "Info" to AppIcons.Info,
            "External" to AppIcons.External,
            "Command" to AppIcons.Command,
            "Layers" to AppIcons.Layers,
            "Grid" to AppIcons.Grid,
            "Filter" to AppIcons.Filter,
            "Sort" to AppIcons.Sort,
            "Eye" to AppIcons.Eye,
            "Link" to AppIcons.Link,
            "Globe" to AppIcons.Globe,
            "Keyboard" to AppIcons.Keyboard,
            "Bell" to AppIcons.Bell,
            "Send" to AppIcons.Send,
            "Run" to AppIcons.Run,
            "Save" to AppIcons.Save,
            "Book" to AppIcons.Book,
            "Gauge" to AppIcons.Gauge,
            "Percent" to AppIcons.Percent,
            "Download" to AppIcons.Download,
            "Upload" to AppIcons.Upload,
            "Code" to AppIcons.Code,
            "Sparkles" to AppIcons.Sparkles,
            "History" to AppIcons.History,
            "Wifi" to AppIcons.Wifi,
            "Zap" to AppIcons.Zap,
            "Database" to AppIcons.Database,
            "Server" to AppIcons.Server,
        )
    GallerySection(eyebrow = "ICONS", title = "图标") {
        Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceMd)) {
            icons.chunked(8).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm)) {
                    row.forEach { (name, icon) ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = icon,
                                contentDescription = name,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(dimens.icon),
                            )
                            Text(
                                text = name,
                                style = typography.monoMeta,
                                color = colors.textQuaternary,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                    repeat(8 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun OverlaysSection(
    onOpenDialog: () -> Unit,
    onOpenSheet: () -> Unit,
    onToggleMenu: () -> Unit,
    menuVisible: Boolean,
    onDismissMenu: () -> Unit,
    onShowToast: (ToastKind) -> Unit,
) {
    val dimens = LocalAppDimens.current
    GallerySection(eyebrow = "OVERLAYS", title = "浮层") {
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppButton("打开对话框", onOpenDialog)
            AppButton("打开侧滑", onOpenSheet)
            Box {
                AppButton(
                    text = "更多菜单",
                    onClick = onToggleMenu,
                    icon = AppIcons.More,
                )
                AppMenu(
                    expanded = menuVisible,
                    onDismissRequest = onDismissMenu,
                    items =
                        listOf(
                            AppMenuItem("复制 Key", AppIcons.Copy, shortcut = "⌘C"),
                            AppMenuItem("重命名", AppIcons.Edit),
                            AppMenuItem("设置 TTL", AppIcons.Clock),
                            AppMenuItem(
                                label = "删除 Key",
                                icon = AppIcons.Delete,
                                danger = true,
                                separatorBefore = true,
                            ),
                        ),
                )
            }
            AppButton(
                text = "成功提示",
                onClick = { onShowToast(ToastKind.Success) },
                icon = AppIcons.Check,
            )
            AppButton(
                text = "错误提示",
                onClick = { onShowToast(ToastKind.Danger) },
                variant = AppButtonVariant.Danger,
            )
        }
        Text(
            text = "对话框与侧滑支持 Esc 关闭、点击遮罩关闭；Toast 3 秒后自动消失，最多同时三条。",
            style = LocalAppTypography.current.caption,
            color = LocalAppColors.current.textTertiary,
        )
    }
}

private fun toastMessage(kind: ToastKind): String =
    when (kind) {
        ToastKind.Info -> "已复制到剪贴板"
        ToastKind.Success -> "已保存"
        ToastKind.Warning -> "TTL 将在 30 秒后到期"
        ToastKind.Danger -> "连接失败：Connection refused"
    }

private fun Color.hexLabel(): String {
    val argb = toArgb()
    val rgb = (argb and 0xFFFFFF).toString(16).uppercase().padStart(6, '0')
    val alpha = ((argb ushr 24) and 0xFF)
    return if (alpha == 0xFF) "#$rgb" else "#$rgb · ${(alpha / 255f * 100).roundToInt()}%"
}

@Preview(widthDp = 1280, heightDp = 900)
@Composable
private fun DesignGalleryScreenPreview() {
    var dark by remember { mutableStateOf(true) }
    var accent by remember { mutableStateOf(Accent.IRIS) }
    AppTheme(dark = dark, accent = accent) {
        DesignGalleryScreen(
            dark = dark,
            accent = accent,
            onDarkChange = { dark = it },
            onAccentChange = { accent = it },
        )
    }
}
