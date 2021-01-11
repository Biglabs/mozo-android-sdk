package io.mozocoin.sdk.wallet.reset

import android.os.Bundle
import android.view.View
import android.widget.TextView
import io.mozocoin.sdk.MozoWallet
import io.mozocoin.sdk.R
import io.mozocoin.sdk.databinding.ActivityResetPinBinding
import io.mozocoin.sdk.ui.BaseActivity
import io.mozocoin.sdk.utils.replace

internal class ResetPinActivity : BaseActivity(), InteractionListener {

    private lateinit var binding: ActivityResetPinBinding
    private var fragments = arrayOf(
            EnterSeedFragment.newInstance(),
            EnterPinFragment.newInstance()
    )
    private val mModel: ResetPinViewModel by lazy { ResetPinViewModel.get(this) }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        MozoWallet.getInstance().isDuringResetPinProcess = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResetPinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.resetPinToolbar.apply {
            findViewById<View>(R.id.button_close)?.isEnabled = false
            onBackPress = ::onBackPressed
            onClosePress = {
                fragments.forEach {
                    it.onCloseClicked()
                }
            }
        }

        replace(R.id.reset_pin_content_frame, fragments[0])
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        MozoWallet.getInstance().isDuringResetPinProcess = false
    }

    override fun onBackPressed() {
        val current = supportFragmentManager.findFragmentById(R.id.reset_pin_content_frame)
        if (current !is EnterPinFragment) {
            finish()
            return
        }
        super.onBackPressed()
    }

    override fun getCloseButton(): TextView? = binding.resetPinToolbar.findViewById(R.id.button_close)

    override fun getResetPinModel(): ResetPinViewModel = mModel

    override fun hideToolbarActions(left: Boolean, right: Boolean) {
        binding.resetPinToolbar.showBackButton(!left)
        binding.resetPinToolbar.showCloseButton(!right)
    }

    override fun requestEnterPin() {
        replace(R.id.reset_pin_content_frame, fragments[1], "enter_pin")
    }
}