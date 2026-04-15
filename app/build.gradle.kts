import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use(::load)
    }
}

android {
    namespace = "com.utt.foodcouriers_admin"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.utt.foodcouriers_admin"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "SUPABASE_URL",
            "\"${localProperties.getProperty("SUPABASE_URL", "")}\""
        )
        buildConfigField(
            "String",
            "SUPABASE_ANON_KEY",
            "\"${localProperties.getProperty("SUPABASE_ANON_KEY", "")}\""
        )
        buildConfigField(
            "String",
            "SUPABASE_STORAGE_BUCKET",
            "\"${localProperties.getProperty("SUPABASE_STORAGE_BUCKET", "images")}\""
        )
        buildConfigField(
            "String",
            "ADMIN_API_BASE_URL",
            "\"${localProperties.getProperty("ADMIN_API_BASE_URL", localProperties.getProperty("SUPABASE_URL", "") + "/functions/v1")}\""
        )
    }

    buildTypes {
        release {
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
        buildConfig = true
    }

    sourceSets {
        getByName("main") {
            res.srcDirs(
                "src/main/res",
                "src/main/res-layouts/common",
                "src/main/res-layouts/auth",
                "src/main/res-layouts/dashboard",
                "src/main/res-layouts/menu",
                "src/main/res-layouts/category",
                "src/main/res-layouts/order",
                "src/main/res-layouts/restaurant",
                "src/main/res-layouts/user",
                "src/main/res-layouts/shipper",
                "src/main/res-layouts/promotion",
                "src/main/res-layouts/report",
                "src/main/res-layouts/notification"
            )
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation(libs.supabase.kt)
    implementation(libs.glide)
    implementation(libs.swiperefreshlayout)
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
