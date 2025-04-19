plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("maven-publish")
}

android {
    namespace = "com.dino.ads"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
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
}

publishing {
    publications {
        register<MavenPublication>("release") {
            afterEvaluate {
                from(components["release"])
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    implementation(libs.androidx.lifecycle.runtime.ktx.v251)
    implementation(libs.androidx.lifecycle.process.v251)

    // UI
    implementation(libs.androidx.appcompat.v170)
    implementation(libs.materialish.progress)
    implementation(libs.shimmer)

    // Ads
    implementation(libs.applovin.sdk.v1320)
    implementation(libs.play.services.ads.v2420)
    implementation(libs.sdp.android)

    // Other
    implementation(libs.gson)
    implementation(libs.lottie.v640)
    implementation(libs.user.messaging.platform)

    //Adjust
    implementation(libs.adjust.android.v520)
    implementation(libs.installreferrer)
    implementation(libs.play.services.ads.identifier)
    implementation(libs.adjust.android.webbridge.v520)

    implementation(libs.kotlin.reflect)
    implementation(libs.firebase.config.ktx)
}