plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.fintracker"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.fintracker"
        minSdk = 24
        targetSdk = 36
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
    implementation("com.tbuonomo:dotsindicator:5.0")
    implementation("com.github.yalantis:ucrop:2.2.11")
// OR for better quality (uses native code, slightly larger APK)
    implementation("com.github.yalantis:ucrop:2.2.11-native")
    // AndroidX
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.cardview)
    implementation(libs.recyclerview)

    implementation("androidx.compose.material:material-icons-extended:1.6.8")

    // Firebase BOM
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)

    // Google Play Services
    implementation(libs.play.services.auth)

    // Charts
    implementation(libs.mpandroidchart)

    // Glide (image loading)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // UCrop for image cropping
    implementation("com.github.yalantis:ucrop:2.2.8")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
