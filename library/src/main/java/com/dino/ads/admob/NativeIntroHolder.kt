package com.dino.ads.admob

class NativeIntroHolder(uid: String) : AdmobHolder(uid) {
    val holders = mutableListOf<AdmobHolder>()

    override fun version(versionCode: Int): NativeIntroHolder {
        this.versionCode = versionCode
        return this
    }

}