const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const GURU_UID = "guru_ustadz_01";
const WALI_UID = "wali_murid_02";
const STRANGER_UID = "stranger_99";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

test("Unauthenticated users are denied everywhere", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("santri").get());
  await assertFails(unauthDb.collection("users").doc(GURU_UID).get());
  await assertFails(unauthDb.collection("share_codes").doc("TPQ12345").get());
});

test("UserProfile: owner CRUD succeeds, cross-user access & shadow fields fail", async () => {
  const guruDb = testEnv.authenticatedContext(GURU_UID).firestore();
  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    guruDb.collection("users").doc(GURU_UID).set({
      userId: GURU_UID,
      displayName: "Ustadz Ahmad",
      email: "ahmad@tpq.id",
      preferredMode: "GURU",
      tpqName: "TPQ Al-Ikhlas",
      biometricEnabled: true,
      dailyReminderEnabled: true,
      dailyReminderHour: 16,
      dailyReminderMinute: 0,
      weeklyDigestEnabled: true,
      monthlyAutoReportEnabled: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Stranger cannot read PII
  await assertFails(strangerDb.collection("users").doc(GURU_UID).get());

  // Shadow field rejected
  await assertFails(
    guruDb.collection("users").doc(GURU_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Valid partial update succeeds
  await assertSucceeds(
    guruDb.collection("users").doc(GURU_UID).update({
      tpqName: "TPQ Nurul Huda",
      appsScriptUrl: "https://script.google.com/macros/s/abc/exec",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Santri, Nilai, Absensi, Laporan: Guru write + Wali Murid shareCode link & read", async () => {
  const guruDb = testEnv.authenticatedContext(GURU_UID).firestore();
  const waliDb = testEnv.authenticatedContext(WALI_UID).firestore();
  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  const created = new Date(Date.now() - 5000);

  // Guru creates Santri & ShareCode
  await assertSucceeds(
    guruDb.collection("santri").doc("santri_1").set({
      teacherId: GURU_UID,
      teacherName: "Ustadz Ahmad",
      memberUids: [GURU_UID],
      parentUids: [],
      name: "Zaid Abdullah",
      nis: "TPQ-001",
      halaqahClass: "Juz Amma A",
      parentName: "Bpk. Abdullah",
      parentContact: "08123456789",
      shareCode: "TPQ8812A",
      xpPoints: 120,
      streakDays: 5,
      completedSurahCount: 2,
      avgTajwidScore: 92.5,
      attendancePercent: 100.0,
      unlockedBadges: ["muqaddimah"],
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    guruDb.collection("share_codes").doc("TPQ8812A").set({
      santriId: "santri_1",
      teacherId: GURU_UID,
      santriName: "Zaid Abdullah",
      halaqahClass: "Juz Amma A",
      createdAt: created,
    })
  );

  // Listing share_codes is always denied
  await assertFails(waliDb.collection("share_codes").get());
  // Direct get by exact code succeeds
  await assertSucceeds(waliDb.collection("share_codes").doc("TPQ8812A").get());

  // Unfiltered santri list query fails
  await assertFails(guruDb.collection("santri").get());
  // Filtered query succeeds
  await assertSucceeds(
    guruDb.collection("santri").where("memberUids", "array-contains", GURU_UID).get()
  );

  // Wali Murid links self with valid share code
  await assertSucceeds(
    waliDb.collection("santri").doc("santri_1").update({
      parentUids: [WALI_UID],
      memberUids: [GURU_UID, WALI_UID],
      lastJoinCode: "TPQ8812A",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Stranger with wrong join code fails
  await assertFails(
    strangerDb.collection("santri").doc("santri_1").update({
      parentUids: [WALI_UID, STRANGER_UID],
      memberUids: [GURU_UID, WALI_UID, STRANGER_UID],
      lastJoinCode: "WRONG999",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Guru records Nilai (Hafalan & Tajwid)
  await assertSucceeds(
    guruDb.collection("santri").doc("santri_1").collection("nilai").doc("nilai_1").set({
      santriId: "santri_1",
      teacherId: GURU_UID,
      memberUids: [GURU_UID, WALI_UID],
      surahNumber: 114,
      surahName: "An-Nas",
      surahArabic: "الناس",
      ayahFrom: 1,
      ayahTo: 6,
      hafalanStatus: "MUMTAZ",
      scoreKelancaran: 95,
      scoreMakhraj: 92,
      scoreHukumTajwid: 90,
      scoreMadQalqalah: 94,
      scoreFashohah: 93,
      totalScore: 93,
      notes: "Bacaan jelas dan tartil",
      dateLabel: "2026-11-26",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Wali Murid can query nilai with memberUids filter, cannot tamper with grades
  await assertSucceeds(
    waliDb.collection("santri").doc("santri_1").collection("nilai")
      .where("memberUids", "array-contains", WALI_UID).get()
  );
  await assertFails(
    waliDb.collection("santri").doc("santri_1").collection("nilai").doc("nilai_1").update({
      totalScore: 100,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Guru records Absensi & Monthly Report; Wali acknowledges report
  await assertSucceeds(
    guruDb.collection("santri").doc("santri_1").collection("absensi").doc("2026-11-26").set({
      santriId: "santri_1",
      teacherId: GURU_UID,
      memberUids: [GURU_UID, WALI_UID],
      dateKey: "2026-11-26",
      status: "HADIR",
      note: "Tepat waktu",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    guruDb.collection("santri").doc("santri_1").collection("laporan").doc("2026-11").set({
      santriId: "santri_1",
      teacherId: GURU_UID,
      memberUids: [GURU_UID, WALI_UID],
      monthKey: "2026-11",
      monthTitle: "November 2026",
      santriName: "Zaid Abdullah",
      totalSetoran: 4,
      completedSurahs: 2,
      avgHafalanScore: 93.0,
      avgTajwidScore: 92.5,
      attendancePercent: 100.0,
      hadirDays: 20,
      izinDays: 0,
      sakitDays: 0,
      alphaDays: 0,
      teacherNote: "Perkembangan sangat baik bulan ini.",
      sentToParent: true,
      parentAcknowledged: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    waliDb.collection("santri").doc("santri_1").collection("laporan").doc("2026-11").update({
      parentAcknowledged: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});
