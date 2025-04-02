package com.dino.sample.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dino.sample.databinding.ActivityInterDummyBinding

class InterDummyActivity : AppCompatActivity() {
    private val binding by lazy { ActivityInterDummyBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.btnOk.setOnClickListener { finish() }
    }

}