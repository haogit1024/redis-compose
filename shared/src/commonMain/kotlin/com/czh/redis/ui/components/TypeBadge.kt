package com.czh.redis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.czh.redis.redis.RedisType
import com.czh.redis.ui.TypeColors

@Composable
fun TypeBadge(
    type: RedisType,
    modifier: Modifier = Modifier,
) {
    val color = TypeColors.forType(type)
    val label =
        when (type) {
            RedisType.STRING -> "STR"
            RedisType.HASH -> "HASH"
            RedisType.LIST -> "LIST"
            RedisType.SET -> "SET"
            RedisType.ZSET -> "ZSET"
            RedisType.STREAM -> "STREAM"
            RedisType.NONE -> "NONE"
        }
    Box(
        modifier =
            modifier
                .background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                .padding(horizontal = 5.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        )
    }
}

@Composable
fun StatusDot(
    status: com.czh.redis.ui.ConnectionStatus,
    modifier: Modifier = Modifier,
) {
    val colors = com.czh.redis.ui.LocalAppColors.current
    val color =
        when (status) {
            com.czh.redis.ui.ConnectionStatus.CONNECTED -> colors.success
            com.czh.redis.ui.ConnectionStatus.CONNECTING -> colors.warn
            com.czh.redis.ui.ConnectionStatus.ERROR -> colors.error
            com.czh.redis.ui.ConnectionStatus.DISCONNECTED -> Color.Gray
        }
    Box(
        modifier =
            modifier
                .background(color, RoundedCornerShape(50))
                .padding(3.5.dp),
    )
}
