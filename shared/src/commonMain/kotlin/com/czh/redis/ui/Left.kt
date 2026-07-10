package com.czh.redis.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.czh.redis.ui.components.NewConnectionButton

@Composable
fun Left(
    onNewConnectionClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.paddingMedium),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spacingSmall),
        ) {
            NewConnectionButton(
                onClick = onNewConnectionClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}