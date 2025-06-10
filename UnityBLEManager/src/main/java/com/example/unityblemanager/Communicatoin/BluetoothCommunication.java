package com.example.unityblemanager.Communicatoin;

import android.annotation.SuppressLint;
import android.bluetooth.*;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.UUID;

public class BluetoothCommunication {

    private static final String TAG = "BluetoothComm";

    private final Context           mContext;
    private final BluetoothDevice   mDevice;
    private final BluetoothCommunicationListener mListener;

    private BluetoothGatt               mBluetoothGatt;
    private BluetoothGattService        mBluetoothGattService;
    private BluetoothGattCharacteristic mBluetoothGattCharacteristic;

    // Usamos el mismo UUID para servicio y characteristic
    private static final UUID SERVICE_UUID        =  UUID.fromString("19B10001-E8F2-537E-4F6C-D104768A1214");
    private static final UUID CHARACTERISTIC_UUID =  SERVICE_UUID;
    private static final UUID CCCD_UUID           = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private boolean mIsConnected = false;
    public  boolean IsConnected() { return mIsConnected; }

    public BluetoothCommunication(Context context,
                                  BluetoothDevice device,
                                  BluetoothCommunicationListener listener) {
        this.mContext  = context;
        this.mDevice   = device;
        this.mListener = listener;
    }

    // 1) StartCommunication: conecta GATT y notifica al listener
    @SuppressLint("MissingPermission")
    public void StartCommunication() {
        mIsConnected = false;
        mBluetoothGatt = mDevice.connectGatt(mContext, false, mGattCallback);
        mListener.OnStartCommunication();
    }

    // 2) EndCommunication: desconecta, cierra GATT y limpia estado
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
        if (mIsConnected && mBluetoothGattCharacteristic != null) {
            mBluetoothGattCharacteristic.setValue(data);
            mBluetoothGatt.writeCharacteristic(mBluetoothGattCharacteristic);
        }
    }

    // Devuelve el BluetoothDevice si está conectado
    public BluetoothDevice GetConnectedDevice() {
        return mIsConnected ? mDevice : null;
    }

    // Fuerza una lectura del characteristic
    @SuppressLint("MissingPermission")
    public void ReadData() {
        if (mIsConnected && mBluetoothGattCharacteristic != null) {
            mBluetoothGatt.readCharacteristic(mBluetoothGattCharacteristic);
        }
    }

    // GATT callback interno
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        // Al cambiar estado de conexión
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(@NonNull BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected → discovering services…");
                gatt.discoverServices();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mIsConnected = false;
                mListener.OnConnectionLost();
                Log.i(TAG, "Disconnected");
            }
        }

        // Al descubrir servicios
        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(@NonNull BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Service discovery failed: " + status);
                return;
            }

            mBluetoothGattService = gatt.getService(SERVICE_UUID);
            if (mBluetoothGattService == null) {
                Log.e(TAG, "Service not found: " + SERVICE_UUID);
                return;
            }

            Log.i(TAG, "Service found: " + SERVICE_UUID);
            mBluetoothGattCharacteristic = mBluetoothGattService.getCharacteristic(CHARACTERISTIC_UUID);

            if (mBluetoothGattCharacteristic == null) {
                Log.e(TAG, "Characteristic not found: " + CHARACTERISTIC_UUID);
                return;
            }

            // Suscribirse a notificaciones
            boolean ok = gatt.setCharacteristicNotification(mBluetoothGattCharacteristic, true);
            if (!ok) {
                Log.e(TAG, "setCharacteristicNotification() failed");
            } else {
                BluetoothGattDescriptor desc = mBluetoothGattCharacteristic.getDescriptor(CCCD_UUID);
                if (desc != null) {
                    desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    boolean wrote = gatt.writeDescriptor(desc);
                    if (!wrote) Log.e(TAG, "Error writing CCCD descriptor");
                } else {
                    Log.e(TAG, "CCCD descriptor missing");
                }
            }

            // Confirmar conexión al listener
            mIsConnected = true;
            mListener.OnConnectionEstablished();
            Log.i(TAG, "Bluetooth ready");
        }

        // Callback para notificaciones (notify)
        @Override
        public void onCharacteristicChanged(@NonNull BluetoothGatt gatt,
                                            @NonNull BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            byte[] raw = characteristic.getValue();
            mListener.OnDataReceived(raw);
        }

        // Callback para lecturas explícitas (readCharacteristic)
        @SuppressLint("MissingPermission")
        @Override
        public void onCharacteristicRead(@NonNull BluetoothGatt gatt,
                                         @NonNull BluetoothGattCharacteristic characteristic,
                                         int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                byte[] raw = characteristic.getValue();
                mListener.OnDataReceived(raw);
            }
        }

        // Callback tras writeCharacteristic()
        @Override
        public void onCharacteristicWrite(@NonNull BluetoothGatt gatt,
                                          @NonNull BluetoothGattCharacteristic characteristic,
                                          int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Write successful");
            } else {
                Log.e(TAG, "Write failed: " + status);
            }
        }
    };
}
