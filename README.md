<!-- GETTING STARTED -->

// 1.Add to dependencyResolutionManagement:
  maven { url = uri("https://jitpack.io") }
  
// 2.Add to build.gradle(app):
  implementation("com.github.DinoLibrary:Ads:1.0.2")

// 3.Init Admob and/or Applovin:
  AdmobUtils.initAdmob(this, isDebug = true)
  ApplovinUtil.initApplovin(application, "your-key",testAds = true, enableAds = true, initialization = object : ApplovinUtil.Initialization {})

// NOTE:
    - Init On Resume:
        AppOpenUtils.getInstance().init(application, "your-id")
        AppOpenUtils.getInstance().disableAppResumeWithActivity(SplashActivity::class.java)

    - Show AOA on SplashActivity:
        AOAUtils(activity, "your-id", timeOut = 20000, object: AOAManager.AppOpenAdsListener{}).loadAoA()

  
