package com.czh.redis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.czh.redis.ui.AppColors

@Composable
fun CustomCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .border(1.dp, AppColors.border, shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(AppColors.primary),
                )
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        androidx.compose.material3.Text(text = label, color = AppColors.textPrimary)
    }
}