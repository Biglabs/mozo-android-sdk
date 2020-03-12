package io.mozocoin.sdk.ui

import android.os.Bundle
import io.mozocoin.sdk.R
import io.mozocoin.sdk.utils.click
import io.mozocoin.sdk.utils.openAppInStore
import kotlinx.android.synthetic.main.activity_update_required.*

internal class UpdateRequiredActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_required)

        button_update?.click {
            openAppInStore()
        }
    }

    override fun onBackPressed() {
        /**
         * Update is required, so prevent back press
         * super.onBackPressed()
         */
    }
}