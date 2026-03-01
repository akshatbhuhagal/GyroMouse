package com.axat.gyromouse.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.axat.gyromouse.MainActivity
import com.axat.gyromouse.R
import com.axat.gyromouse.data.bluetooth.BleHidManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Foreground service that keeps the BLE HID connection alive when the app is in the background.
 * Displays a persistent notification with connection status and a disconnect action.
 */
@AndroidEntryPoint
class BluetoothHidService : Service() {

    companion object {
        private const val TAG = "BluetoothHidService"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_DISCONNECT = "com.axat.gyromouse.ACTION_DISCONNECT"
    }

    @Inject
    lateinit var bleHidManager: BleHidManager

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_DISCONNECT) {
            Log.d(TAG, "Disconnect action received")
            bleHidManager.disconnect()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        // Build notification
        val deviceName = try {
            bleHidManager.getConnectedDevice()?.name ?: "Unknown"
        } catch (e: SecurityException) {
            "Unknown"
        }

        val notification = buildNotification(deviceName)
        startForeground(NOTIFICATION_ID, notification)

        return START_STICKY
    }

    private fun buildNotification(deviceName: String): Notification {
        val channelId = getString(R.string.notification_channel_id)

        // Open app intent
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Disconnect intent
        val disconnectIntent = PendingIntent.getService(
            this, 1,
            Intent(this, BluetoothHidService::class.java).apply {
                action = ACTION_DISCONNECT
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text, deviceName))
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.notification_disconnect_action),
                disconnectIntent
            )
            .build()
    }

    private fun createNotificationChannel() {
        val channelId = getString(R.string.notification_channel_id)
        val channel = NotificationChannel(
            channelId,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_description)
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
}
