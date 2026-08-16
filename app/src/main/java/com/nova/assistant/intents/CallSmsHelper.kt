package com.nova.assistant.intents

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telephony.SmsManager

class CallSmsHelper(private val context: Context) {

    private fun hasPermission(permission: String): Boolean {
        return androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            permission
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    fun findPhoneNumberByName(name: String): String? {
        if (!hasPermission(android.Manifest.permission.READ_CONTACTS)) {
            return null
        }

        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$name%")

        val cursor = context.contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (numberIndex != -1) {
                    val rawNumber = it.getString(numberIndex)
                    return rawNumber.replace(" ", "").replace("-", "")
                }
            }
        }
        return null
    }

    fun makeCall(input: String): String {
        val cleanInput = input.lowercase()
        val target = extractTarget(cleanInput, listOf("call karo", "ko call lagao", "call", "ko call karo"))

        if (target.isEmpty()) {
            return "Kisko call karna hai?"
        }

        val phoneNumber = findPhoneNumberByName(target) ?: target

        return if (hasPermission(android.Manifest.permission.CALL_PHONE)) {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            "$target ko call lagaya ja raha hai."
        } else {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            "Call permission nahi hai, dialer khol diya hai."
        }
    }

    fun sendSms(input: String): String {
        val cleanInput = input.lowercase()
        var target = ""
        var message = ""

        if (cleanInput.contains("ko msg karo") || cleanInput.contains("ko message karo")) {
            val parts = if (cleanInput.contains("ko msg karo")) {
                cleanInput.split("ko msg karo")
            } else {
                cleanInput.split("ko message karo")
            }
            target = parts[0].trim()
            message = if (parts.size > 1) parts[1].trim() else ""
        }

        if (target.isEmpty()) {
            return "Kisko message bhejna hai?"
        }

        val phoneNumber = findPhoneNumberByName(target) ?: target

        return if (hasPermission(android.Manifest.permission.SEND_SMS)) {
            try {
                val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                "$target ko message bhej diya gaya hai."
            } catch (e: Exception) {
                "SMS bhejne me error aaya."
            }
        } else {
            "$target ka contact number nahi mila ya SMS permission nahi hai."
        }
    }

    private fun extractTarget(input: String, keywords: List<String>): String {
        var clean = input.lowercase()
        keywords.forEach {
            clean = clean.replace(it, "")
        }
        return clean.trim()
    }
}
