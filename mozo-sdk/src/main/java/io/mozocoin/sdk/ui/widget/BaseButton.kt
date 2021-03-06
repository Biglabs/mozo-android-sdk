package io.mozocoin.sdk.ui.widget

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.lifecycle.Observer
import com.google.android.material.button.MaterialButton
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.MessageEvent
import io.mozocoin.sdk.common.model.Profile
import io.mozocoin.sdk.utils.dp2Px
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

internal abstract class BaseButton : MaterialButton {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attributes: AttributeSet?) : this(context, attributes, R.attr.buttonStyle)
    constructor(context: Context, attributes: AttributeSet?, defStyle: Int) : super(context, attributes, defStyle) {
        super.setAllCaps(false)
        super.setTextColor(Color.WHITE)
        super.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        super.setTypeface(Typeface.DEFAULT_BOLD)
        super.setBackgroundResource(R.drawable.mozo_dr_btn)
        super.setOnClickListener { onClick(it) }

        val padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15f, resources.displayMetrics).toInt()
        super.setPaddingRelative(padding, padding, padding, padding)

        super.setIconResource(this.buttonIcon())
        super.setIconTintResource(android.R.color.white)
        iconGravity = ICON_GRAVITY_TEXT_START
        iconSize = resources.dp2Px(15f).toInt()
        iconPadding = resources.dp2Px(10f).toInt()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            MozoSDK.getInstance().profileViewModel.run {
                profileLiveData.observeForever(profileObserver)
            }
            if (!EventBus.getDefault().isRegistered(this)) {
                EventBus.getDefault().register(this)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (!isInEditMode) {
            MozoSDK.getInstance().profileViewModel.run {
                profileLiveData.removeObserver(profileObserver)
            }
            EventBus.getDefault().unregister(this)
        }
    }

    @Suppress("UNUSED_PARAMETER", "unused")
    @Subscribe
    internal fun onAuthorizeChanged(auth: MessageEvent.Auth) {
        authorizeChanged(MozoSDK.getInstance().profileViewModel.hasWallet())
    }

    abstract fun buttonIcon(): Int

    private val profileObserver = Observer<Profile?> {
        authorizeChanged(it?.walletInfo != null)
    }

    protected open fun authorizeChanged(isSignUpCompleted: Boolean) {

    }

    protected open fun onClick(view: View) {

    }

    override fun setOnClickListener(l: OnClickListener?) {
        // ignore
    }
//
//    override fun setBackground(background: Drawable?) {
//        // ignore
//    }
//
//    override fun setBackgroundColor(color: Int) {
//        // ignore
//    }
//
//    override fun setBackgroundResource(resid: Int) {
//        // ignore
//    }
//
//    override fun setBackgroundTintList(tint: ColorStateList?) {
//        // ignore
//    }
//
//    override fun setBackgroundTintMode(tintMode: PorterDuff.Mode?) {
//        // ignore
//    }
//
//    override fun setTextColor(color: Int) {
//        // ignore
//    }
//
//    override fun setTextColor(colors: ColorStateList?) {
//        // ignore
//    }
}