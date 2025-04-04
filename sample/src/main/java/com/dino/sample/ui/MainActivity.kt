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

        binding.btnLoadShowBannerCollap.setOnClickListener {
            AdsManager.loadAndShowBanner(this, RemoteConfig.BANNER_HOME2, binding.flBanner)
        }

        binding.btnLoadShowBannerCollapTop.setOnClickListener {
            AdsManager.loadAndShowBanner(this, RemoteConfig.BANNER_HOME4.anchorTop(), binding.flBannerTop)
        }

        binding.btnLoadShowBannerNative.setOnClickListener {
            AdsManager.loadAndShowBanner(this, RemoteConfig.BANNER_HOME3, binding.flBanner)
        }

        binding.btnLoadShowNative.setOnClickListener {
            AdsManager.loadAndShowNative(this, RemoteConfig.NATIVE_HOME, binding.flNative)
        }

        binding.btnLoadShowNativeCollap.setOnClickListener {
            AdsManager.loadAndShowNativeCollap(this, RemoteConfig.NATIVE_HOME, binding.flBanner)
        }

        binding.btnLoadShowNativeCollapTop.setOnClickListener {
            AdsManager.loadAndShowNativeCollap(this, RemoteConfig.BANNER_HOME5.anchorTop(), binding.flBannerTop)
        }

        binding.btnLoadShowInter.setOnClickListener {
            AdsManager.loadAndShowInter(this, RemoteConfig.INTER_HOME) {
                addActivity<InterDummyActivity>()
            }
        }
        binding.btnLoadShowInterNative.setOnClickListener {
            AdmobUtils.loadAndShowInterNative(this, RemoteConfig.INTER_NATIVE_HOME, binding.clNativeFull, R.layout.ad_template_fullscreen){
                addActivity<InterDummyActivity>()
            }
        }
    }

}