/* ===== PENGATURAN: tempel URL Web App Apps Script di sini ===== */
const API_URL = ''; // contoh: https://script.google.com/macros/s/AKfy.../exec

const SURAH = 'Al-Fatihah,Al-Baqarah,Ali Imran,An-Nisa,Al-Maidah,Al-An\'am,Al-A\'raf,Al-Anfal,At-Taubah,Yunus,Hud,Yusuf,Ar-Ra\'d,Ibrahim,Al-Hijr,An-Nahl,Al-Isra,Al-Kahf,Maryam,Taha,Al-Anbiya,Al-Hajj,Al-Mu\'minun,An-Nur,Al-Furqan,Asy-Syu\'ara,An-Naml,Al-Qasas,Al-Ankabut,Ar-Rum,Luqman,As-Sajdah,Al-Ahzab,Saba,Fatir,Yasin,As-Saffat,Sad,Az-Zumar,Gafir,Fussilat,Asy-Syura,Az-Zukhruf,Ad-Dukhan,Al-Jasiyah,Al-Ahqaf,Muhammad,Al-Fath,Al-Hujurat,Qaf,Az-Zariyat,At-Tur,An-Najm,Al-Qamar,Ar-Rahman,Al-Waqi\'ah,Al-Hadid,Al-Mujadalah,Al-Hasyr,Al-Mumtahanah,As-Saff,Al-Jumu\'ah,Al-Munafiqun,At-Tagabun,At-Talaq,At-Tahrim,Al-Mulk,Al-Qalam,Al-Haqqah,Al-Ma\'arij,Nuh,Al-Jinn,Al-Muzzammil,Al-Muddassir,Al-Qiyamah,Al-Insan,Al-Mursalat,An-Naba,An-Nazi\'at,\'Abasa,At-Takwir,Al-Infitar,Al-Mutaffifin,Al-Insyiqaq,Al-Buruj,At-Tariq,Al-A\'la,Al-Gasyiyah,Al-Fajr,Al-Balad,Asy-Syams,Al-Lail,Ad-Duha,Al-Insyirah,At-Tin,Al-\'Alaq,Al-Qadr,Al-Bayyinah,Az-Zalzalah,Al-\'Adiyat,Al-Qari\'ah,At-Takasur,Al-\'Asr,Al-Humazah,Al-Fil,Quraisy,Al-Ma\'un,Al-Kausar,Al-Kafirun,An-Nasr,Al-Lahab,Al-Ikhlas,Al-Falaq,An-Nas'.split(',');

const $ = s => document.querySelector(s);
const esc = v => String(v ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const S = { role: 'guru', pass: '', tab: 'santri', santri: [], sel: '' };

async function api(action, data = {}) {
  if (!API_URL) throw new Error('API_URL belum diisi di app.js.');
  const r = await fetch(API_URL, { method: 'POST', headers: { 'Content-Type': 'text/plain' }, body: JSON.stringify({ action, pass: S.pass, ...data }) });
  const j = await r.json();
  if (j.error) throw new Error(j.error);
  return j.data;
}
const say = t => { $('#msg').textContent = t || ''; };
const today = () => new Date().toISOString().slice(0, 10);

/* ---------- Login ---------- */
document.querySelectorAll('.tab').forEach(b => b.onclick = () => {
  S.role = b.dataset.r;
  document.querySelectorAll('.tab').forEach(x => x.classList.toggle('on', x === b));
  $('#lblPass').hidden = S.role !== 'guru';
  $('#lblCode').hidden = S.role !== 'wali';
});
$('#go').onclick = async () => {
  say(''); $('#go').disabled = true;
  try {
    if (S.role === 'guru') {
      S.pass = $('#pass').value; await api('login');
      $('#login').hidden = true; $('#view').hidden = false; $('#logout').hidden = false;
      $('#nav').hidden = false; await loadSantri(); drawNav(); drawGuru();
    } else {
      const r = await fetch(API_URL, { method: 'POST', headers: { 'Content-Type': 'text/plain' }, body: JSON.stringify({ action: 'wali', code: $('#code').value }) });
      const j = await r.json(); if (j.error) throw new Error(j.error);
      $('#login').hidden = true; $('#view').hidden = false; $('#logout').hidden = false; drawWali(j.data);
    }
  } catch (e) { say(e.message); }
  $('#go').disabled = false;
};
$('#logout').onclick = () => location.reload();

/* ---------- Guru ---------- */
async function loadSantri() { S.santri = await api('listSantri'); if (!S.sel && S.santri[0]) S.sel = S.santri[0].id; }
function drawNav() {
  const t = { santri: 'Santri', nilai: 'Setoran & nilai', absensi: 'Absensi', laporan: 'Laporan bulanan' };
  $('#nav').innerHTML = Object.entries(t).map(([k, v]) => `<button data-t="${k}" class="${S.tab === k ? 'on' : ''}">${v}</button>`).join('');
  $('#nav').querySelectorAll('button').forEach(b => b.onclick = () => { S.tab = b.dataset.t; drawNav(); drawGuru(); });
}
const picker = () => S.santri.length
  ? `<label>Santri<select id="pick">${S.santri.map(s => `<option value="${s.id}" ${s.id === S.sel ? 'selected' : ''}>${esc(s.name)} — ${esc(s.halaqahClass)}</option>`).join('')}</select></label>`
  : '<p class="empty">Belum ada santri. Tambahkan santri pertama di menu Santri.</p>';
const bindPick = () => { const p = $('#pick'); if (p) p.onchange = () => { S.sel = p.value; drawGuru(); }; };
const val = id => $('#' + id).value;
async function act(fn, ok) { try { await fn(); await loadSantri(); drawGuru(); } catch (e) { alert(e.message); } }

async function drawGuru() {
  const v = $('#view');
  if (S.tab === 'santri') {
    v.innerHTML = `<div class="card"><h3>Tambah santri</h3>
      <div class="grid"><label>Nama<input id="n"></label><label>NIS<input id="nis"></label><label>Kelas halaqah<input id="k"></label>
      <label>Nama wali<input id="pn"></label><label>Kontak wali<input id="pc"></label></div>
      <button class="primary" id="add">Simpan santri</button></div>
      <div class="card scroll"><h3>Daftar santri</h3>${S.santri.length ? `<table><tr><th>Nama</th><th>Kelas</th><th>Surat mumtaz</th><th>Tajwid</th><th>Hadir</th><th>XP</th><th>Kode wali</th><th></th></tr>
      ${S.santri.map(s => `<tr><td>${esc(s.name)}<br><small>${esc(s.nis)}</small></td><td>${esc(s.halaqahClass)}</td>
      <td>${s.completedSurahCount}/114<div class="bar"><i style="width:${s.completedSurahCount / 114 * 100}%"></i></div></td>
      <td>${s.avgTajwidScore}</td><td>${s.attendancePercent}%</td><td>${s.xpPoints}</td><td><b>${esc(s.shareCode)}</b></td>
      <td><button class="ghost danger" data-del="${s.id}">Hapus</button></td></tr>`).join('')}</table>` : '<p class="empty">Belum ada santri.</p>'}</div>`;
    $('#add').onclick = () => { if (!val('n').trim()) return alert('Nama santri wajib diisi.');
      act(() => api('saveSantri', { item: { name: val('n'), nis: val('nis'), halaqahClass: val('k'), parentName: val('pn'), parentContact: val('pc') } })); };
    v.querySelectorAll('[data-del]').forEach(b => b.onclick = () => confirm('Hapus santri beserta semua nilainya?') && act(() => api('deleteSantri', { id: b.dataset.del })));
  }
  if (S.tab === 'nilai') {
    const rows = S.sel ? await api('listNilai', { santriId: S.sel }) : [];
    v.innerHTML = `<div class="card">${picker()}</div>${S.sel ? `<div class="card"><h3>Catat setoran</h3><div class="grid">
      <label>Surat<select id="sr">${SURAH.map((n, i) => `<option value="${i + 1}">${i + 1}. ${esc(n)}</option>`).join('')}</select></label>
      <label>Ayat dari<input id="af" type="number" min="1" value="1"></label><label>Ayat sampai<input id="at" type="number" min="1" value="1"></label>
      <label>Status<select id="st"><option>PROSES</option><option>MURAJAAH</option><option>MUMTAZ</option></select></label>
      <label>Tanggal<input id="dt" type="date" value="${today()}"></label></div>
      <div class="grid">${[['sk', 'Kelancaran'], ['sm', 'Makhraj'], ['sh', 'Hukum tajwid'], ['sq', 'Mad & qalqalah'], ['sf', 'Fashohah']].map(([i, l]) => `<label>${l} (0–100)<input id="${i}" type="number" min="0" max="100" value="80"></label>`).join('')}</div>
      <label>Catatan<textarea id="nt" rows="2"></textarea></label><button class="primary" id="sv">Simpan setoran</button></div>
      <div class="card scroll"><h3>Riwayat setoran</h3>${rows.length ? `<table><tr><th>Tanggal</th><th>Surat</th><th>Ayat</th><th>Status</th><th>Nilai</th><th>Catatan</th><th></th></tr>
      ${rows.sort((a, b) => a.dateLabel < b.dateLabel ? 1 : -1).map(r => `<tr><td>${esc(r.dateLabel)}</td><td>${esc(r.surahName)}</td><td>${r.ayahFrom}–${r.ayahTo}</td><td>${r.hafalanStatus}</td><td><b>${r.totalScore}</b></td><td>${esc(r.notes)}</td><td><button class="ghost danger" data-d="${r.id}">Hapus</button></td></tr>`).join('')}</table>` : '<p class="empty">Belum ada setoran untuk santri ini.</p>'}</div>` : ''}`;
    bindPick();
    if (S.sel) {
      $('#sv').onclick = () => act(() => api('saveNilai', { item: { santriId: S.sel, surahNumber: val('sr'), surahName: SURAH[val('sr') - 1], ayahFrom: val('af'), ayahTo: val('at'), hafalanStatus: val('st'),
        scoreKelancaran: val('sk'), scoreMakhraj: val('sm'), scoreHukumTajwid: val('sh'), scoreMadQalqalah: val('sq'), scoreFashohah: val('sf'), notes: val('nt'), dateLabel: val('dt') } }));
      v.querySelectorAll('[data-d]').forEach(b => b.onclick = () => act(() => api('deleteNilai', { id: b.dataset.d, santriId: S.sel })));
    }
  }
  if (S.tab === 'absensi') {
    const rows = S.sel ? await api('listAbsensi', { santriId: S.sel }) : [];
    v.innerHTML = `<div class="card">${picker()}</div>${S.sel ? `<div class="card"><h3>Catat kehadiran</h3><div class="grid">
      <label>Tanggal<input id="dk" type="date" value="${today()}"></label>
      <label>Status<select id="as"><option>HADIR</option><option>IZIN</option><option>SAKIT</option><option>ALPHA</option></select></label>
      <label>Catatan<input id="an"></label></div><button class="primary" id="sa">Simpan kehadiran</button></div>
      <div class="card scroll"><h3>Riwayat kehadiran</h3>${rows.length ? `<table><tr><th>Tanggal</th><th>Status</th><th>Catatan</th></tr>${rows.sort((a, b) => a.dateKey < b.dateKey ? 1 : -1).map(r => `<tr><td>${esc(r.dateKey)}</td><td>${r.status}</td><td>${esc(r.note)}</td></tr>`).join('')}</table>` : '<p class="empty">Belum ada catatan kehadiran.</p>'}</div>` : ''}`;
    bindPick();
    if (S.sel) $('#sa').onclick = () => act(() => api('saveAbsensi', { item: { santriId: S.sel, dateKey: val('dk'), status: val('as'), note: val('an') } }));
  }
  if (S.tab === 'laporan') {
    const rows = S.sel ? await api('listLaporan', { santriId: S.sel }) : [];
    v.innerHTML = `<div class="card">${picker()}</div>${S.sel ? `<div class="card"><h3>Buat laporan bulanan</h3><div class="grid">
      <label>Bulan<input id="mk" type="month" value="${today().slice(0, 7)}"></label></div>
      <label>Catatan guru untuk wali<textarea id="tn" rows="3"></textarea></label><button class="primary" id="gl">Buat dan kirim ke wali</button></div>
      <div id="rep">${repHtml(rows)}</div>` : ''}`;
    bindPick();
    if (S.sel) $('#gl').onclick = () => act(() => api('generateLaporan', { santriId: S.sel, monthKey: val('mk'), note: val('tn') }));
  }
}

const repHtml = rows => rows.length ? rows.sort((a, b) => a.monthKey < b.monthKey ? 1 : -1).map(r => `<div class="card"><h3>${esc(r.santriName)} — ${esc(r.monthKey)}</h3>
  <div class="grid"><div class="stat"><b>${r.totalSetoran}</b>setoran</div><div class="stat"><b>${r.completedSurahs}</b>surat mumtaz</div>
  <div class="stat"><b>${r.avgHafalanScore}</b>rata-rata hafalan</div><div class="stat"><b>${r.avgTajwidScore}</b>rata-rata tajwid</div>
  <div class="stat"><b>${r.attendancePercent}%</b>kehadiran</div></div>
  <p>Hadir ${r.hadirDays} · Izin ${r.izinDays} · Sakit ${r.sakitDays} · Alpha ${r.alphaDays}</p>
  <p><b>Catatan guru:</b> ${esc(r.teacherNote) || '-'}</p></div>`).join('') : '<div class="card"><p class="empty">Belum ada laporan.</p></div>';

/* ---------- Wali murid (hanya baca) ---------- */
function drawWali(d) {
  const s = d.santri;
  $('#view').innerHTML = `<div class="card"><h2>${esc(s.name)}</h2><p>${esc(s.halaqahClass)} · NIS ${esc(s.nis)}</p>
    <div class="grid"><div class="stat"><b>${s.completedSurahCount}/114</b>surat mumtaz</div><div class="stat"><b>${s.avgTajwidScore}</b>rata-rata tajwid</div>
    <div class="stat"><b>${s.attendancePercent}%</b>kehadiran</div><div class="stat"><b>${s.streakDays}</b>hari hadir berturut</div><div class="stat"><b>${s.xpPoints}</b>XP</div></div></div>
    <div class="card scroll"><h3>Setoran terbaru</h3>${d.nilai.length ? `<table><tr><th>Tanggal</th><th>Surat</th><th>Ayat</th><th>Status</th><th>Nilai</th><th>Catatan</th></tr>
    ${d.nilai.sort((a, b) => a.dateLabel < b.dateLabel ? 1 : -1).map(r => `<tr><td>${esc(r.dateLabel)}</td><td>${esc(r.surahName)}</td><td>${r.ayahFrom}–${r.ayahTo}</td><td>${r.hafalanStatus}</td><td><b>${r.totalScore}</b></td><td>${esc(r.notes)}</td></tr>`).join('')}</table>` : '<p class="empty">Belum ada setoran.</p>'}</div>
    <h3>Laporan bulanan</h3>${repHtml(d.laporan)}`;
}
