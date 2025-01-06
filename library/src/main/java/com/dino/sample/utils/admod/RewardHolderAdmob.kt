package com.dino.sample.utils.admod

import androidx.lifecycle.MutableLiveData
import com.google.android.gms.ads.rewarded.RewardedAd

open class RewardHolderAdmob(var ads: String) {
    var inter: RewardedAd? = null
    val mutable: MutableLiveData<RewardedAd> = MutableLiveData(null)
    var isLoading = false
}