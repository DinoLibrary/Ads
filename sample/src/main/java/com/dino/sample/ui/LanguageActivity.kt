package com.dino.sample.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dino.ads.utils.gone
import com.dino.ads.utils.replaceActivity
import com.dino.ads.utils.toast
import com.dino.ads.utils.visible
import com.dino.demo.opening.LanguageModel
import com.dino.sample.RemoteConfig
import com.dino.sample.databinding.ActivityLanguageBinding
import com.dino.sample.utils.AdsManager
import com.dino.sample.utils.Common
import kotlin.system.exitProcess

class LanguageActivity : AppCompatActivity() {
    private val binding by lazy { ActivityLanguageBinding.inflate(layoutInflater) }
    private lateinit var languages: ArrayList<LanguageModel>
    private var adapter: LanguageAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.toolbar.navigationIcon = null
        binding.tvNext.gone()
        AdsManager.loadNativeFullscreen(this, RemoteConfig.NATIVE_INTRO_FULL)
        AdsManager.loadNativeIntro(this, RemoteConfig.NATIVE_INTRO)
        AdsManager.showNativeLanguage(this, RemoteConfig.NATIVE_LANGUAGE, binding.flNative, 0)

        languages = Common.getListLocation(this)
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }

        adapter = LanguageAdapter {
            binding.tvNext.visible()
            if (Common.currentLang == null) {
                AdsManager.showNativeLanguage(this, RemoteConfig.NATIVE_LANGUAGE, binding.flNative, 1)
            }
            Common.currentLang = languages[it].langCode
            adapter?.updatePosition(it)
        }.apply {
            submitList(languages)
            binding.rvLanguage.adapter = this
        }

        binding.tvNext.setOnClickListener {
            if (Common.currentLang == null) {
                toast("Please select a language before continue!")
            } else {
                AdsManager.loadAndShowInter(this, RemoteConfig.INTER_LANGUAGE) {
                    nextActivity()
                }
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        exitProcess(0)
    }

    private fun nextActivity() {
        replaceActivity<IntroActivity>()
    }

}