package com.dino.sample.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.dino.ads.admob.OnResumeUtils
import com.dino.sample.RemoteConfig
import com.dino.sample.databinding.FragmentNativeFullScreenBinding
import com.dino.sample.utils.AdsManager

class NativeFullScreenFragment : Fragment() {
    private val binding by lazy { FragmentNativeFullScreenBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!isAdded && activity == null) return
        binding.btnNext.setOnClickListener { (activity as? IntroActivity)?.onNext() }
        AdsManager.showNativeFullScreen(requireActivity(), RemoteConfig.NATIVE_INTRO_FULL, binding.flNative)
    }

    override fun onResume() {
        super.onResume()
        OnResumeUtils.getInstance().disableOnResume(activity?.javaClass)
    }

    override fun onPause() {
        super.onPause()
        OnResumeUtils.getInstance().enableOnResume(activity?.javaClass)
    }
}