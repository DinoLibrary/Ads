package com.dino.ads.remote_config

import androidx.lifecycle.MutableLiveData
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd

class AdmobHolder {
    var inter: InterstitialAd? = null
    var rewardInter: RewardedInterstitialAd? = null
    val mutable: MutableLiveData<InterstitialAd> = MutableLiveData()
    val mutableRewardInter: MutableLiveData<RewardedInterstitialAd> = MutableLiveData()
    var check = false
    var enableLoadingDialog = true
    var uid: String = ""
    var interCount: Int = 0
}