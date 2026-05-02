# Plan 3: Sangeet Notes Editor Android App

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a native Android app (`sangeet-android`) using Kotlin + Jetpack Compose that consumes the `sangeet-core` Scala 3 JAR directly as an in-process library. All editor logic, layout computation, serialization, and export run locally on the device with zero network dependency.

**Architecture:** The Android app owns all mutable state (composition, cursor, undo history, edit mode). It calls `sangeet-core` API functions directly via JVM interop -- no HTTP, no server. The core JAR is published to local Maven by sbt and consumed by Gradle. Rendering uses Jetpack Compose Canvas to draw Bhatkhande notation. Touch input replaces keyboard shortcuts with an on-screen swar keyboard.

**Package:** `com.varpas.sangeet.android`

**Prerequisites:** Plan 1 (desktop rebuild with `sangeet-core` extraction) must be completed first. The `sangeet-core` JAR must exist and be publishable via `sbt publishLocal`.

**Tech Stack:** Kotlin 1.9+, Jetpack Compose (BOM 2024+), Material Design 3, Android SDK 26-34, Gradle 8+, sangeet-core (Scala 3 JAR)

---

### Task 1: Android Project Setup

**Files:**
- Create: `sangeet-android/build.gradle.kts`
- Create: `sangeet-android/settings.gradle.kts`
- Create: `sangeet-android/gradle.properties`
- Create: `sangeet-android/gradle/wrapper/gradle-wrapper.properties`
- Create: `sangeet-android/src/main/AndroidManifest.xml`
- Create: `sangeet-android/src/main/java/com/varpas/sangeet/android/SangeetApp.kt`
- Create: `sangeet-android/src/main/java/com/varpas/sangeet/android/MainActivity.kt`

- [ ] **Step 1: Create Gradle project skeleton**

Create `sangeet-android/settings.gradle.kts`:
```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        mavenLocal() // sangeet-core JAR from sbt publishLocal
    }
}

rootProject.name = "sangeet-android"
```

- [ ] **Step 2: Create build.gradle.kts with Compose and sangeet-core dependency**

Create `sangeet-android/build.gradle.kts`:
```kotlin
plugins {
    id("com.android.application") version "8.2.0"
    id("org.jetbrains.kotlin.android") version "1.9.22"
}

android {
    namespace = "com.varpas.sangeet.android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.varpas.sangeet.android"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.8" }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

dependencies {
    // sangeet-core from local Maven (sbt publishLocal)
    implementation("com.varpas:sangeet-core_3:1.0.0-SNAPSHOT")

    // Scala 3 standard library (required at runtime)
    implementation("org.scala-lang:scala3-library_3:3.3.1")

    // Jetpack Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.01.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")

    // Lifecycle + ViewModel
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // PDF export (Apache PDFBox -- Android-compatible subset)
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

- [ ] **Step 3: Create AndroidManifest.xml**

Create `sangeet-android/src/main/AndroidManifest.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
        android:maxSdkVersion="28" />

    <application
        android:name=".SangeetApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.Sangeet">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>

            <!-- Handle .swar file opens -->
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <data android:mimeType="application/json" />
                <data android:pathPattern=".*\\.swar" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 4: Create Application class**

Create `sangeet-android/src/main/java/com/varpas/sangeet/android/SangeetApp.kt`:
```kotlin
package com.varpas.sangeet.android

import android.app.Application

class SangeetApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Verify sangeet-core JAR loads on Android runtime
        // This will throw early if any incompatible Java API is used
        val testTaal = sangeet.taal.Taals.teentaal()
        require(testTaal.matras() == 16) { "sangeet-core JAR failed to load correctly" }
    }
}
```

- [ ] **Step 5: Create minimal MainActivity with Compose**

Create `sangeet-android/src/main/java/com/varpas/sangeet/android/MainActivity.kt`:
```kotlin
package com.varpas.sangeet.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.varpas.sangeet.android.ui.theme.SangeetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SangeetTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Text("Sangeet Notes Editor")
                }
            }
        }
    }
}
```

- [ ] **Step 6: Create Material You theme**

Create `sangeet-android/src/main/java/com/varpas/sangeet/android/ui/theme/Theme.kt`:
```kotlin
package com.varpas.sangeet.android.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val SangeetLightColorScheme = lightColorScheme(
    primary = Color(0xFF1A237E),        // dark indigo (swar color)
    secondary = Color(0xFF4A148C),      // deep purple (ornament color)
    tertiary = Color(0xFF00695C),       // teal (stroke color)
    error = Color(0xFFB71C1C),          // dark red (taal marker)
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
)

@Composable
fun SangeetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        else -> SangeetLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
```

- [ ] **Step 7: Create Gradle wrapper and properties**

Create `sangeet-android/gradle.properties`:
```properties
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
```

Initialize the Gradle wrapper:
```bash
cd sangeet-android && gradle wrapper --gradle-version 8.5
```

- [ ] **Step 8: Verify build compiles and sangeet-core loads**

First, publish sangeet-core to local Maven:
```bash
cd .. && sbt "sangeetCore/publishLocal"
```

Then build the Android project:
```bash
cd sangeet-android && ./gradlew assembleDebug
```

Expected: APK builds successfully. The sangeet-core classes are included in the DEX output.

- [ ] **Step 9: Commit**

```bash
git add sangeet-android/
git commit -m "feat(android): initialize Android project with Kotlin + Jetpack Compose

- Gradle project targeting SDK 26-34 with Compose BOM
- sangeet-core JAR consumed from local Maven
- Material You theme with sangeet color palette
- Minimal MainActivity with Compose setup"
```

---

### Task 2: Makefile Integration

**Files:**
- Modify: `Makefile` (project root)

- [ ] **Step 1: Add sangeet-core publish target**

Add to the root `Makefile`:
```makefile
## --- Android ---

.PHONY: core-publish android-debug android-release android-clean

# Publish sangeet-core JAR to local Maven repository
core-publish:
	sbt "sangeetCore/publishLocal"

# Build Android debug APK (publishes core first)
android-debug: core-publish
	cd sangeet-android && ./gradlew assembleDebug
	@echo "APK: sangeet-android/build/outputs/apk/debug/sangeet-android-debug.apk"

# Build Android release APK
android-release: core-publish
	cd sangeet-android && ./gradlew assembleRelease

# Clean Android build artifacts
android-clean:
	cd sangeet-android && ./gradlew clean

# Install debug APK on connected device
android-install: android-debug
	cd sangeet-android && ./gradlew installDebug
```

- [ ] **Step 2: Verify `make android-debug` works end-to-end**

```bash
make android-debug
```

Expected: sbt publishes JAR, Gradle builds APK, path printed to stdout.

- [ ] **Step 3: Commit**

```bash
git add Makefile
git commit -m "build: add Makefile targets for Android builds

- make core-publish: sbt publishLocal for sangeet-core
- make android-debug: builds debug APK (auto-publishes core first)
- make android-install: installs APK on connected device"
```

---

### Task 3: Domain Model Bridge (Kotlin-Scala Interop)

**Files:**
- Create: `sangeet-android/src/main/java/com/varpas/sangeet/android/bridge/CoreBridge.kt`
- Create: `sangeet-android/src/main/java/com/varpas/sangeet/android/bridge/TypeAliases.kt`
- Create: `sangeet-android/src/main/java/com/varpas/sangeet/android/bridge/Extensions.kt`
- Create: `sangeet-android/src/test/java/com/varpas/sangeet/android/bridge/CoreBridgeTest.kt`

Scala 3 case classes compile to standard JVM classes with getter methods. Kotlin can call them, but the ergonomics differ: Scala case class fields become methods (not properties), Scala `List` is not `kotlin.collections.List`, and Scala enums compile to sealed hierarchies. This task creates a thin bridge layer for ergonomic Kotlin usage.

- [ ] **Step 1: Create type aliases for commonly used Scala types**

Create `sangeet-android/src/main/java/com/varpas/sangeet/android/bridge/TypeAliases.kt`:
```kotlin
package com.varpas.sangeet.android.bridge

// Type aliases for sangeet-core Scala types used frequently in Android code.
// These provide shorter names and a single import point.

// Domain model
typealias Composition = sangeet.model.Composition
typealias Metadata = sangeet.model.Metadata
typealias Section = sangeet.model.Section
typealias Event = sangeet.model.Event
typealias Note = sangeet.model.Note
typealias Variant = sangeet.model.Variant
typealias Octave = sangeet.model.Octave
typealias Stroke = sangeet.model.Stroke
typealias Laya = sangeet.model.Laya
typealias SwarScript = sangeet.model.SwarScript
typealias Taal = sangeet.model.Taal
typealias Raag = sangeet.model.Raag
typealias BeatPosition = sangeet.model.BeatPosition
typealias Rational = sangeet.model.Rational
typealias SectionType = sangeet.model.SectionType
typealias CompositionType = sangeet.model.CompositionType
typealias Ornament = sangeet.model.Ornament

// Editor types
typealias CursorModel = sangeet.model.CursorModel
typealias EditorInput = sangeet.core.EditorInput
typealias EditorResult = sangeet.core.EditorResult

// Layout types
typealias SectionGrid = sangeet.model.SectionGrid
typealias GridLine = sangeet.model.GridLine
typealias BeatCell = sangeet.model.BeatCell
typealias LayoutConfig = sangeet.layout.LayoutConfig

// Audio types
typealias TimedNote = sangeet.model.TimedNote
```

- [ ] **Step 2: Create Kotlin extension functions for Scala interop ergonomics**

Create `sangeet-android/src/main/java/com/varpas/sangeet/android/bridge/Extensions.kt`:
```kotlin
package com.varpas.sangeet.android.bridge

import scala.jdk.javaapi.CollectionConverters

/**
 * Extension functions to bridge Scala collections and types to Kotlin-idiomatic code.
 * Scala 3 case class fields compile as methods (e.g., composition.title()),
 * but Kotlin can call them naturally: composition.title()
 *
 * Scala List <-> Kotlin List conversion is the main friction point.
 */

// Convert Scala List to Kotlin List
fun <T> scala.collection.immutable.List<T>.toKotlinList(): List<T> {
    return CollectionConverters.asJava(this).toList()
}

// Convert Kotlin List to Scala List
fun <T> List<T>.toScalaList(): scala.collection.immutable.List<T> {
    val javaList = java.util.ArrayList(this)
    return CollectionConverters.asScala(javaList)
        .toList() as scala.collection.immutable.List<T>
}

// Convert Scala Option to Kotlin nullable
fun <T> scala.Option<T>.toNullable(): T? {
    return if (this.isDefined()) this.get() else null
}

// Convert Kotlin nullable to Scala Option
fun <T> T?.toScalaOption(): scala.Option<T> {
    return if (this != null) scala.Option.apply(this) else scala.Option.empty()
}

// Convenience: get sections as Kotlin list
fun Composition.sectionsList(): List<Section> = this.sections().toKotlinList()

// Convenience: get events as Kotlin list
fun Section.eventsList(): List<Event> = this.events().toKotlinList()

// Convenience: get grid lines as Kotlin list
fun SectionGrid.linesList(): List<GridLine> = this.lines().toKotlinList()

// Convenience: get cells as Kotlin list
fun GridLine.cellsList(): List<BeatCell> = this.cells().toKotlinList()

// Convenience: get events in a cell as Kotlin list
fun BeatCell.eventsList(): List<Event> = this.events().toKotlinList()
```

- [ ] **Step 3: Create CoreBridge -- main entry point for sangeet-core API calls**

Create `sangeet-android/src/main/java/com/varpas/sangeet/android/bridge/CoreBridge.kt`:
```kotlin
package com.varpas.sangeet.android.bridge

import sangeet.core.*
import sangeet.taal.Taals
import sangeet.raag.Raags

/**
 * Kotlin-friendly facade over sangeet-core API objects.
 * Converts between Scala and Kotlin types at the boundary.
 * All methods are stateless pure functions (thread-safe).
 */
object CoreBridge {

    // --- Reference Data ---

    fun allTaals(): List<Taal> =
        ReferenceApi.allTaals().toKotlinList()

    fun allRaags(): List<Raag> =
        ReferenceApi.allRaags().toKotlinList()

    fun taalByName(name: String): Taal? =
        ReferenceApi.taalByName(name).toOption().toNullable()

    fun raagByName(name: String): Raag? =
        ReferenceApi.raagByName(name).toOption().toNullable()

    // --- Composition Operations ---

    data class CreateResult(val composition: Composition, val cursor: CursorModel)

    fun createComposition(
        title: String,
        compositionType: CompositionType,
        taal: Taal,
        raag: Raag,
        laya: Laya?,
        taanCount: Int = 0,
        showStrokeLine: Boolean = false,
        showSahityaLine: Boolean = false
    ): CreateResult {
        val result = CompositionApi.createComposition(
            title, compositionType, taal, raag,
            laya.toScalaOption(), taanCount,
            showStrokeLine, showSahityaLine
        )
        return CreateResult(result._1(), result._2())
    }

    fun parseComposition(json: String): Composition? {
        val result = CompositionApi.parseComposition(json)
        return if (result.isRight()) result.toOption().get() as Composition else null
    }

    fun serializeComposition(composition: Composition): String =
        CompositionApi.serializeComposition(composition)

    // --- Editor Operations ---

    fun insertSwar(
        input: EditorInput,
        note: Note,
        shiftDown: Boolean
    ): EditorResult? {
        val result = EditorApi.insertSwar(input, note, shiftDown)
        return if (result.isRight()) result.toOption().get() as EditorResult else null
    }

    fun insertRest(input: EditorInput): EditorResult? {
        val result = EditorApi.insertRest(input)
        return if (result.isRight()) result.toOption().get() as EditorResult else null
    }

    fun insertSustain(input: EditorInput): EditorResult? {
        val result = EditorApi.insertSustain(input)
        return if (result.isRight()) result.toOption().get() as EditorResult else null
    }

    fun deleteLastEvent(input: EditorInput): EditorResult? {
        val result = EditorApi.deleteLastEvent(input)
        return if (result.isRight()) result.toOption().get() as EditorResult else null
    }

    fun insertDualSwar(
        input: EditorInput,
        note: Note,
        shiftDown: Boolean
    ): EditorResult? {
        val result = EditorApi.insertDualSwar(input, note, shiftDown)
        return if (result.isRight()) result.toOption().get() as EditorResult else null
    }

    // --- Layout ---

    fun computeLayout(
        composition: Composition,
        config: LayoutConfig = LayoutConfig()
    ): List<SectionGrid> =
        LayoutApi.computeLayout(composition, config).toKotlinList()

    // --- Playback ---

    fun schedulePlayback(
        events: scala.collection.immutable.List<Event>,
        bpm: Double,
        matras: Int
    ): List<TimedNote> =
        PlaybackApi.schedulePlayback(events, bpm, matras).toKotlinList()

    // --- Glyph Data ---

    fun noteGlyph(
        note: Note,
        variant: Variant,
        octave: Octave,
        script: SwarScript
    ): GlyphApi.GlyphInfo =
        GlyphApi.noteGlyph(note, variant, octave, script)

    fun notationColors(): GlyphApi.ColorPalette =
        GlyphApi.notationColors()
}
```

- [ ] **Step 4: Create unit test verifying Kotlin can call sangeet-core types**

Create `sangeet-android/src/test/java/com/varpas/sangeet/android/bridge/CoreBridgeTest.kt`:
```kotlin
package com.varpas.sangeet.android.bridge

import org.junit.Assert.*
import org.junit.Test

class CoreBridgeTest {

    @Test
    fun `can list all taals`() {
        val taals = CoreBridge.allTaals()
        assertTrue("Should have at least 11 built-in taals", taals.size >= 11)

        val teentaal = taals.find { it.name() == "Teentaal" }
        assertNotNull("Teentaal should exist", teentaal)
        assertEquals(16, teentaal!!.matras())
    }

    @Test
    fun `can list all raags`() {
        val raags = CoreBridge.allRaags()
        assertTrue("Should have at least 26 built-in raags", raags.size >= 26)

        val yaman = raags.find { it.name() == "Yaman" }
        assertNotNull("Yaman should exist", yaman)
    }

    @Test
    fun `can create a composition`() {
        val taal = CoreBridge.taalByName("Teentaal")!!
        val raag = CoreBridge.raagByName("Yaman")!!
        val laya = sangeet.model.Laya.Vilambit()

        val result = CoreBridge.createComposition(
            title = "Test Gat",
            compositionType = sangeet.model.CompositionType.Gat(),
            taal = taal,
            raag = raag,
            laya = laya,
            taanCount = 2,
            showStrokeLine = true
        )

        assertEquals("Test Gat", result.composition.metadata().title())
        assertEquals(0, result.cursor.cycle())
        assertEquals(0, result.cursor.beat())
    }

    @Test
    fun `can insert swar and get updated state`() {
        val taal = CoreBridge.taalByName("Teentaal")!!
        val raag = CoreBridge.raagByName("Yaman")!!

        val created = CoreBridge.createComposition(
            title = "Test",
            compositionType = sangeet.model.CompositionType.Gat(),
            taal = taal,
            raag = raag,
            laya = sangeet.model.Laya.Vilambit()
        )

        val input = EditorInput(created.composition, 0, created.cursor)
        val result = CoreBridge.insertSwar(input, sangeet.model.Note.Ga(), false)

        assertNotNull("Insert should succeed", result)
        assertTrue("Message should mention Ga", result!!.message().contains("Ga"))
    }

    @Test
    fun `can serialize and parse composition roundtrip`() {
        val taal = CoreBridge.taalByName("Teentaal")!!
        val raag = CoreBridge.raagByName("Yaman")!!

        val created = CoreBridge.createComposition(
            title = "Roundtrip Test",
            compositionType = sangeet.model.CompositionType.Gat(),
            taal = taal,
            raag = raag,
            laya = sangeet.model.Laya.Madhya()
        )

        val json = CoreBridge.serializeComposition(created.composition)
        val parsed = CoreBridge.parseComposition(json)

        assertNotNull("Parse should succeed", parsed)
        assertEquals("Roundtrip Test", parsed!!.metadata().title())
    }

    @Test
    fun `can compute layout`() {
        val taal = CoreBridge.taalByName("Teentaal")!!
        val raag = CoreBridge.raagByName("Yaman")!!

        val created = CoreBridge.createComposition(
            title = "Layout Test",
            compositionType = sangeet.model.CompositionType.Gat(),
            taal = taal,
            raag = raag,
            laya = sangeet.model.Laya.Vilambit()
        )

        val grids = CoreBridge.computeLayout(created.composition)
        assertTrue("Should have at least 1 section grid", grids.isNotEmpty())
    }

    @Test
    fun `scala list to kotlin list conversion works`() {
        val taal = CoreBridge.taalByName("Teentaal")!!
        val vibhags = taal.vibhags().toKotlinList()
        assertEquals(4, vibhags.size)
    }
}
```

- [ ] **Step 5: Run tests**

```bash
cd sangeet-android && ./gradlew test
```

Expected: All CoreBridgeTest tests pass, confirming sangeet-core interop works.

- [ ] **Step 6: Commit**

```bash
git add sangeet-android/src/main/java/com/varpas/sangeet/android/bridge/
git add sangeet-android/src/test/
git commit -m "feat(android): add Kotlin-Scala bridge layer for sangeet-core interop

- Type aliases for all sangeet-core domain types
- Extension functions for Scala List/Option <-> Kotlin conversion
- CoreBridge facade with Kotlin-idiomatic API
- Unit tests verifying roundtrip: create, insert, serialize, parse, layout"
```

---

### Task 4: Android App Architecture (ViewModel + State Management)

**Files:**
- Create: `sangeet-android/src/main/java/com/varpas/sangeet/android/viewmodel/EditorViewModel.kt`
- Create: `sangeet-android/src/main/java/com/varpas/sangeet/android/viewmodel/EditorState.kt`
- Create: `sangeet-android/src/main/java/com/varpas/sangeet/android/viewmodel/UndoManager.kt`
- Create: `sangeet-android/src/test/java/com/varpas/sangeet/android/viewmodel/EditorViewModelTest.kt`

- [ ] **Step 1: Define editor state data classes**

Create `sangeet-android/src/main/java/com/varpas/sangeet/android/viewmodel/EditorState.kt`:
```kotlin
package com.varpas.sangeet.android.viewmodel

import com.varpas.sangeet.android.bridge.*

/**
 * Immutable snapshot of the entire editor state.
 * Compose observes this via StateFlow for recomposition.
 */
data class EditorState(
    val composition: Composition? = null,
    val cursor: CursorModel? = null,
    val currentSectionIndex: Int = 0,
    val grids: List<SectionGrid> = emptyList(),
    val editMode: EditMode = EditMode.SwarEdit,
    val ornamentMode: OrnamentMode? = null,
    val script: SwarScript = sangeet.model.SwarScript.Devanagari(),
    val bpm: Double = 60.0,
    val isPlaying: Boolean = false,
    val statusMessage: String = "",
    val filePath: String? = null,
    val isModified: Boolean = false
) {
    val hasComposition: Boolean get() = composition != null
    val currentSection: Section? get() =
        composition?.sectionsList()?.getOrNull(currentSectionIndex)
}

enum class EditMode { SwarEdit, StrokeEdit }

sealed class OrnamentMode {
    data object MeendStartAsc : OrnamentMode()
    data object MeendStartDesc : OrnamentMode()
    data class MeendEnd(val startNote: NoteRef, val ascending: Boolean) : OrnamentMode()
    data object KrintanStart : OrnamentMode()
    data class KrintanEnd(val firstNote: NoteRef) : OrnamentMode()
    data object KanSwarPending : OrnamentMode()
    data object SparshPending : OrnamentMode()
    data object GhaseetPending : OrnamentMode()
    data class MurkiCollect(val notes: List<NoteRef> = emptyList()) : OrnamentMode()
    data class ZamzamaCollect(val notes: List<NoteRef> = emptyList()) : OrnamentMode()
}

// Alias for NoteRef used in ornaments
typealias NoteRef = sangeet.model.NoteRef
```

- [ ] **Step 2: Create UndoManager**

Create `sangeet-android/src/main/java/com/varpas/sangeet/android/viewmodel/UndoManager.kt`:
```kotlin
package com.varpas.sangeet.android.viewmodel

import com.varpas.sangeet.android.bridge.Composition
import com.varpas.sangeet.android.bridge.CursorModel

/**
 * Undo/redo stack managing composition + cursor snapshots.
 * Immutable -- each operation returns a new UndoManager instance.
 */
data class UndoSnapshot(
    val composition: Composition,
    val cursor: CursorModel,
    val sectionIndex: Int
)

data class UndoManager(
    val past: List<UndoSnapshot> = emptyList(),
    val present: UndoSnapshot,
    val future: List<UndoSnapshot> = emptyList(),
    val maxSize: Int = 50
) {
    val canUndo: Boolean get() = past.isNotEmpty()
    val canRedo: Boolean get() = future.isNotEmpty()

    fun push(snapshot: UndoSnapshot): UndoManager {
        val newPast = (past + present).takeLast(maxSize)
        return copy(past = newPast, present = snapshot, future = emptyList())
    }

    fun undo(): UndoManager? {
        if (!canUndo) return null
        val previous = past.last()
        return copy(
            past = past.dropLast(1),
            present = previous,
            future = listOf(present) + future
        )
    }

    fun redo(): UndoManager? {
        if (!canRedo) return null
        val next = future.first()
        return copy(
            past = past + present,
            present = next,
            future = future.drop(1)
        )
    }
}
```

- [ ] **Step 3: Create EditorViewModel**

Create `sangeet-android/src/main/java/com/varpas/sangeet/android/viewmodel/EditorViewModel.kt`:
```kotlin
package com.varpas.sangeet.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.varpas.sangeet.android.bridge.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EditorViewModel : ViewModel() {

    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    private var undoManager: UndoManager? = null

    // --- Composition Lifecycle ---

    fun createComposition(
        title: String,
        compositionType: CompositionType,
        taal: Taal,
        raag: Raag,
        laya: Laya?,
        taanCount: Int = 0,
        showStrokeLine: Boolean = false,
        showSahityaLine: Boolean = false
    ) {
        val result = CoreBridge.createComposition(
            title, compositionType, taal, raag, laya,
            taanCount, showStrokeLine, showSahityaLine
        )
        val grids = CoreBridge.computeLayout(result.composition)
        val snapshot = UndoSnapshot(result.composition, result.cursor, 0)
        undoManager = UndoManager(present = snapshot)

        _state.value = EditorState(
            composition = result.composition,
            cursor = result.cursor,
            currentSectionIndex = 0,
            grids = grids,
            statusMessage = "New composition: $title"
        )
    }

    fun loadComposition(json: String, filePath: String? = null) {
        val composition = CoreBridge.parseComposition(json) ?: run {
            _state.value = _state.value.copy(statusMessage = "Failed to parse .swar file")
            return
        }
        val taal = composition.metadata().taal()
        val cursor = sangeet.model.CursorModel(taal, 0, 0, 0, 1, sangeet.model.Octave.Madhya())
        val grids = CoreBridge.computeLayout(composition)
        val snapshot = UndoSnapshot(composition, cursor, 0)
        undoManager = UndoManager(present = snapshot)

        _state.value = EditorState(
            composition = composition,
            cursor = cursor,
            grids = grids,
            filePath = filePath,
            statusMessage = "Opened: ${composition.metadata().title()}"
        )
    }

    // --- Swar Input ---

    fun insertSwar(note: Note, shiftDown: Boolean = false) {
        val s = _state.value
        val comp = s.composition ?: return
        val cursor = s.cursor ?: return

        val input = EditorInput(comp, s.currentSectionIndex, cursor)
        val result = CoreBridge.insertSwar(input, note, shiftDown) ?: run {
            _state.value = s.copy(statusMessage = "Insert failed")
            return
        }

        pushResult(result)
    }

    fun insertDualSwar(note: Note, shiftDown: Boolean = false) {
        val s = _state.value
        val comp = s.composition ?: return
        val cursor = s.cursor ?: return

        val input = EditorInput(comp, s.currentSectionIndex, cursor)
        val result = CoreBridge.insertDualSwar(input, note, shiftDown) ?: return
        pushResult(result)
    }

    fun insertRest() {
        val s = _state.value
        val comp = s.composition ?: return
        val cursor = s.cursor ?: return

        val input = EditorInput(comp, s.currentSectionIndex, cursor)
        val result = CoreBridge.insertRest(input) ?: return
        pushResult(result)
    }

    fun insertSustain() {
        val s = _state.value
        val comp = s.composition ?: return
        val cursor = s.cursor ?: return

        val input = EditorInput(comp, s.currentSectionIndex, cursor)
        val result = CoreBridge.insertSustain(input) ?: return
        pushResult(result)
    }

    fun deleteLastEvent() {
        val s = _state.value
        val comp = s.composition ?: return
        val cursor = s.cursor ?: return

        val input = EditorInput(comp, s.currentSectionIndex, cursor)
        val result = CoreBridge.deleteLastEvent(input) ?: run {
            _state.value = s.copy(statusMessage = "Nothing to delete")
            return
        }
        pushResult(result)
    }

    // --- Cursor ---

    fun setOctave(octave: Octave) {
        val s = _state.value
        val cursor = s.cursor ?: return
        val newCursor = sangeet.core.CursorApi.setOctave(cursor, octave)
        _state.value = s.copy(cursor = newCursor)
    }

    fun nextBeat() {
        val s = _state.value
        val cursor = s.cursor ?: return
        val newCursor = sangeet.core.CursorApi.nextBeat(cursor)
        _state.value = s.copy(cursor = newCursor)
    }

    fun prevBeat() {
        val s = _state.value
        val cursor = s.cursor ?: return
        val newCursor = sangeet.core.CursorApi.prevBeat(cursor)
        _state.value = s.copy(cursor = newCursor)
    }

    fun moveTo(cycle: Int, beat: Int) {
        val s = _state.value
        val cursor = s.cursor ?: return
        val newCursor = sangeet.core.CursorApi.moveTo(cursor, cycle, beat)
        _state.value = s.copy(cursor = newCursor)
    }

    fun setSubdivisions(n: Int) {
        val s = _state.value
        val cursor = s.cursor ?: return
        val newCursor = sangeet.core.CursorApi.setSubdivisions(cursor, n)
        _state.value = s.copy(cursor = newCursor)
    }

    // --- Section Navigation ---

    fun switchSection(index: Int) {
        val s = _state.value
        val comp = s.composition ?: return
        val sections = comp.sectionsList()
        if (index < 0 || index >= sections.size) return

        val taal = comp.metadata().taal()
        val newCursor = sangeet.model.CursorModel(taal, 0, 0, 0, 1, sangeet.model.Octave.Madhya())
        _state.value = s.copy(
            currentSectionIndex = index,
            cursor = newCursor,
            statusMessage = "Section: ${sections[index].name()}"
        )
    }

    // --- Script ---

    fun setScript(script: SwarScript) {
        _state.value = _state.value.copy(script = script)
    }

    // --- BPM ---

    fun setBpm(bpm: Double) {
        _state.value = _state.value.copy(bpm = bpm)
    }

    // --- Undo/Redo ---

    fun undo() {
        val manager = undoManager ?: return
        val newManager = manager.undo() ?: return
        undoManager = newManager
        applySnapshot(newManager.present, "Undo")
    }

    fun redo() {
        val manager = undoManager ?: return
        val newManager = manager.redo() ?: return
        undoManager = newManager
        applySnapshot(newManager.present, "Redo")
    }

    // --- Edit Mode ---

    fun toggleStrokeEditMode() {
        val s = _state.value
        val newMode = if (s.editMode == EditMode.SwarEdit) EditMode.StrokeEdit else EditMode.SwarEdit
        _state.value = s.copy(editMode = newMode, statusMessage = "${newMode.name} mode")
    }

    // --- Internal ---

    private fun pushResult(result: EditorResult) {
        val s = _state.value
        val grids = CoreBridge.computeLayout(result.composition())
        val snapshot = UndoSnapshot(result.composition(), result.cursor(), s.currentSectionIndex)
        undoManager = undoManager?.push(snapshot)

        _state.value = s.copy(
            composition = result.composition(),
            cursor = result.cursor(),
            grids = grids,
            isModified = true,
            statusMessage = result.message()
        )
    }

    private fun applySnapshot(snapshot: UndoSnapshot, action: String) {
        val grids = CoreBridge.computeLayout(snapshot.composition)
        _state.value = _state.value.copy(
            composition = snapshot.composition,
            cursor = snapshot.cursor,
            currentSectionIndex = snapshot.sectionIndex,
            grids = grids,
            statusMessage = action
        )
    }
}
```

- [ ] **Step 4: Create ViewModel unit tests**

Create `sangeet-android/src/test/java/com/varpas/sangeet/android/viewmodel/EditorViewModelTest.kt`:
```kotlin
package com.varpas.sangeet.android.viewmodel

import com.varpas.sangeet.android.bridge.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class EditorViewModelTest {

    private lateinit var vm: EditorViewModel

    @Before
    fun setup() {
        vm = EditorViewModel()
        val taal = CoreBridge.taalByName("Teentaal")!!
        val raag = CoreBridge.raagByName("Yaman")!!
        vm.createComposition(
            title = "Test",
            compositionType = sangeet.model.CompositionType.Gat(),
            taal = taal,
            raag = raag,
            laya = sangeet.model.Laya.Vilambit(),
            showStrokeLine = true
        )
    }

    @Test
    fun `createComposition sets initial state`() {
        val state = vm.state.value
        assertNotNull(state.composition)
        assertNotNull(state.cursor)
        assertEquals(0, state.currentSectionIndex)
        assertTrue(state.grids.isNotEmpty())
    }

    @Test
    fun `insertSwar advances cursor and updates composition`() {
        vm.insertSwar(sangeet.model.Note.Ga(), false)
        val state = vm.state.value
        assertTrue(state.isModified)
        assertTrue(state.statusMessage.contains("Ga"))
    }

    @Test
    fun `undo reverts to previous state`() {
        vm.insertSwar(sangeet.model.Note.Ga(), false)
        vm.insertSwar(sangeet.model.Note.Ma(), false)

        val afterTwo = vm.state.value
        vm.undo()
        val afterUndo = vm.state.value

        assertNotEquals(afterTwo.composition, afterUndo.composition)
    }

    @Test
    fun `redo restores undone state`() {
        vm.insertSwar(sangeet.model.Note.Ga(), false)
        val afterInsert = vm.state.value

        vm.undo()
        vm.redo()
        val afterRedo = vm.state.value

        assertEquals(afterInsert.composition, afterRedo.composition)
    }

    @Test
    fun `switchSection resets cursor`() {
        vm.switchSection(1)
        val state = vm.state.value
        assertEquals(1, state.currentSectionIndex)
        assertEquals(0, state.cursor!!.cycle())
        assertEquals(0, state.cursor!!.beat())
    }
}
```

- [ ] **Step 5: Run tests**

```bash
cd sangeet-android && ./gradlew test
```

Expected: All ViewModel tests pass.

- [ ] **Step 6: Commit**

```bash
git add sangeet-android/src/main/java/com/varpas/sangeet/android/viewmodel/
git add sangeet-android/src/test/java/com/varpas/sangeet/android/viewmodel/
git commit -m "feat(android): add EditorViewModel with state management and undo/redo

- EditorState data class with all editor state fields
- UndoManager with immutable snapshot stack (max 50)
- EditorViewModel: create, insert, delete, cursor, section, undo/redo
- Unit tests for ViewModel lifecycle"
```

---

### Task 5: Notation Canvas (Jetpack Compose Canvas)

**Files:**
- Create: `sangeet-android/src/main/java/com/varpas/sangeet/android/ui/canvas/NotationCanvas.kt`
- Create: `sangeet-android/src/main/java/com/varpas/sangeet/android/ui/canvas/NotationDrawScope.kt`
- Create: `sangeet-android/src/main/java/com/varpas/sangeet/android/ui/canvas/NotationColors.kt`
- Create: `sangeet-android/src/main/java/com/varpas/sangeet/android/ui/canvas/CursorRenderer.kt`

- [ ] **Step 1: Create notation color constants**

Create `sangeet-android/src/main/java/com/varpas/sangeet/android/ui/canvas/NotationColors.kt`:
```kotlin
package com.varpas.sangeet.android.ui.canvas

import androidx.compose.ui.graphics.Color

/**
 * Color palette for notation rendering, matching sangeet-core NotationColors.
 * These must stay in sync with the desktop and PDF renderers.
 */
object NotationColors {
    val taalMarker = Color(0xFFB71C1C)        // dark red
    val taalMarkerSam = Color(0xFFD32F2F)     // bright red for Sam (X)
    val swar = Color(0xFF1A237E)              // dark indigo
    val octaveDot = Color(0xFFE65100)         // deep orange
    val ornament = Color(0xFF4A148C)          // deep purple
    val stroke = Color(0xFF00695C)            // teal
    val sahitya = Color(0xFF2E7D32)           // dark green
    val rest = Color(0xFF616161)              // gray
    val sustain = Color(0xFF9E9E9E)           // light gray
    val komalMark = Color(0xFF1A237E)         // same as swar
    val tivraMark = Color(0xFF1A237E)         // same as swar

    // UI chrome
    val cursorSwar = Color(0xFF1976D2)        // blue
    val cursorStroke = Color(0xFFE67800)      // orange
    val activeSectionText = Color(0xFF1976D2) // blue
    val inactiveSectionText = Color(0xFF9E9E9E) // gray
    val vibhagSeparator = Color(0xFF9E9E9E)   // gray
    val subdivisionBracket = Color(0xFF787878) // medium gray
}
```

- [ ] **Step 2: Create NotationDrawScope with row-drawing methods**

Create `sangeet-android/src/main/java/com/varpas/sangeet/android/ui/canvas/NotationDrawScope.kt`:
```kotlin
package com.varpas.sangeet.android.ui.canvas

import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.varpas.sangeet.android.bridge.*

/**
 * Extension functions on DrawScope for drawing notation rows.
 * Mirrors the 5-row layout from the desktop GridRenderer:
 *   Row 1: Taal markers (14px)
 *   Row 2: Subdivision brackets (10px)
 *   Row 3: Ornaments + taar dots (18px)
 *   Row 4: Swar glyphs (18px)
 *   Row 5a: Mandra dots / komal underline (12px)
 *   Row 5b: Strokes (optional, 16px)
 *   Row 5c: Sahitya (optional, 14px)
 */
object NotationDrawScope {

    // Row Y offsets (relative to line top)
    const val MARKER_Y = 0f
    const val BRACKET_Y = 14f
    const val ORNAMENT_Y = 24f
    const val SWAR_Y = 42f
    const val MANDRA_DOT_Y = 54f
    const val STROKE_Y = 58f
    const val SAHITYA_Y = 74f

    // Cell dimensions
    const val CELL_WIDTH_BASE = 60f
    const val CELL_OVERFLOW_EXPAND = 15f

    // Dot radius for octave indicators
    const val DOT_RADIUS = 4f  // slightly larger than desktop 2px for touch readability

    fun DrawScope.drawSectionGrid(
        grid: SectionGrid,
        startY: Float,
        cellWidth: Float,
        startX: Float,
        script: SwarScript,
        showStrokes: Boolean,
        showSahitya: Boolean,
        isActive: Boolean
    ): Float {
        val canvas = drawContext.canvas.nativeCanvas
        var currentY = startY

        // Section header
        val headerPaint = android.graphics.Paint().apply {
            textSize = if (isActive) 42f else 38f
            isFakeBoldText = true
            color = if (isActive) 0xFF1976D2.toInt() else 0xFF9E9E9E.toInt()
            isAntiAlias = true
        }
        val prefix = if (isActive) "> " else "-- "
        canvas.drawText(
            "$prefix${grid.sectionName()}",
            startX,
            currentY + 40f,
            headerPaint
        )
        currentY += 50f

        if (isActive) {
            // Blue underline
            drawLine(
                NotationColors.activeSectionText,
                Offset(startX, currentY),
                Offset(startX + 600f, currentY),
                strokeWidth = 4f
            )
            currentY += 8f
        }

        // Draw each grid line
        for (line in grid.linesList()) {
            currentY = drawGridLine(line, currentY, cellWidth, startX, script, showStrokes, showSahitya)
            currentY += 40f // line spacing
        }

        return currentY
    }

    fun DrawScope.drawGridLine(
        line: GridLine,
        lineTop: Float,
        cellWidth: Float,
        startX: Float,
        script: SwarScript,
        showStrokes: Boolean,
        showSahitya: Boolean
    ): Float {
        val canvas = drawContext.canvas.nativeCanvas
        val cells = line.cellsList()

        // Row 1: Taal markers
        val markers = line.markers()
        // Draw markers at their cell positions
        val markerPaint = android.graphics.Paint().apply {
            textSize = 32f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val scalaMarkers = scala.jdk.javaapi.CollectionConverters.asJava(markers)
        for (entry in scalaMarkers) {
            val tuple = entry as scala.Tuple2<Int, Any>
            val cellIndex = tuple._1() as Int
            val marker = tuple._2()
            val x = startX + cellIndex * cellWidth + cellWidth / 2

            val text = when {
                marker.toString().contains("Sam") -> "X"
                marker.toString().contains("Khali") -> "0"
                marker.toString().contains("Taali") -> {
                    // Extract number from Taali
                    marker.toString().filter { it.isDigit() }.ifEmpty { "?" }
                }
                else -> ""
            }
            markerPaint.color = if (text == "X") 0xFFD32F2F.toInt() else 0xFFB71C1C.toInt()
            canvas.drawText(text, x - 8f, lineTop + MARKER_Y + 28f, markerPaint)
        }

        // Row 4: Swar glyphs
        val swarPaint = android.graphics.Paint().apply {
            textSize = 40f
            color = NotationColors.swar.hashCode()
            isAntiAlias = true
            typeface = Typeface.DEFAULT
        }

        for ((cellIdx, cell) in cells.withIndex()) {
            val cellX = startX + cellIdx * cellWidth
            val events = cell.eventsList()
            val subWidth = if (events.size > 1) cellWidth / events.size else cellWidth

            for ((subIdx, event) in events.withIndex()) {
                val x = cellX + subIdx * subWidth + subWidth / 2
                val y = lineTop + SWAR_Y

                when (event) {
                    is sangeet.model.Event.Swar -> {
                        val glyphInfo = CoreBridge.noteGlyph(
                            event.note(), event.variant(), event.octave(), script
                        )
                        swarPaint.color = 0xFF1A237E.toInt()
                        canvas.drawText(glyphInfo.text(), x - 12f, y, swarPaint)

                        // Octave dots
                        if (glyphInfo.dotCount() > 0) {
                            val dotColor = android.graphics.Paint().apply {
                                color = 0xFFE65100.toInt()
                                isAntiAlias = true
                            }
                            val dotY = if (glyphInfo.dotPosition().toString() == "Above")
                                y - 22f else y + 14f
                            canvas.drawCircle(x, dotY, DOT_RADIUS, dotColor)
                            if (glyphInfo.dotCount() > 1) {
                                canvas.drawCircle(x + DOT_RADIUS * 3, dotY, DOT_RADIUS, dotColor)
                            }
                        }

                        // Komal underline
                        if (glyphInfo.needsKomalMark()) {
                            val underlinePaint = android.graphics.Paint().apply {
                                color = 0xFF1A237E.toInt()
                                strokeWidth = 3f
                                isAntiAlias = true
                            }
                            canvas.drawLine(x - 12f, y + 4f, x + 12f, y + 4f, underlinePaint)
                        }

                        // Tivra overbar
                        if (glyphInfo.needsTivraMark()) {
                            val overbarPaint = android.graphics.Paint().apply {
                                color = 0xFF1A237E.toInt()
                                strokeWidth = 3f
                                isAntiAlias = true
                            }
                            canvas.drawLine(x - 2f, y - 36f, x - 2f, y - 24f, overbarPaint)
                        }
                    }
                    is sangeet.model.Event.Rest -> {
                        swarPaint.color = 0xFF616161.toInt()
                        canvas.drawText("-", x - 4f, y, swarPaint)
                    }
                    is sangeet.model.Event.Sustain -> {
                        swarPaint.color = 0xFF9E9E9E.toInt()
                        canvas.drawText("\u2014", x - 8f, y, swarPaint)
                    }
                    else -> {}
                }
            }
        }

        // Vibhag separators
        val breaks = scala.jdk.javaapi.CollectionConverters.asJava(line.vibhagBreaks())
        for (breakIdx in breaks) {
            val idx = breakIdx as Int
            val x = startX + idx * cellWidth
            drawLine(
                NotationColors.vibhagSeparator,
                Offset(x, lineTop + MARKER_Y - 5f),
                Offset(x, lineTop + SWAR_Y + 20f),
                strokeWidth = 2f
            )
        }

        // Calculate line height
        var lineBottom = lineTop + MANDRA_DOT_Y + 12f
        if (showStrokes) lineBottom = lineTop + STROKE_Y + 16f
        if (showSahitya) lineBottom = lineTop + SAHITYA_Y + 14f

        return lineBottom
    }
}
```

- [ ] **Step 3: Create cursor renderer**

Create `sangeet-android/src/main/java/com/varpas/sangeet/android/ui/canvas/CursorRenderer.kt`:
```kotlin
package com.varpas.sangeet.android.ui.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.varpas.sangeet.android.bridge.CursorModel
import com.varpas.sangeet.android.viewmodel.EditMode

object CursorRenderer {

    fun DrawScope.drawCursor(
        cursor: CursorModel,
        editMode: EditMode,
        lineTop: Float,
        lineBottom: Float,
        cellWidth: Float,
        startX: Float,
        cursorVisible: Boolean
    ) {
        if (!cursorVisible) return

        val x = startX + cursor.beat() * cellWidth + cellWidth - 8f
        val color = when (editMode) {
            EditMode.SwarEdit -> NotationColors.cursorSwar
            EditMode.StrokeEdit -> NotationColors.cursorStroke
        }
        val top = if (editMode == EditMode.StrokeEdit)
            lineTop + NotationDrawScope.STROKE_Y - 10f
        else
            lineTop + NotationDrawScope.MARKER_Y + 4f
        val bottom = if (editMode == EditMode.StrokeEdit)
            lineTop + NotationDrawScope.STROKE_Y + 6f
        else
            lineBottom

        drawLine(
            color,
            Offset(x, top),
            Offset(x, bottom),
            strokeWidth = if (editMode == EditMode.SwarEdit) 5f else 4f
        )
    }
}
```

- [ ] **Step 4: Create main NotationCanvas composable**

Create `sangeet-android/src/main/java/com/varpas/sangeet/android/ui/canvas/NotationCanvas.kt`:
```kotlin
package com.varpas.sangeet.android.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import com.varpas.sangeet.android.bridge.*
import com.varpas.sangeet.android.ui.canvas.CursorRenderer.drawCursor
import com.varpas.sangeet.android.ui.canvas.NotationDrawScope.drawSectionGrid
import com.varpas.sangeet.android.viewmodel.EditMode
import com.varpas.sangeet.android.viewmodel.EditorState
import kotlinx.coroutines.delay

@Composable
fun NotationCanvas(
    state: EditorState,
    onBeatTapped: (cycle: Int, beat: Int) -> Unit,
    onSectionTapped: (index: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Cursor blink state
    var cursorVisible by remember { mutableStateOf(true) }
    LaunchedEffect(state.cursor, state.composition) {
        cursorVisible = true
        while (true) {
            delay(530)
            cursorVisible = !cursorVisible
        }
    }

    val grids = state.grids
    val composition = state.composition ?: return
    val cursor = state.cursor ?: return
    val showStrokes = composition.metadata().showStrokeLine()
    val showSahitya = composition.metadata().showSahityaLine()

    val cellWidth = NotationDrawScope.CELL_WIDTH_BASE
    val startX = 30f
    val matras = composition.metadata().taal().matras()

    // Track section Y bounds for tap handling
    val sectionBounds = remember { mutableListOf<Pair<IntRange, Int>>() }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .pointerInput(grids, cellWidth) {
                detectTapGestures { offset ->
                    // Determine which section was tapped
                    val tappedSection = sectionBounds.indexOfFirst {
                        offset.y.toInt() in it.first
                    }
                    if (tappedSection >= 0 && tappedSection != state.currentSectionIndex) {
                        onSectionTapped(tappedSection)
                    } else {
                        // Compute beat from x coordinate
                        val beatIndex = ((offset.x - startX) / cellWidth).toInt()
                            .coerceIn(0, matras - 1)
                        onBeatTapped(cursor.cycle(), beatIndex)
                    }
                }
            }
    ) {
        sectionBounds.clear()
        var currentY = 20f

        for ((gridIdx, grid) in grids.withIndex()) {
            val sectionTop = currentY.toInt()
            val isActive = gridIdx == state.currentSectionIndex

            currentY = drawSectionGrid(
                grid = grid,
                startY = currentY,
                cellWidth = cellWidth,
                startX = startX,
                script = state.script,
                showStrokes = showStrokes,
                showSahitya = showSahitya,
                isActive = isActive
            )

            sectionBounds.add(Pair(sectionTop..currentY.toInt(), gridIdx))

            // Draw cursor if this is the active section
            if (isActive && grid.linesList().isNotEmpty()) {
                // Find the line containing the cursor's cycle
                for (line in grid.linesList()) {
                    val cells = line.cellsList()
                    if (cells.isNotEmpty()) {
                        val firstBeat = cells.first().position()
                        val lastBeat = cells.last().position()
                        if (cursor.cycle() == firstBeat.cycle() ||
                            (cursor.beat() in firstBeat.beat()..lastBeat.beat())) {
                            drawCursor(
                                cursor = cursor,
                                editMode = state.editMode,
                                lineTop = currentY - 80f, // approximate
                                lineBottom = currentY - 10f,
                                cellWidth = cellWidth,
                                startX = startX,
                                cursorVisible = cursorVisible
                            )
                        }
                    }
                }
            }

            currentY += 20f // gap between sections
        }
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add sangeet-android/src/main/java/com/varpas/sangeet/android/ui/canvas/
git commit -m "feat(android): add notation canvas with Bhatkhande grid rendering

- NotationDrawScope: draws 5 notation rows (markers, ornaments, swar, strokes, sahitya)
- CursorRenderer: blinking cursor for swar and stroke edit modes
- NotationCanvas composable: scrollable canvas with touch-to-beat tap handling
- NotationColors matching desktop palette"
```

---

### Task 6: On-Screen Swar Keyboard

**Files:**
- Create: `sangeet-android/src/main/java/com/varpas/sangeet/android/ui/keyboard/SwarKeyboard.kt`
- Create: `sangeet-android/src/main/java/com/varpas/sangeet/android/ui/keyboard/KeyboardAction.kt`

- [ ] **Step 1: Define keyboard actions**

Create `sangeet-android/src/main/java/com/varpas/sangeet/android/ui/keyboard/KeyboardAction.kt`:
```kotlin
package com.varpas.sangeet.android.ui.keyboard

import com.varpas.sangeet.android.bridge.Note
import com.varpas.sangeet.android.bridge.Octave

sealed class KeyboardAction {
    data class InsertSwar(val note: Note, val shiftDown: Boolean) : KeyboardAction()
    data object InsertRest : KeyboardAction()
    data object InsertSustain : KeyboardAction()
    data object DeleteLast : KeyboardAction()
    data class SetOctave(val octave: Octave) : KeyboardAction()
    data object Undo : KeyboardAction()
    data object Redo : KeyboardAction()
    data object NextBeat : KeyboardAction()
    data object PrevBeat : KeyboardAction()
    data class SetSubdivisions(val n: Int) : KeyboardAction()
}
```

- [ ] **Step 2: Create SwarKeyboard composable**

Create `sangeet-android/src/main/java/com/varpas/sangeet/android/ui/keyboard/SwarKeyboard.kt`:
```kotlin
package com.varpas.sangeet.android.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varpas.sangeet.android.bridge.*
import com.varpas.sangeet.android.ui.canvas.NotationColors

/**
 * On-screen swar keyboard for touch-based note entry.
 * Replaces the desktop physical keyboard shortcuts.
 *
 * Layout:
 * Row 1: Octave selector (Mandra | Madhya | Taar)
 * Row 2: Seven swar buttons: Sa Re Ga Ma Pa Dha Ni
 * Row 3: Variant toggle + special keys (Rest, Sustain, Delete)
 * Row 4: Navigation (Prev, Next) + Undo/Redo + Subdivisions
 */
@Composable
fun SwarKeyboard(
    currentOctave: Octave,
    script: SwarScript,
    onAction: (KeyboardAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var variantActive by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Row 1: Octave selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            OctaveButton("Mandra", sangeet.model.Octave.Mandra(),
                currentOctave, onAction, Modifier.weight(1f))
            OctaveButton("Madhya", sangeet.model.Octave.Madhya(),
                currentOctave, onAction, Modifier.weight(1f))
            OctaveButton("Taar", sangeet.model.Octave.Taar(),
                currentOctave, onAction, Modifier.weight(1f))
        }

        // Row 2: Seven swar buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val notes = listOf(
                "Sa" to sangeet.model.Note.Sa(),
                "Re" to sangeet.model.Note.Re(),
                "Ga" to sangeet.model.Note.Ga(),
                "Ma" to sangeet.model.Note.Ma(),
                "Pa" to sangeet.model.Note.Pa(),
                "Dha" to sangeet.model.Note.Dha(),
                "Ni" to sangeet.model.Note.Ni()
            )
            for ((label, note) in notes) {
                SwarButton(
                    label = label,
                    note = note,
                    shiftDown = variantActive,
                    onAction = { action ->
                        onAction(action)
                        variantActive = false // reset after each note
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Row 3: Variant toggle + special keys
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Variant toggle (Komal/Tivra)
            val variantLabel = if (variantActive) "Komal/Tivra ON" else "Komal/Tivra"
            val variantColor = if (variantActive)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surfaceVariant

            Button(
                onClick = { variantActive = !variantActive },
                modifier = Modifier.weight(2f).height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (variantActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    variantLabel,
                    fontSize = 12.sp,
                    fontWeight = if (variantActive) FontWeight.Bold else FontWeight.Normal
                )
            }

            // Rest
            Button(
                onClick = { onAction(KeyboardAction.InsertRest) },
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Rest", fontSize = 12.sp)
            }

            // Sustain
            Button(
                onClick = { onAction(KeyboardAction.InsertSustain) },
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Hold", fontSize = 12.sp)
            }

            // Delete
            IconButton(
                onClick = { onAction(KeyboardAction.DeleteLast) },
                modifier = Modifier.weight(0.7f)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
            }
        }

        // Row 4: Navigation + Undo/Redo + Subdivisions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onAction(KeyboardAction.PrevBeat) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous beat")
            }
            IconButton(onClick = { onAction(KeyboardAction.NextBeat) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next beat")
            }

            Spacer(Modifier.width(8.dp))

            IconButton(onClick = { onAction(KeyboardAction.Undo) }) {
                Icon(Icons.Default.Undo, contentDescription = "Undo")
            }
            IconButton(onClick = { onAction(KeyboardAction.Redo) }) {
                Icon(Icons.Default.Redo, contentDescription = "Redo")
            }

            Spacer(Modifier.weight(1f))

            // Subdivision selector
            Text("Sub:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            for (n in listOf(1, 2, 3, 4)) {
                TextButton(
                    onClick = { onAction(KeyboardAction.SetSubdivisions(n)) },
                    modifier = Modifier.size(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("$n", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun SwarButton(
    label: String,
    note: sangeet.model.Note,
    shiftDown: Boolean,
    onAction: (KeyboardAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = { onAction(KeyboardAction.InsertSwar(note, shiftDown)) },
        modifier = modifier.height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A237E)
        )
    }
}

@Composable
private fun OctaveButton(
    label: String,
    octave: Octave,
    currentOctave: Octave,
    onAction: (KeyboardAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelected = currentOctave.toString() == octave.toString()
    Button(
        onClick = { onAction(KeyboardAction.SetOctave(octave)) },
        modifier = modifier.height(40.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            label,
            fontSize = 12.sp,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface
        )
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add sangeet-android/src/main/java/com/varpas/sangeet/android/ui/keyboard/
git commit -m "feat(android): add on-screen swar keyboard for touch input

- SwarKeyboard composable with 4 rows: octave, swar, variants+special, nav
- KeyboardAction sealed class mapping touch events to editor operations
- Komal/Tivra toggle replaces Shift key from desktop
- Octave buttons replace ./'/\` modifiers
- Subdivisions selector (1-4)"
```

---

### Task 7: App Chrome (Toolbar, Navigation, Main Screen)

**Files:**
- Create: `sangeet-android/src/main/java/com/varpas/sangeet/android/ui/EditorScreen.kt`
- Create: `sangeet-android/src/main/java/com/varpas/sangeet/android/ui/SangeetTopBar.kt`
- Create: `sangeet-android/src/main/java/com/varpas/sangeet/android/ui/SectionTabs.kt`
- Create: `sangeet-android/src/main/java/com/varpas/sangeet/android/ui/PlaybackControls.kt`
- Modify: `sangeet-android/src/main/java/com/varpas/sangeet/android/MainActivity.kt`

- [ ] **Step 1: Create top app bar with actions**

Create `sangeet-android/src/main/java/com/varpas/sangeet/android/ui/SangeetTopBar.kt`:
```kotlin
package com.varpas.sangeet.android.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.style.TextOverflow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SangeetTopBar(
    title: String,
    onNewComposition: () -> Unit,
    onOpenFile: () -> Unit,
    onSaveFile: () -> Unit,
    onExportPdf: () -> Unit,
    onExportHtml: () -> Unit,
    onEditProperties: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        actions = {
            IconButton(onClick = onNewComposition) {
                Icon(Icons.Default.Add, contentDescription = "New composition")
            }
            IconButton(onClick = onOpenFile) {
                Icon(Icons.Default.FolderOpen, contentDescription = "Open file")
            }
            IconButton(onClick = onSaveFile) {
                Icon(Icons.Default.Save, contentDescription = "Save")
            }

            // Overflow menu
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More")
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Export PDF") },
                    onClick = { menuExpanded = false; onExportPdf() },
                    leadingIcon = { Icon(Icons.Default.PictureAsPdf, null) }
                )
                DropdownMenuItem(
                    text = { Text("Export HTML") },
                    onClick = { menuExpanded = false; onExportHtml() },
                    leadingIcon = { Icon(Icons.Default.Code, null) }
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Properties") },
                    onClick = { menuExpanded = false; onEditProperties() },
                    leadingIcon = { Icon(Icons.Default.Settings, null) }
                )
            }
        }
    )
}
```

- [ ] **Step 2: Create section tab strip**

Create `sangeet-android/src/main/java/com/varpas/sangeet/android/ui/SectionTabs.kt`:
```kotlin
package com.varpas.sangeet.android.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.varpas.sangeet.android.bridge.Section

@Composable
fun SectionTabs(
    sections: List<Section>,
    selectedIndex: Int,
    onSectionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (sections.size <= 1) return

    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier.fillMaxWidth(),
        edgePadding = TabRowDefaults.ScrollableTabRowEdgeStartPadding
    ) {
        sections.forEachIndexed { index, section ->
            Tab(
                selected = index == selectedIndex,
                onClick = { onSectionSelected(index) },
                text = { Text(section.name()) }
            )
        }
    }
}
```

- [ ] **Step 3: Create playback controls**

Create `sangeet-android/src/main/java/com/varpas/sangeet/android/ui/PlaybackControls.kt`:
```kotlin
package com.varpas.sangeet.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PlaybackControls(
    bpm: Double,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    onBpmChange: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Play/Stop button
        IconButton(onClick = { if (isPlaying) onStop() else onPlay() }) {
            Icon(
                if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Stop" else "Play"
            )
        }

        // BPM slider
        Text("BPM:", fontSize = 12.sp)
        Slider(
            value = bpm.toFloat(),
            onValueChange = { onBpmChange(it.toDouble()) },
            valueRange = 10f..300f,
            modifier = Modifier.weight(1f)
        )
        Text("${bpm.toInt()}", fontSize = 14.sp)
    }
}
```

- [ ] **Step 4: Create main EditorScreen composable**

Create `sangeet-android/src/main/java/com/varpas/sangeet/android/ui/EditorScreen.kt`:
```kotlin
package com.varpas.sangeet.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.varpas.sangeet.android.bridge.*
import com.varpas.sangeet.android.ui.canvas.NotationCanvas
import com.varpas.sangeet.android.ui.keyboard.KeyboardAction
import com.varpas.sangeet.android.ui.keyboard.SwarKeyboard
import com.varpas.sangeet.android.viewmodel.EditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel = viewModel(),
    onOpenFile: () -> Unit,
    onSaveFile: () -> Unit,
    onExportPdf: () -> Unit,
    onExportHtml: () -> Unit,
    onNewComposition: () -> Unit,
    onEditProperties: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            SangeetTopBar(
                title = state.composition?.metadata()?.title() ?: "Sangeet Notes Editor",
                onNewComposition = onNewComposition,
                onOpenFile = onOpenFile,
                onSaveFile = onSaveFile,
                onExportPdf = onExportPdf,
                onExportHtml = onExportHtml,
                onEditProperties = onEditProperties
            )
        },
        snackbarHost = {
            // Status messages as snackbars
            val snackbarHostState = remember { SnackbarHostState() }
            LaunchedEffect(state.statusMessage) {
                if (state.statusMessage.isNotBlank()) {
                    snackbarHostState.showSnackbar(
                        state.statusMessage,
                        duration = SnackbarDuration.Short
                    )
                }
            }
            SnackbarHost(snackbarHostState)
        },
        bottomBar = {
            Column {
                // Playback controls
                PlaybackControls(
                    bpm = state.bpm,
                    isPlaying = state.isPlaying,
                    onPlay = { /* Task 10 */ },
                    onStop = { /* Task 10 */ },
                    onBpmChange = { viewModel.setBpm(it) }
                )

                // Swar keyboard
                val currentOctave = state.cursor?.currentOctave()
                    ?: sangeet.model.Octave.Madhya()
                SwarKeyboard(
                    currentOctave = currentOctave,
                    script = state.script,
                    onAction = { action ->
                        handleKeyboardAction(action, viewModel)
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            // Section tabs
            if (state.hasComposition) {
                val sections = state.composition!!.sectionsList()
                SectionTabs(
                    sections = sections,
                    selectedIndex = state.currentSectionIndex,
                    onSectionSelected = { viewModel.switchSection(it) }
                )
            }

            // Notation canvas
            NotationCanvas(
                state = state,
                onBeatTapped = { cycle, beat -> viewModel.moveTo(cycle, beat) },
                onSectionTapped = { viewModel.switchSection(it) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun handleKeyboardAction(action: KeyboardAction, vm: EditorViewModel) {
    when (action) {
        is KeyboardAction.InsertSwar -> vm.insertSwar(action.note, action.shiftDown)
        is KeyboardAction.InsertRest -> vm.insertRest()
        is KeyboardAction.InsertSustain -> vm.insertSustain()
        is KeyboardAction.DeleteLast -> vm.deleteLastEvent()
        is KeyboardAction.SetOctave -> vm.setOctave(action.octave)
        is KeyboardAction.Undo -> vm.undo()
        is KeyboardAction.Redo -> vm.redo()
        is KeyboardAction.NextBeat -> vm.nextBeat()
        is KeyboardAction.PrevBeat -> vm.prevBeat()
        is KeyboardAction.SetSubdivisions -> vm.setSubdivisions(action.n)
    }
}
```

- [ ] **Step 5: Wire EditorScreen into MainActivity**

Update `sangeet-android/src/main/java/com/varpas/sangeet/android/MainActivity.kt`:
```kotlin
package com.varpas.sangeet.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.varpas.sangeet.android.ui.EditorScreen
import com.varpas.sangeet.android.ui.theme.SangeetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SangeetTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EditorScreen(
                        onOpenFile = { /* Task 8 */ },
                        onSaveFile = { /* Task 8 */ },
                        onExportPdf = { /* Task 8 */ },
                        onExportHtml = { /* Task 8 */ },
                        onNewComposition = { /* Task 9 */ },
                        onEditProperties = { /* Task 9 */ }
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 6: Verify build compiles**

```bash
cd sangeet-android && ./gradlew assembleDebug
```

- [ ] **Step 7: Commit**

```bash
git add sangeet-android/src/main/java/com/varpas/sangeet/android/ui/
git add sangeet-android/src/main/java/com/varpas/sangeet/android/MainActivity.kt
git commit -m "feat(android): add main editor screen with toolbar, sections, and playback

- SangeetTopBar with file actions and overflow menu
- SectionTabs for horizontal section switching
- PlaybackControls with BPM slider
- EditorScreen composable wiring canvas, keyboard, and chrome
- MainActivity updated to use EditorScreen"
```

---

### Task 8: File Operations (Storage Access Framework)

**Files:**
- Create: `sangeet-android/src/main/java/com/varpas/sangeet/android/storage/FileManager.kt`
- Create: `sangeet-android/src/main/java/com/varpas/sangeet/android/storage/AutoSaveManager.kt`
- Modify: `sangeet-android/src/main/java/com/varpas/sangeet/android/MainActivity.kt`

- [ ] **Step 1: Create FileManager for SAF operations**

Create `sangeet-android/src/main/java/com/varpas/sangeet/android/storage/FileManager.kt`:
```kotlin
package com.varpas.sangeet.android.storage

import android.content.Context
import android.net.Uri
import com.varpas.sangeet.android.bridge.Composition
import com.varpas.sangeet.android.bridge.CoreBridge

/**
 * Handles .swar file read/write via Android Storage Access Framework.
 * Also manages app-private auto-save storage.
 */
object FileManager {

    fun readSwarFile(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader().readText()
            }
        } catch (e: Exception) {
            null
        }
    }

    fun writeSwarFile(context: Context, uri: Uri, composition: Composition): Boolean {
        return try {
            val json = CoreBridge.serializeComposition(composition)
            context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
                stream.write(json.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun writeToPrivateStorage(context: Context, filename: String, composition: Composition): Boolean {
        return try {
            val json = CoreBridge.serializeComposition(composition)
            context.openFileOutput(filename, Context.MODE_PRIVATE).use { stream ->
                stream.write(json.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun readFromPrivateStorage(context: Context, filename: String): String? {
        return try {
            context.openFileInput(filename).use { stream ->
                stream.bufferedReader().readText()
            }
        } catch (e: Exception) {
            null
        }
    }

    fun autoSaveFilename(title: String): String {
        val sanitized = title.replace(Regex("[^a-zA-Z0-9]"), "_").take(50)
        return "autosave_${sanitized}.swar"
    }
}
```

- [ ] **Step 2: Create AutoSaveManager**

Create `sangeet-android/src/main/java/com/varpas/sangeet/android/storage/AutoSaveManager.kt`:
```kotlin
package com.varpas.sangeet.android.storage

import android.content.Context
import com.varpas.sangeet.android.bridge.Composition
import kotlinx.coroutines.*

/**
 * Debounced auto-save to app-private storage.
 * Saves 500ms after the last edit, on a background coroutine.
 */
class AutoSaveManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val debounceMs: Long = 500L
) {
    private var saveJob: Job? = null

    fun scheduleSave(composition: Composition) {
        saveJob?.cancel()
        saveJob = scope.launch(Dispatchers.IO) {
            delay(debounceMs)
            val filename = FileManager.autoSaveFilename(composition.metadata().title())
            FileManager.writeToPrivateStorage(context, filename, composition)
        }
    }

    fun cancel() {
        saveJob?.cancel()
    }
}
```

- [ ] **Step 3: Wire SAF into MainActivity with ActivityResultContracts**

Update `MainActivity.kt` to add file open/save launchers:
```kotlin
// Add to MainActivity:
private val openFileLauncher = registerForActivityResult(
    ActivityResultContracts.OpenDocument()
) { uri ->
    uri ?: return@registerForActivityResult
    val json = FileManager.readSwarFile(this, uri) ?: return@registerForActivityResult
    editorViewModel.loadComposition(json, uri.toString())
}

private val saveFileLauncher = registerForActivityResult(
    ActivityResultContracts.CreateDocument("application/json")
) { uri ->
    uri ?: return@registerForActivityResult
    val comp = editorViewModel.state.value.composition ?: return@registerForActivityResult
    FileManager.writeSwarFile(this, uri, comp)
}

// In setContent, wire the callbacks:
// onOpenFile = { openFileLauncher.launch(arrayOf("application/json", "*/*")) }
// onSaveFile = { saveFileLauncher.launch("composition.swar") }
```

- [ ] **Step 4: Handle Android lifecycle (save on pause)**

Add to `MainActivity`:
```kotlin
override fun onPause() {
    super.onPause()
    val comp = editorViewModel.state.value.composition ?: return
    val filename = FileManager.autoSaveFilename(comp.metadata().title())
    FileManager.writeToPrivateStorage(this, filename, comp)
}
```

- [ ] **Step 5: Commit**

```bash
git add sangeet-android/src/main/java/com/varpas/sangeet/android/storage/
git add sangeet-android/src/main/java/com/varpas/sangeet/android/MainActivity.kt
git commit -m "feat(android): add file operations with SAF and auto-save

- FileManager for reading/writing .swar via Storage Access Framework
- AutoSaveManager with 500ms debounce on background coroutine
- App-private auto-save on pause for lifecycle safety
- Open/Save wired via ActivityResultContracts"
```

---

### Task 9: Dialogs (New Composition, Properties, Selectors)

**Files:**
- Create: `sangeet-android/src/main/java/com/varpas/sangeet/android/ui/dialogs/NewCompositionDialog.kt`
- Create: `sangeet-android/src/main/java/com/varpas/sangeet/android/ui/dialogs/PropertiesDialog.kt`
- Create: `sangeet-android/src/main/java/com/varpas/sangeet/android/ui/dialogs/TaalRaagSelector.kt`

- [ ] **Step 1: Create taal/raag searchable selector**

Create `sangeet-android/src/main/java/com/varpas/sangeet/android/ui/dialogs/TaalRaagSelector.kt`:
```kotlin
package com.varpas.sangeet.android.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun <T> SearchableSelector(
    label: String,
    items: List<T>,
    selectedItem: T?,
    itemLabel: (T) -> String,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = selectedItem?.let(itemLabel) ?: searchText,
            onValueChange = { searchText = it; expanded = true },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (expanded) {
            val filtered = items.filter {
                itemLabel(it).contains(searchText, ignoreCase = true)
            }
            Card(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)) {
                LazyColumn {
                    items(filtered) { item ->
                        Text(
                            text = itemLabel(item),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onItemSelected(item)
                                    expanded = false
                                    searchText = ""
                                }
                                .padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Create NewCompositionDialog**

Create `sangeet-android/src/main/java/com/varpas/sangeet/android/ui/dialogs/NewCompositionDialog.kt`:
```kotlin
package com.varpas.sangeet.android.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.varpas.sangeet.android.bridge.*

@Composable
fun NewCompositionDialog(
    taals: List<Taal>,
    raags: List<Raag>,
    onDismiss: () -> Unit,
    onCreate: (
        title: String,
        compositionType: CompositionType,
        taal: Taal,
        raag: Raag,
        laya: Laya?,
        taanCount: Int,
        showStrokeLine: Boolean,
        showSahityaLine: Boolean
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("Gat") }
    var selectedTaal by remember { mutableStateOf<Taal?>(null) }
    var selectedRaag by remember { mutableStateOf<Raag?>(null) }
    var selectedLaya by remember { mutableStateOf("Vilambit") }
    var taanCount by remember { mutableIntStateOf(5) }
    var showStrokes by remember { mutableStateOf(true) }
    var showSahitya by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isPalta = selectedType == "Palta"
    val isGat = selectedType == "Gat"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Composition") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    placeholder = { Text("e.g. Yaman Vilambit Gat") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Composition type
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Gat", "Bandish", "Palta").forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type) }
                        )
                    }
                }

                // Raag selector
                SearchableSelector(
                    label = "Raag",
                    items = raags,
                    selectedItem = selectedRaag,
                    itemLabel = { it.name() },
                    onItemSelected = { selectedRaag = it },
                    modifier = Modifier.fillMaxWidth()
                )

                // Taal selector
                SearchableSelector(
                    label = "Taal",
                    items = taals,
                    selectedItem = selectedTaal,
                    itemLabel = { "${it.name()} (${it.matras()})" },
                    onItemSelected = { selectedTaal = it },
                    modifier = Modifier.fillMaxWidth()
                )

                // Laya (hidden for Palta)
                if (!isPalta) {
                    val layaOptions = listOf(
                        "Ati-Vilambit", "Vilambit", "Madhya", "Drut", "Ati-Drut"
                    )
                    var layaExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = layaExpanded,
                        onExpandedChange = { layaExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedLaya,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Laya") },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = layaExpanded,
                            onDismissRequest = { layaExpanded = false }
                        ) {
                            layaOptions.forEach { laya ->
                                DropdownMenuItem(
                                    text = { Text(laya) },
                                    onClick = { selectedLaya = laya; layaExpanded = false }
                                )
                            }
                        }
                    }
                }

                // Taan count (Gat only)
                if (isGat) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Taans: $taanCount")
                        Slider(
                            value = taanCount.toFloat(),
                            onValueChange = { taanCount = it.toInt() },
                            valueRange = 0f..20f,
                            steps = 19,
                            modifier = Modifier.width(200.dp)
                        )
                    }
                }

                // Checkboxes
                Row { Checkbox(showStrokes, { showStrokes = it }); Text("Show Da/Ra strokes") }
                if (!isPalta) {
                    Row { Checkbox(showSahitya, { showSahitya = it }); Text("Show sahitya (lyrics)") }
                }

                // Error
                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                // Validate
                if (title.isBlank()) { errorMessage = "Title is required"; return@TextButton }
                if (selectedRaag == null) { errorMessage = "Raag is required"; return@TextButton }
                if (selectedTaal == null) { errorMessage = "Taal is required"; return@TextButton }

                val compType = when (selectedType) {
                    "Gat" -> sangeet.model.CompositionType.Gat()
                    "Bandish" -> sangeet.model.CompositionType.Bandish()
                    "Palta" -> sangeet.model.CompositionType.Palta()
                    else -> sangeet.model.CompositionType.Gat()
                }

                val laya = if (isPalta) null else when (selectedLaya) {
                    "Ati-Vilambit" -> sangeet.model.Laya.AtiVilambit()
                    "Vilambit" -> sangeet.model.Laya.Vilambit()
                    "Madhya" -> sangeet.model.Laya.Madhya()
                    "Drut" -> sangeet.model.Laya.Drut()
                    "Ati-Drut" -> sangeet.model.Laya.AtiDrut()
                    else -> sangeet.model.Laya.Vilambit()
                }

                onCreate(title, compType, selectedTaal!!, selectedRaag!!, laya,
                    if (isGat) taanCount else 0, showStrokes, showSahitya)
            }) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
```

- [ ] **Step 3: Create PropertiesDialog**

Create `sangeet-android/src/main/java/com/varpas/sangeet/android/ui/dialogs/PropertiesDialog.kt`:
```kotlin
package com.varpas.sangeet.android.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.varpas.sangeet.android.bridge.Composition
import com.varpas.sangeet.android.bridge.Taal

@Composable
fun PropertiesDialog(
    composition: Composition,
    taals: List<Taal>,
    onDismiss: () -> Unit,
    onSave: (title: String, taal: Taal) -> Unit
) {
    val metadata = composition.metadata()
    var title by remember { mutableStateOf(metadata.title()) }
    var selectedTaal by remember { mutableStateOf<Taal?>(metadata.taal()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Composition Properties") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Read-only fields
                Text("Type: ${metadata.compositionType()}", style = MaterialTheme.typography.bodyMedium)
                Text("Raag: ${metadata.raag().name()}", style = MaterialTheme.typography.bodyMedium)

                SearchableSelector(
                    label = "Taal",
                    items = taals,
                    selectedItem = selectedTaal,
                    itemLabel = { "${it.name()} (${it.matras()})" },
                    onItemSelected = { selectedTaal = it }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank() && selectedTaal != null) {
                    onSave(title, selectedTaal!!)
                }
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
```

- [ ] **Step 4: Commit**

```bash
git add sangeet-android/src/main/java/com/varpas/sangeet/android/ui/dialogs/
git commit -m "feat(android): add New Composition and Properties dialogs

- NewCompositionDialog with searchable raag/taal selectors
- Conditional fields by composition type (Gat/Bandish/Palta)
- PropertiesDialog for editing title and taal
- SearchableSelector reusable composable for filtered lists"
```

---

### Task 10: Audio Playback

**Files:**
- Create: `sangeet-android/src/main/java/com/varpas/sangeet/android/audio/AndroidPlaybackEngine.kt`
- Modify: `sangeet-android/src/main/java/com/varpas/sangeet/android/viewmodel/EditorViewModel.kt`

- [ ] **Step 1: Create Android MIDI playback engine**

Create `sangeet-android/src/main/java/com/varpas/sangeet/android/audio/AndroidPlaybackEngine.kt`:
```kotlin
package com.varpas.sangeet.android.audio

import android.media.midi.*
import com.varpas.sangeet.android.bridge.*
import kotlinx.coroutines.*

/**
 * Plays back timed notes using Android's built-in MIDI synthesizer.
 * Falls back to a simple tone generator if no MIDI device is available.
 *
 * Uses sangeet-core PlaybackApi to convert events into TimedNote schedule,
 * then plays them with precise timing.
 */
class AndroidPlaybackEngine(
    private val scope: CoroutineScope
) {
    private var playbackJob: Job? = null
    var isPlaying: Boolean = false
        private set

    /**
     * Converts events to timed notes via sangeet-core and plays them.
     */
    fun play(
        events: scala.collection.immutable.List<Event>,
        bpm: Double,
        matras: Int,
        onPlaybackStarted: () -> Unit = {},
        onPlaybackFinished: () -> Unit = {}
    ) {
        stop()

        val timedNotes = CoreBridge.schedulePlayback(events, bpm, matras)
        if (timedNotes.isEmpty()) return

        isPlaying = true
        onPlaybackStarted()

        playbackJob = scope.launch(Dispatchers.Default) {
            for (note in timedNotes) {
                val timeMs = note.timeMs()
                val durationMs = note.durationMs()

                // Wait until this note's time
                delay(timeMs)

                // Convert swar to MIDI note number
                val midiNote = swarToMidi(note.note(), note.variant(), note.octave())

                // Play via Android AudioTrack tone generation
                // (Full MIDI integration requires MidiManager setup)
                playTone(midiNote, durationMs)
            }
            isPlaying = false
            withContext(Dispatchers.Main) { onPlaybackFinished() }
        }
    }

    fun stop() {
        playbackJob?.cancel()
        isPlaying = false
    }

    /**
     * Map swar + variant + octave to MIDI note number.
     * Middle Sa (Madhya) = MIDI 60 (Middle C).
     */
    private fun swarToMidi(note: Note, variant: Variant, octave: Octave): Int {
        val baseNote = when (note.toString()) {
            "Sa" -> 0
            "Re" -> if (variant.toString() == "Komal") 1 else 2
            "Ga" -> if (variant.toString() == "Komal") 3 else 4
            "Ma" -> if (variant.toString() == "Tivra") 6 else 5
            "Pa" -> 7
            "Dha" -> if (variant.toString() == "Komal") 8 else 9
            "Ni" -> if (variant.toString() == "Komal") 10 else 11
            else -> 0
        }

        val octaveOffset = when (octave.toString()) {
            "AtiMandra" -> -24
            "Mandra" -> -12
            "Madhya" -> 0
            "Taar" -> 12
            "AtiTaar" -> 24
            else -> 0
        }

        return 60 + baseNote + octaveOffset
    }

    private suspend fun playTone(midiNote: Int, durationMs: Long) {
        // Minimal tone generation using AudioTrack
        // A full implementation would use MidiManager or a SoundFont player
        val frequency = 440.0 * Math.pow(2.0, (midiNote - 69).toDouble() / 12.0)
        val sampleRate = 44100
        val numSamples = (sampleRate * durationMs / 1000).toInt()

        val samples = ShortArray(numSamples) { i ->
            val angle = 2.0 * Math.PI * i / (sampleRate / frequency)
            (Math.sin(angle) * Short.MAX_VALUE * 0.5).toInt().toShort()
        }

        val audioTrack = android.media.AudioTrack.Builder()
            .setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                android.media.AudioFormat.Builder()
                    .setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(android.media.AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(numSamples * 2)
            .setTransferMode(android.media.AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(samples, 0, numSamples)
        audioTrack.play()
        delay(durationMs)
        audioTrack.stop()
        audioTrack.release()
    }
}
```

- [ ] **Step 2: Wire playback into EditorViewModel**

Add to `EditorViewModel`:
```kotlin
private var playbackEngine: AndroidPlaybackEngine? = null

fun initPlayback(scope: CoroutineScope) {
    playbackEngine = AndroidPlaybackEngine(scope)
}

fun play() {
    val s = _state.value
    val comp = s.composition ?: return
    val allEvents = comp.sectionsList()
        .flatMap { it.eventsList() }
    // Convert back to Scala list for the API
    val scalaEvents = allEvents.toScalaList()
    val matras = comp.metadata().taal().matras()

    playbackEngine?.play(
        events = scalaEvents as scala.collection.immutable.List<Event>,
        bpm = s.bpm,
        matras = matras,
        onPlaybackStarted = { _state.value = _state.value.copy(isPlaying = true) },
        onPlaybackFinished = { _state.value = _state.value.copy(isPlaying = false) }
    )
}

fun stopPlayback() {
    playbackEngine?.stop()
    _state.value = _state.value.copy(isPlaying = false)
}
```

- [ ] **Step 3: Commit**

```bash
git add sangeet-android/src/main/java/com/varpas/sangeet/android/audio/
git add sangeet-android/src/main/java/com/varpas/sangeet/android/viewmodel/EditorViewModel.kt
git commit -m "feat(android): add audio playback with tone generation

- AndroidPlaybackEngine using AudioTrack tone synthesis
- Swar-to-MIDI note mapping with variant and octave support
- sangeet-core PlaybackApi for timing schedule
- Play/Stop wired into EditorViewModel"
```

---

### Task 11: Ornament Input (Touch-Friendly)

**Files:**
- Create: `sangeet-android/src/main/java/com/varpas/sangeet/android/ui/keyboard/OrnamentMenu.kt`
- Modify: `sangeet-android/src/main/java/com/varpas/sangeet/android/viewmodel/EditorViewModel.kt`

- [ ] **Step 1: Create ornament menu composable**

Create `sangeet-android/src/main/java/com/varpas/sangeet/android/ui/keyboard/OrnamentMenu.kt`:
```kotlin
package com.varpas.sangeet.android.ui.keyboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Ornament selection menu for touch input.
 * Replaces Ctrl+key combos from desktop.
 *
 * Flow:
 * 1. User taps Ornament button on keyboard -> shows this menu
 * 2. User selects ornament type
 * 3. For simple ornaments (Gamak, Andolan, Gitkari): applied immediately
 * 4. For note-based ornaments: keyboard switches to "note collection" mode
 *    - Single-note: tap one swar, ornament applied
 *    - Two-note: tap two swars, ornament applied
 *    - Multi-note: tap swars, press Confirm
 */

enum class OrnamentCategory(val label: String) {
    Simple("Simple"),
    SingleNote("One Note"),
    TwoNote("Two Notes"),
    MultiNote("Multiple Notes")
}

data class OrnamentOption(
    val id: String,
    val label: String,
    val description: String,
    val category: OrnamentCategory
)

val ornamentOptions = listOf(
    OrnamentOption("gamak", "Gamak", "Heavy oscillation", OrnamentCategory.Simple),
    OrnamentOption("andolan", "Andolan", "Gentle oscillation", OrnamentCategory.Simple),
    OrnamentOption("gitkari", "Gitkari", "Hammer/pull trill", OrnamentCategory.Simple),
    OrnamentOption("kanSwar", "Kan Swar", "Grace note", OrnamentCategory.SingleNote),
    OrnamentOption("sparsh", "Sparsh", "Light touch", OrnamentCategory.SingleNote),
    OrnamentOption("ghaseet", "Ghaseet", "Heavy pull", OrnamentCategory.SingleNote),
    OrnamentOption("meendAsc", "Meend (up)", "Ascending glide", OrnamentCategory.TwoNote),
    OrnamentOption("meendDesc", "Meend (down)", "Descending glide", OrnamentCategory.TwoNote),
    OrnamentOption("krintan", "Krintan", "Pull-off sequence", OrnamentCategory.TwoNote),
    OrnamentOption("murki", "Murki", "Ornamental turn", OrnamentCategory.MultiNote),
    OrnamentOption("zamzama", "Zamzama", "Rapid cluster", OrnamentCategory.MultiNote),
)

@Composable
fun OrnamentMenu(
    onOrnamentSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Ornament") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OrnamentCategory.entries.forEach { category ->
                    Text(
                        category.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    val options = ornamentOptions.filter { it.category == category }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.heightIn(max = 100.dp)
                    ) {
                        items(options) { option ->
                            ElevatedButton(
                                onClick = { onOrnamentSelected(option.id) },
                                contentPadding = PaddingValues(4.dp),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Text(option.label, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Bottom bar shown during ornament note collection.
 * Instructs the user and shows a Confirm/Cancel button.
 */
@Composable
fun OrnamentCollectionBar(
    ornamentType: String,
    collectedNotes: Int,
    requiredNotes: Int?, // null for multi-note (variable)
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val instruction = when {
                requiredNotes != null && collectedNotes < requiredNotes ->
                    "Tap note ${collectedNotes + 1} of $requiredNotes for $ornamentType"
                requiredNotes == null ->
                    "$ornamentType: $collectedNotes notes collected. Tap more or confirm."
                else ->
                    "$ornamentType: ready"
            }
            Text(instruction, modifier = Modifier.weight(1f))

            if (requiredNotes == null || collectedNotes >= (requiredNotes ?: 0)) {
                TextButton(onClick = onConfirm) { Text("Done") }
            }
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
    }
}
```

- [ ] **Step 2: Add ornament handling to EditorViewModel**

Add to `EditorViewModel.kt`:
```kotlin
fun startOrnament(ornamentId: String) {
    val newMode = when (ornamentId) {
        "gamak" -> { applySimpleOrnament("gamak"); return }
        "andolan" -> { applySimpleOrnament("andolan"); return }
        "gitkari" -> { applySimpleOrnament("gitkari"); return }
        "kanSwar" -> OrnamentMode.KanSwarPending
        "sparsh" -> OrnamentMode.SparshPending
        "ghaseet" -> OrnamentMode.GhaseetPending
        "meendAsc" -> OrnamentMode.MeendStartAsc
        "meendDesc" -> OrnamentMode.MeendStartDesc
        "krintan" -> OrnamentMode.KrintanStart
        "murki" -> OrnamentMode.MurkiCollect()
        "zamzama" -> OrnamentMode.ZamzamaCollect()
        else -> return
    }
    _state.value = _state.value.copy(
        ornamentMode = newMode,
        statusMessage = "Tap notes for ornament"
    )
}

fun cancelOrnament() {
    _state.value = _state.value.copy(
        ornamentMode = null,
        statusMessage = "Ornament cancelled"
    )
}

private fun applySimpleOrnament(type: String) {
    val s = _state.value
    val comp = s.composition ?: return
    val result = sangeet.core.OrnamentApi.addSimpleOrnament(comp, s.currentSectionIndex, type)
    if (result.isRight()) {
        val tuple = result.toOption().get() as scala.Tuple2<Composition, String>
        val grids = CoreBridge.computeLayout(tuple._1())
        _state.value = s.copy(
            composition = tuple._1(),
            grids = grids,
            statusMessage = tuple._2()
        )
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add sangeet-android/src/main/java/com/varpas/sangeet/android/ui/keyboard/OrnamentMenu.kt
git add sangeet-android/src/main/java/com/varpas/sangeet/android/viewmodel/EditorViewModel.kt
git commit -m "feat(android): add touch-friendly ornament input UI

- OrnamentMenu dialog with categorized ornament buttons
- OrnamentCollectionBar for step-by-step note collection
- Simple ornaments applied immediately
- Note-based ornaments enter collection mode
- Integrated into EditorViewModel ornament state machine"
```

---

### Task 12: Stroke Edit Mode

**Files:**
- Modify: `sangeet-android/src/main/java/com/varpas/sangeet/android/ui/keyboard/SwarKeyboard.kt`
- Modify: `sangeet-android/src/main/java/com/varpas/sangeet/android/viewmodel/EditorViewModel.kt`

- [ ] **Step 1: Add stroke buttons to SwarKeyboard**

Add a stroke button row to `SwarKeyboard.kt` that appears when `editMode == StrokeEdit`:
```kotlin
// Add between Row 1 and Row 2:
if (editMode == EditMode.StrokeEdit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        StrokeButton("Da", sangeet.model.Stroke.Da(), onStroke, Modifier.weight(1f))
        StrokeButton("Ra", sangeet.model.Stroke.Ra(), onStroke, Modifier.weight(1f))
        StrokeButton("Chikari", sangeet.model.Stroke.Chikari(), onStroke, Modifier.weight(1f))
        StrokeButton("Jod", sangeet.model.Stroke.Jod(), onStroke, Modifier.weight(1f))
        Button(
            onClick = onClearStroke,
            modifier = Modifier.weight(1f).height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) { Text("Clear", fontSize = 12.sp) }
    }
}
```

Add a toggle button for stroke edit mode in Row 4, and add `editMode`, `onStroke`, and `onClearStroke` parameters to the `SwarKeyboard` composable signature.

- [ ] **Step 2: Add stroke operations to EditorViewModel**

Add to `EditorViewModel.kt`:
```kotlin
fun setStroke(stroke: Stroke) {
    val s = _state.value
    val comp = s.composition ?: return
    val cursor = s.cursor ?: return
    val result = sangeet.core.StrokeApi.setStroke(comp, s.currentSectionIndex, cursor, stroke)
    if (result.isRight()) {
        val tuple = result.toOption().get() as scala.Tuple2<Composition, String>
        val grids = CoreBridge.computeLayout(tuple._1())
        _state.value = s.copy(
            composition = tuple._1(),
            grids = grids,
            statusMessage = tuple._2()
        )
    }
}

fun clearStroke() {
    val s = _state.value
    val comp = s.composition ?: return
    val cursor = s.cursor ?: return
    val result = sangeet.core.StrokeApi.clearStroke(comp, s.currentSectionIndex, cursor)
    if (result.isRight()) {
        val tuple = result.toOption().get() as scala.Tuple2<Composition, String>
        val grids = CoreBridge.computeLayout(tuple._1())
        _state.value = s.copy(
            composition = tuple._1(),
            grids = grids,
            statusMessage = tuple._2()
        )
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add sangeet-android/src/main/java/com/varpas/sangeet/android/ui/keyboard/SwarKeyboard.kt
git add sangeet-android/src/main/java/com/varpas/sangeet/android/viewmodel/EditorViewModel.kt
git commit -m "feat(android): add stroke edit mode with Da/Ra/Chikari/Jod buttons

- Stroke button row shown in StrokeEdit mode
- Clear stroke button to revert to auto-alternation
- Toggle between SwarEdit and StrokeEdit modes
- StrokeApi calls wired into EditorViewModel"
```

---

### Task 13: Undo/Redo and Navigation Polish

**Files:**
- Modify: `sangeet-android/src/main/java/com/varpas/sangeet/android/ui/canvas/NotationCanvas.kt`
- Modify: `sangeet-android/src/main/java/com/varpas/sangeet/android/ui/EditorScreen.kt`

- [ ] **Step 1: Add swipe gestures for beat navigation**

Add to `NotationCanvas.kt` pointerInput:
```kotlin
// Inside the Canvas modifier chain, add:
.pointerInput(Unit) {
    detectHorizontalDragGestures { _, dragAmount ->
        if (dragAmount > 50f) onSwipeRight()   // prev beat
        if (dragAmount < -50f) onSwipeLeft()   // next beat
    }
}
```

Add `onSwipeLeft` and `onSwipeRight` parameters to `NotationCanvas`.

Wire in `EditorScreen`:
```kotlin
NotationCanvas(
    ...
    onSwipeLeft = { viewModel.nextBeat() },
    onSwipeRight = { viewModel.prevBeat() },
)
```

- [ ] **Step 2: Add section dropdown as alternative to tabs**

For compositions with many sections (e.g., a Gat with 10 taans), tabs overflow. Add a dropdown alternative:
```kotlin
// In EditorScreen, if sections.size > 6, show dropdown instead of tabs:
if (sections.size > 6) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = sections[state.currentSectionIndex].name(),
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.menuAnchor().fillMaxWidth().padding(horizontal = 8.dp)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            sections.forEachIndexed { i, s ->
                DropdownMenuItem(
                    text = { Text(s.name()) },
                    onClick = { viewModel.switchSection(i); expanded = false }
                )
            }
        }
    }
} else {
    SectionTabs(sections, state.currentSectionIndex, { viewModel.switchSection(it) })
}
```

- [ ] **Step 3: Commit**

```bash
git add sangeet-android/src/main/java/com/varpas/sangeet/android/ui/
git commit -m "feat(android): add swipe navigation and section dropdown for many-section compositions

- Horizontal swipe gestures for prev/next beat on canvas
- Dropdown selector replaces tabs when sections > 6
- Improved touch navigation ergonomics"
```

---

### Task 14: Testing

**Files:**
- Create: `sangeet-android/src/test/java/com/varpas/sangeet/android/viewmodel/UndoManagerTest.kt`
- Create: `sangeet-android/src/androidTest/java/com/varpas/sangeet/android/EditorIntegrationTest.kt`
- Create: `sangeet-android/src/androidTest/java/com/varpas/sangeet/android/SwarKeyboardTest.kt`

- [ ] **Step 1: Unit test UndoManager**

Create `sangeet-android/src/test/java/com/varpas/sangeet/android/viewmodel/UndoManagerTest.kt`:
```kotlin
package com.varpas.sangeet.android.viewmodel

import com.varpas.sangeet.android.bridge.*
import org.junit.Assert.*
import org.junit.Test

class UndoManagerTest {

    private fun makeSnapshot(title: String): UndoSnapshot {
        val taal = CoreBridge.taalByName("Teentaal")!!
        val raag = CoreBridge.raagByName("Yaman")!!
        val result = CoreBridge.createComposition(
            title = title,
            compositionType = sangeet.model.CompositionType.Gat(),
            taal = taal,
            raag = raag,
            laya = sangeet.model.Laya.Vilambit()
        )
        return UndoSnapshot(result.composition, result.cursor, 0)
    }

    @Test
    fun `push adds to past and clears future`() {
        val s1 = makeSnapshot("S1")
        val s2 = makeSnapshot("S2")
        val manager = UndoManager(present = s1)
        val updated = manager.push(s2)

        assertEquals(1, updated.past.size)
        assertEquals(s2, updated.present)
        assertTrue(updated.future.isEmpty())
    }

    @Test
    fun `undo moves present to future`() {
        val s1 = makeSnapshot("S1")
        val s2 = makeSnapshot("S2")
        val manager = UndoManager(present = s1).push(s2)

        val undone = manager.undo()!!
        assertEquals(s1, undone.present)
        assertEquals(1, undone.future.size)
    }

    @Test
    fun `redo restores from future`() {
        val s1 = makeSnapshot("S1")
        val s2 = makeSnapshot("S2")
        val manager = UndoManager(present = s1).push(s2).undo()!!

        val redone = manager.redo()!!
        assertEquals(s2, redone.present)
        assertTrue(redone.future.isEmpty())
    }

    @Test
    fun `undo on empty past returns null`() {
        val s1 = makeSnapshot("S1")
        val manager = UndoManager(present = s1)
        assertNull(manager.undo())
    }

    @Test
    fun `max size trims oldest entries`() {
        var manager = UndoManager(present = makeSnapshot("S0"), maxSize = 3)
        for (i in 1..5) {
            manager = manager.push(makeSnapshot("S$i"))
        }
        assertTrue(manager.past.size <= 3)
    }
}
```

- [ ] **Step 2: Create instrumented integration test**

Create `sangeet-android/src/androidTest/java/com/varpas/sangeet/android/EditorIntegrationTest.kt`:
```kotlin
package com.varpas.sangeet.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.varpas.sangeet.android.bridge.*
import com.varpas.sangeet.android.viewmodel.EditorViewModel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditorIntegrationTest {

    private lateinit var vm: EditorViewModel

    @Before
    fun setup() {
        vm = EditorViewModel()
        vm.createComposition(
            title = "Integration Test",
            compositionType = sangeet.model.CompositionType.Gat(),
            taal = CoreBridge.taalByName("Teentaal")!!,
            raag = CoreBridge.raagByName("Yaman")!!,
            laya = sangeet.model.Laya.Vilambit(),
            taanCount = 1,
            showStrokeLine = true
        )
    }

    @Test
    fun fullEditCycle() {
        // Enter 3 notes
        vm.insertSwar(sangeet.model.Note.Sa(), false)
        vm.insertSwar(sangeet.model.Note.Re(), false)
        vm.insertSwar(sangeet.model.Note.Ga(), false)

        val state = vm.state.value
        val events = state.composition!!.sectionsList()[0].eventsList()
        assertEquals(3, events.size)

        // Undo one
        vm.undo()
        val afterUndo = vm.state.value
        val eventsAfterUndo = afterUndo.composition!!.sectionsList()[0].eventsList()
        assertEquals(2, eventsAfterUndo.size)

        // Redo
        vm.redo()
        val afterRedo = vm.state.value
        val eventsAfterRedo = afterRedo.composition!!.sectionsList()[0].eventsList()
        assertEquals(3, eventsAfterRedo.size)

        // Serialize and parse roundtrip
        val json = CoreBridge.serializeComposition(afterRedo.composition!!)
        val parsed = CoreBridge.parseComposition(json)
        assertNotNull(parsed)
        assertEquals("Integration Test", parsed!!.metadata().title())
        assertEquals(3, parsed.sectionsList()[0].eventsList().size)
    }

    @Test
    fun octaveAndVariantInput() {
        // Set mandra octave, enter komal Re
        vm.setOctave(sangeet.model.Octave.Mandra())
        vm.insertSwar(sangeet.model.Note.Re(), true) // Shift = komal

        val events = vm.state.value.composition!!.sectionsList()[0].eventsList()
        val swar = events[0] as sangeet.model.Event.Swar
        assertEquals("Mandra", swar.octave().toString())
        assertEquals("Komal", swar.variant().toString())
    }
}
```

- [ ] **Step 3: Create UI test for swar keyboard**

Create `sangeet-android/src/androidTest/java/com/varpas/sangeet/android/SwarKeyboardTest.kt`:
```kotlin
package com.varpas.sangeet.android

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.varpas.sangeet.android.ui.keyboard.KeyboardAction
import com.varpas.sangeet.android.ui.keyboard.SwarKeyboard
import com.varpas.sangeet.android.viewmodel.EditMode
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class SwarKeyboardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tappingSwarButtonEmitsInsertAction() {
        var lastAction: KeyboardAction? = null

        composeRule.setContent {
            SwarKeyboard(
                currentOctave = sangeet.model.Octave.Madhya(),
                script = sangeet.model.SwarScript.Devanagari(),
                onAction = { lastAction = it }
            )
        }

        composeRule.onNodeWithText("Sa").performClick()
        assertTrue(lastAction is KeyboardAction.InsertSwar)
    }

    @Test
    fun restButtonEmitsRestAction() {
        var lastAction: KeyboardAction? = null

        composeRule.setContent {
            SwarKeyboard(
                currentOctave = sangeet.model.Octave.Madhya(),
                script = sangeet.model.SwarScript.Devanagari(),
                onAction = { lastAction = it }
            )
        }

        composeRule.onNodeWithText("Rest").performClick()
        assertEquals(KeyboardAction.InsertRest, lastAction)
    }

    @Test
    fun octaveButtonUpdatesSelection() {
        var lastAction: KeyboardAction? = null

        composeRule.setContent {
            SwarKeyboard(
                currentOctave = sangeet.model.Octave.Madhya(),
                script = sangeet.model.SwarScript.Devanagari(),
                onAction = { lastAction = it }
            )
        }

        composeRule.onNodeWithText("Taar").performClick()
        assertTrue(lastAction is KeyboardAction.SetOctave)
    }
}
```

- [ ] **Step 4: Run all tests**

```bash
cd sangeet-android && ./gradlew test           # Unit tests
cd sangeet-android && ./gradlew connectedCheck  # Instrumented tests (requires device/emulator)
```

Expected: All unit tests pass. Instrumented tests pass on a connected device or emulator.

- [ ] **Step 5: Commit**

```bash
git add sangeet-android/src/test/
git add sangeet-android/src/androidTest/
git commit -m "test(android): add unit tests, UI tests, and integration tests

- UndoManagerTest: push, undo, redo, max size
- EditorIntegrationTest: full edit cycle with roundtrip serialization
- SwarKeyboardTest: Compose UI tests for keyboard actions
- CoreBridgeTest: Scala interop verification"
```

---

## Summary of Deliverables

| Task | What it produces |
|------|-----------------|
| 1 | Gradle project, Compose setup, sangeet-core dependency verified |
| 2 | `make android-debug` builds APK end-to-end |
| 3 | Kotlin-Scala bridge: type aliases, extensions, CoreBridge facade |
| 4 | EditorViewModel + StateFlow state management + UndoManager |
| 5 | NotationCanvas rendering Bhatkhande grid with 5 rows |
| 6 | On-screen swar keyboard with octave/variant/special keys |
| 7 | Complete app chrome: toolbar, sections, playback, editor screen |
| 8 | File open/save via SAF, auto-save on pause |
| 9 | New composition and properties dialogs with searchable selectors |
| 10 | Audio playback via tone generation with sangeet-core scheduling |
| 11 | Touch-friendly ornament input with categorized menu |
| 12 | Stroke edit mode with Da/Ra/Chikari/Jod buttons |
| 13 | Swipe navigation and section dropdown for large compositions |
| 14 | Unit, UI, and integration test suite |

## Key Constraints and Notes

- **No voice recognition** in this plan (can be added later via whisper.cpp JNI on Android)
- **No physical keyboard assumed** -- all input is touch via the swar keyboard
- **sangeet-core must be ART-compatible** -- verify no `javax.sound.*`, `java.awt.*`, or `javafx.*` APIs leak into core JAR. These are desktop-only. The core extraction in Plan 1 must exclude these.
- **PDFBox on Android** uses `com.tom-roush:pdfbox-android` (a maintained Android port), not Apache's desktop PDFBox
- **Devanagari rendering** is built into Android -- no need to bundle Noto Sans Devanagari font. Android supports Devanagari, Kannada, Telugu natively.
- **Android lifecycle** -- composition is auto-saved to private storage on `onPause()`, restored on `onCreate()`
- **Scala 3 on Android** -- the Scala 3 standard library (`scala3-library_3`) must be included as a Gradle dependency. It runs on ART via DEX compilation. The library is ~5MB.
- **ProGuard/R8** -- when minification is enabled, Scala reflection-heavy code may need keep rules. Test the release build early.
