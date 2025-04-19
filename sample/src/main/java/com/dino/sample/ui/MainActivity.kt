package com.dino.sample.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dino.ads.admob.AdmobUtils
import com.dino.ads.utils.addActivity
import com.dino.sample.R
import com.dino.sample.RemoteConfig
import com.dino.sample.databinding.ActivityMainBinding
import com.dino.sample.utils.AdsManager

class MainActivity : AppCompatActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.btnLoadShowBanner.setOnClickListener {
            AdsManager.loadAndShowBanner(this, RemoteConfig.BANNER_HOME, binding.flBanner)
        }

        binding.btnLoadShowBannerTop.setOnClickListener {
            AdsManager.loadAndShowBanner(this, RemoteConfig.BANNER_HOME_TOP.anchorTop(), binding.flBannerTop)
        }

        binding.btnLoadShowBannerCollap.setOnClickListener {
            AdsManager.loadAndShowBanner(this, RemoteConfig.BANNER_HOME_COLLAP, binding.flBanner)
        }

        binding.btnLoadShowBannerCollapTop.setOnClickListener {
            AdsManager.loadAndShowBanner(this, RemoteConfig.BANNER_HOME_COLLAP_TOP.anchorTop(), binding.flBannerTop)
        }

        binding.btnLoadShowNative.setOnClickListener {
            AdsManager.loadAndShowNative(this, RemoteConfig.NATIVE_HOME, binding.flNative)
        }

        binding.btnLoadShowNativeCollap.setOnClickListener {
            AdsManager.loadAndShowBanner(this, RemoteConfig.BANNER_HOME_COLLAP2, binding.flBanner)
        }

        binding.btnLoadShowNativeCollapTop.setOnClickListener {
            AdsManager.loadAndShowBanner(this, RemoteConfig.BANNER_HOME_COLLAP_TOP2.anchorTop(), binding.flBannerTop)
        }

        binding.btnLoadShowInter.setOnClickListener {
            AdmobUtils.loadAndShowInterstitial(this, RemoteConfig.INTER_HOME, R.layout.ad_template_fullscreen) {
                addActivity<InterDummyActivity>()
            }
        }
        binding.btnLoadShowInterWithNative.setOnClickListener {
            AdmobUtils.loadAndShowInterstitial(this, RemoteConfig.INTER_HOME2, R.layout.ad_template_fullscreen) {
                addActivity<InterDummyActivity>()
            }
        }
    }

}