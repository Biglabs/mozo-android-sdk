package io.mozocoin.sdk.contact

import android.Manifest.permission.READ_CONTACTS
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.ContactsContract.Contacts.DISPLAY_NAME
import android.provider.ContactsContract.Contacts._ID
import android.provider.ContactsContract.Data.CONTACT_ID
import android.provider.ContactsContract.Data.DATA1
import android.provider.Settings
import android.util.Patterns
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.model.ContactInfoDTO
import io.mozocoin.sdk.common.model.ImportContactRequestDTO
import io.mozocoin.sdk.common.service.MozoAPIsService
import io.mozocoin.sdk.databinding.ActivityImportContactsBinding
import io.mozocoin.sdk.ui.BaseActivity
import io.mozocoin.sdk.ui.dialog.MessageDialog
import io.mozocoin.sdk.utils.Support
import io.mozocoin.sdk.utils.click
import io.mozocoin.sdk.utils.gone
import io.mozocoin.sdk.utils.safe
import kotlinx.coroutines.*

internal class ImportContactsActivity : BaseActivity() {

    private lateinit var binding: ActivityImportContactsBinding
    private val apiService by lazy { MozoAPIsService.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImportContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkingProcess()

        binding.buttonUpdate.click {
            fetchContacts()
        }
    }

    private fun checkingProcess() {
        apiService.checkingProcess(this, ::checkingProcess) { responseDTO, _ ->
            updateUIState(responseDTO?.updatedAt, responseDTO?.currentStatus)
        }
    }

    private fun fetchContacts() {
        if (ContextCompat.checkSelfPermission(this,
                        READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return requestReadContact()
        }

        updateUIState(status = KEY_IMPORT_PROCESSING)

        getPhones {
            if (it.isNullOrEmpty()) {
                binding.loUpdating.gone()
                MessageDialog.show(this, R.string.mozo_address_msg_no_contact)
                return@getPhones
            }

            val dto = ImportContactRequestDTO()
            dto.contactInfos = it

            val timeBeforeRequest = System.currentTimeMillis()
            apiService.importContacts(this, dto, ::fetchContacts) { _, _ ->
                setResult(Activity.RESULT_OK)
                //prevent response too fast
                val waitingTime = System.currentTimeMillis() - timeBeforeRequest

                MainScope().launch {
                    delay(if (waitingTime < 2000) 2000L else 0L)
                    Toast.makeText(
                            this@ImportContactsActivity,
                            R.string.mozo_address_msg_import_success,
                            Toast.LENGTH_SHORT
                    ).show()

                    checkingProcess()
                }
            }
        }
    }

    private fun updateUIState(time: Long? = 0L, status: String? = null) {
        val isProcessing = status == KEY_IMPORT_PROCESSING
        binding.loUpdating.isVisible = isProcessing

        if (!isProcessing) {
            binding.importContactsLastTime.text = if (time == null)
                getString(R.string.mozo_address_msg_not_import)
            else Support.getDisplayDate(
                    this,
                    time * 1000,
                    getString(R.string.mozo_contact_format_date_time)
            )
        }
    }

    private fun getPhones(completion: (MutableList<ContactInfoDTO>) -> Unit) = MozoSDK.scope.launch {
        val phonesCursor = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(_ID, DISPLAY_NAME),
                null,
                null,
                null
        )

        val lstContacts = mutableListOf<ContactInfoDTO>()
        while (phonesCursor?.moveToNext() == true) {
            val contactID = phonesCursor.getString(/* _ID */0)

            val pCursor = contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    arrayOf(DATA1),
                    "$CONTACT_ID = ?",
                    arrayOf(contactID),
                    null
            )

            val lstPhones = mutableListOf<String>()
            while (pCursor?.moveToNext() == true) {
                pCursor.getString(0)?.let {
                    if (Patterns.PHONE.matcher(it).matches())
                        lstPhones.add(it)
                }
            }

            val contactName = phonesCursor.getString(/* DISPLAY_NAME */1).safe()
            if (lstPhones.isNotEmpty() && !lstPhones.contains(contactName)) {
                lstContacts.add(ContactInfoDTO().apply {
                    name = contactName
                    phoneNums = lstPhones
                })
            }

            pCursor?.close()
        }

        phonesCursor?.close()

        withContext(Dispatchers.Main) {
            completion.invoke(lstContacts)
        }
    }

    private fun requestReadContact() {
        ActivityCompat.requestPermissions(
                this,
                arrayOf(READ_CONTACTS),
                FLAG_PERMISSIONS_REQUEST_READ_CONTACTS
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if ((requestCode == FLAG_PERMISSIONS_REQUEST_READ_CONTACTS
                        && grantResults.isNotEmpty()
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
            fetchContacts()
        } else
            requirePermissionAgain()
    }

    private fun requirePermissionAgain() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, READ_CONTACTS))
            showRequestPermissionRationale()
        else showAppPermissionSettings()
    }

    private fun showRequestPermissionRationale() {
        MaterialAlertDialogBuilder(this, R.style.PermissionTheme)
                .setTitle(R.string.mozo_address_require_permission_title)
                .setMessage(R.string.mozo_address_require_permission)
                .setNegativeButton(R.string.mozo_button_cancel, null)
                .setPositiveButton(R.string.mozo_button_ok) { _, _ ->
                    requestReadContact()
                }
                .show()
    }

    private fun showAppPermissionSettings() {
        MaterialAlertDialogBuilder(this, R.style.PermissionTheme)
                .setTitle(R.string.mozo_address_require_permission_force_title)
                .setMessage(R.string.mozo_address_require_permission_force)
                .setNegativeButton(R.string.mozo_button_ok, null)
                .setPositiveButton(R.string.mozo_change_settings_title) { _, _ ->
                    Intent().run {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                        data = Uri.parse("package:$packageName")
                        startActivityForResult(this, FLAG_PERMISSIONS_REQUEST_READ_CONTACTS)
                    }
                }
                .show()
    }

    companion object {
        private const val FLAG_PERMISSIONS_REQUEST_READ_CONTACTS = 1901
        private const val KEY_IMPORT_PROCESSING = "PROCESSING"
    }
}
