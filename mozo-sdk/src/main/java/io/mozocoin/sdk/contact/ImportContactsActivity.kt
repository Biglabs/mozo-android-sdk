package io.mozocoin.sdk.contact

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.model.ContactInfoDTO
import io.mozocoin.sdk.common.model.ImportContactRequestDTO
import io.mozocoin.sdk.common.service.MozoAPIsService
import io.mozocoin.sdk.ui.BaseActivity
import io.mozocoin.sdk.utils.Support
import io.mozocoin.sdk.utils.click
import kotlinx.android.synthetic.main.activity_import_contacts.*
import kotlinx.coroutines.*

internal class ImportContactsActivity : BaseActivity() {

    private val apiService by lazy { MozoAPIsService.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import_contacts)
        setSupportActionBar(toolbar)

        checkingProcess()

        button_update?.click {
            fetchContacts()
        }

    }

    private fun checkingProcess() {
        apiService.checkingProcess(this, ::checkingProcess) { responseDTO, _ ->
            updateUIState(responseDTO?.updatedAt, responseDTO?.currentStatus)
        }
    }

    private fun fetchContacts() {
        if (!permissionGranted()) {
            return
        }

        updateUIState(status = "PROCESSING")

        val dto = ImportContactRequestDTO()
        dto.contactInfos = getPhones()

        val timeBeforeRequest = System.currentTimeMillis()
        apiService.importContacts(this, dto, ::fetchContacts) { _, _ ->
            setResult(Activity.RESULT_OK)
            //prevent response too fast
            val waitingTime =
                if (System.currentTimeMillis() - timeBeforeRequest < 3000) 3000L else 0L

            GlobalScope.launch {
                delay(waitingTime)
                withContext(Dispatchers.Main) {
                    updateUIState(System.currentTimeMillis()/1000)
                }
            }
        }
    }

    private fun updateUIState(time: Long? = 0L, status: String? = null) {
        val isProcessing = status == "PROCESSING"
        lo_updating?.isVisible = isProcessing

        if (!isProcessing) {
            import_contacts_last_time?.text = if (time == null)
                "not yet imported"
            else Support.getDisplayDate(
                this,
                time * 1000,
                getString(R.string.mozo_contact_format_date_time)
            )
        }
    }

    private fun getPhones(): MutableList<ContactInfoDTO>? {
        val phonesCursor = contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null,
            null,
            null
        )

        val lstContacts = mutableListOf<ContactInfoDTO>()
        while (phonesCursor?.moveToNext() == true) {
            val contactName = phonesCursor.getString(0)
            val phone = phonesCursor.getString(1)

            lstContacts.add(ContactInfoDTO().apply {
                name = contactName
                phoneNums = listOf(phone)
            })
        }

        phonesCursor?.close()
        return lstContacts
    }

    private fun permissionGranted(): Boolean {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true
        }

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_CONTACTS),
            FLAG_PERMISSIONS_REQUEST_READ_CONTACTS
        )
        return false
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
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.READ_CONTACTS))
            showRequestPermissionRationale()
        else showAppPermissionSettings()
    }

    private fun showRequestPermissionRationale() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.mozo_address_require_permission_title)
            .setMessage(R.string.mozo_address_require_permission)
            .setNegativeButton(R.string.mozo_button_cancel, null)
            .setPositiveButton(R.string.mozo_button_ok) { _, _ ->
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_CONTACTS),
                    FLAG_PERMISSIONS_REQUEST_READ_CONTACTS
                )
            }
            .show()
    }

    private fun showAppPermissionSettings() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.mozo_address_require_permission_force_title)
            .setMessage(R.string.mozo_address_require_permission_force)
            .setNegativeButton(R.string.mozo_button_ok, null)
            .setPositiveButton(R.string.mozo_settings_title) { _, _ ->
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

    }
}