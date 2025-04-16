package com.dino.sample.utils

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.dino.ads.admob.AdmobHolder
import com.dino.ads.admob.AdmobUtils
import com.dino.ads.admob.NativeIntroHolder
import com.dino.ads.utils.log
import com.dino.sample.R
import com.google.android.gms.ads.nativead.NativeAd

object AdsManager {

    fun loadAndShowBanner(activity: AppCompatActivity, holder: AdmobHolder, viewGroup: ViewGroup) {
        AdmobUtils.loadAndShowBanner(
            activity,
            holder,
            viewGroup,
            R.layout.ad_template_collap,
            object : AdmobUtils.BannerCallback() {},
            object : AdmobUtils.NativeCallback() {})
    }

    fun loadAndShowNative(activity: AppCompatActivity, holder: AdmobHolder, viewGroup: ViewGroup) {
        AdmobUtils.loadAndShowNativeDual(
            activity, holder, viewGroup, R.layout.ad_template_medium,
            object : AdmobUtils.NativeCallback() {})
    }

    fun loadAndShowNativeCollap(activity: AppCompatActivity, holder: AdmobHolder, viewGroup: ViewGroup) {
        AdmobUtils.loadAndShowNativeCollap(
            activity, holder, viewGroup, R.layout.ad_template_collap,
            object : AdmobUtils.NativeCallback() {})
    }

    fun loadNative(context: Context, holder: AdmobHolder) {
        AdmobUtils.loadNative(context, holder, object : AdmobUtils.NativeCallback() {})
    }

    fun loadNativeLanguage(context: Context, holder: NativeIntroHolder) {
        AdmobUtils.loadNativeLanguage(context, holder, object : AdmobUtils.NativeCallback() {
            override fun onNativeReady(ad: NativeAd?) {
                super.onNativeReady(ad)
                log("Native Language Ready: ${holder.holders.first().nativeAd}")
            }
        })
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

    fun showNativeLanguage(activity: Activity, holder: NativeIntroHolder, viewGroup: ViewGroup, position: Int) {
        val layout = if (position == 0) R.layout.ad_template_medium else R.layout.ad_template_medium_language
        AdmobUtils.showNativeLanguage(activity, holder, viewGroup, layout, position, object : AdmobUtils.NativeCallbackSimple() {})
    }

    fun showNativeFullScreen(activity: Activity, holder: AdmobHolder, viewGroup: ViewGroup) {
        AdmobUtils.showNativeFull(activity, holder, viewGroup, R.layout.ad_template_fullscreen, object : AdmobUtils.NativeCallbackSimple() {})
    }

    fun showNativeMedium(activity: AppCompatActivity, viewGroup: ViewGroup, holder: AdmobHolder) {
        AdmobUtils.showNative(
            activity, holder, viewGroup, R.layout.ad_template_medium,
            object : AdmobUtils.NativeCallbackSimple() {})
    }

}