package com.nova.assistant.admin

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * Thin, explicit wrapper around Android's official DevicePolicyManager
 * / DeviceAdmin APIs (https://developer.android.com/guide/topics/admin/device-admin).
 *
 * Deliberately narrow: it exposes only the handful of actions Nova's
 * declared policy set actually supports (see device_admin_policies.xml).
 * It does NOT provide, and cannot be extended to provide, arbitrary or
 * unrestricted control of the device — every capability here is gated
 * by both (a) the policy being declared in XML and (b) the user having
 * explicitly granted device admin status via system Settings.
 *
 * Note: Google Play policy restricts publishing consumer apps that use
 * the Device Admin API for anything beyond a narrow set of approved use
 * cases; for most enterprise/parental-control scenarios today Google
 * steers developers toward the Android Enterprise / Device Policy
 * Controller (DPC) APIs instead. Review current Play policy before
 * shipping this feature.
 */
class DeviceAdminManager(private val context: Context) {

    private val devicePolicyManager: DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    private val adminComponent: ComponentName =
        ComponentName(context, NovaDeviceAdminReceiver::class.java)

    val isAdminActive: Boolean
        get() = devicePolicyManager.isAdminActive(adminComponent)

    /**
     * Builds the system intent that prompts the user to grant device
     * admin status. Must be launched with startActivity/startActivityForResult
     * from an Activity — Nova cannot grant this permission itself.
     */
    fun buildRequestAdminIntent(explanation: String): Intent =
        Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, explanation)
        }

    /**
     * Locks the screen immediately. Requires isAdminActive == true and
     * the force-lock policy to be declared (it is, by default). Maps
     * directly to a documented DevicePolicyManager call — no custom
     * or undocumented behavior.
     */
    fun lockScreenIfPermitted(): Boolean {
        if (!isAdminActive) return false
        devicePolicyManager.lockNow()
        return true
    }

    fun removeAdmin() {
        devicePolicyManager.removeActiveAdmin(adminComponent)
    }
}
