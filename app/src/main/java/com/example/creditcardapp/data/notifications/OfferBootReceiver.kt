package com.example.creditcardapp.data.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.creditcardapp.work.BootRecoveryWorker

/**
 * Receives ACTION_BOOT_COMPLETED and ACTION_MY_PACKAGE_REPLACED (which fires on
 * app upgrade). Both events clear the system geofence list, so we enqueue a
 * one-shot worker to re-register offer geofences using last-known location.
 */
class OfferBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != "android.intent.action.LOCKED_BOOT_COMPLETED"
        ) return

        val work = OneTimeWorkRequestBuilder<BootRecoveryWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            BootRecoveryWorker.NAME,
            ExistingWorkPolicy.KEEP,
            work,
        )
    }
}
