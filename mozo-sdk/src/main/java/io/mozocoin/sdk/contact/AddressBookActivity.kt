package io.mozocoin.sdk.contact

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.model.Contact
import io.mozocoin.sdk.databinding.ActivityAddressBookBinding
import io.mozocoin.sdk.ui.BaseActivity
import io.mozocoin.sdk.utils.*
import kotlinx.coroutines.*

internal class AddressBookActivity : BaseActivity() {
    private lateinit var binding: ActivityAddressBookBinding
    private val contacts: ArrayList<Contact> = arrayListOf()
    private val contactsBackup: ArrayList<Contact> = arrayListOf()
    private val onItemClick = { contact: Contact ->
        if (isStartForResult) {
            setResult(RESULT_OK, Intent().putExtra(KEY_SELECTED_ADDRESS, contact))
            finishAndRemoveTask()
        } else {
            //TODO open details
        }
    }

    private var mAdapter = ContactRecyclerAdapter(contacts, onItemClick, {
        launchActivity<ImportContactsActivity>(KEY_REQUEST_IMPORT_CONTACT)
    })

    private var isStartForResult = false
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddressBookBinding.inflate(layoutInflater)
        setContentView(binding.root)
        onNewIntent(intent)

        binding.inputSearch.apply {
            onTextChanged {
                binding.buttonClear.visibility = if (it?.length ?: 0 == 0) View.GONE else View.VISIBLE
                mAdapter.isShowSyncContactsUI = (it?.length ?: 0) == 0
                searchByName(it.toString())
            }
        }

        binding.buttonClear.click { binding.inputSearch.setText("") }

        binding.addressBookTabs.apply {
            //isVisible = !MozoSDK.isRetailerApp
            setOnCheckedChangeListener { group, _ ->
                group.hideKeyboard()
                loadData()
            }
        }

        binding.addressBookTabUser.setOnCheckedChangeListener { buttonView, isChecked ->
            buttonView.typeface = if (isChecked) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
        binding.addressBookTabStore.setOnCheckedChangeListener { buttonView, isChecked ->
            buttonView.typeface = if (isChecked) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

        binding.listContacts.apply {
            mozoSetup(binding.listContactsRefresh)
            adapter = mAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    binding.addressBookTopBarHover.isSelected = recyclerView.canScrollVertically(-1)
                }
            })
            onLetterScrollListener = {
                try {
                    val position = mAdapter.getSectionPosition(it)
                    (binding.listContacts.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, 0)
                } catch (e: Exception) {

                }
            }
        }

        binding.listContactsRefresh.apply {
            isRefreshing = true
            setOnRefreshListener(::refresh)
        }

        loadData()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        isStartForResult = intent?.getBooleanExtra(FLAG_START_FOR_RESULT, isStartForResult)
                ?: isStartForResult
        binding.addressBookToolbar.apply {
            setTitle(if (isStartForResult) R.string.mozo_address_book_pick_title else R.string.mozo_address_book_title)
            showBackButton(isStartForResult)
        }
    }

    override fun onResume() {
        super.onResume()
        MozoSDK.getInstance().contactViewModel.usersLiveData.observe(this) {
            loadData()
        }
    }

    override fun onPause() {
        MozoSDK.getInstance().contactViewModel.usersLiveData.removeObservers(this)
        super.onPause()
    }

    private fun refresh() {
        if (binding.inputSearch.length() == 0) {
            when (binding.addressBookTabs.checkedRadioButtonId) {
                R.id.address_book_tab_user -> {
                    MozoSDK.getInstance().contactViewModel.fetchUser(this) {
                        binding.listContactsRefresh.isRefreshing = false
                        loadData()
                    }
                }
                R.id.address_book_tab_store -> {
                    MozoSDK.getInstance().contactViewModel.fetchStore(this) {
                        binding.listContactsRefresh.isRefreshing = false
                        loadData()
                    }
                }
            }
        } else
            binding.listContactsRefresh.isRefreshing = false
    }

    private fun loadData() {
        contacts.clear()
        contactsBackup.clear()
        binding.viewEmptyState.gone()

        when (binding.addressBookTabs.checkedRadioButtonId) {
            R.id.address_book_tab_user -> {
                contacts.addAll(MozoSDK.getInstance().contactViewModel.users())
                contactsBackup.addAll(MozoSDK.getInstance().contactViewModel.users())
            }
            R.id.address_book_tab_store -> {
                contacts.addAll(MozoSDK.getInstance().contactViewModel.stores())
                contactsBackup.addAll(MozoSDK.getInstance().contactViewModel.stores())
            }
        }

        binding.listContactsRefresh.isRefreshing = false
        mAdapter.mEmptyView = binding.listContactsEmptyView
        mAdapter.notifyData(true, showEmptyView = true)
    }

    private fun searchByName(name: String) {
        searchJob?.cancel()
        searchJob = GlobalScope.launch {

            delay(250)

            contacts.clear()
            contacts.addAll(contactsBackup.filter {
                (it.name ?: "").contains(name, ignoreCase = true)
            })
            withContext(Dispatchers.Main) {
                if (contacts.isEmpty() && name.isNotEmpty()) binding.viewEmptyState.visible() else binding.viewEmptyState.gone()
                mAdapter.notifyData(true, showEmptyView = false)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK)
            return

        if (requestCode == KEY_REQUEST_IMPORT_CONTACT) {
            refresh()
        }
    }

    companion object {
        private const val FLAG_START_FOR_RESULT = "FLAG_START_FOR_RESULT"

        const val KEY_SELECTED_ADDRESS = "KEY_SELECTED_ADDRESS"

        const val KEY_REQUEST_IMPORT_CONTACT = 0x0069

        fun start(context: Context) {
            Intent(context, AddressBookActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(this)
            }
        }

        fun startForResult(activity: Activity, requestCode: Int) {
            Intent(activity, AddressBookActivity::class.java).apply {
                putExtra(FLAG_START_FOR_RESULT, true)
                activity.startActivityForResult(this, requestCode)
            }
        }

        fun startForResult(fragment: Fragment, requestCode: Int) {
            Intent(fragment.context, AddressBookActivity::class.java).apply {
                putExtra(FLAG_START_FOR_RESULT, true)
                fragment.startActivityForResult(this, requestCode)
            }
        }
    }
}