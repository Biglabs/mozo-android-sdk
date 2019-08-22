package io.mozocoin.sdk.contact

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.mozocoin.sdk.R
import io.mozocoin.sdk.ui.BaseActivity
import io.mozocoin.sdk.utils.click
import io.mozocoin.sdk.utils.logAsError
import kotlinx.android.synthetic.main.activity_import_contacts.*

internal class ImportContactsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import_contacts)
        setSupportActionBar(toolbar)

        button_update?.click {
            fetchContacts()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            FLAG_PERMISSIONS_REQUEST_READ_CONTACTS -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    fetchContacts()
                }
            }
        }
    }

    private fun fetchContacts() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            /*
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS), FLAG_PERMISSIONS_REQUEST_READ_CONTACTS)
            }
            */
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS), FLAG_PERMISSIONS_REQUEST_READ_CONTACTS)
            return
        }

        val phones = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null)
        while (phones!!.moveToNext()) {
            val name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
            val phone = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
            ("Name : $name, phone: $phone").logAsError()
        }
        phones.close()
    }

    companion object {
        private const val FLAG_PERMISSIONS_REQUEST_READ_CONTACTS = 1901

    }
}