package com.dino.ads.admob

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.Window
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import com.dino.ads.R
import com.dino.ads.adjust.AdjustUtils
import com.dino.ads.admob.NativeHelper.Companion.populateNativeAdView
import com.dino.ads.admob.NativeHelper.Companion.populateNativeAdViewCollap
import com.dino.ads.admob.NativeHelper.Companion.populateNativeAdViewFull
import com.dino.ads.admob.RemoteUtils.logId
import com.dino.ads.cmp.GoogleMobileAdsConsentManager
import com.dino.ads.utils.AdNativeSize
import com.dino.ads.utils.Utils
import com.dino.ads.utils.dpToPx
import com.dino.ads.utils.gone
import com.dino.ads.utils.invisible
import com.dino.ads.utils.log
import com.dino.ads.utils.logE
import com.dino.ads.utils.visible
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date

object AdmobUtils {
    private var dialogFullScreen: Dialog? = null
    var lastTimeShowInterstitial: Long = 0
    var timeOut = 10000
    @JvmField
    var isAdShowing = false
    @JvmField
    var isEnableAds = false
    @JvmField
    var isTesting = true
    private var isInitialized = false
    private var isConsented = false
    private var testDevices: MutableList<String> = ArrayList()
    @JvmField
    var mBannerCollapView: AdView? = null
    var mRewardedAd: RewardedAd? = null
    var mRewardedInterstitialAd: RewardedInterstitialAd? = null
    var mInterstitialAd: InterstitialAd? = null
    var shimmerFrameLayout: ShimmerFrameLayout? = null
    private var adRequest: AdRequest? = null

    fun setupCMP(activity: Activity, onCompleted: () -> Unit) {
        if (isConsented) return
        isConsented = true
        val googleMobileAdsConsentManager = GoogleMobileAdsConsentManager(activity)
        googleMobileAdsConsentManager.gatherConsent { error ->
            if (error != null || googleMobileAdsConsentManager.canRequestAds) {
                onCompleted()
            }
        }
    }

    @JvmStatic
    fun initAdmob(context: Context, isDebug: Boolean) {
        if (isInitialized) return
        isInitialized = true
        isTesting = isDebug
        isEnableAds = RemoteUtils.enableAds()
        MobileAds.initialize(context) { }
        initListIdTest()
        val requestConfiguration = RequestConfiguration.Builder().setTestDeviceIds(testDevices).build()
        MobileAds.setRequestConfiguration(requestConfiguration)
        initAdRequest(timeOut)
    }

    @JvmStatic
    private fun initAdRequest(timeOut: Int) {
        adRequest = AdRequest.Builder().setHttpTimeoutMillis(timeOut).build()
    }

    private fun initListIdTest() {
        testDevices.add("D4A597237D12FDEC52BE6B2F15508BB")
    }

    @JvmStatic
    fun isNetworkConnected(context: Context): Boolean {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            return cm.activeNetworkInfo != null && cm.activeNetworkInfo!!.isConnected
        } catch (e: Exception) {
            return false
        }
    }

    @JvmStatic
    fun loadAndShowAdSplash(activity: AppCompatActivity, holder: AdmobHolder, callback: InterCallback) {
        val remoteValue = RemoteUtils.getValue("ads_${holder.uid}", holder.versionCode)
        if (!isEnableAds || !isNetworkConnected(activity)) {
            callback.onInterFailed("Not show ads ${holder.uid}: remoteValue = 0")
        }
        when (remoteValue) {
            "0" -> {
                callback.onInterFailed("Not show ads ${holder.uid}: remoteValue = 0")
            }

            "1" -> {
                AOAUtils(activity, holder, 20000, object : AOAUtils.AoaCallback {
                    override fun onAdsClose() {
                        callback.onInterClosed()
                    }

                    override fun onAdsFailed(message: String) {
                        callback.onInterFailed(message)
                    }

                    override fun onAdsLoaded() {
                        callback.onInterLoaded()
                    }

                }).loadAndShowAoa()
            }

            "2" -> {
                performLoadAndShowInterstitial(activity, holder, callback)
            }
        }
    }

    @JvmStatic
    @Deprecated("This function only allow Banner & Banner Collap")
    fun loadAndShowBanner(activity: Activity, holder: AdmobHolder, viewGroup: ViewGroup, callback: BannerCallback) {
        when (val remoteValue = RemoteUtils.getValue("banner_${holder.uid}", holder.versionCode)) {
            "1" -> {
                performLoadAndShowBanner(activity, holder, viewGroup, callback)
            }

            "2" -> {
                performLoadAndShowBannerCollap(activity, holder, viewGroup, callback)
            }

            else -> {
                viewGroup.gone()
                logE("AdmobErr: banner_${holder.uid} = $remoteValue")
                callback.onBannerFailed("Not show banner")
            }
        }
    }

    @JvmStatic
    fun loadAndShowBanner(
        activity: Activity,
        holder: AdmobHolder,
        viewGroup: ViewGroup,
        layoutCollap: Int,
        callback: BannerCallback,
        callBack: NativeCallback
    ) {
        loadAndShowBanner(activity, holder, viewGroup, layoutCollap, null, callback, callBack)
    }

    @JvmStatic
    fun loadAndShowBanner(
        activity: Activity,
        holder: AdmobHolder,
        viewGroup: ViewGroup,
        layoutCollap: Int,
        layoutBanner: Int?,
        callback: BannerCallback,
        callBack: NativeCallback
    ) {
        when (val remoteValue = RemoteUtils.getValue("banner_${holder.uid}", holder.versionCode)) {
            "1" -> { //* Banner
                performLoadAndShowBanner(activity, holder, viewGroup, callback)
            }

            "2" -> {//* Banner Collap
                performLoadAndShowBannerCollap(activity, holder, viewGroup, callback)
            }

            "3" -> {//* Native Small
                if (layoutBanner == null) {
                    performLoadAndShowNative(activity, viewGroup, holder.nativeSmall().tinyLoading(), layoutCollap, callBack, true)
                } else {
                    performLoadAndShowNative(activity, viewGroup, holder.nativeSmall().tinyLoading(), layoutBanner, callBack)
                }
            }

            "4" -> {//* Native Collap
                performLoadAndShowNativeCollap(activity, holder, viewGroup, layoutCollap, layoutBanner, callBack)
            }

            else -> {
                viewGroup.gone()
                logE("AdmobErr: banner_${holder.uid} = $remoteValue")
                callback.onBannerFailed("Not show banner")
            }
        }
    }

    @JvmStatic
    fun loadInterstitial(context: Context, holder: AdmobHolder, callback: LoadInterCallback) {
        val remoteValue = RemoteUtils.getValue("inter_${holder.uid}", holder.versionCode)
        if (remoteValue == "0") {
            callback.onInterFailed("Not show inter")
        } else {
            performLoadInterstitial(context, holder, callback)
        }
    }

    @JvmStatic
    fun showInterstitial(activity: AppCompatActivity, holder: AdmobHolder, callback: InterCallback) {
        destroyBannerCollapView()
        isAdShowing = false
        val remoteValue = RemoteUtils.getValue("inter_${holder.uid}", holder.versionCode)
        if (remoteValue == "0") {
            if (OnResumeUtils.getInstance().isInitialized) {
                OnResumeUtils.getInstance().isOnResumeEnable = true
            }
            callback.onInterFailed("Not show inter")
        } else {
            holder.interCount++
            if (holder.interCount % remoteValue.toInt() != 0) {
                callback.onInterFailed("Not show Inter: count=${holder.interCount}")
            } else {
                performShowInterstitial(activity, holder, callback)
            }
        }
    }

    @JvmStatic
    fun loadAndShowReward(activity: AppCompatActivity, holder: AdmobHolder, callback: RewardCallback) {
        mRewardedAd = null
        isAdShowing = false
        val remoteValue = RemoteUtils.getValue("reward_${holder.uid}", holder.versionCode)
        if (remoteValue == "0" || !isEnableAds) {
            callback.onRewardEarned()
            callback.onRewardClosed()
        } else {
            performLoadAndShowReward(activity, holder, callback)
        }
    }

//    @JvmStatic
//    fun loadAndShowRewardInter(activity: AppCompatActivity, holder: AdmobHolder, callback: RewardCallback) {
//        mRewardedInterstitialAd = null
//        isAdShowing = false
//        val remoteValue = RemoteUtils.getValue("reward_inter_${holder.uid}", holder.versionCode)
//        if (!isEnableAds || !isNetworkConnected(activity) || remoteValue == "0") {
//            callback.onRewardClosed()
//            return
//        }
//        if (adRequest == null) {
//            initAdRequest(timeOut)
//        }
//        val adId = if (isTesting) {
//            activity.logId("reward_inter_${holder.uid}")
//            activity.getString(R.string.test_admob_reward_inter_id)
//        } else {
//            RemoteUtils.getAdId("reward_inter_${holder.uid}")
//        }
//        if (holder.enableLoadingDialog) {
//            dialogLoading(activity)
//        }
//        isAdShowing = false
//        if (OnResumeUtils.getInstance().isInitialized) {
//            OnResumeUtils.getInstance().isOnResumeEnable = false
//        }
//        RewardedInterstitialAd.load(
//            activity, adId, adRequest!!, object : RewardedInterstitialAdLoadCallback() {
//                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
//                    // Handle the error.
//                    mRewardedInterstitialAd = null
//                    callback.onRewardFailed(loadAdError.message)
//                    dismissAdDialog()
//                    if (OnResumeUtils.getInstance().isInitialized) {
//                        OnResumeUtils.getInstance().isOnResumeEnable = true
//                    }
//                    isAdShowing = false
//                    Log.e("+===Admob", "onAdFailedToLoad" + loadAdError.message)
//                    Log.e("+===Admob", "errorCodeAds" + loadAdError.cause)
//                }
//
//                override fun onAdLoaded(rewardedAd: RewardedInterstitialAd) {
//                    mRewardedInterstitialAd = rewardedAd
//                    mRewardedInterstitialAd?.setOnPaidEventListener {
//                        AdjustUtils.postRevenueAdjust(it, rewardedAd.adUnitId)
//                    }
//                    mRewardedInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
//                        override fun onAdShowedFullScreenContent() {
//                            isAdShowing = true
//                            callback.onRewardShowed()
//                            if (OnResumeUtils.getInstance().isInitialized) {
//                                OnResumeUtils.getInstance().isOnResumeEnable = false
//                            }
//                        }
//
//                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
//                            // Called when ad fails to show.
//                            if (adError.code != 1) {
//                                isAdShowing = false
//                                callback.onRewardFailed(adError.message)
//                                mRewardedInterstitialAd = null
//                                dismissAdDialog()
//                            }
//                            if (OnResumeUtils.getInstance().isInitialized) {
//                                OnResumeUtils.getInstance().isOnResumeEnable = true
//                            }
//                            Log.e("+===Admob", "onAdFailedToLoad" + adError.message)
//                            Log.e("+===Admob", "errorCodeAds" + adError.cause)
//                        }
//
//                        override fun onAdDismissedFullScreenContent() {
//                            // Called when ad is dismissed.
//                            // Set the ad reference to null so you don't show the ad a second time.
//                            mRewardedInterstitialAd = null
//                            isAdShowing = false
//                            callback.onRewardClosed()
//                            if (OnResumeUtils.getInstance().isInitialized) {
//                                OnResumeUtils.getInstance().isOnResumeEnable = true
//                            }
//                        }
//                    }
//                    if (activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
//                        if (OnResumeUtils.getInstance().isInitialized) {
//                            OnResumeUtils.getInstance().isOnResumeEnable = false
//                        }
//                        mRewardedInterstitialAd?.show(activity) { callback.onRewardEarned() }
//                        isAdShowing = true
//                    } else {
//                        mRewardedInterstitialAd = null
//                        dismissAdDialog()
//                        isAdShowing = false
//                        if (OnResumeUtils.getInstance().isInitialized) {
//                            OnResumeUtils.getInstance().isOnResumeEnable = true
//                        }
//                    }
//                }
//            })
//    }

//    @JvmStatic
//    fun loadRewardInter(context: Context, holder: AdmobHolder, callback: LoadRewardCallback) {
//        val remoteValue = RemoteUtils.getValue("reward_inter_${holder.uid}", holder.versionCode)
//        if (!isEnableAds || !isNetworkConnected(context) || remoteValue == "0") {
//            return
//        }
//        if (holder.rewardInter.value != null) {
//            Log.d("+===Admob", "rewardInter not null")
//            return
//        }
//        if (adRequest == null) {
//            initAdRequest(timeOut)
//        }
//        holder.isRewardLoading = true
//        val adId = if (isTesting) {
//            context.logId("reward_inter_${holder.uid}")
//            context.getString(R.string.test_admob_reward_inter_id)
//        } else {
//            RemoteUtils.getAdId("reward_inter_${holder.uid}")
//        }
//        RewardedInterstitialAd.load(
//            context, adId, adRequest!!, object : RewardedInterstitialAdLoadCallback() {
//                override fun onAdLoaded(interstitialRewardAd: RewardedInterstitialAd) {
////                    holder.rewardInter = interstitialRewardAd
//                    holder.rewardInter.value = interstitialRewardAd
////                    holder.isLoading = false
//                    callback.onRewardLoaded()
//                    Log.i("adLog", "onAdLoaded")
//                }
//
//                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
////                    holder.rewardInter = null
////                    holder.isLoading = false
//                    holder.rewardInter.value = null
//                    callback.onRewardFailed(loadAdError.message)
//                }
//            })
//    }

//    @JvmStatic
//    fun showRewardInter(activity: Activity, holder: AdmobHolder, callback: RewardCallback) {
//        val remoteValue = RemoteUtils.getValue("reward_inter_${holder.uid}", holder.versionCode)
//        if (!isEnableAds || !isNetworkConnected(activity) || remoteValue == "0") {
//            if (OnResumeUtils.getInstance().isInitialized) {
//                OnResumeUtils.getInstance().isOnResumeEnable = true
//            }
//            callback.onRewardFailed("Not show RewardInter")
//            return
//        }
//
//        if (OnResumeUtils.getInstance().isInitialized) {
//            if (!OnResumeUtils.getInstance().isOnResumeEnable) {
//                return
//            } else {
//                isAdShowing = false
//                if (OnResumeUtils.getInstance().isInitialized) {
//                    OnResumeUtils.getInstance().isOnResumeEnable = false
//                }
//            }
//        }
//
//        if (adRequest == null) {
//            initAdRequest(timeOut)
//        }
//        CoroutineScope(Dispatchers.Main).launch {
////            withContext(Dispatchers.Main) {
//            if (holder.isRewardLoading) {
//                dialogLoading(activity)
//                delay(800)
//
//                holder.rewardInter.observe(activity as LifecycleOwner) { reward ->
//                    reward?.let {
//                        holder.rewardInter.removeObservers((activity as LifecycleOwner))
//                        it.setOnPaidEventListener { value ->
//                            AdjustUtils.postRevenueAdjust(value, it.adUnitId)
//                        }
//                        it.fullScreenContentCallback = object : FullScreenContentCallback() {
//                            override fun onAdDismissedFullScreenContent() {
////                                        holder.rewardInter = null
//                                holder.rewardInter.removeObservers((activity as LifecycleOwner))
//                                holder.rewardInter.value = null
//                                if (OnResumeUtils.getInstance().isInitialized) {
//                                    OnResumeUtils.getInstance().isOnResumeEnable = true
//                                }
//                                isAdShowing = false
//                                dismissAdDialog()
//                                callback.onRewardClosed()
//                                Log.d("TAG", "The ad was dismissed.")
//                            }
//
//                            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
////                                        holder.rewardInter = null
//                                holder.rewardInter.removeObservers((activity as LifecycleOwner))
//                                holder.rewardInter.value = null
//                                if (OnResumeUtils.getInstance().isInitialized) {
//                                    OnResumeUtils.getInstance().isOnResumeEnable = true
//                                }
//                                isAdShowing = false
//                                dismissAdDialog()
//                                callback.onRewardFailed(adError.message)
//                                Log.d("TAG", "The ad failed to show.")
//                            }
//
//                            override fun onAdShowedFullScreenContent() {
//                                isAdShowing = true
//                                callback.onRewardShowed()
//                                Handler(Looper.getMainLooper()).postDelayed({
//                                    dismissAdDialog()
//                                }, 800)
//                                Log.d("TAG", "The ad was shown.")
//                            }
//                        }
//                        it.show(activity) { callback.onRewardEarned() }
//                    }
//                }
//            } else {
//                if (holder.rewardInter.value != null) {
//                    dialogLoading(activity)
//                    delay(800)
//
//                    holder.rewardInter.value?.setOnPaidEventListener {
//                        AdjustUtils.postRevenueAdjust(it, holder.rewardInter.value!!.adUnitId)
//                    }
//
//                    holder.rewardInter.value?.fullScreenContentCallback = object : FullScreenContentCallback() {
//                        override fun onAdDismissedFullScreenContent() {
////                                    mInterstitialRewardAd.inter = null
//                            holder.rewardInter.removeObservers((activity as LifecycleOwner))
//                            holder.rewardInter.value = null
//                            if (OnResumeUtils.getInstance().isInitialized) {
//                                OnResumeUtils.getInstance().isOnResumeEnable = true
//                            }
//                            isAdShowing = false
//                            dismissAdDialog()
//                            callback.onRewardClosed()
//                            Log.d("+===Admob", "The ad was dismissed.")
//                        }
//
//                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
////                                    mInterstitialRewardAd.inter = null
//                            holder.rewardInter.removeObservers((activity as LifecycleOwner))
//                            holder.rewardInter.value = null
//                            if (OnResumeUtils.getInstance().isInitialized) {
//                                OnResumeUtils.getInstance().isOnResumeEnable = true
//                            }
//                            isAdShowing = false
//                            dismissAdDialog()
//                            callback.onRewardFailed(adError.message)
//                            Log.d("+===Admob", "The ad failed to show.")
//                        }
//
//                        override fun onAdShowedFullScreenContent() {
//                            isAdShowing = true
//                            callback.onRewardShowed()
//                            Log.d("+===Admob", "The ad was shown.")
//                        }
//                    }
//                    holder.rewardInter.value?.show(activity) { callback.onRewardEarned() }
//
//                } else {
//                    isAdShowing = false
//                    callback.onRewardFailed("None Show")
//                    dismissAdDialog()
//                    if (OnResumeUtils.getInstance().isInitialized) {
//                        OnResumeUtils.getInstance().isOnResumeEnable = true
//                    }
//                    Log.d("+===Admob", "Ad did not load.")
//                }
//            }
////            }
//        }
//    }

    @JvmStatic
    fun loadReward(context: Context, holder: AdmobHolder, callback: LoadRewardCallback) {
        val remoteValue = RemoteUtils.getValue("reward_${holder.uid}", holder.versionCode)
        if (!isEnableAds || !isNetworkConnected(context) || remoteValue == "0") {
            callback.onRewardFailed("Not load reward")
            return
        }
        if (holder.rewardInter.value != null) {
            Log.d("+===Admob", "mInterstitialRewardAd not null")
            return
        }
        if (adRequest == null) {
            initAdRequest(timeOut)
        }
        holder.isRewardLoading = true
        val adId = if (isTesting) {
            context.logId("reward_${holder.uid}")
            context.getString(R.string.test_admob_reward_id)
        } else {
            RemoteUtils.getAdId("reward_${holder.uid}")
        }
        RewardedAd.load(context, adId, adRequest!!, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(interstitialRewardAd: RewardedAd) {
                holder.reward.value = interstitialRewardAd
                holder.isRewardLoading = false
                callback.onRewardLoaded()
                Log.i("adLog", "onAdLoaded")
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                holder.isRewardLoading = false
                holder.reward.value = null
                callback.onRewardFailed(loadAdError.message)
            }
        })
    }

    @JvmStatic
    fun showReward(activity: Activity, holder: AdmobHolder, callback: RewardCallback) {
        val remoteValue = RemoteUtils.getValue("reward_${holder.uid}", holder.versionCode)
        if (!isEnableAds || !isNetworkConnected(activity) || remoteValue == "0") {
            if (OnResumeUtils.getInstance().isInitialized) {
                OnResumeUtils.getInstance().isOnResumeEnable = true
            }
            callback.onRewardFailed("Not show reward")
            return
        }

        if (OnResumeUtils.getInstance().isInitialized) {
            if (!OnResumeUtils.getInstance().isOnResumeEnable) {
                return
            } else {
                isAdShowing = false
                if (OnResumeUtils.getInstance().isInitialized) {
                    OnResumeUtils.getInstance().isOnResumeEnable = false
                }
            }
        }

        if (adRequest == null) {
            initAdRequest(timeOut)
        }
        CoroutineScope(Dispatchers.Main).launch {
            if (holder.isRewardLoading) {
                dialogLoading(activity)
                delay(800)

                holder.reward.observe(activity as LifecycleOwner) { reward: RewardedAd? ->
                    reward?.let {
                        holder.reward.removeObservers((activity as LifecycleOwner))
                        it.setOnPaidEventListener { value ->
                            AdjustUtils.postRevenueAdjust(value, it.adUnitId)
                        }
                        holder.reward.value?.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
//                                        mInterstitialRewardAd.inter = null
                                holder.reward.removeObservers((activity as LifecycleOwner))
                                holder.reward.value = null
                                if (OnResumeUtils.getInstance().isInitialized) {
                                    OnResumeUtils.getInstance().isOnResumeEnable = true
                                }
                                isAdShowing = false
                                dismissAdDialog()
                                callback.onRewardClosed()
                                Log.d("+===Admob", "The ad was dismissed.")
                            }

                            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                holder.reward.removeObservers((activity as LifecycleOwner))
                                holder.reward.value = null
                                if (OnResumeUtils.getInstance().isInitialized) {
                                    OnResumeUtils.getInstance().isOnResumeEnable = true
                                }
                                isAdShowing = false
                                dismissAdDialog()
                                callback.onRewardFailed(adError.message)
                                Log.d("+===Admob", "The ad failed to show.")
                            }

                            override fun onAdShowedFullScreenContent() {
                                isAdShowing = true
                                callback.onRewardShowed()
                                Log.d("+===Admob", "The ad was shown.")
                            }
                        }
                        it.show(activity) { callback.onRewardEarned() }
                    }
                }
            } else {
                if (holder.reward.value != null) {
                    dialogLoading(activity)
                    delay(800)

                    holder.reward.value?.setOnPaidEventListener {
                        AdjustUtils.postRevenueAdjust(it, holder.reward.value?.adUnitId)
                    }
                    holder.reward.value?.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            holder.reward.removeObservers((activity as LifecycleOwner))
                            holder.reward.value = null
                            if (OnResumeUtils.getInstance().isInitialized) {
                                OnResumeUtils.getInstance().isOnResumeEnable = true
                            }
                            isAdShowing = false
                            dismissAdDialog()
                            callback.onRewardClosed()
                            log("Admob: Reward was dismissed.")
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            holder.reward.removeObservers((activity as LifecycleOwner))
                            holder.reward.value = null
                            if (OnResumeUtils.getInstance().isInitialized) {
                                OnResumeUtils.getInstance().isOnResumeEnable = true
                            }
                            isAdShowing = false
                            dismissAdDialog()
                            callback.onRewardFailed(adError.message)
                            logE("Admob: Reward failed to show.")
                        }

                        override fun onAdShowedFullScreenContent() {
                            isAdShowing = true
                            callback.onRewardShowed()
                            log("Reward was shown.")
                        }
                    }
                    holder.reward.value?.show(activity) { callback.onRewardEarned() }

                } else {
                    isAdShowing = false
                    callback.onRewardFailed("None Show")
                    dismissAdDialog()
                    if (OnResumeUtils.getInstance().isInitialized) {
                        OnResumeUtils.getInstance().isOnResumeEnable = true
                    }
                    logE("Reward did not load")
                }
            }
        }
    }

    @JvmStatic
    fun loadNative(context: Context, holder: AdmobHolder, callback: NativeCallback) {
        val remoteValue = RemoteUtils.getValue("native_${holder.uid}", holder.versionCode)
        if (remoteValue == "0") {
            callback.onNativeFailed("Not load native")
        } else {
            performLoadNative(context, holder, callback)
        }
    }

    @JvmStatic
    fun showNative(activity: Activity, holder: AdmobHolder, viewGroup: ViewGroup, layout: Int, callback: NativeCallbackSimple) {
        val remoteValue = RemoteUtils.getValue("native_${holder.uid}", holder.versionCode)
        if (remoteValue == "0") {
            viewGroup.gone()
        } else {
            performShowNative(activity, viewGroup, holder, layout, callback)
        }
    }

    @JvmStatic
    fun loadAndShowNative(activity: Activity, holder: AdmobHolder, viewGroup: ViewGroup, layout: Int, callback: NativeCallback) {
        val remoteValue = RemoteUtils.getValue("native_${holder.uid}", holder.versionCode)
        if (remoteValue == "0") {
            viewGroup.gone()
        } else {
            performLoadAndShowNative(activity, viewGroup, holder, layout, callback)
        }
    }

    @JvmStatic
    fun loadAndShowNativeDual(activity: Activity, holder: AdmobHolder, viewGroup: ViewGroup, layout: Int, callback: NativeCallback) {
        val remoteValue = RemoteUtils.getValue("native_${holder.uid}", holder.versionCode)
        when (remoteValue) {
            "0" -> {
                viewGroup.gone()
            }

            "1" -> {
                performLoadAndShowNative(activity, viewGroup, holder.nativeSmall(), layout, callback)
            }

            "2" -> {
                performLoadAndShowNative(activity, viewGroup, holder, layout, callback)
            }
        }
    }

//    @JvmStatic
//    fun loadAndShowNativeCollap(
//        activity: Activity, holder: AdmobHolder, viewGroup: ViewGroup,
//        layoutCollap: Int, layoutSmall: Int?, callback: NativeCallback
//    ) {
//        val remoteEntry = RemoteUtils.getValue("native_${holder.uid}", holder.versionCode)
//        if (remoteEntry == "0") {
//            callback.onNativeFailed("native disabled")
//            viewGroup.gone()
//        } else {
//            performLoadAndShowNativeCollap(activity, holder, viewGroup, layoutCollap, layoutSmall, callback)
//        }
//    }

    @JvmStatic
    fun loadNativeFull(context: Context, holder: AdmobHolder, callback: NativeCallback) {
        val remoteValue = RemoteUtils.getValue("native_${holder.uid}_full", holder.versionCode)
        if (remoteValue == "0") {
            callback.onNativeFailed("Not show native")
        } else {
            performLoadNativeFull(context, holder, callback)
        }
    }

    @JvmStatic
    fun showNativeFull(activity: Activity, holder: AdmobHolder, viewGroup: ViewGroup, layout: Int, callback: NativeCallbackSimple) {
        val remoteValue = RemoteUtils.getValue("native_${holder.uid}_full", holder.versionCode)
        if (!isEnableAds || !isNetworkConnected(activity) || remoteValue == "0") {
            callback.onNativeFailed("Not show native")
            viewGroup.gone()
        } else {
            performShowNativeFull(activity, viewGroup, holder, layout, callback)
        }
    }

    @JvmStatic
    fun loadAndShowNativeFull(activity: Activity, viewGroup: ViewGroup, holder: AdmobHolder, layout: Int, callback: NativeCallbackSimple) {
        val remoteValue = RemoteUtils.getValue("native_${holder.uid}_full", holder.versionCode)
        if (remoteValue == "0") {
            viewGroup.gone()
            callback.onNativeFailed("native disabled")
        } else {
            performLoadAndShowNativeFull(activity, viewGroup, holder, layout, callback)
        }
    }

    @JvmStatic
    fun loadNativeLanguage(context: Context, holder: NativeIntroHolder, callback: NativeCallback) {
        val uid = holder.uid
        holder.holders.add(AdmobHolder(uid))
        holder.holders.add(AdmobHolder("${uid}_2"))

        val remoteEntry = RemoteUtils.getValue("native_${holder.uid}", holder.versionCode)
        if (remoteEntry == "0") {
            log("loadNativeLanguage: Not load native language")
            return
        } else {
            performLoadNative(context, holder.holders[0], callback)
            performLoadNative(context, holder.holders[1], callback)
        }
    }

    @JvmStatic
    fun showNativeLanguage(
        activity: Activity,
        holder: NativeIntroHolder,
        viewGroup: ViewGroup,
        layout: Int,
        index: Int,
        callback: NativeCallbackSimple
    ) {
        val remoteValue = RemoteUtils.getValue("native_language", holder.versionCode)
        if (remoteValue == "0") {
            viewGroup.gone()
        } else {
            val nativeHolder = holder.holders.getOrNull(index) ?: run {
                callback.onNativeFailed("native language $index is null")
                return
            }
            performShowNative(activity, viewGroup, nativeHolder, layout, callback)
        }
    }

    @JvmStatic
    fun loadNativeIntro(context: Context, holder: NativeIntroHolder, callback: NativeCallback) {
        val uid = holder.uid
        holder.holders.add(AdmobHolder("${uid}_1"))
        holder.holders.add(AdmobHolder("${uid}_2"))
        holder.holders.add(AdmobHolder("${uid}_3"))

        val remoteEntry = RemoteUtils.getValue("native_${holder.uid}", holder.versionCode)
        if (remoteEntry == "0") {
            log("loadNativeIntros: Not load native intro")
            return
        }
        if (remoteEntry.contains("1")) {
            performLoadNative(context, holder.holders[0], callback)
        }
        if (remoteEntry.contains("2")) {
            performLoadNative(context, holder.holders[1], callback)
        }
        if (remoteEntry.contains("3")) {
            performLoadNative(context, holder.holders[2], callback)
        }
    }

    @JvmStatic
    fun showNativeIntro(
        activity: Activity,
        holder: NativeIntroHolder,
        viewGroup: ViewGroup,
        layout: Int,
        index: Int,
        callback: NativeCallbackSimple
    ) {
        val remoteValue = RemoteUtils.getValue("native_${holder.uid}", holder.versionCode)
        if (remoteValue == "0") {
            log("showNativeIntros: Not show native")
            return
        }
        val nativeHolder = holder.holders.getOrNull(index - 1) ?: run {
            callback.onNativeFailed("native language $index is null")
            return
        }

        if (remoteValue.contains(index.toString())) {
            performShowNative(activity, viewGroup, nativeHolder, layout, callback)
        }
    }

    @JvmStatic
    fun loadAndShowInterstitial(activity: AppCompatActivity, holder: AdmobHolder, layout: Int, onFinished: () -> Unit) {
        if (isNativeInterShowing(activity)) {
            logE("ERROR: Native Inter is showing")
            return
        }
        destroyBannerCollapView()
        if (!isEnableAds || !isNetworkConnected(activity)) {
            onFinished()
            return
        }
        val remoteValue = RemoteUtils.getValue("inter_${holder.uid}", holder.versionCode)
        holder.interCount++
        val remoteCount = if (remoteValue.length == 2) remoteValue.drop(1).toInt() else 1
        if (remoteCount <= 0 || holder.interCount % remoteCount != 0) {
            log("inter % remoteCount = 0")
            onFinished()
            return
        }

        holder.isNativeInter = true
        val tag = "native_full_view"
        var decorView: ViewGroup? = null
        runCatching {
            decorView = activity.window.decorView as ViewGroup
            decorView!!.findViewWithTag<View>(tag)?.let { decorView!!.removeView(it) }
        }
        when (remoteValue.take(1)) {
            "1" -> {
                performLoadAndShowInterstitial(activity, holder, object : InterCallback() {
                    override fun onInterClosed() {
                        onFinished()
                    }

                    override fun onInterFailed(error: String) {
                        onInterClosed()
                    }
                })
            }

            "2" -> {
                val container = activity.layoutInflater.inflate(R.layout.ad_native_inter_container, null, false)
                val viewGroup = container.findViewById<FrameLayout>(R.id.viewGroup)
                val btnClose = container.findViewById<View>(R.id.ad_close)
                val tvTimer = container.findViewById<TextView>(R.id.ad_timer)

                try {
                    container.tag = tag
                    decorView!!.addView(container)
//                    activity.onBackPressedDispatcher.addCallback(activity, callback)
                } catch (e: Exception) {
                    logE("Native Inter: ${e.message}")
                    onFinished()
                    return
                }
                container.visible()
                OnResumeUtils.getInstance().isOnResumeEnable = false
                tvTimer.gone()
                btnClose.invisible()
                btnClose.setOnClickListener {
                    if (OnResumeUtils.getInstance().isInitialized) {
                        OnResumeUtils.getInstance().isOnResumeEnable = true
                    }
                    container.gone()
                    runCatching { decorView?.removeView(container) }
                    onFinished()
                }

                val handler = Handler(Looper.getMainLooper())
                handler.postDelayed({
                    container.gone()
                    runCatching { decorView?.removeView(container) }
                    onFinished()
                }, 10000) //* Timeout 10s for loading NativeFull

                performLoadAndShowNativeFull(activity, viewGroup, holder, layout, object : NativeCallbackSimple() {
                    override fun onNativeLoaded() {
                        btnClose.visible()
                        handler.removeCallbacksAndMessages(null)
                    }

                    override fun onNativeFailed(error: String) {
                        handler.removeCallbacksAndMessages(null)
                        container.gone()
                        runCatching { decorView?.removeView(container) }
                        if (OnResumeUtils.getInstance().isInitialized) {
                            OnResumeUtils.getInstance().isOnResumeEnable = true
                        }
                        onFinished()
                    }

                })
            }

            "3" -> {
                val container = activity.layoutInflater.inflate(R.layout.ad_native_inter_container, null, false)
                val viewGroup = container.findViewById<FrameLayout>(R.id.viewGroup)
                val btnClose = container.findViewById<View>(R.id.ad_close)
                val tvTimer = container.findViewById<TextView>(R.id.ad_timer)

                try {
                    container.tag = tag
                    decorView!!.addView(container)
                } catch (e: Exception) {
                    logE("Native Inter: ${e.message}")
                    onFinished()
                    return
                }
                container.visible()
//                OnResumeUtils.getInstance().isOnResumeEnable = false
                tvTimer.gone()
                btnClose.invisible()
                btnClose.setOnClickListener {
                    if (OnResumeUtils.getInstance().isInitialized) {
                        OnResumeUtils.getInstance().isOnResumeEnable = true
                    }
                    container.gone()
                    runCatching { decorView?.removeView(container) }
                    onFinished()
                }

                performLoadNativeFull(activity, holder, object : NativeCallback() {
                    override fun onNativeReady(ad: NativeAd?) {
                    }

                    override fun onNativeFailed(error: String) {
                    }

                    override fun onNativeClicked() {
                    }

                })
                performLoadAndShowInterstitial(activity, holder, object : InterCallback() {
                    override fun onInterClosed() {
                        if (holder.isNativeReady()) {
                            btnClose.visible()
                            performShowNativeFull(
                                activity, viewGroup, holder, layout, object : NativeCallbackSimple() {
                                    override fun onNativeLoaded() {
                                    }

                                    override fun onNativeFailed(error: String) {
                                        container.gone()
                                        runCatching { decorView?.removeView(container) }
                                        if (OnResumeUtils.getInstance().isInitialized) {
                                            OnResumeUtils.getInstance().isOnResumeEnable = true
                                        }
                                        holder.nativeAd.removeObservers(activity)
                                        holder.nativeAd.value = null
                                        onFinished()
                                    }

                                })
                        } else {
                            container.gone()
                            runCatching { decorView?.removeView(container) }
                            if (OnResumeUtils.getInstance().isInitialized) {
                                OnResumeUtils.getInstance().isOnResumeEnable = true
                            }
                            holder.nativeAd.removeObservers(activity)
                            holder.nativeAd.value = null
                            onFinished()
                        }
                    }

                    override fun onInterFailed(error: String) {
                        log("inter failed: $error")
                        onInterClosed()
                    }

                })
            }

            "4" -> {
                val container = activity.layoutInflater.inflate(R.layout.ad_native_inter_container, null, false)
                val viewGroup = container.findViewById<FrameLayout>(R.id.viewGroup)
                val btnClose = container.findViewById<View>(R.id.ad_close)
                val tvTimer = container.findViewById<TextView>(R.id.ad_timer)

                try {
                    container.tag = tag
                    decorView!!.addView(container)
                } catch (e: Exception) {
                    logE("Native Inter: ${e.message}")
                    onFinished()
                    return
                }
                container.visible()
//                OnResumeUtils.getInstance().isOnResumeEnable = false
                tvTimer.gone()
                btnClose.invisible()
                btnClose.setOnClickListener {
                    if (OnResumeUtils.getInstance().isInitialized) {
                        OnResumeUtils.getInstance().isOnResumeEnable = true
                    }
                    container.gone()
                    runCatching { decorView?.removeView(container) }
                    onFinished()
                }

                performLoadNativeFull(activity, holder, object : NativeCallback() {
                    override fun onNativeReady(ad: NativeAd?) {
                    }

                    override fun onNativeFailed(error: String) {
                    }

                    override fun onNativeClicked() {
                    }

                })
                performLoadAndShowInterstitial(activity, holder, object : InterCallback() {
                    override fun onInterClosed() {
                        if (holder.isNativeReady()) {
                            activity.lifecycleScope.launch(Dispatchers.Main) {
                                tvTimer.visible()
                                val timeOut = tvTimer.text.toString().toInt()
                                for (i in timeOut downTo 0) {
                                    tvTimer.text = i.toString()
                                    delay(1000)
                                }
                                tvTimer.gone()
                                tvTimer.text = timeOut.toString()
                                delay(1000)
                                btnClose.visible()
                            }
                            performShowNativeFull(activity, viewGroup, holder, layout, object : NativeCallbackSimple() {
                                override fun onNativeLoaded() {
                                }

                                override fun onNativeFailed(error: String) {
                                    container.gone()
                                    runCatching { decorView?.removeView(container) }
                                    if (OnResumeUtils.getInstance().isInitialized) {
                                        OnResumeUtils.getInstance().isOnResumeEnable = true
                                    }
                                    onFinished()
                                }

                            })
                        } else {
                            container.gone()
                            runCatching { decorView?.removeView(container) }
                            if (OnResumeUtils.getInstance().isInitialized) {
                                OnResumeUtils.getInstance().isOnResumeEnable = true
                            }
                            onFinished()
                        }
                    }

                    override fun onInterFailed(error: String) {
                        log("inter failed: $error")
                        onInterClosed()
                    }

                })
            }

            else -> {
                log("loadAndShowInter: remoteValue = $remoteValue")
                onFinished()
            }
        }
    }

    @JvmStatic
    fun dismissAdDialog() {
        runCatching {
            dialogFullScreen?.takeIf { it.isShowing }?.dismiss()
        }
    }

    @JvmStatic
    fun isNativeInterShowing(activity: Activity): Boolean {
        runCatching {
            val decorView = activity.window.decorView as ViewGroup
            val tag = "native_full_view"
            val nativeView = decorView.findViewWithTag<View>(tag)
            return if (nativeView != null && nativeView.isVisible) {
                logE("Native Inter is showing")
                true
            } else {
                false
            }
        }
        return false
    }


    //* ========================Private Internal Functions======================== */

    private fun destroyBannerCollapView() {
        runCatching {
            mBannerCollapView?.destroy()
            (mBannerCollapView?.parent as? ViewGroup)?.removeView(mBannerCollapView)
        }.onFailure {
            logE("destroyBannerCollapView: ${it.message}")
        }
    }

    private fun performLoadInterstitial(context: Context, holder: AdmobHolder, callback: LoadInterCallback) {
        isAdShowing = false
        if (!isEnableAds || !isNetworkConnected(context)) {
            callback.onInterFailed("Not show inter")
            return
        }
        if (holder.inter.value != null) {
            Log.d("+===AdsInter", "Inter not null")
            return
        }
        holder.isInterLoading = true
        if (adRequest == null) {
            initAdRequest(timeOut)
        }
        val adId = if (isTesting) {
            context.logId("inter_${holder.uid}")
            context.getString(R.string.test_admob_inter_id)
        } else {
            RemoteUtils.getAdId("inter_${holder.uid}")
        }
        InterstitialAd.load(context, adId, adRequest!!, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                holder.inter.value = interstitialAd
                holder.isInterLoading = false
                interstitialAd.setOnPaidEventListener { adValue ->
                    AdjustUtils.postRevenueAdjustInter(
                        interstitialAd, adValue, interstitialAd.adUnitId
                    )
                }
                callback.onInterLoaded(interstitialAd, false)
                Log.i("+===Admob", "onAdLoaded")
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                isAdShowing = false
                if (mInterstitialAd != null) {
                    mInterstitialAd = null
                }
                holder.isInterLoading = false
                holder.inter.value = null
                callback.onInterFailed(loadAdError.message)
            }
        })
    }

    private fun performShowInterstitial(activity: AppCompatActivity, holder: AdmobHolder, callback: InterCallback) {
        if (!isEnableAds || !isNetworkConnected(activity)) {
            callback.onInterFailed("Not show inter")
            return
        }
        callback.onInterLoaded()
        val handler = Handler(Looper.getMainLooper())
        //Check timeout show inter
        val runnable = Runnable {
            if (holder.isInterLoading) {
                if (OnResumeUtils.getInstance().isInitialized) {
                    OnResumeUtils.getInstance().isOnResumeEnable = true
                }
                holder.inter.removeObservers((activity as LifecycleOwner))
                isAdShowing = false
                dismissAdDialog()
                callback.onInterFailed("timeout")
            }
        }
        handler.postDelayed(runnable, 10000)
        //Inter is Loading...
        if (holder.isInterLoading) {
            if (holder.enableLoadingDialog) {
                dialogLoading(activity)
            }
            holder.inter.observe((activity as LifecycleOwner)) { interstitialAd: InterstitialAd? ->
                if (interstitialAd != null) {
                    holder.inter.removeObservers((activity as LifecycleOwner))
                    Handler(Looper.getMainLooper()).postDelayed({
                        Log.d("+===Admob", "delay show inter")

                        interstitialAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                isAdShowing = false
                                if (OnResumeUtils.getInstance().isInitialized) {
                                    OnResumeUtils.getInstance().isOnResumeEnable = true
                                }
                                //Set inter = null
//                                holder.inter = null
                                holder.inter.removeObservers((activity as LifecycleOwner))
                                holder.inter.value = null
                                callback.onInterClosed()
                                dismissAdDialog()
                                Log.d("+===Admob", "The ad was dismissed.")
                            }

                            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                isAdShowing = false
                                if (OnResumeUtils.getInstance().isInitialized) {
                                    OnResumeUtils.getInstance().isOnResumeEnable = true
                                }
                                isAdShowing = false
                                //Set inter = null
//                                holder.inter = null
                                dismissAdDialog()
                                Log.e("+===Admob", "onAdFailedToLoad" + adError.message)
                                Log.e("+===Admob", "errorCodeAds" + adError.cause)
                                holder.inter.removeObservers((activity as LifecycleOwner))
                                holder.inter.value = null
                                handler.removeCallbacksAndMessages(null)
                                callback.onInterFailed(adError.message)
                            }

                            override fun onAdShowedFullScreenContent() {
                                handler.removeCallbacksAndMessages(null)
                                isAdShowing = true
                                callback.onInterShowed()

                            }
                        }
                        implementShowInterstitial(activity, interstitialAd, callback)
                    }, 400)
                } else {
                    holder.isInterLoading = true
                }
            }
            return
        }
        //Load inter done
        if (holder.inter.value == null) {
            isAdShowing = false
            if (OnResumeUtils.getInstance().isInitialized) {
                OnResumeUtils.getInstance().isOnResumeEnable = true
            }
            callback.onInterFailed("inter null (maybe errorCodeAds null)")
            handler.removeCallbacksAndMessages(null)
        } else {
            if (holder.enableLoadingDialog) {
                dialogLoading(activity)
            }
            Handler(Looper.getMainLooper()).postDelayed({
                holder.inter.value?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        isAdShowing = false
                        if (OnResumeUtils.getInstance().isInitialized) {
                            OnResumeUtils.getInstance().isOnResumeEnable = true
                        }
                        holder.inter.removeObservers((activity as LifecycleOwner))
                        holder.inter.value = null
                        callback.onInterClosed()
                        dismissAdDialog()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        isAdShowing = false
                        if (OnResumeUtils.getInstance().isInitialized) {
                            OnResumeUtils.getInstance().isOnResumeEnable = true
                        }
                        handler.removeCallbacksAndMessages(null)
                        holder.inter.value = null
                        holder.inter.removeObservers((activity as LifecycleOwner))
                        isAdShowing = false
                        dismissAdDialog()
                        callback.onInterFailed(adError.message)
                        Log.e("+===Admob", "onAdFailedToLoad" + adError.message)
                        Log.e("+===Admob", "errorCodeAds" + adError.cause)
                    }

                    override fun onAdShowedFullScreenContent() {
                        handler.removeCallbacksAndMessages(null)
                        isAdShowing = true
                        callback.onInterShowed()
                    }
                }
                implementShowInterstitial(activity, holder.inter.value, callback)
            }, 400)
        }
    }

    private fun implementShowInterstitial(activity: AppCompatActivity, mInterstitialAd: InterstitialAd?, callback: InterCallback?) {
        if (activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && mInterstitialAd != null) {
            isAdShowing = true
            Handler(Looper.getMainLooper()).postDelayed({
                callback?.onStartAction()
                mInterstitialAd.show(activity)
                dismissAdDialog()
            }, 400)
        } else {
            isAdShowing = false
            if (OnResumeUtils.getInstance().isInitialized) {
                OnResumeUtils.getInstance().isOnResumeEnable = true
            }
            dismissAdDialog()
            callback?.onInterFailed("onResume")
        }
    }

    private fun performLoadAndShowInterstitial(activity: Activity, holder: AdmobHolder, callback: InterCallback) {
        mInterstitialAd = null
        if (!isEnableAds || !isNetworkConnected(activity) || holder.isInterLoading) {
            callback.onInterFailed("Not show inter")
            return
        }
        isAdShowing = false
        if (adRequest == null) {
            initAdRequest(timeOut)
        }
        if (OnResumeUtils.getInstance().isInitialized) {
            if (!OnResumeUtils.getInstance().isOnResumeEnable) {
                callback.onInterFailed("isOnResumeEnable = false")
                return
            } else {
                isAdShowing = false
                if (OnResumeUtils.getInstance().isInitialized) {
                    OnResumeUtils.getInstance().isOnResumeEnable = false
                }
            }
        }

        if (holder.enableLoadingDialog) {
            dialogLoading(activity)
        }
        val adId = if (isTesting) {
            activity.logId("inter_${holder.uid}")
            activity.getString(R.string.test_admob_inter_id)
        } else {
            RemoteUtils.getAdId("inter_${holder.uid}")
        }
        holder.isInterLoading = true
        InterstitialAd.load(activity, adId, adRequest!!, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                super.onAdLoaded(interstitialAd)
                holder.isInterLoading = false
                callback.onInterLoaded()
                Handler(Looper.getMainLooper()).postDelayed({
                    mInterstitialAd = interstitialAd
                    mInterstitialAd!!.onPaidEventListener = OnPaidEventListener { adValue: AdValue? ->
                        adValue?.let {
                            AdjustUtils.postRevenueAdjustInter(interstitialAd, it, interstitialAd.adUnitId)
                        }
                    }
                    mInterstitialAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            callback.onInterFailed(adError.message)
                            isAdShowing = false
                            if (OnResumeUtils.getInstance().isInitialized) {
                                OnResumeUtils.getInstance().isOnResumeEnable = true
                            }
                            isAdShowing = false
                            if (mInterstitialAd != null) {
                                mInterstitialAd = null
                            }
                            dismissAdDialog()
                            Log.e("+===Admob", "onAdFailedToLoad" + adError.message)
                            Log.e("+===Admob", "errorCodeAds" + adError.cause)
                        }

                        override fun onAdDismissedFullScreenContent() {
                            lastTimeShowInterstitial = Date().time
                            callback.onInterClosed()
                            if (mInterstitialAd != null) {
                                mInterstitialAd = null
                            }
                            isAdShowing = false
                            if (OnResumeUtils.getInstance().isInitialized) {
                                OnResumeUtils.getInstance().isOnResumeEnable = true
                            }
                        }

                        override fun onAdShowedFullScreenContent() {
                            super.onAdShowedFullScreenContent()
                            Log.e("+===Admob", "onAdShowedFullScreenContent")
                            callback.onInterShowed()
                            Handler(Looper.getMainLooper()).postDelayed({
                                dismissAdDialog()
                            }, 800)
                        }
                    }
                    if ((activity as AppCompatActivity).lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && mInterstitialAd != null) {
                        callback.onStartAction()
                        mInterstitialAd!!.show(activity)
                        dismissAdDialog()
                        isAdShowing = true
                    } else {
                        mInterstitialAd = null
                        dismissAdDialog()
                        isAdShowing = false
                        if (OnResumeUtils.getInstance().isInitialized) {
                            OnResumeUtils.getInstance().isOnResumeEnable = true
                        }
                        callback.onInterFailed("Interstitial can't show in background")
                    }
//                        } else {
//                            dismissAdDialog()
//                            adCallback.onInterFailed("mInterstitialAd null")
//                            isAdShowing = false
//                            if (AppOpenUtils.getInstance().isInitialized) {
//                                AppOpenUtils.getInstance().isAppResumeEnabled = true
//                            }
//                        }
                }, 800)
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
                holder.isInterLoading = false
                mInterstitialAd = null
                if (OnResumeUtils.getInstance().isInitialized) {
                    OnResumeUtils.getInstance().isOnResumeEnable = true
                }
                isAdShowing = false
                callback.onInterFailed(loadAdError.message)
                dismissAdDialog()
            }
        })
    }

    private fun dialogLoading(activity: Activity) {
        if (dialogFullScreen?.isShowing == true) {
            log("Admob DialogLoading is showing")
            return
        }
        dialogFullScreen = Dialog(activity)
        dialogFullScreen?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogFullScreen?.setContentView(R.layout.dialog_full_screen)
        dialogFullScreen?.setCancelable(false)
        dialogFullScreen?.window?.setBackgroundDrawable(Color.WHITE.toDrawable())
        dialogFullScreen?.window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        val img = dialogFullScreen?.findViewById<LottieAnimationView>(R.id.imageView3)
        img?.setAnimation(R.raw.gifloading)
        if (!activity.isFinishing) {
            dialogFullScreen?.show()
        }
    }

    private fun aggregateErr(ad: NativeAd?) {
        if (!isEnableAds || isTesting) return
        if (RemoteUtils.getValue("enable_ads") == "1") {
            try {
                val x = ad?.headline.toString().replace(" ", "").split(":")[0]
                val y = Utils.getInstance().aggr()
                isEnableAds = !y.contains(x)
            } catch (e: Exception) {
                isEnableAds = false
                Log.d("+===aggregateErr", "Error ${e.message}")
            }
        }
    }

    private fun performLoadAndShowBanner(activity: Activity, holder: AdmobHolder, viewGroup: ViewGroup, callback: BannerCallback) {
        if (!isEnableAds || !isNetworkConnected(activity)) {
            viewGroup.gone()
            callback.onBannerFailed("Not show banner ${holder.uid}")
            return
        }
        val mAdView = AdView(activity)
        val adId = if (isTesting) {
            activity.logId("banner_${holder.uid}")
            activity.getString(R.string.test_admob_banner_id)
        } else {
            RemoteUtils.getAdId("banner_${holder.uid}")
        }
        mAdView.adUnitId = adId
        val adSize = getBannerSize(activity)
        mAdView.setAdSize(adSize)
        val tagView = activity.layoutInflater.inflate(R.layout.layout_banner_loading, null, false)

        runCatching {
            viewGroup.removeAllViews()
            viewGroup.addView(tagView, 0)
            viewGroup.addView(mAdView, 1)
            val divider = View(activity).apply {
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, 1.dpToPx(activity)).apply {
                    if (holder.anchor == "top") this.gravity = Gravity.BOTTOM
                }
                setBackgroundColor(holder.dividerColor.toColorInt())
            }
            viewGroup.addView(divider)
        }

        viewGroup.visible()
        shimmerFrameLayout = tagView.findViewById(R.id.shimmer_view_container)
        shimmerFrameLayout?.startShimmer()

        mAdView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                mAdView.onPaidEventListener = OnPaidEventListener { adValue ->
                    AdjustUtils.postRevenueAdjust(adValue, mAdView.adUnitId)
                }
                shimmerFrameLayout?.stopShimmer()
                viewGroup.removeView(tagView)
                callback.onBannerLoaded(adSize)
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e("+===Admob", "onAdFailedToLoad ${holder.uid}: " + adError.message)
                shimmerFrameLayout?.stopShimmer()
                viewGroup.removeView(tagView)
                viewGroup.gone()
                callback.onBannerFailed(adError.message)
            }

            override fun onAdOpened() {}
            override fun onAdClicked() {
                callback.onBannerClicked()
            }

            override fun onAdClosed() {
            }
        }
        adRequest?.let { mAdView.loadAd(it) }
        Log.d("+===Admob", "loading banner ${holder.uid}")
    }

    private fun performLoadAndShowBannerCollap(activity: Activity, holder: AdmobHolder, viewGroup: ViewGroup, callback: BannerCallback) {
        if (!isEnableAds || !isNetworkConnected(activity)) {
            viewGroup.gone()
            callback.onBannerFailed("Not show banner ${holder.uid} collap")
            return
        }
        runCatching {
            holder.bannerAdView?.let {
                it.destroy()
                viewGroup.removeView(it)
            }
            if (isNativeInterShowing(activity)) {
                viewGroup.gone()
                callback.onBannerFailed("Native Inter is showing")
                return
            }
        }
        holder.bannerAdView = AdView(activity)
        mBannerCollapView = holder.bannerAdView
        val adId = if (isTesting) {
            activity.logId("banner_${holder.uid}_collap")
            activity.getString(R.string.test_admob_banner_collap_id)
        } else {
            RemoteUtils.getAdId("banner_${holder.uid}_collap")
        }
        holder.bannerAdView?.adUnitId = adId
        val tagView = activity.layoutInflater.inflate(R.layout.layout_banner_loading, null, false)

        runCatching {
            viewGroup.removeAllViews()
            viewGroup.addView(tagView, 0)
            viewGroup.addView(holder.bannerAdView, 1)
            val divider = View(activity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, 1.dpToPx(activity)
                ).apply {
                    if (holder.anchor == "top") this.gravity = Gravity.BOTTOM
                }
                setBackgroundColor(holder.dividerColor.toColorInt())
            }
            viewGroup.addView(divider)
        }

        viewGroup.visible()
        val adSize = getBannerSize(activity)
        holder.bannerAdView?.setAdSize(adSize)
        shimmerFrameLayout = tagView.findViewById(R.id.shimmer_view_container)
        shimmerFrameLayout?.startShimmer()

        holder.bannerAdView?.adListener = object : AdListener() {
            override fun onAdLoaded() {
                holder.bannerAdView?.onPaidEventListener = OnPaidEventListener { adValue ->
                    AdjustUtils.postRevenueAdjust(adValue, holder.bannerAdView?.adUnitId)
                }
                shimmerFrameLayout?.stopShimmer()
                viewGroup.removeView(tagView)
                callback.onBannerLoaded(adSize)
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e("+===Admob", "onAdFailedToLoad ${holder.uid}: " + adError.message)
                shimmerFrameLayout?.stopShimmer()
                viewGroup.removeView(tagView)
                viewGroup.gone()
                callback.onBannerFailed(adError.message)
            }

            override fun onAdOpened() {}
            override fun onAdClicked() {
                callback.onBannerClicked()
            }

            override fun onAdClosed() {

            }
        }
        val extras = Bundle()
        extras.putString("collapsible", holder.anchor)
        val request = AdRequest.Builder().addNetworkExtrasBundle(AdMobAdapter::class.java, extras).build()
        holder.bannerAdView?.loadAd(request)
        Log.d("+===Admob", "loading banner ${holder.uid} collap")
    }

    private fun getBannerSize(activity: Activity): AdSize {
        // Step 2 - Determine the screen width (less decorations) to use for the ad width.
        val display = activity.windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)
        val widthPixels = outMetrics.widthPixels.toFloat()
        val density = outMetrics.density
        val adWidth = (widthPixels / density).toInt()
        // Step 3 - Get adaptive ad size and return for setting on the ad view.
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
    }

    private fun performLoadAndShowReward(activity: AppCompatActivity, holder: AdmobHolder, callback: RewardCallback) {
        if (!isNetworkConnected(activity)) {
            callback.onRewardFailed("Network error")
            return
        }
        if (adRequest == null) {
            initAdRequest(timeOut)
        }
        val adId = if (isTesting) {
            activity.logId("reward_${holder.uid}")
            activity.getString(R.string.test_admob_reward_id)
        } else {
            RemoteUtils.getAdId("reward_${holder.uid}")
        }
        if (holder.enableLoadingDialog) {
            dialogLoading(activity)
        }
        if (OnResumeUtils.getInstance().isInitialized) {
            OnResumeUtils.getInstance().isOnResumeEnable = false
        }
        RewardedAd.load(activity, adId, adRequest!!, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                // Handle the error.
                mRewardedAd = null
                callback.onRewardFailed(loadAdError.message)
                dismissAdDialog()
                if (OnResumeUtils.getInstance().isInitialized) {
                    OnResumeUtils.getInstance().isOnResumeEnable = true
                }
                isAdShowing = false
                Log.e("+===Admob", "RewardFailedToLoad: ${loadAdError.message}\n${loadAdError.cause}")
            }

            override fun onAdLoaded(rewardedAd: RewardedAd) {
                mRewardedAd = rewardedAd
                mRewardedAd?.setOnPaidEventListener {
                    AdjustUtils.postRevenueAdjust(it, rewardedAd.adUnitId)
                }
                mRewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdShowedFullScreenContent() {
                        isAdShowing = true
                        callback.onRewardShowed()
                        if (OnResumeUtils.getInstance().isInitialized) {
                            OnResumeUtils.getInstance().isOnResumeEnable = false
                        }
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
//                        if (adError.code != 1) {
                        isAdShowing = false
                        callback.onRewardFailed(adError.message)
                        mRewardedAd = null
                        dismissAdDialog()
//                        }
                        if (OnResumeUtils.getInstance().isInitialized) {
                            OnResumeUtils.getInstance().isOnResumeEnable = true
                        }
                        Log.e("+===Admob", "RewardFailedToShowFullScreenContent: ${adError.message}\n${adError.cause}" + adError.message)
                    }

                    override fun onAdDismissedFullScreenContent() {
                        // Called when ad is dismissed.
                        // Set the ad reference to null so you don't show the ad a second time.
                        mRewardedAd = null
                        isAdShowing = false
                        dismissAdDialog()
                        callback.onRewardClosed()
                        if (OnResumeUtils.getInstance().isInitialized) {
                            OnResumeUtils.getInstance().isOnResumeEnable = true
                        }
                    }
                }
                if (activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    if (OnResumeUtils.getInstance().isInitialized) {
                        OnResumeUtils.getInstance().isOnResumeEnable = false
                    }
                    mRewardedAd?.show(activity) {
                        mRewardedAd = null
                        callback.onRewardEarned()
                    }
                    isAdShowing = true
                } else {
                    mRewardedAd = null
                    dismissAdDialog()
                    isAdShowing = false
                    if (OnResumeUtils.getInstance().isInitialized) {
                        OnResumeUtils.getInstance().isOnResumeEnable = true
                    }
                }
//                    } else {
//                        isAdShowing = false
//                        callback.onRewardFailed("None Show")
//                        dismissAdDialog()
//                        if (AppOpenUtils.getInstance().isInitialized) {
//                            AppOpenUtils.getInstance().isAppResumeEnabled = true
//                        }
//                    }
            }
        })
    }

    private fun performLoadNative(context: Context, holder: AdmobHolder, callback: NativeCallback) {
        if (!isEnableAds || !isNetworkConnected(context)) {
            callback.onNativeFailed("Not load native")
            return
        }
        //If native is loaded return
        if (holder.nativeAd.value != null) {
            Log.d("+===AdmobNative", "Native not null")
            return
        }
        val adId = if (isTesting) {
            context.logId("native_${holder.uid}")
            context.getString(R.string.test_admob_native_id)
        } else {
            RemoteUtils.getAdId("native_${holder.uid}")
        }
        holder.isNativeLoading = true
//        val videoOptions = VideoOptions.Builder().setStartMuted(false).build()
        val adLoader = AdLoader.Builder(context, adId).forNativeAd { nativeAd ->
            holder.isNativeLoading = false
            holder.nativeAd.value = nativeAd
            nativeAd.setOnPaidEventListener { adValue: AdValue? ->
                adValue?.let {
                    AdjustUtils.postRevenueAdjustNative(nativeAd, adValue, adId)
                }
            }
            aggregateErr(nativeAd)
            callback.onNativeReady(nativeAd)
        }.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e("+===AdmobNative", "onAdFailedToLoad" + adError.message)
                Log.e("+===AdmobNative", "errorCodeAds" + adError.cause)
                holder.isNativeLoading = false
                holder.nativeAd.value = null
                callback.onNativeFailed(adError.message)
            }

            override fun onAdClicked() {
                super.onAdClicked()
                callback.onNativeClicked()
            }
        }).withNativeAdOptions(NativeAdOptions.Builder().build()).build()
        if (adRequest != null) {
            adLoader.loadAd(adRequest!!)
        }
    }

    private fun performShowNative(
        activity: Activity, viewGroup: ViewGroup, holder: AdmobHolder, layout: Int, callback: NativeCallbackSimple
    ) {
        if (!isEnableAds || !isNetworkConnected(activity)) {
            viewGroup.gone()
            return
        }
        shimmerFrameLayout?.stopShimmer()
        runCatching {
            viewGroup.removeAllViews()
        }

        if (!holder.isNativeLoading) {
            if (holder.nativeAd.value != null) {
                val adView = activity.layoutInflater.inflate(layout, null) as NativeAdView
                populateNativeAdView(holder.nativeAd.value!!, adView, holder.nativeSize)
                shimmerFrameLayout?.stopShimmer()
                holder.nativeAd.removeObservers((activity as LifecycleOwner))
                runCatching {
                    viewGroup.removeAllViews()
                    viewGroup.addView(adView)
                }

                callback.onNativeLoaded()
            } else {
                shimmerFrameLayout?.stopShimmer()
                viewGroup.gone()
                holder.nativeAd.removeObservers((activity as LifecycleOwner))
                callback.onNativeFailed("None Show")
            }
        } else {
            val loadingLayout = if (holder.nativeSize == AdNativeSize.MEDIUM) {
                R.layout.layout_native_loading_medium
            } else if (holder.tinyLoading) {
                R.layout.layout_banner_loading
            } else {
                R.layout.layout_native_loading_small
            }
            val tagView = activity.layoutInflater.inflate(loadingLayout, null, false)
            runCatching {
                viewGroup.addView(tagView, 0)
            }
            val adId = if (isTesting) {
                activity.logId("native_${holder.uid}")
                activity.getString(R.string.test_admob_native_id)
            } else {
                RemoteUtils.getAdId("native_${holder.uid}")
            }
            if (shimmerFrameLayout == null) {
                shimmerFrameLayout = tagView.findViewById(R.id.shimmer_view_container)
                shimmerFrameLayout?.startShimmer()
            }
            holder.nativeAd.observe((activity as LifecycleOwner)) { nativeAd ->
                if (nativeAd != null) {
                    nativeAd.setOnPaidEventListener {
                        AdjustUtils.postRevenueAdjustNative(nativeAd, it, adId)
                    }
                    val adView = activity.layoutInflater.inflate(layout, null) as NativeAdView
                    populateNativeAdView(nativeAd, adView, holder.nativeSize)
                    shimmerFrameLayout?.stopShimmer()
                    runCatching {
                        viewGroup.removeAllViews()
                        viewGroup.addView(adView)
                    }

                    callback.onNativeLoaded()
                    holder.nativeAd.removeObservers((activity as LifecycleOwner))
                } else {
                    shimmerFrameLayout?.stopShimmer()
                    viewGroup.gone()
                    callback.onNativeFailed("None Show")
                    holder.nativeAd.removeObservers((activity as LifecycleOwner))
                }
            }
        }
    }

    private fun performLoadAndShowNative(
        activity: Activity, viewGroup: ViewGroup, holder: AdmobHolder,
        layout: Int, adCallback: NativeCallback, reConstraint: Boolean? = false
    ) {
        Log.d("+===AdmobNative", "loadAndShowNative")
        if (!isEnableAds || !isNetworkConnected(activity)) {
            viewGroup.gone()
            return
        }
//        VideoOptions.Builder().setStartMuted(false).build()

        val loadingLayout = if (holder.nativeSize == AdNativeSize.MEDIUM) {
            R.layout.layout_native_loading_medium
        } else if (holder.tinyLoading) {
            R.layout.layout_banner_loading
        } else {
            R.layout.layout_native_loading_small
        }
        val tagView = activity.layoutInflater.inflate(loadingLayout, null, false)
        runCatching {
            viewGroup.removeAllViews()
            viewGroup.addView(tagView, 0)
        }

        val shimmerFrameLayout = tagView.findViewById<ShimmerFrameLayout>(R.id.shimmer_view_container)
        shimmerFrameLayout.startShimmer()

        val adId = if (isTesting) {
            activity.logId("native_${holder.uid}")
            activity.getString(R.string.test_admob_native_id)
        } else {
            RemoteUtils.getAdId("native_${holder.uid}")
        }
        val adLoader = AdLoader.Builder(activity, adId).forNativeAd { nativeAd ->
            adCallback.onNativeReady(nativeAd)
            val adView = activity.layoutInflater.inflate(layout, null) as NativeAdView
            populateNativeAdView(nativeAd, adView, holder.nativeSize)
            if (reConstraint == true) NativeHelper.reConstraintNativeCollapView(adView)
            shimmerFrameLayout.stopShimmer()
            aggregateErr(nativeAd)
            runCatching {
                viewGroup.removeAllViews()
                viewGroup.addView(adView)
            }

            nativeAd.setOnPaidEventListener { adValue: AdValue ->
                AdjustUtils.postRevenueAdjustNative(nativeAd, adValue, adId)
            }
            //viewGroup.setVisibility(View.VISIBLE);
        }.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e("+===AdmobNative", "onAdFailedToLoad" + adError.message)
                Log.e("+===AdmobNative", "errorCodeAds" + adError.cause)
                shimmerFrameLayout.stopShimmer()
                runCatching {
                    viewGroup.removeAllViews()
                    viewGroup.gone()
                }
                adCallback.onNativeFailed(adError.message)
            }

            override fun onAdClicked() {
                super.onAdClicked()
                adCallback.onNativeClicked()
            }
        }).withNativeAdOptions(NativeAdOptions.Builder().build()).build()
        adRequest?.let { adLoader.loadAd(it) }
        Log.e("+===AdmobNative", "loadAdNativeAds")
    }

    private fun performLoadAndShowNativeCollap(
        activity: Activity, holder: AdmobHolder, viewGroup: ViewGroup,
        layoutCollap: Int, layoutSmall: Int?, callback: NativeCallback
    ) {
        var decorView: ViewGroup? = null
        val tag = "native_collap_view"
        runCatching {
            decorView = activity.window.decorView as ViewGroup
            decorView!!.findViewWithTag<View>(tag)?.let { decorView!!.removeView(it) }
        }

        if (!isEnableAds || !isNetworkConnected(activity)) {
            viewGroup.gone()
            callback.onNativeFailed("Not show native")
            return
        }
        if (isNativeInterShowing(activity)) {
            viewGroup.gone()
            callback.onNativeFailed("Native Inter is showing")
            return
        }
//        val videoOptions = VideoOptions.Builder().setStartMuted(false).setCustomControlsRequested(false).build()
//        val adOptions = NativeAdOptions.Builder().setMediaAspectRatio(holder.mediaAspectRatio).setVideoOptions(videoOptions).build()
        val tagView = activity.layoutInflater.inflate(R.layout.layout_banner_loading, null, false)
        runCatching {
            viewGroup.removeAllViews()
            viewGroup.addView(tagView, 0)
        }

        viewGroup.visible()
        val shimmerFrameLayout = tagView.findViewById<ShimmerFrameLayout>(R.id.shimmer_view_container)
        shimmerFrameLayout.startShimmer()

        val adId = if (isTesting) {
            activity.logId("native_${holder.uid}")
            activity.getString(R.string.test_admob_native_id)
//            activity.getString(R.string.test_admob_native_full_screen_id)
        } else {
            RemoteUtils.getAdId("native_${holder.uid}")
        }
        val adLoader = AdLoader.Builder(activity, adId).forNativeAd { nativeAd ->
            callback.onNativeReady(nativeAd)
            val adViewCollap = activity.layoutInflater.inflate(layoutCollap, null) as NativeAdView
            adViewCollap.tag = tag
            aggregateErr(nativeAd)
            populateNativeAdViewCollap(nativeAd, adViewCollap, holder.nativeSize, holder.anchor) {
                runCatching { //* On icon collapse clicked
                    decorView?.removeView(adViewCollap)
                    if (layoutSmall == null) {
                        NativeHelper.reConstraintNativeCollapView(adViewCollap)
                        viewGroup.addView(adViewCollap)
                    }
                }
            }
            shimmerFrameLayout.stopShimmer()
            runCatching {
                viewGroup.removeView(tagView)
                val layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    gravity = if (holder.anchor == "top") Gravity.TOP else Gravity.BOTTOM
                }
                if (isNativeInterShowing(activity)) {
                    if (layoutSmall == null) {
                        NativeHelper.reConstraintNativeCollapView(adViewCollap)
                        viewGroup.addView(adViewCollap)
                    }
                } else {
                    decorView?.addView(adViewCollap, layoutParams)
                }
                if (layoutSmall != null) {
                    val adViewSmall = activity.layoutInflater.inflate(layoutSmall, null) as NativeAdView
                    populateNativeAdView(nativeAd, adViewSmall, AdNativeSize.SMALL)
                    viewGroup.addView(adViewSmall)
                }
            }.onFailure {
                logE(it.message.toString())
            }

            nativeAd.setOnPaidEventListener { adValue: AdValue ->
                AdjustUtils.postRevenueAdjustNative(nativeAd, adValue, adId)
            }
        }.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e("+===AdmobNative", "onAdFailedToLoad" + adError.message)
                Log.e("+===AdmobNative", "errorCodeAds" + adError.cause)
                shimmerFrameLayout.stopShimmer()
                runCatching {
                    viewGroup.removeAllViews()
                    viewGroup.gone()
                }
                callback.onNativeFailed(adError.message)
            }

            override fun onAdClicked() {
                super.onAdClicked()
                callback.onNativeClicked()
            }

        }).withNativeAdOptions(NativeAdOptions.Builder().build()).build()
        adRequest?.let { adLoader.loadAd(it) }
    }

    private fun performLoadAndShowNativeFull(
        activity: Activity,
        viewGroup: ViewGroup,
        holder: AdmobHolder,
        layout: Int,
        callback: NativeCallbackSimple
    ) {
        if (!isEnableAds || !isNetworkConnected(activity)) {
            viewGroup.gone()
            callback.onNativeFailed("Not show native")
            return
        }
        val tagView = activity.layoutInflater.inflate(R.layout.layout_native_loading_full, null, false)
        runCatching {
            viewGroup.removeAllViews()
            viewGroup.addView(tagView, 0)
        }

        OnResumeUtils.getInstance().isOnResumeEnable = false
        viewGroup.visible()
        viewGroup.isClickable = true
        shimmerFrameLayout = tagView.findViewById(R.id.shimmer_view_container)
        shimmerFrameLayout?.startShimmer()

        val adId = if (holder.isNativeInter) {
            if (isTesting) {
                activity.logId("native_inter_${holder.uid}")
                activity.getString(R.string.test_admob_native_id)
            } else {
                RemoteUtils.getAdId("native_inter_${holder.uid}")
            }
        } else {
            if (isTesting) {
                activity.logId("native_${holder.uid}_full")
                activity.getString(R.string.test_admob_native_id)
            } else {
                RemoteUtils.getAdId("native_${holder.uid}_full")
            }
        }
        val adView = activity.layoutInflater.inflate(layout, null) as NativeAdView
        val builder = AdLoader.Builder(activity, adId)
//        val videoOptions = VideoOptions.Builder().setStartMuted(true).setCustomControlsRequested(false).build()
//        val adOptions = NativeAdOptions.Builder().setMediaAspectRatio(holder.mediaAspectRatio).setVideoOptions(videoOptions).build()
        val adOptions = NativeAdOptions.Builder().setMediaAspectRatio(holder.mediaAspectRatio).build()
        builder.withNativeAdOptions(adOptions).forNativeAd { nativeAd ->
            aggregateErr(nativeAd)
            nativeAd.setOnPaidEventListener { adValue: AdValue? ->
                adValue?.let { AdjustUtils.postRevenueAdjustNative(nativeAd, it, adUnit = adId) }
            }
            callback.onNativeLoaded()
            populateNativeAdViewFull(nativeAd, adView)
            runCatching {
                viewGroup.removeAllViews()
                shimmerFrameLayout?.stopShimmer()
                viewGroup.addView(adView)
            }
        }
        builder.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Log.d("+===Admob", loadAdError.toString())
                shimmerFrameLayout?.stopShimmer()
                viewGroup.gone()
                if (OnResumeUtils.getInstance().isInitialized) {
                    OnResumeUtils.getInstance().isOnResumeEnable = true
                }
                callback.onNativeFailed("native failed to load")
            }
        })
        adRequest?.let { builder.build().loadAd(it) }
    }

    private fun performLoadNativeFull(context: Context, holder: AdmobHolder, adCallback: NativeCallback) {
        if (!isEnableAds || !isNetworkConnected(context)) {
            adCallback.onNativeFailed("Not show native")
            return
        }
        if (holder.nativeAd.value != null) {
            Log.d("+===Admob", "Native not null")
            return
        }
        val adId = if (holder.isNativeInter) {
            if (isTesting) {
                context.logId("native_inter_${holder.uid}")
                context.getString(R.string.test_admob_native_id)
//                context.getString(R.string.test_admob_native_full_screen_id)
            } else {
                RemoteUtils.getAdId("native_inter_${holder.uid}")
            }
        } else {
            if (isTesting) {
                context.logId("native_${holder.uid}_full")
                context.getString(R.string.test_admob_native_id)
            } else {
                RemoteUtils.getAdId("native_${holder.uid}_full")
            }
        }
        holder.isNativeLoading = true
//        val videoOptions = VideoOptions.Builder().setCustomControlsRequested(true).build()
        val adOptions = NativeAdOptions.Builder().setMediaAspectRatio(holder.mediaAspectRatio).build()
        val adLoader = AdLoader.Builder(context, adId)
        adLoader.withNativeAdOptions(adOptions)
        adLoader.forNativeAd { nativeAd ->
//            nativeHolder.nativeAd = nativeAd
            holder.isNativeLoading = false
            holder.nativeAd.value = nativeAd
            Log.d("+===Admob", "Native Fullscreen Loaded")
            nativeAd.setOnPaidEventListener { adValue: AdValue? ->
                adValue?.let {
                    AdjustUtils.postRevenueAdjustNative(nativeAd, it, adUnit = adId)
                }
            }
            aggregateErr(nativeAd)
            adCallback.onNativeReady(nativeAd)
        }
        adLoader.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e("+===Admob", "onAdFailedToLoad" + adError.message)
                Log.e("+===Admob", "errorCodeAds" + adError.cause)
//                nativeHolder.nativeAd = null
                holder.isNativeLoading = false
                holder.nativeAd.value = null
                adCallback.onNativeFailed("errorId2_" + adError.message)
            }

            override fun onAdClicked() {
                super.onAdClicked()
                adCallback.onNativeClicked()
            }
        })
        if (adRequest != null) {
            adLoader.build().loadAd(adRequest!!)
        }
    }

    private fun performShowNativeFull(context: Context, viewGroup: ViewGroup, holder: AdmobHolder, layout: Int, callback: NativeCallbackSimple) {
        if (!isEnableAds || !isNetworkConnected(context)) {
            viewGroup.gone()
            return
        }
        shimmerFrameLayout?.stopShimmer()
        viewGroup.visible()
//        OnResumeUtils.getInstance().isOnResumeEnable = false
        viewGroup.removeAllViews()
        val inflater = LayoutInflater.from(context)
        if (!holder.isNativeLoading) {
            if (holder.nativeAd.value != null) {
                val adView = inflater.inflate(layout, null) as NativeAdView
                populateNativeAdViewFull(holder.nativeAd.value!!, adView)
                shimmerFrameLayout?.stopShimmer()
                holder.nativeAd.removeObservers((context as LifecycleOwner))
                viewGroup.removeAllViews()
                viewGroup.addView(adView)
                callback.onNativeLoaded()
            } else {
                shimmerFrameLayout?.stopShimmer()
                viewGroup.gone()
                holder.nativeAd.removeObservers((context as LifecycleOwner))
                callback.onNativeFailed("None Show")
//                OnResumeUtils.getInstance().isOnResumeEnable = true
            }
        } else {
            val tagView = inflater.inflate(R.layout.layout_native_loading_full, null, false)
            viewGroup.addView(tagView, 0)
            if (shimmerFrameLayout == null) shimmerFrameLayout = tagView.findViewById(R.id.shimmer_view_container)
            shimmerFrameLayout?.startShimmer()

            holder.nativeAd.observe((context as LifecycleOwner)) { nativeAd: NativeAd? ->
                if (nativeAd != null) {
                    val adId = if (holder.isNativeInter) {
                        if (isTesting) {
                            context.logId("native_inter_${holder.uid}")
                            ""
                        } else {
                            RemoteUtils.getAdId("native_inter_${holder.uid}")
                        }
                    } else {
                        if (isTesting) {
                            context.logId("native_${holder.uid}_full")
                            ""
                        } else {
                            RemoteUtils.getAdId("native_${holder.uid}_full")
                        }
                    }
                    nativeAd.setOnPaidEventListener {
                        AdjustUtils.postRevenueAdjustNative(nativeAd, it, adUnit = adId)
                    }

                    val adView = inflater.inflate(layout, null) as NativeAdView
                    populateNativeAdViewFull(nativeAd, adView)

                    shimmerFrameLayout?.stopShimmer()
                    viewGroup.removeAllViews()
                    viewGroup.addView(adView)

                    callback.onNativeLoaded()
                    holder.nativeAd.removeObservers((context as LifecycleOwner))
                } else {
                    shimmerFrameLayout?.stopShimmer()
                    viewGroup.gone()
                    callback.onNativeFailed("None Show")
//                    OnResumeUtils.getInstance().isOnResumeEnable = true
                    holder.nativeAd.removeObservers((context as LifecycleOwner))
                }
            }
        }
    }

//    fun loadAndShowNativeFullScreenNoShimmer(
//        activity: Activity,
//        id: String,
//        viewGroup: ViewGroup,
//        layout: Int,
//        mediaAspectRatio: Int,
//        listener: NativeFullScreenCallback
//    ) {
//        if (!isEnableAds || !isNetworkConnected(
//                activity
//            )
//        ) {
//            viewGroup.gone()
//            return
//        }
//        var adMobId: String = id
//        if (isTesting) {
//            adMobId = activity.getString(R.string.test_admob_native_full_screen_id)
//        }
//        val adView = activity.layoutInflater.inflate(layout, null) as NativeAdView
//        val builder = AdLoader.Builder(activity, adMobId)
//        val videoOptions =
//            VideoOptions.Builder().setStartMuted(false).setCustomControlsRequested(false).build()
//        val adOptions = NativeAdOptions.Builder().setMediaAspectRatio(mediaAspectRatio)
//            .setVideoOptions(videoOptions).build()
//        builder.withNativeAdOptions(adOptions)
//        builder.forNativeAd { nativeAd ->
//            listener.onLoaded(nativeAd)
//            nativeAd.setOnPaidEventListener { adValue: AdValue? ->
//                adValue?.let {
//                    AdjustUtils.postRevenueAdjustNative(
//                        nativeAd, adValue, id
//                    )
//                }
//            }
//            populateNativeAdView(nativeAd, adView.findViewById(R.id.native_ad_view))
//            try {
//                viewGroup.removeAllViews()
//                viewGroup.addView(adView)
//            } catch (_: Exception) {
//
//            }
//
//        }
//        builder.withAdListener(object : AdListener() {
//            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
//                Log.d("+===AdmobFailed", loadAdError.toString())
//                listener.onLoadFailed()
//            }
//        })
//        if (adRequest != null) {
//            builder.build().loadAd(adRequest!!)
//        }
//    }

//    @JvmStatic
//    fun loadNativeNoButton(
//        activity: Activity,
//        nativeHolder: NativeAdmob,
//        viewGroup: ViewGroup,
//        layout: Int,
//        size: AdNativeSize,
//        adCallback: NativeCallback
//    ) {
//        Log.d("+===Native", "Native1")
//        if (!isEnableAds || !isNetworkConnected(
//                activity
//            )
//        ) {
//            viewGroup.gone()
//            return
//        }
////        val videoOptions =
////            VideoOptions.Builder().setStartMuted(false).build()
//        try {
//            viewGroup.removeAllViews()
//        } catch (_: Exception) {
//
//        }
//
//        var s = nativeHolder.ads
//        val tagView: View = if (size ==AdNativeSize.MEDIUM) {
//            activity.layoutInflater.inflate(R.layout.layout_native_loading_medium, null, false)
//        } else {
//            activity.layoutInflater.inflate(R.layout.layout_native_loading_small, null, false)
//        }
//        try {
//            viewGroup.addView(tagView, 0)
//        } catch (_: Exception) {
//
//        }
//
//        val shimmerFrameLayout =
//            tagView.findViewById<ShimmerFrameLayout>(R.id.shimmer_view_container)
//        shimmerFrameLayout.startShimmer()
//
//        if (isTesting) {
//            s = activity.getString(R.string.test_admob_native_id)
//        }
//        val adLoader = AdLoader.Builder(activity, s).forNativeAd { nativeAd ->
//            adCallback.onNativeLoaded()
//            val adView = activity.layoutInflater.inflate(layout, null) as NativeAdView
//            populateNativeAdViewNoBtn(nativeAd, adView, size)
//            shimmerFrameLayout.stopShimmer()
//            try {
//                viewGroup.removeAllViews()
//                viewGroup.addView(adView)
//            } catch (_: Exception) {
//
//            }
//            nativeAd.setOnPaidEventListener { adValue: AdValue ->
//                adCallback.onPaid(adValue, s)
//            }
//            //viewGroup.setVisibility(View.VISIBLE);
//        }.withAdListener(object : AdListener() {
//            override fun onAdFailedToLoad(adError: LoadAdError) {
//                Log.e("Admobfail", "onAdFailedToLoad" + adError.message)
//                Log.e("Admobfail", "errorCodeAds" + adError.cause)
//                shimmerFrameLayout.stopShimmer()
//                try {
//                    viewGroup.removeAllViews()
//                } catch (_: Exception) {
//
//                }
//                nativeHolder.isLoad = false
//                adCallback.onNativeFailed(adError.message)
//            }
//
//            override fun onAdClicked() {
//                super.onAdClicked()
//                adCallback.onNativeClicked()
//            }
//        }).withNativeAdOptions(NativeAdOptions.Builder().build()).build()
//        if (adRequest != null) {
//            adLoader.loadAd(adRequest!!)
//        }
//        Log.e("Admob", "loadAdNativeAds")
//    }

    //    @JvmStatic
//    fun showNativeNoButton(
//        activity: Activity,
//        nativeHolder: NativeAdmob,
//        viewGroup: ViewGroup,
//        layout: Int,
//        size: AdNativeSize,
//        callback: NativeCallbackSimple
//    ) {
//        if (!isEnableAds || !isNetworkConnected(
//                activity
//            )
//        ) {
//            viewGroup.gone()
//            return
//        }
//        if (shimmerFrameLayout != null) {
//            shimmerFrameLayout?.stopShimmer()
//        }
//        try {
//            viewGroup.removeAllViews()
//        } catch (_: Exception) {
//
//        }
//        if (!nativeHolder.isLoad) {
//            if (nativeHolder.nativeAd != null) {
//                val adView = activity.layoutInflater.inflate(layout, null) as NativeAdView
//                populateNativeAdViewNoBtn(nativeHolder.nativeAd!!, adView, size)
//                if (shimmerFrameLayout != null) {
//                    shimmerFrameLayout?.stopShimmer()
//                }
//                nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
//                try {
//                    viewGroup.removeAllViews()
//                    viewGroup.addView(adView)
//                } catch (_: Exception) {
//
//                }
//                callback.onNativeLoaded()
//            } else {
//                if (shimmerFrameLayout != null) {
//                    shimmerFrameLayout?.stopShimmer()
//                }
//                nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
//                callback.onNativeFailed("None Show")
//            }
//        } else {
//            val tagView: View = if (size ==AdNativeSize.MEDIUM) {
//                activity.layoutInflater.inflate(R.layout.layout_native_loading_medium, null, false)
//            } else {
//                activity.layoutInflater.inflate(R.layout.layout_native_loading_small, null, false)
//            }
//            try {
//                viewGroup.addView(tagView, 0)
//            } catch (_: Exception) {
//
//            }
//
//            if (shimmerFrameLayout == null) shimmerFrameLayout =
//                tagView.findViewById(R.id.shimmer_view_container)
//            shimmerFrameLayout?.startShimmer()
//            nativeHolder.native_mutable.observe((activity as LifecycleOwner)) { nativeAd: NativeAd? ->
//                if (nativeAd != null) {
//                    nativeAd.setOnPaidEventListener {
//                        AdjustUtils.postRevenueAdjustNative(nativeAd, it, nativeHolder.ads)
//                    }
//                    val adView = activity.layoutInflater.inflate(layout, null) as NativeAdView
//                    populateNativeAdViewNoBtn(nativeAd, adView, size)
//                    if (shimmerFrameLayout != null) {
//                        shimmerFrameLayout?.stopShimmer()
//                    }
//                    try {
//                        viewGroup.removeAllViews()
//                        viewGroup.addView(adView)
//                    } catch (_: Exception) {
//
//                    }
//                    callback.onNativeLoaded()
//                    nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
//                } else {
//                    if (shimmerFrameLayout != null) {
//                        shimmerFrameLayout?.stopShimmer()
//                    }
//                    callback.onNativeFailed("None Show")
//                    nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
//                }
//            }
//        }
//    }
//    @JvmStatic
//    fun loadNativeGroup(context: Context, holders: List<AdmobHolder>, callback: NativeCallback) {
//        val key = holders.first()
//        val remoteValue = RemoteUtils.getValue("native_${key.uid}")
//        if (remoteValue == "0") {
//            callback.onNativeFailed("Not load native")
//        } else {
//            holders.forEach { performLoadNative(context, it, callback) }
//        }
//    }
//
//    @JvmStatic
//    fun showNativeGroup(activity: Activity, holders: List<AdmobHolder>, viewGroup: ViewGroup, layout: Int, index: Int, callback: NativeCallbackSimple) {
//        val key = holders.first()
//        val remoteValue = RemoteUtils.getValue("native_${key.uid}")
//        if (remoteValue == "0") {
//            viewGroup.gone()
//        } else {
//            performShowNative(activity, viewGroup, holders[index - 1], layout, callback)
//        }
//    }
    abstract class InterCallback {
        open fun onStartAction() {}
        abstract fun onInterClosed()
        open fun onInterShowed() {}
        open fun onInterLoaded() {}
        abstract fun onInterFailed(error: String)
    }

    abstract class LoadInterCallback {
        open fun onInterLoaded(interstitialAd: InterstitialAd?, isLoading: Boolean) {}
        open fun onInterFailed(error: String) {}
    }

    abstract class BannerCallback {
        open fun onBannerClicked() {}
        open fun onBannerLoaded(adSize: AdSize) {}
        open fun onBannerFailed(error: String) {}
    }

    abstract class NativeCallbackSimple {
        open fun onNativeLoaded() {}
        open fun onNativeFailed(error: String) {}
    }

    abstract class NativeCallback {
        open fun onNativeReady(ad: NativeAd?) {}
        open fun onNativeFailed(error: String) {}
        open fun onNativeClicked() {}
    }

    abstract class RewardCallback {
        abstract fun onRewardClosed()
        open fun onRewardShowed() {}
        open fun onRewardFailed(error: String) {}
        abstract fun onRewardEarned()
    }

    abstract class LoadRewardCallback {
        open fun onRewardFailed(error: String) {}
        open fun onRewardLoaded() {}
    }

}
