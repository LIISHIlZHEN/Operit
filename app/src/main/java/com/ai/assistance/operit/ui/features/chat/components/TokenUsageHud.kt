package com.ai.assistance.operit.ui.features.chat.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import com.ai.assistance.operit.data.dao.TokenUsageAggregate
import com.ai.assistance.operit.data.db.AppDatabase
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * 输入框底部那行的会话用量：↑输入 ↓输出 · 缓存命中率 · 输出速度。
 * 数据来自本地 messages 表聚合，1s 轮询，纯本地查询；字体跟随主题（系统字体）。
 */
@Composable
fun TokenUsageHud(chatId: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var aggregate by remember(chatId) { mutableStateOf<TokenUsageAggregate?>(null) }

    LaunchedEffect(chatId) {
        if (chatId.isNullOrBlank()) {
            aggregate = null
            return@LaunchedEffect
        }
        val dao = AppDatabase.getDatabase(context).messageDao()
        while (true) {
            aggregate = runCatching { dao.aggregateTokenUsage(chatId) }.getOrNull()
            delay(1_000L)
        }
    }

    val data = aggregate ?: return
    if (data.outputTokens <= 0L) return

    val hitRate = if (data.inputTokens > 0L) data.cachedInputTokens * 100.0 / data.inputTokens else -1.0
    val tps = if (data.outputDurationMs > 0L) data.outputTokens * 1000.0 / data.outputDurationMs else -1.0

    val text = buildString {
        append('↑').append(formatTokenCount(data.inputTokens))
        append(" ↓").append(formatTokenCount(data.outputTokens))
        append(" · ")
        append(if (hitRate >= 0.0) String.format(Locale.US, "%.1f%%", hitRate) else "-")
        append(" · ")
        append(if (tps >= 0.0) String.format(Locale.US, "%.1ft/s", tps) else "-")
    }

    Row(modifier = modifier) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun formatTokenCount(value: Long): String = when {
    value >= 1_000_000L -> String.format(Locale.US, "%.2fM", value / 1_000_000.0)
    value >= 10_000L -> String.format(Locale.US, "%.1fk", value / 1_000.0)
    value >= 1_000L -> String.format(Locale.US, "%.2fk", value / 1_000.0)
    else -> value.toString()
}
