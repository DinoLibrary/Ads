package com.dino.ads.applovin

import androidx.lifecycle.MutableLiveData
import com.applovin.mediation.ads.MaxInterstitialAd

class InterHolderApplovin(var adsId: String) {
    var inter: MaxInterstitialAd? = null
    val mutable: MutableLiveData<MaxInterstitialAd> = MutableLiveData()
    var check = false
}