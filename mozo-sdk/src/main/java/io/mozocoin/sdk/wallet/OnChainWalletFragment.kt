package io.mozocoin.sdk.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import io.mozocoin.sdk.MozoAuth
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.MozoWallet
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.MessageEvent
import io.mozocoin.sdk.common.ViewModels
import io.mozocoin.sdk.common.model.Profile
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

    private var mBalanceETH = BigDecimal.ZERO
    private var mBalanceOnChain = BigDecimal.ZERO

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
                context?.openTab("https://mozocoin.io/retailer/support#home-faq-how-to-buy-mozox-onchain-on-exchange")
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
        if (MozoAuth.getInstance().isSignedIn()) {
            wallet = MozoWallet.getInstance().getWallet()?.buildWalletInfo()

            updateUI()
            fetchData()

            if (!EventBus.getDefault().isRegistered(this)) {
                EventBus.getDefault().register(this)
            }

            MozoSDK.getInstance().profileViewModel.balanceAndRateLiveData.observe(this, balanceAndRateObserver)
            MozoSDK.getInstance().profileViewModel.profileLiveData.observe(this, profileObserver)
        }
    }

    override fun onPause() {
        super.onPause()
        EventBus.getDefault().unregister(this)
        MozoSDK.getInstance().profileViewModel.balanceAndRateLiveData.removeObserver(balanceAndRateObserver)
        MozoSDK.getInstance().profileViewModel.profileLiveData.removeObserver(profileObserver)

        mBalanceETH = BigDecimal.ZERO
        mBalanceOnChain = BigDecimal.ZERO
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

    private fun updateUI() {
        txHistoryUrl = StringBuilder("https://")
                .append(Support.domainEhterscan())
                .append("/address/")
                .append(wallet?.onchainAddress)
                .append("#transactions")
                .toString()

        wallet_fragment_on_address?.text = wallet?.onchainAddress

        generateQRJob?.cancel()
        generateQRJob = generateQRImage()
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
        if (wallet == null) {
            wallet = MozoWallet.getInstance().getWallet()?.buildWalletInfo()
            updateUI()
        }
        MozoAPIsService.getInstance().getOnChainBalance(
                context ?: return,
                wallet?.onchainAddress ?: return, { data, _ ->
            wallet_fragment_on_swipe?.isRefreshing = false

            data ?: return@getOnChainBalance

            mBalanceETH = data.balanceOfETH?.balanceNonDecimal().safe()
            wallet_fragment_eth_balance?.text = mBalanceETH.displayString()
            wallet_fragment_eth_currency?.text = MozoSDK.getInstance().profileViewModel
                    .calculateAmountInCurrency(mBalanceETH, false)

            mBalanceOnChain = data.balanceOfToken?.balanceNonDecimal().safe()
            wallet_fragment_on_token_balance?.text = mBalanceOnChain.displayString()
            wallet_fragment_on_token_currency?.text = MozoWallet.getInstance()
                    .amountInCurrency(mBalanceOnChain)

            wallet_fragment_on_convert?.isEnabled = data.convertToMozoXOnchain
            wallet_fragment_on_convert?.setText(
                    if (data.convertToMozoXOnchain) R.string.mozo_button_onchain_convert
                    else R.string.mozo_button_onchain_converting
            )
        }, this::fetchData)
    }

    private val balanceAndRateObserver = Observer<ViewModels.BalanceAndRate?> {
        wallet_fragment_eth_currency?.text = MozoSDK.getInstance().profileViewModel
                .calculateAmountInCurrency(mBalanceETH, false)

        wallet_fragment_on_token_currency?.text = MozoWallet.getInstance()
                .amountInCurrency(mBalanceOnChain)
    }

    private val profileObserver = Observer<Profile?> {
        onResume()
    }

    companion object {
        fun getInstance() = OnChainWalletFragment()
    }
}