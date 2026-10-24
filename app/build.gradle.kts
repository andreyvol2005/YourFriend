import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

// Секреты не попадают в репозиторий: файл добавлен в .gitignore.
val gigachatProperties = Properties().apply {
    val file = rootProject.file("gigachat.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

fun property(name: String, fallback: String = ""): String =
    gigachatProperties.getProperty(name, localProperties.getProperty(name, fallback))
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")

android {
    namespace = "com.example.yourfriend"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.yourfriend"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "GIGACHAT_AUTH_KEY", "\"${property("gigachat.authKey")}\"")
        buildConfigField(
            "String",
            "GIGACHAT_SCOPE",
            "\"${property("gigachat.scope", "GIGACHAT_API_PERS")}\""
        )
        buildConfigField(
            "String",
            "GIGACHAT_MODEL",
            "\"${property("gigachat.model", "GigaChat-3-Pro")}\""
        )
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.dynamicanimation)
    implementation(libs.material)
    implementation(libs.okhttp)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}