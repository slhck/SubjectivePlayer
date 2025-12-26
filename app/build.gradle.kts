plugins {
    id("com.android.application")
}

android {
    namespace = "org.univie.subjectiveplayer"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.univie.subjectiveplayer"
        minSdk = 24
        targetSdk = 35
        versionCode = 9
        versionName = "2.4.0"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.preference:preference:1.2.1")
    implementation("com.google.android.material:material:1.12.0")

    // Unit testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core:1.6.1")
}
