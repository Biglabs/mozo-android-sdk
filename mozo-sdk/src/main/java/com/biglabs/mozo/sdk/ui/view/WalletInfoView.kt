package com.biglabs.mozo.sdk.ui.view

import android.content.Context
import android.support.annotation.IntDef
import android.support.constraint.ConstraintLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentManager
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.common.MessageEvent
import com.biglabs.mozo.sdk.auth.MozoAuth
import com.biglabs.mozo.sdk.services.WalletService
import com.biglabs.mozo.sdk.trans.MozoTrans
import com.biglabs.mozo.sdk.ui.dialog.QRCodeDialog
import com.biglabs.mozo.sdk.utils.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import android.support.constraint.ConstraintSet


class WalletInfoView : ConstraintLayout {

    private var mViewMode: Int = 0
    private var mShowCopyButton = true
    private var mShowQRCodeButton = true
    private var mShowQRCodeThumbnail = true

    private var mAddress: String? = null
    private var mBalance: String? = null

    private var mWalletAddressView: TextView? = null
    private var mWalletBalanceView: TextView? = null
    private var fragmentManager: FragmentManager? = null

    private val mTagShowAlways: String
    private val mTagLoginRequire: String

    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        context.theme.obtainStyledAttributes(attrs, R.styleable.WalletInfoView, defStyleAttr, 0).apply {
            try {
                mViewMode = getInt(R.styleable.WalletInfoView_viewMode, mViewMode)
                mShowCopyButton = getBoolean(R.styleable.WalletInfoView_showCopyButton, mShowCopyButton)
                mShowQRCodeButton = getBoolean(R.styleable.WalletInfoView_showQRCodeButton, mShowQRCodeButton)
                mShowQRCodeThumbnail = getBoolean(R.styleable.WalletInfoView_showQRCodeThumbnail, mShowQRCodeThumbnail)
            } finally {
                recycle()
            }
        }

        setBackgroundResource(R.drawable.mozo_bg_component)
        minWidth = resources.getDimensionPixelSize(R.dimen.mozo_view_min_width)
        minHeight = resources.getDimensionPixelSize(R.dimen.mozo_view_min_height)
        mTagShowAlways = resources.getString(R.string.tag_show_always)
        mTagLoginRequire = resources.getString(R.string.tag_login_require)

        inflateLayout()

        if (!isInEditMode) {
            fetchData()

            if (context is FragmentActivity) {
                fragmentManager = context.supportFragmentManager
            } else if (context is Fragment) {
                fragmentManager = context.fragmentManager
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            EventBus.getDefault().register(this)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (!isInEditMode) {
            EventBus.getDefault().unregister(this)
        }
    }

    @Suppress("unused")
    @Subscribe
    internal fun onAuthorizeChanged(auth: MessageEvent.Auth) {
        fetchData()
    }

    private fun fetchData() {
        if (MozoAuth.getInstance().isSignUpCompleted()) {
            launch {
                mAddress = WalletService.getInstance().getAddress().await()
                launch(UI) {
                    mWalletAddressView?.text = mAddress
                }
            }

            launch {
                mBalance = MozoTrans.getInstance().getBalance().await()
                launch(UI) {
                    mWalletBalanceView?.text = mBalance ?: "0"
                }
            }

            showDetailUI()
        } else
            showLoginRequireUI()
    }

    private fun inflateLayout() {
        removeAllViews()
        when (mViewMode) {
            MODE_ONLY_ADDRESS -> inflate(context, R.layout.view_wallet_info_address, this)
            MODE_ONLY_BALANCE -> inflate(context, R.layout.view_wallet_info_balance, this)
            else -> inflate(context, R.layout.view_wallet_info, this)
        }

        updateUI()
    }

    private fun updateUI() {
        if (isInEditMode) return
        val balanceRate: TextView?

        if (mViewMode == MODE_ONLY_BALANCE) {
            balanceRate = find(R.id.mozo_wallet_balance_rate_side)

        } else {
            mWalletAddressView = find(R.id.mozo_wallet_address)
            mAddress?.let { mWalletAddressView?.text = it }

            balanceRate = find(R.id.mozo_wallet_balance_rate_bottom)
            find<View>(R.id.mozo_wallet_btn_show_qr)?.apply {
                if (mShowQRCodeButton) {
                    visible()
                    click { showQRCodeDialog() }
                } else gone()
            }

            find<View>(R.id.mozo_wallet_btn_copy)?.apply {
                if (mShowCopyButton) {
                    visible()
                    click { context.copyWithToast(mAddress) }
                } else gone()
            }
        }

        mWalletBalanceView = find(R.id.mozo_wallet_balance_value)
        mBalance?.let { mWalletBalanceView?.text = it }

        find<View>(R.id.button_login)?.apply {
            click {
                MozoAuth.getInstance().signIn()
            }

//            val loginBtnConstraintSet = ConstraintSet()
//            loginBtnConstraintSet.connect(ConstraintSet.PARENT_ID, ConstraintSet.START, id, ConstraintSet.START)
//            loginBtnConstraintSet.connect(ConstraintSet.PARENT_ID, ConstraintSet.END, id, ConstraintSet.END)
//
//            loginBtnConstraintSet.applyTo(this@WalletInfoView)
        }

        balanceRate?.apply {
            visible()
            text = "₩000"
        }
    }

    private fun showDetailUI() {
        for (i in 0 until childCount) {
            getChildAt(i)?.let {
                val tag = (it.tag ?: "").toString()
                when (tag) {
                    mTagLoginRequire -> it.gone()
                    mTagShowAlways -> it.visible()
                    else -> {
                        if (it.visibility != View.GONE)
                            it.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun showLoginRequireUI() {
        for (i in 0 until childCount) {
            getChildAt(i)?.let {
                val tag = (it.tag ?: "").toString()
                when (tag) {
                    mTagLoginRequire -> it.visible()
                    mTagShowAlways -> it.visible()
                    else -> {
                        if (it.visibility != View.GONE)
                            it.visibility = View.INVISIBLE
                    }
                }
            }
        }
    }

    private fun showQRCodeDialog() {
        if (fragmentManager != null) {
            QRCodeDialog.show(mAddress!!, fragmentManager!!)
        } else {
            "Cannot show QR Code dialog on this context".logAsError("WalletInfoView")
        }
    }


    fun setViewMode(@ViewMode mode: Int) {
        if (mViewMode != mode) {
            mViewMode = mode
            inflateLayout()
            fetchData()
        }
    }

    fun setShowQRCode(isShow: Boolean) {
        mShowQRCodeButton = isShow
        updateUI()
    }

    fun setShowCopy(isShow: Boolean) {
        mShowCopyButton = isShow
        updateUI()
    }

    companion object {
        @Retention(AnnotationRetention.SOURCE)
        @IntDef(MODE_ADDRESS_BALANCE, MODE_ONLY_ADDRESS, MODE_ONLY_BALANCE)
        annotation class ViewMode

        const val MODE_ADDRESS_BALANCE = 0
        const val MODE_ONLY_ADDRESS = 1
        const val MODE_ONLY_BALANCE = 2
    }
}