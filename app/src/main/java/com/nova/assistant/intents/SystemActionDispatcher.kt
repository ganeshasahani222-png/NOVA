package com.nova.assistant.intents

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.provider.AlarmClock

/**
 * Central place for the standard, publicly-documented Android Intents
 * and system-service calls Nova is allowed to trigger on the user's
 * behalf (opening apps, dialing, launching the camera, opening a web
 * page, jumping to system settings, adjusting volume, etc). Everything
 * here uses public Android APIs — no private APIs, no reflection, no
 * permissions beyond what's declared in the manifest.
 *
 * Note on WiFi/Bluetooth: since Android 10 (API 29), apps can no
 * longer silently toggle WiFi or Bluetooth on/off — Android requires
 * the user to do this themselves via Settings or Quick Settings, for
 * privacy reasons. This class opens the relevant settings panel
 * instead, which is the closest permitted behavior.
 */
class SystemActionDispatcher(private val context: Context) {

    sealed interface ActionResult {
        object Launched : ActionResult
        data class Unavailable(val reason: String) : ActionResult
    }

    fun openUrl(url: String): ActionResult = launch(
        Intent(Intent.ACTION_VIEW, Uri.parse(url)).addNewTaskFlag()
    )

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

    // ---- Volume control (works directly, no extra permission needed) ----

    fun increaseVolume(): Boolean = adjustVolume(AudioManager.ADJUST_RAISE)

    fun decreaseVolume(): Boolean = adjustVolume(AudioManager.ADJUST_LOWER)

    fun muteVolume(): Boolean = adjustVolume(AudioManager.ADJUST_MUTE)

    fun setVolumePercent(percent: Int): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return false
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val target = (max * (percent.coerceIn(0, 100) / 100f)).toInt()
        return try {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
            true
        } catch (e: SecurityException) {
            false
        }
    }

    private fun adjustVolume(direction: Int): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return false
        return try {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
            true
        } catch (e: SecurityException) {
            false
        }
    }

    // ---- Brightness (opens the system brightness settings screen —
    // apps cannot silently change system-wide brightness without a
    // special "Modify System Settings" permission the user must grant
    // manually in Settings, so we route there safely instead) ----

    fun openDisplaySettings(): ActionResult = launch(
        Intent(Settings.ACTION_DISPLAY_SETTINGS).addNewTaskFlag()
    )

    // ---- WiFi / Bluetooth (Android 10+ requires user interaction via
    // Settings panel; apps cannot silently flip these anymore) ----

    fun openWifiSettings(): ActionResult = launch(
        Intent(Settings.ACTION_WIFI_SETTINGS).addNewTaskFlag()
    )

    fun openBluetoothSettings(): ActionResult = launch(
        Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addNewTaskFlag()
    )

    // ---- Alarms (delegates to the Clock app via public intents) ----

    fun openAlarmList(): Boolean = launchRaw(
        Intent(AlarmClock.ACTION_SHOW_ALARMS).addNewTaskFlag()
    )

    private fun launchRaw(intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }

    private fun Intent.addNewTaskFlag(): Intent =
        apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

    private fun launch(intent: Intent): ActionResult = try {
        context.startActivity(intent)
        ActionResult.Launched
    } catch (e: ActivityNotFoundException) {
        ActionResult.Unavailable("No app available to handle this action.")
    }
}
