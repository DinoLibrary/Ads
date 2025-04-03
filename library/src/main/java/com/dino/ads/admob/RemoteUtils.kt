package com.dino.ads.admob

import android.util.Log
import androidx.annotation.XmlRes
import com.dino.ads.utils.log
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

object RemoteUtils {

    @JvmStatic
    fun init(@XmlRes xmlFile: Int, onCompleted: () -> Unit) {
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600)
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(xmlFile)

        remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate: ConfigUpdate) {
                remoteConfig.activate().addOnCompleteListener {
                    AdmobUtils.isEnableAds = enableAds()
                }
            }

            override fun onError(error: FirebaseRemoteConfigException) {
                Log.d("===RemoteConfig", "onError: ${error.message}")
            }
        })

        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            task.exception?.let { Log.e("===RemoteConfig", "onComplete: $it") }
//            if (task.isSuccessful) {
            AdmobUtils.isEnableAds = enableAds()
            onCompleted()
//            }
        }
    }

    fun getValue(key: String): String {
        val value = FirebaseRemoteConfig.getInstance().getString(key)
        log("getValue: $key = $value")
        return value
    }

    fun getAdId(key: String): String {
        val adId = FirebaseRemoteConfig.getInstance().getString("${key.uppercase()}_ID")
        log("getAdId: ${key.uppercase()}_ID: $adId")
        return adId
    }

    fun checkTestAd() = getValue("check_test_ad") != "0" && AdmobUtils.isTesting

    fun enableAds() = getValue("enable_ads") == "1"

    @Deprecated("Không sử dụng. Mặc định isDebug = true sẽ hiện log")
    fun enableLog() {
//        enableLog = true
    }
}
