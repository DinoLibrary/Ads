package com.dino.sample.callback_applovin

import com.applovin.mediation.MaxAd

interface InterstitialCallback {
    fun onInterstitialReady()
    fun onInterstitialClosed()
    fun onInterstitialLoadFail(error:String)
    fun onInterstitialShowSucceed()
    fun onAdRevenuePaid(ad: MaxAd)
}