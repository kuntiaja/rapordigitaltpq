package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.BadgeCatalog
import com.example.data.MonthlyReport
import com.example.data.Santri
import com.example.data.UserViewMode
import com.example.notifications.NotificationHelper
import com.example.ui.UiState
import com.example.ui.theme.GoldTertiary
import com.example.ui.theme.MumtazGreen
import com.example.ui.theme.MumtazSurface

@Composable
fun GamifikasiLaporanScreen(
    selectedSantri: Santri?,
    laporanListState: UiState<List<MonthlyReport>>,
    viewMode: UserViewMode,
    currentUserId: String,
    onGenerateMonthlyReport: (santri: Santri, note: String, onDone: (MonthlyReport) -> Unit) -> Unit,
    onAcknowledgeReport: (santriId: String, reportId: String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var teacherNoteInput by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Badge & XP Santri") },
                modifier = Modifier.testTag("tab_badges")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Laporan Bulanan") },
                modifier = Modifier.testTag("tab_laporan")
            )
        }

        if (selectedSantri == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Pilih atau tambahkan santri di menu Beranda untuk melihat badge pencapaian dan laporan bulanan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Column
        }

        if (selectedTab == 0) {
            val xp = selectedSantri.xpPoints.toInt()
            val (levelTitle, levelNum) = BadgeCatalog.levelTitle(xp)
            val nextTarget = when (levelNum) {
                1 -> 150
                2 -> 400
                3 -> 800
                4 -> 1500
                else -> 2500
            }
            val progress = (xp.toFloat() / nextTarget.toFloat()).coerceIn(0f, 1f)
            val unlockedSet = selectedSantri.unlockedBadges.toSet()

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_hafalan_badge),
                                contentDescription = "Lencana Prestasi Santri",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "${selectedSantri.name} • Lv.$levelNum",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$levelTitle ($xp / $nextTarget XP)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50.dp))
                                )
                                Text(
                                    text = "${unlockedSet.size} dari ${BadgeCatalog.allBadges.size} Badge Pencapaian Terbuka",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                items(BadgeCatalog.allBadges, key = { it.id }) { badge ->
                    val unlocked = unlockedSet.contains(badge.id)
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = if (unlocked) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(if (unlocked) 1f else 0.72f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (unlocked) GoldTertiary.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (unlocked) Icons.Rounded.EmojiEvents else Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint = if (unlocked) GoldTertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = badge.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = badge.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = if (unlocked) MumtazSurface else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (unlocked) "+${badge.xpReward} XP" else badge.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (unlocked) MumtazGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            val laporanList = (laporanListState as? UiState.Success)?.data.orEmpty()
            val isTeacher = selectedSantri.teacherId == currentUserId && viewMode == UserViewMode.GURU

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isTeacher) {
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
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Terbitkan Laporan Bulanan ke Wali Murid",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Merangkum otomatis total setoran, jumlah surat Mumtaz, rata-rata nilai tajwid, dan rekap kehadiran bulan ini untuk dikirim ke akun Wali Murid.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedTextField(
                                    value = teacherNoteInput,
                                    onValueChange = { teacherNoteInput = it },
                                    label = { Text("Catatan Ustadz/Ustadzah untuk Wali Murid") },
                                    maxLines = 3,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        onGenerateMonthlyReport(selectedSantri, teacherNoteInput) { report ->
                                            NotificationHelper.sendNotification(
                                                context = context,
                                                channelId = NotificationHelper.CHANNEL_MONTHLY,
                                                notificationId = 2001,
                                                title = "Rapor Bulanan ${report.santriName}",
                                                body = "Laporan ${report.monthTitle}: ${report.completedSurahs} surat Mumtaz, Tajwid ${report.avgTajwidScore.toInt()}, Kehadiran ${report.attendancePercent.toInt()}%."
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("generate_monthly_report_button")
                                ) {
                                    Icon(Icons.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Generate & Kirim Otomatis ke Wali")
                                }
                            }
                        }
                    }
                }

                if (laporanList.isEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Belum ada laporan bulanan yang diterbitkan untuk ${selectedSantri.name}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(laporanList, key = { it.id }) { report ->
                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Rapor ${report.monthTitle}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        color = if (report.parentAcknowledged) MumtazSurface else MaterialTheme.colorScheme.tertiaryContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = if (report.parentAcknowledged) "Dibaca Wali" else "Terkirim ke Wali",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (report.parentAcknowledged) MumtazGreen else MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Setoran: ${report.totalSetoran}x • Surat Mumtaz: ${report.completedSurahs}/114 • Tajwid: ${report.avgTajwidScore.toInt()} • Kehadiran: ${report.attendancePercent.toInt()}% (H:${report.hadirDays} I:${report.izinDays} S:${report.sakitDays} A:${report.alphaDays})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "\"${report.teacherNote}\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilledTonalButton(
                                        onClick = {
                                            val shareText = """
                                                *LAPORAN PERKEMBANGAN TPQ - ${report.monthTitle}*
                                                Santri: ${report.santriName}
                                                • Total Setoran: ${report.totalSetoran} kali
                                                • Surat Mumtaz: ${report.completedSurahs}/114
                                                • Rata-rata Hafalan: ${report.avgHafalanScore.toInt()}
                                                • Rata-rata Tajwid: ${report.avgTajwidScore.toInt()}
                                                • Kehadiran: ${report.attendancePercent.toInt()}% (H:${report.hadirDays}, I:${report.izinDays}, S:${report.sakitDays}, A:${report.alphaDays})
                                                Catatan Ustadz: ${report.teacherNote}
                                            """.trimIndent()
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, shareText)
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Kirim Rapor ke Wali Murid"))
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Bagikan / WA")
                                    }
                                    if (!report.parentAcknowledged) {
                                        Button(
                                            onClick = { onAcknowledgeReport(selectedSantri.id, report.id) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Konfirmasi Baca")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
