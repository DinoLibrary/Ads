package com.dino.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dino.ads.AdmobUtils
import com.dino.ads.remote_config.AdmobHolder
import com.dino.sample.utils.AdmobManager
import com.dino.sample.databinding.ActivityMain2Binding

class NativeFullScreenActivity : AppCompatActivity() {
    val binding by lazy { ActivityMain2Binding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

//        AdmobManager.showAdsNativeFullScreen(
//            this,
//            AdmobManager.nativeHolderFull,
//            binding.bannerContainer
//        )
//        AdmobUtils.showNativeFullScreen(
//            this,
//            binding.bannerContainer,
//            RemoteConfig.NATIVE_INTRO_FULLSCREEN,
//            R.layout.ad_native_fullscreen,
//            object : AdmobUtils.NativeCallbackSimple{
//                override fun onNativeLoaded() {
//                }
//
//                override fun onNativeFailed(error: String) {
//                }
//
//            })

    }
}