package com.dino.sample.utils

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.dino.ads.AdmobUtils
import com.dino.ads.remote_config.AdmobHolder
import com.dino.ads.remote_config.NativeIntroHolder
import com.dino.ads.utils.log
import com.dino.sample.R
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.nativead.NativeAd

object AdsManager {
    var showRate = 0

    fun loadAndShowBanner(activity: AppCompatActivity, holder: AdmobHolder, viewGroup: ViewGroup) {
        AdmobUtils.loadAndShowBanner(
            activity, holder, viewGroup, object : AdmobUtils.BannerCallback {
                override fun onBannerClicked() {
                }

                override fun onBannerLoaded(adSize: AdSize) {
                }

                override fun onBannerFailed(error: String) {
                }

            })
    }

    fun loadAndShowBannerNative(activity: AppCompatActivity, holder: AdmobHolder, viewGroup: ViewGroup) {
        AdmobUtils.loadAndShowBannerNative(
            activity, holder, viewGroup, R.layout.ad_template_medium, object : AdmobUtils.BannerCallback {
                override fun onBannerClicked() {
                }

                override fun onBannerLoaded(adSize: AdSize) {
                }

                override fun onBannerFailed(error: String) {
                }

            }, object : AdmobUtils.NativeCallback{
                override fun onNativeReady(ad: NativeAd?) {
                }

                override fun onNativeFailed(error: String) {
                }

                override fun onNativeClicked() {
                }
            })
    }

    fun loadAndShowNative(activity: AppCompatActivity, holder: AdmobHolder, viewGroup: ViewGroup) {
        AdmobUtils.loadAndShowNativeDual(
            activity, holder, viewGroup, R.layout.ad_template_medium,
            object : AdmobUtils.NativeCallback {
                override fun onNativeClicked() {
                }

                override fun onNativeReady(ad: NativeAd?) {
                }

                override fun onNativeFailed(error: String) {
                }
            })
    }

    fun loadAndShowNativeCollap(activity: AppCompatActivity, holder: AdmobHolder, viewGroup: ViewGroup) {
        AdmobUtils.loadAndShowNativeCollap(
            activity, holder, viewGroup, R.layout.ad_template_medium,
            object : AdmobUtils.NativeCallback {
                override fun onNativeClicked() {
                }

                override fun onNativeReady(ad: NativeAd?) {
                }

                override fun onNativeFailed(error: String) {
                }
            })
    }

    fun loadNative(context: Context, holder: AdmobHolder) {
        AdmobUtils.loadNative(context, holder, object : AdmobUtils.NativeCallback {
            override fun onNativeClicked() {
            }

            override fun onNativeFailed(error: String) {
                log("Native failed: $error")
            }

            override fun onNativeReady(ad: NativeAd?) {
            }

        })
    }

    fun loadNativeIntro(context: Context, holder: NativeIntroHolder) {
        AdmobUtils.loadNativeIntro(context, holder, object : AdmobUtils.NativeCallback {
            override fun onNativeClicked() {
            }

            override fun onNativeFailed(error: String) {
                log("Native failed: $error")
            }

            override fun onNativeReady(ad: NativeAd?) {
                log("Native ready: ${holder.uid}")
            }
        })
    }

    fun loadNativeFullscreen(context: Context, holder: AdmobHolder) {
        AdmobUtils.loadNativeFullScreen(context, holder, object : AdmobUtils.NativeCallback {
            override fun onNativeClicked() {
            }

            override fun onNativeFailed(error: String) {
                log("Native failed: $error")
            }

            override fun onNativeReady(ad: NativeAd?) {
                log("Native Fullscreen ready: ${holder.uid}")
            }

        })
    }

    fun showNativeIntro(activity: Activity, holder: NativeIntroHolder, viewGroup: ViewGroup, position: Int) {
        AdmobUtils.showNativeIntro(
            activity, holder, viewGroup, R.layout.ad_template_medium, position, object : AdmobUtils.NativeCallbackSimple {
                override fun onNativeLoaded() {

                }

                override fun onNativeFailed(error: String) {
                }

            })
    }

    fun showNativeFullScreen(activity: Activity, holder: AdmobHolder, viewGroup: ViewGroup) {
        AdmobUtils.showNativeFullScreen(
            activity, holder, viewGroup, R.layout.ad_template_fullscreen, object : AdmobUtils.NativeCallbackSimple {
                override fun onNativeLoaded() {

                }

                override fun onNativeFailed(error: String) {
                }

            })
    }

    fun showNativeMedium(activity: AppCompatActivity, viewGroup: ViewGroup, holder: AdmobHolder) {
        AdmobUtils.showNative(
            activity, holder, viewGroup, R.layout.ad_template_medium,
            object : AdmobUtils.NativeCallbackSimple {

                override fun onNativeLoaded() {
                    log("showNativeMedium Native loaded")
                }

                override fun onNativeFailed(error: String) {
                    log("showNativeMedium Native failed: $error")
                }
            })
    }

    fun loadAndShowInter(activity: AppCompatActivity, holder: AdmobHolder, onFinished: () -> Unit) {
        AdmobUtils.loadAndShowInterstitial(
            activity, holder, object : AdmobUtils.InterCallback {
                override fun onStartAction() {
                }

                override fun onDismissedInter() {
                    onFinished()
                }

                override fun onInterShowed() {
                }

                override fun onInterLoaded() {
                }

                override fun onInterFailed(error: String) {
                    onFinished()
                }

            })
    }

}