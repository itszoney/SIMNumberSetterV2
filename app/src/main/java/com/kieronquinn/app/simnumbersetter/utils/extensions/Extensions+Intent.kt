package com.kieronquinn.app.simnumbersetter.utils.extensions

import android.app.PendingIntent
import android.content.Context
import android.content.Intent

private const val KEY_PENDING_INTENT = "SECURITY_PENDING_INTENT"
private const val PENDING_INTENT_REQUEST_CODE = 1001

fun Intent.applySecurity(context: Context) {
    putExtra(
        KEY_PENDING_INTENT, PendingIntent.getActivity(
        context,
        PENDING_INTENT_REQUEST_CODE,
        Intent(),
        PendingIntent.FLAG_IMMUTABLE
    ))
}

// Runs inside com.android.phone on API 26+, so the type-safe API 33 overload can't be used
@Suppress("DEPRECATION")
fun Intent.checkSecurity(moduleUid: Int): Boolean {
    val pendingIntent = getParcelableExtra<PendingIntent>(KEY_PENDING_INTENT) ?: return false
    return pendingIntent.creatorUid == moduleUid
}
