package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.HafalanStatus
import com.example.data.NilaiRecord
import com.example.data.QuranSurah
import com.example.data.QuranSurahCatalog
import com.example.data.Santri
import com.example.data.UserViewMode
import com.example.ui.UiState
import com.example.ui.theme.AmiriArabicFontFamily
import com.example.ui.theme.MumtazGreen
import com.example.ui.theme.MumtazSurface
import com.example.ui.theme.MurajaahAmber
import com.example.ui.theme.MurajaahSurface
import com.example.ui.theme.ProsesSurface
import com.example.ui.theme.ProsesTeal
import kotlin.math.roundToInt

@Composable
fun HafalanTajwidScreen(
    selectedSantri: Santri?,
    nilaiListState: UiState<List<NilaiRecord>>,
    viewMode: UserViewMode,
    currentUserId: String,
    onRecordNilai: (
        santri: Santri,
        surah: QuranSurah,
        ayahFrom: Int,
        ayahTo: Int,
        status: HafalanStatus,
        kelancaran: Int,
        makhraj: Int,
        hukumTajwid: Int,
        madQalqalah: Int,
        fashohah: Int,
        notes: String
    ) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf("SEMUA") } // SEMUA, JUZ_30, SELESAI
    var gradingSurah by remember { mutableStateOf<QuranSurah?>(null) }

    val nilaiList = (nilaiListState as? UiState.Success)?.data.orEmpty()
    val latestNilaiBySurah = remember(nilaiList) {
        nilaiList.groupBy { it.surahNumber.toInt() }.mapValues { (_, list) -> list.first() }
    }

    val filteredSurahs = remember(searchQuery, filterMode, latestNilaiBySurah) {
        QuranSurahCatalog.allSurahs.filter { surah ->
            val matchesQuery = searchQuery.isBlank() ||
                surah.nameLatin.contains(searchQuery, ignoreCase = true) ||
                surah.meaningId.contains(searchQuery, ignoreCase = true) ||
                surah.number.toString() == searchQuery.trim()
            val matchesFilter = when (filterMode) {
                "JUZ_30" -> surah.juzStart == 30 || surah.juzEnd == 30
                "SELESAI" -> latestNilaiBySurah[surah.number] != null
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    val canGrade = selectedSantri != null &&
        selectedSantri.teacherId == currentUserId &&
        viewMode == UserViewMode.GURU

    Column(modifier = Modifier.fillMaxSize()) {
        // Header + Search + Filters
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Kurikulum 114 Surat Al-Qur'an",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = selectedSantri?.let {
                            "Santri: ${it.name} • ${latestNilaiBySurah.size}/114 surat ternilai"
                        } ?: "Pilih atau tambahkan santri di Beranda terlebih dahulu",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari surat (cth: An-Naba, Al-Mulk, 36)...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("surah_search_input")
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = filterMode == "SEMUA",
                        onClick = { filterMode = "SEMUA" },
                        label = { Text("Semua (114)") }
                    )
                }
                item {
                    FilterChip(
                        selected = filterMode == "JUZ_30",
                        onClick = { filterMode = "JUZ_30" },
                        label = { Text("Juz 'Amma (37)") }
                    )
                }
                item {
                    FilterChip(
                        selected = filterMode == "SELESAI",
                        onClick = { filterMode = "SELESAI" },
                        label = { Text("Sudah Disetor (${latestNilaiBySurah.size})") }
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredSurahs, key = { it.number }) { surah ->
                val latest = latestNilaiBySurah[surah.number]
                val statusEnum = latest?.let { HafalanStatus.fromRaw(it.hafalanStatus) }
                val (badgeBg, badgeFg) = when (statusEnum) {
                    HafalanStatus.MUMTAZ -> MumtazSurface to MumtazGreen
                    HafalanStatus.MURAJAAH -> MurajaahSurface to MurajaahAmber
                    HafalanStatus.PROSES -> ProsesSurface to ProsesTeal
                    null -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                }

                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = canGrade) { gradingSurah = surah }
                        .testTag("surah_card_${surah.number}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (latest != null) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.primaryContainer
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${surah.number}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (latest != null) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = surah.nameLatin,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = surah.nameArabic,
                                    fontFamily = AmiriArabicFontFamily,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = "${surah.meaningId} • ${surah.totalAyah} Ayat • ${surah.juzLabel} (${surah.revelationPlace})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(color = badgeBg, shape = RoundedCornerShape(8.dp)) {
                                    Text(
                                        text = if (latest != null) {
                                            "${statusEnum?.label} • Nilai ${latest.totalScore}"
                                        } else "Belum Setor",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = badgeFg,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                if (canGrade) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.EditNote,
                                            contentDescription = "Input nilai ${surah.nameLatin}",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Nilai",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
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

    val targetSantri = selectedSantri
    val targetSurah = gradingSurah
    if (targetSantri != null && targetSurah != null) {
        InputNilaiDialog(
            santri = targetSantri,
            surah = targetSurah,
            existingNilai = latestNilaiBySurah[targetSurah.number],
            onDismiss = { gradingSurah = null },
            onSubmit = { ayahFrom, ayahTo, status, kel, mak, huk, mad, fas, notes ->
                onRecordNilai(targetSantri, targetSurah, ayahFrom, ayahTo, status, kel, mak, huk, mad, fas, notes)
                gradingSurah = null
            }
        )
    }
}

@Composable
private fun InputNilaiDialog(
    santri: Santri,
    surah: QuranSurah,
    existingNilai: NilaiRecord?,
    onDismiss: () -> Unit,
    onSubmit: (Int, Int, HafalanStatus, Int, Int, Int, Int, Int, String) -> Unit
) {
    var ayahFromText by remember { mutableStateOf((existingNilai?.ayahFrom ?: 1L).toString()) }
    var ayahToText by remember { mutableStateOf((existingNilai?.ayahTo ?: surah.totalAyah.toLong()).toString()) }
    var selectedStatus by remember {
        mutableStateOf(
            existingNilai?.let { HafalanStatus.fromRaw(it.hafalanStatus) } ?: HafalanStatus.MUMTAZ
        )
    }
    var kelancaran by remember { mutableFloatStateOf((existingNilai?.scoreKelancaran ?: 90L).toFloat()) }
    var makhraj by remember { mutableFloatStateOf((existingNilai?.scoreMakhraj ?: 88L).toFloat()) }
    var hukumTajwid by remember { mutableFloatStateOf((existingNilai?.scoreHukumTajwid ?: 86L).toFloat()) }
    var madQalqalah by remember { mutableFloatStateOf((existingNilai?.scoreMadQalqalah ?: 88L).toFloat()) }
    var fashohah by remember { mutableFloatStateOf((existingNilai?.scoreFashohah ?: 90L).toFloat()) }
    var notes by remember { mutableStateOf(existingNilai?.notes ?: "") }

    val avgScore = remember(kelancaran, makhraj, hukumTajwid, madQalqalah, fashohah) {
        ((kelancaran + makhraj + hukumTajwid + madQalqalah + fashohah) / 5f).roundToInt()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Nilai ${surah.nameLatin} (${surah.nameArabic})")
                Text(
                    text = "Santri: ${santri.name} • ${surah.totalAyah} Ayat",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ayahFromText,
                        onValueChange = { ayahFromText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Ayat Dari") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = ayahToText,
                        onValueChange = { ayahToText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Sampai") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Status Hafalan", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    HafalanStatus.entries.forEach { st ->
                        FilterChip(
                            selected = selectedStatus == st,
                            onClick = { selectedStatus = st },
                            label = {
                                Text(
                                    when (st) {
                                        HafalanStatus.MUMTAZ -> "Mumtaz"
                                        HafalanStatus.MURAJAAH -> "Muraja'ah"
                                        HafalanStatus.PROSES -> "Proses"
                                    }
                                )
                            }
                        )
                    }
                }

                ScoreSliderRow("Kelancaran Hafalan", kelancaran) { kelancaran = it }
                ScoreSliderRow("Makharijul Huruf", makhraj) { makhraj = it }
                ScoreSliderRow("Hukum Nun/Mim Sukun", hukumTajwid) { hukumTajwid = it }
                ScoreSliderRow("Mad & Qalqalah", madQalqalah) { madQalqalah = it }
                ScoreSliderRow("Fashohah & Adab", fashohah) { fashohah = it }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Nilai Akhir Rata-Rata: $avgScore / 100",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan Ustadz/Ustadzah") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val from = (ayahFromText.toIntOrNull() ?: 1).coerceIn(1, surah.totalAyah)
                    val to = (ayahToText.toIntOrNull() ?: surah.totalAyah).coerceIn(from, surah.totalAyah)
                    onSubmit(
                        from, to, selectedStatus,
                        kelancaran.roundToInt(), makhraj.roundToInt(),
                        hukumTajwid.roundToInt(), madQalqalah.roundToInt(),
                        fashohah.roundToInt(), notes
                    )
                },
                modifier = Modifier.testTag("submit_nilai_button")
            ) {
                Text("Simpan Real-Time")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
private fun ScoreSliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(
                text = "${value.roundToInt()}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 50f..100f
        )
    }
}
