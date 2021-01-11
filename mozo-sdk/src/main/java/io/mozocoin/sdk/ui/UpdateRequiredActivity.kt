package io.mozocoin.sdk.ui

import android.os.Bundle
import io.mozocoin.sdk.databinding.ActivityUpdateRequiredBinding
import io.mozocoin.sdk.utils.click
import io.mozocoin.sdk.utils.openAppInStore

internal class UpdateRequiredActivity : BaseActivity() {

    private lateinit var binding: ActivityUpdateRequiredBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdateRequiredBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonUpdate.click {
            openAppInStore()
        }
    }
}