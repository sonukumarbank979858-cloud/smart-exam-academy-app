plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val runNumber = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
val keystorePath: String? = System.getenv("KEYSTORE_PATH")

// Defaults point at the live project; override with -P or env (GitHub secrets) to use another one.
val defaults = mapOf(
    "SUPABASE_URL" to "https://rypdlwistramwltrekgt.supabase.co",
    "SUPABASE_ANON_KEY" to "sb_publishable_rg5hoFbDlGuk_3OzhPUJcw_VwbuZ1P0",
)

fun cfg(name: String): String {
    val v = (project.findProperty(name) as String?)?.takeIf { it.isNotBlank() }
        ?: System.getenv(name)?.takeIf { it.isNotBlank() }
        ?: defaults[name] ?: ""
    return v.trim().replace("\\", "\\\\").replace("\"", "\\\"")
}

android {
    namespace = "com.smartexam.academy"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.smartexam.academy"
        minSdk = 26
        targetSdk = 34
        // v2 (premium UI, OTP sign-up, notifications, AI गुरु): code jumps past every v1 build.
        versionCode = 1000 + runNumber
        versionName = "2.0.$runNumber"
        buildConfigField("String", "SUPABASE_URL", "\"${cfg("SUPABASE_URL").trimEnd('/')}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${cfg("SUPABASE_ANON_KEY")}\"")
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        create("release") {
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = "disha"
                keyPassword = System.getenv("KEYSTORE_PASSWORD")
                storeType = "pkcs12"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (keystorePath != null) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { buildConfig = true; compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
    lint { abortOnError = false; checkReleaseBuilds = false }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.security:security-crypto:1.0.0")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
}
