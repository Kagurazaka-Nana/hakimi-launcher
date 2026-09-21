package com.minecraft.launcher.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composeunstyled.UnstyledButton

/** 像素贴纸底：偏移实心阴影 + 填充 + 硬描边（drawBehind，内容绘制在其上）。 */
fun Modifier.pixelSurface(
    fill: Color,
    border: Color,
    shadow: Color,
    corner: Dp,
    stroke: Dp,
    offset: Dp,
): Modifier = drawBehind {
    val cr = CornerRadius(corner.toPx(), corner.toPx())
    drawRoundRect(color = shadow, topLeft = Offset(offset.toPx(), offset.toPx()), size = size, cornerRadius = cr)
    drawRoundRect(color = fill, size = size, cornerRadius = cr)
    drawRoundRect(color = border, size = size, cornerRadius = cr, style = Stroke(width = stroke.toPx()))
}

/** 像素角标：在右上/左下画小色块，呼应概念图的像素装饰。 */
fun Modifier.pixelCorners(color: Color, size: Dp = 6.dp): Modifier = drawBehind {
    val px = size.toPx()
    drawRect(color = color, topLeft = Offset(this.size.width - px * 2, 0f), size = androidx.compose.ui.geometry.Size(px, px))
    drawRect(color = color, topLeft = Offset(0f, this.size.height - px), size = androidx.compose.ui.geometry.Size(px, px))
}

@Composable
fun HakimiText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = HakimiTheme.type.body,
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

@Composable
fun HakimiCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    background: Color = HakimiTheme.colors.surface,
    contentPadding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = HakimiTheme.colors
    val m = HakimiTheme.metrics
    val corner = 14.dp
    val clickableModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(
        modifier = modifier
            .padding(end = m.shadow, bottom = m.shadow)
            .pixelSurface(background, c.ink, c.ink, corner, m.stroke, m.shadow)
            .then(clickableModifier)
            .padding(contentPadding),
        content = content,
    )
}

@Composable
fun HakimiButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    filled: Boolean = true,
) {
    val c = HakimiTheme.colors
    val m = HakimiTheme.metrics
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "press")
    UnstyledButton(onClick = onClick, modifier = modifier, interactionSource = source) {
        Row(
            modifier = Modifier
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .padding(end = m.shadow, bottom = m.shadow)
                .pixelSurface(
                    fill = if (filled) c.primary else c.surface,
                    border = c.ink,
                    shadow = c.ink,
                    corner = 10.dp,
                    stroke = m.stroke,
                    offset = m.shadow,
                )
                .padding(horizontal = 22.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (icon != null) HakimiIcon(icon, null, c.onPrimary, size = 18.dp)
            HakimiText(text, style = HakimiTheme.type.label, color = c.onPrimary)
        }
    }
}

@Composable
fun HakimiIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    tint: Color = HakimiTheme.colors.text,
    background: Color = HakimiTheme.colors.surface,
) {
    val c = HakimiTheme.colors
    val m = HakimiTheme.metrics
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "press")
    UnstyledButton(onClick = onClick, modifier = modifier, interactionSource = source) {
        Box(
            modifier = Modifier
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .size(size)
                .padding(end = 3.dp, bottom = 3.dp)
                .pixelSurface(background, c.ink, c.ink, 8.dp, m.stroke, 3.dp),
            contentAlignment = Alignment.Center,
        ) {
            HakimiIcon(icon, contentDescription, tint, size = size * 0.45f)
        }
    }
}

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
    val m = HakimiTheme.metrics
    UnstyledButton(onClick = onClick, modifier = modifier) {
        Row(
            modifier = Modifier
                .height(36.dp)
                .padding(end = 3.dp, bottom = 3.dp)
                .pixelSurface(
                    fill = if (selected) c.primary else c.surface,
                    border = c.ink,
                    shadow = c.ink,
                    corner = 8.dp,
                    stroke = m.stroke,
                    offset = 3.dp,
                )
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

@Composable
fun HakimiChip(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = HakimiTheme.colors.primary,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val c = HakimiTheme.colors
    val bg = if (selected) color else color.copy(alpha = 0.18f)
    val fg = if (selected) c.onPrimary else c.text
    val clickableModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(2.dp, c.ink, RoundedCornerShape(6.dp))
            .then(clickableModifier)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        HakimiText(text, style = HakimiTheme.type.label, color = fg)
    }
}

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
            .clip(RoundedCornerShape(8.dp))
            .background(c.surface)
            .border(2.dp, c.ink, RoundedCornerShape(8.dp))
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

@Composable
fun HakimiProgressBar(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val c = HakimiTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(c.surfaceMuted)
            .border(2.dp, c.ink, RoundedCornerShape(3.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(10.dp)
                .background(color)
        )
    }
}

@Composable
fun HakimiToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = HakimiTheme.colors
    Box(
        modifier = modifier
            .size(width = 48.dp, height = 26.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (checked) c.primary else c.surfaceMuted)
            .border(2.dp, c.ink, RoundedCornerShape(6.dp))
            .toggleable(checked) { onCheckedChange(it) },
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .padding(3.dp)
                .size(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(c.surface)
                .border(2.dp, c.ink, RoundedCornerShape(4.dp)),
        )
    }
}

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
