package com.example.mincholisme.urinalysis;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.os.Handler;
import android.content.pm.PackageManager;
import android.widget.TextView;
import android.widget.Toast;
import android.bluetooth.le.BluetoothLeScanner;

import java.util.List;


public class Main2Activity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter myBluetoothAdapter ;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 30000;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private BluetoothGatt mGatt;
    private BluetoothDevice mDevice;

    private void updateDevice(String devName){
        TextView t=(TextView)findViewById(R.id.dev_type);
        t.setText(devName);
    }

    private void updateValue(String value){
        TextView t=(TextView)findViewById(R.id.value_read);
        t.setText(value);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        mHandler = new Handler();
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE Not Supported",
                    Toast.LENGTH_SHORT).show();
            finish();
        }
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        //myBluetoothAdapter = bluetoothManager.getAdapter();
        //1. Get adapter for Bluetooth
            //error
        if(!myBluetoothAdapter.isEnabled()){
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BT);
        }
        //2. Enable bluetooth
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (myBluetoothAdapter != null && myBluetoothAdapter.isEnabled()) {
            scanLeDevice(false);
        }
    }

    @Override
    protected void onDestroy() {
        if (mGatt == null) {
            return;
        }
        mGatt.close();
        mGatt = null;
        super.onDestroy();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                //Bluetooth not enabled.
                finish();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    private BluetoothAdapter.LeScanCallback myLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.i("onLeScan", device.toString());
                            connectToDevice(device);
                        }
                    });
                }
            };

    private ScanCallback myScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i("callbackType", String.valueOf(callbackType));
            String devicename = result.getDevice().getName();


            if (devicename != null) {
                if (devicename.startsWith("TAIDOC")) {
                    Log.i("result", "Device name: " + devicename);
                    Log.i("result", result.toString());
                    BluetoothDevice btDevice = result.getDevice();
                    connectToDevice(btDevice);
                }
            }

        }
    };
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT < 21) {
                        myBluetoothAdapter.stopLeScan(myLeScanCallback);
                    } else {
                        mLEScanner.stopScan(myScanCallback);

                    }
                }
            }, SCAN_PERIOD);
            if (Build.VERSION.SDK_INT < 21) {
                myBluetoothAdapter.startLeScan(myLeScanCallback);
            } else {
                mLEScanner.startScan(filters, settings, myScanCallback);
            }
        } else {
            if (Build.VERSION.SDK_INT < 21) {
                myBluetoothAdapter.stopLeScan(myLeScanCallback);
            } else {
                mLEScanner.stopScan(myScanCallback);
            }
        }
    }

    public void connectToDevice(BluetoothDevice device) {
        if (mGatt == null) {
            Log.d("connectToDevice", "connecting to device: "+device.toString());
            this.mDevice = device;
            mGatt = device.connectGatt(this, false, gattCallback);
            scanLeDevice(false);// will stop after first device detection
        }
    }
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {


        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i("onConnectionStateChange", "Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED");

                    //update UI
                    Message msg = Message.obtain();

                    String deviceName = gatt.getDevice().getName();
                    switch (deviceName) {
                        case "TAIDOC TD1261":
                            deviceName = "Thermo";
                            break;
                        case "TAIDOC TD8255":
                            deviceName = "SPO2";
                            break;
                        case "TAIDOC TD4279":
                            deviceName = "SPO2";
                            break;
                    }

                    msg.obj = deviceName;
                    msg.what = 0;
                    //   msg.setTarget(uiHandler);
                    msg.sendToTarget();

                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    Log.i("gattCallback", "reconnecting...");
                    BluetoothDevice mDevice = gatt.getDevice();
                    mGatt = null;
                    connectToDevice(mDevice);
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }

        }

    };
}


