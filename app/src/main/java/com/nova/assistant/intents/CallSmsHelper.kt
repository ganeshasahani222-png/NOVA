package com.nova.assistant.intents

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import android.telecom.TelecomManager
import android.os.Build

/**
 * Places calls and sends SMS directly, without opening the dialer or
 * messaging app first. Requires CALL_PHONE, SEND_SMS, and
 * READ_CONTACTS permissions to already be granted — callers should
 * check this before invoking these methods.
 */
class CallSmsHelper(private val context: Context) {

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    /**
     * Looks up a contact's phone number by name. Returns null if no
     * match is found or contacts permission isn't granted.
     */
    fun findPhoneNumberByName(name: String): String? {
        if (!hasPermission(Manifest.permission.READ_CONTACTS)) return null

        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$name%")

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (numberIndex >= 0) return cursor.getString(numberIndex)
            }
        } finally {
            cursor?.close()
        }
        return null
    }

    /**
     * Places a call directly to the given number. Returns true if the
     * call was initiated, false if permission is missing or it failed.
     */
    fun callNumber(phoneNumber: String): Boolean {
        if (!hasPermission(Manifest.permission.CALL_PHONE)) return false

        return try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            val uri = Uri.parse("tel:$phoneNumber")
            if (telecomManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (hasPermission(Manifest.permission.CALL_PHONE)) {
                    @Suppress("MissingPermission")
                    telecomManager.placeCall(uri, null)
                    true
                } else {
                    false
                }
            } else {
                false
            }
        } catch (e: SecurityException) {
            false
        }
    }

    /**
     * Sends an SMS directly to the given number. Returns true if the
     * message was handed to the system for sending, false if
     * permission is missing or it failed.
     */
    fun sendSms(phoneNumber: String, message: String): Boolean {
        if (!hasPermission(Manifest.permission.SEND_SMS)) return false

        return try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            true
        } catch (e: Exception) {
            false
        }
    }
}
