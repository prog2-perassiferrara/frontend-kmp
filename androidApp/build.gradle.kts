import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.net.URI

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

val turnosUrl = providers.gradleProperty("TURNOS_BASE_URL")
val catalogUrl = providers.gradleProperty("CATALOG_BASE_URL")
fun quotedUrl(value: String): String = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
    .replace("\n", "\\n").replace("\r", "\\r") + "\""

val validateReleaseEndpoints by tasks.registering {
    val urls = listOf(turnosUrl.getOrElse(""), catalogUrl.getOrElse(""))
    inputs.property("endpoints", urls)
    doLast {
        check(urls.all { value ->
            runCatching {
                val uri = URI(value)
                uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.rawUserInfo == null &&
                    uri.rawQuery == null && uri.rawFragment == null && uri.path in listOf("", "/") &&
                    (uri.port == -1 || uri.port in 1..65535)
            }.getOrDefault(false)
        }) { "Release requiere TURNOS_BASE_URL y CATALOG_BASE_URL como orígenes HTTPS propios." }
    }
}
tasks.matching { it.name in setOf("packageRelease", "bundleRelease", "installRelease") }
    .configureEach { dependsOn(validateReleaseEndpoints) }

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "com.prog2.frontend"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.prog2.frontend"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        debug {
            buildConfigField("String", "TURNOS_BASE_URL", quotedUrl(turnosUrl.getOrElse("http://10.0.2.2:8081")))
            buildConfigField("String", "CATALOG_BASE_URL", quotedUrl(catalogUrl.getOrElse("http://10.0.2.2:8080")))
        }
        release {
            buildConfigField("String", "TURNOS_BASE_URL", quotedUrl(turnosUrl.getOrElse("")))
            buildConfigField("String", "CATALOG_BASE_URL", quotedUrl(catalogUrl.getOrElse("")))
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
}
