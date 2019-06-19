package io.mozocoin.sdk.wallet

import android.os.Bundle
import io.mozocoin.sdk.MozoAuth
import io.mozocoin.sdk.MozoWallet
import io.mozocoin.sdk.R
import io.mozocoin.sdk.ui.BaseActivity
import io.mozocoin.sdk.ui.SecurityActivity

internal class ChangePinActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_loading)
        MozoAuth.getInstance().syncProfile(this) {
            if (!it) return@syncProfile

            val wallet = MozoWallet.getInstance().getWallet(false)
            if (wallet == null) {
                // TODO OMG current wallet is null ?
                finish()
                return@syncProfile
            }

            if (wallet.isUnlocked()) {
                //displaySeedWords()

            } else {
                SecurityActivity.start(this, SecurityActivity.KEY_VERIFY_PIN, 0)
            }
        }
    }

    companion object {

    }
}