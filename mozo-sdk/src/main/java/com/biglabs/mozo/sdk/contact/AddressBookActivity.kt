package com.biglabs.mozo.sdk.contact

import android.app.Activity
import android.arch.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import com.biglabs.mozo.sdk.MozoSDK
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.common.Models
import com.biglabs.mozo.sdk.ui.BaseActivity
import com.biglabs.mozo.sdk.utils.*
import kotlinx.android.synthetic.main.view_address_book.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch

internal class AddressBookActivity : BaseActivity() {

    private val contacts: ArrayList<Models.Contact> = arrayListOf()
    private val contactsBackup: ArrayList<Models.Contact> = arrayListOf()
    private val onItemClick = { contact: Models.Contact ->
        if (isStartForResult) {
            val result = Intent()
            result.putExtra(KEY_SELECTED_ADDRESS, contact)

            setResult(RESULT_OK, result)
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

        setContentView(R.layout.view_address_book)
        onNewIntent(intent)

        input_search?.apply {
            onTextChanged {
                button_clear.visibility = if (it?.length ?: 0 == 0) View.GONE else View.VISIBLE
                searchByName(it.toString())
            }
        }

        button_clear.click { input_search.setText("") }

        list_contacts?.apply {
            setHasFixedSize(true)
            itemAnimator = DefaultItemAnimator()
            adapter = mAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                    address_book_top_bar_hover.isSelected = list_contacts.canScrollVertically(-1)
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
                if (input_search.length() == 0)
                    MozoSDK.getInstance().contactViewModel.fetchData()
                else
                    isRefreshing = false
            }
        }

        MozoSDK.getInstance().contactViewModel.run {
            contactsLiveData.observeForever(contactsObserver)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        isStartForResult = intent?.getBooleanExtra(FLAG_START_FOR_RESULT, isStartForResult) ?: isStartForResult
        address_book_toolbar?.apply {
            setTitle(if (isStartForResult) R.string.mozo_address_book_pick_title else R.string.mozo_address_book_title)
            showBackButton(isStartForResult)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        MozoSDK.getInstance().contactViewModel.run {
            contactsLiveData.removeObserver(contactsObserver)
        }
    }

    private val contactsObserver = Observer<List<Models.Contact>> {
        it?.run {
            contacts.clear()
            contacts.addAll(this)

            contactsBackup.clear()
            contactsBackup.addAll(this)

            list_contacts_refresh.isRefreshing = false
            mAdapter.notifyData()
        }
    }

    private fun searchByName(name: String) {
        searchJob?.cancel()
        searchJob = launch {

            delay(250)

            contacts.clear()
            contacts.addAll(contactsBackup.filter {
                (it.name ?: "").contains(name, ignoreCase = true)
            })
            launch(UI) {
                if (contacts.isEmpty()) view_empty_state.visible() else view_empty_state.gone()
                mAdapter.notifyData(name.isNotEmpty())
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
    }
}