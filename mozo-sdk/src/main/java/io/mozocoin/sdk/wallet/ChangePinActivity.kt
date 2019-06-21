package io.mozocoin.sdk.wallet

import android.os.Bundle
import android.widget.Toast
import io.mozocoin.sdk.MozoAuth
import io.mozocoin.sdk.MozoWallet
import io.mozocoin.sdk.R
import io.mozocoin.sdk.ui.BaseActivity
import kotlinx.android.synthetic.main.fragment_reset_enter_pin.*

internal class ChangePinActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_loading)

        if (!MozoAuth.getInstance().isSignedIn()) {
            MozoAuth.getInstance().isSignUpCompleted(this) { isCompleted ->
                if (isCompleted) loadWallet()
                else {
                    val prefix = getString(R.string.mozo_view_text_login_require_prefix)
                    val btn = getString(R.string.mozo_view_text_login_require_btn)
                    val suffix = getString(R.string.mozo_view_text_login_require_suffix)
                    Toast.makeText(this, "$prefix $btn $suffix", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            return
        }

        loadWallet()
    }

    private fun loadWallet() {
        MozoAuth.getInstance().syncProfile(this) {
            if (!it) return@syncProfile

            val wallet = MozoWallet.getInstance().getWallet()
            if (wallet == null) {
                // TODO OMG current wallet is null ?
                finish()
                return@syncProfile
            }

            setContentView(R.layout.activity_change_pin)
            if (wallet.isUnlocked()) showPinInputNewUI()
            else showPinInputUI()
        }
    }

    private fun showPinInputUI() {
        reset_pin_enter_pin_header?.setText(R.string.mozo_pin_change_sub_enter_current)
        // showPinInputNewUI()
    }

    private fun showPinInputNewUI() {
        reset_pin_enter_pin_header?.setText(R.string.mozo_pin_reset_header_create)
        // showPinInputConfirmUI()
    }

    private fun showPinInputConfirmUI() {
        reset_pin_enter_pin_header?.setText(R.string.mozo_pin_change_sub_confirm_new)
        reset_pin_enter_pin_sub_content?.setText(R.string.mozo_pin_change_confirm_content)
    }
}