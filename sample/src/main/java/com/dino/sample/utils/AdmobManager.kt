package com.dino.sample.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.dino.ads.AdmobUtils
import com.dino.ads.AdNativeSize
import com.dino.ads.utils.admod.InterAdmob
import com.dino.sample.R
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.nativead.NativeAd

object AdmobManager {


    interface AdListener {
        fun onAdClosed()
        fun onFailed()
    }
}
