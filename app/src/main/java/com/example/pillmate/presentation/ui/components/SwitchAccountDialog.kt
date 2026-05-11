// ============================================================
// FILE: SwitchAccountDialog.kt  (file MỚI — đặt cùng package)
// Đặt tại: .../presentation/ui/components/SwitchAccountDialog.kt
//
// Cách dùng trong SettingsScreen:
//
//   var showSwitchDialog by remember { mutableStateOf(false) }
//
//   if (showSwitchDialog) {
//       SwitchAccountDialog(
//           viewModel        = authViewModel,
//           onDismiss        = { showSwitchDialog = false },
//           onAccountSelected= { email, password, isGoogle ->
//               showSwitchDialog = false
//               if (isGoogle) launchGoogleSignIn(email)
//               else          onNavigateToSignIn(email, password)
//           }
//       )
//   }
//
//   // Trong danh sách cài đặt, thêm item:
//   SettingsItem(
//       icon  = Icons.Outlined.SwitchAccount,
//       title = stringResource(R.string.switch_account),
//       onClick = { showSwitchDialog = true }
//   )
// ============================================================

package com.example.pillmate.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.pillmate.data.local.entity.SavedAccountEntity
import com.example.pillmate.presentation.ui.screens.PrimaryGreen
import com.example.pillmate.presentation.viewmodel.AuthViewModel

// ─────────────────────────────────────────────────────────────
// SwitchAccountDialog
// ─────────────────────────────────────────────────────────────
@Composable
fun SwitchAccountDialog(
	viewModel: AuthViewModel,
	onDismiss: () -> Unit,
	// isGoogle=true → cần gọi Google Sign-In với emailHint
	// isGoogle=false → navigate tới SignInScreen với email/password
	onAccountSelected: (email: String, password: String?, isGoogle: Boolean) -> Unit
) {
	val savedAccounts by viewModel.savedAccounts.collectAsState(initial = emptyList())
	
	Dialog(
		onDismissRequest = onDismiss,
		properties = DialogProperties(usePlatformDefaultWidth = false)
	) {
		Surface(
			modifier = Modifier
				.fillMaxWidth(0.92f)
				.wrapContentHeight(),
			shape = RoundedCornerShape(20.dp),
			color = Color.White,
			tonalElevation = 8.dp
		) {
			Column(modifier = Modifier.padding(24.dp)) {
				
				// ── Header ──
				Text(
					text = "Chuyển tài khoản",
					fontSize = 20.sp,
					fontWeight = FontWeight.Bold,
					color = PrimaryGreen,
					modifier = Modifier.padding(bottom = 4.dp)
				)
				Text(
					text = "Chọn tài khoản bạn muốn đăng nhập",
					fontSize = 13.sp,
					color = Color.Gray,
					modifier = Modifier.padding(bottom = 20.dp)
				)
				
				HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
				Spacer(modifier = Modifier.height(16.dp))
				
				// ── Danh sách tài khoản ──
				if (savedAccounts.isEmpty()) {
					// Trạng thái rỗng
					Box(
						modifier = Modifier
							.fillMaxWidth()
							.padding(vertical = 32.dp),
						contentAlignment = Alignment.Center
					) {
						Column(horizontalAlignment = Alignment.CenterHorizontally) {
							Icon(
								imageVector = Icons.Outlined.Person,
								contentDescription = null,
								tint = Color.LightGray,
								modifier = Modifier.size(48.dp)
							)
							Spacer(modifier = Modifier.height(12.dp))
							Text(
								text = "Chưa có tài khoản nào được lưu",
								color = Color.Gray,
								fontSize = 14.sp
							)
						}
					}
				} else {
					LazyColumn(
						modifier = Modifier
							.heightIn(max = 320.dp)
							.fillMaxWidth(),
						verticalArrangement = Arrangement.spacedBy(10.dp)
					) {
						items(savedAccounts) { account ->
							SwitchAccountDialogItem(
								account = account,
								onClick = {
									onAccountSelected(
										account.email,
										account.password,
										account.loginMethod == "GOOGLE"
									)
								}
							)
						}
					}
				}
				
				Spacer(modifier = Modifier.height(20.dp))
				HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
				Spacer(modifier = Modifier.height(12.dp))
				
				// ── Nút Huỷ ──
				TextButton(
					onClick = onDismiss,
					modifier = Modifier.align(Alignment.End)
				) {
					Text(
						text = "Huỷ",
						color = Color.Gray,
						fontWeight = FontWeight.Medium,
						fontSize = 15.sp
					)
				}
			}
		}
	}
}

// ─────────────────────────────────────────────────────────────
// Item trong dialog
// ─────────────────────────────────────────────────────────────
@Composable
private fun SwitchAccountDialogItem(
	account: SavedAccountEntity,
	onClick: () -> Unit
) {
	Surface(
		modifier = Modifier
			.fillMaxWidth()
			.clickable { onClick() },
		shape = RoundedCornerShape(14.dp),
		color = PrimaryGreen.copy(alpha = 0.08f),
		border = androidx.compose.foundation.BorderStroke(
			width = 1.dp,
			color = PrimaryGreen.copy(alpha = 0.20f)
		)
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 16.dp, vertical = 14.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			// Avatar tròn
			Box(
				modifier = Modifier
					.size(44.dp)
					.background(PrimaryGreen.copy(alpha = 0.15f), CircleShape),
				contentAlignment = Alignment.Center
			) {
				Icon(
					imageVector = Icons.Outlined.Person,
					contentDescription = null,
					tint = PrimaryGreen,
					modifier = Modifier.size(24.dp)
				)
			}
			
			Spacer(modifier = Modifier.width(14.dp))
			
			// Tên & email
			Column(modifier = Modifier.weight(1f)) {
				Text(
					text = account.name,
					color = Color(0xFF1A1A1A),
					fontWeight = FontWeight.SemiBold,
					fontSize = 15.sp
				)
				Text(
					text = account.email,
					color = Color.Gray,
					fontSize = 12.sp,
					modifier = Modifier.padding(top = 2.dp)
				)
			}
			
			// Badge phương thức đăng nhập
			LoginMethodBadge(method = account.loginMethod)
		}
	}
}

// ─────────────────────────────────────────────────────────────
// Badge hiển thị "Google" hoặc "Email"
// ─────────────────────────────────────────────────────────────
@Composable
private fun LoginMethodBadge(method: String?) {
	val isGoogle = method == "GOOGLE"
	Surface(
		shape = RoundedCornerShape(20.dp),
		color = if (isGoogle) Color(0xFFE8F0FE) else Color(0xFFE8F5E9)
	) {
		Text(
			text = if (isGoogle) "Google" else "Email",
			color = if (isGoogle) Color(0xFF1A73E8) else PrimaryGreen,
			fontSize = 11.sp,
			fontWeight = FontWeight.Medium,
			modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
		)
	}
}