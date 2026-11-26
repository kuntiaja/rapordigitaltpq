package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties

enum class UserViewMode(val label: String) {
    GURU("Mode Guru / Ustadz"),
    WALI_MURID("Mode Wali Murid");

    companion object {
        fun fromRaw(raw: String?): UserViewMode =
            entries.firstOrNull { it.name == raw } ?: GURU
    }
}

enum class HafalanStatus(val label: String) {
    MUMTAZ("Mumtaz (Lulus)"),
    MURAJAAH("Muraja'ah"),
    PROSES("Proses Hafal");

    companion object {
        fun fromRaw(raw: String?): HafalanStatus =
            entries.firstOrNull { it.name == raw } ?: PROSES
    }
}

enum class AbsensiStatus(val label: String) {
    HADIR("Hadir"),
    IZIN("Izin"),
    SAKIT("Sakit"),
    ALPHA("Alpha");

    companion object {
        fun fromRaw(raw: String?): AbsensiStatus =
            entries.firstOrNull { it.name == raw } ?: HADIR
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val preferredMode: String = UserViewMode.GURU.name,
    val tpqName: String = "TPQ Nurul Qur'an",
    val biometricEnabled: Boolean = true,
    val dailyReminderEnabled: Boolean = true,
    val dailyReminderHour: Long = 16L,
    val dailyReminderMinute: Long = 0L,
    val weeklyDigestEnabled: Boolean = true,
    val monthlyAutoReportEnabled: Boolean = true,
    val appsScriptUrl: String? = null,
    val githubRepo: String? = null,
    @com.google.firebase.firestore.ServerTimestamp val createdAt: Timestamp? = null,
    @com.google.firebase.firestore.ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("displayName", displayName.ifBlank { "Pengguna TPQ" }.take(100))
        put("email", email.take(150))
        put("preferredMode", UserViewMode.fromRaw(preferredMode).name)
        put("tpqName", tpqName.take(100))
        put("biometricEnabled", biometricEnabled)
        put("dailyReminderEnabled", dailyReminderEnabled)
        put("dailyReminderHour", dailyReminderHour.coerceIn(0L, 23L))
        put("dailyReminderMinute", dailyReminderMinute.coerceIn(0L, 59L))
        put("weeklyDigestEnabled", weeklyDigestEnabled)
        put("monthlyAutoReportEnabled", monthlyAutoReportEnabled)
        appsScriptUrl?.takeIf { it.isNotBlank() }?.let { put("appsScriptUrl", it.take(500)) }
        githubRepo?.takeIf { it.isNotBlank() }?.let { put("githubRepo", it.take(150)) }
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@Keep
@IgnoreExtraProperties
data class Santri(
    @DocumentId val id: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val memberUids: List<String> = emptyList(),
    val parentUids: List<String> = emptyList(),
    val name: String = "",
    val nis: String = "",
    val halaqahClass: String = "",
    val parentName: String = "",
    val parentContact: String = "",
    val shareCode: String = "",
    val xpPoints: Long = 0L,
    val streakDays: Long = 0L,
    val completedSurahCount: Long = 0L,
    val avgTajwidScore: Double = 0.0,
    val attendancePercent: Double = 100.0,
    val unlockedBadges: List<String> = emptyList(),
    @com.google.firebase.firestore.ServerTimestamp val createdAt: Timestamp? = null,
    @com.google.firebase.firestore.ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class NilaiRecord(
    @DocumentId val id: String = "",
    val santriId: String = "",
    val teacherId: String = "",
    val memberUids: List<String> = emptyList(),
    val surahNumber: Long = 1L,
    val surahName: String = "",
    val surahArabic: String = "",
    val ayahFrom: Long = 1L,
    val ayahTo: Long = 1L,
    val hafalanStatus: String = HafalanStatus.PROSES.name,
    val scoreKelancaran: Long = 80L,
    val scoreMakhraj: Long = 80L,
    val scoreHukumTajwid: Long = 80L,
    val scoreMadQalqalah: Long = 80L,
    val scoreFashohah: Long = 80L,
    val totalScore: Long = 80L,
    val notes: String = "",
    val dateLabel: String = "",
    @com.google.firebase.firestore.ServerTimestamp val createdAt: Timestamp? = null,
    @com.google.firebase.firestore.ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AbsensiRecord(
    @DocumentId val id: String = "",
    val santriId: String = "",
    val teacherId: String = "",
    val memberUids: List<String> = emptyList(),
    val dateKey: String = "",
    val status: String = AbsensiStatus.HADIR.name,
    val note: String = "",
    @com.google.firebase.firestore.ServerTimestamp val createdAt: Timestamp? = null,
    @com.google.firebase.firestore.ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MonthlyReport(
    @DocumentId val id: String = "",
    val santriId: String = "",
    val teacherId: String = "",
    val memberUids: List<String> = emptyList(),
    val monthKey: String = "",
    val monthTitle: String = "",
    val santriName: String = "",
    val totalSetoran: Long = 0L,
    val completedSurahs: Long = 0L,
    val avgHafalanScore: Double = 0.0,
    val avgTajwidScore: Double = 0.0,
    val attendancePercent: Double = 100.0,
    val hadirDays: Long = 0L,
    val izinDays: Long = 0L,
    val sakitDays: Long = 0L,
    val alphaDays: Long = 0L,
    val teacherNote: String = "",
    val sentToParent: Boolean = false,
    val parentAcknowledged: Boolean = false,
    @com.google.firebase.firestore.ServerTimestamp val createdAt: Timestamp? = null,
    @com.google.firebase.firestore.ServerTimestamp val updatedAt: Timestamp? = null
)
