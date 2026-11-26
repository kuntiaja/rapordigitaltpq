/**
 * Google Apps Script - Rapor Digital TPQ
 * Langkah pemasangan dari Google Spreadsheet:
 * 1. Buka Google Spreadsheet baru
 * 2. Klik menu: Ekstensi -> Apps Script
 * 3. Tempel kode ini ke Code.gs dan simpan
 * 4. Klik Terapkan -> Deployment baru -> Aplikasi Web (Akses: Siapa saja)
 * 5. Salin URL Web App ke menu Integrasi di aplikasi Android Rapor TPQ
 * 6. (Opsional) Isi Script Properties: GITHUB_TOKEN, GITHUB_OWNER, GITHUB_REPO untuk auto-backup ke GitHub
 */

const SHEETS = {
  SANTRI: 'Santri',
  NILAI: 'Nilai_Hafalan_Tajwid',
  ABSENSI: 'Absensi_Harian',
  LAPORAN: 'Laporan_Bulanan'
};

function setupSheets() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  ensureSheet(ss, SHEETS.SANTRI, [
    'ID_Santri', 'NIS', 'Nama_Santri', 'Kelas_Halaqah', 'Nama_Wali',
    'Kontak_Wali', 'Kode_Akses', 'Surat_Mumtaz', 'Rata_Tajwid', 'Kehadiran_%', 'XP_Poin', 'Diperbarui'
  ]);
  ensureSheet(ss, SHEETS.NILAI, [
    'ID_Nilai', 'ID_Santri', 'Nama_Santri', 'No_Surat', 'Nama_Surat', 'Ayat',
    'Status_Hafalan', 'Kelancaran', 'Makhraj', 'Hukum_Tajwid', 'Mad_Qalqalah',
    'Fashohah', 'Nilai_Akhir', 'Catatan', 'Tanggal'
  ]);
  ensureSheet(ss, SHEETS.ABSENSI, [
    'Tanggal', 'ID_Santri', 'Nama_Santri', 'Status', 'Keterangan', 'Dicatat_Pada'
  ]);
  ensureSheet(ss, SHEETS.LAPORAN, [
    'Bulan', 'ID_Santri', 'Nama_Santri', 'Total_Setoran', 'Surat_Mumtaz',
    'Rata_Tajwid', 'Kehadiran_%', 'Catatan_Guru', 'Terkirim_Ke_Wali'
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
  const result = {
    status: 'ok',
    spreadsheetName: ss.getName(),
    santriCount: Math.max(0, ss.getSheetByName(SHEETS.SANTRI).getLastRow() - 1),
    nilaiCount: Math.max(0, ss.getSheetByName(SHEETS.NILAI).getLastRow() - 1),
    absensiCount: Math.max(0, ss.getSheetByName(SHEETS.ABSENSI).getLastRow() - 1),
    timestamp: new Date().toISOString()
  };
  return ContentService.createTextOutput(JSON.stringify(result)).setMimeType(ContentService.MimeType.JSON);
}

function doPost(e) {
  setupSheets();
  const payload = JSON.parse(e.postData.contents || '{}');
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  const now = new Date().toISOString();

  if (Array.isArray(payload.santri)) {
    const sSheet = ss.getSheetByName(SHEETS.SANTRI);
    if (sSheet.getLastRow() > 1) sSheet.getRange(2, 1, sSheet.getLastRow() - 1, 12).clearContent();
    payload.santri.forEach((s, idx) => {
      sSheet.getRange(idx + 2, 1, 1, 12).setValues([[
        s.id, s.nis, s.name, s.halaqahClass, s.parentName, s.parentContact,
        s.shareCode, s.completedSurahCount, s.avgTajwidScore, s.attendancePercent, s.xpPoints, now
      ]]);
    });
  }

  if (Array.isArray(payload.nilai)) {
    const nSheet = ss.getSheetByName(SHEETS.NILAI);
    if (nSheet.getLastRow() > 1) nSheet.getRange(2, 1, nSheet.getLastRow() - 1, 15).clearContent();
    payload.nilai.forEach((n, idx) => {
      nSheet.getRange(idx + 2, 1, 1, 15).setValues([[
        n.id, n.santriId, n.santriName, n.surahNumber, n.surahName, n.ayahRange,
        n.hafalanStatus, n.scoreKelancaran, n.scoreMakhraj, n.scoreHukumTajwid,
        n.scoreMadQalqalah, n.scoreFashohah, n.totalScore, n.notes, n.dateLabel
      ]]);
    });
  }

  if (Array.isArray(payload.absensi)) {
    const aSheet = ss.getSheetByName(SHEETS.ABSENSI);
    if (aSheet.getLastRow() > 1) aSheet.getRange(2, 1, aSheet.getLastRow() - 1, 6).clearContent();
    payload.absensi.forEach((a, idx) => {
      aSheet.getRange(idx + 2, 1, 1, 6).setValues([[
        a.dateKey, a.santriId, a.santriName, a.status, a.note, now
      ]]);
    });
  }

  if (payload.pushToGithub === true) {
    pushDatabaseSnapshotToGithub(payload);
  }

  return ContentService.createTextOutput(JSON.stringify({
    status: 'synced',
    syncedAt: now
  })).setMimeType(ContentService.MimeType.JSON);
}

function pushDatabaseSnapshotToGithub(payload) {
  const props = PropertiesService.getScriptProperties();
  const token = props.getProperty('GITHUB_TOKEN');
  const owner = props.getProperty('GITHUB_OWNER');
  const repo = props.getProperty('GITHUB_REPO');
  if (!token || !owner || !repo) return;

  const path = 'data/rapor_tpq_snapshot.json';
  const apiUrl = `https://api.github.com/repos/${owner}/${repo}/contents/${path}`;
  let sha = null;
  const getResp = UrlFetchApp.fetch(apiUrl, {
    method: 'get',
    headers: { Authorization: `Bearer ${token}`, Accept: 'application/vnd.github+json' },
    muteHttpExceptions: true
  });
  if (getResp.getResponseCode() === 200) {
    sha = JSON.parse(getResp.getContentText()).sha;
  }

  const contentB64 = Utilities.base64Encode(JSON.stringify(payload, null, 2), Utilities.Charset.UTF_8);
  const body = { message: `Sync Rapor TPQ ${new Date().toISOString()}`, content: contentB64 };
  if (sha) body.sha = sha;

  UrlFetchApp.fetch(apiUrl, {
    method: 'put',
    contentType: 'application/json',
    headers: { Authorization: `Bearer ${token}`, Accept: 'application/vnd.github+json' },
    payload: JSON.stringify(body)
  });
}
