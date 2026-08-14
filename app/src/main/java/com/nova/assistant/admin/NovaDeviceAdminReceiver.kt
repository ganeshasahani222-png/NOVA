package com.nova.assistant.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Receives device admin lifecycle callbacks from the system.
 *
 * IMPORTANT: This class only ever gets called for the specific
 * policies declared in res/xml/device_admin_policies.xml (currently:
 * limit-password, watch-login, force-lock). It cannot do anything
 * outside that declared policy set — Android enforces this, not this
 * class. Activation always requires explicit user action in system
 * Settings; Nova cannot silently become a device admin.
 */
class NovaDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, "Nova device admin enabled", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(context, "Nova device admin disabled", Toast.LENGTH_SHORT).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "Disabling device admin will stop Nova from performing screen-lock actions on request."
    }
}
