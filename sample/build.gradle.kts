import javax.xml.parsers.DocumentBuilderFactory

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.google.gms.google-services")
}

val packageName = "com.dino.sample"

android {
    namespace = packageName
    compileSdk = 35

    defaultConfig {
        applicationId = packageName
        minSdk = 27
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
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

    buildFeatures {
        viewBinding = true
        buildConfig = true
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

//* Task to generate RemoteConfig.kt
val generateRemoteConfig = tasks.register("generateRemoteConfig") {
    val xmlFile = file("src/main/res/xml/remote_config_defaults.xml")
    val outputDir = file("build/generated/source/remoteConfig/")
    val outputFile = File(outputDir, "RemoteConfig.kt")
    doLast {
        if (!xmlFile.exists()) throw GradleException("❌ RemoteConfig XML not found!")
        outputDir.mkdirs()
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile)
        val entries = doc.getElementsByTagName("entry")
        val configVars = mutableListOf<String>()
        val delegatedVars = mutableListOf<String>()

        for (i in 0 until entries.length) {
            val entry = entries.item(i)
            val key = entry.childNodes.item(1).textContent
            val value = entry.childNodes.item(3).textContent
            if (key == "check_test_ad" || key == "enable_ads" || key == "on_resume" || key.takeLast(3) == "_ID") {
                //* Do nothing
            } else if (key.endsWith("_full")) {
                configVars.add("        \"$key\" to \"$value\"")
                delegatedVars.add("    var ${key.uppercase()} = NativeFullHolder(\"${key.removeSuffix("_full").substringAfter("native_")}\")")
            } else if (key == "native_intro"||key == "native_language") {
                configVars.add("        \"$key\" to \"$value\"")
                delegatedVars.add("    var ${key.uppercase()} = NativeIntroHolder(\"${key.substringAfter("_")}\")")
            } else {
                configVars.add("        \"$key\" to \"$value\"")
                val uid = when {
                    key.startsWith("inter_native") -> key.substringAfter("inter_native_")
                    key.startsWith("reward_inter") -> "reward_inter_"
                    else -> key.substringAfter("_")
                }
                delegatedVars.add("    var ${key.uppercase()} = AdmobHolder(\"$uid\")")
            }
        }

        val remoteConfigCode = StringBuilder()
            .append("package $packageName\n\n")
            .append("import com.dino.ads.admob.*\n\n")
            .append("object RemoteConfig {\n")
            .append(delegatedVars.joinToString("\n"))
            .append("\n}\n")
            .toString()
        outputFile.writeText(remoteConfigCode)
    }
}

tasks.named("preBuild").configure {
    dependsOn(generateRemoteConfig)
}

android.sourceSets.getByName("main").kotlin.srcDir("build/generated/source/remoteConfig/")