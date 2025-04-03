package com.dino.ads.admob

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.LinearLayout
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView
import com.dino.ads.R
import com.dino.ads.adjust.AdjustUtils
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AOAUtils(private val activity: Activity, val holder: AdmobHolder, val timeOut: Long, val callback: AoaCallback) {
    private var appOpenAd: AppOpenAd? = null
    var isShowingAd = true
    var isLoading = true
    var dialogFullScreen: Dialog? = null
    var isStart = true

    private var isLoadAndShow = true
    private val adRequest: AdRequest
        get() = AdRequest.Builder().build()

    private val isAdAvailable: Boolean
        get() = appOpenAd != null

    fun loadAndShowAoa() {
//        Log.d("===Load", "id1")
        if (!AdmobUtils.isEnableAds || !AdmobUtils.isNetworkConnected(activity)) {
            callback.onAdsFailed("isShowAds false")
            return
        }
        //Check timeout show inter
        val job = CoroutineScope(Dispatchers.Main).launch {
            delay(timeOut)
            if (isLoading && isStart) {
                isStart = false
                isLoading = false
                onAOADestroyed()
                callback.onAdsFailed("Time out")
                Log.d("====Timeout", "TimeOut")
            }
        }
        if (isAdAvailable) {
            job.cancel()
            callback.onAdsFailed("isAdAvailable true")
            return
        } else {
            Log.d("====Timeout", "fetching... ")
            val adId = if (AdmobUtils.isTesting) {
                activity.getString(R.string.test_admob_on_resume_id)
            } else {
                RemoteUtils.getAdId("AOA_${holder.uid}")
            }
            isShowingAd = false
            val request = adRequest
            AppOpenAd.load(activity, adId, request, object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdFailedToLoad(p0: LoadAdError) {
                    isLoading = false
                    super.onAdFailedToLoad(p0)
                    if (isStart) {
                        isStart = false
                        callback.onAdsFailed(p0.message)
                    }
                    job.cancel()
                    Log.d("====Timeout", "onAppOpenAdFailedToLoad: $p0")
                }

                override fun onAdLoaded(ad: AppOpenAd) {
                    super.onAdLoaded(ad)
                    appOpenAd = ad
                    callback.onAdsLoaded()
                    job.cancel()
                    Log.d("====Timeout", "isAdAvailable = true")
                    if (!OnResumeUtils.getInstance().isShowingAd && !isShowingAd && isLoadAndShow) {
                        showAOA()
                    }
                }
            })
        }
    }

    fun showAOA() {
        Log.d("====Timeout", "$isShowingAd - $isAdAvailable")
        if (!isShowingAd && isAdAvailable && isLoading) {
            isLoading = false
            if (OnResumeUtils.getInstance().isInitialized) {
                OnResumeUtils.getInstance().isAppResumeEnabled = false
            }
            Log.d("====Timeout", "will show ad ")
            val fullScreenContentCallback: FullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    try {
                        dialogFullScreen?.dismiss()
                    } catch (ignored: Exception) {
                    }
                    appOpenAd = null
                    isShowingAd = true
                    Log.d("====Timeout", "Dismiss... ")
                    if (isStart) {
                        isStart = false
                        callback.onAdsClose()
                    }
                    if (OnResumeUtils.getInstance().isInitialized) {
                        OnResumeUtils.getInstance().isAppResumeEnabled = true
                    }
                }

                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    try {
                        dialogFullScreen?.dismiss()
                    } catch (ignored: Exception) {
                    }
                    isShowingAd = true
                    if (isStart) {
                        isStart = false
                        callback.onAdsFailed(p0.message)
                        Log.d("====Timeout", "Failed... $p0")
                    }
                    if (OnResumeUtils.getInstance().isInitialized) {
                        OnResumeUtils.getInstance().isAppResumeEnabled = true
                    }
                }

                override fun onAdShowedFullScreenContent() {
                    isShowingAd = true
                }
            }
            appOpenAd?.run {
                this.fullScreenContentCallback = fullScreenContentCallback
                dialogFullScreen = Dialog(activity)
                dialogFullScreen?.requestWindowFeature(Window.FEATURE_NO_TITLE)
                dialogFullScreen?.setContentView(R.layout.dialog_full_screen)
                dialogFullScreen?.setCancelable(false)
                dialogFullScreen?.window?.setBackgroundDrawable(ColorDrawable(Color.WHITE))
                dialogFullScreen?.window?.setLayout(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                val img = dialogFullScreen?.findViewById<LottieAnimationView>(R.id.imageView3)
                img?.setAnimation(R.raw.gifloading)
                try {
                    if (!activity.isFinishing && dialogFullScreen != null && dialogFullScreen?.isShowing == false) {
                        dialogFullScreen?.show()
                    }
                } catch (ignored: Exception) {
                }
                Handler(Looper.getMainLooper()).postDelayed({
                    if (!OnResumeUtils.getInstance().isShowingAd && !isShowingAd) {
                        Log.d("===AOA", "Show")
                        try {
                            val txt = dialogFullScreen?.findViewById<TextView>(R.id.txtLoading)
                            img?.visibility = View.INVISIBLE
                            txt?.visibility = View.INVISIBLE
                        } catch (ignored: Exception) {
                        }
                        setOnPaidEventListener {
                            AdjustUtils.postRevenueAdjust(it, adUnitId)
                        }
                        show(activity)
                    } else {
                        callback.onAdsFailed("AOA can't show")
                    }
                }, 800)
            }
        } else {
            callback.onAdsFailed("AOA can't show in background!")
        }
    }

    fun onAOADestroyed() {
        isShowingAd = true
        isLoading = false
        try {
            if (!activity.isFinishing && dialogFullScreen != null && dialogFullScreen?.isShowing == true) {
                dialogFullScreen?.dismiss()
            }
            appOpenAd?.fullScreenContentCallback?.onAdDismissedFullScreenContent()
        } catch (ignored: Exception) {
        }
    }

    fun setLoadAndShow(loadAndShow: Boolean) {
        isLoadAndShow = loadAndShow
    }

    interface AoaCallback {
        fun onAdsClose()
        fun onAdsLoaded()
        fun onAdsFailed(message: String)
    }

}