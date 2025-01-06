package com.dino.sample

import android.app.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

class DialogLifecycleObserver(val dialog: Dialog) : LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onDestroy(){
        if(dialog.isShowing){
            dialog.dismiss()
            if (AppOpenUtils.getInstance().isInitialized) {
                AppOpenUtils.getInstance().isAppResumeEnabled = true
            }
        }
    }
}