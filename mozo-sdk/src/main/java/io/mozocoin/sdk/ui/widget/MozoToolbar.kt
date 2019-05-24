package io.mozocoin.sdk.ui.widget

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.MessageEvent
import io.mozocoin.sdk.utils.click
import org.greenrobot.eventbus.EventBus

class MozoToolbar : ConstraintLayout {

    private var viewScreenTitle: TextView? = null
    private var viewButtonBack: View? = null
    private var viewButtonClose: TextView? = null

    private var mTitle: String? = null
    private var mShowBack = false
    private var mShowClose = false
    private var mButtonCloseText: String? = null
    private var mPaddingTop = -1

    var onBackPress: (() -> Unit)? = null
    var onClosePress: (() -> Unit)? = null

    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        val typeArray = context.obtainStyledAttributes(attrs, R.styleable.MozoToolbar, defStyleAttr, 0)
        try {
            mTitle = typeArray.getString(R.styleable.MozoToolbar_title)
            mShowBack = typeArray.getBoolean(R.styleable.MozoToolbar_buttonBack, mShowBack)
            mShowClose = typeArray.getBoolean(R.styleable.MozoToolbar_buttonClose, mShowClose)
            mButtonCloseText = typeArray.getString(R.styleable.MozoToolbar_buttonCloseText)
        } finally {
            typeArray.recycle()
        }

        inflate(context, R.layout.view_toolbar, this)

        viewScreenTitle = this.findViewById(R.id.screen_title)
        viewButtonBack = this.findViewById(R.id.button_back)
        viewButtonClose = this.findViewById(R.id.button_close)
        updateUI()

        if (!isInEditMode) {
            viewButtonBack?.click {
                if (onBackPress != null) onBackPress?.invoke()
                else (context as? Activity)?.onBackPressed()
            }
            viewButtonClose?.click {
                if (onClosePress != null) onClosePress?.invoke()
                else {
                    EventBus.getDefault().post(MessageEvent.CloseActivities())
                }
            }
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (mPaddingTop == -1) mPaddingTop = paddingTop
        parent?.let {
            if (!(it as View).fitsSystemWindows) {
                val inset = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24f, resources.displayMetrics)
                setPaddingRelative(
                        paddingStart,
                        (mPaddingTop + inset).toInt(),
                        paddingEnd,
                        paddingBottom
                )
            }
        }
    }

    private fun updateUI() {
        viewScreenTitle?.text = mTitle
        viewButtonBack?.visibility = if (mShowBack) View.VISIBLE else View.GONE
        viewButtonClose?.visibility = if (mShowClose) View.VISIBLE else View.GONE

        viewButtonClose?.text = mButtonCloseText ?: context.getString(R.string.mozo_button_cancel)
    }

    fun showBackButton(isShow: Boolean) {
        mShowBack = isShow
        updateUI()
    }

    fun showCloseButton(isShow: Boolean) {
        mShowClose = isShow
        updateUI()
    }

    fun setTitle(title: String?) {
        mTitle = title
        updateUI()
    }

    fun setTitle(@StringRes id: Int) {
        mTitle = context.getString(id)
        updateUI()
    }

    fun setCloseButtonText(text: String) {
        mButtonCloseText = text
        updateUI()
    }
}