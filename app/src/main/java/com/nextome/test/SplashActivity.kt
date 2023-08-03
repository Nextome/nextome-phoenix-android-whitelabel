package com.nextome.test

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextome.test.data.NextomeSdkCredentials
import com.nextome.test.databinding.ActivitySplashBinding
import com.nextome.test.map.MapActivity
import com.nextome.test.settings.SettingsActivity
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.annotations.AfterPermissionGranted
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel


private const val PERMISSIONS_REQUEST_CODE = 1
private const val PERMISSIONS_BACKGROUND_REQUEST_CODE = 2

class SplashActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {

    companion object {
        const val INTENT_EXTRA_LOGOUT = "intent_logout"
        fun getIntent(forceLogOut: Boolean, ctx: Context) =
            Intent(ctx, SplashActivity::class.java).apply {
                putExtra(INTENT_EXTRA_LOGOUT, forceLogOut)
            }
    }

    private val REQUEST_ENABLE_BLUETOOTH = 0
    private val viewModel: SplashScreenViewModel by viewModel()

    private lateinit var binding: ActivitySplashBinding
    private val progressDialog: ProgressDialog by lazy {
        ProgressDialog(this).apply {
            setCancelable(false)
        }
    }

    private var PERMISSIONS: Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE,
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        arrayOf(
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private var PERMISSION_BACKGROUND = Manifest.permission.ACCESS_BACKGROUND_LOCATION

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkPermissions()
        checkBatteryOptimizations()

        with (binding) {
            settingsButton.setOnClickListener {
                startActivity(Intent(this@SplashActivity, SettingsActivity::class.java))
            }

            startButton.setOnClickListener {
                openMapWithCredentials(NextomeSdkCredentials(
                    clientId = NextomeCredentials.clientId,
                    clientSecret = NextomeCredentials.clientSecret,
                ))
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiEvents.collect {
                        when (it) {
                            is SplashScreenViewModel.ShowMessageEvent -> {
                                Toast.makeText(
                                    this@SplashActivity, it.message, Toast.LENGTH_LONG).show()
                            }
                            is SplashScreenViewModel.ShowLoading -> {
                                progressDialog.show()
                            }
                            is SplashScreenViewModel.HideLoading -> {
                                progressDialog.dismiss()
                            }
                            is SplashScreenViewModel.ShowHasEditedSetting -> {
                                binding.settingsEditedCircle.isVisible = it.settingsEdited
                            }
                        }
                    }
                }
            }
        }
    }

    private fun openMapWithCredentials(credentials: NextomeSdkCredentials) {
        if (isBluetoothReadyToUse()) {
            startActivity(MapActivity.getIntent(credentials, this).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }

    @AfterPermissionGranted(PERMISSIONS_REQUEST_CODE)
    private fun checkPermissions() {
        if (!EasyPermissions.hasPermissions(this, *PERMISSIONS)) {
            showPermissionRationale()
        } else {
            // Has normal permissions
            // now check for background
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (!EasyPermissions.hasPermissions(this, PERMISSION_BACKGROUND)) {
                    showBackgroundPermissionRationale()
                }
            }
        }
    }

    private fun isBluetoothReadyToUse(): Boolean {
        if (!packageManager.hasSystemFeature(
                        PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, getString(R.string.error_ble_not_supported),
                    Toast.LENGTH_SHORT).show()
            return false
        } else {
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            if (!bluetoothManager.adapter.isEnabled){
                if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Toast.makeText(this, getString(R.string.bluetooth_off),
                        Toast.LENGTH_SHORT).show()
                    try {
                        startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BLUETOOTH)
                    } catch(e: Exception) {
                        e.printStackTrace()
                    }

                    return false
                }
                startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BLUETOOTH)
                return false
            }
        }

        return true
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun showBackgroundPermissionRationale() {
        if (!EasyPermissions.hasPermissions(this, PERMISSION_BACKGROUND)) {
            EasyPermissions.requestPermissions(
                host = this,
                rationale = getString(R.string.permission_request_background),
                requestCode = PERMISSIONS_BACKGROUND_REQUEST_CODE,
                perms = arrayOf(PERMISSION_BACKGROUND)
            )
        }
    }

    private fun showPermissionRationale() {
        val builder = MaterialAlertDialogBuilder(this)

        builder.setMessage(R.string.permission_request_rationale)
            .setCancelable(false)
            .setTitle(R.string.permission_request_title)
            .setPositiveButton(R.string.permission_ok) { dialog, which ->
                dialog.dismiss()
                EasyPermissions.requestPermissions(
                    host = this,
                    rationale = getString(R.string.permission_request),
                    requestCode = PERMISSIONS_REQUEST_CODE,
                    perms = PERMISSIONS
                )
            }

        builder.create().show()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_ENABLE_BLUETOOTH && resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(this, "Please enable bluetooth to use this app.",
                    Toast.LENGTH_SHORT).show()
            finish()
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (perms.size == 1 && perms[0] == PERMISSION_BACKGROUND) {
                // App can still work, but will not in background
                Toast.makeText(
                    this,
                    getString(R.string.permission_background_denied_rationale),
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
        }

        // Can't proceed
        Toast.makeText(this, getString(R.string.permission_denied_rationale), Toast.LENGTH_SHORT).show()
        finish()
    }


    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {}


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    private fun checkBatteryOptimizations() {
        viewModel.checkBatteryOptimizations(this)
    }

    override fun onResume() {
        super.onResume()
        viewModel.reloadSettings()
    }
}