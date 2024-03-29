package io.mozocoin.sdk.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.IntDef
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.doOnLayout
import androidx.lifecycle.Observer
import io.mozocoin.sdk.MozoAuth
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.Constant
import io.mozocoin.sdk.common.ViewModels
import io.mozocoin.sdk.common.model.Profile
import io.mozocoin.sdk.ui.dialog.QRCodeDialog
import io.mozocoin.sdk.utils.*
import kotlinx.coroutines.*

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
    private var buttonShowQRCode: View? = null

    private var stateNotLoginView: View? = null
    private var stateErrorView: View? = null

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

        if (isInEditMode) {
            if (mShowQRCode) {
                imageAddressQRView?.visible()
                buttonShowQRCode?.visible()
            } else {
                imageAddressQRView?.gone()
                buttonShowQRCode?.gone()
            }
        }
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
        textCurrencyBalanceView?.alpha = if (Constant.SHOW_MOZO_EQUIVALENT_CURRENCY) 1f else 0f
        imageAddressQRView = find(R.id.mozo_wallet_qr_image)
        buttonShowQRCode = find(R.id.mozo_wallet_qr_image_button)

        find<TextView>(R.id.button_copy)?.apply {
            click { context.copyWithToast(mAddress) }
            doOnLayout { btn ->
                textAddressView?.let {
                    val layoutParams = it.layoutParams as LayoutParams
                    layoutParams.marginEnd = btn.width
                    layoutParams.goneEndMargin = btn.width
                    it.layoutParams = layoutParams
                }
            }
        }
        find<View>(R.id.button_login)?.click { MozoAuth.getInstance().signIn() }
        find<View>(R.id.button_refresh)?.click {
            hideErrorStateUI()
            MozoSDK.getInstance().profileViewModel.fetchBalance(context)
        }

        stateNotLoginView = find(R.id.mozo_wallet_state_login)
        if (stateNotLoginView is LinearLayout && (mViewMode == MODE_ONLY_ADDRESS || mViewMode == MODE_ONLY_BALANCE)) {
            (stateNotLoginView as LinearLayout).gravity = Gravity.CENTER_VERTICAL
        }

        stateErrorView = find(R.id.mozo_wallet_state_error)
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

    private val profileObserver = Observer<Profile?> {
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
            mBalance = balanceNonDecimal.displayString()
            mBalanceRate = balanceNonDecimalInCurrencyDisplay
            updateUI()
        }

        MozoSDK.getInstance().profileViewModel.run {
            if (profileLiveData.value?.walletInfo != null && balanceInfoLiveData.value == null)
                showErrorStateUI()
            else
                hideErrorStateUI()
        }
    }

    private fun showLoginRequireUI() {
        stateNotLoginView?.visible()
        hideErrorStateUI()
    }

    private fun hideLoginRequireUI() {
        stateNotLoginView?.gone()
    }

    private fun showErrorStateUI() {
        stateErrorView?.visible()
    }

    private fun hideErrorStateUI() {
        stateErrorView?.gone()
    }

    private fun updateUI() {
        textAddressView?.text = mAddress
        textBalanceView?.text = mBalance
        textCurrencyBalanceView?.text = mBalanceRate

        if (mShowQRCode && !mAddress.isNullOrEmpty()) {
            imageAddressQRView?.apply {
                generateQRJob = getQRImage()
                visible()
                click { QRCodeDialog.show(context, mAddress!!) }
            }
            buttonShowQRCode?.apply {
                visible()
                click { QRCodeDialog.show(context, mAddress!!) }
            }
        } else {
            imageAddressQRView?.gone()
            buttonShowQRCode?.gone()
        }
    }

    private fun getQRImage() = MozoSDK.scope.launch {
        val qrImage = Support.createQRCode(mAddress!!, sizeOfQRImage)
        withContext(Dispatchers.Main) {
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