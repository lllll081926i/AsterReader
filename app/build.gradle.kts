import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.*
import java.nio.ByteBuffer
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk { version = release(37) }

  defaultConfig {
    applicationId = "com.asterreader.app"
    minSdk = 31
    targetSdk = 37
    versionCode = 4
    versionName = "1.0.1"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  // Do not remove or modify these signingConfigs. They are necessary for building, installing,
  // and updating Android apps in AI Studio.
  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/.local/signing/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/.local/signing/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // Do not remove or modify this signingConfig assignment. It is necessary for Android apps in
      // AI Studio.
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      // Do not remove or modify this signingConfig assignment. It is necessary for Android apps in
      // AI Studio.
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  splits {
    abi {
      isEnable = providers.gradleProperty("aster.splitApks").orNull == "true"
      reset()
      include("arm64-v8a", "armeabi-v7a")
      isUniversalApk = true
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  // minSdk >= 28 时 AGP 默认把 dex 以「未压缩」方式塞进 APK（系统可直接 mmap，安装更快），
  // 代价是 APK 文件几乎等于 dex 原始大小。恢复压缩 dex 可让安装包体积大幅下降，
  // 运行时与设备安装占用不变（系统安装时本来就会解压）。
  packaging {
    dex { useLegacyPackaging = true }
    resources { excludes += setOf("META-INF/{AL2.0,LGPL2.1}") }
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }



// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(project(":core:model"))
  implementation(project(":core:database"))
  implementation(project(":core:designsystem"))
  implementation(project(":core:vault"))

  implementation(platform(libs.androidx.compose.bom))
  // implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  // implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  // Uncomment to use Firestore:
  // implementation(libs.firebase.firestore)

  // Uncomment ALL FOUR of the following dependencies together to use Firebase Auth and Google
  // Sign-In via Credential Manager:
  // implementation(libs.firebase.auth)
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
  // implementation(libs.googleid)
  // implementation(libs.firebase.appcheck.recaptcha)
  // implementation(libs.firebase.appcheck.debug)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  // 漫画归档：7z / tar / rar 真实读取
  implementation(libs.commons.compress)
  implementation(libs.xz)
  implementation(libs.junrar)
  runtimeOnly(libs.slf4j.nop)
  // implementation(libs.logging.interceptor)
  // implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  // implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  // "ksp"(libs.androidx.room.compiler)
  // "ksp"(libs.moshi.kotlin.codegen)
}

abstract class AsterNoticesTask : DefaultTask() {
    @get:Input abstract val coordinates: ListProperty<String>
    @get:InputFiles @get:PathSensitive(PathSensitivity.NONE)
    abstract val pomFiles: ConfigurableFileCollection
    @get:InputFiles @get:PathSensitive(PathSensitivity.NONE)
    abstract val fonts: ConfigurableFileCollection
    @get:InputFiles @get:PathSensitive(PathSensitivity.NONE)
    abstract val fontLicenses: ConfigurableFileCollection
    @get:InputDirectory @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val runtimeLicenses: DirectoryProperty
    @get:OutputDirectory abstract val outputDirectory: DirectoryProperty

    @TaskAction fun generate() {
        val licenseDirectory = runtimeLicenses.get().asFile
        @Suppress("UNCHECKED_CAST")
        val licenseIndex = groovy.json.JsonSlurper().parse(licenseDirectory.resolve("index.json")) as Map<String, List<String>>
        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        }
        fun Element.child(tag: String): String = (0 until childNodes.length)
            .mapNotNull { childNodes.item(it) as? Element }.firstOrNull { it.tagName == tag }?.textContent?.trim().orEmpty()
        fun safeUrl(value: String): String = value.replaceFirst("http://", "https://")
            .takeIf { it.startsWith("https://") && !it.contains('$') }.orEmpty()
        val indexedPoms = pomFiles.files.associateBy { file ->
            val version = file.parentFile.parentFile
            "${version.parentFile.parentFile.name}:${version.parentFile.name}:${version.name}"
        }
        fun licenseRoot(coordinate: String, visited: Set<String> = emptySet()): Element? {
            if (coordinate in visited) return null
            val file = indexedPoms[coordinate] ?: return null
            val root = factory.newDocumentBuilder().parse(file).documentElement
            if (root.getElementsByTagName("license").length > 0) return root
            val parent = root.getElementsByTagName("parent").item(0) as? Element ?: return root
            return licenseRoot("${parent.child("groupId")}:${parent.child("artifactId")}:${parent.child("version")}", visited + coordinate) ?: root
        }
        val entries = coordinates.get().map { coordinate ->
            val pom = requireNotNull(indexedPoms[coordinate]) { "缺少依赖许可元数据：$coordinate" }
            val root = factory.newDocumentBuilder().parse(pom).documentElement
            val licenses = (licenseRoot(coordinate) ?: root).getElementsByTagName("license")
            val names = (0 until licenses.length).map { (licenses.item(it) as Element).child("name") }.filter(String::isNotBlank)
            val licenseUrl = (0 until licenses.length).map { safeUrl((licenses.item(it) as Element).child("url")) }.firstOrNull(String::isNotBlank)
            val parts = coordinate.split(':')
            val metadataUrl = (if (parts[0].startsWith("androidx.") || parts[0].startsWith("com.android."))
                "https://dl.google.com/dl/android/maven2/" else "https://repo.maven.apache.org/maven2/") +
                "${parts[0].replace('.', '/')}/${parts[1]}/${parts[2]}/${parts[1]}-${parts[2]}.pom"
            mapOf("id" to coordinate, "name" to "${parts[0]}:${parts[1]}", "version" to parts[2],
                "license" to names.joinToString(" / ").ifBlank { "许可见来源" },
                "url" to (licenseUrl ?: safeUrl(root.child("url")).ifBlank { metadataUrl }),
                "notice" to requireNotNull(licenseIndex[coordinate]) { "缺少离线许可：$coordinate" }
                    .joinToString("\n\n") { name -> licenseDirectory.resolve(name).readText() }
                    .also { require(it.isNotBlank()) { "离线许可为空：$coordinate" } })
        }.toMutableList()
        fonts.files.sortedBy { it.name }.forEach { font ->
            val bytes = font.readBytes()
            val buffer = ByteBuffer.wrap(bytes)
            fun short(offset: Int) = buffer.getShort(offset).toInt() and 0xffff
            val tableCount = short(4)
            val table = (0 until tableCount).map { 12 + it * 16 }.first { buffer.getInt(it) == 0x6e616d65 }
            val offset = buffer.getInt(table + 8)
            val strings = offset + short(offset + 4)
            val names = mutableMapOf<Int, String>()
            repeat(short(offset + 2)) { index ->
                val record = offset + 6 + index * 12
                val platform = short(record)
                val language = short(record + 4)
                if (platform == 0 || platform == 3 && language == 0x409) {
                    val id = short(record + 6)
                    val start = strings + short(record + 10)
                    val value = String(bytes, start, short(record + 8), Charsets.UTF_16BE)
                    names.putIfAbsent(id, value)
                }
            }
            val licenseFile = fontLicenses.files.firstOrNull { it.name == "${font.nameWithoutExtension}.OFL.txt" }
            val fullLicense = requireNotNull(licenseFile) { "缺少字体许可全文：${font.name}" }.readText()
            require(fullLicense.contains(names[0].orEmpty()) && fullLicense.contains("SIL OPEN FONT LICENSE"))
            val notice = fullLicense
            require(notice.isNotBlank()) { "字体缺少许可元数据：${font.name}" }
            entries += mapOf("id" to "font:${font.name}", "name" to names[1].orEmpty().ifBlank { font.name },
                "version" to names[5].orEmpty(), "license" to if (notice.contains("SIL Open Font", true)) "SIL Open Font License" else "字体许可",
                "url" to safeUrl(names[14].orEmpty()), "notice" to notice)
        }
        val destination = outputDirectory.file("open_source_notices.json").get().asFile
        destination.parentFile.mkdirs()
        destination.writeText(groovy.json.JsonOutput.toJson(entries.sortedBy { it.getValue("name").lowercase() }), Charsets.UTF_8)
    }
}

val mavenCache = gradle.gradleUserHomeDir.resolve("caches/modules-2/files-2.1")
androidComponents.onVariants { variant ->
    val runtime = configurations.named("${variant.name}RuntimeClasspath")
    val modules = runtime.map { configuration ->
        configuration.incoming.resolutionResult.allComponents.mapNotNull { it.id as? ModuleComponentIdentifier }
            .map { "${it.group}:${it.module}:${it.version}" }.distinct().sorted()
    }
    val task = tasks.register<AsterNoticesTask>("generate${variant.name.replaceFirstChar(Char::uppercase)}Notices") {
        coordinates.set(modules)
        pomFiles.from(fileTree(mavenCache) { include("**/*.pom") })
        fonts.from(fileTree("src/main/res/font") { include("*.ttf", "*.otf") })
        fontLicenses.from(fileTree("src/main/licenses") { include("*.OFL.txt") })
        runtimeLicenses.set(layout.projectDirectory.dir("src/main/licenses/runtime"))
        outputDirectory.set(layout.buildDirectory.dir("generated/notices/${variant.name}"))
    }
    variant.sources.assets?.addGeneratedSourceDirectory(task, AsterNoticesTask::outputDirectory)
}
