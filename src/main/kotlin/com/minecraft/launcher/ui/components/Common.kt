package com.minecraft.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minecraft.launcher.ui.theme.CardWhite
import com.minecraft.launcher.ui.theme.ChipBg
import com.minecraft.launcher.ui.theme.ChipText
import com.minecraft.launcher.ui.theme.PrimaryIndigo
import com.minecraft.launcher.ui.theme.TextDark
import com.minecraft.launcher.ui.theme.TextMuted

/** 白色圆角卡片容器。 */
@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize(), content = content)
    }
}

/** 圆角药丸标签。 */
@Composable
fun Pill(text: String, bg: Color = ChipBg, fg: Color = ChipText) {
    Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(bg).padding(horizontal = 12.dp, vertical = 5.dp)) {
        Text(text, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

/** 页面顶部标题区：图标 + 标题 + 副标题 + 右侧可选内容。 */
@Composable
fun PageHeader(icon: ImageVector, title: String, subtitle: String, right: (@Composable RowScope.() -> Unit)? = null) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon = icon, color = PrimaryIndigo, size = 44.dp)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Text(subtitle, color = TextMuted, fontSize = 13.sp)
        }
        right?.invoke(this)
    }
}

/** 圆角图标徽标：渐变底 + 图标。 */
@Composable
fun IconBadge(icon: ImageVector, color: Color, size: androidx.compose.ui.unit.Dp = 40.dp, iconSize: androidx.compose.ui.unit.Dp = 22.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(listOf(color.copy(alpha = 0.22f), color.copy(alpha = 0.08f)))),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(iconSize))
    }
}

/** 缩略图占位（无贴图，用渐变 + 图标）。 */
@Composable
fun PlaceholderThumb(icon: ImageVector, modifier: Modifier = Modifier, tint: Color = PrimaryIndigo) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(14.dp)).background(
            Brush.verticalGradient(listOf(tint.copy(alpha = 0.28f), ChipBg))
        ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
    }
}

/** 圆形头像占位。 */
@Composable
fun AvatarCircle(icon: ImageVector, color: Color, size: androidx.compose.ui.unit.Dp = 44.dp) {
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(size * 0.5f))
    }
}

/** 区块标题行：图标 + 文本 + 右侧内容。 */
@Composable
fun SectionHeaderRow(icon: ImageVector, title: String, color: Color = PrimaryIndigo, trailing: (@Composable RowScope.() -> Unit)? = null) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = color)
        Text(title, fontWeight = FontWeight.Bold, color = TextDark, modifier = Modifier.weight(1f))
        trailing?.invoke(this)
    }
}
