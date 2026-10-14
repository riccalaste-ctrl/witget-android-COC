package com.zacatac.clashboard.widget

import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider
import com.zacatac.clashboard.data.model.UpgradeCategory

val WidgetWhite = colorProvider(Color.White)
val WidgetCardBg = colorProvider(Color(0xFFF4F6F8))
val WidgetDarkText = colorProvider(Color(0xFF202124))
val WidgetSubText = colorProvider(Color(0xFF5F6368))

private fun colorProvider(color: Color) = ColorProvider(color)

fun emojiForCategory(category: UpgradeCategory): String = when (category) {
    UpgradeCategory.BUILDER_VILLAGE -> "🏠"
    UpgradeCategory.LAB -> "⚗️"
    UpgradeCategory.STAR_LAB -> "✨"
    UpgradeCategory.PETS -> "🐾"
    UpgradeCategory.BUILDER_BASE -> "🛠️"
}
