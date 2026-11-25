package com.example.data

import com.example.model.DasaDarmaItem
import com.example.model.JenjangPramuka
import com.example.model.PackingItem
import com.example.model.QuizQuestion
import com.example.model.ReguMember
import com.example.model.ScoutActivityItem
import com.example.model.ScoutHoliday
import com.example.model.ScoutMaterial
import com.example.model.ScoutNewsItem
import com.example.model.SkuItem
import com.example.model.TkkItem
import java.util.Calendar
import java.util.Locale

object ScoutCatalog {

    val dasaDarmaList: List<DasaDarmaItem> = listOf(
        DasaDarmaItem(1, "Takwa kepada Tuhan Yang Maha Esa", "Awali latihan atau kegiatan harian dengan doa tulus dan syukur."),
        DasaDarmaItem(2, "Cinta alam dan kasih sayang sesama manusia", "Bawa botol minum guna ulang, jaga kebersihan taman, dan sapa teman dengan ramah."),
        DasaDarmaItem(3, "Patriot yang sopan dan kesatria", "Hormati orang tua, guru, dan pembina serta berani mengakui kesalahan."),
        DasaDarmaItem(4, "Patuh dan suka bermusyawarah", "Dengarkan pendapat rekan satu regu atau sangga sebelum mengambil keputusan bersama."),
        DasaDarmaItem(5, "Rela menolong dan tabah", "Bantu teman yang kesulitan memahami materi latihan dengan sabar."),
        DasaDarmaItem(6, "Rajin, terampil, dan gembira", "Selesaikan tugas SKU tepat waktu sambil menjaga semangat positif di regu."),
        DasaDarmaItem(7, "Hemat, cermat, dan bersahaja", "Kelola uang saku dan gunakan perlengkapan perkemahan seperlunya tanpa berlebihan."),
        DasaDarmaItem(8, "Disiplin, berani, dan setia", "Hadir 10 menit lebih awal sebelum apel pembukaan latihan dimulai."),
        DasaDarmaItem(9, "Bertanggung jawab dan dapat dipercaya", "Jaga amanah alat regu dan kembalikan inventaris dalam kondisi bersih."),
        DasaDarmaItem(10, "Suci dalam pikiran, perkataan, dan perbuatan", "Gunakan kata-kata yang santun baik saat berlatih langsung maupun di ruang digital.")
    )

    fun dasaDarmaForToday(): DasaDarmaItem {
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        return dasaDarmaList[(dayOfYear - 1).mod(dasaDarmaList.size)]
    }

    val scoutHolidays: List<ScoutHoliday> = listOf(
        ScoutHoliday("Hari Baden-Powell", 2, 22, "Memperingati hari lahir Bapak Pandu Dunia, Lord Robert Baden-Powell (22 Februari 1857)."),
        ScoutHoliday("Hari Tunas Gerakan Pramuka", 3, 9, "Tonggak sejarah penyatuan organisasi kepanduan Indonesia tahun 1961."),
        ScoutHoliday("Hari Pendidikan Nasional", 5, 2, "Momentum menumbuhkan semangat belajar kepramukaan dan budi pekerti Ki Hadjar Dewantara."),
        ScoutHoliday("Hari Pramuka Indonesia", 8, 14, "Peringatan pelantikan Mapinas, Kwarnas, dan penyerahan Panji Gerakan Pramuka 14 Agustus 1961."),
        ScoutHoliday("Hari Kemerdekaan RI", 8, 17, "Pengibaran Sang Merah Putih dan pengabdian Pramuka bagi bangsa Indonesia."),
        ScoutHoliday("Hari Sumpah Pemuda", 10, 28, "Meneguhkan persatuan pemuda dan semangat kebangsaan Gerakan Pramuka."),
        ScoutHoliday("Hari Pahlawan", 11, 10, "Mengenang jasa pahlawan bangsa dengan jiwa patriotik dan rela berkorban.")
    )

    fun currentOrNearestHoliday(): Pair<ScoutHoliday, String> {
        val cal = Calendar.getInstance()
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val exact = scoutHolidays.firstOrNull { it.month == month && it.day == day }
        if (exact != null) return exact to "Hari Ini!"

        val currentDoY = cal.get(Calendar.DAY_OF_YEAR)
        val year = cal.get(Calendar.YEAR)
        var bestHoliday = scoutHolidays.first()
        var bestDiff = Int.MAX_VALUE
        for (h in scoutHolidays) {
            val target = Calendar.getInstance().apply { set(year, h.month - 1, h.day) }
            var diff = target.get(Calendar.DAY_OF_YEAR) - currentDoY
            if (diff < 0) diff += 365
            if (diff < bestDiff) {
                bestDiff = diff
                bestHoliday = h
            }
        }
        return bestHoliday to "$bestDiff hari lagi"
    }

    fun formattedIndonesianDateToday(): String {
        val cal = Calendar.getInstance()
        val days = listOf("Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu")
        val months = listOf(
            "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        )
        val dayName = days[cal.get(Calendar.DAY_OF_WEEK) - 1]
        val dateNum = cal.get(Calendar.DAY_OF_MONTH)
        val monthName = months[cal.get(Calendar.MONTH)]
        val year = cal.get(Calendar.YEAR)
        return String.format(Locale("id", "ID"), "%s, %d %s %d", dayName, dateNum, monthName, year)
    }

    val coreNews: List<ScoutNewsItem> = listOf(
        ScoutNewsItem(
            id = "news_1",
            title = "Penguatan Kecakapan Digital & Navigasi Lapangan Pramuka",
            summary = "Kwartir mendorong latihan gabungan kompas, pemetaan sederhana, dan etika bermedia sosial bagi seluruh gugus depan.",
            sourceLabel = "Info Kepramukaan",
            dateLabel = "Update Nasional"
        ),
        ScoutNewsItem(
            id = "news_2",
            title = "Gerakan Pramuka Peduli Lingkungan: Aksi Nol Sampah Perkemahan",
            summary = "Setiap regu dan sangga dianjurkan menerapkan pemilahan sampah organik dan bawa peralatan makan guna ulang saat berkemah.",
            sourceLabel = "Bina Muda",
            dateLabel = "Edisi Terbaru"
        ),
        ScoutNewsItem(
            id = "news_3",
            title = "Panduan SKU & TKK Terstruktur untuk Evaluasi Mingguan",
            summary = "Pembina dan adik-adik dapat memantau kemajuan syarat kecakapan umum secara bertahap sebelum pelantikan tingkat berikutnya.",
            sourceLabel = "Akademi My Scout",
            dateLabel = "Tips Latihan"
        )
    )

    val builtInMaterials: List<ScoutMaterial> = listOf(
        ScoutMaterial(
            id = "mat_kode_kehormatan",
            title = "Kode Kehormatan: Dwi Satya, Tri Satya & Dasa Darma",
            category = "Kode Kehormatan",
            jenjangTarget = "Semua",
            summary = "Memahami janji (Satya) dan ketentuan moral (Darma) untuk Siaga, Penggalang, dan Penegak.",
            content = """
                Kode Kehormatan Pramuka terdiri dari Janji (Satya) dan Ketentuan Moral (Darma) yang disesuaikan dengan golongan usia:

                1. Golongan Siaga (7-10 tahun)
                • Dwi Satya: Demi kehormatanku aku berjanji akan bersungguh-sungguh menjalankan kewajibanku terhadap Tuhan Yang Maha Esa, Negara Kesatuan Republik Indonesia, dan menurut aturan keluarga; setiap hari berbuat kebaikan.
                • Dwi Darma: Siaga berbakti pada ayah dan ibundanya; Siaga berani dan tidak putus asa.

                2. Golongan Penggalang, Penegak & Pandega
                • Tri Satya: Menjalankan kewajiban terhadap Tuhan, NKRI dan mengamalkan Pancasila; menolong sesama hidup dan ikut serta membangun masyarakat; menepati Dasa Darma.
                • Dasa Darma: 10 pedoman sikap mulai dari Takwa kepada Tuhan Yang Maha Esa hingga Suci dalam pikiran, perkataan, dan perbuatan.
            """.trimIndent(),
            readMinutes = 5
        ),
        ScoutMaterial(
            id = "mat_lambang_sejarah",
            title = "Lambang Tunas Kelapa & Sejarah Gerakan Pramuka",
            category = "Sejarah & Lambang",
            jenjangTarget = "Semua",
            summary = "Makna filosofis Tunas Kelapa karya Soenardjo Atmodipurwo dan jejak kepanduan dunia hingga Indonesia.",
            content = """
                Lambang Gerakan Pramuka adalah bayangan (siluet) Tunas Kelapa yang diciptakan oleh Kak Soenardjo Atmodipurwo dan ditetapkan melalui Keppres No. 238 Tahun 1961.

                Makna Utama Tunas Kelapa:
                • Buah kelapa yang tumbuh (cikal): Pramuka merupakan inti bagi kelangsungan hidup bangsa Indonesia.
                • Buah kelapa dapat bertahan lama: Anggota Pramuka sehat jasmani, rohani, kuat, dan ulet.
                • Kelapa dapat tumbuh di mana saja: Pramuka mampu beradaptasi di berbagai kondisi dan lingkungan.
                • Tumbuh menjulang lurus ke atas: Pramuka memiliki cita-cita tinggi dan teguh pendirian.
                • Akar tumbuh kuat di tanah: Tekad dan keyakinan berlandaskan dasar yang kuat.
                • Pohon yang serba guna: Setiap anggota berguna bagi nusa, bangsa, dan agama.

                Sejarah Singkat:
                Lord Robert Baden-Powell mendirikan kepanduan dunia (1907 di Brownsea Island). Di Indonesia, berbagai organisasi pandu disatukan menjadi Gerakan Pramuka pada 14 Agustus 1961 dengan Bapak Pramuka Indonesia Sri Sultan Hamengkubuwono IX.
            """.trimIndent(),
            readMinutes = 6
        ),
        ScoutMaterial(
            id = "mat_pionering",
            title = "Tali-Temali Dasar & Teknik Pionering",
            category = "Pionering",
            jenjangTarget = "Penggalang",
            summary = "Mengenal perbedaan tali, simpul, ikatan, serta teknik menyusun menara pandang dan tandu darurat.",
            content = """
                Dalam kepramukaan, istilah tali-temali dibedakan menjadi tiga bagian penting:
                • Tali: Bendanya (misal tali pramuka/tambang katun).
                • Simpul: Hubungan antara tali dengan tali.
                • Ikatan: Hubungan antara tali dengan benda lain (seperti tongkat/kayu).

                Simpul & Ikatan Wajib Dikuasai:
                1. Simpul Mati: Menyambung dua utas tali yang sama besar dan tidak licin.
                2. Simpul Pangkal: Memulai dan mengakhiri sebuah ikatan pada tongkat.
                3. Simpul Jangkar: Membuat tandu darurat atau menautkan tali pada cincin/batang.
                4. Simpul Tiang: Membuat sosok tali yang tidak bergeser untuk menambat atau penyelamatan.
                5. Ikatan Palang: Mengikat dua tongkat yang bersilangan tegak lurus (90 derajat).
                6. Ikatan Silang: Mengikat dua tongkat yang bersilangan membentuk sudut miring/X.
            """.trimIndent(),
            readMinutes = 5
        ),
        ScoutMaterial(
            id = "mat_pbb",
            title = "Peraturan Baris-Berbaris (PBB) & Aba-Aba",
            category = "PBB",
            jenjangTarget = "Penggalang",
            summary = "Jenis aba-aba (petunjuk, peringatan, pelaksanaan), sikap sempurna, dan gerakan di tempat.",
            content = """
                PBB melatih ketangkasan, kekompakan regu, kedisiplinan, dan kepatuhan terhadap pimpinan barisan.

                Tiga Bagian Aba-Aba:
                1. Aba-aba Petunjuk: Menegaskan maksud perintah (contoh: "Kepada Pemimpin Upacara...").
                2. Aba-aba Peringatan: Inti perintah yang cukup jelas (contoh: "Hormat...", "Hadap kanan...").
                3. Aba-aba Pelaksanaan: Saat tepat pelaksanaan gerak ("...GERAK!", "...JALAN!", "...MULAI!").

                Gerakan Dasar di Tempat:
                • Sikap Sempurna ("Siap, GERAK!"): Badan tegap, tumit rapat, ujung kaki membuka 45 derajat, tangan mengepal di samping jahitan celana/rok.
                • Istirahat di Tempat: Kaki kiri dibuka selebar bahu, tangan kiri memegang pergelangan tangan kanan di belakang pinggang.
                • Hadap Kanan/Kiri & Balik Kanan: Menggunakan poros tumit dengan tiga hitungan rapi tanpa mengayun badan berlebihan.
            """.trimIndent(),
            readMinutes = 4
        ),
        ScoutMaterial(
            id = "mat_p3k",
            title = "Pertolongan Pertama Pada Kecelakaan (P3K)",
            category = "P3K",
            jenjangTarget = "Penegak",
            summary = "Prinsip DRABC, penanganan luka ringan, kram, mimisan, dan penggunaan pembalut mitela.",
            content = """
                P3K adalah pertolongan sementara yang diberikan kepada korban sebelum ditangani tenaga medis profesional.

                Langkah Awal Evaluasi (DRABC):
                • Danger: Pastikan lokasi aman bagi penolong dan korban.
                • Response: Cek kesadaran korban dengan memanggil dan menepuk bahu perlahan.
                • Airways & Breathing: Pastikan jalan napas terbuka dan korban bernapas normal.
                • Circulation: Periksa tanda perdarahan aktif atau syok.

                Penanganan Kasus Umum di Perkemahan:
                1. Luka Lecet/Sayat Ringan: Cuci dengan air bersih mengalir, bersihkan area sekitar luka, beri antiseptik, dan tutup kasa steril.
                2. Mimisan: Dudukkan korban sedikit condong ke depan (jangan menengadah!), pencet cuping hidung lunak selama 10 menit dan kompres dingin di pangkal hidung.
                3. Keseleo (Metode RICE): Rest (istirahatkan), Ice (kompres dingin terbungkus kain), Compression (balut elastis tidak terlalu ketat), Elevation (tinggikan bagian cedera).
            """.trimIndent(),
            readMinutes = 6
        ),
        ScoutMaterial(
            id = "mat_kompas_peta",
            title = "Kompas Bidik, Azimuth & Membaca Peta Lapangan",
            category = "Navigasi",
            jenjangTarget = "Penegak",
            summary = "Cara menentukan sudut azimuth, back azimuth, serta membaca garis kontur pada peta topografi.",
            content = """
                Navigasi darat sangat penting saat penjelajahan (hiking) dan pengembaraan.

                Rumus Back Azimuth:
                • Jika Azimuth < 180 derajat -> Back Azimuth = Azimuth + 180 derajat.
                • Jika Azimuth > 180 derajat -> Back Azimuth = Azimuth - 180 derajat.

                Delapan Arah Mata Angin Utama:
                Utara (0°/360°), Timur Laut (45°), Timur (90°), Tenggara (135°), Selatan (180°), Barat Daya (225°), Barat (270°), Barat Laut (315°).

                Membaca Peta Pita & Kontur:
                • Garis kontur rapat menandakan lereng terjal; garis kontur renggang menandakan medan landai.
                • Peta pita dibuat sepanjang lintasan perjalanan dengan mencatat waktu, arah kompas, jarak, serta kenampakan kanan-kiri jalan.
            """.trimIndent(),
            readMinutes = 5
        ),
        ScoutMaterial(
            id = "mat_seragam",
            title = "Aturan Seragam Pramuka (SK Kwarnas 174/2012)",
            category = "Seragam",
            jenjangTarget = "Semua",
            summary = "Kelengkapan seragam harian, pemasangan tanda pengenal, setangan leher, dan warna tingkatan.",
            content = """
                Pakaian Seragam Pramuka berfungsi menumbuhkan jiwa korsa, kerapian, dan identitas anggota Gerakan Pramuka.

                Komponen Seragam Harian:
                • Baju berwarna cokelat muda dan celana/rok berwarna cokelat tua.
                • Setangan leher (hasduk) merah putih dengan cincin (ring) sesuai golongan.
                • Tanda Tutup Kepala, Tanda Pelantikan (di dada kiri untuk putra / kerah untuk putri), Tanda Pandu Dunia (WOSM), Papan Nama, Tanda Lokasi & Nomor Gugus Depan di lengan kanan.

                Warna Identitas Golongan:
                • Siaga: Hijau (#2E7D32)
                • Penggalang: Merah (#C62828)
                • Penegak: Kuning
                • Pandega: Cokelat
            """.trimIndent(),
            readMinutes = 4
        )
    )

    val skuCatalog: List<SkuItem> = listOf(
        // Siaga
        SkuItem("sku_s_1", JenjangPramuka.SIAGA, "Siaga Mula", 1, "Menghafal Dwi Satya & Dwi Darma", "Dapat menyebutkan dan membiasakan sikap Dwi Satya dan Dwi Darma di rumah serta barung."),
        SkuItem("sku_s_2", JenjangPramuka.SIAGA, "Siaga Mula", 2, "Berani & Jujur di Barung", "Menunjukkan sikap jujur saat bermain dan latihan bersama teman barung."),
        SkuItem("sku_s_3", JenjangPramuka.SIAGA, "Siaga Bantu", 3, "Hormat Bendera & Lagu Indonesia Raya", "Mengetahui sikap hormat yang benar saat lagu kebangsaan dinyanyikan."),
        SkuItem("sku_s_4", JenjangPramuka.SIAGA, "Siaga Bantu", 4, "Mengenal Arah Mata Angin Utama", "Menunjukkan arah Utara, Timur, Selatan, dan Barat di halaman sekolah."),
        SkuItem("sku_s_5", JenjangPramuka.SIAGA, "Siaga Tata", 5, "Membuat Simpul Mati & Simpul Hidup", "Mampu mempraktikkan simpul dasar dengan rapi menggunakan tali pramuka."),
        SkuItem("sku_s_6", JenjangPramuka.SIAGA, "Siaga Tata", 6, "Menabung & Merawat Kebersihan", "Memiliki buku tabungan sederhana dan menjaga kerapian seragam Siaga."),

        // Penggalang
        SkuItem("sku_g_1", JenjangPramuka.PENGGALANG, "Ramu", 1, "Mengamalkan Tri Satya & Dasa Darma", "Menjelaskan arti Tri Satya dan memberi contoh penerapan 10 Dasa Darma."),
        SkuItem("sku_g_2", JenjangPramuka.PENGGALANG, "Ramu", 2, "PBB & Upacara Pembukaan Penggalang", "Menguasai gerakan PBB di tempat dan tata upacara bentuk angkare."),
        SkuItem("sku_g_3", JenjangPramuka.PENGGALANG, "Ramu", 3, "Menjelaskan Makna Lambang Tunas Kelapa", "Menceritakan sejarah singkat lambang Gerakan Pramuka dan penciptanya."),
        SkuItem("sku_g_4", JenjangPramuka.PENGGALANG, "Rakit", 4, "Tali-Temali: Pangkal, Jangkar, Tiang, Mati", "Mampu mempraktikkan minimal 4 simpul/ikatan dan membuat tiang bendera sederhana."),
        SkuItem("sku_g_5", JenjangPramuka.PENGGALANG, "Rakit", 5, "Menggunakan Kompas & Peta Pita", "Menentukan azimuth sasaran di lapangan dan membuat catatan peta pita."),
        SkuItem("sku_g_6", JenjangPramuka.PENGGALANG, "Terap", 6, "Sandi Morse & Semaphore Dasar", "Menerima dan mengirim pesan pendek menggunakan peluit Morse atau bendera Semaphore."),
        SkuItem("sku_g_7", JenjangPramuka.PENGGALANG, "Terap", 7, "Pertolongan Pertama Luka Ringan", "Mempraktikkan pembersihan luka ringan dan pembalutan mitela dengan benar."),

        // Penegak
        SkuItem("sku_t_1", JenjangPramuka.PENEGAK, "Bantara", 1, "Aktif Mengelola Musyawarah Sangga", "Memimpin rapat kerja sangga dan menyusun notulensi kegiatan Ambalan."),
        SkuItem("sku_t_2", JenjangPramuka.PENEGAK, "Bantara", 2, "Mengetahui Sejarah Kepanduan Dunia & RI", "Menjelaskan berdirinya kepanduan oleh Baden-Powell hingga Hari Pramuka 14 Agustus."),
        SkuItem("sku_t_3", JenjangPramuka.PENEGAK, "Bantara", 3, "Merancang Proyek Pionering atau Baksos", "Membuat rancangan konstruksi pionering atau program bakti masyarakat."),
        SkuItem("sku_t_4", JenjangPramuka.PENEGAK, "Laksana", 4, "Navigasi Kompas, Resection & Peta Kontur", "Melakukan bidikan azimuth, back azimuth, dan orientasi medan lapangan."),
        SkuItem("sku_t_5", JenjangPramuka.PENEGAK, "Laksana", 5, "Penanganan Kegawatdaruratan P3K", "Menguasai prosedur DRABC serta evakuasi tandu darurat bersama sangga."),
        SkuItem("sku_t_6", JenjangPramuka.PENEGAK, "Laksana", 6, "Menjadi Teladan & Pendamping Adik Satuan", "Membantu Pembina melatih keterampilan dasar bagi Penggalang atau Siaga.")
    )

    val tkkCatalog: List<TkkItem> = listOf(
        TkkItem("tkk_p3k", "Pertolongan Pertama (P3K)", "Kesehatan & Ketangkasan", "Bidang Putih", "Mampu menangani luka ringan, pingsan, dan menyiapkan kotak P3K regu."),
        TkkItem("tkk_berkemah", "Berkemah", "Keterampilan & Pembangunan", "Bidang Hijau", "Menguasai pemilihan tapak kemah, pendirian tenda, parit drainase, dan kebersihan."),
        TkkItem("tkk_pengamat", "Pengamat", "Patriotisme & Seni", "Bidang Merah", "Mampu mengamati jejak, ciri lingkungan, dan mengingat detail benda lapangan."),
        TkkItem("tkk_juru_masak", "Juru Masak", "Keterampilan & Pembangunan", "Bidang Hijau", "Menyusun menu sehat bergizi seimbang serta memasak higienis di alam terbuka."),
        TkkItem("tkk_penabung", "Penabung", "Sosial & Kebangsaan", "Bidang Biru", "Memiliki kebiasaan menabung rutin dan mengelola keuangan pribadi secara hemat."),
        TkkItem("tkk_gerak_jalan", "Gerak Jalan", "Kesehatan & Ketangkasan", "Bidang Putih", "Menguasai ketahanan berjalan jauh secara berkelompok dengan aturan keselamatan jalan."),
        TkkItem("tkk_juru_peta", "Juru Peta", "Keterampilan & Pembangunan", "Bidang Hijau", "Mampu membuat peta pita, peta lapangan, dan membaca arah mata angin."),
        TkkItem("tkk_pengatur_rumah", "Pengatur Ruangan", "Sosial & Kebangsaan", "Bidang Biru", "Menata ruang sekretariat gudep atau rumah dengan rapi, sehat, dan nyaman.")
    )

    val quizQuestions: List<QuizQuestion> = listOf(
        QuizQuestion(
            id = "q1",
            category = "Sejarah Pramuka",
            question = "Siapakah pencipta lambang Tunas Kelapa Gerakan Pramuka?",
            options = listOf("Sri Sultan HB IX", "Soenardjo Atmodipurwo", "Lord Baden-Powell", "H. Agus Salim"),
            correctIndex = 1,
            explanation = "Soenardjo Atmodipurwo adalah tokoh yang merancang siluet Tunas Kelapa sebagai lambang Gerakan Pramuka pada tahun 1961."
        ),
        QuizQuestion(
            id = "q2",
            category = "Hari Besar",
            question = "Tanggal berapakah diperingati sebagai Hari Pramuka Indonesia?",
            options = listOf("22 Februari", "9 Maret", "14 Agustus", "28 Oktober"),
            correctIndex = 2,
            explanation = "Hari Pramuka Indonesia diperingati setiap 14 Agustus sejak pelantikan pengurus dan penganugerahan Panji Gerakan Pramuka tahun 1961."
        ),
        QuizQuestion(
            id = "q3",
            category = "Kode Kehormatan",
            question = "Kode kehormatan yang berisi janji bagi Pramuka Siaga disebut...",
            options = listOf("Tri Satya", "Dwi Satya", "Dasa Darma", "Panca Satya"),
            correctIndex = 1,
            explanation = "Golongan Siaga menggunakan Dwi Satya (janji) dan Dwi Darma (ketentuan moral), sedangkan Penggalang/Penegak menggunakan Tri Satya dan Dasa Darma."
        ),
        QuizQuestion(
            id = "q4",
            category = "Tali-Temali",
            question = "Simpul apa yang paling tepat digunakan untuk memulai dan mengakhiri ikatan pada tongkat?",
            options = listOf("Simpul Mati", "Simpul Tiang", "Simpul Pangkal", "Simpul Kembar"),
            correctIndex = 2,
            explanation = "Simpul Pangkal (clove hitch) mengikat erat pada permukaan kayu atau tongkat sehingga menjadi awal dan akhir ikatan palang maupun silang."
        ),
        QuizQuestion(
            id = "q5",
            category = "Kompas & Navigasi",
            question = "Jika sudut azimuth sasaran adalah 65 derajat, berapakah Back Azimuth-nya?",
            options = listOf("115 derajat", "245 derajat", "295 derajat", "180 derajat"),
            correctIndex = 1,
            explanation = "Karena Azimuth < 180°, Back Azimuth = 65° + 180° = 245°."
        ),
        QuizQuestion(
            id = "q6",
            category = "Sandi & Morse",
            question = "Huruf 'S' dan 'O' dalam sandi Morse secara berurutan dilambangkan dengan...",
            options = listOf("... dan ---", "--- dan ...", ".-. dan -.-", ".. dan --"),
            correctIndex = 0,
            explanation = "Huruf S adalah tiga titik (...) dan huruf O adalah tiga garis (---), membentuk sinyal darurat internasional SOS (... --- ...)."
        ),
        QuizQuestion(
            id = "q7",
            category = "P3K",
            question = "Posisi kepala yang benar saat memberikan pertolongan pertama pada korban mimisan adalah...",
            options = listOf("Mendongak penuh ke atas", "Berbaring telentang tanpa bantal", "Duduk sedikit condong ke depan", "Menunduk menyentuh lutut"),
            correctIndex = 2,
            explanation = "Posisi duduk agak condong ke depan mencegah darah mengalir masuk ke tenggorokan atau saluran napas."
        ),
        QuizQuestion(
            id = "q8",
            category = "Dasa Darma",
            question = "Bunyi Dasa Darma ke-7 adalah...",
            options = listOf(
                "Rela menolong dan tabah",
                "Hemat, cermat, dan bersahaja",
                "Rajin, terampil, dan gembira",
                "Disiplin, berani, dan setia"
            ),
            correctIndex = 1,
            explanation = "Urutan Dasa Darma ke-5: Rela menolong dan tabah; ke-6: Rajin, terampil, dan gembira; ke-7: Hemat, cermat, dan bersahaja."
        )
    )

    val defaultPackingList: List<PackingItem> = listOf(
        PackingItem("pack_1", "Seragam Pramuka Lengkap & Hasduk", "Seragam", false),
        PackingItem("pack_2", "Tali Pramuka (5 meter) & Tongkat", "Perlengkapan", false),
        PackingItem("pack_3", "Peluit & Senter/Headlamp", "Lapangan", false),
        PackingItem("pack_4", "Botol Minum Guna Ulang (1 Liter)", "Pribadi", false),
        PackingItem("pack_5", "Alat Makan (Piring, Sendok, Gelas)", "Pribadi", false),
        PackingItem("pack_6", "Jas Hujan / Ponco", "Lapangan", false),
        PackingItem("pack_7", "Matras & Kantong Tidur (Sleeping Bag)", "Tidur", false),
        PackingItem("pack_8", "Buku Saku SKU & Alat Tulis", "Perlengkapan", false),
        PackingItem("pack_9", "Obat Pribadi & Plester P3K", "P3K", false),
        PackingItem("pack_10", "Pakaian Ganti & Jaket Hangat", "Pribadi", false)
    )

    val defaultReguMembers: List<ReguMember> = listOf(
        ReguMember("mem_1", "Raka Pratama", "Pemimpin Regu", "Penggalang", true),
        ReguMember("mem_2", "Nadia Kusuma", "Wakil Pemimpin", "Penggalang", true),
        ReguMember("mem_3", "Dimas Aditya", "Sekretaris & Logistik", "Penggalang", true),
        ReguMember("mem_4", "Sinta Dewi", "Bendahara & P3K", "Penggalang", false)
    )

    val defaultActivities: List<ScoutActivityItem> = listOf(
        ScoutActivityItem(
            id = "act_1",
            title = "Latihan Rutin Mingguan: Tali-Temali & PBB",
            dateLabel = "Sabtu, 14:30 WIB",
            location = "Lapangan Utama Gugus Depan",
            description = "Praktik simpul pangkal, ikatan palang, dan formasi upacara pembukaan.",
            attended = false
        ),
        ScoutActivityItem(
            id = "act_2",
            title = "Uji SKU & Penjelajahan Kompas",
            dateLabel = "Minggu Depan, 07:30 WIB",
            location = "Taman Hutan Kota / Bumi Perkemahan",
            description = "Pembuatan peta pita regu dan evaluasi bidikan kompas azimuth.",
            attended = false
        ),
        ScoutActivityItem(
            id = "act_3",
            title = "Persiapan Perkemahan Sabtu-Minggu (Persami)",
            dateLabel = "Akhir Bulan",
            location = "Lapangan Belakang Sekolah",
            description = "Pengecekan tenda regu, pembagian tugas masak, dan gladi api unggun.",
            attended = false
        )
    )
}
