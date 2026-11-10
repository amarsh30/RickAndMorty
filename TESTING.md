# Panduan Automation Testing — RickAndMorty

Dokumen ini menjelaskan **apa** yang dites, **kenapa** dites dengan cara tersebut, dan **bagaimana** menjalankan serta menambah test baru.

---

## 1. Gambaran Besar

Proyek ini memakai **Clean Architecture**, prinsip **SOLID**, dan **Koin** untuk Dependency Injection. Arsitektur ini membuat setiap layer bisa dites **terpisah**, karena tiap layer hanya bergantung pada *interface*, bukan implementasi konkret.

```
┌──────────────────────────────────────────────────────────────┐
│  Presentation  (feature:character)                           │
│  CharacterScreen ──► CharacterViewModel                      │  ← UI Test + ViewModel Test
├──────────────────────────────────────────────────────────────┤
│  Domain  (core:domain)                                       │
│  ObserveCharactersUseCase / RefreshCharactersUseCase         │  ← UseCase Test
│  CharacterRepository (interface)                             │
├──────────────────────────────────────────────────────────────┤
│  Data  (core:data)                                           │
│  CharacterRepositoryImpl, Mapper                             │  ← Repository + Mapper Test
│  CharacterRemoteDataSource / CharacterLocalDataSource (interface)
├──────────────────────────────────────────────────────────────┤
│  DI  (shared)                                                │
│  networkModule, databaseModule, dataModule, domainModule ... │  ← Koin Graph Test
└──────────────────────────────────────────────────────────────┘
```

### Piramida Test

```
            ▲   UI Test (4)          → lambat, butuh emulator, paling "mirip user"
           ▲▲▲  Koin / Integrasi (2)
         ▲▲▲▲▲▲ Unit Test (18)       → cepat, jalan di JVM, paling banyak
```

Sebagian besar test adalah **unit test** karena cepat dan stabil. UI test hanya untuk alur penting yang dilihat user.

---

## 2. Daftar Test

| Layer | File | Jumlah | Jalan di |
|---|---|---|---|
| Domain | `core/domain/src/commonTest/.../usecase/CharacterUseCasesTest.kt` | 4 | JVM & Emulator |
| Data | `core/data/src/commonTest/.../repository/CharacterRepositoryImplTest.kt` | 4 | JVM & Emulator |
| Data | `core/data/src/commonTest/.../mapper/CharacterMapperTest.kt` | 4 | JVM & Emulator |
| Presentation | `feature/character/src/commonTest/.../presentation/CharacterViewModelTest.kt` | 6 | JVM & Emulator |
| Presentation (UI) | `feature/character/src/androidDeviceTest/.../presentation/CharacterScreenTest.kt` | 4 | **Emulator saja** |
| DI | `shared/src/commonTest/.../di/KoinModulesTest.kt` | 2 | JVM & Emulator |

### Fake (pengganti dependency asli)

| File | Menggantikan |
|---|---|
| `core/data/src/commonTest/.../fake/FakeCharacterRemoteDataSource.kt` | API (Ktor) |
| `core/data/src/commonTest/.../fake/FakeCharacterLocalDataSource.kt` | Database (Room) |
| `core/domain/src/commonTest/.../fake/FakeCharacterRepository.kt` | Repository (untuk test UseCase) |
| `feature/character/src/commonTest/.../fake/FakeCharacterRepository.kt` | Repository (untuk test ViewModel & UI) |

---

## 3. Penjelasan Tiap Jenis Test

### 3.1 UseCase Test — `CharacterUseCasesTest`

**Tujuan:** memastikan UseCase meneruskan data dan error dari repository dengan benar.

| Test | Skenario |
|---|---|
| `observeCharactersUseCase_emitsRepositoryData` | Data di repository → keluar dari UseCase |
| `getCharactersUseCase_emitsRepositoryData` | Sama, untuk `GetCharactersUseCase` |
| `refreshCharactersUseCase_delegatesToRepository` | Memanggil `refreshCharacters()` tepat 1 kali |
| `refreshCharactersUseCase_propagatesError` | Error dari repository diteruskan, tidak "ditelan" |

UseCase adalah Kotlin murni, jadi cukup diberi `FakeCharacterRepository`.

### 3.2 Repository Test — `CharacterRepositoryImplTest`

**Tujuan:** memastikan strategi **offline-first** berjalan benar: API → simpan ke DB → UI membaca dari DB.

| Test | Skenario |
|---|---|
| `observeCharacters_initially_emitsEmptyList` | DB kosong → list kosong |
| `refreshCharacters_whenApiSucceeds_savesMappedDataToLocal` | API sukses → DTO di-map ke Domain → tersimpan di DB |
| `refreshCharacters_whenApiSucceeds_replacesOldCache` | Data baru menggantikan cache lama |
| `refreshCharacters_whenApiFails_throwsAndKeepsLocalCache` | API gagal → error dilempar, **cache lama tetap aman** |

### 3.3 Mapper Test — `CharacterMapperTest`

**Tujuan:** memastikan konversi `CharacterDto` / `CharacterEntity` ↔ `Character` tidak kehilangan data, termasuk `imageUrl = null`.

### 3.4 ViewModel Test — `CharacterViewModelTest`

**Tujuan:** memastikan `CharacterUiState` berubah sesuai kondisi.

| Test | State yang dicek |
|---|---|
| `initialState_isLoading` | Awal: `isLoading = true` |
| `init_whenRefreshSucceeds_showsCharacters` | Sukses: list terisi, tanpa error |
| `init_whenRefreshFailsAndNoCache_showsLoadError` | Gagal & tanpa cache: *"Gagal memuat karakter..."* |
| `init_whenRefreshFailsWithCache_showsCachedDataAndWarning` | Gagal & ada cache: *"Gagal memperbarui data..."* |
| `refresh_whileRefreshing_isIgnored` | Klik refresh berkali-kali → API hanya dipanggil 1 kali |
| `refresh_afterError_clearsErrorOnSuccess` | Retry berhasil → pesan error hilang |

**Teknik penting — mengganti `Dispatchers.Main`:**

```kotlin
private val testDispatcher = StandardTestDispatcher()

@BeforeTest fun setUp()    { Dispatchers.setMain(testDispatcher) }
@AfterTest  fun tearDown() { Dispatchers.resetMain() }

@Test
fun contoh() = runTest(testDispatcher) {
    val viewModel = createViewModel()
    advanceUntilIdle()          // jalankan semua coroutine sampai selesai
    // assert state...
}
```

`viewModelScope` memakai `Dispatchers.Main`, yang tidak tersedia di unit test. Karena itu Main diganti dengan `TestDispatcher` supaya urutan coroutine bisa dikontrol (deterministik).

### 3.5 UI Test — `CharacterScreenTest`

**Tujuan:** memastikan layar benar-benar menampilkan hal yang benar dan tombol berfungsi. Layar **digambar di emulator**.

| Test | Yang terjadi di layar |
|---|---|
| `whenRefreshSucceeds_showsCharacterList` | Muncul "Rick Sanchez" & "Morty Smith" |
| `whenRefreshFailsWithoutCache_showsErrorAndRetryWorks` | Muncul error → **klik "Coba Lagi"** → list muncul |
| `whenRefreshFailsWithCache_showsCachedListAndWarning` | List lama tampil + teks peringatan |
| `whenApiReturnsEmpty_showsEmptyStateAndLoadButtonWorks` | "Belum ada karakter" → **klik "Muat Karakter"** → list muncul |

Pola dasar UI test:

```kotlin
@Test
fun contoh() = runComposeUiTest {
    setContent { CharacterScreen(viewModel = createViewModel(repository)) }  // 1. tampilkan layar

    onNodeWithText("Coba Lagi").performClick()                              // 2. aksi user
    onNodeWithText("Rick Sanchez").assertIsDisplayed()                       // 3. cek hasil
}
```

`CharacterScreen` menerima `viewModel` sebagai parameter, sehingga test bisa menyuntikkan ViewModel dengan fake repository **tanpa Koin**.

### 3.6 Koin Graph Test — `KoinModulesTest`

**Tujuan:** menangkap kesalahan DI (misalnya `NoDefinitionFoundException`) **sebelum aplikasi crash**.

| Test | Skenario |
|---|---|
| `commonModules_resolveWholeGraph` | Semua dependency bisa dibuat sampai `CharacterViewModel` |
| `repository_isSingleton` | `CharacterRepository` selalu instance yang sama |

Module asli (`commonModules`) dipakai, tetapi dependency platform (Ktor & Room) di-*override* dengan fake:

```kotlin
startKoin {
    allowOverride(true)
    modules(commonModules + fakeOverrides)
}
```

---

## 4. Kenapa Pakai Fake, Bukan Mock?

| | Fake | Mock (Mockito/MockK) |
|---|---|---|
| Jalan di semua target KMP (Android, iOS) | ✅ | ❌ MockK/Mockito hanya JVM |
| Butuh library tambahan | ❌ | ✅ |
| Mudah dibaca | ✅ Kode Kotlin biasa | Perlu memahami DSL mock |

Fake di proyek ini punya "tombol kontrol":

| Properti | Fungsi |
|---|---|
| `refreshResult` / `response` | Data yang dikembalikan saat sukses |
| `errorToThrow` | Paksa terjadi error |
| `failWithNoInternet()` | Shortcut untuk `NetworkException.NoInternet` |
| `refreshCount` / `callCount` | Hitung berapa kali dipanggil |
| `gate` (`CompletableDeferred`) | Tahan refresh di tengah jalan untuk mengecek `isRefreshing` |

---

## 5. Cara Menjalankan

> Selalu lakukan **Sync Project with Gradle Files** (🐘 / `Ctrl+Shift+O`) setelah mengubah `build.gradle.kts`. Kalau belum sync, import test akan berwarna merah.

### 5.0 Perintah Singkat (Rekomendasi)

Task ini didaftarkan di **setiap modul** oleh convention plugin `amr.kmp.test` (lihat bagian 6). Jika dijalankan dari root, Gradle otomatis mengeksekusinya di **semua modul** yang memakai plugin tersebut. Modul baru langsung ikut tanpa perlu didaftarkan manual.

| Perintah | Yang dijalankan | Butuh emulator? | Perkiraan waktu |
|---|---|---|---|
| `./gradlew unitTest` | Semua unit test (UseCase, Repository, Mapper, ViewModel, Koin) di JVM | ❌ | ± 20 detik |
| `./gradlew uiTest` | Device test di modul yang memakai Compose (UI test + test lain di modul itu) | ✅ | ± 40 detik |
| `./gradlew deviceTest` | Semua test di semua modul, dijalankan di emulator | ✅ | ± 1–2 menit |
| `./gradlew allTest` | `unitTest` + `deviceTest` (paling lengkap) | ✅ | ± 2–3 menit |

**Hanya UI test** (khusus `CharacterScreenTest`, tanpa test lain):

```bash
./gradlew uiTest "-Pandroid.testInstrumentationRunnerArguments.class=com.amr.feature.character.presentation.CharacterScreenTest"
```

**Satu test tertentu di emulator** (format `NamaClass#namaFungsi`):

```bash
./gradlew uiTest "-Pandroid.testInstrumentationRunnerArguments.class=com.amr.feature.character.presentation.CharacterScreenTest#whenRefreshFailsWithoutCache_showsErrorAndRetryWorks"
```

**Satu class unit test di JVM:**

```bash
./gradlew :feature:character:testAndroidHostTest --tests "*CharacterViewModelTest"
```

**Kapan pakai yang mana?**

- Sedang coding / sebelum commit → `unitTest` (cepat).
- Mengubah tampilan / Composable → `uiTest`.
- Sebelum merge / rilis → `allTest`.

**Hanya satu modul:**

```bash
./gradlew :core:data:unitTest
./gradlew :feature:character:uiTest
```

> Di Android Studio, task ini juga bisa dijalankan dari panel **Gradle** (kanan) → pilih modul → `Tasks` → `verification` → klik dua kali `unitTest` / `uiTest` / `deviceTest` / `allTest`.

Bagian di bawah ini adalah perintah detail per modul jika dibutuhkan.

### 5.1 Di JVM (cepat, tanpa emulator) — untuk sehari-hari

```bash
# Semua modul
./gradlew testAndroidHostTest

# Satu modul
./gradlew :feature:character:testAndroidHostTest

# Satu class
./gradlew :feature:character:testAndroidHostTest --tests "*CharacterViewModelTest"
```

Laporan HTML: `<modul>/build/reports/tests/testAndroidHostTest/index.html`

### 5.2 Di Emulator / HP — termasuk UI test

1. Nyalakan emulator (**Device Manager** → ▶) atau sambungkan HP dengan USB debugging.
2. Jalankan:

```bash
# Semua modul
./gradlew connectedAndroidDeviceTest

# Hanya feature character (termasuk UI test)
./gradlew :feature:character:connectedAndroidDeviceTest
```

Laporan HTML: `<modul>/build/reports/androidTests/connected/androidMain/index.html`

### 5.3 Dari Android Studio

Buka file test → klik ▶ di samping nama class atau fungsi → pilih target:

- **androidHostTest** → JVM
- **androidDeviceTest** → emulator

> Unit test (ViewModel, UseCase, dll.) **tidak menampilkan apa pun** di layar emulator, karena hanya menguji logic. Hanya `CharacterScreenTest` yang menggambar layar.

---

## 6. Konfigurasi Gradle (Penjelasan)

### 6.1 Convention Plugin `amr.kmp.test`

Semua konfigurasi test **tidak lagi ditulis di tiap modul**, melainkan dikumpulkan dalam satu *convention plugin*. Ini pola yang direkomendasikan Gradle dan dipakai di proyek contoh Google *Now in Android*.

```
gradle/build-logic/                      ← included build (didaftarkan di settings.gradle.kts)
├── settings.gradle.kts                  ← memakai libs.versions.toml yang sama
└── convention/
    ├── build.gradle.kts                 ← mendaftarkan plugin id "amr.kmp.test"
    └── src/main/kotlin/
        └── KmpTestConventionPlugin.kt   ← SEMUA konfigurasi test ada di sini
```

Di `settings.gradle.kts` root:

```kotlin
pluginManagement {
    includeBuild("gradle/build-logic") {
        name = "convention-plugins"   // dibedakan dari modul template :build-logic
    }
}
```

Setiap modul cukup menambahkan **satu baris**:

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    id("amr.kmp.test")   // ← ini saja
}
```

Saat ini dipakai oleh: `core:domain`, `core:data`, `feature:character`, `shared`.

**Yang dilakukan plugin secara otomatis:**

| Kondisi | Yang disiapkan |
|---|---|
| Semua modul | `withHostTest`, `withDeviceTestBuilder`, dependency `kotlin-test` + `kotlinx-coroutines-test` + `androidx.test:runner`, task `unitTest` / `deviceTest` / `allTest` |
| Modul memakai plugin Compose (`org.jetbrains.compose`) | Tambahan `androidResources.enable = true`, dependency UI test (`ui-test`, `ui-test-manifest`, `espresso-core`), task `uiTest` |

### 6.2 Isi konfigurasi (yang dikerjakan plugin)

Kode di bawah ini **sudah tidak perlu ditulis manual**. Ditampilkan agar Anda paham apa yang terjadi di balik `id("amr.kmp.test")`:

```kotlin
android {
    // Aktifkan test di JVM lokal → task: testAndroidHostTest
    withHostTest {}

    // Aktifkan test di emulator → task: connectedAndroidDeviceTest
    // sourceSetTreeName = "test" → commonTest ikut dijalankan di emulator
    withDeviceTestBuilder {
        sourceSetTreeName = "test"
    }.configure {
        instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

sourceSets {
    commonTest.dependencies {
        implementation(libs.kotlin.test)              // @Test, assertEquals, ...
        implementation(libs.kotlinx.coroutines.test)  // runTest, TestDispatcher
    }
    getByName("androidDeviceTest").dependencies {
        implementation(libs.androidx.testRunner)
    }
}
```

Tambahan untuk modul Compose (otomatis terdeteksi):

```kotlin
android {
    androidResources { enable = true }  // wajib untuk plugin Compose Resources di device test
}

getByName("androidDeviceTest").dependencies {
    implementation(libs.compose.ui.test)                    // runComposeUiTest, onNodeWithText
    implementation(libs.androidx.compose.ui.test.manifest)  // Activity kosong untuk wadah layar
    implementation(libs.androidx.espresso.core)             // versi baru, kompatibel Android 17
}
```

### Lokasi source set

| Folder | Jalan di | Isi |
|---|---|---|
| `src/commonTest/` | JVM **dan** emulator | Unit test (logic) |
| `src/androidDeviceTest/` | Emulator saja | UI test Compose |

---

## 7. Cara Menambah Test Baru

### Contoh: fitur baru `LocationRepository`

1. **Buat fake** untuk dependency-nya di `core/data/src/commonTest/.../fake/`.
2. **Tulis test** dengan pola **Arrange – Act – Assert**:

```kotlin
class LocationRepositoryImplTest {

    @Test
    fun refresh_whenApiSucceeds_savesToLocal() = runTest {
        // Arrange — siapkan kondisi
        val remote = FakeLocationRemoteDataSource()
        val local = FakeLocationLocalDataSource()
        val repository = LocationRepositoryImpl(remote, local)

        // Act — jalankan aksi
        repository.refreshLocations()

        // Assert — cek hasil
        assertEquals(2, local.observeLocations().first().size)
    }
}
```

3. **Jika menambah module Koin**, tambahkan juga ke `commonModules`. `KoinModulesTest` otomatis akan mengecek apakah graph-nya valid.

### Menambah modul baru (mis. `:feature:location`)

Cukup tambahkan `id("amr.kmp.test")` di blok `plugins {}` modul tersebut (letakkan **setelah** plugin KMP & Android library). Setelah Gradle Sync:

- `./gradlew unitTest` / `deviceTest` / `allTest` dari root **otomatis** ikut menjalankan test modul baru.
- Jika modulnya memakai Compose, task `uiTest` dan dependency UI test juga otomatis tersedia.
- Tidak perlu mengubah `build.gradle.kts` root maupun menyalin konfigurasi test.

### Mengubah konfigurasi test untuk semua modul

Edit satu file saja: `gradle/build-logic/convention/src/main/kotlin/KmpTestConventionPlugin.kt`. Contoh: menambah library test baru (misalnya Turbine) cukup di sana, dan semua modul langsung mendapatkannya.

### Konvensi penamaan test

```
namaFungsi_kondisi_hasilYangDiharapkan
```

Contoh: `refreshCharacters_whenApiFails_keepsLocalCache`

---

## 8. Troubleshooting

| Masalah | Penyebab | Solusi |
|---|---|---|
| Import `kotlinx.coroutines.test` merah | IDE belum Gradle Sync | Sync Project with Gradle Files |
| File di `gradle/build-logic` merah semua (bahkan `` `kotlin-dsl` ``) | IDE belum memuat included build | Tunggu Gradle Sync selesai; jika tetap merah: File → Invalidate Caches → Restart. Cek dengan `./gradlew :convention-plugins:convention:compileKotlin` |
| Tidak ada task `unitTest` / `testAndroidHostTest` di sebuah modul | Modul belum memakai plugin | Tambahkan `id("amr.kmp.test")` di blok `plugins {}` modul tersebut |
| `Module with the Main dispatcher is missing` | Lupa `Dispatchers.setMain` | Tambahkan di `@BeforeTest` |
| `NoSuchMethodException: InputManager.getInstance` | Espresso lama tidak kompatibel Android 17 | Sudah ditangani plugin (`espresso-core` 3.7.0 untuk modul Compose). Jika muncul lagi, cek dependency di `KmpTestConventionPlugin.kt` |
| `outputDirectory doesn't have a configured value` | Plugin Compose Resources | Sudah ditangani plugin (`androidResources.enable = true` untuk modul Compose). Pastikan modul memakai `id("amr.kmp.test")` |
| `Plugin [id: 'amr.kmp.test'] was not found` | `includeBuild` belum ada di `settings.gradle.kts` | Pastikan blok `pluginManagement { includeBuild("gradle/build-logic") { ... } }` ada, lalu Sync |
| `Included build ... has name 'build-logic' which is the same as a project` | Nama bentrok dengan modul template `:build-logic` | Beri nama lain: `includeBuild("gradle/build-logic") { name = "convention-plugins" }` |
| `No connected devices` | Emulator belum menyala | Nyalakan lewat Device Manager, cek dengan `adb devices` |

---

## 9. Catatan / Temuan

- **Spinner loading hampir tidak pernah terlihat.** Di `CharacterViewModel`, `observeCharacters()` langsung menerima list kosong dari DB dan mengubah `isLoading = false` sebelum API selesai. Akibatnya, saat aplikasi pertama kali dibuka tanpa cache, yang tampil adalah "Belum ada karakter", bukan spinner. Karena itu belum ada UI test untuk state loading.
- `FakeCharacterRepository` ada di dua modul (domain & feature), karena source set test tidak bisa dipakai bersama antar modul. Jika fake makin banyak, pertimbangkan membuat modul khusus `:core:testing`.

### Ide test berikutnya

- **Room DAO test** dengan database in-memory di `core:database` (`androidDeviceTest`).
- **`CharacterRemoteDataSourceImpl` test** dengan Ktor `MockEngine` untuk mengecek parsing JSON & mapping error HTTP → `NetworkException`.
- **Screenshot test** untuk `CharacterCard` / `CharacterScreen`.
