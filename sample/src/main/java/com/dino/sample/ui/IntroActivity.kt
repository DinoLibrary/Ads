package com.dino.sample.ui

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.dino.ads.utils.replaceActivity
import com.dino.sample.RemoteConfig
import com.dino.sample.databinding.ActivityIntroBinding
import com.dino.sample.utils.AdsManager
import kotlin.system.exitProcess

class IntroActivity : AppCompatActivity() {
    private val binding by lazy { ActivityIntroBinding.inflate(layoutInflater) }
    private var fragments = mutableListOf<Fragment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        createListFragment()
        binding.viewPager.adapter = IntroAdapter(this)
    }

    private fun createListFragment() {
        fragments.add(IntroFragment.newInstance(1))
        if (RemoteConfig.NATIVE_INTRO_FULL.isNativeReady() && RemoteConfig.NATIVE_INTRO_FULL.remoteValue.contains("1")) {
            fragments.add(NativeFullScreenFragment())
        }
        fragments.add(IntroFragment.newInstance(2))
        if (RemoteConfig.NATIVE_INTRO_FULL.isNativeReady() && RemoteConfig.NATIVE_INTRO_FULL.remoteValue.contains("2")) {
            fragments.add(NativeFullScreenFragment())
        }
        fragments.add(IntroFragment.newInstance(3))
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        val pos = binding.viewPager.currentItem
        if (pos > 0) {
            binding.viewPager.setCurrentItem(pos - 1, true)
        } else {
            finish()
            exitProcess(0)
        }
    }

    fun onNext() {
        if (binding.viewPager.currentItem == fragments.size - 1) {
            AdsManager.loadAndShowInter(this, RemoteConfig.INTER_INTRO) {
                startActivity()
            }
        } else {
            val current = binding.viewPager.currentItem
            binding.viewPager.setCurrentItem(current + 1, true)
        }
    }

    private fun startActivity() {
        if (!isFinishing) {
            replaceActivity<MainActivity>()
        }
    }

    inner class IntroAdapter(fragmentActivity: FragmentActivity) :
        FragmentStateAdapter(fragmentActivity) {
        override fun createFragment(position: Int): Fragment {
            return fragments[position]
        }

        override fun getItemCount(): Int {
            return fragments.size
        }
    }
}
