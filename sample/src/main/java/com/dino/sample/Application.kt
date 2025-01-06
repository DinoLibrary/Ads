package com.dino.sample

import com.dino.sample.adjust.AdjustUtils
import com.dino.sample.application.AdsApplication

class Application : AdsApplication() {
    override fun onCreateApplication() {
        AdjustUtils.initAdjust(this,"",false)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level == TRIM_MEMORY_UI_HIDDEN){
            AppOpenUtils.getInstance().timeToBackground = System.currentTimeMillis()
        }
    }
}