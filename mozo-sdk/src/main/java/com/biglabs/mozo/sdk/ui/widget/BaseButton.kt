package com.biglabs.mozo.sdk.ui.widget

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.Button
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.common.MessageEvent
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

open class BaseButton : Button {
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
        val drawablePadding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, resources.displayMetrics).toInt()
        super.setCompoundDrawablePadding(drawablePadding)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(measuredWidth, measuredHeight)
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
        authorizeChanged(auth)
    }

    protected open fun authorizeChanged(auth: MessageEvent.Auth) {

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