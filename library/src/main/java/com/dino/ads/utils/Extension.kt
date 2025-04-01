package com.dino.ads.utils

import android.app.Activity
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.dino.ads.AdmobUtils


fun Context.toast(msg: String, length: Int = Toast.LENGTH_SHORT) {
    Handler(Looper.getMainLooper()).post {
        Toast.makeText(this, msg, length).show()
    }
}

fun log(msg: String) {
    if (AdmobUtils.isTesting) Log.d("===", msg)
}

fun Context.prefs(): SharedPreferences {
    return getSharedPreferences("APP_PREFS", MODE_PRIVATE)
}

inline fun <reified T : Activity> Context.addActivity() {
    startActivity(Intent(this, T::class.java))
}

inline fun <reified T : Activity> Context.addActivity(block: Intent.() -> Unit = {}) {
    startActivity(Intent(this, T::class.java).apply(block))
}

inline fun <reified T : Activity> Context.replaceActivity() {
    val i = Intent(this, T::class.java)
    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(i)
}
