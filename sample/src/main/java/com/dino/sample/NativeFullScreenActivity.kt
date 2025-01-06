package com.dino.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dino.sample.utils.AdmobManager
import com.dino.sample.databinding.ActivityMain2Binding

class NativeFullScreenActivity : AppCompatActivity() {
    val binding by lazy { ActivityMain2Binding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        AdmobManager.showAdsNativeFullScreen(
            this,
            AdmobManager.nativeHolderFull,
            binding.bannerContainer
        )

    }
}