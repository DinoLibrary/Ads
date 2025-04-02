package com.dino.ads.remote_config

import androidx.lifecycle.MutableLiveData
import com.dino.ads.AdNativeSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MediaAspectRatio
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd

open class AdmobHolder(val uid: String = "") {
    var enableLoadingDialog = true

    var isInterLoading = false
    val inter: MutableLiveData<InterstitialAd> = MutableLiveData()
    var interCount: Int = 0

    var isRewardLoading = false
    val rewardInter: MutableLiveData<RewardedInterstitialAd> = MutableLiveData()
    val reward: MutableLiveData<RewardedAd> = MutableLiveData()

    var anchor = "bottom"
    var bannerAdView: AdView? = null

    var isNativeLoading = false
    var nativeSize = AdNativeSize.MEDIUM
    var nativeAd: MutableLiveData<NativeAd> = MutableLiveData()
    var mediaAspectRatio: Int = MediaAspectRatio.SQUARE

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
     * default is bottom collap banner
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