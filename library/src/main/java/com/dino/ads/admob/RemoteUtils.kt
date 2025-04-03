package com.dino.ads.admob

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.XmlRes
import com.dino.ads.utils.log
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

object RemoteUtils {

    var enableLog = false

    @JvmStatic
    fun init(@XmlRes xmlFile: Int, isDebug: Boolean, onCompleted: () -> Unit) {
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
            if (task.isSuccessful) {
                AdmobUtils.isEnableAds = enableAds()
                onCompleted()
            }
        }
        if (isDebug) Handler(Looper.getMainLooper()).postDelayed({ onCompleted() }, 1000)
    }

    fun getValue(key: String): String {
        if (enableLog) log("getValue: $key - ${FirebaseRemoteConfig.getInstance().getString(key)}")
        return FirebaseRemoteConfig.getInstance().getString(key)
    }

    fun getAdId(key: String): String {
        if (enableLog) log("getValue: $key - ${FirebaseRemoteConfig.getInstance().getString(key)}")
        return FirebaseRemoteConfig.getInstance().getString("${key.uppercase()}_ID")
    }

    fun checkTestAd() = getValue("check_test_ad") != "0" && AdmobUtils.isTesting
    fun enableAds() = getValue("enable_ads") == "1"
}
