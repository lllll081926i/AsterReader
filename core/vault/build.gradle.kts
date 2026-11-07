plugins { alias(libs.plugins.android.library) }

android {
    namespace = "com.asterreader.core.vault"
    compileSdk { version = release(37) }
    defaultConfig { minSdk = 31 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation("org.bouncycastle:bcprov-jdk18on:1.86")
    testImplementation(libs.junit)
}
