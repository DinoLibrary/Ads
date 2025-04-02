package com.dino.ads.applovin

import android.app.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.dino.ads.admob.OnResumeUtils

class DialogLifecycleObserver(val dialog: Dialog) : LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onDestroy(){
        if(dialog.isShowing){
            dialog.dismiss()
            if (OnResumeUtils.getInstance().isInitialized) {
                OnResumeUtils.getInstance().isAppResumeEnabled = true
            }
        }
    }
}