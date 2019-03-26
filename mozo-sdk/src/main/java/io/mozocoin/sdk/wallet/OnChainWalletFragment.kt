package io.mozocoin.sdk.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.MozoWallet
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.MessageEvent
import io.mozocoin.sdk.common.model.WalletInfo
import io.mozocoin.sdk.common.service.MozoAPIsService
import io.mozocoin.sdk.ui.dialog.QRCodeDialog
import io.mozocoin.sdk.utils.*
import kotlinx.android.synthetic.main.fragment_mozo_wallet_on.*
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.math.BigDecimal

class OnChainWalletFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {

    private var wallet: WalletInfo? = null
    private var txHistoryUrl: String? = null
    private var generateQRJob: Job? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.fragment_mozo_wallet_on, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        wallet_fragment_on_swipe?.apply {
            mozoSetup()
            setOnRefreshListener(this@OnChainWalletFragment)
        }

        wallet_fragment_on_how_button?.apply {
            isSelected = false
            click {
                context?.openTab("https://mozocoin.io")
            }
        }

        wallet_fragment_on_qr_image?.click {
            QRCodeDialog.show(context ?: return@click, wallet?.onchainAddress ?: return@click)
        }

        wallet_fragment_on_address?.click {
            it.copyWithToast()
        }

        wallet_fragment_on_tx_history?.apply {
            isSelected = true
            click {
                context?.openTab(txHistoryUrl ?: return@click)
            }
        }

        wallet_fragment_on_convert?.click {
            ConvertOnChainActivity.start(context ?: return@click)
        }
    }

    override fun onResume() {
        super.onResume()
        wallet = MozoWallet.getInstance().getWallet().buildWalletInfo()

        txHistoryUrl = StringBuilder("https://")
                .append(Support.domainEhterscan())
                .append("/address/")
                .append(wallet?.onchainAddress)
                .append("#transactions")
                .toString()

        wallet_fragment_on_address?.text = wallet?.onchainAddress

        generateQRJob?.cancel()
        generateQRJob = generateQRImage()

        fetchData()

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }

    override fun onPause() {
        super.onPause()
        EventBus.getDefault().unregister(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        generateQRJob?.cancel()
    }

    override fun onRefresh() {
        fetchData()
    }

    @Suppress("unused")
    @Subscribe
    internal fun onReceiveEvent(event: MessageEvent.ConvertOnChain) {
        checkNotNull(event)

        fetchData()
    }

    private fun generateQRImage() = GlobalScope.launch {
        val qrImage = Support.generateQRCode(
                wallet?.onchainAddress ?: return@launch,
                resources.dp2Px(128f).toInt()
        )
        withContext(Dispatchers.Main) {
            wallet_fragment_on_qr_image?.setImageBitmap(qrImage)
        }
        generateQRJob = null
    }

    private fun fetchData() {
        MozoAPIsService.getInstance().getOnChainBalance(
                context ?: return,
                wallet?.onchainAddress ?: return
        ) { data, _ ->
            wallet_fragment_on_swipe?.isRefreshing = false

            data ?: return@getOnChainBalance
            wallet_fragment_eth_balance?.text = data.balanceOfETH?.balanceNonDecimal().displayString()
            wallet_fragment_eth_currency?.text = MozoSDK.getInstance().profileViewModel
                    .calculateAmountInCurrency(
                            data.balanceOfETH?.balanceNonDecimal() ?: BigDecimal.ZERO,
                            false
                    )

            wallet_fragment_on_token_balance?.text = data.balanceOfToken?.balanceNonDecimal().displayString()
            wallet_fragment_on_token_currency?.text = MozoWallet.getInstance()
                    .amountInCurrency(data.balanceOfToken?.balanceNonDecimal() ?: BigDecimal.ZERO)

            wallet_fragment_on_convert?.isEnabled = data.convertToMozoXOnchain
            wallet_fragment_on_convert?.setText(
                    if (data.convertToMozoXOnchain) R.string.mozo_button_onchain_convert
                    else R.string.mozo_button_onchain_converting
            )
        }
    }

    companion object {
        fun getInstance() = OnChainWalletFragment()
    }
}