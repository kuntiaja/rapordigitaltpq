package com.example.data

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private val Context.integrationDataStore by preferencesDataStore(name = "tpq_integrations")

data class LocalIntegrationCredentials(
    val githubToken: String = ""
)

class IntegrationSyncService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private object Keys {
        val GITHUB_TOKEN = stringPreferencesKey("github_token")
    }

    val localCredentials: Flow<LocalIntegrationCredentials> =
        context.integrationDataStore.data.map { prefs ->
            LocalIntegrationCredentials(githubToken = prefs[Keys.GITHUB_TOKEN] ?: "")
        }

    suspend fun saveGithubToken(token: String) {
        context.integrationDataStore.edit { prefs ->
            prefs[Keys.GITHUB_TOKEN] = token.trim()
        }
    }

    suspend fun testAppsScriptConnection(webAppUrl: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = webAppUrl.trim()
            require(cleanUrl.startsWith("https://script.google.com/")) {
                "URL harus berasal dari Google Apps Script (https://script.google.com/...)"
            }
            val request = Request.Builder().url(cleanUrl).get().build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("HTTP ${response.code}: Gagal menghubungi Apps Script"))
                }
                val json = runCatching { JSONObject(body) }.getOrNull()
                val ssName = json?.optString("spreadsheetName")?.takeIf { it.isNotBlank() } ?: "Google Spreadsheet"
                val sCount = json?.optInt("santriCount", 0) ?: 0
                Result.success("Terhubung ke $ssName ($sCount santri tercatat)")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncToAppsScriptSpreadsheet(
        webAppUrl: String,
        santriList: List<Santri>,
        nilaiList: List<NilaiRecord>,
        absensiList: List<AbsensiRecord>,
        pushToGithubViaScript: Boolean
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = webAppUrl.trim()
            require(cleanUrl.startsWith("https://script.google.com/")) {
                "Masukkan URL Web App Google Apps Script yang valid."
            }
            val payload = buildSnapshotJson(santriList, nilaiList, absensiList, pushToGithubViaScript)
            val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().url(cleanUrl).post(body).build()
            client.newCall(request).execute().use { response ->
                val respText = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("HTTP ${response.code}: Gagal sinkron ke Spreadsheet"))
                }
                val respJson = runCatching { JSONObject(respText) }.getOrNull()
                val syncedAt = respJson?.optString("syncedAt").orEmpty()
                Result.success("Berhasil sinkron ${santriList.size} santri, ${nilaiList.size} nilai & ${absensiList.size} absensi ke Spreadsheet${if (syncedAt.isNotBlank()) " ($syncedAt)" else ""}")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pushSnapshotDirectToGitHub(
        repoSlug: String,
        token: String,
        santriList: List<Santri>,
        nilaiList: List<NilaiRecord>,
        absensiList: List<AbsensiRecord>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanRepo = repoSlug.trim().removePrefix("https://github.com/").trim('/')
            require(cleanRepo.contains("/") && cleanRepo.split("/").size == 2) {
                "Format repositori harus owner/nama-repo (contoh: tpq-nurul/rapor-santri)"
            }
            val cleanToken = token.trim()
            require(cleanToken.isNotEmpty()) {
                "Masukkan GitHub Personal Access Token (scope repo) untuk push langsung ke GitHub."
            }

            val filePath = "data/rapor_tpq_snapshot.json"
            val apiUrl = "https://api.github.com/repos/$cleanRepo/contents/$filePath"

            var existingSha: String? = null
            val getReq = Request.Builder()
                .url(apiUrl)
                .header("Authorization", "Bearer $cleanToken")
                .header("Accept", "application/vnd.github+json")
                .get()
                .build()
            client.newCall(getReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    val json = runCatching { JSONObject(resp.body?.string().orEmpty()) }.getOrNull()
                    existingSha = json?.optString("sha")?.takeIf { it.isNotBlank() }
                }
            }

            val snapshotJson = buildSnapshotJson(santriList, nilaiList, absensiList, false).toString(2)
            val encoded = Base64.encodeToString(snapshotJson.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

            val commitBody = JSONObject().apply {
                put("message", "Sync Rapor TPQ (${santriList.size} santri, ${nilaiList.size} nilai)")
                put("content", encoded)
                if (existingSha != null) put("sha", existingSha)
            }

            val putReq = Request.Builder()
                .url(apiUrl)
                .header("Authorization", "Bearer $cleanToken")
                .header("Accept", "application/vnd.github+json")
                .put(commitBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            client.newCall(putReq).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val err = resp.body?.string().orEmpty().take(120)
                    return@withContext Result.failure(IllegalStateException("GitHub HTTP ${resp.code}: $err"))
                }
                Result.success("Snapshot database berhasil di-push ke GitHub ($cleanRepo/$filePath)")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun buildSnapshotJson(
        santriList: List<Santri>,
        nilaiList: List<NilaiRecord>,
        absensiList: List<AbsensiRecord>,
        pushToGithub: Boolean
    ): JSONObject {
        val santriMap = santriList.associateBy { it.id }

        val santriArray = JSONArray().apply {
            santriList.forEach { s ->
                put(JSONObject().apply {
                    put("id", s.id)
                    put("nis", s.nis)
                    put("name", s.name)
                    put("halaqahClass", s.halaqahClass)
                    put("parentName", s.parentName)
                    put("parentContact", s.parentContact)
                    put("shareCode", s.shareCode)
                    put("completedSurahCount", s.completedSurahCount)
                    put("avgTajwidScore", s.avgTajwidScore)
                    put("attendancePercent", s.attendancePercent)
                    put("xpPoints", s.xpPoints)
                })
            }
        }

        val nilaiArray = JSONArray().apply {
            nilaiList.forEach { n ->
                put(JSONObject().apply {
                    put("id", n.id)
                    put("santriId", n.santriId)
                    put("santriName", santriMap[n.santriId]?.name ?: "")
                    put("surahNumber", n.surahNumber)
                    put("surahName", n.surahName)
                    put("ayahRange", "${n.ayahFrom}-${n.ayahTo}")
                    put("hafalanStatus", n.hafalanStatus)
                    put("scoreKelancaran", n.scoreKelancaran)
                    put("scoreMakhraj", n.scoreMakhraj)
                    put("scoreHukumTajwid", n.scoreHukumTajwid)
                    put("scoreMadQalqalah", n.scoreMadQalqalah)
                    put("scoreFashohah", n.scoreFashohah)
                    put("totalScore", n.totalScore)
                    put("notes", n.notes)
                    put("dateLabel", n.dateLabel)
                })
            }
        }

        val absensiArray = JSONArray().apply {
            absensiList.forEach { a ->
                put(JSONObject().apply {
                    put("dateKey", a.dateKey)
                    put("santriId", a.santriId)
                    put("santriName", santriMap[a.santriId]?.name ?: "")
                    put("status", a.status)
                    put("note", a.note)
                })
            }
        }

        return JSONObject().apply {
            put("app", "Rapor TPQ Digital")
            put("pushToGithub", pushToGithub)
            put("santri", santriArray)
            put("nilai", nilaiArray)
            put("absensi", absensiArray)
        }
    }

    companion object {
        val APPS_SCRIPT_CODE_GS = "https://script.google.com/macros/s/AKfycbxMfzanb_0IR0C38-SE9sHMdhpT9pSaXVMbye7WfrKgSO9yYGtyTECHLV6ZFlxrGsXb/exec"
            const SHEETS = {
              SANTRI: 'Santri',
              NILAI: 'Nilai_Hafalan_Tajwid',
              ABSENSI: 'Absensi_Harian',
              LAPORAN: 'Laporan_Bulanan'
            };

            function setupSheets() {
              const ss = SpreadsheetApp.getActiveSpreadsheet();
              ensureSheet(ss, SHEETS.SANTRI, [
                'ID_Santri','NIS','Nama_Santri','Kelas_Halaqah','Nama_Wali',
                'Kontak_Wali','Kode_Akses','Surat_Mumtaz','Rata_Tajwid','Kehadiran_%','XP_Poin','Diperbarui'
              ]);
              ensureSheet(ss, SHEETS.NILAI, [
                'ID_Nilai','ID_Santri','Nama_Santri','No_Surat','Nama_Surat','Ayat',
                'Status_Hafalan','Kelancaran','Makhraj','Hukum_Tajwid','Mad_Qalqalah',
                'Fashohah','Nilai_Akhir','Catatan','Tanggal'
              ]);
              ensureSheet(ss, SHEETS.ABSENSI, [
                'Tanggal','ID_Santri','Nama_Santri','Status','Keterangan','Dicatat_Pada'
              ]);
              ensureSheet(ss, SHEETS.LAPORAN, [
                'Bulan','ID_Santri','Nama_Santri','Total_Setoran','Surat_Mumtaz',
                'Rata_Tajwid','Kehadiran_%','Catatan_Guru','Terkirim_Ke_Wali'
              ]);
            }

            function ensureSheet(ss, name, headers) {
              let sheet = ss.getSheetByName(name);
              if (!sheet) {
                sheet = ss.insertSheet(name);
                sheet.appendRow(headers);
                sheet.getRange(1, 1, 1, headers.length).setFontWeight('bold').setBackground('#0D4F44').setFontColor('#FFFFFF');
                sheet.setFrozenRows(1);
              }
              return sheet;
            }

            function doGet(e) {
              setupSheets();
              const ss = SpreadsheetApp.getActiveSpreadsheet();
              return ContentService.createTextOutput(JSON.stringify({
                status: 'ok',
                spreadsheetName: ss.getName(),
                santriCount: Math.max(0, ss.getSheetByName(SHEETS.SANTRI).getLastRow() - 1)
              })).setMimeType(ContentService.MimeType.JSON);
            }

            function doPost(e) {
              setupSheets();
              const payload = JSON.parse(e.postData.contents || '{}');
              const ss = SpreadsheetApp.getActiveSpreadsheet();
              const now = new Date().toISOString();
              if (Array.isArray(payload.santri)) {
                const sSheet = ss.getSheetByName(SHEETS.SANTRI);
                if (sSheet.getLastRow() > 1) sSheet.getRange(2, 1, sSheet.getLastRow() - 1, 12).clearContent();
                payload.santri.forEach((s, i) => sSheet.getRange(i + 2, 1, 1, 12).setValues([[
                  s.id, s.nis, s.name, s.halaqahClass, s.parentName, s.parentContact,
                  s.shareCode, s.completedSurahCount, s.avgTajwidScore, s.attendancePercent, s.xpPoints, now
                ]]));
              }
              if (Array.isArray(payload.nilai)) {
                const nSheet = ss.getSheetByName(SHEETS.NILAI);
                if (nSheet.getLastRow() > 1) nSheet.getRange(2, 1, nSheet.getLastRow() - 1, 15).clearContent();
                payload.nilai.forEach((n, i) => nSheet.getRange(i + 2, 1, 1, 15).setValues([[
                  n.id, n.santriId, n.santriName, n.surahNumber, n.surahName, n.ayahRange,
                  n.hafalanStatus, n.scoreKelancaran, n.scoreMakhraj, n.scoreHukumTajwid,
                  n.scoreMadQalqalah, n.scoreFashohah, n.totalScore, n.notes, n.dateLabel
                ]]));
              }
              if (Array.isArray(payload.absensi)) {
                const aSheet = ss.getSheetByName(SHEETS.ABSENSI);
                if (aSheet.getLastRow() > 1) aSheet.getRange(2, 1, aSheet.getLastRow() - 1, 6).clearContent();
                payload.absensi.forEach((a, i) => aSheet.getRange(i + 2, 1, 1, 6).setValues([[
                  a.dateKey, a.santriId, a.santriName, a.status, a.note, now
                ]]));
              }
              return ContentService.createTextOutput(JSON.stringify({ status: 'synced', syncedAt: now }))
                .setMimeType(ContentService.MimeType.JSON);
            }
        """.trimIndent()
    }
}
