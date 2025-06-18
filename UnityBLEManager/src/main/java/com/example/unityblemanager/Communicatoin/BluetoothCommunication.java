package com.example.unityblemanager.Communicatoin;

import android.annotation.SuppressLint;
import android.bluetooth.*;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.UUID;

public class BluetoothCommunication {

    private static final String TAG = "BluetoothComm";

    private final Context mContext;
    private final BluetoothDevice mDevice;
    private final BluetoothCommunicationListener mListener;

    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattService mBluetoothGattService;
    private BluetoothGattCharacteristic mRxCharacteristic;  // App → Arduino
    private BluetoothGattCharacteristic mTxCharacteristic;  // Arduino → App

    private static final UUID SERVICE_UUID         = UUID.fromString("19B10000-E8F2-537E-4F6C-D104768A1214");
    private static final UUID RX_CHARACTERISTIC_UUID = UUID.fromString("19B10001-E8F2-537E-4F6C-D104768A1214");
    private static final UUID TX_CHARACTERISTIC_UUID = UUID.fromString("19B10002-E8F2-537E-4F6C-D104768A1214");
    private static final UUID CCCD_UUID            = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private boolean mIsConnected = false;
    public boolean IsConnected() { return mIsConnected; }

    public BluetoothCommunication(Context context,
                                  BluetoothDevice device,
                                  BluetoothCommunicationListener listener) {
        this.mContext = context;
        this.mDevice = device;
        this.mListener = listener;
    }

    @SuppressLint("MissingPermission")
    public void StartCommunication() {
        mIsConnected = false;
        mBluetoothGatt = mDevice.connectGatt(mContext, false, mGattCallback);
        mListener.OnStartCommunication();
    }

    @SuppressLint("MissingPermission")
    public void EndCommunication() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
        mIsConnected = false;
    }

    @SuppressLint("MissingPermission")
    public void WriteData(byte[] data) {
        if (mIsConnected && mRxCharacteristic != null) {
            mRxCharacteristic.setValue(data);
            mBluetoothGatt.writeCharacteristic(mRxCharacteristic);
        }
    }

    public BluetoothDevice GetConnectedDevice() {
        return mIsConnected ? mDevice : null;
    }

    @SuppressLint("MissingPermission")
    public void ReadData() {
        if (mIsConnected && mTxCharacteristic != null) {
            mBluetoothGatt.readCharacteristic(mTxCharacteristic);
        }
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(@NonNull BluetoothGatt gatt, int status, int newState) {
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected → discovering services…");
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mIsConnected = false;
                mListener.OnConnectionLost();
                Log.i(TAG, "Disconnected");
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(@NonNull BluetoothGatt gatt, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Service discovery failed: " + status);
                return;
            }

            mBluetoothGattService = gatt.getService(SERVICE_UUID);
            if (mBluetoothGattService == null) {
                Log.e(TAG, "Service not found: " + SERVICE_UUID);
                return;
            }

            // RX: App → Arduino (write)
            mRxCharacteristic = mBluetoothGattService.getCharacteristic(RX_CHARACTERISTIC_UUID);
            if (mRxCharacteristic == null) {
                Log.e(TAG, "RX Characteristic not found: " + RX_CHARACTERISTIC_UUID);
                return;
            }

            // TX: Arduino → App (notify)
            mTxCharacteristic = mBluetoothGattService.getCharacteristic(TX_CHARACTERISTIC_UUID);
            if (mTxCharacteristic == null) {
                Log.e(TAG, "TX Characteristic not found: " + TX_CHARACTERISTIC_UUID);
                return;
            }

            // Habilitar notificaciones para TX
            boolean ok = gatt.setCharacteristicNotification(mTxCharacteristic, true);
            if (!ok) {
                Log.e(TAG, "setCharacteristicNotification() failed");
            } else {
                BluetoothGattDescriptor desc = mTxCharacteristic.getDescriptor(CCCD_UUID);
                if (desc != null) {
                    desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    boolean wrote = gatt.writeDescriptor(desc);
                    if (!wrote) Log.e(TAG, "Error writing CCCD descriptor");
                } else {
                    Log.e(TAG, "CCCD descriptor missing");
                }
            }

            mIsConnected = true;
            mListener.OnConnectionEstablished();
            Log.i(TAG, "Bluetooth ready");
        }

        @Override
        public void onCharacteristicChanged(@NonNull BluetoothGatt gatt,
                                            @NonNull BluetoothGattCharacteristic characteristic) {
            if (characteristic.getUuid().equals(TX_CHARACTERISTIC_UUID)) {
                byte[] raw = characteristic.getValue();
                mListener.OnDataReceived(raw);
            }
        }
        
        @Override
        public void onCharacteristicRead(@NonNull BluetoothGatt gatt,
                                         @NonNull BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS && characteristic.getUuid().equals(TX_CHARACTERISTIC_UUID)) {
                byte[] raw = characteristic.getValue();
                mListener.OnDataReceived(raw);
            }
        }

        @Override
        public void onCharacteristicWrite(@NonNull BluetoothGatt gatt,
                                          @NonNull BluetoothGattCharacteristic characteristic,
                                          int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Write successful");
            } else {
                Log.e(TAG, "Write failed: " + status);
            }
        }
    };

    private String byteArrayToString(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append((b & 0xFF)).append(" ");
        }
        return sb.toString().trim();
    }
}
