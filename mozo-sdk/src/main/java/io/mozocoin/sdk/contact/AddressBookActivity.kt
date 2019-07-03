package io.mozocoin.sdk.contact

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import androidx.recyclerview.widget.DefaultItemAnimator
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.model.Contact
import io.mozocoin.sdk.ui.BaseActivity
import io.mozocoin.sdk.utils.*
import kotlinx.android.synthetic.main.activity_address_book.*
import kotlinx.coroutines.*

internal class AddressBookActivity : BaseActivity() {

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
    private var mAdapter = ContactRecyclerAdapter(contacts, onItemClick)

    private var isStartForResult = false
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_address_book)
        onNewIntent(intent)

        input_search?.apply {
            onTextChanged {
                button_clear.visibility = if (it?.length ?: 0 == 0) View.GONE else View.VISIBLE
                searchByName(it.toString())
            }
        }

        button_clear.click { input_search.setText("") }

        address_book_tabs?.apply {
            //isVisible = !MozoSDK.isRetailerApp
            setOnCheckedChangeListener { group, _ ->
                group.hideKeyboard()
                loadData()
            }
        }

        address_book_tab_user.setOnCheckedChangeListener { buttonView, isChecked ->
            buttonView.typeface = if (isChecked) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
        address_book_tab_store.setOnCheckedChangeListener { buttonView, isChecked ->
            buttonView.typeface = if (isChecked) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

        list_contacts?.apply {
            setHasFixedSize(true)
            itemAnimator = DefaultItemAnimator()
            adapter = mAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    address_book_top_bar_hover.isSelected = recyclerView.canScrollVertically(-1)
                }
            })
            onLetterScrollListener = {
                try {
                    val position = mAdapter.getSectionPosition(it)
                    (list_contacts.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, 0)
                } catch (e: Exception) {

                }
            }
        }

        list_contacts_refresh?.apply {
            mozoSetup()
            isRefreshing = true
            setOnRefreshListener {
                if (input_search.length() == 0) {
                    when (address_book_tabs?.checkedRadioButtonId ?: R.id.address_book_tab_user) {
                        R.id.address_book_tab_user -> {
                            MozoSDK.getInstance().contactViewModel.fetchUser(context) {
                                isRefreshing = false
                                loadData()
                            }
                        }
                        R.id.address_book_tab_store -> {
                            MozoSDK.getInstance().contactViewModel.fetchStore(context) {
                                isRefreshing = false
                                loadData()
                            }
                        }
                    }
                } else
                    isRefreshing = false
            }
        }

        loadData()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        isStartForResult = intent?.getBooleanExtra(FLAG_START_FOR_RESULT, isStartForResult)
                ?: isStartForResult
        address_book_toolbar?.apply {
            setTitle(if (isStartForResult) R.string.mozo_address_book_pick_title else R.string.mozo_address_book_title)
            showBackButton(isStartForResult)
        }
    }

    private fun loadData() {
        contacts.clear()
        contactsBackup.clear()
        view_empty_state.gone()

        when (address_book_tabs?.checkedRadioButtonId ?: R.id.address_book_tab_user) {
            R.id.address_book_tab_user -> {
                contacts.addAll(MozoSDK.getInstance().contactViewModel.users())
                contactsBackup.addAll(MozoSDK.getInstance().contactViewModel.users())
            }
            R.id.address_book_tab_store -> {
                contacts.addAll(MozoSDK.getInstance().contactViewModel.stores())
                contactsBackup.addAll(MozoSDK.getInstance().contactViewModel.stores())
            }
        }

        list_contacts_refresh.isRefreshing = false
        mAdapter.mEmptyView = list_contacts_empty_view
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
                if (contacts.isEmpty()) view_empty_state.visible() else view_empty_state.gone()
                mAdapter.notifyData(true, showEmptyView = false)
            }
        }
    }

    companion object {
        private const val FLAG_START_FOR_RESULT = "FLAG_START_FOR_RESULT"
        const val KEY_SELECTED_ADDRESS = "KEY_SELECTED_ADDRESS"

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