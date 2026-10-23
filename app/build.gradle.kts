plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "edu.sv.ues.dam235.parcial2"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "edu.sv.ues.dam235.parcial2"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

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
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)

    // Firebase (la BoM maneja las versiones)
    implementation(platform("com.google.firebase:firebase-bom:35.1.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-auth")

    // Room (Java usa annotationProcessor)
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")

    // Mapa
    implementation("com.google.android.gms:play-services-maps:19.0.0")

    // SharedPreferences cifrado
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
}