plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.sic_2"
    compileSdk = 35

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

    // Retrofit for API calls
    implementation (libs.retrofit)
    implementation (libs.converter.gson)

// OkHttp for networking
    implementation (libs.okhttp)

// Kotlin Coroutines for asynchronous tasks
    implementation (libs.kotlinx.coroutines.core)
    implementation (libs.kotlinx.coroutines.android)

    // Firebase Libraries
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.inappmessaging)
    implementation(libs.firebase.appcheck.playintegrity)


    implementation ("de.hdodenhof:circleimageview:3.1.0")
    implementation ("androidx.cardview:cardview:1.0.0")


    // Google Play Services Auth
    implementation("com.google.android.gms:play-services-auth:20.4.0") // Use a specific version

    implementation (libs.firebase.messaging)


    // Navigation Components
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)

    implementation ("com.github.bumptech.glide:glide:4.13.0")


    implementation (libs.com.google.firebase.firebase.storage)
    implementation (libs.com.google.firebase.firebase.database2)

    implementation (libs.work.runtime)
    implementation ("com.google.guava:guava:32.1.3-android")
    implementation (libs.picasso)


    implementation ("de.hdodenhof:circleimageview:3.1.0")
    implementation ("com.github.bumptech.glide:glide:4.12.0")


    // ImageKit
    //implementation (libs.github.imagekit.android)


    //implementation ("io.appwrite:sdk-for-android:2.0.0")

    implementation("com.squareup.okhttp3:okhttp:4.11.0") // or latest version
    implementation("com.google.code.gson:gson:2.10.1")
//    implementation ('com.squareup.retrofit2:retrofit:2.9.0')
//    implementation ('com.squareup.retrofit2:converter-gson:2.9.0')
//    implementation ('com.squareup.okhttp3:okhttp:4.9.3')
//    implementation ('com.squareup.okhttp3:logging-interceptor:4.9.3')
    implementation (libs.cloudinary.android)

    // Material CalendarView
    implementation(libs.material.calendarview)


    implementation("com.amazonaws:aws-android-sdk-s3:2.35.0") // or latest version
    implementation("com.amazonaws:aws-android-sdk-core:2.35.0")

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
    implementation(libs.recyclerview)
    annotationProcessor(libs.compiler)

    // Floating Action Button
    implementation(libs.fab)


    // Test Dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}