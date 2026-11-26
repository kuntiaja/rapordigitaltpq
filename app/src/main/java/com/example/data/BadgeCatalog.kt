package com.example.data

data class BadgeDefinition(
    val id: String,
    val title: String,
    val subtitle: String,
    val xpReward: Int,
    val category: String
)

object BadgeCatalog {
    val allBadges: List<BadgeDefinition> = listOf(
        BadgeDefinition(
            id = "muqaddimah",
            title = "Muqaddimah",
            subtitle = "Menyelesaikan setoran hafalan pertama di TPQ",
            xpReward = 50,
            category = "Hafalan"
        ),
        BadgeDefinition(
            id = "bintang_mumtaz",
            title = "Bintang Mumtaz",
            subtitle = "Meraih predikat Mumtaz pada 5 surat Al-Qur'an",
            xpReward = 150,
            category = "Hafalan"
        ),
        BadgeDefinition(
            id = "hafizh_juz_amma",
            title = "Hafizh Juz 'Amma",
            subtitle = "Menyelesaikan minimal 15 surat dari Juz 30",
            xpReward = 300,
            category = "Juz 30"
        ),
        BadgeDefinition(
            id = "ahli_tajwid",
            title = "Ahli Tajwid",
            subtitle = "Rata-rata nilai Tajwid mencapai 90 atau lebih",
            xpReward = 200,
            category = "Tajwid"
        ),
        BadgeDefinition(
            id = "istiqomah_pekan",
            title = "Istiqomah Sepekan",
            subtitle = "Menjaga streak kehadiran & setoran minimal 7 hari",
            xpReward = 120,
            category = "Kedisiplinan"
        ),
        BadgeDefinition(
            id = "rajin_mengaji",
            title = "Rajin Mengaji",
            subtitle = "Tingkat kehadiran mencapai 90% dengan minimal 5 catatan hadir",
            xpReward = 150,
            category = "Kehadiran"
        ),
        BadgeDefinition(
            id = "penakluk_panjang",
            title = "Penakluk Surat Panjang",
            subtitle = "Menghafal tuntas surat dengan minimal 40 ayat",
            xpReward = 250,
            category = "Hafalan"
        ),
        BadgeDefinition(
            id = "khatam_114",
            title = "Khatam 30 Juz",
            subtitle = "Menyelesaikan hafalan seluruh 114 surat Al-Qur'an",
            xpReward = 1000,
            category = "Prestasi Agung"
        )
    )

    fun evaluateUnlockedBadges(
        nilaiList: List<NilaiRecord>,
        absensiList: List<AbsensiRecord>,
        streakDays: Int
    ): List<String> {
        val unlocked = mutableListOf<String>()
        val mumtazSurahNumbers = nilaiList
            .filter { it.hafalanStatus == HafalanStatus.MUMTAZ.name }
            .map { it.surahNumber.toInt() }
            .distinct()

        if (nilaiList.isNotEmpty()) unlocked.add("muqaddimah")
        if (mumtazSurahNumbers.size >= 5) unlocked.add("bintang_mumtaz")

        val juzAmmaCompleted = mumtazSurahNumbers.count { it in 78..114 }
        if (juzAmmaCompleted >= 15) unlocked.add("hafizh_juz_amma")

        val avgTajwid = if (nilaiList.isEmpty()) 0.0 else nilaiList.map { it.totalScore }.average()
        if (nilaiList.isNotEmpty() && avgTajwid >= 90.0) unlocked.add("ahli_tajwid")

        if (streakDays >= 7) unlocked.add("istiqomah_pekan")

        val hadirCount = absensiList.count { it.status == AbsensiStatus.HADIR.name }
        val attendanceRate = if (absensiList.isEmpty()) 0.0 else (hadirCount * 100.0) / absensiList.size
        if (hadirCount >= 5 && attendanceRate >= 90.0) unlocked.add("rajin_mengaji")

        val hasLongSurah = mumtazSurahNumbers.any { surahNo ->
            (QuranSurahCatalog.byNumber(surahNo)?.totalAyah ?: 0) >= 40
        }
        if (hasLongSurah) unlocked.add("penakluk_panjang")

        if (mumtazSurahNumbers.size >= 114) unlocked.add("khatam_114")

        return unlocked
    }

    fun levelTitle(xp: Int): Pair<String, Int> = when {
        xp >= 1500 -> "Hafizh Muda" to 5
        xp >= 800 -> "Qari' Teladan" to 4
        xp >= 400 -> "Bintang Tajwid" to 3
        xp >= 150 -> "Penjaga Ayat" to 2
        else -> "Santri Pemula" to 1
    }
}
