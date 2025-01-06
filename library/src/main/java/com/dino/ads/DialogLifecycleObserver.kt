package com.dino.ads

import android.app.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

class DialogLifecycleObserver(val dialog: Dialog) : LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onDestroy(){
        if(dialog.isShowing){
            dialog.dismiss()
            if (com.dino.ads.AppOpenUtils.getInstance().isInitialized) {
                com.dino.ads.AppOpenUtils.getInstance().isAppResumeEnabled = true
            }
        }
    }
}