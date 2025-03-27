package com.dino.sample.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.dino.ads.AdmobUtils
import com.dino.ads.AdNativeSize
import com.dino.ads.utils.admod.InterAdmob
import com.dino.sample.R
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.nativead.NativeAd

object AdmobManager {
    var nativeHolder =
        com.dino.ads.utils.admod.NativeAdmob("ca-app-pub-3940256099942544/2247696110")
    var nativeHolderFull =
        com.dino.ads.utils.admod.NativeAdmob("ca-app-pub-3940256099942544/7342230711")
    var interholder = InterAdmob("ca-app-pub-3940256099942544/1033173712")

    fun loadInter(context: Context, interHolder: InterAdmob) {
        AdmobUtils.loadInterstitial(context, interHolder,
            object :
                AdmobUtils.LoadInterCallback {

                override fun onInterLoaded(interstitialAd: InterstitialAd?, isLoading: Boolean) {
                    interholder.inter = interstitialAd
                    interHolder.check = isLoading
                    com.dino.ads.utils.Utils.getInstance().showMessenger(context, "onAdLoaded")
                }

                override fun onInterFailed(error: String) {
                    com.dino.ads.utils.Utils.getInstance().showMessenger(context, "onAdFail")
                }

                override fun onPaid(adValue: AdValue?, adUnitAds: String?) {

                }
            }
        )
    }


    fun showInter(
        context: Context,
        interHolder: InterAdmob,
        adListener: AdListener,
        enableLoadingDialog: Boolean
    ) {
        AdmobUtils.showInterstitial(
            context as Activity, interHolder, 10000, object :
                AdmobUtils.InterCallback {
                override fun onInterLoaded() {
                    com.dino.ads.utils.Utils.getInstance().showMessenger(context, "onAdLoaded")
                }

                override fun onStartAction() {
                    adListener.onAdClosed()
                }

                override fun onInterFailed(error: String) {
                    interHolder.inter = null
                    loadInter(context, interHolder)
                    adListener.onFailed()
                    com.dino.ads.utils.Utils.getInstance().showMessenger(context, "onAdFail")
                }

                override fun onPaid(adValue: AdValue?, adUnitAds: String?) {

                }

                override fun onDismissedInter() {
                    interHolder.inter = null
                    loadInter(context, interHolder)
//                    adListener.onAdClosed()
                    com.dino.ads.utils.Utils.getInstance().showMessenger(context, "onEventClickAdClosed")
                }

                override fun onInterShowed() {
                    com.dino.ads.utils.Utils.getInstance().showMessenger(context, "onAdShowed")
                }
            }, enableLoadingDialog
        )
    }

    fun loadAdsNativeNew(context: Context, holder: com.dino.ads.utils.admod.NativeAdmob) {
        AdmobUtils.loadNative(
            context,
            holder,
            object : AdmobUtils.NativeCallback {
                override fun onNativeReady(ad: NativeAd?) {
                }

                override fun onNativeLoaded() {
                }

                override fun onNativeFailed(error: String) {
                }

                override fun onPaid(adValue: AdValue?, adUnitAds: String?) {

                }

                override fun onNativeClicked() {
                }
            })
    }

    fun showNative(activity: Activity, viewGroup: ViewGroup, holder: com.dino.ads.utils.admod.NativeAdmob) {
        if (!AdmobUtils.isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
        AdmobUtils.showNative(
            activity,
            holder,
            viewGroup,
            R.layout.ad_unified_medium,
            AdNativeSize.MEDIUM,
            object : AdmobUtils.NativeCallbackSimple {
                override fun onNativeLoaded() {
                    com.dino.ads.utils.Utils.getInstance().showMessenger(activity, "onNativeShow")
                }

                override fun onNativeFailed(error: String) {
                    com.dino.ads.utils.Utils.getInstance().showMessenger(activity, "onAdsFailed")
                }

                override fun onPaid(adValue: AdValue?, adUnitAds: String?) {

                }
            })
    }

    fun showAdsNativeFullScreen(
        activity: Activity,
        nativeHolder: com.dino.ads.utils.admod.NativeAdmob,
        viewGroup: ViewGroup
    ) {
        AdmobUtils.showNativeFullScreen(activity, nativeHolder, viewGroup,
            R.layout.ad_native_fullscreen, object :
                AdmobUtils.NativeCallbackSimple {
                override fun onNativeLoaded() {
                    Log.d("==full==", "NativeLoaded: ")
                }

                override fun onNativeFailed(error: String) {
                    Log.d("==full==", "NativeFailed: $error")
                }

                override fun onPaid(adValue: AdValue?, adUnitAds: String?) {

                }

            })
    }


    interface AdListener {
        fun onAdClosed()
        fun onFailed()
    }
}
