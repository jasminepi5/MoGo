plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    // Supabase uses Kotlin serialization
    alias(libs.plugins.kotlin.serialization)

    // Secrets Gradle Plugin
    alias(libs.plugins.secrets)
}

android {
    namespace = "com.fit3161.fit3162.mogo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.fit3161.fit3162.mogo"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // MAPS_API_KEY, SUPABASE_URL, SUPABASE_ANON_KEY and MAPS_SHA1_FINGERPRINT come from
        // local.properties through the Secrets Gradle Plugin (see `secrets` below).
    }

    buildTypes {
        release {
            // TODO: enable once a release build has been tested on a device with keep rules for
            // kotlinx.serialization, Supabase, Retrofit and Gson.
            isMinifyEnabled = false
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
    // Migrating from kotlinOptions to compilerOptions
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// For Secrets Gradle Plugin.
secrets {
    propertiesFileName = "local.properties"
    defaultPropertiesFileName = "local.defaults.properties"

    // Make secrets available in AndroidManifest.xml as ${MAPS_API_KEY}
    // AND in Kotlin code as BuildConfig.MAPS_API_KEY
    ignoreList.add("sdk.*") // Don't expose sdk.dir as a BuildConfig field
    ignoreList.add("keyAlias") // Ignore signing config keys if present
    ignoreList.add("keyPassword")
    ignoreList.add("storeFile")
    ignoreList.add("storePassword")
}

dependencies {
    // AndroidX core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.datastore.preferences)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Image loading (Coil)
    implementation(libs.coil.compose)

    // Supabase (database, auth, realtime, storage) and its HTTP engine
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.auth)
    implementation(libs.supabase.realtime)
    implementation(libs.supabase.storage)
    implementation(libs.ktor.client.android)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Maps, location and Places
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    implementation(libs.maps.utils)
    implementation(libs.maps.compose)
    implementation(libs.places)

    // Retrofit for the Routes API
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
}
