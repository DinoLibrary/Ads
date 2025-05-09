package com.dino.ads.admob

import androidx.lifecycle.MutableLiveData
import com.dino.ads.utils.AdNativeSize
import com.dino.ads.utils.LoadingSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MediaAspectRatio
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd

open class AdmobHolder(var uid: String = "") {
    internal var enableLoadingDialog = true
    internal var versionCode: Int? = null

    //* Inter
    internal var isInterLoading = false
    internal val inter: MutableLiveData<InterstitialAd> = MutableLiveData()
    internal var interCount: Int = 0

    //* Reward
    internal var isRewardLoading = false
    internal val rewardInter: MutableLiveData<RewardedInterstitialAd> = MutableLiveData()
    internal val reward: MutableLiveData<RewardedAd> = MutableLiveData()

    //* Banner
    internal var anchor = "bottom"
    internal var bannerAdView: AdView? = null
    internal var dividerColor: String = "#000000"

    //* Native
    internal var isNativeLoading = false
    internal var nativeSize = AdNativeSize.MEDIUM
    internal var nativeAd: MutableLiveData<NativeAd> = MutableLiveData()
    internal var mediaAspectRatio: Int = MediaAspectRatio.ANY
    internal var isNativeInter = false
    internal var loadingSize: LoadingSize? = null

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
    fun loadingSize(size: LoadingSize): AdmobHolder {
        loadingSize = size
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