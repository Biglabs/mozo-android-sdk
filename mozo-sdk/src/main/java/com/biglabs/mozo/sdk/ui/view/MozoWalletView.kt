package com.biglabs.mozo.sdk.ui.view

import android.arch.lifecycle.Observer
import android.content.Context
import android.support.annotation.IntDef
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.biglabs.mozo.sdk.MozoAuth
import com.biglabs.mozo.sdk.MozoSDK
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.common.Models
import com.biglabs.mozo.sdk.common.ViewModels
import com.biglabs.mozo.sdk.utils.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

class MozoWalletView : ConstraintLayout {

    private var mViewMode = MODE_ALL
    private var mShowQRCode = true

    private var mAddress: String? = null
    private var mBalance: String? = null
    private var mBalanceRate: String? = null

    private var textAddressView: TextView? = null
    private var textBalanceView: TextView? = null
    private var textCurrencyBalanceView: TextView? = null
    private var imageAddressQRView: ImageView? = null

    private var stateNotLoginView: View? = null

    private var generateQRJob: Job? = null
    private var sizeOfQRImage = 0

    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        val typeArray = context.obtainStyledAttributes(attrs, R.styleable.MozoWalletView, defStyleAttr, 0)
        try {
            mViewMode = typeArray.getInt(R.styleable.MozoWalletView_viewMode, mViewMode)
            mShowQRCode = typeArray.getBoolean(R.styleable.MozoWalletView_showQRCode, mShowQRCode)
        } finally {
            typeArray.recycle()
        }

        setBackgroundResource(R.drawable.mozo_bg_component)
        minWidth = resources.getDimensionPixelSize(R.dimen.mozo_view_min_width)

        inflateLayout()
    }

    private fun inflateLayout() {
        when (mViewMode) {
            MODE_ONLY_ADDRESS -> {
                inflate(context, R.layout.view_wallet_address_only, this)
                sizeOfQRImage = context.resources.getDimensionPixelSize(R.dimen.mozo_qr_small_size)
            }
            MODE_ONLY_BALANCE -> {
                inflate(context, R.layout.view_wallet_balance_only, this)
            }
            else -> {
                inflate(context, R.layout.view_wallet_all_info, this)
                sizeOfQRImage = context.resources.getDimensionPixelSize(R.dimen.mozo_qr_medium_size)
            }
        }

        textAddressView = find(R.id.mozo_wallet_address)
        textBalanceView = find(R.id.mozo_wallet_balance_value)
        textCurrencyBalanceView = find(R.id.mozo_wallet_currency_balance)
        imageAddressQRView = find(R.id.mozo_wallet_qr_image)

        find<TextView>(R.id.button_copy)?.click { context.copyWithToast(mAddress) }
        find<View>(R.id.button_login)?.click { MozoAuth.getInstance().signIn() }

        stateNotLoginView = find(R.id.mozo_wallet_state_login)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            showLoginRequireUI()

            MozoSDK.getInstance().profileViewModel.run {
                profileLiveData.observeForever(profileObserver)
                balanceAndRateLiveData.observeForever(balanceAndRateObserver)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        MozoSDK.getInstance().profileViewModel.run {
            profileLiveData.removeObserver(profileObserver)
            balanceAndRateLiveData.removeObserver(balanceAndRateObserver)
        }
        generateQRJob?.cancel()
        generateQRJob = null
    }

    private val profileObserver = Observer<Models.Profile?> {
        if (it?.walletInfo != null) {
            mAddress = it.walletInfo!!.offchainAddress
            hideLoginRequireUI()
            updateUI()
        } else {
            showLoginRequireUI()
        }
    }

    private val balanceAndRateObserver = Observer<ViewModels.BalanceAndRate?> {
        it?.run {
            mBalance = balanceInDecimal.displayString()
            mBalanceRate = balanceInCurrencyDisplay
            updateUI()
        }
    }

    private fun showLoginRequireUI() {
        stateNotLoginView?.visible()
    }

    private fun hideLoginRequireUI() {
        stateNotLoginView?.gone()
    }

    private fun updateUI() {
        textAddressView?.text = mAddress
        textBalanceView?.text = mBalance
        textCurrencyBalanceView?.text = mBalanceRate

        if (mShowQRCode && mAddress != null) {
            imageAddressQRView?.apply {
                visible()
                generateQRJob = getQRImage()
            }
        } else imageAddressQRView?.gone()
    }

    private fun getQRImage() = launch {
        val qrImage = Support.generateQRCode(mAddress!!, sizeOfQRImage)
        launch(UI) {
            imageAddressQRView?.setImageBitmap(qrImage)
        }
        generateQRJob = null
    }

    companion object {
        @Retention(AnnotationRetention.SOURCE)
        @IntDef(MODE_ALL, MODE_ONLY_ADDRESS, MODE_ONLY_BALANCE)
        annotation class ViewMode

        const val MODE_ALL = 0
        const val MODE_ONLY_ADDRESS = 1
        const val MODE_ONLY_BALANCE = 2
    }
}