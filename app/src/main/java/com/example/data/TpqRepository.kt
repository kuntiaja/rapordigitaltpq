package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class TpqRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            throw IllegalStateException("Pengguna harus masuk dengan akun Google terlebih dahulu.")
        }
        return uid
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        if (userId.isBlank()) {
            emit(null)
            return@flow
        }
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    handleFlowError(error, OperationType.GET, path, userId)
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(
        userId: String,
        displayName: String,
        email: String
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "UID pengguna tidak sesuai." }
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val profile = UserProfile(
                    userId = uid,
                    displayName = displayName.ifBlank { "Ustadz / Wali TPQ" }.take(100),
                    email = email.take(150),
                    preferredMode = UserViewMode.GURU.name,
                    tpqName = "TPQ Nurul Qur'an"
                )
                docRef.set(profile.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateUserSettings(
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
        githubRepo: String?
    ): Result<Unit> {
        var path = "users/unknown"
        return try {
            val uid = requireUserId()
            path = "users/$uid"
            require(displayName.trim().isNotEmpty()) { "Nama tampilan wajib diisi." }
            val updates = buildMap<String, Any> {
                put("displayName", displayName.trim().take(100))
                put("tpqName", tpqName.trim().ifEmpty { "TPQ Nurul Qur'an" }.take(100))
                put("preferredMode", preferredMode.name)
                put("biometricEnabled", biometricEnabled)
                put("dailyReminderEnabled", dailyReminderEnabled)
                put("dailyReminderHour", dailyReminderHour.coerceIn(0, 23).toLong())
                put("dailyReminderMinute", dailyReminderMinute.coerceIn(0, 59).toLong())
                put("weeklyDigestEnabled", weeklyDigestEnabled)
                put("monthlyAutoReportEnabled", monthlyAutoReportEnabled)
                appsScriptUrl?.trim()?.takeIf { it.isNotEmpty() }?.let {
                    put("appsScriptUrl", it.take(500))
                }
                githubRepo?.trim()?.takeIf { it.isNotEmpty() }?.let {
                    put("githubRepo", it.take(150))
                }
                put("updatedAt", FieldValue.serverTimestamp())
            }
            db.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeSantriList(userId: String): Flow<List<Santri>> = flow {
        if (userId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "santri"
        emitAll(
            db.collection(path)
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Santri::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.name.lowercase(Locale.getDefault()) }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeNilai(santriId: String, userId: String): Flow<List<NilaiRecord>> = flow {
        if (santriId.isBlank() || userId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "santri/$santriId/nilai"
        emitAll(
            db.collection("santri").document(santriId).collection("nilai")
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        NilaiRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeAbsensi(santriId: String, userId: String): Flow<List<AbsensiRecord>> = flow {
        if (santriId.isBlank() || userId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "santri/$santriId/absensi"
        emitAll(
            db.collection("santri").document(santriId).collection("absensi")
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AbsensiRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.dateKey }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeLaporan(santriId: String, userId: String): Flow<List<MonthlyReport>> = flow {
        if (santriId.isBlank() || userId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "santri/$santriId/laporan"
        emitAll(
            db.collection("santri").document(santriId).collection("laporan")
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MonthlyReport::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.monthKey }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    suspend fun createSantri(
        name: String,
        nis: String,
        halaqahClass: String,
        teacherName: String,
        parentName: String,
        parentContact: String
    ): Result<String> {
        var path = "santri"
        return try {
            val uid = requireUserId()
            val cleanName = name.trim()
            val cleanNis = nis.trim()
            val cleanClass = halaqahClass.trim()
            require(cleanName.isNotEmpty() && cleanName.length <= 100) { "Nama santri tidak valid (1-100 karakter)." }
            require(cleanNis.isNotEmpty() && cleanNis.length <= 30) { "NIS santri wajib diisi." }
            require(cleanClass.isNotEmpty() && cleanClass.length <= 60) { "Kelas/Halaqah wajib diisi." }

            val santriRef = db.collection("santri").document()
            path = santriRef.path
            val shareCode = generateShareCode()

            val payload = mapOf(
                "teacherId" to uid,
                "teacherName" to teacherName.trim().ifEmpty { "Ustadz TPQ" }.take(100),
                "memberUids" to listOf(uid),
                "parentUids" to emptyList<String>(),
                "name" to cleanName,
                "nis" to cleanNis,
                "halaqahClass" to cleanClass,
                "parentName" to parentName.trim().take(100),
                "parentContact" to parentContact.trim().take(60),
                "shareCode" to shareCode,
                "xpPoints" to 0L,
                "streakDays" to 0L,
                "completedSurahCount" to 0L,
                "avgTajwidScore" to 0.0,
                "attendancePercent" to 100.0,
                "unlockedBadges" to emptyList<String>(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val shareCodePayload = mapOf(
                "santriId" to santriRef.id,
                "teacherId" to uid,
                "santriName" to cleanName,
                "halaqahClass" to cleanClass,
                "createdAt" to FieldValue.serverTimestamp()
            )

            val batch = db.batch()
            batch.set(santriRef, payload)
            batch.set(db.collection("share_codes").document(shareCode), shareCodePayload)
            batch.commit().await()

            Result.success(santriRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun linkSantriByShareCode(rawCode: String): Result<String> {
        val code = rawCode.trim().uppercase(Locale.US)
        var path = "share_codes/$code"
        return try {
            val uid = requireUserId()
            require(code.length in 6..24 && code.matches(Regex("^[a-zA-Z0-9_\\-]+$"))) {
                "Format kode akses santri tidak valid."
            }
            val codeSnap = db.collection("share_codes").document(code).get().await()
            if (!codeSnap.exists()) {
                return Result.failure(IllegalArgumentException("Kode akses tidak ditemukan. Minta kode dari Ustadz/Guru TPQ."))
            }
            val santriId = codeSnap.getString("santriId")
                ?: return Result.failure(IllegalStateException("Data kode akses tidak lengkap."))
            val santriName = codeSnap.getString("santriName") ?: "Santri"
            path = "santri/$santriId"

            db.collection("santri").document(santriId).update(
                mapOf(
                    "parentUids" to FieldValue.arrayUnion(uid),
                    "memberUids" to FieldValue.arrayUnion(uid),
                    "lastJoinCode" to code,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success("Berhasil menautkan akun wali murid ke $santriName")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun syncMemberUidsToSubcollections(santri: Santri): Result<Unit> {
        val path = "santri/${santri.id}"
        return try {
            val uid = requireUserId()
            if (santri.teacherId != uid || santri.memberUids.size <= 1) {
                return Result.success(Unit)
            }
            val santriRef = db.collection("santri").document(santri.id)
            val batch = db.batch()
            var hasUpdates = false

            listOf("nilai", "absensi", "laporan").forEach { sub ->
                val docs = santriRef.collection(sub)
                    .whereArrayContains("memberUids", uid)
                    .get()
                    .await()
                docs.documents.forEach { doc ->
                    val existingMembers = (doc.get("memberUids") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    if (!existingMembers.containsAll(santri.memberUids)) {
                        batch.update(
                            doc.reference,
                            mapOf(
                                "memberUids" to santri.memberUids,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                        hasUpdates = true
                    }
                }
            }
            if (hasUpdates) {
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun recordNilaiAndUpdateStats(
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
    ): Result<Unit> {
        val path = "santri/${santri.id}/nilai"
        return try {
            val uid = requireUserId()
            check(santri.teacherId == uid) { "Hanya Guru pengampu yang dapat menginput nilai." }
            val safeFrom = ayahFrom.coerceIn(1, surah.totalAyah)
            val safeTo = ayahTo.coerceIn(safeFrom, surah.totalAyah)
            val sKel = kelancaran.coerceIn(0, 100)
            val sMak = makhraj.coerceIn(0, 100)
            val sHuk = hukumTajwid.coerceIn(0, 100)
            val sMad = madQalqalah.coerceIn(0, 100)
            val sFas = fashohah.coerceIn(0, 100)
            val totalScore = (sKel + sMak + sHuk + sMad + sFas) / 5
            val dateLabel = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

            val nilaiRef = db.collection("santri").document(santri.id).collection("nilai").document()
            val payload = mapOf(
                "santriId" to santri.id,
                "teacherId" to uid,
                "memberUids" to santri.memberUids,
                "surahNumber" to surah.number.toLong(),
                "surahName" to surah.nameLatin.take(60),
                "surahArabic" to surah.nameArabic.take(60),
                "ayahFrom" to safeFrom.toLong(),
                "ayahTo" to safeTo.toLong(),
                "hafalanStatus" to hafalanStatus.name,
                "scoreKelancaran" to sKel.toLong(),
                "scoreMakhraj" to sMak.toLong(),
                "scoreHukumTajwid" to sHuk.toLong(),
                "scoreMadQalqalah" to sMad.toLong(),
                "scoreFashohah" to sFas.toLong(),
                "totalScore" to totalScore.toLong(),
                "notes" to notes.trim().take(300),
                "dateLabel" to dateLabel,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            nilaiRef.set(payload).await()
            recalculateSantriStats(santri)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun recordAbsensiAndUpdateStats(
        santri: Santri,
        dateKey: String,
        status: AbsensiStatus,
        note: String
    ): Result<Unit> {
        val path = "santri/${santri.id}/absensi/$dateKey"
        return try {
            val uid = requireUserId()
            check(santri.teacherId == uid) { "Hanya Guru pengampu yang dapat mencatat kehadiran." }
            require(dateKey.length == 10) { "Format tanggal harus yyyy-MM-dd." }
            val docRef = db.collection("santri").document(santri.id).collection("absensi").document(dateKey)
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "memberUids" to santri.memberUids,
                        "status" to status.name,
                        "note" to note.trim().take(200),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "santriId" to santri.id,
                        "teacherId" to uid,
                        "memberUids" to santri.memberUids,
                        "dateKey" to dateKey,
                        "status" to status.name,
                        "note" to note.trim().take(200),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            recalculateSantriStats(santri)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private suspend fun recalculateSantriStats(santri: Santri) {
        val uid = requireUserId()
        val santriRef = db.collection("santri").document(santri.id)
        val nilaiList = santriRef.collection("nilai")
            .whereArrayContains("memberUids", uid)
            .get().await()
            .toObjects(NilaiRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

        val absensiList = santriRef.collection("absensi")
            .whereArrayContains("memberUids", uid)
            .get().await()
            .toObjects(AbsensiRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

        val mumtazCount = nilaiList
            .filter { it.hafalanStatus == HafalanStatus.MUMTAZ.name }
            .map { it.surahNumber }
            .distinct()
            .size
            .coerceIn(0, 114)

        val avgTajwid = if (nilaiList.isEmpty()) 0.0 else nilaiList.map { it.totalScore.toDouble() }.average()

        val totalAbsensi = absensiList.size
        val hadirCount = absensiList.count { it.status == AbsensiStatus.HADIR.name }
        val attendancePercent = if (totalAbsensi == 0) 100.0 else (hadirCount * 100.0) / totalAbsensi

        val sortedAbsensi = absensiList.sortedByDescending { it.dateKey }
        var streak = 0
        for (rec in sortedAbsensi) {
            if (rec.status == AbsensiStatus.HADIR.name) streak++ else break
        }
        val effectiveStreak = maxOf(streak, nilaiList.map { it.dateLabel }.distinct().size.coerceAtMost(365))

        val unlockedBadges = BadgeCatalog.evaluateUnlockedBadges(nilaiList, absensiList, effectiveStreak)
        val badgeXp = unlockedBadges.sumOf { id ->
            BadgeCatalog.allBadges.firstOrNull { it.id == id }?.xpReward ?: 0
        }
        val xpTotal = (nilaiList.size * 35 + mumtazCount * 50 + hadirCount * 15 + badgeXp).toLong()

        santriRef.update(
            mapOf(
                "xpPoints" to xpTotal,
                "streakDays" to effectiveStreak.toLong(),
                "completedSurahCount" to mumtazCount.toLong(),
                "avgTajwidScore" to (Math.round(avgTajwid * 10.0) / 10.0),
                "attendancePercent" to (Math.round(attendancePercent * 10.0) / 10.0),
                "unlockedBadges" to unlockedBadges,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun generateAndSendMonthlyReport(
        santri: Santri,
        nilaiList: List<NilaiRecord>,
        absensiList: List<AbsensiRecord>,
        teacherNote: String
    ): Result<MonthlyReport> {
        val monthKey = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
        val monthTitle = SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(Date())
        val path = "santri/${santri.id}/laporan/$monthKey"
        return try {
            val uid = requireUserId()
            check(santri.teacherId == uid) { "Hanya Guru yang dapat menerbitkan laporan bulanan." }

            val monthNilai = nilaiList.filter { it.dateLabel.startsWith(monthKey) }.ifEmpty { nilaiList }
            val monthAbsensi = absensiList.filter { it.dateKey.startsWith(monthKey) }.ifEmpty { absensiList }

            val hadir = monthAbsensi.count { it.status == AbsensiStatus.HADIR.name }.coerceAtMost(31)
            val izin = monthAbsensi.count { it.status == AbsensiStatus.IZIN.name }.coerceAtMost(31)
            val sakit = monthAbsensi.count { it.status == AbsensiStatus.SAKIT.name }.coerceAtMost(31)
            val alpha = monthAbsensi.count { it.status == AbsensiStatus.ALPHA.name }.coerceAtMost(31)
            val totalDays = hadir + izin + sakit + alpha
            val attPct = if (totalDays == 0) 100.0 else (hadir * 100.0) / totalDays

            val avgHafalan = if (monthNilai.isEmpty()) 0.0 else monthNilai.map { it.scoreKelancaran.toDouble() }.average()
            val avgTajwid = if (monthNilai.isEmpty()) 0.0 else monthNilai.map { it.totalScore.toDouble() }.average()
            val completedSurahs = nilaiList.filter { it.hafalanStatus == HafalanStatus.MUMTAZ.name }
                .map { it.surahNumber }.distinct().size.coerceIn(0, 114)

            val noteClean = teacherNote.trim().ifEmpty {
                "Alhamdulillah, ananda ${santri.name} aktif mengikuti setor hafalan & pembelajaran tajwid bulan $monthTitle."
            }.take(500)

            val docRef = db.collection("santri").document(santri.id).collection("laporan").document(monthKey)
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "memberUids" to santri.memberUids,
                        "monthTitle" to monthTitle.take(40),
                        "santriName" to santri.name.take(100),
                        "totalSetoran" to monthNilai.size.toLong(),
                        "completedSurahs" to completedSurahs.toLong(),
                        "avgHafalanScore" to (Math.round(avgHafalan * 10.0) / 10.0),
                        "avgTajwidScore" to (Math.round(avgTajwid * 10.0) / 10.0),
                        "attendancePercent" to (Math.round(attPct * 10.0) / 10.0),
                        "hadirDays" to hadir.toLong(),
                        "izinDays" to izin.toLong(),
                        "sakitDays" to sakit.toLong(),
                        "alphaDays" to alpha.toLong(),
                        "teacherNote" to noteClean,
                        "sentToParent" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "santriId" to santri.id,
                        "teacherId" to uid,
                        "memberUids" to santri.memberUids,
                        "monthKey" to monthKey,
                        "monthTitle" to monthTitle.take(40),
                        "santriName" to santri.name.take(100),
                        "totalSetoran" to monthNilai.size.toLong(),
                        "completedSurahs" to completedSurahs.toLong(),
                        "avgHafalanScore" to (Math.round(avgHafalan * 10.0) / 10.0),
                        "avgTajwidScore" to (Math.round(avgTajwid * 10.0) / 10.0),
                        "attendancePercent" to (Math.round(attPct * 10.0) / 10.0),
                        "hadirDays" to hadir.toLong(),
                        "izinDays" to izin.toLong(),
                        "sakitDays" to sakit.toLong(),
                        "alphaDays" to alpha.toLong(),
                        "teacherNote" to noteClean,
                        "sentToParent" to true,
                        "parentAcknowledged" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val report = MonthlyReport(
                id = monthKey,
                santriId = santri.id,
                teacherId = uid,
                memberUids = santri.memberUids,
                monthKey = monthKey,
                monthTitle = monthTitle,
                santriName = santri.name,
                totalSetoran = monthNilai.size.toLong(),
                completedSurahs = completedSurahs.toLong(),
                avgHafalanScore = Math.round(avgHafalan * 10.0) / 10.0,
                avgTajwidScore = Math.round(avgTajwid * 10.0) / 10.0,
                attendancePercent = Math.round(attPct * 10.0) / 10.0,
                hadirDays = hadir.toLong(),
                izinDays = izin.toLong(),
                sakitDays = sakit.toLong(),
                alphaDays = alpha.toLong(),
                teacherNote = noteClean,
                sentToParent = true,
                parentAcknowledged = false
            )
            Result.success(report)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun acknowledgeMonthlyReport(santriId: String, reportId: String): Result<Unit> {
        val path = "santri/$santriId/laporan/$reportId"
        return try {
            requireUserId()
            db.collection("santri").document(santriId)
                .collection("laporan").document(reportId)
                .update(
                    mapOf(
                        "parentAcknowledged" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    private fun handleFlowError(
        error: Throwable,
        operationType: OperationType,
        path: String,
        expectedUid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }

    private fun generateShareCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val suffix = (1..5).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        return "TPQ-$suffix"
    }
}
