package com.dino.sample.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.applovin.mediation.MaxAd
import com.applovin.mediation.nativeAds.MaxNativeAdView
import com.dino.ads.admob.AdmobUtils
import com.dino.ads.admob.OnResumeUtils
import com.dino.ads.admob.RemoteUtils
import com.dino.ads.applovin.ApplovinUtils
import com.dino.ads.applovin.NativeCallback
import com.dino.ads.utils.replaceActivity
import com.dino.sample.R
import com.dino.sample.RemoteConfig
import com.dino.sample.databinding.ActivitySplashBinding
import com.dino.sample.utils.AdsManager
import com.dino.sample.utils.ApplovinManager


@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private val binding by lazy { ActivitySplashBinding.inflate(layoutInflater) }
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        if (!isTaskRoot
            && intent.hasCategory(Intent.CATEGORY_LAUNCHER)
            && intent.action != null && intent.action == Intent.ACTION_MAIN
        ) {
            finish()
            return
        }

        if (!AdmobUtils.isNetworkConnected(this)) {
            binding.tvLoading.visibility = View.INVISIBLE
            AlertDialog.Builder(this)
                .setTitle("No Internet Connection")
                .setMessage("Please check your internet connection and try again.")
                .setPositiveButton("OK") { _, _ -> finish() }
                .setCancelable(false)
                .show()
            return
        }

        //* Fetch RemoteConfig values
        RemoteUtils.enableLogId = true
        RemoteUtils.init(R.xml.remote_config_defaults) {
            //* Gather consent
            AdmobUtils.setupCMP(this) {
                //* Init Admob
                AdmobUtils.initAdmob(this, isDebug = true)

                //* Init Ads On Resume
                OnResumeUtils.getInstance().init(this)

                //* Load Native Language
                AdsManager.loadNativeLanguage(this, RemoteConfig.NATIVE_LANGUAGE)

                //* Show Interstitial or AOA based on Remote Config value
                showInterOrAoa()
            }
        }
    }

    private fun showInterOrAoa() {
        AdmobUtils.loadAndShowAdSplash(this, RemoteConfig.ADS_SPLASH, R.layout.ad_template_fullscreen, object : AdmobUtils.InterCallback() {
            override fun onInterClosed() {
                nextActivity()
            }

            override fun onInterFailed(error: String) {
                handler.postDelayed({ nextActivity() }, 100)
            }
        })
    }

    private fun nextActivity() {
        replaceActivity<LanguageActivity>()
//        replaceActivity<MainActivity>()
    }

    private fun initApplovin() {
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
//                    Utils.getInstance().replaceActivity(this@SplashActivity, MainActivity::class.java)
                }
            })
    }

}