package com.dino.sample.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dino.ads.admob.AdmobUtils
import com.dino.ads.utils.addActivity
import com.dino.sample.R
import com.dino.sample.RemoteConfig
import com.dino.sample.databinding.ActivityInterDummyBinding
import com.dino.sample.utils.AdsManager

class InterDummyActivity : AppCompatActivity() {
    private val binding by lazy { ActivityInterDummyBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.btnOk.setOnClickListener { finish() }
    }

    override fun onBackPressed() {
        AdmobUtils.loadAndShowInterstitial(this, RemoteConfig.INTER_HOME2, R.layout.ad_template_fullscreen) {
            finish()
        }
    }
}