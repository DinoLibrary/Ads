package com.dino.ads.remote_config

import android.util.Log
import androidx.annotation.XmlRes
import com.dino.ads.AdmobUtils
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

object RemoteConfigUtils {

    @JvmStatic
    fun init(
        @XmlRes xmlFile: Int,
        remoteEntries: MutableMap<String, String>,
        onCompleted: (Map<String, String>) -> Unit
    ) {
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600)
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(xmlFile)

        remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate: ConfigUpdate) {
                remoteConfig.activate().addOnCompleteListener {
                    val newEntries = remoteEntries.mapValues { remoteConfig.getString(it.key) }
                    AdmobUtils.isEnableAds = enableAds()
                    onCompleted(newEntries)
                }
            }

            override fun onError(error: FirebaseRemoteConfigException) {
                Log.d("===RemoteConfig", "onError: ${error.message}")
            }
        })

        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val newEntries = remoteEntries.mapValues { remoteConfig.getString(it.key) }
                AdmobUtils.isEnableAds = enableAds()
                onCompleted(newEntries)
            }
        }
    }

    fun getValue(key: String): String {
        return FirebaseRemoteConfig.getInstance().getString(key)
    }

    fun checkTestAd() = getValue("check_test_ad") != "0" && AdmobUtils.isTesting
    fun enableAds() = getValue("enable_ads") == "1"
}
