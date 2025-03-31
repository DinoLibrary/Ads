package com.dino.ads.remote_config

import androidx.lifecycle.MutableLiveData
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd

class AdmobHolder {
    var inter: InterstitialAd? = null
    val mutable: MutableLiveData<InterstitialAd> = MutableLiveData()
    val rewardInter: MutableLiveData<RewardedInterstitialAd> = MutableLiveData(null)
    val reward: MutableLiveData<RewardedAd> = MutableLiveData(null)
    var check = false
    var enableLoadingDialog = true
    var uid: String = ""
    var interCount: Int = 0
    var isLoading = false
}