package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.IntegrationSyncService
import com.example.data.LocalIntegrationCredentials
import com.example.data.UserProfile
import com.example.data.UserViewMode
import com.example.notifications.NotificationHelper
import com.example.notifications.TpqWorkScheduler

@Composable
fun IntegrasiSettingsScreen(
    userProfile: UserProfile?,
    localCredentials: LocalIntegrationCredentials,
    isBusy: Boolean,
    onSaveSettings: (
        displayName: String,
        tpqName: String,
        mode: UserViewMode,
        biometricEnabled: Boolean,
        dailyReminderEnabled: Boolean,
        dailyHour: Int,
        dailyMinute: Int,
        weeklyDigestEnabled: Boolean,
        monthlyAutoReportEnabled: Boolean,
        appsScriptUrl: String?,
        githubRepo: String?,
        githubToken: String?
    ) -> Unit,
    onTestAppsScript: (String) -> Unit,
    onSyncToSpreadsheet: (url: String, pushToGithubViaScript: Boolean) -> Unit,
    onPushDirectToGitHub: (repo: String, token: String) -> Unit,
    onFeedback: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var displayName by remember(userProfile) { mutableStateOf(userProfile?.displayName ?: "") }
    var tpqName by remember(userProfile) { mutableStateOf(userProfile?.tpqName ?: "TPQ Nurul Qur'an") }
    var preferredMode by remember(userProfile) {
        mutableStateOf(UserViewMode.fromRaw(userProfile?.preferredMode))
    }
    var biometricEnabled by remember(userProfile) { mutableStateOf(userProfile?.biometricEnabled ?: true) }
    var dailyReminder by remember(userProfile) { mutableStateOf(userProfile?.dailyReminderEnabled ?: true) }
    var dailyHour by remember(userProfile) { mutableIntStateOf((userProfile?.dailyReminderHour ?: 16L).toInt()) }
    var dailyMinute by remember(userProfile) { mutableIntStateOf((userProfile?.dailyReminderMinute ?: 0L).toInt()) }
    var weeklyDigest by remember(userProfile) { mutableStateOf(userProfile?.weeklyDigestEnabled ?: true) }
    var monthlyAuto by remember(userProfile) { mutableStateOf(userProfile?.monthlyAutoReportEnabled ?: true) }

    var appsScriptUrl by remember(userProfile) { mutableStateOf(userProfile?.appsScriptUrl ?: "") }
    var githubRepo by remember(userProfile) { mutableStateOf(userProfile?.githubRepo ?: "") }
    var githubToken by remember(localCredentials) { mutableStateOf(localCredentials.githubToken) }
    var pushToGithubViaScript by remember { mutableStateOf(true) }
    var showCodeGsDialog by remember { mutableStateOf(false) }

    var hasNotificationPermission by remember {
        mutableStateOf(NotificationHelper.hasNotificationPermission(context))
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
        if (granted) {
            TpqWorkScheduler.syncSchedules(
                context, dailyReminder, dailyHour, dailyMinute, weeklyDigest, monthlyAuto
            )
            onFeedback("Izin notifikasi aktif! Jadwal pengingat otomatis telah dijalankan.")
        } else {
            onFeedback("Izin notifikasi ditolak. Aktifkan di Pengaturan perangkat agar pengingat muncul.")
        }
    }

    LaunchedEffect(dailyReminder, dailyHour, dailyMinute, weeklyDigest, monthlyAuto) {
        TpqWorkScheduler.syncSchedules(
            context, dailyReminder, dailyHour, dailyMinute, weeklyDigest, monthlyAuto
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section 1: Spreadsheet -> Extensions -> Apps Script -> GitHub
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.TableChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Spreadsheet → Ekstensi Apps Script → GitHub",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Alur Integrasi:\n1. Buat Google Spreadsheet baru.\n2. Klik Ekstensi → Apps Script.\n3. Salin kode Code.gs di bawah lalu Deploy sebagai Web App.\n4. Tempel URL Web App untuk sinkron otomatis ke Sheet & GitHub.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    FilledTonalButton(
                        onClick = { showCodeGsDialog = true },
                        modifier = Modifier.fillMaxWidth().testTag("view_code_gs_button")
                    ) {
                        Icon(Icons.Rounded.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Lihat & Salin Kode Apps Script (Code.gs)")
                    }

                    OutlinedTextField(
                        value = appsScriptUrl,
                        onValueChange = { appsScriptUrl = it },
                        label = { Text("URL Web App Apps Script") },
                        placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_apps_script_url")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = pushToGithubViaScript,
                            onCheckedChange = { pushToGithubViaScript = it }
                        )
                        Text(
                            text = "Minta Apps Script ikut push snapshot ke GitHub",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onTestAppsScript(appsScriptUrl) },
                            enabled = !isBusy && appsScriptUrl.isNotBlank(),
                            modifier = Modifier.weight(1f).testTag("test_apps_script_button")
                        ) {
                            Text("Uji Koneksi")
                        }
                        Button(
                            onClick = { onSyncToSpreadsheet(appsScriptUrl, pushToGithubViaScript) },
                            enabled = !isBusy && appsScriptUrl.isNotBlank(),
                            modifier = Modifier.weight(1f).testTag("sync_spreadsheet_button")
                        ) {
                            Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sinkron Sheet")
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Backup Langsung ke GitHub Repository",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = githubRepo,
                        onValueChange = { githubRepo = it },
                        label = { Text("Repositori GitHub (owner/nama-repo)") },
                        placeholder = { Text("tpq-nurul/rapor-santri-db") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_github_repo")
                    )
                    OutlinedTextField(
                        value = githubToken,
                        onValueChange = { githubToken = it },
                        label = { Text("GitHub Personal Access Token (disimpan lokal)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_github_token")
                    )
                    Button(
                        onClick = { onPushDirectToGitHub(githubRepo, githubToken) },
                        enabled = !isBusy && githubRepo.isNotBlank() && githubToken.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("push_github_button")
                    ) {
                        Icon(Icons.Rounded.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Push Snapshot Database ke GitHub")
                    }
                }
            }
        }

        // Section 2: Notifications & Reminders
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Notifikasi & Pengingat Otomatis",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            modifier = Modifier.fillMaxWidth().testTag("request_notif_permission_button")
                        ) {
                            Text("Izinkan Notifikasi Push Perangkat")
                        }
                    }

                    SwitchRow(
                        title = "Pengingat Setor Hafalan Harian (${String.format("%02d:%02d", dailyHour, dailyMinute)})",
                        subtitle = "Notifikasi harian jadwal setor hafalan santri",
                        checked = dailyReminder,
                        onCheckedChange = { dailyReminder = it }
                    )
                    SwitchRow(
                        title = "Notifikasi Progres Mingguan Otomatis",
                        subtitle = "Ringkasan perkembangan ayat & tajwid setiap pekan",
                        checked = weeklyDigest,
                        onCheckedChange = { weeklyDigest = it }
                    )
                    SwitchRow(
                        title = "Laporan Perkembangan Bulanan Otomatis",
                        subtitle = "Kirim rekap rapor bulanan otomatis ke Wali Murid",
                        checked = monthlyAuto,
                        onCheckedChange = { monthlyAuto = it }
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    NotificationHelper.sendNotification(
                                        context = context,
                                        channelId = NotificationHelper.CHANNEL_DAILY,
                                        notificationId = 3001,
                                        title = "Pengingat Setor Hafalan TPQ",
                                        body = "Saatnya setor hafalan harian santri di $tpqName!"
                                    )
                                    onFeedback("Notifikasi pengingat harian dikirim!")
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("test_daily_notif_button")
                        ) {
                            Text("Tes Harian")
                        }
                        OutlinedButton(
                            onClick = {
                                if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    NotificationHelper.sendNotification(
                                        context = context,
                                        channelId = NotificationHelper.CHANNEL_WEEKLY,
                                        notificationId = 3002,
                                        title = "Progres Mingguan Santri",
                                        body = "Rekap mingguan hafalan & tajwid telah siap dilihat di Rapor TPQ."
                                    )
                                    onFeedback("Notifikasi progres mingguan dikirim!")
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("test_weekly_notif_button")
                        ) {
                            Text("Tes Mingguan")
                        }
                    }
                }
            }
        }

        // Section 3: Profile, Role View Mode & Biometric Security
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Fingerprint,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Profil Akun & Otentikasi Biometrik",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Nama Guru / Wali Murid") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tpqName,
                        onValueChange = { tpqName = it },
                        label = { Text("Nama Lembaga TPQ") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Mode Tampilan Aplikasi", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        UserViewMode.entries.forEach { mode ->
                            FilterChip(
                                selected = preferredMode == mode,
                                onClick = { preferredMode = mode },
                                label = { Text(mode.label) },
                                modifier = Modifier.testTag("mode_chip_${mode.name}")
                            )
                        }
                    }

                    SwitchRow(
                        title = "Kunci Otentikasi Biometrik",
                        subtitle = "Wajibkan sidik jari / wajah / PIN saat membuka aplikasi",
                        checked = biometricEnabled,
                        onCheckedChange = { biometricEnabled = it }
                    )

                    Button(
                        onClick = {
                            onSaveSettings(
                                displayName, tpqName, preferredMode, biometricEnabled,
                                dailyReminder, dailyHour, dailyMinute, weeklyDigest, monthlyAuto,
                                appsScriptUrl, githubRepo, githubToken
                            )
                        },
                        enabled = !isBusy && displayName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_settings_button")
                    ) {
                        Text("Simpan Semua Pengaturan")
                    }

                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Keluar Akun Google")
                    }
                }
            }
        }
    }

    if (showCodeGsDialog) {
        AlertDialog(
            onDismissRequest = { showCodeGsDialog = false },
            title = { Text("Kode Google Apps Script (Code.gs)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Buka Google Spreadsheet → Ekstensi → Apps Script → tempel kode ini → Deploy sebagai Web App:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                    ) {
                        Text(
                            text = IntegrationSyncService.APPS_SCRIPT_CODE_GS,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .horizontalScroll(rememberScrollState())
                                .padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboard.setText(AnnotatedString(IntegrationSyncService.APPS_SCRIPT_CODE_GS))
                        onFeedback("Kode Apps Script Code.gs disalin ke clipboard!")
                        showCodeGsDialog = false
                    }
                ) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Salin Kode")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCodeGsDialog = false }) { Text("Tutup") }
            }
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
