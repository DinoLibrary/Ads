package com.dino.ads

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
import com.dino.ads.NativeHelper.Companion.populateNativeAdView
import com.dino.ads.NativeHelper.Companion.populateNativeAdViewClose
import com.dino.ads.NativeHelper.Companion.populateNativeAdViewNoBtn
import com.dino.ads.adjust.AdjustUtils
import com.dino.ads.utils.SweetAlert.SweetAlertDialog
import com.dino.ads.utils.admod.BannerHolderAdmob
import com.dino.ads.utils.admod.InterHolderAdmob
import com.dino.ads.utils.admod.NativeHolderAdmob
import com.dino.ads.utils.admod.RewardHolderAdmob
import com.dino.ads.utils.admod.RewardedInterstitialHolderAdmob
import com.dino.ads.utils.admod.remote.BannerPlugin
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
    private var dialog: com.dino.ads.utils.SweetAlert.SweetAlertDialog? = null
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
        AdmobUtils.timeOut = timeout
        if (AdmobUtils.timeOut < 5000 && timeout != 0) {
            Toast.makeText(context, "Nên để limit time ~10000", Toast.LENGTH_LONG).show()
        }
        AdmobUtils.timeOut = if (timeout > 0) {
            timeout
        } else {
            10000
        }
        AdmobUtils.isTesting = isDebug
        AdmobUtils.isShowAds = isEnableAds
        MobileAds.initialize(context) { initializationStatus: InitializationStatus? -> }
        AdmobUtils.initListIdTest()
        val requestConfiguration = RequestConfiguration.Builder()
            .setTestDeviceIds(AdmobUtils.testDevices)
            .build()
        MobileAds.setRequestConfiguration(requestConfiguration)
        AdmobUtils.initAdRequest(timeout)
    }

    @JvmStatic
    private fun initAdRequest(timeOut: Int) {
        AdmobUtils.adRequest = AdRequest.Builder()
            .setHttpTimeoutMillis(timeOut)
            .build()
    }

    private fun initListIdTest() {
        AdmobUtils.testDevices.add("D4A597237D12FDEC52BE6B2F15508BB")
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
        bannerAdCallback: AdmobUtils.BannerCallback
    ) {
        var bannerId = adId
        if (!AdmobUtils.isShowAds || !AdmobUtils.isNetworkConnected(
                activity
            )
        ) {
            viewGroup.visibility = View.GONE
            bannerAdCallback.onBannerFailed("None Show")
            return
        }
        val mAdView = AdView(activity)
        if (AdmobUtils.isTesting) {
            bannerId = activity.getString(R.string.test_ads_admob_banner_id)
        }
        mAdView.adUnitId = bannerId!!
        val adSize = AdmobUtils.getBannerSize(activity)
        mAdView.setAdSize(adSize)
        val tagView = activity.layoutInflater.inflate(R.layout.layoutbanner_loading, null, false)

        try {
            viewGroup.removeAllViews()
            viewGroup.addView(tagView, 0)
            viewGroup.addView(mAdView, 1)
        } catch (_: Exception) {

        }
        AdmobUtils.shimmerFrameLayout = tagView.findViewById(R.id.shimmer_view_container)
        AdmobUtils.shimmerFrameLayout?.startShimmer()
        mAdView.onPaidEventListener =
            OnPaidEventListener { adValue ->
                AdjustUtils.postRevenueAdjust(
                    adValue,
                    mAdView.adUnitId
                )
            }
        mAdView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                AdmobUtils.shimmerFrameLayout?.stopShimmer()
                viewGroup.removeView(tagView)
                bannerAdCallback.onBannerLoaded()
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(" Admod", "failloadbanner" + adError.message)
                AdmobUtils.shimmerFrameLayout?.stopShimmer()
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
        if (AdmobUtils.adRequest != null) {
            mAdView.loadAd(AdmobUtils.adRequest!!)
        }
        Log.e(" Admod", "loadAdBanner")
    }

    @JvmStatic
    fun loadBannerCollap(
        activity: Activity,
        bannerId: String?,
        bannerCollapAnchor: BannerCollapAnchor,
        viewGroup: ViewGroup,
        callback: AdmobUtils.BannerCollapCallback
    ) {
        var bannerId = bannerId
        if (!AdmobUtils.isShowAds || !AdmobUtils.isNetworkConnected(
                activity
            )
        ) {
            viewGroup.visibility = View.GONE
            return
        }
        val mAdView = AdView(activity)
        if (AdmobUtils.isTesting) {
            bannerId = activity.getString(R.string.test_ads_admob_banner_collapsible_id)
        }
        mAdView.adUnitId = bannerId!!
        val adSize = AdmobUtils.getBannerSize(activity)
        mAdView.setAdSize(adSize)
        val tagView = activity.layoutInflater.inflate(R.layout.layoutbanner_loading, null, false)
        try {
            viewGroup.removeAllViews()
            viewGroup.addView(tagView, 0)
            viewGroup.addView(mAdView, 1)
        } catch (_: Exception) {

        }
        AdmobUtils.shimmerFrameLayout = tagView.findViewById(R.id.shimmer_view_container)
        AdmobUtils.shimmerFrameLayout?.startShimmer()

        mAdView?.adListener = object : AdListener() {
            override fun onAdLoaded() {
                mAdView.onPaidEventListener =
                    OnPaidEventListener { adValue ->
                        AdjustUtils.postRevenueAdjust(
                            adValue,
                            mAdView.adUnitId
                        )
                    }
                AdmobUtils.shimmerFrameLayout?.stopShimmer()
                viewGroup.removeView(tagView)
                callback.onBannerLoaded(adSize)
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(" Admod", "failloadbanner" + adError.message)
                AdmobUtils.shimmerFrameLayout?.stopShimmer()
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
        callback: AdmobUtils.BannerCollapCallback
    ) {
        var bannerId = banner.ads
        if (!AdmobUtils.isShowAds || !AdmobUtils.isNetworkConnected(
                activity
            )
        ) {
            viewGroup.visibility = View.GONE
            return
        }
        banner.mAdView?.destroy()
        banner.mAdView?.let {
            viewGroup.removeView(it)
        }
        banner.mAdView = AdView(activity)
        if (AdmobUtils.isTesting) {
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
        val adSize = AdmobUtils.getBannerSize(activity)
        banner.mAdView?.setAdSize(adSize)
        AdmobUtils.shimmerFrameLayout = tagView.findViewById(R.id.shimmer_view_container)
        AdmobUtils.shimmerFrameLayout?.startShimmer()

        banner.mAdView?.adListener = object : AdListener() {
            override fun onAdLoaded() {
                banner.mAdView?.onPaidEventListener =
                    OnPaidEventListener { adValue ->
                        AdjustUtils.postRevenueAdjust(
                            adValue,
                            banner.mAdView?.adUnitId
                        )
                    }
                AdmobUtils.shimmerFrameLayout?.stopShimmer()
                viewGroup.removeView(tagView)
                callback.onBannerLoaded(adSize)
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(" Admod", "failloadbanner" + adError.message)
                AdmobUtils.shimmerFrameLayout?.stopShimmer()
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
        bannerAdCallback: AdmobUtils.BannerCollapCallback
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
        bannerAdCallback: AdmobUtils.BannerCollapCallback
    ) {
        var bannerPlugin: BannerPlugin? = null
        val bannerConfig = BannerPlugin.BannerConfig(id, size, refreshRateSec, cbFetchIntervalSec)
        bannerPlugin = bannerConfig.adUnitId?.let {
            BannerPlugin(
                activity, view, it, bannerConfig, object : BannerRemoteConfig {
                    override fun onBannerAdLoaded(adSize: AdSize?) {
                        adSize?.let { it1 -> bannerAdCallback.onBannerLoaded(it1) }
                        AdmobUtils.shimmerFrameLayout?.stopShimmer()
                        Log.d("===Banner==", "reload banner")
                    }

                    override fun onAdFail() {
                        Log.d("===Banner", "Banner2")
                        AdmobUtils.shimmerFrameLayout?.stopShimmer()
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
        adLoadCallback: AdmobUtils.LoadInterCallback
    ) {
        AdmobUtils.isAdShowing = false
        if (!AdmobUtils.isShowAds || !AdmobUtils.isNetworkConnected(
                context
            )
        ) {
            adLoadCallback.onInterFailed("None Show")
            return
        }
        if (interHolder.inter != null) {
            Log.d("===AdsInter", "inter not null")
            return
        }
        interHolder.check = true
        if (AdmobUtils.adRequest == null) {
            AdmobUtils.initAdRequest(AdmobUtils.timeOut)
        }
        if (AdmobUtils.isTesting) {
            interHolder.ads = context.getString(R.string.test_ads_admob_inter_id)
        }
        AdmobUtils.idIntersitialReal = interHolder.ads
        InterstitialAd.load(
            context,
            AdmobUtils.idIntersitialReal!!,
            AdmobUtils.adRequest!!,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    if (AdmobUtils.isClick) {
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
                    AdmobUtils.isAdShowing = false
                    if (AdmobUtils.mInterstitialAd != null) {
                        AdmobUtils.mInterstitialAd = null
                    }
                    interHolder.check = false
                    if (AdmobUtils.isClick) {
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
        adCallback: AdmobUtils.InterCallback?,
        enableLoadingDialog: Boolean
    ) {
        AdmobUtils.isClick = true
        //Check internet
        if (!AdmobUtils.isShowAds || !AdmobUtils.isNetworkConnected(
                activity
            )
        ) {
            AdmobUtils.isAdShowing = false
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
                AdmobUtils.isClick = false
                interHolder.mutable.removeObservers((activity as LifecycleOwner))
                AdmobUtils.isAdShowing = false
                AdmobUtils.dismissAdDialog()
                adCallback?.onInterFailed("timeout")
            }
        }
        handler.postDelayed(runnable, timeout)
        //Inter is Loading...
        if (interHolder.check) {
            if (enableLoadingDialog) {
                AdmobUtils.dialogLoading(activity)
            }
            interHolder.mutable.observe((activity as LifecycleOwner)) { aBoolean: InterstitialAd? ->
                if (aBoolean != null) {
                    interHolder.mutable.removeObservers((activity as LifecycleOwner))
                    AdmobUtils.isClick = false
                    Handler(Looper.getMainLooper()).postDelayed({
                        Log.d("===DelayLoad", "delay")

                        aBoolean.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                AdmobUtils.isAdShowing = false
                                if (AppOpenUtils.getInstance().isInitialized) {
                                    AppOpenUtils.getInstance().isAppResumeEnabled = true
                                }
                                AdmobUtils.isClick = false
                                //Set inter = null
                                interHolder.inter = null
                                interHolder.mutable.removeObservers((activity as LifecycleOwner))
                                interHolder.mutable.value = null
                                adCallback?.onDismissedInter()
                                AdmobUtils.dismissAdDialog()
                                Log.d("TAG", "The ad was dismissed.")
                            }

                            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                AdmobUtils.isAdShowing = false
                                if (AppOpenUtils.getInstance().isInitialized) {
                                    AppOpenUtils.getInstance().isAppResumeEnabled = true
                                }
                                AdmobUtils.isClick = false
                                AdmobUtils.isAdShowing = false
                                //Set inter = null
                                interHolder.inter = null
                                AdmobUtils.dismissAdDialog()
                                Log.e("Admodfail", "onAdFailedToLoad" + adError.message)
                                Log.e("Admodfail", "errorCodeAds" + adError.cause)
                                interHolder.mutable.removeObservers((activity as LifecycleOwner))
                                interHolder.mutable.value = null
                                handler.removeCallbacksAndMessages(null)
                                adCallback?.onInterFailed(adError.message)
                            }

                            override fun onAdShowedFullScreenContent() {
                                handler.removeCallbacksAndMessages(null)
                                AdmobUtils.isAdShowing = true
                                adCallback?.onInterShowed()

                            }
                        }
                        AdmobUtils.showInterstitial(
                            activity,
                            aBoolean,
                            adCallback
                        )
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
                AdmobUtils.isAdShowing = false
                if (AppOpenUtils.getInstance().isInitialized) {
                    AppOpenUtils.getInstance().isAppResumeEnabled = true
                }
                adCallback.onInterFailed("inter null")
                handler.removeCallbacksAndMessages(null)
            }
        } else {
            if (enableLoadingDialog) {
                AdmobUtils.dialogLoading(activity)
            }
            Handler(Looper.getMainLooper()).postDelayed({
                interHolder.inter?.fullScreenContentCallback =
                    object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            AdmobUtils.isAdShowing = false
                            if (AppOpenUtils.getInstance().isInitialized) {
                                AppOpenUtils.getInstance().isAppResumeEnabled = true
                            }
                            AdmobUtils.isClick = false
                            interHolder.mutable.removeObservers((activity as LifecycleOwner))
                            interHolder.inter = null
                            adCallback?.onDismissedInter()
                            AdmobUtils.dismissAdDialog()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            AdmobUtils.isAdShowing = false
                            if (AppOpenUtils.getInstance().isInitialized) {
                                AppOpenUtils.getInstance().isAppResumeEnabled = true
                            }
                            handler.removeCallbacksAndMessages(null)
                            AdmobUtils.isClick = false
                            interHolder.inter = null
                            interHolder.mutable.removeObservers((activity as LifecycleOwner))
                            AdmobUtils.isAdShowing = false
                            AdmobUtils.dismissAdDialog()
                            adCallback?.onInterFailed(adError.message)
                            Log.e("Admodfail", "onAdFailedToLoad" + adError.message)
                            Log.e("Admodfail", "errorCodeAds" + adError.cause)
                        }

                        override fun onAdShowedFullScreenContent() {
                            handler.removeCallbacksAndMessages(null)
                            AdmobUtils.isAdShowing = true
                            adCallback?.onInterShowed()
                        }
                    }
                AdmobUtils.showInterstitial(
                    activity,
                    interHolder.inter,
                    adCallback
                )
            }, 400)
        }
    }

    @JvmStatic
    private fun showInterstitial(
        activity: Activity,
        mInterstitialAd: InterstitialAd?,
        callback: AdmobUtils.InterCallback?
    ) {
        if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && mInterstitialAd != null) {
            AdmobUtils.isAdShowing = true
            Handler(Looper.getMainLooper()).postDelayed({
                callback?.onStartAction()

                mInterstitialAd.show(activity)
            }, 400)
        } else {
            AdmobUtils.isAdShowing = false
            if (AppOpenUtils.getInstance().isInitialized) {
                AppOpenUtils.getInstance().isAppResumeEnabled = true
            }
            AdmobUtils.dismissAdDialog()
            callback?.onInterFailed("onResume")
        }
    }

    @JvmStatic
    fun dismissAdDialog() {
        try {
            if (AdmobUtils.dialog != null && AdmobUtils.dialog!!.isShowing) {
                AdmobUtils.dialog!!.dismiss()
            }
            if (AdmobUtils.dialogFullScreen != null && AdmobUtils.dialogFullScreen?.isShowing == true) {
                AdmobUtils.dialogFullScreen?.dismiss()
            }
        } catch (_: Exception) {

        }
    }

    @JvmStatic
    fun loadAndShowRewarded(
        activity: Activity,
        admobId: String?,
        adCallback2: AdmobUtils.RewardedCallback,
        enableLoadingDialog: Boolean
    ) {
        var admobId = admobId
        AdmobUtils.mInterstitialAd = null
        AdmobUtils.isAdShowing = false
        if (!AdmobUtils.isShowAds || !AdmobUtils.isNetworkConnected(
                activity
            )
        ) {
            adCallback2.onRewardClosed()
            return
        }
        if (AdmobUtils.adRequest == null) {
            AdmobUtils.initAdRequest(AdmobUtils.timeOut)
        }
        if (AdmobUtils.isTesting) {
            admobId = activity.getString(R.string.test_ads_admob_reward_id)
        }
        if (enableLoadingDialog) {
            AdmobUtils.dialogLoading(activity)
        }
        AdmobUtils.isAdShowing = false
        if (AppOpenUtils.getInstance().isInitialized) {
            AppOpenUtils.getInstance().isAppResumeEnabled = false
        }
        RewardedAd.load(activity, admobId!!,
            AdmobUtils.adRequest!!, object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    // Handle the error.
                    AdmobUtils.mRewardedAd = null
                    adCallback2.onRewardFailed(loadAdError.message)
                    AdmobUtils.dismissAdDialog()
                    if (AppOpenUtils.getInstance().isInitialized) {
                        AppOpenUtils.getInstance().isAppResumeEnabled = true
                    }
                    AdmobUtils.isAdShowing = false
                    Log.e("Admodfail", "onAdFailedToLoad" + loadAdError.message)
                    Log.e("Admodfail", "errorCodeAds" + loadAdError.cause)
                }

                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    AdmobUtils.mRewardedAd = rewardedAd
                    if (AdmobUtils.mRewardedAd != null) {
                        AdmobUtils.mRewardedAd?.setOnPaidEventListener {
                            AdjustUtils.postRevenueAdjust(
                                it,
                                AdmobUtils.mRewardedAd?.adUnitId
                            )
                        }
                        AdmobUtils.mRewardedAd?.fullScreenContentCallback =
                            object : FullScreenContentCallback() {
                                override fun onAdShowedFullScreenContent() {
                                    AdmobUtils.isAdShowing = true
                                    adCallback2.onRewardShowed()
                                    if (AppOpenUtils.getInstance().isInitialized) {
                                        AppOpenUtils.getInstance().isAppResumeEnabled = false
                                    }
                                }

                                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                    // Called when ad fails to show.
                                    if (adError.code != 1) {
                                        AdmobUtils.isAdShowing = false
                                        adCallback2.onRewardFailed(adError.message)
                                        AdmobUtils.mRewardedAd = null
                                        AdmobUtils.dismissAdDialog()
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
                                    AdmobUtils.mRewardedAd = null
                                    AdmobUtils.isAdShowing = false
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
                            AdmobUtils.mRewardedAd?.show(activity) { adCallback2.onRewardEarned() }
                            AdmobUtils.isAdShowing = true
                        } else {
                            AdmobUtils.mRewardedAd = null
                            AdmobUtils.dismissAdDialog()
                            AdmobUtils.isAdShowing = false
                            if (AppOpenUtils.getInstance().isInitialized) {
                                AppOpenUtils.getInstance().isAppResumeEnabled = true
                            }
                        }
                    } else {
                        AdmobUtils.isAdShowing = false
                        adCallback2.onRewardFailed("None Show")
                        AdmobUtils.dismissAdDialog()
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
        adCallback2: AdmobUtils.RewardedCallback,
        enableLoadingDialog: Boolean
    ) {
        var admobId = admobId
        AdmobUtils.mInterstitialAd = null
        AdmobUtils.isAdShowing = false
        if (!AdmobUtils.isShowAds || !AdmobUtils.isNetworkConnected(
                activity
            )
        ) {
            adCallback2.onRewardClosed()
            return
        }
        if (AdmobUtils.adRequest == null) {
            AdmobUtils.initAdRequest(AdmobUtils.timeOut)
        }
        if (AdmobUtils.isTesting) {
            admobId = activity.getString(R.string.test_ads_admob_inter_reward_id)
        }
        if (enableLoadingDialog) {
            AdmobUtils.dialogLoading(activity)
        }
        AdmobUtils.isAdShowing = false
        if (AppOpenUtils.getInstance().isInitialized) {
            AppOpenUtils.getInstance().isAppResumeEnabled = false
        }
        RewardedInterstitialAd.load(activity, admobId!!,
            AdmobUtils.adRequest!!, object : RewardedInterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    // Handle the error.
                    AdmobUtils.mRewardedInterstitialAd = null
                    adCallback2.onRewardFailed(loadAdError.message)
                    AdmobUtils.dismissAdDialog()
                    if (AppOpenUtils.getInstance().isInitialized) {
                        AppOpenUtils.getInstance().isAppResumeEnabled = true
                    }
                    AdmobUtils.isAdShowing = false
                    Log.e("Admodfail", "onAdFailedToLoad" + loadAdError.message)
                    Log.e("Admodfail", "errorCodeAds" + loadAdError.cause)
                }

                override fun onAdLoaded(rewardedAd: RewardedInterstitialAd) {
                    AdmobUtils.mRewardedInterstitialAd = rewardedAd
                    if (AdmobUtils.mRewardedInterstitialAd != null) {
                        AdmobUtils.mRewardedInterstitialAd?.setOnPaidEventListener {
                            AdjustUtils.postRevenueAdjust(
                                it,
                                AdmobUtils.mRewardedInterstitialAd?.adUnitId
                            )
                        }
                        AdmobUtils.mRewardedInterstitialAd?.fullScreenContentCallback =
                            object : FullScreenContentCallback() {
                                override fun onAdShowedFullScreenContent() {
                                    AdmobUtils.isAdShowing = true
                                    adCallback2.onRewardShowed()
                                    if (AppOpenUtils.getInstance().isInitialized) {
                                        AppOpenUtils.getInstance().isAppResumeEnabled = false
                                    }
                                }

                                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                    // Called when ad fails to show.
                                    if (adError.code != 1) {
                                        AdmobUtils.isAdShowing = false
                                        adCallback2.onRewardFailed(adError.message)
                                        AdmobUtils.mRewardedInterstitialAd = null
                                        AdmobUtils.dismissAdDialog()
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
                                    AdmobUtils.mRewardedInterstitialAd = null
                                    AdmobUtils.isAdShowing = false
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
                            AdmobUtils.mRewardedInterstitialAd?.show(activity) { adCallback2.onRewardEarned() }
                            AdmobUtils.isAdShowing = true
                        } else {
                            AdmobUtils.mRewardedInterstitialAd = null
                            AdmobUtils.dismissAdDialog()
                            AdmobUtils.isAdShowing = false
                            if (AppOpenUtils.getInstance().isInitialized) {
                                AppOpenUtils.getInstance().isAppResumeEnabled = true
                            }
                        }
                    } else {
                        AdmobUtils.isAdShowing = false
                        adCallback2.onRewardFailed("None Show")
                        AdmobUtils.dismissAdDialog()
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
        adLoadCallback: AdmobUtils.RewardedInterCallback
    ) {
        var admobId = mInterstitialRewardAd.ads
        if (!AdmobUtils.isShowAds || !AdmobUtils.isNetworkConnected(
                context
            )
        ) {
            return
        }
        if (mInterstitialRewardAd.inter != null) {
            Log.d("===AdsInter", "mInterstitialRewardAd not null")
            return
        }
        if (AdmobUtils.adRequest == null) {
            AdmobUtils.initAdRequest(AdmobUtils.timeOut)
        }
        mInterstitialRewardAd.isLoading = true
        if (AdmobUtils.isTesting) {
            admobId = context.getString(R.string.test_ads_admob_inter_reward_id)
        }
        RewardedInterstitialAd.load(
            context,
            admobId,
            AdmobUtils.adRequest!!,
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
        adCallback: AdmobUtils.RewardedCallback
    ) {
        if (AdmobUtils.adRequest == null) {
            AdmobUtils.initAdRequest(AdmobUtils.timeOut)
        }
        if (!AdmobUtils.isShowAds || !AdmobUtils.isNetworkConnected(
                activity
            )
        ) {
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
                AdmobUtils.isAdShowing = false
                if (AppOpenUtils.getInstance().isInitialized) {
                    AppOpenUtils.getInstance().isAppResumeEnabled = false
                }
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                if (mInterstitialRewardAd.isLoading) {
                    AdmobUtils.dialogLoading(activity)
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
                                        AdmobUtils.isAdShowing = false
                                        AdmobUtils.dismissAdDialog()
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
                                        AdmobUtils.isAdShowing = false
                                        AdmobUtils.dismissAdDialog()
                                        adCallback.onRewardFailed(adError.message)
                                        Log.d("TAG", "The ad failed to show.")
                                    }

                                    override fun onAdShowedFullScreenContent() {
                                        AdmobUtils.isAdShowing = true
                                        adCallback.onRewardShowed()
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            AdmobUtils.dismissAdDialog()
                                        }, 800)
                                        Log.d("TAG", "The ad was shown.")
                                    }
                                }
                            it.show(activity) { adCallback.onRewardEarned() }
                        }
                    }
                } else {
                    if (mInterstitialRewardAd.inter != null) {
                        AdmobUtils.dialogLoading(activity)
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
                                    AdmobUtils.isAdShowing = false
                                    AdmobUtils.dismissAdDialog()
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
                                    AdmobUtils.isAdShowing = false
                                    AdmobUtils.dismissAdDialog()
                                    adCallback.onRewardFailed(adError.message)
                                    Log.d("TAG", "The ad failed to show.")
                                }

                                override fun onAdShowedFullScreenContent() {
                                    AdmobUtils.isAdShowing = true
                                    adCallback.onRewardShowed()
                                    Log.d("TAG", "The ad was shown.")
                                }
                            }
                        mInterstitialRewardAd.inter?.show(activity) { adCallback.onRewardEarned() }

                    } else {
                        AdmobUtils.isAdShowing = false
                        adCallback.onRewardFailed("None Show")
                        AdmobUtils.dismissAdDialog()
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
        adLoadCallback: AdmobUtils.RewardedInterCallback
    ) {
        var admobId = mInterstitialRewardAd.ads
        if (!AdmobUtils.isShowAds || !AdmobUtils.isNetworkConnected(
                context
            )
        ) {
            return
        }
        if (mInterstitialRewardAd.inter != null) {
            Log.d("===AdsInter", "mInterstitialRewardAd not null")
            return
        }
        if (AdmobUtils.adRequest == null) {
            AdmobUtils.initAdRequest(AdmobUtils.timeOut)
        }
        mInterstitialRewardAd.isLoading = true
        if (AdmobUtils.isTesting) {
            admobId = context.getString(R.string.test_ads_admob_reward_id)
        }
        RewardedAd.load(
            context,
            admobId,
            AdmobUtils.adRequest!!,
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
        adCallback: AdmobUtils.RewardedCallback
    ) {
        if (AdmobUtils.adRequest == null) {
            AdmobUtils.initAdRequest(AdmobUtils.timeOut)
        }
        if (!AdmobUtils.isShowAds || !AdmobUtils.isNetworkConnected(
                activity
            )
        ) {
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
                AdmobUtils.isAdShowing = false
                if (AppOpenUtils.getInstance().isInitialized) {
                    AppOpenUtils.getInstance().isAppResumeEnabled = false
                }
            }
        }

        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.Main) {
                if (mInterstitialRewardAd.isLoading) {
                    AdmobUtils.dialogLoading(activity)
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
                                        AdmobUtils.isAdShowing = false
                                        AdmobUtils.dismissAdDialog()
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
                                        AdmobUtils.isAdShowing = false
                                        AdmobUtils.dismissAdDialog()
                                        adCallback.onRewardFailed(adError.message)
                                        Log.d("TAG", "The ad failed to show.")
                                    }

                                    override fun onAdShowedFullScreenContent() {
                                        AdmobUtils.isAdShowing = true
                                        adCallback.onRewardShowed()
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            AdmobUtils.dismissAdDialog()
                                        }, 800)
                                        Log.d("TAG", "The ad was shown.")
                                    }
                                }
                            it.show(activity) { adCallback.onRewardEarned() }
                        }
                    }
                } else {
                    if (mInterstitialRewardAd.inter != null) {
                        AdmobUtils.dialogLoading(activity)
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
                                    AdmobUtils.isAdShowing = false
                                    AdmobUtils.dismissAdDialog()
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
                                    AdmobUtils.isAdShowing = false
                                    AdmobUtils.dismissAdDialog()
                                    adCallback.onRewardFailed(adError.message)
                                    Log.d("TAG", "The ad failed to show.")
                                }

                                override fun onAdShowedFullScreenContent() {
                                    AdmobUtils.isAdShowing = true
                                    adCallback.onRewardShowed()
                                    Log.d("TAG", "The ad was shown.")
                                }
                            }
                        mInterstitialRewardAd.inter?.show(activity) { adCallback.onRewardEarned() }

                    } else {
                        AdmobUtils.isAdShowing = false
                        adCallback.onRewardFailed("None Show")
                        AdmobUtils.dismissAdDialog()
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
        adCallback: AdmobUtils.InterCallback,
        enableLoadingDialog: Boolean
    ) {
        var admobId = admobId.ads
        AdmobUtils.mInterstitialAd = null
        AdmobUtils.isAdShowing = false
        if (AdmobUtils.adRequest == null) {
            AdmobUtils.initAdRequest(AdmobUtils.timeOut)
        }
        if (!AdmobUtils.isShowAds || !AdmobUtils.isNetworkConnected(
                activity
            )
        ) {
            adCallback.onInterFailed("No internet")
            return
        }
        if (AppOpenUtils.getInstance().isInitialized) {
            if (!AppOpenUtils.getInstance().isAppResumeEnabled) {
                return
            } else {
                AdmobUtils.isAdShowing = false
                if (AppOpenUtils.getInstance().isInitialized) {
                    AppOpenUtils.getInstance().isAppResumeEnabled = false
                }
            }
        }

        if (enableLoadingDialog) {
            AdmobUtils.dialogLoading(activity)
        }
        if (AdmobUtils.isTesting) {
            admobId = activity.getString(R.string.test_ads_admob_inter_id)
        } else {
            AdmobUtils.checkIdTest(activity, admobId)
        }
        InterstitialAd.load(
            activity,
            admobId,
            AdmobUtils.adRequest!!,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    super.onAdLoaded(interstitialAd)
                    adCallback.onInterLoaded()
                    Handler(Looper.getMainLooper()).postDelayed({
                        AdmobUtils.mInterstitialAd = interstitialAd
                        if (AdmobUtils.mInterstitialAd != null) {
                            AdmobUtils.mInterstitialAd!!.onPaidEventListener =
                                OnPaidEventListener { adValue: AdValue? ->
                                    adValue?.let {
                                        AdjustUtils.postRevenueAdjustInter(
                                            AdmobUtils.mInterstitialAd!!,
                                            it, AdmobUtils.mInterstitialAd!!.adUnitId
                                        )
                                    }
                                }
                            AdmobUtils.mInterstitialAd!!.fullScreenContentCallback =
                                object : FullScreenContentCallback() {
                                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                        adCallback.onInterFailed(adError.message)
                                        AdmobUtils.isAdShowing = false
                                        if (AppOpenUtils.getInstance().isInitialized) {
                                            AppOpenUtils.getInstance().isAppResumeEnabled = true
                                        }
                                        AdmobUtils.isAdShowing = false
                                        if (AdmobUtils.mInterstitialAd != null) {
                                            AdmobUtils.mInterstitialAd = null
                                        }
                                        AdmobUtils.dismissAdDialog()
                                        Log.e("Admodfail", "onAdFailedToLoad" + adError.message)
                                        Log.e("Admodfail", "errorCodeAds" + adError.cause)
                                    }

                                    override fun onAdDismissedFullScreenContent() {
                                        AdmobUtils.lastTimeShowInterstitial = Date().time
                                        adCallback.onDismissedInter()
                                        if (AdmobUtils.mInterstitialAd != null) {
                                            AdmobUtils.mInterstitialAd = null
                                        }
                                        AdmobUtils.isAdShowing = false
                                        if (AppOpenUtils.getInstance().isInitialized) {
                                            AppOpenUtils.getInstance().isAppResumeEnabled = true
                                        }
                                    }

                                    override fun onAdShowedFullScreenContent() {
                                        super.onAdShowedFullScreenContent()
                                        Log.e("===onAdShowed", "onAdShowedFullScreenContent")
                                        adCallback.onInterShowed()
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            AdmobUtils.dismissAdDialog()
                                        }, 800)
                                    }
                                }
                            if (activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && AdmobUtils.mInterstitialAd != null) {
                                adCallback.onStartAction()
                                AdmobUtils.mInterstitialAd!!.show(activity)
                                AdmobUtils.isAdShowing = true
                            } else {
                                AdmobUtils.mInterstitialAd = null
                                AdmobUtils.dismissAdDialog()
                                AdmobUtils.isAdShowing = false
                                if (AppOpenUtils.getInstance().isInitialized) {
                                    AppOpenUtils.getInstance().isAppResumeEnabled = true
                                }
                                adCallback.onInterFailed("Interstitial can't show in background")
                            }
                        } else {
                            AdmobUtils.dismissAdDialog()
                            adCallback.onInterFailed("mInterstitialAd null")
                            AdmobUtils.isAdShowing = false
                            if (AppOpenUtils.getInstance().isInitialized) {
                                AppOpenUtils.getInstance().isAppResumeEnabled = true
                            }
                        }
                    }, 800)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    super.onAdFailedToLoad(loadAdError)
                    AdmobUtils.mInterstitialAd = null
                    if (AppOpenUtils.getInstance().isInitialized) {
                        AppOpenUtils.getInstance().isAppResumeEnabled = true
                    }
                    AdmobUtils.isAdShowing = false
                    adCallback.onInterFailed(loadAdError.message)
                    AdmobUtils.dismissAdDialog()
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
        return AdmobUtils.md5(android_id).uppercase(Locale.getDefault())
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
        AdmobUtils.dialogFullScreen = Dialog(activity)
        AdmobUtils.dialogFullScreen?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        AdmobUtils.dialogFullScreen?.setContentView(R.layout.dialog_full_screen)
        AdmobUtils.dialogFullScreen?.setCancelable(false)
        AdmobUtils.dialogFullScreen?.window!!.setBackgroundDrawable(ColorDrawable(Color.WHITE))
        AdmobUtils.dialogFullScreen?.window!!.setLayout(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        val img = AdmobUtils.dialogFullScreen?.findViewById<LottieAnimationView>(R.id.imageView3)
        img?.setAnimation(R.raw.gifloading)
        try {
            if (!activity.isFinishing && AdmobUtils.dialogFullScreen != null && AdmobUtils.dialogFullScreen?.isShowing == false) {
                AdmobUtils.dialogFullScreen?.show()
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
        nativeHolder: com.dino.ads.utils.admod.NativeHolderAdmob,
        adCallback: AdmobUtils.NativeCallback
    ) {
        if (!AdmobUtils.isShowAds || !AdmobUtils.isNetworkConnected(
                context
            )
        ) {
            adCallback.onNativeFailed("No internet")
            return
        }
        //If native is loaded return
        if (nativeHolder.nativeAd != null) {
            Log.d("===AdsLoadsNative", "Native not null")
            return
        }
        if (AdmobUtils.isTesting) {
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
        if (AdmobUtils.adRequest != null) {
            adLoader.loadAd(AdmobUtils.adRequest!!)
        }
    }

    /**
     * Example: AdmobUtils.showNative(activity, holder, viewGroup, R.layout.ad_unified_medium,
     * AdNativeSize.MEDIUM, object : AdmobUtils.NativeCallbackSimple {})
     */
    @JvmStatic
    fun showNative(
        activity: Activity,
        nativeHolder: com.dino.ads.utils.admod.NativeHolderAdmob,
        viewGroup: ViewGroup,
        layout: Int,
        size: AdNativeSize,
        callback: AdmobUtils.NativeCallbackSimple
    ) {
        if (!AdmobUtils.isShowAds || !AdmobUtils.isNetworkConnected(
                activity
            )
        ) {
            viewGroup.visibility = View.GONE
            return
        }
        if (AdmobUtils.shimmerFrameLayout != null) {
            AdmobUtils.shimmerFrameLayout?.stopShimmer()
        }
        try {
            viewGroup.removeAllViews()
        } catch (_: Exception) {

        }

        if (!nativeHolder.isLoad) {
            if (nativeHolder.nativeAd != null) {
                val adView = activity.layoutInflater.inflate(layout, null) as NativeAdView
                populateNativeAdView(nativeHolder.nativeAd!!, adView, size)
                if (AdmobUtils.shimmerFrameLayout != null) {
                    AdmobUtils.shimmerFrameLayout?.stopShimmer()
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
                if (AdmobUtils.shimmerFrameLayout != null) {
                    AdmobUtils.shimmerFrameLayout?.stopShimmer()
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

            if (AdmobUtils.shimmerFrameLayout == null) AdmobUtils.shimmerFrameLayout =
                tagView.findViewById(R.id.shimmer_view_container)
            AdmobUtils.shimmerFrameLayout?.startShimmer()
            nativeHolder.native_mutable.observe((activity as LifecycleOwner)) { nativeAd: NativeAd? ->
                if (nativeAd != null) {
                    nativeAd.setOnPaidEventListener {
                        AdjustUtils.postRevenueAdjustNative(nativeAd, it, nativeHolder.ads)
                        callback.onPaid(it, nativeHolder.ads)
                    }
                    val adView = activity.layoutInflater.inflate(layout, null) as NativeAdView
                    populateNativeAdView(nativeAd, adView, size)
                    if (AdmobUtils.shimmerFrameLayout != null) {
                        AdmobUtils.shimmerFrameLayout?.stopShimmer()
                    }
                    try {
                        viewGroup.removeAllViews()
                        viewGroup.addView(adView)
                    } catch (_: Exception) {

                    }

                    callback.onNativeLoaded()
                    nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
                } else {
                    if (AdmobUtils.shimmerFrameLayout != null) {
                        AdmobUtils.shimmerFrameLayout?.stopShimmer()
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
        nativeHolder: com.dino.ads.utils.admod.NativeHolderAdmob,
        viewGroup: ViewGroup,
        layout: Int,
        size: AdNativeSize,
        adCallback: AdmobUtils.NativeCallback
    ) {
        Log.d("===Native", "Native1")
        if (!AdmobUtils.isShowAds || !AdmobUtils.isNetworkConnected(
                activity
            )
        ) {
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

        if (AdmobUtils.isTesting) {
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
        if (AdmobUtils.adRequest != null) {
            adLoader.loadAd(AdmobUtils.adRequest!!)
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
        nativeHolder: com.dino.ads.utils.admod.NativeHolderAdmob,
        viewGroup: ViewGroup,
        layout: Int,
        size: AdNativeSize,
        adCallback: AdmobUtils.NativeCallback
    ) {
        Log.d("===Native", "Native1")
        if (!AdmobUtils.isShowAds || !AdmobUtils.isNetworkConnected(
                activity
            )
        ) {
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

        if (AdmobUtils.isTesting) {
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
        if (AdmobUtils.adRequest != null) {
            adLoader.loadAd(AdmobUtils.adRequest!!)
        }
    }

    @JvmStatic
    fun loadAndShowNativeNoShimmer(
        activity: Activity,
        nativeHolder: com.dino.ads.utils.admod.NativeHolderAdmob,
        viewGroup: ViewGroup,
        layout: Int,
        size: AdNativeSize,
        adCallback: AdmobUtils.NativeCallback
    ) {
        Log.d("===Native", "Native1")
        if (!AdmobUtils.isShowAds || !AdmobUtils.isNetworkConnected(
                activity
            )
        ) {
            viewGroup.visibility = View.GONE
            return
        }
        var s = nativeHolder.ads
        if (AdmobUtils.isTesting) {
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
        if (AdmobUtils.adRequest != null) {
            adLoader.loadAd(AdmobUtils.adRequest!!)
        }
        Log.e("Admod", "loadAdNativeAds")
    }

    fun loadAndShowNativeFullScreen(
        activity: Activity,
        id: String,
        viewGroup: ViewGroup,
        layout: Int,
        mediaAspectRatio: Int,
        listener: AdmobUtils.NativeFullScreenCallback
    ) {
        if (!AdmobUtils.isShowAds || !AdmobUtils.isNetworkConnected(
                activity
            )
        ) {
            viewGroup.visibility = View.GONE
            return
        }
        var adMobId: String = id
        if (AdmobUtils.isTesting) {
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

        AdmobUtils.shimmerFrameLayout = tagView.findViewById(R.id.shimmer_view_container)
        AdmobUtils.shimmerFrameLayout?.startShimmer()
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
            AdmobUtils.shimmerFrameLayout?.stopShimmer()
            try {
                viewGroup.addView(adView)
            } catch (_: Exception) {

            }

        }
        builder.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Log.d("===AdmobFailed", loadAdError.toString())
                AdmobUtils.shimmerFrameLayout?.stopShimmer()
                listener.onLoadFailed()
            }
        })
        if (AdmobUtils.adRequest != null) {
            builder.build().loadAd(AdmobUtils.adRequest!!)
        }
    }

    @JvmStatic
    fun loadNativeFullScreen(
        context: Context,
        nativeHolder: com.dino.ads.utils.admod.NativeHolderAdmob, mediaAspectRatio: Int,
        adCallback: AdmobUtils.NativeCallback
    ) {
        if (!AdmobUtils.isShowAds || !AdmobUtils.isNetworkConnected(
                context
            )
        ) {
            adCallback.onNativeFailed("No internet")
            return
        }
        //If native is loaded return
        if (nativeHolder.nativeAd != null) {
            Log.d("===AdsLoadsNative", "Native not null")
            return
        }
        if (AdmobUtils.isTesting) {
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
        if (AdmobUtils.adRequest != null) {
            adLoader.build().loadAd(AdmobUtils.adRequest!!)
        }
    }

    @JvmStatic
    fun showNativeFullScreen(
        activity: Activity,
        nativeHolder: com.dino.ads.utils.admod.NativeHolderAdmob,
        viewGroup: ViewGroup,
        layout: Int,
        callback: AdmobUtils.NativeCallbackSimple
    ) {
        if (!AdmobUtils.isShowAds || !AdmobUtils.isNetworkConnected(
                activity
            )
        ) {
            viewGroup.visibility = View.GONE
            return
        }
        if (AdmobUtils.shimmerFrameLayout != null) {
            AdmobUtils.shimmerFrameLayout?.stopShimmer()
        }
        viewGroup.removeAllViews()
        if (!nativeHolder.isLoad) {
            if (nativeHolder.nativeAd != null) {
                val adView = activity.layoutInflater.inflate(layout, null) as NativeAdView
                populateNativeAdView(
                    nativeHolder.nativeAd!!,
                    adView.findViewById(R.id.native_ad_view)
                )
                AdmobUtils.shimmerFrameLayout?.stopShimmer()
                if (AdmobUtils.shimmerFrameLayout != null) {
                    AdmobUtils.shimmerFrameLayout?.stopShimmer()
                }
                nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
                viewGroup.removeAllViews()
                viewGroup.addView(adView)
                callback.onNativeLoaded()
            } else {
                if (AdmobUtils.shimmerFrameLayout != null) {
                    AdmobUtils.shimmerFrameLayout?.stopShimmer()
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

            if (AdmobUtils.shimmerFrameLayout == null) AdmobUtils.shimmerFrameLayout =
                tagView.findViewById(R.id.shimmer_view_container)
            AdmobUtils.shimmerFrameLayout?.startShimmer()
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
                    if (AdmobUtils.shimmerFrameLayout != null) {
                        AdmobUtils.shimmerFrameLayout?.stopShimmer()
                    }

                    viewGroup.removeAllViews()
                    viewGroup.addView(adView)

                    callback.onNativeLoaded()
                    nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
                } else {
                    if (AdmobUtils.shimmerFrameLayout != null) {
                        AdmobUtils.shimmerFrameLayout?.stopShimmer()
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
        listener: AdmobUtils.NativeFullScreenCallback
    ) {
        if (!AdmobUtils.isShowAds || !AdmobUtils.isNetworkConnected(
                activity
            )
        ) {
            viewGroup.visibility = View.GONE
            return
        }
        var adMobId: String = id
        if (AdmobUtils.isTesting) {
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
        if (AdmobUtils.adRequest != null) {
            builder.build().loadAd(AdmobUtils.adRequest!!)
        }
    }

    @JvmStatic
    fun loadNativeNoButton(
        activity: Activity,
        nativeHolder: com.dino.ads.utils.admod.NativeHolderAdmob,
        viewGroup: ViewGroup,
        layout: Int,
        size: AdNativeSize,
        adCallback: AdmobUtils.NativeCallback
    ) {
        Log.d("===Native", "Native1")
        if (!AdmobUtils.isShowAds || !AdmobUtils.isNetworkConnected(
                activity
            )
        ) {
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

        if (AdmobUtils.isTesting) {
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
        if (AdmobUtils.adRequest != null) {
            adLoader.loadAd(AdmobUtils.adRequest!!)
        }
        Log.e("Admod", "loadAdNativeAds")
    }

    @JvmStatic
    fun showNativeNoButton(
        activity: Activity,
        nativeHolder: com.dino.ads.utils.admod.NativeHolderAdmob,
        viewGroup: ViewGroup,
        layout: Int,
        size: AdNativeSize,
        callback: AdmobUtils.NativeCallbackSimple
    ) {
        if (!AdmobUtils.isShowAds || !AdmobUtils.isNetworkConnected(
                activity
            )
        ) {
            viewGroup.visibility = View.GONE
            return
        }
        if (AdmobUtils.shimmerFrameLayout != null) {
            AdmobUtils.shimmerFrameLayout?.stopShimmer()
        }
        try {
            viewGroup.removeAllViews()
        } catch (_: Exception) {

        }
        if (!nativeHolder.isLoad) {
            if (nativeHolder.nativeAd != null) {
                val adView = activity.layoutInflater.inflate(layout, null) as NativeAdView
                populateNativeAdViewNoBtn(nativeHolder.nativeAd!!, adView, size)
                if (AdmobUtils.shimmerFrameLayout != null) {
                    AdmobUtils.shimmerFrameLayout?.stopShimmer()
                }
                nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
                try {
                    viewGroup.removeAllViews()
                    viewGroup.addView(adView)
                } catch (_: Exception) {

                }
                callback.onNativeLoaded()
            } else {
                if (AdmobUtils.shimmerFrameLayout != null) {
                    AdmobUtils.shimmerFrameLayout?.stopShimmer()
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

            if (AdmobUtils.shimmerFrameLayout == null) AdmobUtils.shimmerFrameLayout =
                tagView.findViewById(R.id.shimmer_view_container)
            AdmobUtils.shimmerFrameLayout?.startShimmer()
            nativeHolder.native_mutable.observe((activity as LifecycleOwner)) { nativeAd: NativeAd? ->
                if (nativeAd != null) {
                    nativeAd.setOnPaidEventListener {
                        AdjustUtils.postRevenueAdjustNative(nativeAd, it, nativeHolder.ads)
                        callback.onPaid(it, nativeHolder.ads)
                    }
                    val adView = activity.layoutInflater.inflate(layout, null) as NativeAdView
                    populateNativeAdViewNoBtn(nativeAd, adView, size)
                    if (AdmobUtils.shimmerFrameLayout != null) {
                        AdmobUtils.shimmerFrameLayout?.stopShimmer()
                    }
                    try {
                        viewGroup.removeAllViews()
                        viewGroup.addView(adView)
                    } catch (_: Exception) {

                    }
                    callback.onNativeLoaded()
                    nativeHolder.native_mutable.removeObservers((activity as LifecycleOwner))
                } else {
                    if (AdmobUtils.shimmerFrameLayout != null) {
                        AdmobUtils.shimmerFrameLayout?.stopShimmer()
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
