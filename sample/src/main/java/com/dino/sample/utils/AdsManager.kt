package com.dino.sample.utils

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.dino.ads.admob.AdmobHolder
import com.dino.ads.admob.AdmobUtils
import com.dino.ads.admob.NativeIntroHolder
import com.dino.sample.R

object AdsManager {

    fun loadAndShowBanner(activity: AppCompatActivity, holder: AdmobHolder, viewGroup: ViewGroup) {
        AdmobUtils.loadAndShowBanner(
            activity, holder, viewGroup, object : AdmobUtils.BannerCallback() {})
    }

    fun loadAndShowBannerNative(activity: AppCompatActivity, holder: AdmobHolder, viewGroup: ViewGroup) {
        AdmobUtils.loadAndShowBanner(
            activity, holder, viewGroup, R.layout.ad_template_medium, object : AdmobUtils.BannerCallback() {}, object : AdmobUtils.NativeCallback() {})
    }

    fun loadAndShowNative(activity: AppCompatActivity, holder: AdmobHolder, viewGroup: ViewGroup) {
        AdmobUtils.loadAndShowNativeDual(
            activity, holder, viewGroup, R.layout.ad_template_medium,
            object : AdmobUtils.NativeCallback() {})
    }

    fun loadAndShowNativeCollap(activity: AppCompatActivity, holder: AdmobHolder, viewGroup: ViewGroup) {
        AdmobUtils.loadAndShowNativeCollap(
            activity, holder, viewGroup, R.layout.ad_template_medium,
            object : AdmobUtils.NativeCallback() {})
    }

    fun loadNative(context: Context, holder: AdmobHolder) {
        AdmobUtils.loadNative(context, holder, object : AdmobUtils.NativeCallback() {})
    }

    fun loadNativeIntro(context: Context, holder: NativeIntroHolder) {
        AdmobUtils.loadNativeIntro(context, holder, object : AdmobUtils.NativeCallback() {})
    }

    fun loadNativeFullscreen(context: Context, holder: AdmobHolder) {
        AdmobUtils.loadNativeFull(context, holder, object : AdmobUtils.NativeCallback() {})
    }

    fun showNativeIntro(activity: Activity, holder: NativeIntroHolder, viewGroup: ViewGroup, position: Int) {
        AdmobUtils.showNativeIntro(
            activity, holder, viewGroup, R.layout.ad_template_medium, position, object : AdmobUtils.NativeCallbackSimple() {})
    }

    fun showNativeFullScreen(activity: Activity, holder: AdmobHolder, viewGroup: ViewGroup) {
        AdmobUtils.showNativeFull(
            activity, holder, viewGroup, R.layout.ad_template_fullscreen, object : AdmobUtils.NativeCallbackSimple() {})
    }

    fun showNativeMedium(activity: AppCompatActivity, viewGroup: ViewGroup, holder: AdmobHolder) {
        AdmobUtils.showNative(
            activity, holder, viewGroup, R.layout.ad_template_medium,
            object : AdmobUtils.NativeCallbackSimple() {})
    }

    fun loadAndShowInter(activity: AppCompatActivity, holder: AdmobHolder, onFinished: () -> Unit) {
        AdmobUtils.loadAndShowInterstitial(
            activity, holder, object : AdmobUtils.InterCallback() {
                override fun onInterClosed() {
                    onFinished()
                }

                override fun onInterFailed(error: String) {
                    onFinished()
                }
            })
    }

}