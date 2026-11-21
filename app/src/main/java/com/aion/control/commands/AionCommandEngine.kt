package com.aion.control.commands

import com.aion.control.apps.LaunchableApp

/** Local allowlisted command interpreter. It never executes arbitrary shell/code. */
sealed class AionAction(val title: String, val detail: String) {
    data object OpenSettings : AionAction("Open Android Settings", "Launch the system settings screen")
    data object OpenDisplay : AionAction("Open Display Settings", "Launch Android display settings")
    data object OpenSound : AionAction("Open Sound Settings", "Launch Android sound settings")
    data class LaunchApp(val app: LaunchableApp) : AionAction("Open ${app.label}", "Launch ${app.packageName}")
}

sealed class AionPlan {
    data class Ready(val action: AionAction) : AionPlan()
    data class Unknown(val reason: String) : AionPlan()
}

class AionCommandEngine {
    fun plan(input: String, apps: List<LaunchableApp>): AionPlan {
        val q = input.trim().lowercase()
        if (q.isBlank()) return AionPlan.Unknown("Enter a command first")
        if (listOf("open settings", "buka pengaturan", "buka setelan", "settings").any(q::contains))
            return AionPlan.Ready(AionAction.OpenSettings)
        if (listOf("open display", "buka tampilan", "pengaturan layar", "display settings").any(q::contains))
            return AionPlan.Ready(AionAction.OpenDisplay)
        if (listOf("open sound", "buka suara", "pengaturan suara", "sound settings").any(q::contains))
            return AionPlan.Ready(AionAction.OpenSound)

        val launchPrefixes = listOf("open ", "buka ", "jalankan ", "launch ")
        val target = launchPrefixes.firstNotNullOfOrNull { prefix ->
            if (q.startsWith(prefix)) q.removePrefix(prefix).trim() else null
        }
        if (target != null) {
            val match = apps.firstOrNull { it.label.equals(target, ignoreCase = true) }
                ?: apps.firstOrNull { it.label.equals(target, ignoreCase = true) }
            return if (match != null) AionPlan.Ready(AionAction.LaunchApp(match))
            else AionPlan.Unknown("No installed app exactly matches '$target'")
        }
        return AionPlan.Unknown("Command not in the local allowlist. Try: buka pengaturan, buka tampilan, buka suara, or buka [nama aplikasi].")
    }
}
