package com.dino.ads.remote_config

class NativeFullHolder(uid: String) : AdmobHolder(uid) {
    val remoteValue: String
        get() = RemoteUtils.getValue("native_${uid}_full")
}