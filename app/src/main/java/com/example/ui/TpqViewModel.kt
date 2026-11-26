package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AbsensiRecord
import com.example.data.AbsensiStatus
import com.example.data.HafalanStatus
import com.example.data.IntegrationSyncService
import com.example.data.LocalIntegrationCredentials
import com.example.data.MonthlyReport
import com.example.data.NilaiRecord
import com.example.data.QuranSurah
import com.example.data.Santri
import com.example.data.TpqRepository
import com.example.data.UserProfile
import com.example.data.UserViewMode
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@OptIn(ExperimentalCoroutinesApi::class)
class TpqViewModel(
    private val repository: TpqRepository,
    private val integrationService: IntegrationSyncService,
    val currentUserId: String,
    val initialDisplayName: String,
    val initialEmail: String
) : ViewModel() {

    private val _selectedSantriId = MutableStateFlow<String?>(null)
    val selectedSantriId: StateFlow<String?> = _selectedSantriId.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
        .catch { error ->
            unwrapFirestoreOrRethrow(error, "observeUserProfile")
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val santriListState: StateFlow<UiState<List<Santri>>> = repository.observeSantriList(currentUserId)
        .toUiState("Gagal memuat daftar santri")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val selectedSantri: StateFlow<Santri?> = combine(santriListState, _selectedSantriId) { state, selectedId ->
        val list = (state as? UiState.Success)?.data.orEmpty()
        list.firstOrNull { it.id == selectedId } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val nilaiListState: StateFlow<UiState<List<NilaiRecord>>> = selectedSantri
        .flatMapLatest { santri ->
            if (santri == null) flowOf(UiState.Success(emptyList()))
            else repository.observeNilai(santri.id, currentUserId).toUiState("Gagal memuat nilai hafalan")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val absensiListState: StateFlow<UiState<List<AbsensiRecord>>> = selectedSantri
        .flatMapLatest { santri ->
            if (santri == null) flowOf(UiState.Success(emptyList()))
            else repository.observeAbsensi(santri.id, currentUserId).toUiState("Gagal memuat data absensi")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val laporanListState: StateFlow<UiState<List<MonthlyReport>>> = selectedSantri
        .flatMapLatest { santri ->
            if (santri == null) flowOf(UiState.Success(emptyList()))
            else repository.observeLaporan(santri.id, currentUserId).toUiState("Gagal memuat laporan bulanan")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val localCredentials: StateFlow<LocalIntegrationCredentials> = integrationService.localCredentials
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), LocalIntegrationCredentials())

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(currentUserId, initialDisplayName, initialEmail)
        }
        // Automatically ensure linked parents have memberUids on subcollections
        viewModelScope.launch {
            santriListState.collect { state ->
                if (state is UiState.Success) {
                    state.data
                        .filter { it.teacherId == currentUserId && it.memberUids.size > 1 }
                        .forEach { repository.syncMemberUidsToSubcollections(it) }
                }
            }
        }
    }

    fun selectSantri(santriId: String) {
        _selectedSantriId.value = santriId
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun showFeedback(message: String) {
        _feedbackMessage.value = message
    }

    fun createSantri(
        name: String,
        nis: String,
        halaqahClass: String,
        parentName: String,
        parentContact: String
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val teacherName = userProfile.value?.displayName?.ifBlank { initialDisplayName } ?: initialDisplayName
            val res = repository.createSantri(
                name = name,
                nis = nis,
                halaqahClass = halaqahClass,
                teacherName = teacherName.ifBlank { "Ustadz TPQ" },
                parentName = parentName,
                parentContact = parentContact
            )
            _isBusy.value = false
            res.onSuccess { newId ->
                _selectedSantriId.value = newId
                _feedbackMessage.value = "Santri \"$name\" berhasil ditambahkan."
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Gagal menambah santri."
            }
        }
    }

    fun linkParentByShareCode(code: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.linkSantriByShareCode(code)
            _isBusy.value = false
            res.onSuccess { msg ->
                _feedbackMessage.value = msg
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Kode akses tidak valid."
            }
        }
    }

    fun recordNilai(
        santri: Santri,
        surah: QuranSurah,
        ayahFrom: Int,
        ayahTo: Int,
        hafalanStatus: HafalanStatus,
        kelancaran: Int,
        makhraj: Int,
        hukumTajwid: Int,
        madQalqalah: Int,
        fashohah: Int,
        notes: String
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.recordNilaiAndUpdateStats(
                santri = santri,
                surah = surah,
                ayahFrom = ayahFrom,
                ayahTo = ayahTo,
                hafalanStatus = hafalanStatus,
                kelancaran = kelancaran,
                makhraj = makhraj,
                hukumTajwid = hukumTajwid,
                madQalqalah = madQalqalah,
                fashohah = fashohah,
                notes = notes
            )
            _isBusy.value = false
            res.onSuccess {
                _feedbackMessage.value = "Nilai ${surah.nameLatin} untuk ${santri.name} tersimpan real-time."
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Gagal menyimpan nilai."
            }
        }
    }

    fun recordAbsensi(
        santri: Santri,
        dateKey: String,
        status: AbsensiStatus,
        note: String
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.recordAbsensiAndUpdateStats(santri, dateKey, status, note)
            _isBusy.value = false
            res.onSuccess {
                _feedbackMessage.value = "Kehadiran ${santri.name} ($dateKey): ${status.label}"
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Gagal mencatat kehadiran."
            }
        }
    }

    fun markAllHadir(santriList: List<Santri>, dateKey: String) {
        viewModelScope.launch {
            _isBusy.value = true
            var successCount = 0
            santriList.filter { it.teacherId == currentUserId }.forEach { santri ->
                val res = repository.recordAbsensiAndUpdateStats(santri, dateKey, AbsensiStatus.HADIR, "Hadir serentak")
                if (res.isSuccess) successCount++
            }
            _isBusy.value = false
            _feedbackMessage.value = "Absensi Hadir tercatat untuk $successCount santri pada $dateKey."
        }
    }

    fun generateMonthlyReport(
        santri: Santri,
        teacherNote: String,
        onGenerated: (MonthlyReport) -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val nilai = (nilaiListState.value as? UiState.Success)?.data.orEmpty()
            val absensi = (absensiListState.value as? UiState.Success)?.data.orEmpty()
            val res = repository.generateAndSendMonthlyReport(santri, nilai, absensi, teacherNote)
            _isBusy.value = false
            res.onSuccess { report ->
                _feedbackMessage.value = "Laporan ${report.monthTitle} berhasil diterbitkan ke Wali Murid!"
                onGenerated(report)
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Gagal membuat laporan bulanan."
            }
        }
    }

    fun acknowledgeReport(santriId: String, reportId: String) {
        viewModelScope.launch {
            val res = repository.acknowledgeMonthlyReport(santriId, reportId)
            res.onSuccess {
                _feedbackMessage.value = "Laporan ditandai sudah dibaca oleh Wali Murid."
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Gagal memperbarui status baca."
            }
        }
    }

    fun updateSettings(
        displayName: String,
        tpqName: String,
        preferredMode: UserViewMode,
        biometricEnabled: Boolean,
        dailyReminderEnabled: Boolean,
        dailyReminderHour: Int,
        dailyReminderMinute: Int,
        weeklyDigestEnabled: Boolean,
        monthlyAutoReportEnabled: Boolean,
        appsScriptUrl: String?,
        githubRepo: String?,
        githubToken: String? = null
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            if (githubToken != null) {
                integrationService.saveGithubToken(githubToken)
            }
            val res = repository.updateUserSettings(
                displayName = displayName,
                tpqName = tpqName,
                preferredMode = preferredMode,
                biometricEnabled = biometricEnabled,
                dailyReminderEnabled = dailyReminderEnabled,
                dailyReminderHour = dailyReminderHour,
                dailyReminderMinute = dailyReminderMinute,
                weeklyDigestEnabled = weeklyDigestEnabled,
                monthlyAutoReportEnabled = monthlyAutoReportEnabled,
                appsScriptUrl = appsScriptUrl,
                githubRepo = githubRepo
            )
            _isBusy.value = false
            res.onSuccess {
                _feedbackMessage.value = "Pengaturan & integrasi tersimpan."
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Gagal menyimpan pengaturan."
            }
        }
    }

    fun testAppsScript(url: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = integrationService.testAppsScriptConnection(url)
            _isBusy.value = false
            _feedbackMessage.value = res.getOrElse { it.localizedMessage ?: "Gagal menguji koneksi Apps Script." }
        }
    }

    fun syncToSpreadsheet(url: String, pushToGithubViaScript: Boolean) {
        viewModelScope.launch {
            _isBusy.value = true
            val santriList = (santriListState.value as? UiState.Success)?.data.orEmpty()
            val nilaiList = (nilaiListState.value as? UiState.Success)?.data.orEmpty()
            val absensiList = (absensiListState.value as? UiState.Success)?.data.orEmpty()
            val res = integrationService.syncToAppsScriptSpreadsheet(
                webAppUrl = url,
                santriList = santriList,
                nilaiList = nilaiList,
                absensiList = absensiList,
                pushToGithubViaScript = pushToGithubViaScript
            )
            _isBusy.value = false
            _feedbackMessage.value = res.getOrElse { it.localizedMessage ?: "Gagal sinkronisasi ke Spreadsheet." }
        }
    }

    fun pushDirectToGitHub(repo: String, token: String) {
        viewModelScope.launch {
            _isBusy.value = true
            integrationService.saveGithubToken(token)
            val santriList = (santriListState.value as? UiState.Success)?.data.orEmpty()
            val nilaiList = (nilaiListState.value as? UiState.Success)?.data.orEmpty()
            val absensiList = (absensiListState.value as? UiState.Success)?.data.orEmpty()
            val res = integrationService.pushSnapshotDirectToGitHub(
                repoSlug = repo,
                token = token,
                santriList = santriList,
                nilaiList = nilaiList,
                absensiList = absensiList
            )
            _isBusy.value = false
            _feedbackMessage.value = res.getOrElse { it.localizedMessage ?: "Gagal push ke GitHub." }
        }
    }

    private fun <T> Flow<T>.toUiState(fallbackError: String): Flow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val root = unwrapFirestoreOrRethrow(error, "FlowObserve")
                emit(UiState.Error(root.message ?: fallbackError))
            }

    private fun unwrapFirestoreOrRethrow(error: Throwable, tag: String): Throwable {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val root = firestoreError ?: error
        Log.w("TpqVM:$tag", "Stream error", root)
        return root
    }
}
