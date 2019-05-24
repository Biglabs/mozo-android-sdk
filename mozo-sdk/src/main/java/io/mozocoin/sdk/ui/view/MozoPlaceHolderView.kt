package io.mozocoin.sdk.ui.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isVisible
import io.mozocoin.sdk.R
import io.mozocoin.sdk.utils.click
import io.mozocoin.sdk.utils.color
import io.mozocoin.sdk.utils.dp2Px
import kotlinx.android.synthetic.main.view_mozo_place_holder.view.*

class MozoPlaceHolderView : LinearLayout {

    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        View.inflate(context, R.layout.view_mozo_place_holder, this)

        val typeArray = context.obtainStyledAttributes(attrs, R.styleable.MozoPlaceHolderView, defStyle, 0)
        try {
            val defaultTextColor = context.color(R.color.mozo_color_section_text)

            icon = typeArray.getDrawable(R.styleable.MozoPlaceHolderView_holder_icon)
            iconSize = typeArray.getDimensionPixelSize(R.styleable.MozoPlaceHolderView_holder_iconSize, resources.dp2Px(160f).toInt())
            title = typeArray.getString(R.styleable.MozoPlaceHolderView_holder_title)
            titleColor = typeArray.getColor(R.styleable.MozoPlaceHolderView_holder_titleColor, defaultTextColor)
            content = typeArray.getString(R.styleable.MozoPlaceHolderView_holder_content)
            contentColor = typeArray.getColor(R.styleable.MozoPlaceHolderView_holder_contentColor, defaultTextColor)

            primaryButtonEnable = typeArray.getBoolean(R.styleable.MozoPlaceHolderView_holder_primaryButtonEnable, true)
            primaryButtonText = typeArray.getString(R.styleable.MozoPlaceHolderView_holder_primaryButtonText)
            primaryButtonVisible = typeArray.getBoolean(R.styleable.MozoPlaceHolderView_holder_primaryButtonVisible, true)

            secondButtonEnable = typeArray.getBoolean(R.styleable.MozoPlaceHolderView_holder_secondButtonEnable, true)
            secondButtonText = typeArray.getString(R.styleable.MozoPlaceHolderView_holder_secondButtonText)
            secondButtonVisible = typeArray.getBoolean(R.styleable.MozoPlaceHolderView_holder_secondButtonVisible, true)

        } finally {
            typeArray.recycle()
        }

        updateUI()
    }

    private fun updateUI() {
        place_holder_icon?.apply {
            setImageDrawable(icon)

            val params = layoutParams
            params.width = iconSize
            params.height = iconSize
            layoutParams = params
        }

        place_holder_title?.apply {
            text = title
            setTextColor(titleColor)
        }

        place_holder_content?.apply {
            text = content
            setTextColor(contentColor)
        }

        place_holder_primary_button?.apply {
            isEnabled = primaryButtonEnable
            text = primaryButtonText
            isVisible = primaryButtonVisible && !primaryButtonText.isNullOrEmpty()
            click { onPrimaryClicked?.invoke() }
        }

        place_holder_second_button?.apply {
            isEnabled = secondButtonEnable
            text = secondButtonText
            isVisible = secondButtonVisible && !secondButtonText.isNullOrEmpty()
            click { onSecondClicked?.invoke() }
        }
    }

    var onPrimaryClicked: (() -> Unit)? = null
    var onSecondClicked: (() -> Unit)? = null

    var icon: Drawable?
        set(value) {
            field = value
            updateUI()
        }
    var iconSize: Int
        set(value) {
            field = value
            updateUI()
        }
    var title: String?
        set(value) {
            field = value
            updateUI()
        }
    var titleColor: Int
        set(value) {
            field = value
            updateUI()
        }
    var content: String?
        set(value) {
            field = value
            updateUI()
        }
    var contentColor: Int
        set(value) {
            field = value
            updateUI()
        }
    var primaryButtonEnable: Boolean
        set(value) {
            field = value
            updateUI()
        }
    var primaryButtonText: String?
        set(value) {
            field = value
            updateUI()
        }
    var primaryButtonVisible: Boolean
        set(value) {
            field = value
            updateUI()
        }
    var secondButtonEnable: Boolean
        set(value) {
            field = value
            updateUI()
        }
    var secondButtonText: String?
        set(value) {
            field = value
            updateUI()
        }
    var secondButtonVisible: Boolean
        set(value) {
            field = value
            updateUI()
        }
}