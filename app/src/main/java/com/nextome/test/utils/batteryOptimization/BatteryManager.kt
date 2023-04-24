package com.nextome.test.utils.batteryOptimization

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.PowerManager
import android.provider.Settings
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextome.test.R
import org.koin.core.component.KoinComponent

class BatteryManager(
    private val sharedPref: SharedPreferences
) : KoinComponent {

    companion object {
        private const val USER_IGNORED_BATTERY_OPTIMIZATION_KEY = "ignore_batt_opt_key"
    }

    fun warnIfBatteryOptimized(ctx: Context) {
        if (!getUserIgnoredOptimizationMessage()) {
            val powerManager =
                ctx.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager

            if (!powerManager.isIgnoringBatteryOptimizations(ctx.applicationContext.packageName)) {
                showIgnoreBatteryOptimizationDialog(ctx)
            }
        }
    }

    private fun showIgnoreBatteryOptimizationDialog(ctx: Context) {
        val alertDialogBuilder = MaterialAlertDialogBuilder(ctx)

        alertDialogBuilder.setTitle(ctx.getString(R.string.battery_opt_title))

        // set dialog message
        alertDialogBuilder
            .setMessage(ctx.getString(R.string.battery_opt_desc))
            .setCancelable(true)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                dialog.cancel()
            }
            .setNegativeButton(R.string.generic_no) { dialog, _ ->
                dialog.cancel()
            }
            .setNeutralButton(R.string.generic_doNotAskAgain) { dialog, _ ->
                setUserIgnoredOptimizationMessage(true)
                dialog.cancel()
            }

        alertDialogBuilder.create().show()
    }

    private fun setUserIgnoredOptimizationMessage(value: Boolean) {
        with (sharedPref.edit()) {
            putBoolean(USER_IGNORED_BATTERY_OPTIMIZATION_KEY, value)
            commit()
        }
    }

    private fun getUserIgnoredOptimizationMessage(): Boolean {
        with (sharedPref) {
            return getBoolean(USER_IGNORED_BATTERY_OPTIMIZATION_KEY, false)
        }
    }
}