import javax.xml.parsers.DocumentBuilderFactory

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("maven-publish")
}

        tasks.register("generateRemoteConfig") {
            doLast {
                val xmlPath = project.findProperty("remoteConfigPath") as? String
                    ?: throw IllegalArgumentException("❌ RemoteConfig XML path not provided by the project!")

                val outputDirPath = "src/main/java/com/example/admob/generated"
                val packageName = "com.example.admob.generated"

                val xmlFile = File(xmlPath)
                if (!xmlFile.exists()) {
                    throw IllegalArgumentException("❌ Missing remote_config_defaults.xml at $xmlFile")
                }

                val outputDir = file(outputDirPath)
                val outputFile = File(outputDir, "RemoteConfig.kt")

                val document = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(xmlFile)

                val entries = document.getElementsByTagName("entry")

                val content = buildString {
                    append("package $packageName\n\n")
                    append("object RemoteConfig {\n")

                    for (i in 0 until entries.length) {
                        val node = entries.item(i)
                        val keyNode = node.childNodes?.let {
                            (0 until it.length).map { index -> it.item(index) }
                                .find { it.nodeName == "key" }
                        }
                        val valueNode = node.childNodes?.let {
                            (0 until it.length).map { index -> it.item(index) }
                                .find { it.nodeName == "value" }
                        }

                        if (keyNode == null || valueNode == null) {
                            throw IllegalArgumentException("❌ Invalid entry in remote_config_defaults.xml. Each entry must have both <key> and <value> tags.")
                        }

                        val key = keyNode.textContent.trim().replace("-", "_")
                        val defaultValue = valueNode.textContent.trim()
                        append("    const val $key: String = \"$defaultValue\"\n")
                    }

                    append("}\n")
                }

                outputDir.mkdirs()
                outputFile.writeText(content)

                println("✅ RemoteConfig.kt generated at: $outputFile")
            }
        }

// Ensure the task runs before compilation
tasks.named("preBuild").configure {
    dependsOn("generateRemoteConfig")
}

android {
    namespace = "com.dino.ads"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
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
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.5.1")
    implementation("androidx.lifecycle:lifecycle-process:2.5.1")

    // UI
    implementation("androidx.appcompat:appcompat:1.5.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
    implementation("com.pnikosis:materialish-progress:1.7")
    implementation("com.facebook.shimmer:shimmer:0.5.0@aar")

    // Ads
    implementation("com.applovin:applovin-sdk:13.0.0")
    implementation("com.google.android.gms:play-services-ads:23.3.0")
    implementation("com.intuit.sdp:sdp-android:1.1.1")

    // Other
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.airbnb.android:lottie:6.4.0")
    implementation("com.google.android.ump:user-messaging-platform:3.0.0")

    //Adjust
    implementation("com.adjust.sdk:adjust-android:5.0.0")
    implementation("com.android.installreferrer:installreferrer:2.2")
    implementation("com.google.android.gms:play-services-ads-identifier:18.1.0")
    implementation("com.adjust.sdk:adjust-android-webbridge:5.0.0")

    implementation("org.jetbrains.kotlin:kotlin-reflect:2.0.21")
    implementation(libs.firebase.config.ktx)
}