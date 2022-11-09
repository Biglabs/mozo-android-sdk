package io.mozocoin.sdk.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import io.mozocoin.sdk.databinding.ActivityWebviewBinding
import io.mozocoin.sdk.utils.*

internal class MozoWebViewActivity : BaseActivity() {

    private lateinit var binding: ActivityWebviewBinding

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent?.getStringExtra(KEY_DATA) ?: return finish()

        binding = ActivityWebviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonClose.click { finish() }
        binding.webView.apply {
            settings.javaScriptEnabled = true
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportZoom(false)
            settings.userAgentString = Support.userAgent()
        }
        binding.buttonRefresh.click {
            binding.webView.reload()
        }
        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                binding.errorContainer.gone()
                binding.webView.visible()
                binding.progressIndicator.visible()
            }

            override fun onPageFinished(view: WebView, url: String) {
                binding.title.text = view.title
                binding.progressIndicator.gone()
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                binding.webView.gone()
                binding.progressIndicator.gone()
                binding.errorContainer.visible()
            }
        }

        binding.webView.loadUrl(url)
    }

    companion object {
        private const val KEY_DATA = "url_data"
        fun start(context: Context, url: String) {
            context.launchActivity<MozoWebViewActivity> {
                putExtra(KEY_DATA, url)
            }
        }
    }
}