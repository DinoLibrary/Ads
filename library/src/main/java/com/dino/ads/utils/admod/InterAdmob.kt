package com.dino.ads.utils.admod

import androidx.lifecycle.MutableLiveData
import com.google.android.gms.ads.interstitial.InterstitialAd

class InterAdmob(var ads: String) {
    var inter: InterstitialAd? = null
    val mutable: MutableLiveData<InterstitialAd> = MutableLiveData()
    var check = false
    var remoteValue: String = ""
    var count: Int = 0
}