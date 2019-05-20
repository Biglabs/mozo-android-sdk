package io.mozocoin.sdk.wallet.reset

import android.os.Bundle
import android.widget.TextView
import io.mozocoin.sdk.MozoWallet
import io.mozocoin.sdk.R
import io.mozocoin.sdk.ui.BaseActivity
import io.mozocoin.sdk.utils.replace
import kotlinx.android.synthetic.main.activity_reset_pin.*
import kotlinx.android.synthetic.main.view_toolbar.view.*

internal class ResetPinActivity : BaseActivity(), InteractionListener {

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
        setContentView(R.layout.activity_reset_pin)

        reset_pin_toolbar?.apply {
            button_close?.isEnabled = false
            onBackPress = { onBackPressed() }
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
        }
    }

    override fun getCloseButton(): TextView? = reset_pin_toolbar?.button_close

    override fun getResetPinModel(): ResetPinViewModel = mModel

    override fun hideToolbarActions(left: Boolean, right: Boolean) {
        reset_pin_toolbar?.showBackButton(!left)
        reset_pin_toolbar?.showCloseButton(!right)
    }

    override fun requestEnterPin() {
        replace(R.id.reset_pin_content_frame, fragments[1], "enter_pin")
    }
}