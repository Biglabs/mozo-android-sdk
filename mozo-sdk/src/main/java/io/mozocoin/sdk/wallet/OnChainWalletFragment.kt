package io.mozocoin.sdk.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import io.mozocoin.sdk.MozoAuth
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.MozoWallet
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.Constant
import io.mozocoin.sdk.common.MessageEvent
import io.mozocoin.sdk.common.ViewModels
import io.mozocoin.sdk.common.model.Profile
import io.mozocoin.sdk.common.model.WalletInfo
import io.mozocoin.sdk.common.service.MozoAPIsService
import io.mozocoin.sdk.databinding.FragmentMozoWalletOnBinding
import io.mozocoin.sdk.ui.dialog.QRCodeDialog
import io.mozocoin.sdk.utils.*
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.math.BigDecimal

class OnChainWalletFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {
    private var _binding: FragmentMozoWalletOnBinding? = null
    private val binding get() = _binding!!
    private var wallet: WalletInfo? = null
    private var txHistoryUrl: String? = null
    private var generateQRJob: Job? = null

    private var mBalanceETH = BigDecimal.ZERO
    private var mBalanceOnChain = BigDecimal.ZERO

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMozoWalletOnBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.walletFragmentOnSwipe.apply {
            mozoSetup()
            setOnRefreshListener(this@OnChainWalletFragment)
        }

        binding.walletFragmentOnHowButton.apply {
            isSelected = false
            click {
                context?.openTab("${Support.homePage()}/" + getString(R.string.tips_buy_on_chain))
            }
        }

        binding.walletFragmentOnQrImage.click {
            QRCodeDialog.show(context ?: return@click, wallet?.onchainAddress ?: return@click)
        }

        binding.walletFragmentOnAddress.click {
            it.copyWithToast()
        }

        binding.walletFragmentOnTxHistory.apply {
            isSelected = true
            click {
                context?.openTab(txHistoryUrl ?: return@click)
            }
        }

        binding.walletFragmentOnConvert.click {
            ConvertOnChainActivity.start(context ?: return@click)
        }

        binding.walletFragmentOnTokenCurrency.isVisible = Constant.SHOW_MOZO_EQUIVALENT_CURRENCY
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

            MozoSDK.getInstance().profileViewModel.balanceAndRateLiveData.observe(viewLifecycleOwner, balanceAndRateObserver)
            MozoSDK.getInstance().profileViewModel.profileLiveData.observe(viewLifecycleOwner, profileObserver)
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
        _binding = null
    }

    override fun onRefresh() {
        fetchData()
    }

    @Suppress("unused")
    @Subscribe
    internal fun onReceiveEvent(event: MessageEvent.ConvertOnChain) {
        event::class.java.canonicalName.logAsInfo("onReceiveEvent")
        fetchData()
    }

    private fun updateUI() {
        txHistoryUrl = StringBuilder("https://")
                .append(Support.domainEhterscan())
                .append("/address/")
                .append(wallet?.onchainAddress)
                .append("#transactions")
                .toString()

        binding.walletFragmentOnAddress.text = wallet?.onchainAddress

        generateQRJob?.cancel()
        generateQRJob = generateQRImage()
    }

    private fun generateQRImage() = MozoSDK.scope.launch {
        val qrImage = Support.createQRCode(
                wallet?.onchainAddress ?: return@launch,
                resources.dp2Px(128f).toInt()
        )
        withContext(Dispatchers.Main) {
            binding.walletFragmentOnQrImage.setImageBitmap(qrImage)
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
            binding.walletFragmentOnSwipe.isRefreshing = false

            data ?: return@getOnChainBalance

            mBalanceETH = data.balanceOfETH?.balanceNonDecimal().safe()
            binding.walletFragmentEthBalance.text = mBalanceETH.displayString()
            binding.walletFragmentEthCurrency.text = MozoSDK.getInstance().profileViewModel
                    .calculateAmountInCurrency(mBalanceETH, false)

            mBalanceOnChain = data.balanceOfToken?.balanceNonDecimal().safe()
            binding.walletFragmentOnTokenBalance.text = mBalanceOnChain.displayString()
            binding.walletFragmentOnTokenCurrency.text = MozoWallet.getInstance()
                    .amountInCurrency(mBalanceOnChain)

            binding.walletFragmentOnConvert.isEnabled = data.convertToMozoXOnchain
            binding.walletFragmentOnConvert.setText(
                    if (data.convertToMozoXOnchain) R.string.mozo_button_onchain_convert
                    else R.string.mozo_button_onchain_converting
            )
        }, this::fetchData)
    }

    private val balanceAndRateObserver = Observer<ViewModels.BalanceAndRate?> {
        binding.walletFragmentEthCurrency.text = MozoSDK.getInstance().profileViewModel
                .calculateAmountInCurrency(mBalanceETH, false)

        binding.walletFragmentOnTokenCurrency.text = MozoWallet.getInstance()
                .amountInCurrency(mBalanceOnChain)
    }

    private val profileObserver = Observer<Profile?> {
        onResume()
    }

    companion object {
        fun getInstance() = OnChainWalletFragment()
    }
}