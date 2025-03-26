package com.dino.sample.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.applovin.mediation.MaxAd
import com.applovin.mediation.ads.MaxInterstitialAd
import com.applovin.mediation.nativeAds.MaxNativeAdLoader
import com.applovin.mediation.nativeAds.MaxNativeAdView
import com.dino.ads.AdNativeSize
import com.dino.ads.ApplovinUtils
import com.dino.ads.callback_applovin.InterstitialCallback
import com.dino.ads.callback_applovin.NativeCallback
import com.dino.ads.utils.InterHolderApplovin


object ApplovinManager {
    var interHolder = InterHolderApplovin("134656413e36e374")
    var nativeHolder = com.dino.ads.utils.NativeHolderApplovin("0f688c4e22b9688b")
    var banner = "f443c90308f39f17"

    fun showAdsNative(
        activity: Activity,
        nativeHolder: com.dino.ads.utils.NativeHolderApplovin,
        viewGroup: ViewGroup
    ) {
        ApplovinUtils.loadAndShowNative(activity, nativeHolder, viewGroup,
            AdNativeSize.MEDIUM, object :
                NativeCallback {
                override fun onNativeAdLoaded(nativeAd: MaxAd?, nativeAdView: MaxNativeAdView?) {
                    Toast.makeText(activity, "Loaded", Toast.LENGTH_SHORT).show()
                }

                override fun onAdFail(error: String) {
                    Toast.makeText(activity, "LoadFailed", Toast.LENGTH_SHORT).show()
                }

                override fun onAdRevenuePaid(ad: MaxAd) {

                }
            })
    }


    fun loadInter(context: Context) {
        ApplovinUtils.loadInterstitial(context, interHolder, object :
            com.dino.ads.callback_applovin.InterstitialCallbackNew {
            override fun onInterstitialReady(interstitialAd: MaxInterstitialAd) {
//                Toast.makeText(context,"Loaded",Toast.LENGTH_SHORT).show()
            }

            override fun onInterstitialClosed() {

            }

            override fun onInterstitialLoadFail(error: String) {
//                Toast.makeText(context,"LoadFailed",Toast.LENGTH_SHORT).show()
            }

            override fun onInterstitialShowSucceed() {

            }

            override fun onAdRevenuePaid(ad: MaxAd?) {

            }
        })
    }

    fun showInter(context: AppCompatActivity, interHolder: InterHolderApplovin, adsOnClick: AdsOnClick) {
        ApplovinUtils.showInterstitial(context, 800, interHolder, object :
            com.dino.ads.callback_applovin.InterstitialCallbackNew {
            override fun onInterstitialReady(interstitialAd: MaxInterstitialAd) {
                Toast.makeText(context, "Ready", Toast.LENGTH_SHORT).show()
            }

            override fun onInterstitialClosed() {
                loadInter(context)
                Toast.makeText(context, "Closed", Toast.LENGTH_SHORT).show()
                adsOnClick.onAdsCloseOrFailed()
            }


            override fun onInterstitialLoadFail(error: String) {
                loadInter(context)
                adsOnClick.onAdsCloseOrFailed()
                Toast.makeText(context, "Failed: $error", Toast.LENGTH_SHORT).show()
            }

            override fun onInterstitialShowSucceed() {
                Toast.makeText(context, "Show", Toast.LENGTH_SHORT).show()
            }

            override fun onAdRevenuePaid(ad: MaxAd?) {

            }
        })
    }

    interface AdsOnClick {
        fun onAdsCloseOrFailed()
    }

    var nativeAdLoader: MaxNativeAdLoader? = null
    var native: MaxAd? = null
    var isLoad = false
    var native_mutable: MutableLiveData<MaxAd> = MutableLiveData()

    fun loadAndShowIntersial(activity: Activity, idAd: InterHolderApplovin, adsOnClick: AdsOnClick) {
        ApplovinUtils.loadAndShowInterstitial(activity as AppCompatActivity, idAd, object :
            InterstitialCallback {
            override fun onInterstitialReady() {
                com.dino.ads.AppOpenUtils.getInstance().isAppResumeEnabled = false
            }

            override fun onInterstitialClosed() {
                adsOnClick.onAdsCloseOrFailed()
            }

            override fun onInterstitialLoadFail(error: String) {
                Log.d("===Ads", error)
                adsOnClick.onAdsCloseOrFailed()
            }

            override fun onInterstitialShowSucceed() {
                com.dino.ads.AppOpenUtils.getInstance().isAppResumeEnabled = false
            }

            override fun onAdRevenuePaid(ad: MaxAd) {

            }

        })

    }
}
