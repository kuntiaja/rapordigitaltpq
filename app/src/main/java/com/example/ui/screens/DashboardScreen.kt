package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.BadgeCatalog
import com.example.data.HafalanStatus
import com.example.data.NilaiRecord
import com.example.data.Santri
import com.example.data.UserProfile
import com.example.data.UserViewMode
import com.example.ui.UiState
import com.example.ui.components.SurahMasteryRing
import com.example.ui.components.TajwidBreakdownChart
import com.example.ui.theme.MumtazGreen
import com.example.ui.theme.MumtazSurface

@Composable
fun DashboardScreen(
    userProfile: UserProfile?,
    santriListState: UiState<List<Santri>>,
    selectedSantri: Santri?,
    nilaiListState: UiState<List<NilaiRecord>>,
    currentUserId: String,
    onSelectSantri: (String) -> Unit,
    onCreateSantri: (name: String, nis: String, halaqah: String, parentName: String, parentContact: String) -> Unit,
    onLinkParentCode: (String) -> Unit,
    onNavigateToHafalan: () -> Unit,
    onNavigateToAbsensi: () -> Unit,
    onCopyFeedback: (String) -> Unit
) {
    var showAddSantriDialog by remember { mutableStateOf(false) }
    var showLinkParentDialog by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val viewMode = UserViewMode.fromRaw(userProfile?.preferredMode)
    val tpqName = userProfile?.tpqName?.ifBlank { "TPQ Nurul Qur'an" } ?: "TPQ Nurul Qur'an"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Banner Header
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(152.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_tpq_hero_banner),
                        contentDescription = "Banner TPQ",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF062C25).copy(alpha = 0.86f),
                                        Color(0xFF0D4F44).copy(alpha = 0.55f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiary,
                                shape = RoundedCornerShape(50.dp)
                            ) {
                                Text(
                                    text = viewMode.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = tpqName,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        Column {
                            Text(
                                text = "Assalamu'alaikum, ${userProfile?.displayName?.ifBlank { "Sahabat TPQ" } ?: "Sahabat TPQ"}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Pantau hafalan 114 surat, tajwid & kehadiran secara real-time",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }

        // Santri Switcher & Action Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daftar Santri",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showLinkParentDialog = true },
                        modifier = Modifier.testTag("link_parent_code_button")
                    ) {
                        Icon(Icons.Rounded.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kode Wali")
                    }
                    if (viewMode == UserViewMode.GURU) {
                        FilledTonalButton(
                            onClick = { showAddSantriDialog = true },
                            modifier = Modifier.testTag("add_santri_button")
                        ) {
                            Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tambah")
                        }
                    }
                }
            }
        }

        when (santriListState) {
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            is UiState.Error -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = santriListState.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            is UiState.Success -> {
                val santriList = santriListState.data
                if (santriList.isEmpty()) {
                    item {
                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.School,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "Belum Ada Data Santri",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (viewMode == UserViewMode.GURU) {
                                        "Tambahkan santri pertama Anda untuk mulai mencatat hafalan 114 surat, nilai tajwid, dan absensi harian."
                                    } else {
                                        "Masukkan Kode Akses Santri yang dibagikan Ustadz/Guru TPQ untuk melihat rapor putra-putri Anda."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    if (viewMode == UserViewMode.GURU) {
                                        Button(
                                            onClick = { showAddSantriDialog = true },
                                            modifier = Modifier.testTag("empty_add_santri_button")
                                        ) {
                                            Icon(Icons.Rounded.Add, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Tambah Santri")
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = { showLinkParentDialog = true },
                                        modifier = Modifier.testTag("empty_link_code_button")
                                    ) {
                                        Text("Masukkan Kode Wali")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(santriList, key = { it.id }) { santri ->
                                val isSelected = santri.id == selectedSantri?.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSelectSantri(santri.id) },
                                    label = { Text("${santri.name} • ${santri.halaqahClass}") },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    modifier = Modifier.testTag("santri_chip_${santri.id}")
                                )
                            }
                        }
                    }

                    selectedSantri?.let { santri ->
                        val levelPair = BadgeCatalog.levelTitle(santri.xpPoints.toInt())
                        val nilaiList = (nilaiListState as? UiState.Success)?.data.orEmpty()

                        // Selected Santri Profile & Share Code Card
                        item {
                            Card(
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        SurahMasteryRing(
                                            completedCount = santri.completedSurahCount.toInt(),
                                            totalCount = 114
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = santri.name,
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "NIS: ${santri.nis} • ${santri.halaqahClass}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Surface(
                                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = "Level ${levelPair.second}: ${levelPair.first} (${santri.xpPoints} XP)",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "Kode Wali: ${santri.shareCode}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                IconButton(
                                                    onClick = {
                                                        clipboard.setText(AnnotatedString(santri.shareCode))
                                                        onCopyFeedback("Kode akses ${santri.shareCode} disalin!")
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.ContentCopy,
                                                        contentDescription = "Salin kode akses wali",
                                                        modifier = Modifier.size(16.dp),
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // 3 KPI Summary Pills
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        KpiMiniCard(
                                            label = "Rata Tajwid",
                                            value = "${santri.avgTajwidScore.toInt()}",
                                            icon = Icons.Rounded.AutoAwesome,
                                            modifier = Modifier.weight(1f)
                                        )
                                        KpiMiniCard(
                                            label = "Kehadiran",
                                            value = "${santri.attendancePercent.toInt()}%",
                                            icon = Icons.Rounded.CheckCircle,
                                            modifier = Modifier.weight(1f)
                                        )
                                        KpiMiniCard(
                                            label = "Streak",
                                            value = "${santri.streakDays} Hr",
                                            icon = Icons.Rounded.LocalFireDepartment,
                                            modifier = Modifier.weight(1f)
                                        )
                                        KpiMiniCard(
                                            label = "Badge",
                                            value = "${santri.unlockedBadges.size}/8",
                                            icon = Icons.Rounded.EmojiEvents,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        // Tajwid Analytics Card
                        item {
                            Card(
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Statistik 5 Pilar Tajwid",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${nilaiList.size} Setoran",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    TajwidBreakdownChart(nilaiList = nilaiList)
                                    if (santri.teacherId == currentUserId && viewMode == UserViewMode.GURU) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Button(
                                                onClick = onNavigateToHafalan,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(48.dp)
                                                    .testTag("quick_input_nilai_button")
                                            ) {
                                                Icon(Icons.Rounded.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Input Hafalan")
                                            }
                                            FilledTonalButton(
                                                onClick = onNavigateToAbsensi,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(48.dp)
                                                    .testTag("quick_absensi_button")
                                            ) {
                                                Text("Catat Absensi")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Recent Setoran History
                        item {
                            Text(
                                text = "Riwayat Setoran Terbaru",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (nilaiList.isEmpty()) {
                            item {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Belum ada setoran hafalan tercatat untuk ${santri.name}. Buka tab 114 Surat untuk menilai setoran pertama.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        } else {
                            items(nilaiList.take(5), key = { it.id }) { itemNilai ->
                                Card(
                                    shape = MaterialTheme.shapes.medium,
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${itemNilai.surahNumber}",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${itemNilai.surahName} (Ayat ${itemNilai.ayahFrom}-${itemNilai.ayahTo})",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "${itemNilai.dateLabel} • ${HafalanStatus.fromRaw(itemNilai.hafalanStatus).label}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Surface(
                                            color = MumtazSurface,
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(
                                                text = "${itemNilai.totalScore}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MumtazGreen,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
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

    if (showAddSantriDialog) {
        AddSantriDialog(
            onDismiss = { showAddSantriDialog = false },
            onConfirm = { name, nis, halaqah, pName, pContact ->
                onCreateSantri(name, nis, halaqah, pName, pContact)
                showAddSantriDialog = false
            }
        )
    }

    if (showLinkParentDialog) {
        LinkParentCodeDialog(
            onDismiss = { showLinkParentDialog = false },
            onConfirm = { code ->
                onLinkParentCode(code)
                showLinkParentDialog = false
            }
        )
    }
}

@Composable
private fun KpiMiniCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AddSantriDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var nis by remember { mutableStateOf("") }
    var halaqah by remember { mutableStateOf("Juz 'Amma A") }
    var parentName by remember { mutableStateOf("") }
    var parentContact by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Santri Baru") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Lengkap Santri *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_santri_name")
                )
                OutlinedTextField(
                    value = nis,
                    onValueChange = { nis = it },
                    label = { Text("NIS (cth: TPQ-001) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_santri_nis")
                )
                OutlinedTextField(
                    value = halaqah,
                    onValueChange = { halaqah = it },
                    label = { Text("Kelas / Halaqah *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_santri_halaqah")
                )
                OutlinedTextField(
                    value = parentName,
                    onValueChange = { parentName = it },
                    label = { Text("Nama Wali Murid") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = parentContact,
                    onValueChange = { parentContact = it },
                    label = { Text("No. WA / Email Wali") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, nis, halaqah, parentName, parentContact) },
                enabled = name.isNotBlank() && nis.isNotBlank() && halaqah.isNotBlank(),
                modifier = Modifier.testTag("confirm_add_santri_button")
            ) {
                Text("Simpan Santri")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
private fun LinkParentCodeDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tautkan Akun Wali Murid") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Masukkan Kode Akses Santri (contoh: TPQ-A1B2C) yang diberikan oleh Ustadz/Guru TPQ.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("Kode Akses Santri") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_share_code")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(code) },
                enabled = code.trim().length >= 6,
                modifier = Modifier.testTag("confirm_link_code_button")
            ) {
                Text("Tautkan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
