package com.dino.ads.admob

class NativeFullHolder(uid: String) : AdmobHolder(uid) {
    val remoteValue: String
        get() = RemoteUtils.getValue("native_${uid}_full", versionCode)

    override fun version(versionCode: Int): NativeFullHolder {
        this.versionCode = versionCode
        return this
    }

}