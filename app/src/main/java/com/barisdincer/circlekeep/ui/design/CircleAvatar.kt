package com.barisdincer.circlekeep.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/** Round avatar with initials over a deterministic warm tone derived from [name]. */
@Composable
fun CircleAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    overrideContainer: Color? = null,
    overrideContent: Color? = null,
) {
    val tone = avatarToneFor(name)
    val container = overrideContainer ?: tone.container
    val content = overrideContent ?: tone.content
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initialsFor(name),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.36f).sp,
            color = content,
        )
    }
}

private fun initialsFor(name: String): String {
    val parts = name.trim().split(" ", "-").filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts.first().take(1).uppercase(Locale("tr", "TR"))
        else -> (parts.first().take(1) + parts.last().take(1)).uppercase(Locale("tr", "TR"))
    }
}
