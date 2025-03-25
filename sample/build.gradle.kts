plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

// Inside app/build.gradle.kts
tasks.register("generateRemoteConfig") {
    doLast {
        val remoteConfigFile = file("src/main/res/xml/remote_config_defaults.xml")
        if (!remoteConfigFile.exists()) {
            throw IllegalArgumentException("❌ remote_config_defaults.xml is missing at ${remoteConfigFile.absolutePath}")
        }

        // Pass the XML file path to the library
        project.extensions.extraProperties["remoteConfigPath"] = remoteConfigFile.absolutePath
    }
}

// Ensure this task runs before preBuild
tasks.named("preBuild").configure {
    dependsOn("generateRemoteConfig")
}

android {
    namespace = "com.dino.sample"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dino.sample"
        minSdk = 27
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures{
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(project(":library"))

    implementation(libs.applovin.sdk)
    implementation(libs.applovin)
    implementation(libs.play.services.ads)
}