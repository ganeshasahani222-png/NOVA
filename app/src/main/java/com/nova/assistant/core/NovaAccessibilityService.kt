package com.nova.assistant.core

/**
 * NOT REGISTERED IN THE MANIFEST YET — intentionally left out.
 *
 * AccessibilityService is a powerful, sensitive API (it can read
 * screen content and perform gestures across other apps). Add it only
 * if a real feature needs it, and only after:
 *
 *   1. Extending android.accessibilityservice.AccessibilityService
 *      here with a narrowly-scoped implementation.
 *   2. Declaring it in AndroidManifest.xml with
 *      android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
 *      and a res/xml/accessibility_service_config.xml that limits
 *      canRetrieveWindowContent / eventTypes / packageNames to exactly
 *      what's needed.
 *   3. Clearly explaining to the user, in-app, what the service does
 *      before sending them to Settings > Accessibility to enable it —
 *      Android does not allow enabling it programmatically.
 *
 * This file is a documentation/placeholder marker so the module
 * boundary exists in the project structure ahead of implementation.
 */
object NovaAccessibilityServicePlaceholder
