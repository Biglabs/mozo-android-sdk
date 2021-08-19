package io.mozocoin.sdk.wallet

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
import androidx.core.view.isVisible
import androidx.core.view.postDelayed
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import io.mozocoin.sdk.MozoAuth
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.MozoTx
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.Constant
import io.mozocoin.sdk.common.ViewModels
import io.mozocoin.sdk.common.model.BalanceInfo
import io.mozocoin.sdk.common.model.Profile
import io.mozocoin.sdk.common.model.TransactionHistory
import io.mozocoin.sdk.common.service.MozoAPIsService
import io.mozocoin.sdk.databinding.FragmentMozoWalletOffBinding
import io.mozocoin.sdk.transaction.TransactionDetailsActivity
import io.mozocoin.sdk.transaction.TransactionHistoryRecyclerAdapter
import io.mozocoin.sdk.transaction.payment.PaymentRequestActivity
import io.mozocoin.sdk.ui.dialog.QRCodeDialog
import io.mozocoin.sdk.utils.*
import kotlinx.coroutines.*

internal class OffChainWalletFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {
    private var _binding: FragmentMozoWalletOffBinding? = null
    private val histories = arrayListOf<TransactionHistory>()
    private val onItemClick = { history: TransactionHistory ->
        if (context != null) {
            TransactionDetailsActivity.start(requireContext(), history)
        }
    }

    private var buttonPaymentRequest = true
    private var buttonSend = true

    private val historyAdapter: TransactionHistoryRecyclerAdapter by lazy {
        TransactionHistoryRecyclerAdapter(layoutInflater, onItemClick, null)
    }
    private var currentAddress: String? = null
    private var fetchDataJob: Job? = null
    private var fetchDataJobHandler: Job? = null
    private var generateQRJob: Job? = null

    private var mOnChainBalanceInfo: BalanceInfo? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMozoWalletOffBinding.inflate(inflater, container, false)
        return _binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = _binding ?: return
        binding.walletFragmentOffSwipe.apply {
            mozoSetup()
            setOnRefreshListener(this@OffChainWalletFragment)
        }
        binding.walletFragmentCurrencyValue.isVisible = Constant.SHOW_MOZO_EQUIVALENT_CURRENCY
        binding.walletFragmentBtnPaymentRequest.apply {
            visibility = if (buttonPaymentRequest) View.VISIBLE else View.GONE
            click {
                if (context != null) PaymentRequestActivity.start(requireContext())
            }
        }
        binding.walletFragmentBtnSend.apply {
            visibility = if (buttonSend) View.VISIBLE else View.GONE
            click {
                MozoTx.getInstance().transfer()
            }
        }
        binding.walletFragmentBtnViewAll.click {
            MozoTx.getInstance().openTransactionHistory(it.context)
        }
        binding.walletFragmentQrImage.click {
            QRCodeDialog.show(it.context, currentAddress ?: return@click)
        }
        binding.walletFragmentAddress.click {
            it.copyWithToast()
        }

        binding.walletInfoDetectedOnChain.click {
            val lastTxHash = SharedPrefsUtils.getLastTxConvertOnChainInOffChain()
            if (lastTxHash.isNullOrEmpty()) {
                ConvertOnInOffActivity.start(
                    it.context,
                    currentAddress ?: return@click,
                    mOnChainBalanceInfo ?: return@click
                )
            } else {
                ConvertBroadcastActivity.start(it.context, lastTxHash)
            }
        }

        historyAdapter.emptyView = binding.walletFragmentHistoryEmptyView
        binding.walletFragmentHistoryRecycler.apply {
            setHasFixedSize(false)
            adapter = historyAdapter
        }
    }

    override fun onInflate(context: Context, attrs: AttributeSet, savedInstanceState: Bundle?) {
        super.onInflate(context, attrs, savedInstanceState)
        val typedArray = resources.obtainAttributes(attrs, R.styleable.MozoWalletFragment)
        buttonPaymentRequest = typedArray.getBoolean(
            R.styleable.MozoWalletFragment_buttonPaymentRequest,
            buttonPaymentRequest
        )
        buttonSend = typedArray.getBoolean(R.styleable.MozoWalletFragment_buttonSend, buttonSend)
        typedArray.recycle()
    }

    override fun onResume() {
        super.onResume()
        _binding?.walletInfoDetectedOnChain?.gone()
        if (MozoAuth.getInstance().isSignedIn()) {
            view?.postDelayed(250) {
                MozoSDK.getInstance().profileViewModel.run {
                    profileLiveData.observe(
                        this@OffChainWalletFragment.viewLifecycleOwner,
                        profileObserver
                    )
                    balanceAndRateLiveData.observeForever(balanceAndRateObserver)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        MozoSDK.getInstance().profileViewModel.run {
            profileLiveData.removeObserver(profileObserver)
            balanceAndRateLiveData.removeObserver(balanceAndRateObserver)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fetchDataJob?.cancel()
        fetchDataJobHandler?.cancel()
        generateQRJob?.cancel()
        _binding = null
    }

    override fun onRefresh() {
        MozoSDK.getInstance().profileViewModel.fetchBalance(requireContext())
        fetchData()
    }

    private val profileObserver = Observer<Profile?> {
        if (it?.walletInfo != null) {
            currentAddress = it.walletInfo!!.offchainAddress
            historyAdapter.address = currentAddress

            _binding?.walletFragmentAddress?.text = currentAddress

            fetchData()

            generateQRJob?.cancel()
            generateQRJob = generateQRImage()
        } else {

            /* Clear last information */
            view?.find<TextView>(R.id.wallet_fragment_balance_value)?.text = null
            view?.find<TextView>(R.id.wallet_fragment_currency_value)?.text = null

            currentAddress = null
            histories.clear()
            historyAdapter.address = null
            historyAdapter.setData(mutableListOf())

            _binding?.walletFragmentAddress?.text = null
            generateQRJob?.cancel()
            _binding?.walletFragmentQrImage?.setImageDrawable(null)
        }
    }

    private val balanceAndRateObserver = Observer<ViewModels.BalanceAndRate?> {
        it?.run {
            view?.find<TextView>(R.id.wallet_fragment_balance_value)?.apply {
                text = if (realValues) balanceNonDecimal.displayString() else null
            }
            view?.find<TextView>(R.id.wallet_fragment_currency_value)?.apply {
                text = if (realValues) balanceNonDecimalInCurrencyDisplay else null
            }
        }
    }

    @Synchronized
    private fun fetchData() {
        fetchDataJob?.cancel()
        fetchDataJobHandler?.cancel()
        fetchDataJobHandler = MozoSDK.scope.launch {
            delay(1000)
            if (!isAdded || activity == null || _binding == null) return@launch
            if (context == null || currentAddress == null) return@launch

            MozoAPIsService.getInstance().getTransactionHistory(
                requireContext(), currentAddress!!,
                page = Constant.PAGING_START_INDEX,
                callback = { data, _ ->
                    _binding?.walletFragmentOffSwipe?.isRefreshing = false
                    _binding?.walletFragmentTxLoading?.gone()

                    if (data?.items == null) {
                        historyAdapter.setCanLoadMore(false)
                        historyAdapter.setData(mutableListOf())
                        return@getTransactionHistory
                    }

                    fetchDataJob = MozoSDK.scope.launch {
                        histories.clear()
                        histories.addAll(data.items!!.map {
                            it.apply {
                                contactName = MozoSDK.getInstance().contactViewModel.findByAddress(
                                    if (it.type(currentAddress)) it.addressTo else it.addressFrom
                                )?.name
                            }
                        })
                        withContext(Dispatchers.Main) {
                            fetchDataJob = null
                            historyAdapter.setCanLoadMore(false)
                            historyAdapter.setData(histories)
//                            _binding?.walletFragmentHistoryRecycler?.scheduleLayoutAnimation()
                        }
                    }
                },
                retry = this@OffChainWalletFragment::fetchData
            )

            /**
             * Detect Onchain MozoX inside Offchain Wallet Address
             * */

            MozoAPIsService.getInstance()
                .getOnChainBalanceInOffChain(requireContext(), currentAddress!!, { data, _ ->
                    _binding ?: return@getOnChainBalanceInOffChain
                    data ?: return@getOnChainBalanceInOffChain

                    _binding?.walletInfoDetectedOnChain?.isVisible =
                        data.detectedOnchain || !data.convertToMozoXOnchain
                    mOnChainBalanceInfo = data.balanceOfTokenOnchain

                    when {
                        !data.convertToMozoXOnchain -> {
                            _binding?.walletInfoDetectedOnChain?.text = HtmlCompat.fromHtml(
                                getString(R.string.mozo_convert_on_in_off_converting),
                                FROM_HTML_MODE_LEGACY
                            )
                        }
                        data.detectedOnchain -> {
                            _binding?.walletInfoDetectedOnChain?.text = HtmlCompat.fromHtml(
                                getString(
                                    R.string.mozo_convert_on_in_off_detected,
                                    data.balanceOfTokenOnchain?.balanceNonDecimal()?.displayString()
                                ), FROM_HTML_MODE_LEGACY
                            )
                        }
                    }

                    if (data.convertToMozoXOnchain) {
                        SharedPrefsUtils.setLastInfoConvertOnChainInOffChain(null, null)
                    }

                }, this@OffChainWalletFragment::fetchData)
        }
    }

    private fun generateQRImage() = MozoSDK.scope.launch {
        val qrImage = Support.generateQRCode(
            currentAddress ?: return@launch,
            resources.dp2Px(128f).toInt()
        )
        withContext(Dispatchers.Main) {
            _binding ?: return@withContext
            _binding?.walletFragmentQrImage?.setImageBitmap(qrImage)
        }
        generateQRJob = null
    }

    @Suppress("unused")
    fun showPaymentRequestButton(display: Boolean) {
        buttonPaymentRequest = display
        _binding?.walletFragmentBtnPaymentRequest?.visibility =
            if (buttonPaymentRequest) View.VISIBLE else View.GONE
    }

    @Suppress("unused")
    fun showSendButton(display: Boolean) {
        buttonSend = display
        _binding?.walletFragmentBtnSend?.visibility = if (buttonSend) View.VISIBLE else View.GONE
    }

    companion object {
        fun getInstance() = OffChainWalletFragment()
    }
}