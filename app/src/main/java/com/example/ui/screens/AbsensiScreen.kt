package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AbsensiRecord
import com.example.data.AbsensiStatus
import com.example.data.Santri
import com.example.data.UserViewMode
import com.example.ui.UiState
import com.example.ui.theme.AlphaRed
import com.example.ui.theme.HadirGreen
import com.example.ui.theme.IzinBlue
import com.example.ui.theme.SakitAmber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun AbsensiScreen(
    santriList: List<Santri>,
    selectedSantri: Santri?,
    absensiListState: UiState<List<AbsensiRecord>>,
    viewMode: UserViewMode,
    currentUserId: String,
    onRecordAbsensi: (santri: Santri, dateKey: String, status: AbsensiStatus, note: String) -> Unit,
    onMarkAllHadir: (santriList: List<Santri>, dateKey: String) -> Unit
) {
    var dayOffset by remember { mutableIntStateOf(0) }
    var noteInput by remember { mutableStateOf("") }

    val selectedDateKey = remember(dayOffset) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, dayOffset)
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }
    val selectedDateDisplay = remember(dayOffset) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, dayOffset)
        SimpleDateFormat("EEEE, dd MMM yyyy", Locale("id", "ID")).format(cal.time)
    }

    val absensiList = (absensiListState as? UiState.Success)?.data.orEmpty()
    val todayRecord = absensiList.firstOrNull { it.dateKey == selectedDateKey }
    val isTeacher = selectedSantri?.teacherId == currentUserId && viewMode == UserViewMode.GURU

    val hadirCount = absensiList.count { it.status == AbsensiStatus.HADIR.name }
    val izinCount = absensiList.count { it.status == AbsensiStatus.IZIN.name }
    val sakitCount = absensiList.count { it.status == AbsensiStatus.SAKIT.name }
    val alphaCount = absensiList.count { it.status == AbsensiStatus.ALPHA.name }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Kehadiran Harian Santri",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = selectedSantri?.name ?: "Belum ada santri dipilih",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Date Stepper
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { dayOffset-- }) {
                                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Hari sebelumnya")
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = selectedDateDisplay,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (dayOffset == 0) "Hari Ini ($selectedDateKey)" else selectedDateKey,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = { if (dayOffset < 0) dayOffset++ },
                                enabled = dayOffset < 0
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Hari berikutnya")
                            }
                        }
                    }

                    // Attendance Stats Summary
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AttendanceStatPill("Hadir", hadirCount, HadirGreen, Modifier.weight(1f))
                        AttendanceStatPill("Izin", izinCount, IzinBlue, Modifier.weight(1f))
                        AttendanceStatPill("Sakit", sakitCount, SakitAmber, Modifier.weight(1f))
                        AttendanceStatPill("Alpha", alphaCount, AlphaRed, Modifier.weight(1f))
                    }
                }
            }
        }

        if (selectedSantri != null && isTeacher) {
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Catat Absensi: ${selectedSantri.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = noteInput,
                            onValueChange = { noteInput = it },
                            label = { Text("Keterangan (opsional, cth: Hadir tepat waktu)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AbsensiStatus.entries.forEach { st ->
                                val isCurrent = todayRecord?.status == st.name
                                val activeColor = statusColor(st)
                                FilterChip(
                                    selected = isCurrent,
                                    onClick = {
                                        onRecordAbsensi(selectedSantri, selectedDateKey, st, noteInput)
                                    },
                                    label = { Text(st.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = activeColor.copy(alpha = 0.16f),
                                        selectedLabelColor = activeColor
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("absensi_chip_${st.name}")
                                )
                            }
                        }

                        if (santriList.count { it.teacherId == currentUserId } > 1) {
                            Button(
                                onClick = { onMarkAllHadir(santriList, selectedDateKey) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("absensi_hadir_semua_button")
                            ) {
                                Icon(Icons.Rounded.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Tandai Semua Santri Hadir Hari Ini")
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Riwayat Kehadiran (${absensiList.size} hari)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (absensiList.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Belum ada catatan absensi untuk santri ini. Pilih status Hadir/Izin/Sakit/Alpha di atas untuk mencatat kehadiran harian.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(absensiList, key = { it.id }) { rec ->
                val st = AbsensiStatus.fromRaw(rec.status)
                val color = statusColor(st)
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = rec.dateKey,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (rec.note.isNotBlank()) {
                                Text(
                                    text = rec.note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Surface(
                            color = color.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = st.label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = color,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceStatPill(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

private fun statusColor(status: AbsensiStatus): Color = when (status) {
    AbsensiStatus.HADIR -> HadirGreen
    AbsensiStatus.IZIN -> IzinBlue
    AbsensiStatus.SAKIT -> SakitAmber
    AbsensiStatus.ALPHA -> AlphaRed
}
