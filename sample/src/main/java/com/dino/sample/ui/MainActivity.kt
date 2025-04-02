package com.dino.sample.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dino.ads.AdmobUtils
import com.dino.ads.utils.addActivity
import com.dino.sample.R
import com.dino.sample.databinding.ActivityMainBinding
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.nativead.NativeAd

class MainActivity : AppCompatActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.btnLoadShowBanner.setOnClickListener {
//            AdmobUtils.loadAndShowBanner(this, binding.flBanner, RemoteConfig.BANNER_HOME, AdmobUtils.BannerCallback().triggerBannerClicked {  })

            AdmobUtils.loadAndShowBanner(this, binding.flBanner, RemoteConfig.BANNER_HOME, object : AdmobUtils.BannerCallback {
                override fun onBannerClicked() {
                }

                override fun onBannerLoaded(adSize: AdSize) {
                }

                override fun onBannerFailed(error: String) {
                }
            })
        }

        binding.btnLoadShowNative.setOnClickListener {
            AdmobUtils.loadAndShowNativeDual(this, binding.flNative, RemoteConfig.NATIVE_HOME, R.layout.ad_template_medium, object : AdmobUtils.NativeCallback {
                override fun onNativeClicked() {
                }

                override fun onNativeReady(ad: NativeAd?) {
                }

                override fun onNativeFailed(error: String) {
                }
            })
        }

        binding.btnLoadShowInter.setOnClickListener {
            AdmobUtils.loadAndShowInterstitial(this, RemoteConfig.INTER_HOME, object : AdmobUtils.InterCallback {
                override fun onStartAction() {
                }

                override fun onDismissedInter() {
                    addActivity<InterDummyActivity>()
                }

                override fun onInterShowed() {
                }

                override fun onInterLoaded() {
                }

                override fun onInterFailed(error: String) {
                    addActivity<InterDummyActivity>()
                }

            })
        }
    }

}