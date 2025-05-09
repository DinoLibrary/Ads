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
        private set
    var bannerAdView: AdView? = null
    var dividerColor: String = "#000000"
        private set

    //* Native
    var isNativeLoading = false
    var nativeSize = AdNativeSize.MEDIUM
        private set
    var nativeAd: MutableLiveData<NativeAd> = MutableLiveData()
    var mediaAspectRatio: Int = MediaAspectRatio.ANY
        private set
    var isNativeInter = false
    var tinyLoading = false
        private set

    open fun version(versionCode: Int): AdmobHolder {
        this.versionCode = versionCode
        return this
    }

    fun isRewardEnable() = RemoteUtils.getValue("reward_$uid") == "1" && AdmobUtils.isEnableAds

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
     * Use tiny loading layout for Native Small
     * (Same as banner loading layout)
     */
    fun tinyLoading(): AdmobHolder {
        tinyLoading = true
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