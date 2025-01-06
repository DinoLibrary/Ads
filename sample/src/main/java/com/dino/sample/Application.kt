package com.dino.sample

import com.dino.ads.adjust.AdjustUtils
import com.dino.ads.application.AdsApplication

class Application : AdsApplication() {
    override fun onCreateApplication() {
        AdjustUtils.initAdjust(this,"",false)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level == TRIM_MEMORY_UI_HIDDEN){
            com.dino.ads.AppOpenUtils.getInstance().timeToBackground = System.currentTimeMillis()
        }
    }
}