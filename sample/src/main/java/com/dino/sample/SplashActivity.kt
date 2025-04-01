package com.dino.sample

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
import com.dino.ads.AdmobUtils
import com.dino.ads.AppOpenUtils
import com.dino.ads.ApplovinUtils
import com.dino.ads.callback_applovin.NativeCallback
import com.dino.ads.remote_config.RemoteUtils
import com.dino.ads.utils.Utils
import com.dino.ads.utils.replaceActivity
import com.dino.sample.databinding.ActivitySplashBinding
import com.dino.sample.utils.ApplovinManager


class SplashActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())

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
        RemoteUtils.init(R.xml.remote_config_defaults, isDebug = true) {
            //* Gather consent
            AdmobUtils.setupCMP(this) {
                //* Init Admob
                AdmobUtils.initAdmob(this, isDebug = true)

                //* Init Ads On Resume
                AppOpenUtils.getInstance().init(application, getString(com.dino.ads.R.string.test_admob_on_resume_id))
                AppOpenUtils.getInstance().disableAppResumeWithActivity(SplashActivity::class.java)

                //* Show Interstitial or AOA based on Remote Config value
                showInterOrAoa()

                //* Init Applovin
                if (ApplovinUtils.isNetworkConnected(this)) {
                    initApplovin()
                }
            }
        }
    }

    private fun showInterOrAoa() {
        handler.postDelayed({ nextActivity() }, 3000)
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
                    Utils.getInstance().replaceActivity(this@SplashActivity, MainActivity::class.java)
                }
            })
    }

    private fun nextActivity() {
        replaceActivity<MainActivity>()
    }

}