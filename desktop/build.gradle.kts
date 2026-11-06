import org.jetbrains.compose.desktop.application.dsl.TargetFormat

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17

        freeCompilerArgs.add("-Xskip-metadata-version-check")
    }
}

val zedVersion = project.property("spider.versionName") as String

val generateVersion = tasks.register("generateVersion") {
    val out = layout.buildDirectory.dir("generated/version")
    val version = zedVersion
    inputs.property("version", version)
    outputs.dir(out)
    doLast {
        val dir = out.get().asFile.resolve("dev/spider/spider/desktop")
        dir.mkdirs()
        dir.resolve("BuildVersion.kt").writeText(
            "package dev.spider.spider.desktop\n\ninternal const val BUILD_VERSION = \"$version\"\n",
        )
    }
}

val desktopPlatform = when {
    System.getProperty("os.name").lowercase().contains("win") -> "windows"
    System.getProperty("os.name").lowercase().contains("mac") -> "macos"
    else -> "linux"
}
val soarExecutable = if (desktopPlatform == "windows") "soar-socks.exe" else "soar-socks"
val soarBinary = layout.buildDirectory.file("generated/soar/$desktopPlatform/$soarExecutable")
val soarBinaryFile = soarBinary.get().asFile.apply { parentFile.mkdirs() }
val buildSoarProxy = tasks.register<Exec>("buildSoarProxy") {
    workingDir(project.file("src/main/go/soar-main"))
    commandLine("go", "build", "-trimpath", "-o", soarBinaryFile.absolutePath, "./cmd/soar-socks")
    outputs.file(soarBinary)
}

val firefoxCoreBinary = layout.buildDirectory.file("generated/firefox/$desktopPlatform/${if (desktopPlatform == "windows") "arman-vpn-core.exe" else "arman-vpn-core"}")
val firefoxCoreBinaryFile = firefoxCoreBinary.get().asFile.apply { parentFile.mkdirs() }
val buildFirefoxCore = tasks.register<Exec>("buildFirefoxCore") {
    workingDir(rootProject.file("third_party/arman-vpn/core"))
    commandLine("go", "build", "-trimpath", "-o", firefoxCoreBinaryFile.absolutePath, "./cmd/arman-vpn-core")
    outputs.file(firefoxCoreBinary)
    inputs.dir(rootProject.file("third_party/arman-vpn/core"))
}

tasks.named<org.gradle.language.jvm.tasks.ProcessResources>("processResources") {
    dependsOn(buildSoarProxy, buildFirefoxCore)
    from(soarBinary) {
        into("bin/$desktopPlatform")
    }
    from(firefoxCoreBinary) {
        into("bin/$desktopPlatform")
    }
}

kotlin.sourceSets.main {
    kotlin.srcDir(generateVersion)
}

configurations.configureEach {
    resolutionStrategy {
        force(
            "org.jetbrains.kotlin:kotlin-stdlib:${libs.versions.kotlin.get()}",
            "org.jetbrains.kotlin:kotlin-stdlib-jdk7:${libs.versions.kotlin.get()}",
            "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${libs.versions.kotlin.get()}",
        )
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = System.getenv("SPIDER_LIVE_TUN") == "1"
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.material3.mp)
    implementation(libs.compose.components.resources)
    implementation(libs.lifecycle.viewmodel.compose.mp)
    implementation(libs.lifecycle.runtime.compose.mp)
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)
    implementation(libs.compose.native.tray)
    implementation(libs.jna)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.compose.ui:ui-test:${libs.versions.composeMultiplatform.get()}")
    testImplementation("org.junit.platform:junit-platform-launcher")
}

compose.desktop {
    application {
        mainClass = "dev.spider.spider.desktop.GuiKt"
        jvmArgs += listOf("-XX:TieredStopAtLevel=1")
        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Rpm, TargetFormat.AppImage, TargetFormat.Msi, TargetFormat.Dmg)
            modules("java.net.http", "jdk.localedata")
            packageName = "Spider"
            packageVersion = zedVersion
            description = "Spider VPN client"
            vendor = "Spider Studio"
            linux {
                iconFile.set(project.file("icons/spider.png"))
                packageName = "spider"
                menuGroup = "Network"
                shortcut = true
            }
            windows {
                iconFile.set(project.file("icons/spider.ico"))
                menuGroup = "Spider"
                shortcut = true
            }
            macOS {
                iconFile.set(project.file("icons/spider.icns"))
                bundleID = "dev.spider.spider"
                minimumSystemVersion = "12.0"
            }
        }
    }
}
