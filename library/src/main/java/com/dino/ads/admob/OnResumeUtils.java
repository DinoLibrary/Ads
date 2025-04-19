package com.dino.ads.admob;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Window;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.dino.ads.R;
import com.dino.ads.applovin.ApplovinUtils;
import com.google.android.gms.ads.AdActivity;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OnResumeUtils implements Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {
    private static final String TAG = "+===OnResumeUtils";
    private static volatile OnResumeUtils INSTANCE;
    private AppOpenAd appResumeAd = null;
    private AppOpenAd splashAd = null;
    private AppOpenAd.AppOpenAdLoadCallback loadCallback;
    private FullScreenContentCallback fullScreenContentCallback;
    private String appResumeAdId;
    private Activity currentActivity;
    private Application myApplication;
    private static boolean isShowingAd = false;
    public boolean isShowingAdsOnResume = false;
    public boolean isShowingAdsOnResumeBanner = false;
    private long appResumeLoadTime = 0;
    private long splashLoadTime = 0;
    public long timeToBackground = 0;
    private boolean isInitialized = false;
    public boolean isOnResumeEnable = true;
    private final List<Class> disabledAppOpenList;
    private Class splashActivity;
    private Dialog dialogFullScreen;

    public OnResumeUtils() {
        disabledAppOpenList = new ArrayList<>();
    }

    public static synchronized OnResumeUtils getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new OnResumeUtils();
        }
        return INSTANCE;
    }

    public void init(Activity activity) {
        String remoteValue = RemoteUtils.INSTANCE.getValue("on_resume", null);
        if (!remoteValue.equals("1")) return;
        isInitialized = true;
        this.myApplication = activity.getApplication();
        initAdRequest();
        if (activity.getClass().getSimpleName().equals("SplashActivity")) {
            disableOnResume(activity.getClass());
        }

        if (AdmobUtils.isTesting) {
            this.appResumeAdId = myApplication.getString(R.string.test_admob_on_resume_id);
        } else {
            this.appResumeAdId = RemoteUtils.INSTANCE.getAdId("on_resume");
        }
        this.myApplication.registerActivityLifecycleCallbacks(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        if (!isAdAvailable(false) && appResumeAdId != null) {
            fetchAd(false);
        }
    }

    AdRequest adRequest;

    // get AdRequest
    public void initAdRequest() {
        adRequest = new AdRequest.Builder()
                .setHttpTimeoutMillis(5000)
                .build();
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * Check app open ads is showing
     */
    public boolean isShowingAd() {
        return isShowingAd;
    }

    public boolean isShowingAdsOnResume() {
        return isShowingAdsOnResume;
    }

    /**
     * Disable app open app on specific activity
     */
    public void disableOnResume(Class activityClass) {
        Log.d(TAG, "disableOnResume: " + activityClass.getName());
        disabledAppOpenList.add(activityClass);
    }

    public void enableOnResume(Class activityClass) {
        Log.d(TAG, "enableOnResume: " + activityClass.getName());
        new Handler().postDelayed(() -> disabledAppOpenList.remove(activityClass), 40);
    }


    public void setAppResumeAdId(String appResumeAdId) {
        this.appResumeAdId = appResumeAdId;
    }

    public void setFullScreenContentCallback(FullScreenContentCallback callback) {
        this.fullScreenContentCallback = callback;
    }

    public void removeFullScreenContentCallback() {
        this.fullScreenContentCallback = null;
    }

    boolean isLoading = false;
    public boolean isDismiss = false;


    public void fetchAd(final boolean isSplash) {
        Log.d(TAG, "fetchAd: isSplash = " + isSplash);
        String remoteValue = RemoteUtils.INSTANCE.getValue("on_resume", null);
        if (remoteValue.equals("0") || !AdmobUtils.isEnableAds || !AdmobUtils.isNetworkConnected(myApplication)) {
            return;
        }
        if (isAdAvailable(isSplash) || appResumeAdId == null || OnResumeUtils.this.appResumeAd != null) {
            Log.d(TAG, "Ad is ready or id = null");
            return;
        }

        if (isLoading) return;
        Log.d(TAG, "fetchAd");
        isLoading = true;
        loadCallback = new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(AppOpenAd ad) {
                Log.d(TAG, "Loaded");
                OnResumeUtils.this.appResumeAd = ad;
                OnResumeUtils.this.appResumeLoadTime = (new Date()).getTime();
            }

            @Override
            public void onAdFailedToLoad(LoadAdError loadAdError) {
                isLoading = false;
                Log.d(TAG, "onAdFailedToLoad");
                String a = "fail";
            }

        };
        AppOpenAd.load(myApplication, appResumeAdId, adRequest, loadCallback);
    }

    private boolean wasLoadTimeLessThanNHoursAgo(long loadTime, long numHours) {
        long dateDifference = (new Date()).getTime() - loadTime;
        long numMilliSecondsPerHour = 3600000;
        return (dateDifference < (numMilliSecondsPerHour * numHours));
    }

    public boolean isAdAvailable(boolean isSplash) {
        long loadTime = isSplash ? splashLoadTime : appResumeLoadTime;
        boolean wasLoadTimeLessThanNHoursAgo = wasLoadTimeLessThanNHoursAgo(loadTime, 4);
        Log.d(TAG, "isAdAvailable " + wasLoadTimeLessThanNHoursAgo);
        return (isSplash ? splashAd != null : appResumeAd != null)
                && wasLoadTimeLessThanNHoursAgo;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(Activity activity) {
        Log.d(TAG, activity.getClass() + "|" + AdActivity.class);
        currentActivity = activity;
        Log.d(TAG, "Running");
    }

    @Override
    public void onActivityResumed(Activity activity) {
        currentActivity = activity;
        if (splashActivity == null) {
            if (!activity.getClass().getName().equals(AdActivity.class.getName())) {
                fetchAd(false);
            }
        } else {
            if (!activity.getClass().getName().equals(splashActivity.getName()) && !activity.getClass().getName().equals(AdActivity.class.getName())) {
                fetchAd(false);
            }
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        currentActivity = null;
        if (dialogFullScreen != null && dialogFullScreen.isShowing()) {
            dialogFullScreen.dismiss();
        }
    }

    public void showAppOpenAd(final boolean isSplash) {
        if (!ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
            Log.d("+===OnResume", "STARTED");
            if (fullScreenContentCallback != null) {
                try {
                    dialogFullScreen.dismiss();
                    dialogFullScreen = null;
                } catch (Exception ignored) {

                }
                fullScreenContentCallback.onAdDismissedFullScreenContent();
            }
            return;
        }
        Log.d("+===OnResume", "FullScreenContentCallback");
        if (!isShowingAd && isAdAvailable(isSplash)) {
            isDismiss = true;
            FullScreenContentCallback callback =
                    new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            Log.d("==TestAOA==", "onResume: true");
                            new Handler().postDelayed(() -> {
                                isDismiss = false;
                                Log.d("==TestAOA==", "onResume: false");
                            }, 200);
                            isLoading = false;
                            Log.d(TAG, "onAdShowedFullScreenContent: Dismiss");
                            try {
                                dialogFullScreen.dismiss();
                                dialogFullScreen = null;
                            } catch (Exception ignored) {

                            }
                            // Set the reference to null so isAdAvailable() returns false.
                            appResumeAd = null;
                            if (fullScreenContentCallback != null) {
                                fullScreenContentCallback.onAdDismissedFullScreenContent();
                            }
                            isShowingAd = false;
                            fetchAd(isSplash);
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(AdError adError) {
                            isLoading = false;
                            isDismiss = false;
                            Log.d(TAG, "onAdShowedFullScreenContent: Show false");
                            try {
                                dialogFullScreen.dismiss();
                                dialogFullScreen = null;
                            } catch (Exception ignored) {

                            }

                            if (fullScreenContentCallback != null) {
                                fullScreenContentCallback.onAdFailedToShowFullScreenContent(adError);
                            }
                            fetchAd(isSplash);
                        }

                        @Override
                        public void onAdShowedFullScreenContent() {
                            Log.d(TAG, "onAdShowedFullScreenContent: Show");
                            isShowingAd = true;
                            appResumeAd = null;
                        }
                    };
            showAdsResume(callback);

        } else {
            Log.d(TAG, "Ad is not ready");
            if (!isSplash) {
                fetchAd(false);
            }
        }
    }

    private void showAdsResume(final FullScreenContentCallback callback) {
        if (ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
            new Handler().postDelayed(() -> {
                if (appResumeAd != null) {
                    appResumeAd.setFullScreenContentCallback(callback);
                    if (currentActivity != null) {
                        showDialog(currentActivity);
                        appResumeAd.show(currentActivity);
                    }
                }
            }, 100);
        }
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        Log.d(TAG, "onStart");
        // Show the ad (if available) when the app moves to foreground.
        new Handler().postDelayed(() -> {
            if (System.currentTimeMillis() - timeToBackground < 30000) {
                return;
            }

            if (currentActivity == null) {
                return;
            }
            if (currentActivity.getClass() == AdActivity.class) {
                return;
            }
            if (ApplovinUtils.INSTANCE.isClickAds()) {
                ApplovinUtils.INSTANCE.setClickAds(false);
                return;
            }
            if (AdmobUtils.isAdShowing) {
                return;
            }
            if (!AdmobUtils.isEnableAds) {
                return;
            }

            if (AdmobUtils.isNativeInterShowing(currentActivity)) {
                Log.e("+===OnResume", "Native inter is showing => disable on_resume");
                return;
            }

            if (!isOnResumeEnable) {
                Log.d("+===OnResume", "enableOnResume: false");
                return;
            } else {
                Log.d("+===OnResume", "enableOnResume: true");
                AdmobUtils.dismissAdDialog();
            }

            for (Class activity : disabledAppOpenList) {
                if (activity.getName().equals(currentActivity.getClass().getName())) {
                    Log.d(TAG, "onStart: activity is disabled");
                    return;
                }
            }
            showAppOpenAd(false);
        }, 30);
    }

    public void showDialog(Context context) {
        isShowingAdsOnResume = true;
        isShowingAdsOnResumeBanner = true;
        dialogFullScreen = new Dialog(context);
        dialogFullScreen.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogFullScreen.setContentView(R.layout.dialog_onresume);
        dialogFullScreen.setCancelable(false);
        dialogFullScreen.getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        dialogFullScreen.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        try {
            if (!currentActivity.isFinishing() && dialogFullScreen != null && !dialogFullScreen.isShowing()) {
                dialogFullScreen.show();
            }
        } catch (Exception ignored) {

        }
    }

}

