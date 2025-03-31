package com.dino.ads.remote_config

import androidx.lifecycle.MutableLiveData
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd

class AdmobHolder {
    var uid: String = ""
    var enableLoadingDialog = true

    var isInterLoading = false
    val inter: MutableLiveData<InterstitialAd> = MutableLiveData(null)
    var interCount: Int = 0

    var isRewardLoading = false
    val rewardInter: MutableLiveData<RewardedInterstitialAd> = MutableLiveData(null)
    val reward: MutableLiveData<RewardedAd> = MutableLiveData(null)

    var anchor = "bottom"
    var bannerAdView: AdView? = null

    /**
     * enable loading dialog when showing Inter or RewardInter
     */
    fun enableLoading(b: Boolean): AdmobHolder {
        enableLoadingDialog = b
        return this
    }

    /**
     * s: "top" or "bottom"
     * default is "bottom"
     */
    fun anchor(s: String): AdmobHolder {
        anchor = s
        return this
    }
}