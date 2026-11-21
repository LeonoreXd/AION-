package com.aion.control.device

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings

class DeviceController(private val context: Context) {
    fun openSystemSettings() = open(Settings.ACTION_SETTINGS)
    fun openDisplaySettings() = open(Settings.ACTION_DISPLAY_SETTINGS)
    fun openSoundSettings() = open(Settings.ACTION_SOUND_SETTINGS)

    private fun open(action: String) {
        context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun setMediaVolume(percent: Int) {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val target = (max * percent.coerceIn(0, 100) / 100f).toInt()
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
    }
}
