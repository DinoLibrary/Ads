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
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.airbnb.lottie.LottieAnimationView
import com.dino.ads.NativeHelper.Companion.populateNativeAdView
import com.dino.ads.NativeHelper.Companion.populateNativeAdViewClose
import com.dino.ads.NativeHelper.Companion.populateNativeAdViewNoBtn
import com.dino.ads.adjust.AdjustUtils
import com.dino.ads.cmp.GoogleMobileAdsConsentManager
import com.dino.ads.remote_config.AdmobHolder
import com.dino.ads.remote_config.RemoteConfigUtils
import com.dino.ads.utils.SweetAlert.SweetAlertDialog
import com.dino.ads.utils.admod.BannerAdmob
import com.dino.ads.utils.admod.NativeAdmob
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
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean

object AdmobUtils {
    private var dialog: SweetAlertDialog? = null
    private var dialogFullScreen: Dialog? = null

    // Biến check lần cuối hiển thị quảng cáo
    var lastTimeShowInterstitial: Long = 0

    // Timeout init admob
    var timeOut = 10000

    //Check quảng cáo đang show hay không
    @JvmField
    var isAdShowing = false
    var isClick = false

    //Ẩn hiện quảng cáo
    @JvmField
    var isEnableAds = false

    //Dùng ID Test để hiển thị quảng cáo
    @JvmField
    var isTesting = false
    var isInitialized = false
    private val isConsented = AtomicBoolean(false)

    //List device test
    private var testDevices: MutableList<String> = ArrayList()

    @JvmField
    var mRewardedAd: RewardedAd? = null
    var mRewardedInterstitialAd: RewardedInterstitialAd? = null
    var mInterstitialAd: InterstitialAd? = null
    var shimmerFrameLayout: ShimmerFrameLayout? = null

    private var adRequest: AdRequest? = null

    fun setupCMP(activity: Activity, onCompleted: () -> Unit) {
        if (isConsented.get()) return
        isConsented.set(true)
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
        this.isEnableAds = RemoteConfigUtils.enableAds()
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

    private fun checkTestAd(ad: NativeAd?) {
        if (!isEnableAds) {
            return
        }
//        if (checkTestAd == null) {
//            Log.e("===CheckAdsTest", "check_test_ad hasn't been initialized!!!")
//        }
        if (RemoteConfigUtils.checkTestAd()) {
            try {
                val testAdResponse = ad?.headline.toString().replace(" ", "").split(":")[0]
                Log.d("===CheckAdsTest", ad?.headline.toString().replace(" ", "").split(":")[0])
                val testAdResponses = arrayOf(
                    "TestAd",
                    "Anunciodeprueba",
                    "Annoncetest",
                    "테스트광고",
                    "Annuncioditesto",
                    "Testanzeige",
                    "TesIklan",
                    "Anúnciodeteste",
                    "Тестовоеобъявление",
                    "পরীক্ষামূলকবিজ্ঞাপন",
                    "जाँचविज्ञापन",
                    "إعلانتجريبي",
                    "Quảngcáothửnghiệm"
                )
                isEnableAds = !testAdResponses.contains(testAdResponse)
            } catch (_: Exception) {
                isEnableAds = false
                Log.d("===CheckAdsTest", "Error")
            }
        }
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
    fun loadAndShowBanner(
        activity: Activity,
        adHolder: BannerAdmob,
        viewGroup: ViewGroup,
        callback: BannerCallback
    ) {
        if (!isTesting && adHolder.remoteValue.isBlank()) throw Exception("ERROR: remote value is blank")
        if (!isEnableAds || !isNetworkConnected(activity) || adHolder.remoteValue == "0") {
            viewGroup.visibility = View.GONE
            callback.onBannerFailed("Not show banner")
            return
        }
        var bannerId = adHolder.ads
        val mAdView = AdView(activity)
        if (isTesting) {
            bannerId = activity.getString(R.string.test_admob_banner_id)
        }
        mAdView.adUnitId = bannerId
        val adSize = getBannerSize(activity)
        mAdView.setAdSize(adSize)
        val tagView = activity.layoutInflater.inflate(R.layout.layout_banner_loading, null, false)

        try {
            viewGroup.removeAllViews()
            viewGroup.addView(tagView, 0)
            viewGroup.addView(mAdView, 1)
        } catch (_: Exception) {

        }

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
                Log.e("===Admob", "onAdFailedToLoad" + adError.message)
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
        adRequest?.let { mAdView.loadAd(it) }
        Log.d("===Admob", "loadAdBanner")
    }

    @JvmStatic
    fun loadAndShowBannerDual(
        activity: Activity,
        adHolder: BannerAdmob,
        adHolderCollap: BannerAdmob,
        viewGroup: ViewGroup,
        callback: BannerCallback,
        anchor: String = "bottom"
    ) {
        if (!isTesting && adHolder.remoteValue.isBlank()) throw Exception("ERROR: remote value is blank")
        if (!isEnableAds || !isNetworkConnected(activity) || adHolder.remoteValue == "0") {
            viewGroup.visibility = View.GONE
            callback.onBannerFailed("Not show banner")
            return
        }
        if (adHolder.remoteValue == "2") {
            loadAndShowBannerCollap(activity, adHolder, viewGroup, callback, anchor)
        } else {
            loadAndShowBanner(activity, adHolder, viewGroup, callback)
        }
    }

    @JvmStatic
    /**
     * Use loadAndShowBannerDual() instead
     */
    private fun loadAndShowBannerCollap(
        activity: Activity,
        adHolder: BannerAdmob,
        viewGroup: ViewGroup,
        callback: BannerCallback,
        anchor: String = "bottom"
    ) {
        if (!isTesting && adHolder.remoteValue.isBlank()) throw Exception("ERROR: remote value is blank")
        if (!isEnableAds || !isNetworkConnected(activity) || adHolder.remoteValue == "0") {
            viewGroup.visibility = View.GONE
            callback.onBannerFailed("Not show banner")
            return
        }
        var bannerId = adHolder.ads
        adHolder.mAdView?.let {
            it.destroy()
            viewGroup.removeView(it)
        }
        adHolder.mAdView = AdView(activity)
        if (isTesting) {
            bannerId = activity.getString(R.string.test_admob_banner_collap_id)
        }
        adHolder.mAdView?.adUnitId = bannerId
        val tagView = activity.layoutInflater.inflate(R.layout.layout_banner_loading, null, false)

        try {
            viewGroup.removeAllViews()
            viewGroup.addView(tagView, 0)
            viewGroup.addView(adHolder.mAdView, 1)
        } catch (_: Exception) {

        }

        val adSize = getBannerSize(activity)
        adHolder.mAdView?.setAdSize(adSize)
        shimmerFrameLayout = tagView.findViewById(R.id.shimmer_view_container)
        shimmerFrameLayout?.startShimmer()

        adHolder.mAdView?.adListener = object : AdListener() {
            override fun onAdLoaded() {
                adHolder.mAdView?.onPaidEventListener = OnPaidEventListener { adValue ->
                    AdjustUtils.postRevenueAdjust(
                        adValue, adHolder.mAdView?.adUnitId
                    )
                }
                shimmerFrameLayout?.stopShimmer()
                viewGroup.removeView(tagView)
                callback.onBannerLoaded(adSize)
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e("===Admob", "onAdFailedToLoad" + adError.message)
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
        extras.putString("collapsible", anchor)
        val adRequestCollap =
            AdRequest.Builder().addNetworkExtrasBundle(AdMobAdapter::class.java, extras).build()
        adHolder.mAdView?.loadAd(adRequestCollap)
        Log.d("===Admob", "loadAdBanner")
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
    fun loadInterstitial(context: Context, holder: AdmobHolder, adLoadCallback: LoadInterCallback) {
        isAdShowing = false
        val remoteValue = RemoteConfigUtils.getValue("inter_${holder.uid}")
        if (!isEnableAds || !isNetworkConnected(context) || remoteValue == "0") {
            adLoadCallback.onInterFailed("Not show inter")
            return
        }
        if (holder.inter != null) {
            Log.d("===AdsInter", "Inter not null")
            return
        }
        holder.check = true
        if (adRequest == null) {
            initAdRequest(timeOut)
        }
        val adId = if (isTesting) {
            context.getString(R.string.test_admob_inter_id)
        } else {
            RemoteConfigUtils.getValue("inter_${holder.uid}_id")
        }
        InterstitialAd.load(context, adId, adRequest!!, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                if (isClick) {
                    holder.mutable.value = interstitialAd
                }
                holder.inter = interstitialAd
                holder.check = false
                holder.inter!!.setOnPaidEventListener { adValue ->
                    AdjustUtils.postRevenueAdjustInter(
                        holder.inter!!, adValue, holder.inter!!.adUnitId
                    )
                    adLoadCallback.onPaid(adValue, holder.inter!!.adUnitId)
                }
                adLoadCallback.onInterLoaded(interstitialAd, false)
                Log.i("===Admob", "onAdLoaded")
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                isAdShowing = false
                if (mInterstitialAd != null) {
                    mInterstitialAd = null
                }
                holder.check = false
                if (isClick) {
                    holder.mutable.value = null
                }
                adLoadCallback.onInterFailed(loadAdError.message)
            }
        })
    }

    @JvmStatic
    fun showInterstitial(
        activity: Activity,
        holder: AdmobHolder,
        timeout: Long,
        adCallback: InterCallback?,
        enableLoadingDialog: Boolean
    ) {
        isClick = true
        val remoteValue = RemoteConfigUtils.getValue("inter_${holder.uid}")
        if (!isEnableAds || !isNetworkConnected(activity) || remoteValue == "0") {
            isAdShowing = false
            if (AppOpenUtils.getInstance().isInitialized) {
                AppOpenUtils.getInstance().isAppResumeEnabled = true
            }
            adCallback?.onInterFailed("Not show inter")
            return
        }

        holder.interCount++
        if (holder.interCount % remoteValue.toInt() != 0) {
            adCallback?.onInterFailed("Not show Inter: count=${holder.interCount}")
            return
        }

        adCallback?.onInterLoaded()
        val handler = Handler(Looper.getMainLooper())
        //Check timeout show inter
        val runnable = Runnable {
            if (holder.check) {
                if (AppOpenUtils.getInstance().isInitialized) {
                    AppOpenUtils.getInstance().isAppResumeEnabled = true
                }
                isClick = false
                holder.mutable.removeObservers((activity as LifecycleOwner))
                isAdShowing = false
                dismissAdDialog()
                adCallback?.onInterFailed("timeout")
            }
        }
        handler.postDelayed(runnable, timeout)
        //Inter is Loading...
        if (holder.check) {
            if (enableLoadingDialog) {
                dialogLoading(activity)
            }
            holder.mutable.observe((activity as LifecycleOwner)) { aBoolean: InterstitialAd? ->
                if (aBoolean != null) {
                    holder.mutable.removeObservers((activity as LifecycleOwner))
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
                                holder.inter = null
                                holder.mutable.removeObservers((activity as LifecycleOwner))
                                holder.mutable.value = null
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
                                holder.inter = null
                                dismissAdDialog()
                                Log.e("Admobfail", "onAdFailedToLoad" + adError.message)
                                Log.e("Admobfail", "errorCodeAds" + adError.cause)
                                holder.mutable.removeObservers((activity as LifecycleOwner))
                                holder.mutable.value = null
                                handler.removeCallbacksAndMessages(null)
                                adCallback?.onInterFailed(adError.message)
                            }

                            override fun onAdShowedFullScreenContent() {
                                handler.removeCallbacksAndMessages(null)
                                isAdShowing = true
                                adCallback?.onInterShowed()

                            }
                        }
                        startShowingInterstitial(
                            activity, aBoolean, adCallback
                        )
                    }, 400)
                } else {
                    holder.check = true
                }
            }
            return
        }
        //Load inter done
        if (holder.inter == null) {
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
                holder.inter?.fullScreenContentCallback =
                    object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            isAdShowing = false
                            if (AppOpenUtils.getInstance().isInitialized) {
                                AppOpenUtils.getInstance().isAppResumeEnabled = true
                            }
                            isClick = false
                            holder.mutable.removeObservers((activity as LifecycleOwner))
                            holder.inter = null
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
                            holder.inter = null
                            holder.mutable.removeObservers((activity as LifecycleOwner))
                            isAdShowing = false
                            dismissAdDialog()
                            adCallback?.onInterFailed(adError.message)
                            Log.e("Admobfail", "onAdFailedToLoad" + adError.message)
                            Log.e("Admobfail", "errorCodeAds" + adError.cause)
                        }

                        override fun onAdShowedFullScreenContent() {
                            handler.removeCallbacksAndMessages(null)
                            isAdShowing = true
                            adCallback?.onInterShowed()
                        }
                    }
                startShowingInterstitial(
                    activity, holder.inter, adCallback
                )
            }, 400)
        }
    }

    @JvmStatic
    private fun startShowingInterstitial(
        activity: Activity, mInterstitialAd: InterstitialAd?, callback: InterCallback?
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
    fun loadAndShowInterstitial(
        activity: AppCompatActivity,
        holder: AdmobHolder,
        adCallback: InterCallback,
        enableLoadingDialog: Boolean
    ) {
        mInterstitialAd = null
        val remoteValue = RemoteConfigUtils.getValue("inter_${holder.uid}")
        if (!isEnableAds || !isNetworkConnected(activity) || remoteValue == "0") {
            adCallback.onInterFailed("Not show Inter")
            return
        }

        holder.interCount++
        if (holder.interCount % remoteValue.toInt() != 0) {
            adCallback.onInterFailed("Not show Inter: count=${holder.interCount}")
            return
        }

        isAdShowing = false
        if (adRequest == null) {
            initAdRequest(timeOut)
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
        val adId = if (isTesting) {
            activity.getString(R.string.test_admob_inter_id)
        } else {
            RemoteConfigUtils.getValue("inter${holder.uid}_id")
        }
        InterstitialAd.load(
            activity, adId, adRequest!!, object : InterstitialAdLoadCallback() {
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
                                            mInterstitialAd!!, it, mInterstitialAd!!.adUnitId
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
                                        Log.e("===Admob", "onAdFailedToLoad" + adError.message)
                                        Log.e("===Admob", "errorCodeAds" + adError.cause)
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
                                        Log.e("===Admob", "onAdShowedFullScreenContent")
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

    @JvmStatic
    fun dismissAdDialog() {
        try {
            if (dialog != null && dialog!!.isShowing) {
                dialog!!.dismiss()
            }
            if (dialogFullScreen != null && dialogFullScreen!!.isShowing) {
                dialogFullScreen!!.dismiss()
            }
        } catch (_: Exception) {

        }
    }

    @JvmStatic
    fun loadAndShowRewarded(
        activity: Activity,
        holder: AdmobHolder,
        callback: RewardedCallback,
        enableLoadingDialog: Boolean
    ) {
        mRewardedAd = null
        isAdShowing = false
        val remoteValue = RemoteConfigUtils.getValue("reward${holder.uid}")
        if (!isEnableAds || !isNetworkConnected(activity) || remoteValue == "0") {
            callback.onRewardClosed()
            return
        }
        if (adRequest == null) {
            initAdRequest(timeOut)
        }
        val adId = if (isTesting) {
            activity.getString(R.string.test_admob_reward_id)
        } else {
            RemoteConfigUtils.getValue("reward${holder.uid}_id")
        }
        if (enableLoadingDialog) {
            dialogLoading(activity)
        }
        isAdShowing = false
        if (AppOpenUtils.getInstance().isInitialized) {
            AppOpenUtils.getInstance().isAppResumeEnabled = false
        }
        RewardedAd.load(
            activity, adId, adRequest!!, object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    // Handle the error.
                    mRewardedAd = null
                    callback.onRewardFailed(loadAdError.message)
                    dismissAdDialog()
                    if (AppOpenUtils.getInstance().isInitialized) {
                        AppOpenUtils.getInstance().isAppResumeEnabled = true
                    }
                    isAdShowing = false
                    Log.e("===Admob", "onAdFailedToLoad" + loadAdError.message)
                    Log.e("===Admob", "errorCodeAds" + loadAdError.cause)
                }

                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    mRewardedAd = rewardedAd
//                    if (mRewardedAd != null) {
                    mRewardedAd?.setOnPaidEventListener {
                        AdjustUtils.postRevenueAdjust(
                            it, mRewardedAd?.adUnitId
                        )
                    }
                    mRewardedAd?.fullScreenContentCallback =
                        object : FullScreenContentCallback() {
                            override fun onAdShowedFullScreenContent() {
                                isAdShowing = true
                                callback.onRewardShowed()
                                if (AppOpenUtils.getInstance().isInitialized) {
                                    AppOpenUtils.getInstance().isAppResumeEnabled = false
                                }
                            }

                            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                // Called when ad fails to show.
                                if (adError.code != 1) {
                                    isAdShowing = false
                                    callback.onRewardFailed(adError.message)
                                    mRewardedAd = null
                                    dismissAdDialog()
                                }
                                if (AppOpenUtils.getInstance().isInitialized) {
                                    AppOpenUtils.getInstance().isAppResumeEnabled = true
                                }
                                Log.e("Admobfail", "onAdFailedToLoad" + adError.message)
                                Log.e("Admobfail", "errorCodeAds" + adError.cause)
                            }

                            override fun onAdDismissedFullScreenContent() {
                                // Called when ad is dismissed.
                                // Set the ad reference to null so you don't show the ad a second time.
                                mRewardedAd = null
                                isAdShowing = false
                                callback.onRewardClosed()
                                if (AppOpenUtils.getInstance().isInitialized) {
                                    AppOpenUtils.getInstance().isAppResumeEnabled = true
                                }
                            }
                        }
                    if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        if (AppOpenUtils.getInstance().isInitialized) {
                            AppOpenUtils.getInstance().isAppResumeEnabled = false
                        }
                        mRewardedAd?.show(activity) { callback.onRewardEarned() }
                        isAdShowing = true
                    } else {
                        mRewardedAd = null
                        dismissAdDialog()
                        isAdShowing = false
                        if (AppOpenUtils.getInstance().isInitialized) {
                            AppOpenUtils.getInstance().isAppResumeEnabled = true
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

    @JvmStatic
    fun loadAndShowRewardedInter(
        activity: Activity,
        holder: AdmobHolder,
        callback: RewardedCallback
    ) {
        mRewardedInterstitialAd = null
        isAdShowing = false
        val remoteValue = RemoteConfigUtils.getValue("rewardinter_${holder.uid}")
        if (!isEnableAds || !isNetworkConnected(activity) || remoteValue == "0") {
            callback.onRewardClosed()
            return
        }
        if (adRequest == null) {
            initAdRequest(timeOut)
        }
        val adId = if (isTesting) {
            activity.getString(R.string.test_admob_reward_inter_id)
        } else {
            RemoteConfigUtils.getValue("rewardinter${holder.uid}_id")
        }
        if (holder.enableLoadingDialog) {
            dialogLoading(activity)
        }
        isAdShowing = false
        if (AppOpenUtils.getInstance().isInitialized) {
            AppOpenUtils.getInstance().isAppResumeEnabled = false
        }
        RewardedInterstitialAd.load(
            activity, adId, adRequest!!, object : RewardedInterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    // Handle the error.
                    mRewardedInterstitialAd = null
                    callback.onRewardFailed(loadAdError.message)
                    dismissAdDialog()
                    if (AppOpenUtils.getInstance().isInitialized) {
                        AppOpenUtils.getInstance().isAppResumeEnabled = true
                    }
                    isAdShowing = false
                    Log.e("===Admob", "onAdFailedToLoad" + loadAdError.message)
                    Log.e("===Admob", "errorCodeAds" + loadAdError.cause)
                }

                override fun onAdLoaded(rewardedAd: RewardedInterstitialAd) {
                    mRewardedInterstitialAd = rewardedAd
//                    if (mRewardedInterstitialAd != null) {
                    mRewardedInterstitialAd?.setOnPaidEventListener {
                        AdjustUtils.postRevenueAdjust(
                            it, mRewardedInterstitialAd?.adUnitId
                        )
                    }
                    mRewardedInterstitialAd?.fullScreenContentCallback =
                        object : FullScreenContentCallback() {
                            override fun onAdShowedFullScreenContent() {
                                isAdShowing = true
                                callback.onRewardShowed()
                                if (AppOpenUtils.getInstance().isInitialized) {
                                    AppOpenUtils.getInstance().isAppResumeEnabled = false
                                }
                            }

                            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                // Called when ad fails to show.
                                if (adError.code != 1) {
                                    isAdShowing = false
                                    callback.onRewardFailed(adError.message)
                                    mRewardedInterstitialAd = null
                                    dismissAdDialog()
                                }
                                if (AppOpenUtils.getInstance().isInitialized) {
                                    AppOpenUtils.getInstance().isAppResumeEnabled = true
                                }
                                Log.e("Admobfail", "onAdFailedToLoad" + adError.message)
                                Log.e("Admobfail", "errorCodeAds" + adError.cause)
                            }

                            override fun onAdDismissedFullScreenContent() {
                                // Called when ad is dismissed.
                                // Set the ad reference to null so you don't show the ad a second time.
                                mRewardedInterstitialAd = null
                                isAdShowing = false
                                callback.onRewardClosed()
                                if (AppOpenUtils.getInstance().isInitialized) {
                                    AppOpenUtils.getInstance().isAppResumeEnabled = true
                                }
                            }
                        }
                    if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        if (AppOpenUtils.getInstance().isInitialized) {
                            AppOpenUtils.getInstance().isAppResumeEnabled = false
                        }
                        mRewardedInterstitialAd?.show(activity) { callback.onRewardEarned() }
                        isAdShowing = true
                    } else {
                        mRewardedInterstitialAd = null
                        dismissAdDialog()
                        isAdShowing = false
                        if (AppOpenUtils.getInstance().isInitialized) {
                            AppOpenUtils.getInstance().isAppResumeEnabled = true
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

    @JvmStatic
    fun loadRewardedInter(
        context: Context,
        holder: AdmobHolder,
        callback: RewardInterCallback
    ) {
        val remoteValue = RemoteConfigUtils.getValue("rewardInter${holder.uid}")
        if (!isEnableAds || !isNetworkConnected(context) || remoteValue == "0") {
            return
        }
        if (holder.rewardInter.value != null) {
            Log.d("===Admob", "rewardInter not null")
            return
        }
        if (adRequest == null) {
            initAdRequest(timeOut)
        }
        holder.isLoading = true
        val adId = if (isTesting) {
            context.getString(R.string.test_admob_reward_inter_id)
        } else {
            RemoteConfigUtils.getValue("rewardInter${holder.uid}_id")
        }
        RewardedInterstitialAd.load(
            context, adId, adRequest!!, object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialRewardAd: RewardedInterstitialAd) {
//                    holder.rewardInter = interstitialRewardAd
                    holder.rewardInter.postValue(interstitialRewardAd)
//                    holder.isLoading = false
                    callback.onRewardLoaded()
                    Log.i("adLog", "onAdLoaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
//                    holder.rewardInter = null
//                    holder.isLoading = false
                    holder.rewardInter.postValue(null)
                    callback.onRewardFailed(loadAdError.message)
                }
            })
    }

    @JvmStatic
    fun showRewardedInter(activity: Activity, holder: AdmobHolder, callback: RewardedCallback) {
        if (adRequest == null) {
            initAdRequest(timeOut)
        }
        val remoteValue = RemoteConfigUtils.getValue("rewardInter_${holder.uid}")
        if (!isEnableAds || !isNetworkConnected(activity) || remoteValue == "0") {
            if (AppOpenUtils.getInstance().isInitialized) {
                AppOpenUtils.getInstance().isAppResumeEnabled = true
            }
            callback.onRewardFailed("Not show RewardInter")
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
                if (holder.isLoading) {
                    dialogLoading(activity)
                    delay(800)

                    holder.rewardInter.observe(activity as LifecycleOwner) { reward ->
                        reward?.let {
                            holder.rewardInter.removeObservers((activity as LifecycleOwner))
                            it.setOnPaidEventListener { value ->
                                AdjustUtils.postRevenueAdjust(value, it.adUnitId)
                            }
                            it.fullScreenContentCallback =
                                object : FullScreenContentCallback() {
                                    override fun onAdDismissedFullScreenContent() {
//                                        holder.rewardInter = null
                                        holder.rewardInter.removeObservers((activity as LifecycleOwner))
                                        holder.rewardInter.postValue(null)
                                        if (AppOpenUtils.getInstance().isInitialized) {
                                            AppOpenUtils.getInstance().isAppResumeEnabled = true
                                        }
                                        isAdShowing = false
                                        dismissAdDialog()
                                        callback.onRewardClosed()
                                        Log.d("TAG", "The ad was dismissed.")
                                    }

                                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
//                                        holder.rewardInter = null
                                        holder.rewardInter.removeObservers((activity as LifecycleOwner))
                                        holder.rewardInter.postValue(null)
                                        if (AppOpenUtils.getInstance().isInitialized) {
                                            AppOpenUtils.getInstance().isAppResumeEnabled = true
                                        }
                                        isAdShowing = false
                                        dismissAdDialog()
                                        callback.onRewardFailed(adError.message)
                                        Log.d("TAG", "The ad failed to show.")
                                    }

                                    override fun onAdShowedFullScreenContent() {
                                        isAdShowing = true
                                        callback.onRewardShowed()
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            dismissAdDialog()
                                        }, 800)
                                        Log.d("TAG", "The ad was shown.")
                                    }
                                }
                            it.show(activity) { callback.onRewardEarned() }
                        }
                    }
                } else {
                    if (holder.rewardInter.value != null) {
                        dialogLoading(activity)
                        delay(800)

                        holder.rewardInter.value?.setOnPaidEventListener {
                            AdjustUtils.postRevenueAdjust(it, holder.rewardInter.value!!.adUnitId)
                        }

                        holder.rewardInter.value?.fullScreenContentCallback =
                            object : FullScreenContentCallback() {
                                override fun onAdDismissedFullScreenContent() {
//                                    mInterstitialRewardAd.inter = null
                                    holder.rewardInter.removeObservers((activity as LifecycleOwner))
                                    holder.rewardInter.postValue(null)
                                    if (AppOpenUtils.getInstance().isInitialized) {
                                        AppOpenUtils.getInstance().isAppResumeEnabled = true
                                    }
                                    isAdShowing = false
                                    dismissAdDialog()
                                    callback.onRewardClosed()
                                    Log.d("TAG", "The ad was dismissed.")
                                }

                                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
//                                    mInterstitialRewardAd.inter = null
                                    holder.rewardInter.removeObservers((activity as LifecycleOwner))
                                    holder.rewardInter.postValue(null)
                                    if (AppOpenUtils.getInstance().isInitialized) {
                                        AppOpenUtils.getInstance().isAppResumeEnabled = true
                                    }
                                    isAdShowing = false
                                    dismissAdDialog()
                                    callback.onRewardFailed(adError.message)
                                    Log.d("TAG", "The ad failed to show.")
                                }

                                override fun onAdShowedFullScreenContent() {
                                    isAdShowing = true
                                    callback.onRewardShowed()
                                    Log.d("TAG", "The ad was shown.")
                                }
                            }
                        holder.rewardInter.value?.show(activity) { callback.onRewardEarned() }

                    } else {
                        isAdShowing = false
                        callback.onRewardFailed("None Show")
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
    fun loadRewarded(context: Context, holder: AdmobHolder, adLoadCallback: RewardInterCallback) {
        val remoteValue = RemoteConfigUtils.getValue("reward${holder.uid}")
        if (!isEnableAds || !isNetworkConnected(context) || remoteValue == "0") {
            return
        }
        if (holder.rewardInter.value != null) {
            Log.d("===AdsInter", "mInterstitialRewardAd not null")
            return
        }
        if (adRequest == null) {
            initAdRequest(timeOut)
        }
        holder.isLoading = true
        val adId = if (isTesting) {
            context.getString(R.string.test_admob_reward_id)
        } else {
            RemoteConfigUtils.getValue("reward${holder.uid}_id")
        }
        RewardedAd.load(
            context, adId, adRequest!!, object : RewardedAdLoadCallback() {
                override fun onAdLoaded(interstitialRewardAd: RewardedAd) {
//                    mInterstitialRewardAd.inter = interstitialRewardAd
                    holder.reward.postValue(interstitialRewardAd)
                    holder.isLoading = false
                    adLoadCallback.onRewardLoaded()
                    Log.i("adLog", "onAdLoaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
//                    mInterstitialRewardAd.inter = null
                    holder.isLoading = false
                    holder.reward.postValue(null)
                    adLoadCallback.onRewardFailed(loadAdError.message)
                }
            })
    }

    @JvmStatic
    fun showRewarded(
        activity: Activity, holder: AdmobHolder, adCallback: RewardedCallback
    ) {
        if (adRequest == null) {
            initAdRequest(timeOut)
        }
        val remoteValue = RemoteConfigUtils.getValue("reward${holder.uid}")
        if (!isEnableAds || !isNetworkConnected(activity) || remoteValue == "0") {
            if (AppOpenUtils.getInstance().isInitialized) {
                AppOpenUtils.getInstance().isAppResumeEnabled = true
            }
            adCallback.onRewardFailed("Not show reward")
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
                if (holder.isLoading) {
                    dialogLoading(activity)
                    delay(800)

                    holder.reward.observe(activity as LifecycleOwner) { reward: RewardedAd? ->
                        reward?.let {
                            holder.reward.removeObservers((activity as LifecycleOwner))
                            it.setOnPaidEventListener { value ->
                                AdjustUtils.postRevenueAdjust(value, it.adUnitId)
                            }
                            holder.reward.value?.fullScreenContentCallback =
                                object : FullScreenContentCallback() {
                                    override fun onAdDismissedFullScreenContent() {
//                                        mInterstitialRewardAd.inter = null
                                        holder.reward.removeObservers((activity as LifecycleOwner))
                                        holder.reward.postValue(null)
                                        if (AppOpenUtils.getInstance().isInitialized) {
                                            AppOpenUtils.getInstance().isAppResumeEnabled = true
                                        }
                                        isAdShowing = false
                                        dismissAdDialog()
                                        adCallback.onRewardClosed()
                                        Log.d("===Admob", "The ad was dismissed.")
                                    }

                                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
//                                        mInterstitialRewardAd.inter = null
                                        holder.reward.removeObservers((activity as LifecycleOwner))
                                        holder.reward.postValue(null)
                                        if (AppOpenUtils.getInstance().isInitialized) {
                                            AppOpenUtils.getInstance().isAppResumeEnabled = true
                                        }
                                        isAdShowing = false
                                        dismissAdDialog()
                                        adCallback.onRewardFailed(adError.message)
                                        Log.d("===Admob", "The ad failed to show.")
                                    }

                                    override fun onAdShowedFullScreenContent() {
                                        isAdShowing = true
                                        adCallback.onRewardShowed()
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            dismissAdDialog()
                                        }, 800)
                                        Log.d("===Admob", "The ad was shown.")
                                    }
                                }
                            it.show(activity) { adCallback.onRewardEarned() }
                        }
                    }
                } else {
                    if (holder.reward.value != null) {
                        dialogLoading(activity)
                        delay(800)

                        holder.reward.value?.setOnPaidEventListener {
                            AdjustUtils.postRevenueAdjust(it, holder.reward.value?.adUnitId)
                        }
                        holder.reward.value?.fullScreenContentCallback =
                            object : FullScreenContentCallback() {
                                override fun onAdDismissedFullScreenContent() {
//                                        mInterstitialRewardAd.inter = null
                                    holder.reward.removeObservers((activity as LifecycleOwner))
                                    holder.reward.postValue(null)
                                    if (AppOpenUtils.getInstance().isInitialized) {
                                        AppOpenUtils.getInstance().isAppResumeEnabled = true
                                    }
                                    isAdShowing = false
                                    dismissAdDialog()
                                    adCallback.onRewardClosed()
                                    Log.d("TAG", "The ad was dismissed.")
                                }

                                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
//                                        mInterstitialRewardAd.inter = null
                                    holder.reward.removeObservers((activity as LifecycleOwner))
                                    holder.reward.postValue(null)
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
                        holder.reward.value?.show(activity) { adCallback.onRewardEarned() }

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

    private fun dialogLoading(activity: Activity) {
        dialogFullScreen = Dialog(activity)
        dialogFullScreen?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogFullScreen?.setContentView(R.layout.dialog_full_screen)
        dialogFullScreen?.setCancelable(false)
        dialogFullScreen?.window!!.setBackgroundDrawable(ColorDrawable(Color.WHITE))
        dialogFullScreen?.window!!.setLayout(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT
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
        context: Context, nativeHolder: NativeAdmob, adCallback: NativeCallback
    ) {
        if (!isEnableAds || !isNetworkConnected(context)) {
            adCallback.onNativeFailed("No internet")
            return
        }
        //If native is loaded return
        if (nativeHolder.nativeAd != null) {
            Log.d("===Admob", "Native not null")
            return
        }
        if (isTesting) {
            nativeHolder.ads = context.getString(R.string.test_admob_native_id)
        }
        nativeHolder.isLoad = true
        val videoOptions = VideoOptions.Builder().setStartMuted(false).build()
        val adLoader: AdLoader =
            AdLoader.Builder(context, nativeHolder.ads).forNativeAd { nativeAd ->
                nativeHolder.nativeAd = nativeAd
                nativeHolder.isLoad = false
                nativeHolder.native_mutable.value = nativeAd
                nativeAd.setOnPaidEventListener { adValue: AdValue? ->
                    adValue?.let {
                        adCallback.onPaid(adValue, nativeHolder.ads)
                        AdjustUtils.postRevenueAdjustNative(
                            nativeAd, it, nativeHolder.ads
                        )
                    }
                }
                adCallback.onNativeReady(nativeAd)
                checkTestAd(nativeAd)
            }.withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e("===Admob", "onAdFailedToLoad" + adError.message)
                    Log.e("===Admob", "errorCodeAds" + adError.cause)
                    nativeHolder.nativeAd = null
                    nativeHolder.isLoad = false
                    nativeHolder.native_mutable.value = null
                    adCallback.onNativeFailed(adError.message)
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    adCallback.onNativeClicked()
                }
            }).withNativeAdOptions(NativeAdOptions.Builder().build()).build()
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
        nativeHolder: NativeAdmob,
        viewGroup: ViewGroup,
        layout: Int,
        size: AdNativeSize,
        callback: NativeCallbackSimple
    ) {
        if (!isEnableAds || !isNetworkConnected(
                activity
            )
        ) {
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
                activity.layoutInflater.inflate(R.layout.layout_banner_loading, null, false)
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
        entry: AdmobHolder,
        viewGroup: ViewGroup,
        layout: Int,
        size: AdNativeSize,
        adCallback: NativeCallback
    ) {
        Log.d("===Admob", "loadAndShowNative")
        val remoteValue = RemoteConfigUtils.getValue("native_${entry.uid}")
        if (!isEnableAds || !isNetworkConnected(activity) || remoteValue == "0") {
            viewGroup.visibility = View.GONE
            return
        }
//        VideoOptions.Builder().setStartMuted(false).build() //TODO: recheck
        try {
            viewGroup.removeAllViews()
        } catch (_: Exception) {

        }
        var adId = RemoteConfigUtils.getValue("native_${entry.uid}_id")
        val tagView: View = if (size === AdNativeSize.MEDIUM) {
            activity.layoutInflater.inflate(R.layout.layoutnative_loading_medium, null, false)
        } else {
            activity.layoutInflater.inflate(R.layout.layoutnative_loading_small, null, false)
//        } else {
//            activity.layoutInflater.inflate(R.layout.layout_banner_loading, null, false)
        }

        try {
            viewGroup.addView(tagView, 0)
        } catch (_: Exception) {

        }

        val shimmerFrameLayout =
            tagView.findViewById<ShimmerFrameLayout>(R.id.shimmer_view_container)
        shimmerFrameLayout.startShimmer()

        if (isTesting) {
            adId = activity.getString(R.string.test_admob_native_id)
        }
        val adLoader = AdLoader.Builder(activity, adId).forNativeAd { nativeAd ->
            adCallback.onNativeLoaded()
            val adView = activity.layoutInflater.inflate(layout, null) as NativeAdView
            populateNativeAdView(nativeAd, adView, size)
            shimmerFrameLayout.stopShimmer()
            try {
                viewGroup.removeAllViews()
                viewGroup.addView(adView)
            } catch (_: Exception) {

            }

            nativeAd.setOnPaidEventListener { adValue: AdValue ->
                adCallback.onPaid(adValue, adId)
                AdjustUtils.postRevenueAdjustNative(nativeAd, adValue, adId)
            }
            //viewGroup.setVisibility(View.VISIBLE);
        }.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e("Admobfail", "onAdFailedToLoad" + adError.message)
                Log.e("Admobfail", "errorCodeAds" + adError.cause)
                shimmerFrameLayout.stopShimmer()
                try {
                    viewGroup.removeAllViews()
                } catch (_: Exception) {

                }
                adCallback.onNativeFailed(adError.message)
            }

            override fun onAdClicked() {
                super.onAdClicked()
                adCallback.onNativeClicked()
            }
        }).withNativeAdOptions(NativeAdOptions.Builder().build()).build()
        if (adRequest != null) {
            adLoader.loadAd(adRequest!!)
        }
        Log.e("Admob", "loadAdNativeAds")
    }

    /**
     * Example: AdmobUtils.loadAndShowNativeCollap(this,nativeHolder,binding.native,
     * R.layout.ad_template_medium,AdNativeSize.MEDIUM,object :AdmobUtils.NativeAdCallbackNew {})
     */
    @JvmStatic
    fun loadAndShowNativeCollap(
        activity: Activity,
        entry: AdmobHolder,
        viewGroup: ViewGroup,
        layout: Int,
        size: AdNativeSize,
        adCallback: NativeCallback
    ) {
        Log.d("===Native", "Native1")
        val remoteEntry = RemoteConfigUtils.getValue("native_${entry.uid}")
        if (!isEnableAds || !isNetworkConnected(activity) || remoteEntry == "0") {
            viewGroup.visibility = View.GONE
            return
        }
//        val videoOptions =
//            VideoOptions.Builder().setStartMuted(false).build()
        try {
            viewGroup.removeAllViews()
        } catch (_: Exception) {

        }
        var adId = RemoteConfigUtils.getValue("native_collap_${entry.uid}_id")
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
            adId = activity.getString(R.string.test_admob_native_id)
        }
        val adLoader = AdLoader.Builder(activity, adId).forNativeAd { nativeAd ->
            adCallback.onNativeLoaded()
            val adView = activity.layoutInflater.inflate(layout, null) as NativeAdView
            populateNativeAdViewClose(nativeAd, adView, size, adCallback)
            shimmerFrameLayout.stopShimmer()
            try {
                viewGroup.removeAllViews()
                viewGroup.addView(adView)
            } catch (_: Exception) {

            }

            nativeAd.setOnPaidEventListener { adValue: AdValue ->
                adCallback.onPaid(adValue, adId)
                AdjustUtils.postRevenueAdjustNative(nativeAd, adValue, adId)
            }
            //viewGroup.setVisibility(View.VISIBLE);
        }.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e("Admobfail", "onAdFailedToLoad" + adError.message)
                Log.e("Admobfail", "errorCodeAds" + adError.cause)
                shimmerFrameLayout.stopShimmer()
                try {
                    viewGroup.removeAllViews()
                } catch (_: Exception) {

                }
                adCallback.onNativeFailed(adError.message)
            }

        }).withNativeAdOptions(NativeAdOptions.Builder().build()).build()
        if (adRequest != null) {
            adLoader.loadAd(adRequest!!)
        }
    }

    @JvmStatic
    fun loadAndShowNativeNoShimmer(
        activity: Activity,
        nativeHolder: NativeAdmob,
        viewGroup: ViewGroup,
        layout: Int,
        size: AdNativeSize,
        adCallback: NativeCallback
    ) {
        Log.d("===Native", "Native1")
        if (!isEnableAds || !isNetworkConnected(
                activity
            )
        ) {
            viewGroup.visibility = View.GONE
            return
        }
        var s = nativeHolder.ads
        if (isTesting) {
            s = activity.getString(R.string.test_admob_native_id)
        }
        val adLoader = AdLoader.Builder(activity, s).forNativeAd { nativeAd ->
            adCallback.onNativeLoaded()
            val adView = activity.layoutInflater.inflate(layout, null) as NativeAdView
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
                Log.e("Admobfail", "onAdFailedToLoad" + adError.message)
                Log.e("Admobfail", "errorCodeAds" + adError.cause)
                try {
                    viewGroup.removeAllViews()
                } catch (_: Exception) {

                }
                adCallback.onNativeFailed(adError.message)
            }

            override fun onAdClicked() {
                super.onAdClicked()
                adCallback.onNativeClicked()
            }
        }).withNativeAdOptions(NativeAdOptions.Builder().build()).build()
        if (adRequest != null) {
            adLoader.loadAd(adRequest!!)
        }
        Log.e("Admob", "loadAdNativeAds")
    }

    fun loadAndShowNativeFullScreen(
        activity: Activity,
        id: String,
        viewGroup: ViewGroup,
        layout: Int,
        mediaAspectRatio: Int,
        listener: NativeFullScreenCallback
    ) {
        if (!isEnableAds || !isNetworkConnected(
                activity
            )
        ) {
            viewGroup.visibility = View.GONE
            return
        }
        var adMobId: String = id
        if (isTesting) {
            adMobId = activity.getString(R.string.test_admob_native_full_screen_id)
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
        val adOptions = NativeAdOptions.Builder().setMediaAspectRatio(mediaAspectRatio)
            .setVideoOptions(videoOptions).build()
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
        nativeHolder: NativeAdmob,
        mediaAspectRatio: Int,
        adCallback: NativeCallback
    ) {
        if (!isEnableAds || !isNetworkConnected(
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
        if (isTesting) {
            nativeHolder.ads = context.getString(R.string.test_admob_native_full_screen_id)
        }
        nativeHolder.isLoad = true
        val videoOptions =
            VideoOptions.Builder().setStartMuted(false).setCustomControlsRequested(true).build()
        val adOptions = NativeAdOptions.Builder().setMediaAspectRatio(mediaAspectRatio)
            .setVideoOptions(videoOptions).build()
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
            checkTestAd(nativeAd)
        }
        adLoader.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e("Admobfail", "onAdFailedToLoad" + adError.message)
                Log.e("Admobfail", "errorCodeAds" + adError.cause)
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
        nativeHolder: NativeAdmob,
        viewGroup: ViewGroup,
        layout: Int,
        callback: NativeCallbackSimple
    ) {
        if (!isEnableAds || !isNetworkConnected(
                activity
            )
        ) {
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
                    nativeHolder.nativeAd!!, adView.findViewById(R.id.native_ad_view)
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
                R.layout.layoutnative_loading_fullscreen, null, false
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
                        nativeHolder.nativeAd!!, adView.findViewById(R.id.native_ad_view)
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
        if (!isEnableAds || !isNetworkConnected(
                activity
            )
        ) {
            viewGroup.visibility = View.GONE
            return
        }
        var adMobId: String = id
        if (isTesting) {
            adMobId = activity.getString(R.string.test_admob_native_full_screen_id)
        }
        val adView = activity.layoutInflater.inflate(layout, null) as NativeAdView
        val builder = AdLoader.Builder(activity, adMobId)
        val videoOptions =
            VideoOptions.Builder().setStartMuted(false).setCustomControlsRequested(false).build()
        val adOptions = NativeAdOptions.Builder().setMediaAspectRatio(mediaAspectRatio)
            .setVideoOptions(videoOptions).build()
        builder.withNativeAdOptions(adOptions)
        builder.forNativeAd { nativeAd ->
            listener.onLoaded(nativeAd)
            nativeAd.setOnPaidEventListener { adValue: AdValue? ->
                adValue?.let {
                    AdjustUtils.postRevenueAdjustNative(
                        nativeAd, adValue, id
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
        nativeHolder: NativeAdmob,
        viewGroup: ViewGroup,
        layout: Int,
        size: AdNativeSize,
        adCallback: NativeCallback
    ) {
        Log.d("===Native", "Native1")
        if (!isEnableAds || !isNetworkConnected(
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

        if (isTesting) {
            s = activity.getString(R.string.test_admob_native_id)
        }
        val adLoader = AdLoader.Builder(activity, s).forNativeAd { nativeAd ->
            adCallback.onNativeLoaded()
            val adView = activity.layoutInflater.inflate(layout, null) as NativeAdView
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
                Log.e("Admobfail", "onAdFailedToLoad" + adError.message)
                Log.e("Admobfail", "errorCodeAds" + adError.cause)
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
        }).withNativeAdOptions(NativeAdOptions.Builder().build()).build()
        if (adRequest != null) {
            adLoader.loadAd(adRequest!!)
        }
        Log.e("Admob", "loadAdNativeAds")
    }

    @JvmStatic
    fun showNativeNoButton(
        activity: Activity,
        nativeHolder: NativeAdmob,
        viewGroup: ViewGroup,
        layout: Int,
        size: AdNativeSize,
        callback: NativeCallbackSimple
    ) {
        if (!isEnableAds || !isNetworkConnected(
                activity
            )
        ) {
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

//    interface BannerCallback {
//        fun onBannerClicked()
//        fun onBannerLoaded()
//        fun onBannerFailed(error: String)
//    }

    interface BannerCallback {
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

    interface RewardInterCallback {
        fun onRewardFailed(error: String)
        fun onRewardLoaded()
//        fun onPaid(adValue: AdValue?, adUnitAds: String?)
    }

}
