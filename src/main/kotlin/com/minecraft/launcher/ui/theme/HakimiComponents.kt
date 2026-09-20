package com.minecraft.launcher.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composeunstyled.UnstyledButton

/** 文本（Foundation BasicText，无 Material）。 */
@Composable
fun HakimiText(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = HakimiTheme.type.body,
    color: Color = HakimiTheme.colors.text,
    maxLines: Int = Int.MAX_VALUE,
    align: TextAlign = TextAlign.Unspecified,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(color = color, textAlign = align),
        maxLines = maxLines,
    )
}

/** 图标（Foundation Image + tint）。 */
@Composable
fun HakimiIcon(
    image: ImageVector,
    contentDescription: String?,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
) {
    Image(
        imageVector = image,
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier.size(size),
    )
}

/** 卡片容器。 */
@Composable
fun HakimiCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    background: Color = HakimiTheme.colors.surface,
    contentPadding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = HakimiTheme.shapes.large
    val clickableModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(
        modifier = modifier
            .clip(shape)
            .background(background)
            .then(clickableModifier)
            .padding(contentPadding),
        content = content,
    )
}

/** 主按钮：UnstyledButton 提供焦点/键盘/语义，视觉自绘。 */
@Composable
fun HakimiButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    filled: Boolean = true,
) {
    val c = HakimiTheme.colors
    val shape = HakimiTheme.shapes.pill
    UnstyledButton(onClick = onClick, modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(shape)
                .background(if (filled) c.primary else c.surfaceMuted)
                .padding(horizontal = 22.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (icon != null) {
                HakimiIcon(icon, null, if (filled) c.onPrimary else c.primary, size = 18.dp)
            }
            HakimiText(text, style = HakimiTheme.type.label, color = if (filled) c.onPrimary else c.primary)
        }
    }
}

/** 图标按钮。 */
@Composable
fun HakimiIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    tint: Color = HakimiTheme.colors.text,
    background: Color = Color.Transparent,
) {
    UnstyledButton(onClick = onClick, modifier = modifier) {
        Box(
            modifier = Modifier.size(size).clip(HakimiTheme.shapes.pill).background(background),
            contentAlignment = Alignment.Center,
        ) {
            HakimiIcon(icon, contentDescription, tint, size = size * 0.45f)
        }
    }
}

/** 标签页（浏览器式，可关闭）。 */
@Composable
fun HakimiTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val c = HakimiTheme.colors
    UnstyledButton(onClick = onClick, modifier = modifier) {
        Row(
            modifier = Modifier
                .height(34.dp)
                .clip(HakimiTheme.shapes.pill)
                .background(if (selected) c.primary else Color.Transparent)
                .padding(start = 14.dp, end = if (onClose != null) 8.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            HakimiIcon(icon, null, if (selected) c.onPrimary else c.textMuted, size = 16.dp)
            HakimiText(label, style = HakimiTheme.type.label, color = if (selected) c.onPrimary else c.text)
            if (onClose != null) {
                UnstyledButton(onClick = onClose) {
                    Box(modifier = Modifier.size(18.dp), contentAlignment = Alignment.Center) {
                        HakimiText("✕", style = HakimiTheme.type.caption, color = if (selected) c.onPrimary else c.textMuted)
                    }
                }
            }
        }
    }
}

/** 胶囊标签。 */
@Composable
fun HakimiChip(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = HakimiTheme.colors.primary,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val bg = if (selected) color else color.copy(alpha = 0.14f)
    val fg = if (selected) HakimiTheme.colors.onPrimary else color
    Box(
        modifier = modifier
            .clip(HakimiTheme.shapes.pill)
            .background(bg)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        HakimiText(text, style = HakimiTheme.type.label, color = fg)
    }
}

/** 搜索框（Foundation BasicTextField）。 */
@Composable
fun HakimiSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    val c = HakimiTheme.colors
    Row(
        modifier = modifier
            .clip(HakimiTheme.shapes.medium)
            .background(c.surface)
            .border(1.dp, c.outline, HakimiTheme.shapes.medium)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HakimiIcon(icon, null, c.textMuted, size = 18.dp)
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                HakimiText(placeholder, style = HakimiTheme.type.body, color = c.textMuted)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = HakimiTheme.type.body.copy(color = c.text),
                cursorBrush = SolidColor(c.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** 进度条。 */
@Composable
fun HakimiProgressBar(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val track = HakimiTheme.colors.surfaceMuted
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(HakimiTheme.shapes.pill)
            .background(track)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(8.dp)
                .clip(HakimiTheme.shapes.pill)
                .background(color)
        )
    }
}

/** 开关（Foundation + 语义）。 */
@Composable
fun HakimiToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = HakimiTheme.colors
    val trackColor = if (checked) c.primary else c.surfaceMuted
    Box(
        modifier = modifier
            .size(width = 46.dp, height = 26.dp)
            .clip(HakimiTheme.shapes.pill)
            .background(trackColor)
            .toggleable(checked) { onCheckedChange(it) },
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .padding(3.dp)
                .size(20.dp)
                .clip(HakimiTheme.shapes.pill)
                .background(c.surface)
        )
    }
}

/** 区块标题行。 */
@Composable
fun HakimiSectionHeader(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    color: Color = HakimiTheme.colors.primary,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HakimiIcon(icon, null, color, size = 18.dp)
        HakimiText(title, style = HakimiTheme.type.title, modifier = Modifier.weight(1f))
        trailing?.invoke(this)
    }
}

/** 通用弹层（覆盖 + 居中卡片）。 */
@Composable
fun HakimiOverlay(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HakimiTheme.colors.scrim)
            .clickable(onClick = onDismiss),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center, content = content)
    }
}
