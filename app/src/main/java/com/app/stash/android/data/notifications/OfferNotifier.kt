package com.app.stash.android.data.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.app.stash.android.MainActivity
import com.app.stash.android.R
import com.app.stash.android.domain.model.Offer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shows a local notification when a nearby place matches an unactivated offer.
 *
 * v1 is foreground-only: triggered from [com.app.stash.android.ui.rewards.RewardsMapViewModel]
 * when the user opens / refreshes the map. No background geofencing, no
 * persistent location tracking — keeps the privacy posture clean. A
 * future v1.4.x can add proper [android.location.LocationManager] geofences
 * once the user grants ACCESS_BACKGROUND_LOCATION.
 */
@Singleton
class OfferNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val notificationManager: NotificationManagerCompat =
        NotificationManagerCompat.from(context)

    init {
        ensureChannel()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Card-linked offers",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Reminders to activate Amex / Chase / Citi offers when you're near a matching merchant."
                setShowBadge(true)
            }
            val sysManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            sysManager.createNotificationChannel(channel)
        }
    }

    /** Fire a notification for the highest-value offer matching a nearby place. */
    @SuppressLint("MissingPermission") // guarded by hasPermission() above
    fun notifyOfferNearby(offer: Offer, placeName: String) {
        if (!hasPermission()) return

        // Throttle: don't re-fire the same offer for the same place within the window.
        val key = "${offer.id}:${placeName.lowercase()}"
        val now = System.currentTimeMillis()
        val last = recentNotifications[key]
        if (last != null && now - last < THROTTLE_MS) return
        recentNotifications[key] = now

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapPending = PendingIntent.getActivity(
            context,
            offer.id.toInt(),
            tapIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Offer at $placeName")
            .setContentText("${offer.issuer} · ${offer.shortLabel()} — tap to activate")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "${offer.issuer} offer: ${offer.shortLabel()}\n" +
                        (offer.description ?: "Activate it in your issuer's app, then swipe to earn.")
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(tapPending)

        // Add an "Open issuer" action if we have a deep link.
        offer.deepLinkUri?.let { uri ->
            val openIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val openPending = PendingIntent.getActivity(
                context,
                offer.id.toInt() + 10_000,
                openIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            builder.addAction(
                NotificationCompat.Action.Builder(0, "Open ${offer.issuer}", openPending).build()
            )
        }

        notificationManager.notify(NOTIFICATION_ID_BASE + offer.id.toInt(), builder.build())
    }

    private fun hasPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val CHANNEL_ID = "offers_nearby"
        private const val NOTIFICATION_ID_BASE = 4200
        // 30 min throttle per (offer, place) — avoid re-spamming on map refresh.
        private const val THROTTLE_MS = 30L * 60 * 1000

        // In-memory throttle table. Cleared on process death; that's fine since
        // notifications are user-facing reminders, not durable signals.
        private val recentNotifications = mutableMapOf<String, Long>()
    }
}
