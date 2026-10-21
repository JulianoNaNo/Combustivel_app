plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.projetodoappdecombustivel"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.projetodoappdecombustivel"
        minSdk = 24
        targetSdk = 37
        versionCode = 2
        versionName = "0.0.0.2L"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    flavorDimensions += "ambiente"

    productFlavors {
        create("dev") {
            dimension = "ambiente"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
        }
        create("prod") {
            dimension = "ambiente"
        }
    }

    buildTypes {
        // ...seu bloco release existente...
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
