package com.dino.ads.utils.admod

import androidx.lifecycle.MutableLiveData
import com.google.android.gms.ads.rewarded.RewardedAd

open class RewardAdmob(var ads: String) {
    var inter: RewardedAd? = null
    val mutable: MutableLiveData<RewardedAd> = MutableLiveData(null)
    var isLoading = false
}