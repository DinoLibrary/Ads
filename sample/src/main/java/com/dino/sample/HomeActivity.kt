package com.dino.sample

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.applovin.mediation.MaxAd
import com.applovin.mediation.nativeAds.MaxNativeAdLoader
import com.applovin.mediation.nativeAds.MaxNativeAdView
import com.applovin.sdk.AppLovinSdkUtils
import com.dino.ads.AdNativeSize
import com.dino.ads.AdmobUtils
import com.dino.ads.ApplovinUtils
import com.dino.ads.callback_applovin.NativeCallback
import com.dino.ads.callback_applovin.RewardCallback
import com.dino.ads.utils.admod.BannerAdmob
import com.dino.ads.utils.admod.RewardAdmob
import com.dino.ads.utils.admod.RewardInterAdmob
import com.dino.sample.databinding.ActivityMainBinding
import com.dino.sample.utils.AdmobManager
import com.dino.sample.utils.ApplovinManager
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MediaAspectRatio
import com.google.android.gms.ads.nativead.NativeAd

class HomeActivity : AppCompatActivity() {
    val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    lateinit var bannerContainer: ViewGroup
    lateinit var nativeLoader: MaxNativeAdLoader
    var rewardInterHolder = RewardInterAdmob("")
    var rewardHolder = RewardAdmob("")

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        val nativeAds = findViewById<FrameLayout>(R.id.nativead)
        bannerContainer = findViewById<FrameLayout>(R.id.banner_container)
        binding.btnShowNative.setOnClickListener {
            ApplovinManager.showAdsNative(this, ApplovinManager.nativeHolder, nativeAds)
        }
        binding.btnLoadInter.setOnClickListener {
            AdmobManager.loadInter(this, AdmobManager.interholder)
        }
        binding.btnLoadInterAppLovin.setOnClickListener {
            ApplovinManager.loadInter(this)
        }

        binding.btnShowInterAppLovin.setOnClickListener {
            ApplovinManager.showInter(
                this,
                ApplovinManager.interHolder,
                object : ApplovinManager.AdsOnClick {
                    override fun onAdsCloseOrFailed() {
                        startActivity(
                            Intent(
                                this@HomeActivity,
                                NativeFullScreenActivity::class.java
                            )
                        )
                    }
                })
        }

        binding.btnShowInter.setOnClickListener {
            AdmobManager.showInter(
                this,
                AdmobManager.interholder,
                object : AdmobManager.AdListener {
                    override fun onAdClosed() {
                        startActivity(
                            Intent(
                                this@HomeActivity,
                                NativeFullScreenActivity::class.java
                            )
                        )
                    }

                    override fun onFailed() {
                        startActivity(
                            Intent(
                                this@HomeActivity,
                                NativeFullScreenActivity::class.java
                            )
                        )
                    }
                },
                true
            )

        }

        binding.btnLoadShowInterCallback2.setOnClickListener {
            ApplovinManager.loadAndShowIntersial(
                this,
                ApplovinManager.interHolder,
                object : ApplovinManager.AdsOnClick {
                    override fun onAdsCloseOrFailed() {
                        startActivity(
                            Intent(
                                this@HomeActivity,
                                NativeFullScreenActivity::class.java
                            )
                        )
                    }
                })
        }

        binding.btnShowReward.setOnClickListener {
            ApplovinUtils.loadRewarded(this, "c10d259dcb47378d", 15000, object : RewardCallback {
                override fun onRewardReady() {
                    ApplovinUtils.showRewarded(
                        this@HomeActivity,
                        1500,
                        object : RewardCallback {
                            override fun onRewardReady() {

                            }

                            override fun onRewardClosed() {
                                Toast.makeText(
                                    this@HomeActivity,
                                    "onRewardClosed",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            override fun onRewardLoadFail(error: String) {
                            }

                            override fun onRewardShowSucceed() {
                            }

                            override fun onUserRewarded() {
                                Toast.makeText(
                                    this@HomeActivity,
                                    "onUserRewarded",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            override fun onRewardedVideoStarted() {
                            }

                            override fun onRewardedVideoCompleted() {
                                Toast.makeText(
                                    this@HomeActivity,
                                    "onRewardedVideoCompleted",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            override fun onAdRevenuePaid(ad: MaxAd?) {
                            }
                        })
                }

                override fun onRewardClosed() {
                }

                override fun onRewardLoadFail(error: String) {
                }

                override fun onRewardShowSucceed() {
                }

                override fun onUserRewarded() {
                }

                override fun onRewardedVideoStarted() {
                }

                override fun onRewardedVideoCompleted() {
                }

                override fun onAdRevenuePaid(ad: MaxAd?) {
                }
            })
        }

        binding.loadNative.setOnClickListener {
            AdmobManager.loadAdsNativeNew(this, AdmobManager.nativeHolder)
        }

        binding.loadNativeMax.setOnClickListener {
            ApplovinUtils.loadNative(
                this,
                ApplovinManager.nativeHolder,
                object : NativeCallback {
                    override fun onNativeAdLoaded(
                        nativeAd: MaxAd?,
                        nativeAdView: MaxNativeAdView?
                    ) {
                        Toast.makeText(this@HomeActivity, "Loaded", Toast.LENGTH_SHORT).show()
                    }

                    override fun onAdFail(error: String) {
                        Toast.makeText(this@HomeActivity, "Failed", Toast.LENGTH_SHORT).show()
                    }

                    override fun onAdRevenuePaid(ad: MaxAd) {

                    }

                })
        }

        binding.showNative.setOnClickListener {
            AdmobManager.showNative(this, nativeAds, AdmobManager.nativeHolder)
        }

        binding.showNativeMax.setOnClickListener {
            ApplovinUtils.showNative(
                nativeAds,
                this,
                ApplovinManager.nativeHolder,
                R.layout.native_custom_ad_view,
                AdNativeSize.MEDIUM,
                object :
                    NativeCallback {
                    override fun onNativeAdLoaded(
                        nativeAd: MaxAd?,
                        nativeAdView: MaxNativeAdView?
                    ) {
                        Toast.makeText(this@HomeActivity, "show success", Toast.LENGTH_SHORT).show()
                    }

                    override fun onAdFail(error: String) {
                        Toast.makeText(this@HomeActivity, "Show failed", Toast.LENGTH_SHORT).show()
                    }

                    override fun onAdRevenuePaid(ad: MaxAd) {

                    }
                })
        }
        binding.loadAndShowBanner.setOnClickListener {
            AdmobUtils.loadAndShowBanner(this, BannerAdmob(""), binding.bannerContainer, object : AdmobUtils.BannerCallback {
                override fun onBannerClicked() {

                }

                override fun onBannerLoaded(adSize: AdSize) {
                }

                override fun onBannerFailed(error: String) {
                }

                override fun onPaid(adValue: AdValue, adView: AdView) {
                }

            })
        }

        binding.bannerMax.setOnClickListener {
            ApplovinUtils.showBanner(this, binding.bannerContainer, "f443c90308f39f17", object :
                com.dino.ads.callback_applovin.BannerCallback {
                override fun onBannerLoadFail(error: String) {
                }

                override fun onBannerShowSucceed() {
                }

                override fun onAdRevenuePaid(ad: MaxAd) {

                }
            })
        }

        binding.btnLoadShowInterAdmob.setOnClickListener {
            AdmobUtils.loadAndShowInterstitial(this, AdmobManager.interholder, object :
                AdmobUtils.InterCallback {
                override fun onStartAction() {

                }

                override fun onDismissedInter() {
                    startActivity(Intent(this@HomeActivity, NativeFullScreenActivity::class.java))
                }

                override fun onInterShowed() {

                }

                override fun onInterLoaded() {

                }

                override fun onInterFailed(error: String) {
                    startActivity(Intent(this@HomeActivity, NativeFullScreenActivity::class.java))
                }

                override fun onPaid(adValue: AdValue?, adUnitAds: String?) {

                }
            }, true)
        }

        binding.loadAndShowNativeAdmob.setOnClickListener {
            AdmobUtils.loadAndShowNativeCollap(
                this,
                AdmobManager.nativeHolder,
                binding.nativead,
                R.layout.ad_template_medium,
                AdNativeSize.MEDIUM,
                object :
                    AdmobUtils.NativeCallback {
                    override fun onNativeReady(ad: NativeAd?) {

                    }

                    override fun onNativeLoaded() {

                    }

                    override fun onNativeFailed(error: String) {

                    }

                    override fun onPaid(adValue: AdValue?, adUnitAds: String?) {

                    }

                    override fun onNativeClicked() {
                        val params: ViewGroup.LayoutParams = binding.nativead.layoutParams
                        params.height = AppLovinSdkUtils.dpToPx(this@HomeActivity, 100)
                        binding.nativead.layoutParams = params
                    }
                })
        }

        binding.loadReward.setOnClickListener {
            AdmobUtils.loadRewarded(
                this@HomeActivity,
                rewardHolder,
                object : AdmobUtils.RewardedInterCallback {
                    override fun onRewardFailed(error: String) {
                    }

                    override fun onRewardLoaded() {
                    }

                }
            )
        }

        binding.showReward.setOnClickListener {
//            AdmobUtils.showAdRewardWithCallback(this,rewardHolder,object :
//                RewardAdCallback {
//                override fun onAdClosed() {
//                    Log.d("==RewardAdCallback==", "onAdClosed: ")
//                }
//
//                override fun onAdShowed() {
//                    Log.d("==RewardAdCallback==", "onAdShowed: ")
//                }
//
//                override fun onAdFail(message: String?) {
//
//                }
//
//                override fun onEarned() {
//                    Log.d("==RewardAdCallback==", "onEarned: ")
//                }
//
//                override fun onPaid(adValue: AdValue?, adUnitAds: String?) {
//
//                }
//
//            })

            AdmobUtils.loadAndShowRewardedInter(this, "", object : AdmobUtils.RewardedCallback {
                override fun onRewardClosed() {

                }

                override fun onRewardShowed() {
                    Handler().postDelayed({
                        AdmobUtils.dismissAdDialog()
                    }, 200)
                }

                override fun onRewardFailed(error: String) {
                }

                override fun onRewardEarned() {
                }

            }, true)
        }
//        ApplovinUtil.showNativeWithLayout(nativeAds,this, AdsManager.nativeHolder,R.layout.native_custom_ad_view,
//            AdNativeSize.MEDIUM,object :
//            NativeCallBackNew {
//            override fun onNativeAdLoaded(nativeAd: MaxAd?, nativeAdView: MaxNativeAdView?) {
//                Toast.makeText(this@MainActivity,"show success", Toast.LENGTH_SHORT).show()
//            }
//
//            override fun onAdFail(error: String) {
//                Toast.makeText(this@MainActivity,"Show failed", Toast.LENGTH_SHORT).show()
//            }
//
//                override fun onAdRevenuePaid(ad: MaxAd) {
//
//                }
//            })

        AdmobUtils.loadNativeFullScreen(
            this, AdmobManager.nativeHolderFull, MediaAspectRatio.ANY,
            object : AdmobUtils.NativeCallback {
                override fun onNativeReady(ad: NativeAd?) {
                    Log.d("==full==", "Load onNativeAdLoaded: ")
                }

                override fun onNativeLoaded() {
                    Log.d("==full==", "Load onNativeAdLoaded: ")
                }

                override fun onNativeFailed(error: String) {
                    Log.d("==full==", "Load onAdFail: ")
                }

                override fun onPaid(adValue: AdValue?, adUnitAds: String?) {

                }

                override fun onNativeClicked() {

                }

            })
    }

}