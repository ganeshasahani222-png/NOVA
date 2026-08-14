package com.nova.assistant.intents

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings

/**
 * Central place for the standard, publicly-documented Android Intents
 * Nova is allowed to fire on the user's behalf (opening apps, dialing,
 * launching the camera, opening a web page, jumping to system settings,
 * etc). Everything here uses public Intent actions — no private APIs,
 * no reflection, no permissions beyond what's declared in the manifest.
 *
 * Voice/chat command parsing should route recognized "do X" requests
 * to a method here rather than constructing Intents inline elsewhere,
 * so this file stays the single audit point for "what can Nova
 * actually trigger on the device".
 */
class SystemActionDispatcher(private val context: Context) {

    sealed interface ActionResult {
        object Launched : ActionResult
        data class Unavailable(val reason: String) : ActionResult
    }

    fun openUrl(url: String): ActionResult = launch(
        Intent(Intent.ACTION_VIEW, Uri.parse(url)).addNewTaskFlag()
    )

    /** Opens the dialer with the number pre-filled; does NOT place the call automatically. */
    fun dialNumber(phoneNumber: String): ActionResult = launch(
        Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")).addNewTaskFlag()
    )

    fun openCamera(): ActionResult = launch(
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).addNewTaskFlag()
    )

    fun openAppSettings(): ActionResult = launch(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }.addNewTaskFlag()
    )

    fun shareText(text: String): ActionResult = launch(
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }.let { Intent.createChooser(it, null) }.addNewTaskFlag()
    )

    private fun Intent.addNewTaskFlag(): Intent =
        apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

    private fun launch(intent: Intent): ActionResult = try {
        context.startActivity(intent)
        ActionResult.Launched
    } catch (e: ActivityNotFoundException) {
        ActionResult.Unavailable("No app available to handle this action.")
    }
}
