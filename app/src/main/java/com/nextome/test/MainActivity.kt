package com.nextome.test

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.annotations.AfterPermissionGranted
import kotlinx.android.synthetic.main.activity_main.*


private const val PERMISSIONS_REQUEST_CODE = 1
private const val PERMISSIONS_BACKGROUND_REQUEST_CODE = 2

class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {
    val REQUEST_ENABLE_BLUETOOTH = 0

    private var PERMISSIONS: Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        arrayOf(
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private var PERMISSION_BACKGROUND = Manifest.permission.ACCESS_BACKGROUND_LOCATION

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkBluetoothLe()
        checkPermissions()

        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        startSession.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }
    }

    @AfterPermissionGranted(PERMISSIONS_REQUEST_CODE)
    private fun checkPermissions() {
        if (!EasyPermissions.hasPermissions(this, *PERMISSIONS)) {
            showPermissionRationale()
        }
    }

    private fun checkBluetoothLe() {
        if (!packageManager.hasSystemFeature(
                        PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE is not supported in this device.",
                    Toast.LENGTH_SHORT).show()
            finish()
        } else {
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            if (!bluetoothManager.adapter.isEnabled){
                startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BLUETOOTH)
            }
        }
    }

    private fun showPermissionRationale() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)

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

                if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                    if (!EasyPermissions.hasPermissions(this, PERMISSION_BACKGROUND)) {
                        EasyPermissions.requestPermissions(
                            host = this,
                            rationale = getString(R.string.permission_request_background),
                            requestCode = PERMISSIONS_REQUEST_CODE,
                            perms = arrayOf(PERMISSION_BACKGROUND)
                        )
                    }
                }
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

}