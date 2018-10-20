package com.biglabs.mozo.sdk.contact

import android.app.Activity
import android.arch.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.DefaultItemAnimator
import android.view.View
import com.biglabs.mozo.sdk.MozoSDK
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.common.Models
import com.biglabs.mozo.sdk.ui.BaseActivity
import com.biglabs.mozo.sdk.utils.click
import com.biglabs.mozo.sdk.utils.mozoSetup
import com.biglabs.mozo.sdk.utils.onTextChanged
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

        input_search.onTextChanged {
            button_clear.visibility = if (it?.length ?: 0 == 0) View.GONE else View.VISIBLE
            searchByName(it.toString())
        }

        button_clear.click { input_search.setText("") }

        list_contacts.setHasFixedSize(true)
        list_contacts.itemAnimator = DefaultItemAnimator()
        list_contacts.adapter = mAdapter

        list_contacts_refresh?.apply {
            mozoSetup()
            isRefreshing = true
            setOnRefreshListener { MozoSDK.contactViewModel?.fetchData() }
        }

        MozoSDK.contactViewModel?.run {
            contactsLiveData.observeForever(contactsObserver)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        isStartForResult = intent?.getBooleanExtra(FLAG_START_FOR_RESULT, isStartForResult) ?: isStartForResult
    }

    override fun onDestroy() {
        super.onDestroy()
        MozoSDK.contactViewModel?.run {
            contactsLiveData.removeObserver(contactsObserver)
        }
    }

    private val contactsObserver = Observer<List<Models.Contact>> {
        it?.run {
            contacts.clear()
            contacts.addAll(this)

            contactsBackup.clear()
            contactsBackup.addAll(this)

            launch(UI) {
                list_contacts_refresh.isRefreshing = false
                mAdapter.notifyDataSetChanged()
            }
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
                mAdapter.notifyDataSetChanged()
            }
        }
    }

    companion object {
        private const val FLAG_START_FOR_RESULT = "FLAG_START_FOR_RESULT"
        const val KEY_SELECTED_ADDRESS = "KEY_SELECTED_ADDRESS"

        fun start(context: Context) {
            Intent(context, AddressBookActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
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