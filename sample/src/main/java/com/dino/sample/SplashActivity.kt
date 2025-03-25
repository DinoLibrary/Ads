package com.dino.sample

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.applovin.mediation.MaxAd
import com.applovin.mediation.nativeAds.MaxNativeAdView
import com.dino.ads.AdmobUtils
import com.dino.ads.AppOpenUtils
import com.dino.ads.ApplovinUtils
import com.dino.ads.callback_applovin.NativeCallback
import com.dino.ads.utils.Utils
import com.dino.sample.databinding.ActivitySplashBinding
import com.dino.sample.utils.ApplovinManager


class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (!isTaskRoot
            && intent.hasCategory(Intent.CATEGORY_LAUNCHER)
            && intent.action != null && intent.action == Intent.ACTION_MAIN
        ) {
            finish()
            return
        }

        AdmobUtils.initAdmob(this, 10000, isDebug = true, isEnableAds = true, check_test_ad = "0")

        AppOpenUtils.getInstance().init(application, getString(com.dino.ads.R.string.test_ads_admob_app_open_new))
        AppOpenUtils.getInstance().disableAppResumeWithActivity(SplashActivity::class.java)

        test()

        if (ApplovinUtils.isNetworkConnected(this)) {
            ApplovinUtils.initApplovin(
                application,
                "Hd8NW44NTx4ndvT7Pw2PIQR_omwB0DB00BKnHGXorX1hCETptrgiRyRCtDcZqbhU9Wi_l4R0Icd5N5SkKJFGIy",
                isDebug = true,
                isEnableAds = true,
                callback = object : ApplovinUtils.Initialization {
                    override fun onInitSuccessful() {
                        ApplovinUtils.loadNative(
                            this@SplashActivity,
                            ApplovinManager.nativeHolder, object :
                                NativeCallback {
                                override fun onNativeAdLoaded(
                                    nativeAd: MaxAd?,
                                    nativeAdView: MaxNativeAdView?
                                ) {
                                    Toast.makeText(
                                        this@SplashActivity,
                                        "Loaded",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                                override fun onAdFail(error: String) {
                                    Toast.makeText(
                                        this@SplashActivity,
                                        "Failed",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                                override fun onAdRevenuePaid(ad: MaxAd) {

                                }
                            })
                        Utils.getInstance().replaceActivity(this@SplashActivity, HomeActivity::class.java)
                    }
                })
        } else {
            Utils.getInstance().replaceActivity(this@SplashActivity, HomeActivity::class.java)
        }
    }

    private fun test() {
//        RemoteConfig
//        RemoteConfig.ads_splash
//        RemoteConfigUtils.init(this, R.xml.remote_config_defaults) {
//            Toast.makeText(this, "Remote Config init success", Toast.LENGTH_SHORT).show()
//        }
    }
}