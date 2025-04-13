package com.dino.ads.admob

import androidx.lifecycle.MutableLiveData
import com.dino.ads.utils.AdNativeSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MediaAspectRatio
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd

open class AdmobHolder(var uid: String = "") {
    var enableLoadingDialog = true
    var versionCode: Int? = null

    //* Inter
    var isInterLoading = false
    val inter: MutableLiveData<InterstitialAd> = MutableLiveData()
    var interCount: Int = 0

    //* Reward
    var isRewardLoading = false
    val rewardInter: MutableLiveData<RewardedInterstitialAd> = MutableLiveData()
    val reward: MutableLiveData<RewardedAd> = MutableLiveData()

    //* Banner
    var anchor = "bottom"
    var bannerAdView: AdView? = null
    var dividerColor: String = "#000000"

    //* Native
    var isNativeLoading = false
    var nativeSize = AdNativeSize.MEDIUM
    var nativeAd: MutableLiveData<NativeAd> = MutableLiveData()
    var mediaAspectRatio: Int = MediaAspectRatio.SQUARE
    var isNativeInter = false

    fun version(versionCode: Int): AdmobHolder {
        this.versionCode = versionCode
        return this
    }

    fun isRewardEnable() = RemoteUtils.getValue("reward_$uid") == "1" && AdmobUtils.isEnableAds && !AdmobUtils.isPremium

    /**
     * Banner divider color
     */
    open fun dividerColor(color: String): AdmobHolder {
        dividerColor = color
        return this
    }

    /**
     * Native is ready when it's loaded successfully
     */
    open fun isNativeReady() = nativeAd.value != null

    /**
     * enable loading dialog when showing Inter or RewardInter
     */
    fun enableLoading(b: Boolean): AdmobHolder {
        enableLoadingDialog = b
        return this
    }

    /**
     * Only for banner on top of the screen
     * default is bottom
     */
    fun anchorTop(): AdmobHolder {
        anchor = "top"
        return this
    }

    fun nativeSmall(): AdmobHolder {
        nativeSize = AdNativeSize.SMALL
        return this
    }

    /**
     * MediaAspectRatio.SQUARE,
     * MediaAspectRatio.LANDSCAPE,
     * MediaAspectRatio.PORTRAIT,
     * MediaAspectRatio.UNKNOWN.
     * MediaAspectRatio.ANY
     */
    fun mediaAspectRatio(ratio: Int): AdmobHolder {
        mediaAspectRatio = ratio
        return this
    }

}