plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.sic_2"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.sic_2"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}

dependencies {
    // Navigation Components
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
    implementation(libs.firebase.bom)

    // Firebase Authentication
    implementation(libs.google.firebase.auth)

    // Firebase Realtime Database
    implementation(libs.google.firebase.database)

    // Firebase Firestore
    implementation(libs.google.firebase.firestore)

    // Firebase Storage
    implementation(libs.google.firebase.storage)

    // Firebase In-App Messaging
    implementation(libs.google.firebase.inappmessaging)

    // Firebase App Check
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(platform(libs.firebase.bom.v3231)) // Use the latest version
    implementation(libs.com.google.firebase.firebase.auth)
    implementation(libs.com.google.firebase.firebase.database)

    // Material CalendarView (corrected exclusion syntax)
    implementation(libs.material.calendarview)

    // App dependencies
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.preference)

    // Firebase dependencies
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database.v2000) // Use only one version of Firebase Database
    implementation(libs.firebase.inappmessaging)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.firestore)
    implementation(libs.play.services.auth)

    // Retrofit & Gson for API calls
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.gson)

    // Image loading with Glide
    implementation(libs.glide)
    annotationProcessor(libs.compiler)

    // Floating Action Button
    implementation(libs.fab)

    // Test dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}