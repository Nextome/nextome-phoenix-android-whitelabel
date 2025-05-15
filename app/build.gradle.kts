plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.nextome.test"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.nextome.test"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
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

    packagingOptions {
        resources {
            excludes += listOf("META-INF/DEPENDENCIES", "META-INF/LICENSE", "META-INF/LICENSE.txt",
                "META-INF/license.txt", "META-INF/NOTICE", "META-INF/NOTICE.txt", "META-INF/notice.txt",
                "META-INF/ASL2.0", "META-INF/atomicfu.kotlin_module", "META-INF/INDEX.LIST")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

val ktorVersion = "2.2.4"
val koinVersion = "3.4.0"

dependencies {

    implementation("com.nextome.localization:nextome_localization:3.0.2")
    implementation("com.nextome.nextomemapview:nextomemapview:2.0.21.0")
    implementation("net.nextome.nextome_map_module:flutter:2.0.23")
    // implementation("com.nextome.localization_map_utils:nextome_localization_map_utils:1.5.1.1")
    // implementation("net.nextome.nextome_map_module:flutter:1.5.1")
    implementation ("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.8.0")
    implementation ("androidx.activity:activity-ktx:1.6.1")
    implementation ("androidx.fragment:fragment-ktx:1.5.4")
    implementation ("com.vmadalin:easypermissions-ktx:1.0.0")
    implementation ("androidx.lifecycle:lifecycle-livedata-ktx:2.5.1")
    implementation ("androidx.appcompat:appcompat:1.5.1")
    implementation ("com.google.android.material:material:1.7.0")
    implementation ("androidx.cardview:cardview:1.0.0")
    implementation ("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha05")

    implementation("io.ktor:ktor-client-android:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-client-auth:$ktorVersion")

    implementation("io.insert-koin:koin-android:$koinVersion")

    // OpenStreetMaps Example
    implementation("org.osmdroid:osmdroid-android:6.1.8")

    testImplementation ("junit:junit:4.13.2")
    androidTestImplementation ("androidx.test:runner:1.4.0")
    androidTestImplementation ("androidx.test.espresso:espresso-core:3.4.0")
}