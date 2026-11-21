package com.aion.control.device

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import java.util.Locale

data class DeviceSnapshot(
    val manufacturer: String, val model: String, val androidVersion: String,
    val batteryPercent: Int?, val isCharging: Boolean,
    val ramUsedBytes: Long, val ramTotalBytes: Long,
    val storageUsedBytes: Long, val storageTotalBytes: Long
) {
    val ramPercent get() = if (ramTotalBytes > 0) (ramUsedBytes * 100 / ramTotalBytes).toInt() else 0
    val storagePercent get() = if (storageTotalBytes > 0) (storageUsedBytes * 100 / storageTotalBytes).toInt() else 0
    fun formatBytes(bytes: Long): String =
        String.format(Locale.getDefault(), "%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
}

class DeviceMonitor(private val context: Context) {
    fun readSnapshot(): DeviceSnapshot {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memory = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memory)
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val battery = if (level >= 0 && scale > 0) level * 100 / scale else null
        val state = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = state == BatteryManager.BATTERY_STATUS_CHARGING || state == BatteryManager.BATTERY_STATUS_FULL
        val stat = StatFs(Environment.getDataDirectory().absolutePath)
        return DeviceSnapshot(
            Build.MANUFACTURER.orEmpty(), Build.MODEL.orEmpty(), Build.VERSION.RELEASE.orEmpty(),
            battery, charging, (memory.totalMem - memory.availMem).coerceAtLeast(0),
            memory.totalMem, (stat.totalBytes - stat.availableBytes).coerceAtLeast(0), stat.totalBytes
        )
    }
}
