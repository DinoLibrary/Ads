package com.dino.ads

import android.content.Context
import android.content.res.XmlResourceParser
import android.util.Log
import androidx.annotation.XmlRes
import com.dino.ads.utils.admod.remote.BannerPlugin.Companion.log
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import org.xmlpull.v1.XmlPullParser


object RemoteConfigUtils {
    private lateinit var remoteConfig: FirebaseRemoteConfig

    fun init(context: Context, @XmlRes xmlFile: Int) {
        remoteConfig = FirebaseRemoteConfig.getInstance()

        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600) // Adjust fetch interval
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)

        // Read XML and generate default values dynamically
        val defaultValues = parseXml(context, xmlFile)
        remoteConfig.setDefaultsAsync(defaultValues)

        // Fetch and update values
        fetchConfig()
    }

    private fun fetchConfig() {
        remoteConfig.fetch().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                remoteConfig.activate()
            }
//            AppConfig.updateValues(remoteConfig)
        }
    }

    private fun parseXml(context: Context, @XmlRes xmlFile: Int): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        val parser: XmlResourceParser = context.resources.getXml(xmlFile)

        var key: String? = null
        var value: String? = null

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "entry") {
                        key = parser.getAttributeValue(null, "key")
                        value = parser.getAttributeValue(null, "value")
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "entry" && key != null && value != null) {
                        result[key] = value
//                        AppConfig.setDefault(key, value) // Dynamically create variables
                    }
                }
            }
            parser.next()
        }
        return result
    }
}
