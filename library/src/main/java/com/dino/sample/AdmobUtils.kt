package com.dino.sample

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.dino.sample.NativeHelper.Companion.populateNativeAdView
import com.dino.sample.NativeHelper.Companion.populateNativeAdViewClose
import com.dino.sample.NativeHelper.Companion.populateNativeAdViewNoBtn
import com.dino.sample.adjust.AdjustUtils
import com.dino.sample.utils.SweetAlert.SweetAlertDialog
import com.dino.sample.utils.admod.BannerHolderAdmob
import com.dino.sample.utils.admod.InterHolderAdmob
import com.dino.sample.utils.admod.NativeHolderAdmob
import com.dino.sample.utils.admod.RewardHolderAdmob
import com.dino.sample.utils.admod.RewardedInterstitialHolderAdmob
import com.dino.sample.utils.admod.remote.BannerPlugin
import com.airbnb.lottie.LottieAnimationView
import com.dino.ads.R
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
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.vapp.admoblibrary.ads.remote.BannerRemoteConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Date
import java.util.Locale

object AdmobUtils {
    private var dialog: SweetAlertDialog? = null
    var dialogFullScreen: Dialog? = null

    // Biến check lần cuối hiển thị quảng cáo
    var lastTimeShowInterstitial: Long = 0

    // Timeout init admob
    var timeOut = 0

    //Check quảng cáo đang show hay không
    @JvmField
    var isAdShowing = false
    var isClick = false

    //Ẩn hiện quảng cáo
    @JvmField
    var isShowAds = true

    //Dùng ID Test để hiển thị quảng cáo
    @JvmField
    var isTesting = false

    //List device test
    var testDevices: MutableList<String> = ArrayList()
    var deviceId = ""

    @JvmField
    var mRewardedAd: RewardedAd? = null
    var mRewardedInterstitialAd: RewardedInterstitialAd? = null
    var mInterstitialAd: InterstitialAd? = null
    var shimmerFrameLayout: ShimmerFrameLayout? = null

    //id thật
    var idIntersitialReal: String? = null

    var adRequest: AdRequest? = null

    @JvmStatic
    fun initAdmob(context: Context, timeout: Int, isDebug: Boolean, isEnableAds: Boolean) {
        timeOut = timeout
        if (timeOut < 5000 && timeout != 0) {
            Toast.makeText(context, "Nên để limit time ~10000", Toast.LENGTH_LONG).show()
        }
        timeOut = if (timeout > 0) {
            timeout
        } else {
            10000
        }
        isTesting = isDebug
        isShowAds = isEnableAds
        MobileAds.initialize(context) { initializationStatus: InitializationStatus? -> }
        initListIdTest()
        val requestConfiguration = RequestConfiguration.Builder()
            .setTestDeviceIds(testDevices)
            .build()
        MobileAds.setRequestConfiguration(requestConfiguration)
        initAdRequest(timeout)
    }

    @JvmStatic
    private fun initAdRequest(timeOut: Int) {
        adRequest = AdRequest.Builder()
            .setHttpTimeoutMillis(timeOut)
            .build()
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
    fun loadBanner(
        activity: Activity,
        adId: String?,
        viewGroup: ViewGroup,
        bannerAdCallback: BannerCallback
    ) {
        var bannerId = adId
        if (!isShowAds || !isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            bannerAdCallback.onBannerFailed("None Show")
            return
        }
        val mAdView = AdView(activity)
        if (isTesting) {
            bannerId = activity.getString(R.string.test_ads_admob_banner_id)
        }
        mAdView.adUnitId = bannerId!!
        val adSize = getBannerSize(activity)
        mAdView.setAdSize(adSize)
        val tagView = activity.layoutInflater.inflate(R.layout.layoutbanner_loading, null, false)

        try {
            viewGroup.removeAllViews()
            viewGroup.addView(tagView, 0)
            viewGroup.addView(mAdView, 1)
        } catch (_: Exception) {

        }
        shimmerFrameLayout = tagView.findViewById(R.id.shimmer_view_container)
        shimmerFrameLayout?.startShimmer()
        mAdView.onPaidEventListener =
            OnPaidEventListener { adValue ->
                AdjustUtils.postRevenueAdjust(
                    adValue,
                    mAdView.adUnitId
                )
            }
        mAdView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                shimmerFrameLayout?.stopShimmer()
                viewGroup.removeView(tagView)
                bannerAdCallback.onBannerLoaded()
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(" Admod", "failloadbanner" + adError.message)
                shimmerFrameLayout?.stopShimmer()
                viewGroup.removeView(tagView)
                bannerAdCallback.onBannerFailed(adError.message)
            }

            override fun onAdOpened() {}
            override fun onAdClicked() {
                bannerAdCallback.onBannerClicked()
            }

            override fun onAdClosed() {
                // Code to be executed when the user is about to return
                // to the app after tapping on an ad.
            }
        }
        if (adRequest != null) {
            mAdView.loadAd(adRequest!!)
        }
        Log.e(" Admod", "loadAdBanner")
    }

    @JvmStatic
    fun loadBannerCollap(
        activity: Activity,
        bannerId: String?,
        bannerCollapAnchor: BannerCollapAnchor,
        viewGroup: ViewGroup,
        callback: BannerCollapCallback
    ) {
        var bannerId = bannerId
        if (!isShowAds || !isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
        val mAdView = AdView(activity)
        if (isTesting) {
            bannerId = activity.getString(R.string.test_ads_admob_banner_collapsible_id)
        }
        mAdView.adUnitId = bannerId!!
        val adSize = getBannerSize(activity)
        mAdView.setAdSize(adSize)
        val tagView = activity.layoutInflater.inflate(R.layout.layoutbanner_loading, null, false)
        try {
            viewGroup.removeAllViews()
            viewGroup.addView(tagView, 0)
            viewGroup.addView(mAdView, 1)
        } catch (_: Exception) {

        }
        shimmerFrameLayout = tagView.findViewById(R.id.shimmer_view_container)
        shimmerFrameLayout?.startShimmer()

        mAdView?.adListener = object : AdListener() {
            override fun onAdLoaded() {
                mAdView.onPaidEventListener =
                    OnPaidEventListener { adValue ->
                        AdjustUtils.postRevenueAdjust(
                            adValue,
                            mAdView.adUnitId
                        )
                    }
                shimmerFrameLayout?.stopShimmer()
                viewGroup.removeView(tagView)
                callback.onBannerLoaded(adSize)
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(" Admod", "failloadbanner" + adError.message)
                shimmerFrameLayout?.stopShimmer()
                viewGroup.removeView(tagView)
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
        var anchored = "top"
        anchored = if (bannerCollapAnchor === BannerCollapAnchor.TOP) {
            "top"
        } else {
            "bottom"
        }
        extras.putString("collapsible", anchored)
        val adRequest2 =
            AdRequest.Builder().addNetworkExtrasBundle(AdMobAdapter::class.java, extras).build()
        mAdView.loadAd(adRequest2)
        Log.e(" Admod", "loadAdBanner")
    }

    @JvmStatic
    fun loadBannerCollap(
        activity: Activity,
        banner: BannerHolderAdmob,
        bannerCollapAnchor: BannerCollapAnchor,
        viewGroup: ViewGroup,
        callback: BannerCollapCallback
    ) {
        var bannerId = banner.ads
        if (!isShowAds || !isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
        banner.mAdView?.destroy()
        banner.mAdView?.let {
            viewGroup.removeView(it)
        }
        banner.mAdView = AdView(activity)
        if (isTesting) {
            bannerId = activity.getString(R.string.test_ads_admob_banner_collapsible_id)
        }
        banner.mAdView?.adUnitId = bannerId
        val tagView = activity.layoutInflater.inflate(R.layout.layoutbanner_loading, null, false)
        try {
            viewGroup.removeAllViews()
            viewGroup.addView(tagView, 0)
            viewGroup.addView(banner.mAdView, 1)
        } catch (_: Exception) {

        }
        val adSize = getBannerSize(activity)
        banner.mAdView?.setAdSize(adSize)
        shimmerFrameLayout = tagView.findViewById(R.id.shimmer_view_container)
        shimmerFrameLayout?.startShimmer()

        banner.mAdView?.adListener = object : AdListener() {
            override fun onAdLoaded() {
                banner.mAdView?.onPaidEventListener =
                    OnPaidEventListener { adValue ->
                        AdjustUtils.postRevenueAdjust(
                            adValue,
                            banner.mAdView?.adUnitId
                        )
                    }
                shimmerFrameLayout?.stopShimmer()
                viewGroup.removeView(tagView)
                callback.onBannerLoaded(adSize)
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(" Admod", "failloadbanner" + adError.message)
                shimmerFrameLayout?.stopShimmer()
                viewGroup.removeView(tagView)
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
        val anchored = if (bannerCollapAnchor === BannerCollapAnchor.TOP) {
            "top"
        } else {
            "bottom"
        }
        extras.putString("collapsible", anchored)
        val adRequest2 =
            AdRequest.Builder().addNetworkExtrasBundle(AdMobAdapter::class.java, extras).build()
        banner.mAdView?.loadAd(adRequest2)
        Log.e(" Admod", "loadAdBanner")
    }

    @JvmStatic
    fun loadAndShowBannerCollap(
        activity: Activity,
        id: String,
        refreshRateSec: Int,
        cbFetchIntervalSec: Int,
        view: ViewGroup,
        size: BannerAnchor,
        bannerAdCallback: BannerCollapCallback
    ) {
        var bannerPlugin: BannerPlugin? = null
        val type = if (size == BannerAnchor.TOP) {
            "collapsible_top"
        } else {
            "collapsible_bottom"
        }
        val bannerConfig = BannerPlugin.BannerConfig(id, type, refreshRateSec, cbFetchIntervalSec)
        bannerPlugin = bannerConfig.adUnitId?.let {
            BannerPlugin(
                activity, view, it, bannerConfig, object : BannerRemoteConfig {
                    override fun onBannerAdLoaded(adSize: AdSize?) {
                        adSize?.let { it1 -> bannerAdCallback.onBannerLoaded(it1) }
                    }

                    override fun onAdFail() {
                        Log.d("===Banner", "Banner2")
                        bannerAdCallback.onBannerFailed("Banner Failed")
                    }

                    override fun onAdPaid(adValue: AdValue, mAdView: AdView) {
                        AdjustUtils.postRevenueAdjust(adValue, mAdView.adUnitId)
                    }
                })
        }
    }

    /**
     * Example: AdmobUtils.loadAndShowBanner(this,"id",5,10,binding.bannerContainer,
     *                 BannerConfig.TYPE_ADAPTIVE,object : AdmobUtils.BannerCollapCallback{})
     */
    @JvmStatic
    fun loadAndShowBanner(
        activity: Activity,
        id: String, refreshRateSec: Int, cbFetchIntervalSec: Int, view: ViewGroup, size: String,
        bannerAdCallback: BannerCollapCallback
    ) {
        var bannerPlugin: BannerPlugin? = null
        val bannerConfig = BannerPlugin.BannerConfig(id, size, refreshRateSec, cbFetchIntervalSec)
        bannerPlugin = bannerConfig.adUnitId?.let {
            BannerPlugin(
                activity, view, it, bannerConfig, object : BannerRemoteConfig {
                    override fun onBannerAdLoaded(adSize: AdSize?) {
                        adSize?.let { it1 -> bannerAdCallback.onBannerLoaded(it1) }
                        shimmerFrameLayout?.stopShimmer()
                        Log.d("===Banner==", "reload banner")
                    }

                    override fun onAdFail() {
                        Log.d("===Banner", "Banner2")
                        shimmerFrameLayout?.stopShimmer()
                        bannerAdCallback.onBannerFailed("Banner Failed")
                    }

                    override fun onAdPaid(adValue: AdValue, mAdView: AdView) {
                        AdjustUtils.postRevenueAdjust(adValue, mAdView.adUnitId)
                    }
                })
        }
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

    /**
     * Example: AdmobUtils.loadInterstitial(context,interHolder,
     *             object :AdmobUtils.LoadInterCallback {})
     */
    @JvmStatic
    fun loadInterstitial(
        context: Context,
        interHolder: InterHolderAdmob,
        adLoadCallback: LoadInterCallback
    ) {
        isAdShowing = false
        if (!isShowAds || !isNetworkConnected(context)) {
            adLoadCallback.onInterFailed("None Show")
            return
        }
        if (interHolder.inter != null) {
            Log.d("===AdsInter", "inter not null")
            return
        }
        interHolder.check = true
        if (adRequest == null) {
            initAdRequest(timeOut)
        }
        if (isTesting) {
            interHolder.ads = context.getString(R.string.test_ads_admob_inter_id)
        }
        idIntersitialReal = interHolder.ads
        InterstitialAd.load(
            context,
            idIntersitialReal!!,
            adRequest!!,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    if (isClick) {
                        interHolder.mutable.value = interstitialAd
                    }
                    interHolder.inter = interstitialAd
                    interHolder.check = false
                    interHolder.inter!!.setOnPaidEventListener { adValue ->
                        AdjustUtils.postRevenueAdjustInter(
                            interHolder.inter!!,
                            adValue,
                            interHolder.inter!!.adUnitId
                        )
                        adLoadCallback.onPaid(adValue, interHolder.inter!!.adUnitId)
                    }
                    adLoadCallback.onInterLoaded(interstitialAd, false)
                    Log.i("adLog", "onAdLoaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isAdShowing = false
                    if (mInterstitialAd != null) {
                        mInterstitialAd = null
                    }
                    interHolder.check = false
                    if (isClick) {
                        interHolder.mutable.value = null
                    }
                    adLoadCallback.onInterFailed(loadAdError.message)
                }
            })
    }

    //Load Inter 2 in here if inter 1 false

    //Show Inter in here
    /**
     * Example: AdmobUtils.showInterstitial(activity,interHolder,10000,
     *          object :AdmobUtils.InterCallback {})
     */
    @JvmStatic
    fun showInterstitial(
        activity: Activity,
        interHolder: InterHolderAdmob,
        timeout: Long,
        adCallback: InterCallback?,
        enableLoadingDialog: Boolean
    ) {
        isClick = true
        //Check internet
        if (!isShowAds || !isNetworkConnected(activity)) {
            isAdShowing = false
            if (AppOpenUtils.getInstance().isInitialized) {
                AppOpenUtils.getInstance().isAppResumeEnabled = true
            }
            adCallback?.onInterFailed("No internet")
            return
        }
        adCallback?.onInterLoaded()
        val handler = Handler(Looper.getMainLooper())
        //Check timeout show inter
        val runnable = Runnable {
            if (interHolder.check) {
                if (AppOpenUtils.getInstance().isInitialized) {
                    AppOpenUtils.getInstance().isAppResumeEnabled = true
                }
                isClick = false
                interHolder.mutable.removeObservers((activity as LifecycleOwner))
                isAdShowing = false
                dismissAdDialog()
                adCallback?.onInterFailed("timeout")
            }
        }
        handler.postDelayed(runnable, timeout)
        //Inter is Loading...
        if (interHolder.check) {
            if (enableLoadingDialog) {
                dialogLoading(activity)
            }
            interHolder.mutable.observe((activity as LifecycleOwner)) { aBoolean: InterstitialAd? ->
                if (aBoolean != null) {
                    interHolder.mutable.removeObservers((activity as LifecycleOwner))
                    isClick = false
                    Handler(Looper.getMainLooper()).postDelayed({
                        Log.d("===DelayLoad", "delay")

                        aBoolean.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                isAdShowing = false
                                if (AppOpenUtils.getInstance().isInitialized) {
                                    AppOpenUtils.getInstance().isAppResumeEnabled = true
                                }
                                isClick = false
                                //Set inter = null
                                interHolder.inter = null
                                interHolder.mutable.removeObservers((activity as LifecycleOwner))
                                interHolder.mutable.value = null
                                adCallback?.onDismissedInter()
                                dismissAdDialog()
                                Log.d("TAG", "The ad was dismissed.")
                            }

                            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                isAdShowing = false
                                if (AppOpenUtils.getInstance().isInitialized) {
                                    AppOpenUtils.getInstance().isAppResumeEnabled = true
                                }
                                isClick = false
                                isAdShowing = false
                                //Set inter = null
                                interHolder.inter = null
                                dismissAdDialog()
                                Log.e("Admodfail", "onAdFailedToLoad" + adError.message)
                                Log.e("Admodfail", "errorCodeAds" + adError.cause)
                                interHolder.mutable.removeObservers((activity as LifecycleOwner))
                                interHolder.mutable.value = null
                                handler.removeCallbacksAndMessages(null)
                                adCallback?.onInterFailed(adError.message)
                            }

                            override fun onAdShowedFullScreenContent() {
                                handler.removeCallbacksAndMessages(null)
                                isAdShowing = true
                                adCallback?.onInterShowed()

                            }
                        }
                        showInterstitial(activity, aBoolean, adCallback)
                    }, 400)
                } else {
                    interHolder.check = true
                }
            }
            return
        }
        //Load inter done
        if (interHolder.inter == null) {
            if (adCallback != null) {
                isAdShowing = false
                if (AppOpenUtils.getInstance().isInitialized) {
                    AppOpenUtils.getInstance().isAppResumeEnabled = true
                }
                adCallback.onInterFailed("inter null")
                handler.removeCallbacksAndMessages(null)
            }
        } else {
            if (enableLoadingDialog) {
                dialogLoading(activity)
            }
            Handler(Looper.getMainLooper()).postDelayed({
                interHolder.inter?.fullScreenContentCallback =
                    object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            isAdShowing = false
                            if (AppOpenUtils.getInstance().isInitialized) {
                                AppOpenUtils.getInstance().isAppResumeEnabled = true
                            }
                            isClick = false
                            interHolder.mutable.removeObservers((activity as LifecycleOwner))
                            interHolder.inter = null
                            adCallback?.onDismissedInter()
                            dismissAdDialog()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            isAdShowing = false
                            if (AppOpenUtils.getInstance().isInitialized) {
                                AppOpenUtils.getInstance().isAppResumeEnabled = true
                            }
                            handler.removeCallbacksAndMessages(null)
                            isClick = false
                            interHolder.inter = null
                            interHolder.mutable.removeObservers((activity as LifecycleOwner))
                            isAdShowing = false
                            dismissAdDialog()
                            adCallback?.onInterFailed(adError.message)
                            Log.e("Admodfail", "onAdFailedToLoad" + adError.message)
                            Log.e("Admodfail", "errorCodeAds" + adError.cause)
                        }

                        override fun onAdShowedFullScreenContent() {
                            handler.removeCallbacksAndMessages(null)
                            isAdShowing = true
                            adCallback?.onInterShowed()
                        }
                    }
                showInterstitial(activity, interHolder.inter, adCallback)
            }, 400)
        }
    }

    @JvmStatic
    private fun showInterstitial(
        activity: Activity,
        mInterstitialAd: InterstitialAd?,
        callback: InterCallback?
    ) {
        if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && mInterstitialAd != null) {
            isAdShowing = true
            Handler(Looper.getMainLooper()).postDelayed({
                callback?.onStartAction()

                mInterstitialAd.show(activity)
            }, 400)
        } else {
            isAdShowing = false
            if (AppOpenUtils.getInstance().isInitialized) {
                AppOpenUtils.getInstance().isAppResumeEnabled = true
            }
            dismissAdDialog()
            callback?.onInterFailed("onResume")
        }
    }

    @JvmStatic
    fun dismissAdDialog() {
        try {
            if (dialog != null && dialog!!.isShowing) {
                dialog!!.dismiss()
            }
            if (dialogFullScreen != null && dialogFullScreen?.isShowing == true) {
                dialogFullScreen?.dismiss()
            }
        } catch (_: Exception) {

        }
    }

    @JvmStatic
    fun loadAndShowRewarded(
        activity: Activity,
        admobId: String?,
        adCallback2: RewardedCallback,
        enableLoadingDialog: Boolean
    ) {
        var admobId = admobId
        mInterstitialAd = null
        isAdShowing = false
        if (!isShowAds || !isNetworkConnected(activity)) {
            adCallback2.onRewardClosed()
            return
        }
        if (adRequest == null) {
            initAdRequest(timeOut)
        }
        if (isTesting) {
            admobId = activity.getString(R.string.test_ads_admob_reward_id)
        }
        if (enableLoadingDialog) {
            dialogLoading(activity)
        }
        isAdShowing = false
        if (AppOpenUtils.getInstance().isInitialized) {
            AppOpenUtils.getInstance().isAppResumeEnabled = false
        }
        RewardedAd.load(activity, admobId!!,
            adRequest!!, object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    // Handle the error.
                    mRewardedAd = null
                    adCallback2.onRewardFailed(loadAdError.message)
                    dismissAdDialog()
                    if (AppOpenUtils.getInstance().isInitialized) {
                        AppOpenUtils.getInstance().isAppResumeEnabled = true
                    }
                    isAdShowing = false
                    Log.e("Admodfail", "onAdFailedToLoad" + loadAdError.message)
                    Log.e("Admodfail", "errorCodeAds" + loadAdError.cause)
                }

                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    mRewardedAd = rewardedAd
                    if (mRewardedAd != null) {
                        mRewardedAd?.setOnPaidEventListener {
                            AdjustUtils.postRevenueAdjust(
                                it,
                                mRewardedAd?.adUnitId
                            )
                        }
                        mRewardedAd?.fullScreenContentCallback =
                            object : FullScreenContentCallback() {
                                override fun onAdShowedFullScreenContent() {
                                    isAdShowing = true
                                    adCallback2.onRewardShowed()
                                    if (AppOpenUtils.getInstance().isInitialized) {
                                        AppOpenUtils.getInstance().isAppResumeEnabled = false
                                    }
                                }

                                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                    // Called when ad fails to show.
                                    if (adError.code != 1) {
                                        isAdShowing = false
                                        adCallback2.onRewardFailed(adError.message)
                                        mRewardedAd = null
                                        dismissAdDialog()
                                    }
                                    if (AppOpenUtils.getInstance().isInitialized) {
                                        AppOpenUtils.getInstance().isAppResumeEnabled = true
                                    }
                                    Log.e("Admodfail", "onAdFailedToLoad" + adError.message)
                                    Log.e("Admodfail", "errorCodeAds" + adError.cause)
                                }

                                override fun onAdDismissedFullScreenContent() {
                                    // Called when ad is dismissed.
                                    // Set the ad reference to null so you don't show the ad a second time.
                                    mRewardedAd = null
                                    isAdShowing = false
                                    adCallback2.onRewardClosed()
                                    if (AppOpenUtils.getInstance().isInitialized) {
                                        AppOpenUtils.getInstance().isAppResumeEnabled = true
                                    }
                                }
                            }
                        if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                            if (AppOpenUtils.getInstance().isInitialized) {
                                AppOpenUtils.getInstance().isAppResumeEnabled = false
                            }
                            mRewardedAd?.show(activity) { adCallback2.onRewardEarned() }
                            isAdShowing = true
                        } else {
                            mRewardedAd = null
                            dismissAdDialog()
                            isAdShowing = false
                            if (AppOpenUtils.getInstance().isInitialized) {
                                AppOpenUtils.getInstance().isAppResumeEnabled = true
                            }
                        }
                    } else {
                        isAdShowing = false
                        adCallback2.onRewardFailed("None Show")
                        dismissAdDialog()
                        if (AppOpenUtils.getInstance().isInitialized) {
                            AppOpenUtils.getInstance().isAppResumeEnabled = true
                        }
                    }
                }
            })
    }

    @JvmStatic
    fun loadAndShowRewardedInter(
        activity: Activity,
        admobId: String?,
        adCallback2: RewardedCallback,
        enableLoadingDialog: Boolean
    ) {
        var admobId = admobId
        mInterstitialAd = null
        isAdShowing = false
        if (!isShowAds || !isNetworkConnected(activity)) {
            adCallback2.onRewardClosed()
            return
        }
        if (adRequest == null) {
            initAdRequest(timeOut)
        }
        if (isTesting) {
            admobId = activity.getString(R.string.test_ads_admob_inter_reward_id)
        }
        if (enableLoadingDialog) {
            dialogLoading(activity)
        }
        isAdShowing = false
        if (AppOpenUtils.getInstance().isInitialized) {
            AppOpenUtils.getInstance().isAppResumeEnabled = false
        }
        RewardedInterstitialAd.load(activity, admobId!!,
            adRequest!!, object : RewardedInterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    // Handle the error.
                    mRewardedInterstitialAd = null
                    adCallback2.onRewardFailed(loadAdError.message)
                    dismissAdDialog()
                    if (AppOpenUtils.getInstance().isInitialized) {
                        AppOpenUtils.getInstance().isAppResumeEnabled = true
                    }
                    isAdShowing = false
                    Log.e("Admodfail", "onAdFailedToLoad" + loadAdError.message)
                    Log.e("Admodfail", "errorCodeAds" + loadAdError.cause)
                }

                override fun onAdLoaded(rewardedAd: RewardedInterstitialAd) {
                    mRewardedInterstitialAd = rewardedAd
                    if (mRewardedInterstitialAd != null) {
                        mRewardedInterstitialAd?.setOnPaidEventListener {
                            AdjustUtils.postRevenueAdjust(
                                it,
                                mRewardedInterstitialAd?.adUnitId
                            )
                        }
                        mRewardedInterstitialAd?.fullScreenContentCallback =
                            object : FullScreenContentCallback() {
                                override fun onAdShowedFullScreenContent() {
                                    isAdShowing = true
                                    adCallback2.onRewardShowed()
                                    if (AppOpenUtils.getInstance().isInitialized) {
                                        AppOpenUtils.getInstance().isAppResumeEnabled = false
                                    }
                                }

                                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                    // Called when ad fails to show.
                                    if (adError.code != 1) {
                                        isAdShowing = false
                                        adCallback2.onRewardFailed(adError.message)
                                        mRewardedInterstitialAd = null
                                        dismissAdDialog()
                                    }
                                    if (AppOpenUtils.getInstance().isInitialized) {
                                        AppOpenUtils.getInstance().isAppResumeEnabled = true
                                    }
                                    Log.e("Admodfail", "onAdFailedToLoad" + adError.message)
                                    Log.e("Admodfail", "errorCodeAds" + adError.cause)
                                }

                                override fun onAdDismissedFullScreenContent() {
                                    // Called when ad is dismissed.
                                    // Set the ad reference to null so you don't show the ad a second time.
                                    mRewardedInterstitialAd = null
                                    isAdShowing = false
                                    adCallback2.onRewardClosed()
                                    if (AppOpenUtils.getInstance().isInitialized) {
                                        AppOpenUtils.getInstance().isAppResumeEnabled = true
                                    }
                                }
                            }
                        if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                            if (AppOpenUtils.getInstance().isInitialized) {
                                AppOpenUtils.getInstance().isAppResumeEnabled = false
                            }
                            mRewardedInterstitialAd?.show(activity) { adCallback2.onRewardEarned() }
                            isAdShowing = true
                        } else {
                            mRewardedInterstitialAd = null
                            dismissAdDialog()
                            isAdShowing = false
                            if (AppOpenUtils.getInstance().isInitialized) {
                                AppOpenUtils.getInstance().isAppResumeEnabled = true
                            }
                        }
                    } else {
                        isAdShowing = false
                        adCallback2.onRewardFailed("None Show")
                        dismissAdDialog()
                        if (AppOpenUtils.getInstance().isInitialized) {
                            AppOpenUtils.getInstance().isAppResumeEnabled = true
                        }
                    }
                }
            })
    }

    @JvmStatic
    fun loadRewardedInter(
        context: Context,
        mInterstitialRewardAd: RewardedInterstitialHolderAdmob,
        adLoadCallback: RewardedInterCallback
    ) {
        var admobId = mInterstitialRewardAd.ads
        if (!isShowAds || !isNetworkConnected(context)) {
            return
        }
        if (mInterstitialRewardAd.inter != null) {
            Log.d("===AdsInter", "mInterstitialRewardAd not null")
            return
        }
        if (adRequest == null) {
            initAdRequest(timeOut)
        }
        mInterstitialRewardAd.isLoading = true
        if (isTesting) {
            admobId = context.getString(R.string.test_ads_admob_inter_reward_id)
        }
        RewardedInterstitialAd.load(
            context,
            admobId,
            adRequest!!,
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialRewardAd: RewardedInterstitialAd) {
                    mInterstitialRewardAd.inter = interstitialRewardAd
                    mInterstitialRewardAd.mutable.value = interstitialRewardAd
                    mInterstitialRewardAd.isLoading = false
                    adLoadCallback.onRewardLoaded()
                    Log.i("adLog", "onAdLoaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    mInterstitialRewardAd.inter = null
                    mInterstitialRewardAd.isLoading = false
                    mInterstitialRewardAd.mutable.value = null
                    adLoadCallback.onRewardFailed(loadAdError.message)
                }
            })
    }

    @JvmStatic
    fun showRewardedInter(
        activity: Activity, mInterstitialRewardAd: RewardedInterstitialHolderAdmob,
        adCallback: RewardedCallback
    ) {
        if (adRequest == null) {
            initAdRequest(timeOut)
        }
        if (!isShowAds || !isNetworkConnected(activity)) {
            if (AppOpenUtils.getInstance().isInitialized) {
                AppOpenUtils.getInstance().isAppResumeEnabled = true
            }
            adCallback.onRewardFailed("No internet or isShowAds = false")
            return
        }

        if (AppOpenUtils.getInstance().isInitialized) {
            if (!AppOpenUtils.getInstance().isAppResumeEnabled) {
                return
            } else {
                isAdShowing = false
                if (AppOpenUtils.getInstance().isInitialized) {
                    AppOpenUtils.getInstance().isAppResumeEnabled = false
                }
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                if (mInterstitialRewardAd.isLoading) {
                    dialogLoading(activity)
                    delay(800)

                    mInterstitialRewardAd.mutable.observe(activity as LifecycleOwner) { reward: RewardedInterstitialAd? ->
                        reward?.let {
                            mInterstitialRewardAd.mutable.removeObservers((activity as LifecycleOwner))
                            it.setOnPaidEventListener { value ->
                                AdjustUtils.postRevenueAdjust(
                                    value,
                                    mInterstitialRewardAd.inter?.adUnitId
                                )
                            }
                            mInterstitialRewardAd.inter?.fullScreenContentCallback =
                                object : FullScreenContentCallback() {
                                    override fun onAdDismissedFullScreenContent() {
                                        mInterstitialRewardAd.inter = null
                                        mInterstitialRewardAd.mutable.removeObservers((activity as LifecycleOwner))
                                        mInterstitialRewardAd.mutable.value = null
                                        if (AppOpenUtils.getInstance().isInitialized) {
                                            AppOpenUtils.getInstance().isAppResumeEnabled = true
                                        }
                                        isAdShowing = false
                                        dismissAdDialog()
                                        adCallback.onRewardClosed()
                                        Log.d("TAG", "The ad was dismissed.")
                                    }

                                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                        mInterstitialRewardAd.inter = null
                                        mInterstitialRewardAd.mutable.removeObservers((activity as LifecycleOwner))
                                        mInterstitialRewardAd.mutable.value = null
                                        if (AppOpenUtils.getInstance().isInitialized) {
                                            AppOpenUtils.getInstance().isAppResumeEnabled = true
                                        }
                                        isAdShowing = false
                                        dismissAdDialog()
                                        adCallback.onRewardFailed(adError.message)
                                        Log.d("TAG", "The ad failed to show.")
                                    }

                                    override fun onAdShowedFullScreenContent() {
                                        isAdShowing = true
                                        adCallback.onRewardShowed()
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            dismissAdDialog()
                                        }, 800)
                                        Log.d("TAG", "The ad was shown.")
                                    }
                                }
                            it.show(activity) { adCallback.onRewardEarned() }
                        }
                    }
                } else {
                    if (mInterstitialRewardAd.inter != null) {
                        dialogLoading(activity)
                        delay(800)

                        mInterstitialRewardAd.inter?.setOnPaidEventListener {
                            AdjustUtils.postRevenueAdjust(it, mInterstitialRewardAd.inter?.adUnitId)
                        }
                        mInterstitialRewardAd.inter?.fullScreenContentCallback =
                            object : FullScreenContentCallback() {
                                override fun onAdDismissedFullScreenContent() {
                                    mInterstitialRewardAd.inter = null
                                    mInterstitialRewardAd.mutable.removeObservers((activity as LifecycleOwner))
                                    mInterstitialRewardAd.mutable.value = null
                                    if (AppOpenUtils.getInstance().isInitialized) {
                                        AppOpenUtils.getInstance().isAppResumeEnabled = true
                                    }
                                    isAdShowing = false
                                    dismissAdDialog()
                                    adCallback.onRewardClosed()
                                    Log.d("TAG", "The ad was dismissed.")
                                }

                                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                    mInterstitialRewardAd.inter = null
                                    mInterstitialRewardAd.mutable.removeObservers((activity as LifecycleOwner))
                                    mInterstitialRewardAd.mutable.value = null
                                    if (AppOpenUtils.getInstance().isInitialized) {
                                        AppOpenUtils.getInstance().isAppResumeEnabled = true
                                    }
                                    isAdShowing = false
                                    dismissAdDialog()
                                    adCallback.onRewardFailed(adError.message)
                                    Log.d("TAG", "The ad failed to show.")
                                }

                                override fun onAdShowedFullScreenContent() {
                                    isAdShowing = true
                                    adCallback.onRewardShowed()
                                    Log.d("TAG", "The ad was shown.")
                                }
                            }
                        mInterstitialRewardAd.inter?.show(activity) { adCallback.onRewardEarned() }

                    } else {
                        isAdShowing = false
                        adCallback.onRewardFailed("None Show")
                        dismissAdDialog()
                        if (AppOpenUtils.getInstance().isInitialized) {
                            AppOpenUtils.getInstance().isAppResumeEnabled = true
                        }
                        Log.d("TAG", "Ad did not load.")
                    }
                }
            }
        }
    }


    @JvmStatic
    fun loadRewarded(
        context: Context,
        mInterstitialRewardAd: RewardHolderAdmob,
        adLoadCallback: RewardedInterCallback
    ) {
        var admobId = mInterstitialRewardAd.ads
        if (!isShowAds || !isNetworkConnected(context)) {
            return
        }
        if (mInterstitialRewardAd.inter != null) {
            Log.d("===AdsInter", "mInterstitialRewardAd not null")
            return
        }
        if (adRequest == null) {
            initAdRequest(timeOut)
        }
        mInterstitialRewardAd.isLoading = true
        if (isTesting) {
            admobId = context.getString(R.string.test_ads_admob_reward_id)
        }
        RewardedAd.load(
            context,
            admobId,
            adRequest!!,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(interstitialRewardAd: RewardedAd) {
                    mInterstitialRewardAd.inter = interstitialRewardAd
                    mInterstitialRewardAd.mutable.value = interstitialRewardAd
                    mInterstitialRewardAd.isLoading = false
                    adLoadCallback.onRewardLoaded()
                    Log.i("adLog", "onAdLoaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    mInterstitialRewardAd.inter = null
                    mInterstitialRewardAd.isLoading = false
                    mInterstitialRewardAd.mutable.value = null
                    adLoadCallback.onRewardFailed(loadAdError.message)
                }
            })
    }

    @JvmStatic
    fun showRewarded(
        activity: Activity, mInterstitialRewardAd: RewardHolderAdmob,
        adCallback: RewardedCallback
    ) {
        if (adRequest == null) {
            initAdRequest(timeOut)
        }
        if (!isShowAds || !isNetworkConnected(activity)) {
            if (AppOpenUtils.getInstance().isInitialized) {
                AppOpenUtils.getInstance().isAppResumeEnabled = true
            }
            adCallback.onRewardFailed("No internet or isShowAds = false")
            return
        }

        if (AppOpenUtils.getInstance().isInitialized) {
            if (!AppOpenUtils.getInstance().isAppResumeEnabled) {
                return
            } else {
                isAdShowing = false
                if (AppOpenUtils.getInstance().isInitialized) {
                    AppOpenUtils.getInstance().isAppResumeEnabled = false
                }
            }
        }

        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.Main) {
                if (mInterstitialRewardAd.isLoading) {
                    dialogLoading(activity)
                    delay(800)

                    mInterstitialRewardAd.mutable.observe(activity as LifecycleOwner) { reward: RewardedAd? ->
                        reward?.let {
                            mInterstitialRewardAd.mutable.removeObservers((activity as LifecycleOwner))
                            it.setOnPaidEventListener { value ->
                                AdjustUtils.postRevenueAdjust(
                                    value,
                                    mInterstitialRewardAd.inter?.adUnitId
                                )
                            }
                            mInterstitialRewardAd.inter?.fullScreenContentCallback =
                                object : FullScreenContentCallback() {
                                    override fun onAdDismissedFullScreenContent() {
                                        mInterstitialRewardAd.inter = null
                                        mInterstitialRewardAd.mutable.removeObservers((activity as LifecycleOwner))
                                        mInterstitialRewardAd.mutable.value = null
                                        if (AppOpenUtils.getInstance().isInitialized) {
                                            AppOpenUtils.getInstance().isAppResumeEnabled = true
                                        }
                                        isAdShowing = false
                                        dismissAdDialog()
                                        adCallback.onRewardClosed()
                                        Log.d("TAG", "The ad was dismissed.")
                                    }

                                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                        mInterstitialRewardAd.inter = null
                                        mInterstitialRewardAd.mutable.removeObservers((activity as LifecycleOwner))
                                        mInterstitialRewardAd.mutable.value = null
                                        if (AppOpenUtils.getInstance().isInitialized) {
                                            AppOpenUtils.getInstance().isAppResumeEnabled = true
                                        }
                                        isAdShowing = false
                                        dismissAdDialog()
                                        adCallback.onRewardFailed(adError.message)
                                        Log.d("TAG", "The ad failed to show.")
                                    }

                                    override fun onAdShowedFullScreenContent() {
                                        isAdShowing = true
                                        adCallback.onRewardShowed()
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            dismissAdDialog()
                                        }, 800)
                                        Log.d("TAG", "The ad was shown.")
                                    }
                                }
                            it.show(activity) { adCallback.onRewardEarned() }
                        }
                    }
                } else {
                    if (mInterstitialRewardAd.inter != null) {
                        dialogLoading(activity)
                        delay(800)

                        mInterstitialRewardAd.inter?.setOnPaidEventListener {
                            AdjustUtils.postRevenueAdjust(it, mInterstitialRewardAd.inter?.adUnitId)
                        }
                        mInterstitialRewardAd.inter?.fullScreenContentCallback =
                            object : FullScreenContentCallback() {
                                override fun onAdDismissedFullScreenContent() {
                                    mInterstitialRewardAd.inter = null
                                    mInterstitialRewardAd.mutable.removeObservers((activity as LifecycleOwner))
                                    mInterstitialRewardAd.mutable.value = null
                                    if (AppOpenUtils.getInstance().isInitialized) {
                                        AppOpenUtils.getInstance().isAppResumeEnabled = true
                                    }
                                    isAdShowing = false
                                    dismissAdDialog()
                                    adCallback.onRewardClosed()
                                    Log.d("TAG", "The ad was dismissed.")
                                }

                                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                    mInterstitialRewardAd.inter = null
                                    mInterstitialRewardAd.mutable.removeObservers((activity as LifecycleOwner))
                                    mInterstitialRewardAd.mutable.value = null
                                    if (AppOpenUtils.getInstance().isInitialized) {
                                        AppOpenUtils.getInstance().isAppResumeEnabled = true
                                    }
                                    isAdShowing = false
                                    dismissAdDialog()
                                    adCallback.onRewardFailed(adError.message)
                                    Log.d("TAG", "The ad failed to show.")
                                }

                                override fun onAdShowedFullScreenContent() {
                                    isAdShowing = true
                                    adCallback.onRewardShowed()
                                    Log.d("TAG", "The ad was shown.")
                                }
                            }
                        mInterstitialRewardAd.inter?.show(activity) { adCallback.onRewardEarned() }

                    } else {
                        isAdShowing = false
                        adCallback.onRewardFailed("None Show")
                        dismissAdDialog()
                        if (AppOpenUtils.getInstance().isInitialized) {
                            AppOpenUtils.getInstance().isAppResumeEnabled = true
                        }
                        Log.d("TAG", "Ad did not load.")
                    }
                }
            }
        }
    }


    /**
     * Example: AdmobUtils.loadAndShowInterstitial(activity, interHolder,
     * object :AdmobUtils.InterCallback {})
     */
    @JvmStatic
    fun loadAndShowInterstitial(
        activity: AppCompatActivity,
        admobId: InterHolderAdmob,
        adCallback: InterCallback,
        enableLoadingDialog: Boolean
    ) {
        var admobId = admobId.ads
        mInterstitialAd = null
        isAdShowing = false
        if (adRequest == null) {
            initAdRequest(timeOut)
        }
        if (!isShowAds || !isNetworkConnected(activity)) {
            adCallback.onInterFailed("No internet")
            return
        }
        if (AppOpenUtils.getInstance().isInitialized) {
            if (!AppOpenUtils.getInstance().isAppResumeEnabled) {
                return
            } else {
                isAdShowing = false
                if (AppOpenUtils.getInstance().isInitialized) {
                    AppOpenUtils.getInstance().isAppResumeEnabled = false
                }
            }
        }

        if (enableLoadingDialog) {
            dialogLoading(activity)
        }
        if (isTesting) {
            admobId = activity.getString(R.string.test_ads_admob_inter_id)
        } else {
            checkIdTest(activity, admobId)
        }
        InterstitialAd.load(
            activity,
            admobId,
            adRequest!!,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    super.onAdLoaded(interstitialAd)
                    adCallback.onInterLoaded()
                    Handler(Looper.getMainLooper()).postDelayed({
                        mInterstitialAd = interstitialAd
                        if (mInterstitialAd != null) {
                            mInterstitialAd!!.onPaidEventListener =
                                OnPaidEventListener { adValue: AdValue? ->
                                    adValue?.let {
                                        AdjustUtils.postRevenueAdjustInter(
                                            mInterstitialAd!!,
                                            it, mInterstitialAd!!.adUnitId
                                        )
                                    }
                                }
                            mInterstitialAd!!.fullScreenContentCallback =
                                object : FullScreenContentCallback() {
                                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                        adCallback.onInterFailed(adError.message)
                                        isAdShowing = false
                                        if (AppOpenUtils.getInstance().isInitialized) {
                                            AppOpenUtils.getInstance().isAppResumeEnabled = true
                                        }
                                        isAdShowing = false
                                        if (mInterstitialAd != null) {
                                            mInterstitialAd = null
                                        }
                                        dismissAdDialog()
                                        Log.e("Admodfail", "onAdFailedToLoad" + adError.message)
                                        Log.e("Admodfail", "errorCodeAds" + adError.cause)
                                    }

                                    override fun onAdDismissedFullScreenContent() {
                                        lastTimeShowInterstitial = Date().time
                                        adCallback.onDismissedInter()
                                        if (mInterstitialAd != null) {
                                            mInterstitialAd = null
                                        }
                                        isAdShowing = false
                                        if (AppOpenUtils.getInstance().isInitialized) {
                                            AppOpenUtils.getInstance().isAppResumeEnabled = true
                                        }
                                    }

                                    override fun onAdShowedFullScreenContent() {
                                        super.onAdShowedFullScreenContent()
                                        Log.e("===onAdShowed", "onAdShowedFullScreenContent")
                                        adCallback.onInterShowed()
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            dismissAdDialog()
                                        }, 800)
                                    }
                                }
                            if (activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && mInterstitialAd != null) {
                                adCallback.onStartAction()
                                mInterstitialAd!!.show(activity)
                                isAdShowing = true
                            } else {
                                mInterstitialAd = null
                                dismissAdDialog()
                                isAdShowing = false
                                if (AppOpenUtils.getInstance().isInitialized) {
                                    AppOpenUtils.getInstance().isAppResumeEnabled = true
                                }
                                adCallback.onInterFailed("Interstitial can't show in background")
                            }
                        } else {
                            dismissAdDialog()
                            adCallback.onInterFailed("mInterstitialAd null")
                            isAdShowing = false
                            if (AppOpenUtils.getInstance().isInitialized) {
                                AppOpenUtils.getInstance().isAppResumeEnabled = true
                            }
                        }
                    }, 800)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    super.onAdFailedToLoad(loadAdError)
                    mInterstitialAd = null
                    if (AppOpenUtils.getInstance().isInitialized) {
                        AppOpenUtils.getInstance().isAppResumeEnabled = true
                    }
                    isAdShowing = false
                    adCallback.onInterFailed(loadAdError.message)
                    dismissAdDialog()
                }
            })
    }

    //Update New Lib
    private fun checkIdTest(activity: Activity, admobId: String?) {
//        if (admobId.equals(activity.getString(R.string.test_ads_admob_inter_id)) && !BuildConfig.DEBUG) {
//            if (dialog != null) {
//                dialog.dismiss();
//            }
//            Utils.getInstance().showDialogTitle(activity, "Warning", "Build bản release nhưng đang để id test ads", "Đã biết", DialogType.WARNING_TYPE, false, "", new DialogCallback() {
//                @Override
//                public void onClosed() {
//                }
//
//                @Override
//                public void cancel() {
//                }
//            });
//        }
    }

    fun getDeviceID(context: Context): String {
        val android_id = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        return md5(android_id).uppercase(Locale.getDefault())
    }


    fun md5(s: String): String {
        try {
            // Create MD5 Hash
            val digest = MessageDigest.getInstance("MD5")
            digest.update(s.toByteArray())
            val messageDigest = digest.digest()

            // Create Hex String
            val hexString = StringBuffer()
            for (i in messageDigest.indices) hexString.append(Integer.toHexString(0xFF and messageDigest[i].toInt()))
            return hexString.toString()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }
        return ""
    }


    private fun dialogLoading(activity: Activity) {
        dialogFullScreen = Dialog(activity)
        dialogFullScreen?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogFullScreen?.setContentView(R.layout.dialog_full_screen)
        dialogFullScreen?.setCancelable(false)
        dialogFullScreen?.window!!.setBackgroundDrawable(ColorDrawable(Color.WHITE))
        dialogFullScreen?.window!!.setLayout(
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

    }

    /**
     * Example: AdmobUtils.loadNative(context,holder,object :AdmobUtils.NativeCallback {})
     */
    @JvmStatic
    fun loadNative(
        context: Context,
        nativeHolder: NativeHolderAdmob,
        adCallback: NativeCallback
    ) {
        if (!isShowAds || !isNetworkConnected(context)) {
            adCallback.onNativeFailed("No internet")
            return
        }
        //If native is loaded return
        if (nativeHolder.nativeAd != null) {
            Log.d("===AdsLoadsNative", "Native not null")
            return
        }
        if (isTesting) {
            nativeHolder.ads = context.getString(R.string.test_ads_admob_native_id)
        }
        nativeHolder.isLoad = true
        val videoOptions =
            VideoOptions.Builder().setStartMuted(false).build()
        val adLoader: AdLoader = AdLoader.Builder(context, nativeHolder.ads)
            .forNativeAd { nativeAd ->
                nativeHolder.nativeAd = nativeAd
                nativeHolder.isLoad = false
                nativeHolder.native_mutable.value = nativeAd
                nativeAd.setOnPaidEventListener { adValue: AdValue? ->
                    adValue?.let {
                        adCallback.onPaid(adValue, nativeHolder.ads)
                        AdjustUtils.postRevenueAdjustNative(
                            nativeAd,
                            it, nativeHolder.ads
                        )
                    }
                }
                adCallback.onNativeReady(nativeAd)
            }.withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e("Admodfail", "onAdFailedToLoad" + adError.message)
                    Log.e("Admodfail", "errorCodeAds" + adError.cause)
                    nativeHolder.nativeAd = null
                    nativeHolder.isLoad = false
                    nativeHolder.native_mutable.value = null
                    adCallback.onNativeFailed(adError.message)
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    adCallback.onNativeClicked()
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build()).build()
        if (adRequest != null) {
            adLoader.loadAd(adRequest!!)
        }
    }

    /**
     * Example: AdmobUtils.showNative(activity, holder, viewGroup, R.layout.ad_unified_medium,
     * AdNativeSize.MEDIUM, object : AdmobUtils.NativeCallbackSimple {})
     */
    @JvmStatic
    fun showNative(
        activity: Activity,
        nativeHolder: NativeHolderAdmob,
        viewGroup: ViewGroup,
        layout: Int,
        size: AdNativeSize,
        callback: NativeCallbackSimple
    ) {
        if (!isShowAds || !isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
        if (shimmerFrameLayout != null) {
            shimmerFrameLayout?.stopShimmer()
        }
        try {
            viewGroup.removeAllViews()
        } catch (_: Exception) {

        }

        if (!nativeHolder.isLoad) {
            if (nativeHolder.nativeAd != null) {
                val adView = activity.layoutInflater.inflate(layout, null) as NativeAdView
                populateNativeAdView(nativeHolder.nativeAd!!, adView, size)
                if (shimmerFrameLayout != null) {
                    shimmerFrameLayout?.stopShimmer()
                }
                nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
                try {
                    viewGroup.removeAllViews()
                } catch (_: Exception) {

                }
                try {
                    viewGroup.addView(adView)
                } catch (_: Exception) {

                }

                callback.onNativeLoaded()
            } else {
                if (shimmerFrameLayout != null) {
                    shimmerFrameLayout?.stopShimmer()
                }
                nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
                callback.onNativeFailed("None Show")
            }
        } else {
            val tagView: View = if (size === AdNativeSize.MEDIUM) {
                activity.layoutInflater.inflate(R.layout.layoutnative_loading_medium, null, false)
            } else if (size === AdNativeSize.SMALL) {
                activity.layoutInflater.inflate(R.layout.layoutnative_loading_small, null, false)
            } else {
                activity.layoutInflater.inflate(R.layout.layoutbanner_loading, null, false)
            }
            try {
                viewGroup.addView(tagView, 0)
            } catch (_: Exception) {

            }

            if (shimmerFrameLayout == null) shimmerFrameLayout =
                tagView.findViewById(R.id.shimmer_view_container)
            shimmerFrameLayout?.startShimmer()
            nativeHolder.native_mutable.observe((activity as LifecycleOwner)) { nativeAd: NativeAd? ->
                if (nativeAd != null) {
                    nativeAd.setOnPaidEventListener {
                        AdjustUtils.postRevenueAdjustNative(nativeAd, it, nativeHolder.ads)
                        callback.onPaid(it, nativeHolder.ads)
                    }
                    val adView = activity.layoutInflater.inflate(layout, null) as NativeAdView
                    populateNativeAdView(nativeAd, adView, size)
                    if (shimmerFrameLayout != null) {
                        shimmerFrameLayout?.stopShimmer()
                    }
                    try {
                        viewGroup.removeAllViews()
                        viewGroup.addView(adView)
                    } catch (_: Exception) {

                    }

                    callback.onNativeLoaded()
                    nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
                } else {
                    if (shimmerFrameLayout != null) {
                        shimmerFrameLayout?.stopShimmer()
                    }
                    callback.onNativeFailed("None Show")
                    nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
                }
            }
        }
    }

    @JvmStatic
    fun loadAndShowNative(
        activity: Activity,
        nativeHolder: NativeHolderAdmob,
        viewGroup: ViewGroup,
        layout: Int,
        size: AdNativeSize,
        adCallback: NativeCallback
    ) {
        Log.d("===Native", "Native1")
        if (!isShowAds || !isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
//        val videoOptions =
//            VideoOptions.Builder().setStartMuted(false).build()
        try {
            viewGroup.removeAllViews()
        } catch (_: Exception) {

        }
        var s = nativeHolder.ads
        val tagView: View = if (size === AdNativeSize.MEDIUM) {
            activity.layoutInflater.inflate(R.layout.layoutnative_loading_medium, null, false)
        } else if (size === AdNativeSize.SMALL) {
            activity.layoutInflater.inflate(R.layout.layoutnative_loading_small, null, false)
        } else {
            activity.layoutInflater.inflate(R.layout.layoutbanner_loading, null, false)
        }

        try {
            viewGroup.addView(tagView, 0)
        } catch (_: Exception) {

        }

        val shimmerFrameLayout =
            tagView.findViewById<ShimmerFrameLayout>(R.id.shimmer_view_container)
        shimmerFrameLayout.startShimmer()

        if (isTesting) {
            s = activity.getString(R.string.test_ads_admob_native_id)
        }
        val adLoader = AdLoader.Builder(activity, s)
            .forNativeAd { nativeAd ->
                adCallback.onNativeLoaded()
                val adView = activity.layoutInflater
                    .inflate(layout, null) as NativeAdView
                populateNativeAdView(nativeAd, adView, size)
                shimmerFrameLayout.stopShimmer()
                try {
                    viewGroup.removeAllViews()
                    viewGroup.addView(adView)
                } catch (_: Exception) {

                }

                nativeAd.setOnPaidEventListener { adValue: AdValue ->
                    adCallback.onPaid(adValue, s)
                    AdjustUtils.postRevenueAdjustNative(nativeAd, adValue, s)
                }
                //viewGroup.setVisibility(View.VISIBLE);
            }.withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e("Admodfail", "onAdFailedToLoad" + adError.message)
                    Log.e("Admodfail", "errorCodeAds" + adError.cause)
                    shimmerFrameLayout.stopShimmer()
                    try {
                        viewGroup.removeAllViews()
                    } catch (_: Exception) {

                    }
                    nativeHolder.isLoad = false
                    adCallback.onNativeFailed(adError.message)
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    adCallback.onNativeClicked()
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build()).build()
        if (adRequest != null) {
            adLoader.loadAd(adRequest!!)
        }
        Log.e("Admod", "loadAdNativeAds")
    }

    /**
     * Example: AdmobUtils.loadAndShowNativeCollap(this,nativeHolder,binding.native,
     * R.layout.ad_template_medium,AdNativeSize.MEDIUM,object :AdmobUtils.NativeAdCallbackNew {})
     */
    @JvmStatic
    fun loadAndShowNativeCollap(
        activity: Activity,
        nativeHolder: NativeHolderAdmob,
        viewGroup: ViewGroup,
        layout: Int,
        size: AdNativeSize,
        adCallback: NativeCallback
    ) {
        Log.d("===Native", "Native1")
        if (!isShowAds || !isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
//        val videoOptions =
//            VideoOptions.Builder().setStartMuted(false).build()
        try {
            viewGroup.removeAllViews()
        } catch (_: Exception) {

        }
        var s = nativeHolder.ads
        val tagView: View = if (size === AdNativeSize.MEDIUM) {
            activity.layoutInflater.inflate(R.layout.layoutnative_loading_medium, null, false)
        } else {
            activity.layoutInflater.inflate(R.layout.layoutnative_loading_small, null, false)
        }
        try {
            viewGroup.addView(tagView, 0)
        } catch (_: Exception) {

        }

        val shimmerFrameLayout =
            tagView.findViewById<ShimmerFrameLayout>(R.id.shimmer_view_container)
        shimmerFrameLayout.startShimmer()

        if (isTesting) {
            s = activity.getString(R.string.test_ads_admob_native_id)
        }
        val adLoader = AdLoader.Builder(activity, s)
            .forNativeAd { nativeAd ->
                adCallback.onNativeLoaded()
                val adView = activity.layoutInflater
                    .inflate(layout, null) as NativeAdView
                populateNativeAdViewClose(nativeAd, adView, size, adCallback)
                shimmerFrameLayout.stopShimmer()
                try {
                    viewGroup.removeAllViews()
                    viewGroup.addView(adView)
                } catch (_: Exception) {

                }

                nativeAd.setOnPaidEventListener { adValue: AdValue ->
                    adCallback.onPaid(adValue, s)
                    AdjustUtils.postRevenueAdjustNative(nativeAd, adValue, s)
                }
                //viewGroup.setVisibility(View.VISIBLE);
            }.withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e("Admodfail", "onAdFailedToLoad" + adError.message)
                    Log.e("Admodfail", "errorCodeAds" + adError.cause)
                    shimmerFrameLayout.stopShimmer()
                    try {
                        viewGroup.removeAllViews()
                    } catch (_: Exception) {

                    }
                    nativeHolder.isLoad = false
                    adCallback.onNativeFailed(adError.message)
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build()).build()
        if (adRequest != null) {
            adLoader.loadAd(adRequest!!)
        }
    }

    @JvmStatic
    fun loadAndShowNativeNoShimmer(
        activity: Activity,
        nativeHolder: NativeHolderAdmob,
        viewGroup: ViewGroup,
        layout: Int,
        size: AdNativeSize,
        adCallback: NativeCallback
    ) {
        Log.d("===Native", "Native1")
        if (!isShowAds || !isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
        var s = nativeHolder.ads
        if (isTesting) {
            s = activity.getString(R.string.test_ads_admob_native_id)
        }
        val adLoader = AdLoader.Builder(activity, s)
            .forNativeAd { nativeAd ->
                adCallback.onNativeLoaded()
                val adView = activity.layoutInflater
                    .inflate(layout, null) as NativeAdView
                populateNativeAdView(nativeAd, adView, size)
                try {
                    viewGroup.removeAllViews()
                    viewGroup.addView(adView)
                } catch (_: Exception) {

                }

                nativeAd.setOnPaidEventListener { adValue: AdValue ->
                    AdjustUtils.postRevenueAdjustNative(nativeAd, adValue, s)
                    adCallback.onPaid(adValue, s)
                }
                //viewGroup.setVisibility(View.VISIBLE);
            }.withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e("Admodfail", "onAdFailedToLoad" + adError.message)
                    Log.e("Admodfail", "errorCodeAds" + adError.cause)
                    try {
                        viewGroup.removeAllViews()
                    } catch (_: Exception) {

                    }
                    nativeHolder.isLoad = false
                    adCallback.onNativeFailed(adError.message)
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    adCallback.onNativeClicked()
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build()).build()
        if (adRequest != null) {
            adLoader.loadAd(adRequest!!)
        }
        Log.e("Admod", "loadAdNativeAds")
    }

    fun loadAndShowNativeFullScreen(
        activity: Activity,
        id: String,
        viewGroup: ViewGroup,
        layout: Int,
        mediaAspectRatio: Int,
        listener: NativeFullScreenCallback
    ) {
        if (!isShowAds || !isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
        var adMobId: String = id
        if (isTesting) {
            adMobId = activity.getString(R.string.test_ads_admob_native_full_screen_id)
        }
        try {
            viewGroup.removeAllViews()
        } catch (_: Exception) {

        }
        val tagView =
            activity.layoutInflater.inflate(R.layout.layoutnative_loading_fullscreen, null, false)
        try {
            viewGroup.addView(tagView, 0)
        } catch (_: Exception) {

        }

        shimmerFrameLayout = tagView.findViewById(R.id.shimmer_view_container)
        shimmerFrameLayout?.startShimmer()
        val adView = activity.layoutInflater.inflate(layout, null) as NativeAdView
        val builder = AdLoader.Builder(activity, adMobId)
        val videoOptions =
            VideoOptions.Builder().setStartMuted(false).setCustomControlsRequested(false).build()
        val adOptions = NativeAdOptions.Builder()
            .setMediaAspectRatio(mediaAspectRatio)
            .setVideoOptions(videoOptions)
            .build()
        builder.withNativeAdOptions(adOptions)
        builder.forNativeAd { nativeAd ->
            nativeAd.setOnPaidEventListener { adValue: AdValue? ->
                adValue?.let {
                    AdjustUtils.postRevenueAdjustNative(nativeAd, it, adUnit = id)
                }
            }
            listener.onLoaded(nativeAd)
            populateNativeAdView(nativeAd, adView.findViewById(R.id.native_ad_view))
            try {
                viewGroup.removeAllViews()
            } catch (_: Exception) {

            }
            shimmerFrameLayout?.stopShimmer()
            try {
                viewGroup.addView(adView)
            } catch (_: Exception) {

            }

        }
        builder.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Log.d("===AdmobFailed", loadAdError.toString())
                shimmerFrameLayout?.stopShimmer()
                listener.onLoadFailed()
            }
        })
        if (adRequest != null) {
            builder.build().loadAd(adRequest!!)
        }
    }

    @JvmStatic
    fun loadNativeFullScreen(
        context: Context,
        nativeHolder: NativeHolderAdmob, mediaAspectRatio: Int,
        adCallback: NativeCallback
    ) {
        if (!isShowAds || !isNetworkConnected(context)) {
            adCallback.onNativeFailed("No internet")
            return
        }
        //If native is loaded return
        if (nativeHolder.nativeAd != null) {
            Log.d("===AdsLoadsNative", "Native not null")
            return
        }
        if (isTesting) {
            nativeHolder.ads = context.getString(R.string.test_ads_admob_native_full_screen_id)
        }
        nativeHolder.isLoad = true
        val videoOptions =
            VideoOptions.Builder().setStartMuted(false).setCustomControlsRequested(true).build()
        val adOptions = NativeAdOptions.Builder()
            .setMediaAspectRatio(mediaAspectRatio)
            .setVideoOptions(videoOptions)
            .build()
        val adLoader = AdLoader.Builder(context, nativeHolder.ads)
        adLoader.withNativeAdOptions(adOptions)
        adLoader.forNativeAd { nativeAd ->
            nativeHolder.nativeAd = nativeAd
            nativeHolder.isLoad = false
            nativeHolder.native_mutable.value = nativeAd
            nativeAd.setOnPaidEventListener { adValue: AdValue? ->
                adValue?.let {
                    AdjustUtils.postRevenueAdjustNative(nativeAd, it, adUnit = nativeHolder.ads)
                }
            }
            adCallback.onNativeReady(nativeAd)
        }
        adLoader.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e("Admodfail", "onAdFailedToLoad" + adError.message)
                Log.e("Admodfail", "errorCodeAds" + adError.cause)
                nativeHolder.nativeAd = null
                nativeHolder.isLoad = false
                nativeHolder.native_mutable.value = null
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

    @JvmStatic
    fun showNativeFullScreen(
        activity: Activity,
        nativeHolder: NativeHolderAdmob,
        viewGroup: ViewGroup,
        layout: Int,
        callback: NativeCallbackSimple
    ) {
        if (!isShowAds || !isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
        if (shimmerFrameLayout != null) {
            shimmerFrameLayout?.stopShimmer()
        }
        viewGroup.removeAllViews()
        if (!nativeHolder.isLoad) {
            if (nativeHolder.nativeAd != null) {
                val adView = activity.layoutInflater.inflate(layout, null) as NativeAdView
                populateNativeAdView(
                    nativeHolder.nativeAd!!,
                    adView.findViewById(R.id.native_ad_view)
                )
                shimmerFrameLayout?.stopShimmer()
                if (shimmerFrameLayout != null) {
                    shimmerFrameLayout?.stopShimmer()
                }
                nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
                viewGroup.removeAllViews()
                viewGroup.addView(adView)
                callback.onNativeLoaded()
            } else {
                if (shimmerFrameLayout != null) {
                    shimmerFrameLayout?.stopShimmer()
                }
                nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
                callback.onNativeFailed("None Show")
            }
        } else {
            val tagView = activity.layoutInflater.inflate(
                R.layout.layoutnative_loading_fullscreen,
                null,
                false
            )
            viewGroup.addView(tagView, 0)

            if (shimmerFrameLayout == null) shimmerFrameLayout =
                tagView.findViewById(R.id.shimmer_view_container)
            shimmerFrameLayout?.startShimmer()
            nativeHolder.native_mutable.observe((activity as LifecycleOwner)) { nativeAd: NativeAd? ->
                if (nativeAd != null) {
                    nativeAd.setOnPaidEventListener {
                        AdjustUtils.postRevenueAdjustNative(nativeAd, it, adUnit = nativeHolder.ads)
                    }

                    val adView = activity.layoutInflater.inflate(layout, null) as NativeAdView
                    populateNativeAdView(
                        nativeHolder.nativeAd!!,
                        adView.findViewById(R.id.native_ad_view)
                    )
                    if (shimmerFrameLayout != null) {
                        shimmerFrameLayout?.stopShimmer()
                    }

                    viewGroup.removeAllViews()
                    viewGroup.addView(adView)

                    callback.onNativeLoaded()
                    nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
                } else {
                    if (shimmerFrameLayout != null) {
                        shimmerFrameLayout?.stopShimmer()
                    }
                    callback.onNativeFailed("None Show")
                    nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
                }
            }
        }
    }

    fun loadAndShowNativeFullScreenNoShimmer(
        activity: Activity,
        id: String,
        viewGroup: ViewGroup,
        layout: Int,
        mediaAspectRatio: Int,
        listener: NativeFullScreenCallback
    ) {
        if (!isShowAds || !isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
        var adMobId: String = id
        if (isTesting) {
            adMobId = activity.getString(R.string.test_ads_admob_native_full_screen_id)
        }
        val adView = activity.layoutInflater.inflate(layout, null) as NativeAdView
        val builder = AdLoader.Builder(activity, adMobId)
        val videoOptions =
            VideoOptions.Builder().setStartMuted(false).setCustomControlsRequested(false).build()
        val adOptions = NativeAdOptions.Builder()
            .setMediaAspectRatio(mediaAspectRatio)
            .setVideoOptions(videoOptions)
            .build()
        builder.withNativeAdOptions(adOptions)
        builder.forNativeAd { nativeAd ->
            listener.onLoaded(nativeAd)
            nativeAd.setOnPaidEventListener { adValue: AdValue? ->
                adValue?.let {
                    AdjustUtils.postRevenueAdjustNative(
                        nativeAd,
                        adValue,
                        id
                    )
                }
            }
            populateNativeAdView(nativeAd, adView.findViewById(R.id.native_ad_view))
            try {
                viewGroup.removeAllViews()
                viewGroup.addView(adView)
            } catch (_: Exception) {

            }

        }
        builder.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Log.d("===AdmobFailed", loadAdError.toString())
                listener.onLoadFailed()
            }
        })
        if (adRequest != null) {
            builder.build().loadAd(adRequest!!)
        }
    }

    @JvmStatic
    fun loadNativeNoButton(
        activity: Activity,
        nativeHolder: NativeHolderAdmob,
        viewGroup: ViewGroup,
        layout: Int,
        size: AdNativeSize,
        adCallback: NativeCallback
    ) {
        Log.d("===Native", "Native1")
        if (!isShowAds || !isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
//        val videoOptions =
//            VideoOptions.Builder().setStartMuted(false).build()
        try {
            viewGroup.removeAllViews()
        } catch (_: Exception) {

        }

        var s = nativeHolder.ads
        val tagView: View = if (size === AdNativeSize.MEDIUM) {
            activity.layoutInflater.inflate(R.layout.layoutnative_loading_medium, null, false)
        } else {
            activity.layoutInflater.inflate(R.layout.layoutnative_loading_small, null, false)
        }
        try {
            viewGroup.addView(tagView, 0)
        } catch (_: Exception) {

        }

        val shimmerFrameLayout =
            tagView.findViewById<ShimmerFrameLayout>(R.id.shimmer_view_container)
        shimmerFrameLayout.startShimmer()

        if (isTesting) {
            s = activity.getString(R.string.test_ads_admob_native_id)
        }
        val adLoader = AdLoader.Builder(activity, s)
            .forNativeAd { nativeAd ->
                adCallback.onNativeLoaded()
                val adView = activity.layoutInflater
                    .inflate(layout, null) as NativeAdView
                populateNativeAdViewNoBtn(nativeAd, adView, size)
                shimmerFrameLayout.stopShimmer()
                try {
                    viewGroup.removeAllViews()
                    viewGroup.addView(adView)
                } catch (_: Exception) {

                }
                nativeAd.setOnPaidEventListener { adValue: AdValue ->
                    adCallback.onPaid(adValue, s)
                }
                //viewGroup.setVisibility(View.VISIBLE);
            }.withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e("Admodfail", "onAdFailedToLoad" + adError.message)
                    Log.e("Admodfail", "errorCodeAds" + adError.cause)
                    shimmerFrameLayout.stopShimmer()
                    try {
                        viewGroup.removeAllViews()
                    } catch (_: Exception) {

                    }
                    nativeHolder.isLoad = false
                    adCallback.onNativeFailed(adError.message)
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    adCallback.onNativeClicked()
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build()).build()
        if (adRequest != null) {
            adLoader.loadAd(adRequest!!)
        }
        Log.e("Admod", "loadAdNativeAds")
    }

    @JvmStatic
    fun showNativeNoButton(
        activity: Activity,
        nativeHolder: NativeHolderAdmob,
        viewGroup: ViewGroup,
        layout: Int,
        size: AdNativeSize,
        callback: NativeCallbackSimple
    ) {
        if (!isShowAds || !isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
        if (shimmerFrameLayout != null) {
            shimmerFrameLayout?.stopShimmer()
        }
        try {
            viewGroup.removeAllViews()
        } catch (_: Exception) {

        }
        if (!nativeHolder.isLoad) {
            if (nativeHolder.nativeAd != null) {
                val adView = activity.layoutInflater.inflate(layout, null) as NativeAdView
                populateNativeAdViewNoBtn(nativeHolder.nativeAd!!, adView, size)
                if (shimmerFrameLayout != null) {
                    shimmerFrameLayout?.stopShimmer()
                }
                nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
                try {
                    viewGroup.removeAllViews()
                    viewGroup.addView(adView)
                } catch (_: Exception) {

                }
                callback.onNativeLoaded()
            } else {
                if (shimmerFrameLayout != null) {
                    shimmerFrameLayout?.stopShimmer()
                }
                nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
                callback.onNativeFailed("None Show")
            }
        } else {
            val tagView: View = if (size === AdNativeSize.MEDIUM) {
                activity.layoutInflater.inflate(R.layout.layoutnative_loading_medium, null, false)
            } else {
                activity.layoutInflater.inflate(R.layout.layoutnative_loading_small, null, false)
            }
            try {
                viewGroup.addView(tagView, 0)
            } catch (_: Exception) {

            }

            if (shimmerFrameLayout == null) shimmerFrameLayout =
                tagView.findViewById(R.id.shimmer_view_container)
            shimmerFrameLayout?.startShimmer()
            nativeHolder.native_mutable.observe((activity as LifecycleOwner)) { nativeAd: NativeAd? ->
                if (nativeAd != null) {
                    nativeAd.setOnPaidEventListener {
                        AdjustUtils.postRevenueAdjustNative(nativeAd, it, nativeHolder.ads)
                        callback.onPaid(it, nativeHolder.ads)
                    }
                    val adView = activity.layoutInflater.inflate(layout, null) as NativeAdView
                    populateNativeAdViewNoBtn(nativeAd, adView, size)
                    if (shimmerFrameLayout != null) {
                        shimmerFrameLayout?.stopShimmer()
                    }
                    try {
                        viewGroup.removeAllViews()
                        viewGroup.addView(adView)
                    } catch (_: Exception) {

                    }
                    callback.onNativeLoaded()
                    nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
                } else {
                    if (shimmerFrameLayout != null) {
                        shimmerFrameLayout?.stopShimmer()
                    }
                    callback.onNativeFailed("None Show")
                    nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
                }
            }
        }
    }

    interface InterCallback {
        fun onStartAction()
        fun onDismissedInter()
        fun onInterShowed()
        fun onInterLoaded()
        fun onInterFailed(error: String)
        fun onPaid(adValue: AdValue?, adUnitAds: String?)
    }

    interface LoadInterCallback {
        fun onInterLoaded(interstitialAd: InterstitialAd?, isLoading: Boolean)
        fun onInterFailed(error: String)
        fun onPaid(adValue: AdValue?, adUnitAds: String?)
    }

    interface BannerCallback {
        fun onBannerClicked()
        fun onBannerLoaded()
        fun onBannerFailed(error: String)
//        fun onPaid(adValue: AdValue?, mAdView: AdView?)
    }

    interface BannerCollapCallback {
        fun onBannerClicked()
        fun onBannerLoaded(adSize: AdSize)
        fun onBannerFailed(error: String)
        fun onPaid(adValue: AdValue, adView: AdView)
    }

    interface NativeCallbackSimple {
        fun onNativeLoaded()
        fun onNativeFailed(error: String)
        fun onPaid(adValue: AdValue?, adUnitAds: String?)
    }

    interface NativeCallback {
        fun onNativeReady(ad: NativeAd?)
        fun onNativeLoaded() //* giống hàm onNativeReady nhưng ko kèm param
        fun onNativeFailed(error: String)
        fun onPaid(adValue: AdValue?, adUnitAds: String?)
        fun onNativeClicked()
    }

    interface NativeFullScreenCallback {
        fun onLoaded(nativeAd: NativeAd)
        fun onLoadFailed()
    }

    interface RewardedCallback {
        fun onRewardClosed()
        fun onRewardShowed()
        fun onRewardFailed(error: String)
        fun onRewardEarned()
//        fun onPaid(adValue: AdValue?, adUnitAds: String?)
    }

    interface RewardedInterCallback {
        fun onRewardFailed(error: String)
        fun onRewardLoaded()
//        fun onPaid(adValue: AdValue?, adUnitAds: String?)
    }

}
