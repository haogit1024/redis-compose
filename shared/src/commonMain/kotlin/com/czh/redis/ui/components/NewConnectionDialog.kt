package com.czh.redis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.border
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.czh.redis.ui.AppColors
import com.czh.redis.ui.AppDimens

@Composable
fun NewConnectionDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    CustomDialog(visible = visible, onDismiss = onDismiss) {
        Column(modifier = Modifier.width(500.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material3.Text(
                    text = "New Connection",
                    color = AppColors.textPrimary,
                    fontSize = 18.sp,
                )
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.Text(text = "×", color = AppColors.textSecondary, fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(AppDimens.paddingLarge))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Text(text = "*", color = Color.Red)
                        androidx.compose.material3.Text(text = "Host", color = AppColors.textPrimary)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    CustomTextField(
                        value = "127.0.0.1",
                        onValueChange = {},
                        readOnly = true,
                    )
                }
                Spacer(modifier = Modifier.width(AppDimens.spacingLarge))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Text(text = "*", color = Color.Red)
                        androidx.compose.material3.Text(text = "Port", color = AppColors.textPrimary)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    CustomTextField(
                        value = "6379",
                        onValueChange = {},
                        readOnly = true,
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppDimens.spacingLarge))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    androidx.compose.material3.Text(text = "Password", color = AppColors.textPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    CustomTextField(
                        value = "",
                        onValueChange = {},
                        placeholder = "Auth",
                    )
                }
                Spacer(modifier = Modifier.width(AppDimens.spacingLarge))
                Column(modifier = Modifier.weight(1f)) {
                    androidx.compose.material3.Text(text = "Username", color = AppColors.textPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    CustomTextField(
                        value = "",
                        onValueChange = {},
                        placeholder = "ACL in Redis >= 6.0",
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppDimens.spacingLarge))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    androidx.compose.material3.Text(text = "Connection Name", color = AppColors.textPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    CustomTextField(
                        value = "",
                        onValueChange = {},
                    )
                }
                Spacer(modifier = Modifier.width(AppDimens.spacingLarge))
                Column(modifier = Modifier.weight(1f)) {
                    androidx.compose.material3.Text(text = "Separator", color = AppColors.textPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    CustomTextField(
                        value = ":",
                        onValueChange = {},
                        readOnly = true,
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppDimens.paddingLarge))

            Row(modifier = Modifier.fillMaxWidth()) {
                var sshChecked by remember { mutableStateOf(false) }
                var sslChecked by remember { mutableStateOf(false) }
                var sentinelChecked by remember { mutableStateOf(false) }
                var clusterChecked by remember { mutableStateOf(false) }
                var readonlyChecked by remember { mutableStateOf(false) }

                CustomCheckbox(checked = sshChecked, onCheckedChange = { sshChecked = it }, label = "SSH")
                Spacer(modifier = Modifier.width(AppDimens.spacingLarge))
                CustomCheckbox(checked = sslChecked, onCheckedChange = { sslChecked = it }, label = "SSL")
                Spacer(modifier = Modifier.width(AppDimens.spacingLarge))
                CustomCheckbox(checked = sentinelChecked, onCheckedChange = { sentinelChecked = it }, label = "Sentinel")
                Spacer(modifier = Modifier.width(AppDimens.spacingLarge))
                CustomCheckbox(checked = clusterChecked, onCheckedChange = { clusterChecked = it }, label = "Cluster")
                Spacer(modifier = Modifier.width(AppDimens.spacingLarge))
                CustomCheckbox(checked = readonlyChecked, onCheckedChange = { readonlyChecked = it }, label = "Readonly")
            }

            Spacer(modifier = Modifier.height(AppDimens.paddingLarge))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.White, shape = RoundedCornerShape(AppDimens.cornerSmall))
                        .border(1.dp, AppColors.border, shape = RoundedCornerShape(AppDimens.cornerSmall))
                        .clickable { onDismiss() }
                        .padding(AppDimens.paddingMedium),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.Text(text = "Cancel", color = AppColors.textPrimary)
                }
                Spacer(modifier = Modifier.width(AppDimens.spacingMedium))
                Box(
                    modifier = Modifier
                        .background(AppColors.primary, shape = RoundedCornerShape(AppDimens.cornerSmall))
                        .clickable { onConfirm() }
                        .padding(AppDimens.paddingMedium),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.Text(text = "OK", color = Color.White)
                }
            }
        }
    }
}