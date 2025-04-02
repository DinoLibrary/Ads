package com.dino.ads.applovin

import com.applovin.mediation.MaxAd
import com.applovin.mediation.ads.MaxInterstitialAd

interface InterstitialCallbackNew {
    fun onInterstitialReady(interstitialAd : MaxInterstitialAd)
    fun onInterstitialClosed()
    fun onInterstitialLoadFail(error:String)
    fun onInterstitialShowSucceed()
    fun onAdRevenuePaid(ad: MaxAd?)
}