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
    // Firebase BOM (Bill of Materials)
    implementation(platform(libs.firebase.bom))

    // Firebase Libraries
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.inappmessaging)
    implementation(libs.firebase.appcheck.playintegrity)

    // Google Play Services Auth
    implementation("com.google.android.gms:play-services-auth:20.4.0") // Use a specific version

    // Navigation Components
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)

    // Material CalendarView
    implementation(libs.material.calendarview)

    // App Dependencies
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.preference)

    // Retrofit & Gson
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.gson)

    // Image Loading with Glide
    implementation(libs.glide)
    annotationProcessor(libs.compiler)

    // Floating Action Button
    implementation(libs.fab)

    // Test Dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}