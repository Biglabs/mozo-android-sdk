package io.mozocoin.sdk.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.os.Build
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import io.mozocoin.sdk.R

@Suppress("unused")
class MozoSnackbar(
        parent: ViewGroup,
        content: View,
        contentViewCallback: com.google.android.material.snackbar.ContentViewCallback
) : BaseTransientBottomBar<MozoSnackbar>(parent, content, contentViewCallback) {

    private val accessibilityManager = parent.context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    private var hasAction: Boolean = false

    override fun getSnackbarBaseLayoutResId(): Int {
        return R.layout.view_mozo_snack_bar_base
    }

    /**
     * Update the text in this [Snackbar].
     *
     * @param message The new text for this [BaseTransientBottomBar].
     */
    @SuppressLint("RestrictedApi")
    fun setText(message: CharSequence): MozoSnackbar {
        view.findViewById<TextView>(R.id.snackbar_text)?.text = message
        return this
    }

    /**
     * Update the text in this [Snackbar].
     *
     * @param resId The new text for this [BaseTransientBottomBar].
     */
    fun setText(@StringRes resId: Int): MozoSnackbar {
        return setText(context.getText(resId))
    }

    /**
     * Set the action to be displayed in this [BaseTransientBottomBar].
     *
     * @param resId String resource to display for the action
     * @param listener callback to be invoked when the action is clicked
     */
    fun setAction(@StringRes resId: Int, listener: View.OnClickListener): MozoSnackbar {
        return setAction(context.getText(resId), listener)
    }

    /**
     * Set the action to be displayed in this [BaseTransientBottomBar].
     *
     * @param text Text to display for the action
     * @param listener callback to be invoked when the action is clicked
     */
    @SuppressLint("RestrictedApi")
    fun setAction(text: CharSequence, listener: View.OnClickListener?): MozoSnackbar {
        view.findViewById<TextView>(R.id.snackbar_action)?.run {
            if (TextUtils.isEmpty(text) || listener == null) {
                visibility = View.GONE
                setOnClickListener(null)
                hasAction = false
            } else {
                hasAction = true
                visibility = View.VISIBLE
                setText(text)
                setOnClickListener { view ->
                    listener.onClick(view)
                    // Now dismiss the Snackbar
                    // dispatchDismiss(BaseCallback.DISMISS_EVENT_ACTION)
                }
            }
        }

        view.findViewById<View>(R.id.snackbar_container)?.run {
            if (TextUtils.isEmpty(text) || listener == null) {
                setOnClickListener(null)
            } else {
                setOnClickListener { view ->
                    listener.onClick(view)
                    // Now dismiss the Snackbar
                    // dispatchDismiss(BaseCallback.DISMISS_EVENT_ACTION)
                }
            }
        }
        return this
    }

    @SuppressLint("ObsoleteSdkInt")
    @Duration
    override fun getDuration(): Int {
        val userSetDuration = super.getDuration()
        if (userSetDuration == LENGTH_INDEFINITE) {
            return LENGTH_INDEFINITE
        }

        // If touch exploration is enabled override duration to give people chance to interact.
        return if (hasAction && accessibilityManager.isTouchExplorationEnabled)
            LENGTH_INDEFINITE
        else
            userSetDuration
    }

    /**
     * Sets the text color of the message specified in [.setText] and [ ][.setText].
     */
    @SuppressLint("RestrictedApi")
    fun setTextColor(colors: ColorStateList): MozoSnackbar {
        view.findViewById<TextView>(R.id.snackbar_text)?.setTextColor(colors)
        return this
    }

    /**
     * Sets the text color of the message specified in [.setText] and [ ][.setText].
     */
    @SuppressLint("RestrictedApi")
    fun setTextColor(@ColorInt color: Int): MozoSnackbar {
        view.findViewById<TextView>(R.id.snackbar_text)?.setTextColor(color)
        return this
    }

    /**
     * Sets the text color of the action specified in [.setAction].
     */
    @SuppressLint("RestrictedApi")
    fun setActionTextColor(colors: ColorStateList): MozoSnackbar {
        view.findViewById<TextView>(R.id.snackbar_action)?.setTextColor(colors)
        return this
    }

    /**
     * Sets the text color of the action specified in [.setAction].
     */
    @SuppressLint("RestrictedApi")
    fun setActionTextColor(@ColorInt color: Int): MozoSnackbar {
        view.findViewById<TextView>(R.id.snackbar_action)?.setTextColor(color)
        return this
    }

    /** Sets the tint color of the background Drawable.  */
    @SuppressLint("RestrictedApi")
    fun setBackgroundTint(@ColorInt color: Int): MozoSnackbar {
        val background = view.background
        if (background != null) {
            // Drawable doesn't implement setTint in API 21 and MozoSnackbar does not yet use
            // MaterialShapeDrawable as its background (i.e. TintAwareDrawable)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                DrawableCompat.setTint(background, color)
            } else {
                background.setColorFilter(color, PorterDuff.Mode.SRC_IN)
            }
        }
        return this
    }

    companion object {

        private var instance: MozoSnackbar? = null

        private fun findSuitableParent(view: View?): ViewGroup? {
            var tmpView = view
            var fallback: ViewGroup? = null
            do {
                if (tmpView is CoordinatorLayout) {
                    // We've found a CoordinatorLayout, use it
                    return tmpView
                } else if (tmpView is FrameLayout) {
                    if (tmpView.id == android.R.id.content) {
                        // If we've hit the decor content view, then we didn't find a CoL in the
                        // hierarchy, so use it.
                        return tmpView
                    } else {
                        // It's not the content view but we'll use it as our fallback
                        fallback = tmpView
                    }
                }

                if (tmpView != null) {
                    // Else, we will loop and crawl up the view hierarchy and try to find a parent
                    val parent = tmpView.parent
                    tmpView = if (parent is View) parent else null
                }
            } while (tmpView != null)

            // If we reach here then we didn't find a CoL or a suitable content view so we'll fallback
            return fallback
        }

        /**
         * Make a Snackbar to display a message
         *
         *
         * Snackbar will try and find a parent view to hold Snackbar's view from the value given to
         * `view`. Snackbar will walk up the view tree trying to find a suitable parent, which is
         * defined as a [CoordinatorLayout] or the window decor's content view, whichever comes
         * first.
         *
         *
         * Having a [CoordinatorLayout] in your view hierarchy allows Snackbar to enable certain
         * features, such as swipe-to-dismiss and automatically moving of widgets.
         *
         * @param view The view to find a parent from.
         * @param text The text to show. Can be formatted text.
         * @param duration How long to display the message. Can be [.LENGTH_SHORT], [     ][.LENGTH_LONG], [.LENGTH_INDEFINITE], or a custom duration in milliseconds.
         */
        fun make(
                view: View, text: CharSequence, @Duration duration: Int): MozoSnackbar {
            val parent = findSuitableParent(view) ?: throw IllegalArgumentException(
                    "No suitable parent found from the given view. Please provide a valid view.")
            val inflater = LayoutInflater.from(parent.context)
            val content = inflater.inflate(
                    R.layout.view_mozo_snack_bar,
                    parent,
                    false)

            val messageView = content.findViewById<View>(R.id.snackbar_text)
            val actionView = content.findViewById<View>(R.id.snackbar_action)

            val contentCallback = object : com.google.android.material.snackbar.ContentViewCallback {
                override fun animateContentIn(delay: Int, duration: Int) {
                    messageView.alpha = 0f
                    messageView.animate().alpha(1f).setDuration(duration.toLong()).setStartDelay(delay.toLong()).start()

                    if (actionView.visibility == View.VISIBLE) {
                        actionView.alpha = 0f
                        actionView.animate().alpha(1f).setDuration(duration.toLong()).setStartDelay(delay.toLong()).start()
                    }
                }

                override fun animateContentOut(delay: Int, duration: Int) {
                    messageView.alpha = 1f
                    messageView.animate().alpha(0f).setDuration(duration.toLong()).setStartDelay(delay.toLong()).start()

                    if (actionView.visibility == View.VISIBLE) {
                        actionView.alpha = 1f
                        actionView.animate().alpha(0f).setDuration(duration.toLong()).setStartDelay(delay.toLong()).start()
                    }
                }
            }

            val snackbar = MozoSnackbar(parent, content, contentCallback)
            snackbar.setText(text)
            snackbar.duration = duration
            instance = snackbar
            return snackbar
        }

        /**
         * Make a Snackbar to display a message.
         *
         *
         * Snackbar will try and find a parent view to hold Snackbar's view from the value given to
         * `view`. Snackbar will walk up the view tree trying to find a suitable parent, which is
         * defined as a [CoordinatorLayout] or the window decor's content view, whichever comes
         * first.
         *
         *
         * Having a [CoordinatorLayout] in your view hierarchy allows Snackbar to enable certain
         * features, such as swipe-to-dismiss and automatically moving of widgets.
         *
         * @param view The view to find a parent from.
         * @param resId The resource id of the string resource to use. Can be formatted text.
         * @param duration How long to display the message. Can be [.LENGTH_SHORT], [     ][.LENGTH_LONG], [.LENGTH_INDEFINITE], or a custom duration in milliseconds.
         */
        fun make(view: View, @StringRes resId: Int, @Duration duration: Int): MozoSnackbar {
            return make(view, view.resources.getText(resId), duration)
        }

        fun dismiss() {
            instance?.dismiss()
            instance = null
        }
    }
}