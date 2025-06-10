package com.example.unityblemanager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.content.ContextCompat;

import com.example.unityblemanager.Communicatoin.BluetoothCommunication;
import com.example.unityblemanager.Communicatoin.BluetoothCommunicationListener;
import com.example.unityblemanager.Scanner.BluetoothScanCallback;
import com.example.unityblemanager.Scanner.BluetoothScanner;
import com.unity3d.player.UnityPlayer;

import java.util.ArrayList;
import java.util.UUID;
import java.util.Base64;


public class BluetoothController {

    //App context
    //private Context mContext;
    // Stops scanning after 30 seconds.
    private static final long SCAN_PERIOD = 30000;

    // Scanner callback object
    private static BluetoothScanCallback mBluetoothDevices;

    // Bluetooth Connection controller
    private static BluetoothScanner mScanner;

    // Bluetooth Gatt Communication controller
    private static BluetoothCommunication mBluetoothCommunication;

    // device UUID
    private static final UUID DEVICE_UUID = UUID.fromString("800713BC-3AF7-4CA1-9029-CA765444188F");
    private static final String TAG = "BluetoothLeService";

    @SuppressLint("MissingPermission")
    public static void Init(){
        System.out.println("INITIALIZE BLUETOOTH");
        CheckPermissions();

        mBluetoothDevices = new BluetoothScanCallback() {
            @Override
            public void OnDeviceFound(BluetoothDevice aDevice) {
                SendOnDeviceFound(aDevice);
            }
        };

        mScanner = new BluetoothScanner(UnityPlayer.currentActivity, mBluetoothDevices);
    }


    private static String[] mNeededPermissions = new String[]{
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.BLUETOOTH_CONNECT,

    };
    private static void CheckPermissions(){
        ArrayList<String> lToRequestPermissions = new ArrayList<String>();
        for(int i = 0; i < mNeededPermissions.length; ++i){
            if (ContextCompat.checkSelfPermission(UnityPlayer.currentActivity.getApplicationContext(), mNeededPermissions[i]) != PackageManager.PERMISSION_GRANTED){
                lToRequestPermissions.add(mNeededPermissions[i]);
            }
        }

        if (lToRequestPermissions.size() > 0){
            String[] lPermissionsList = new String[lToRequestPermissions.size()];
            lToRequestPermissions.toArray(lPermissionsList);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                UnityPlayer.currentActivity.requestPermissions(lPermissionsList, 100);
            }
        }
    }

    public static void SendData(byte[] aData){
        if (IsConnected()){
            mBluetoothCommunication.WriteData(aData);
        }
    }

    // Scan for other bluetooth devices
    public static void Scan(){
        System.out.println("START SCANNING");
        mScanner.StartScan();
        SendOnStartScan();
    }

    public static boolean IsScanning(){
        return mScanner.IsScanning();
    }

    public static void StopScan() {
        mScanner.StopScan();
        SendOnStopScan();
    }

    // Connect to the selected device
    @SuppressLint("MissingPermission")
    public static void Connect(String aDeviceAddress){
        if (mBluetoothCommunication == null) {
            StopScan();
            mBluetoothCommunication = new BluetoothCommunication(UnityPlayer.currentActivity.getApplicationContext(), mBluetoothDevices.GetDevice(aDeviceAddress), mBluetoothCommunicationListener);
            mBluetoothCommunication.StartCommunication();
        }
    }

    @SuppressLint("MissingPermission")
    private static void SendOnDeviceFound(BluetoothDevice aNewDevice){
        UnityPlayer.UnitySendMessage("BLEManager", "OnDeviceFound", aNewDevice.getAddress());
    }

    private static void SendOnStartScan() {
        UnityPlayer.UnitySendMessage("BLEManager", "OnStartScan", null);
    }

    private static void SendOnStopScan() {
       UnityPlayer.UnitySendMessage("BLEManager", "OnStopScan", null);
    }

    @SuppressLint("MissingPermission")
    public static void Disconnect(){
        if (mBluetoothCommunication != null) {
            mBluetoothCommunication.EndCommunication();
            UnityPlayer.UnitySendMessage("BLEManager", "OnDeviceDisconnected", null);
        }
    }

    @SuppressLint("MissingPermission")
    public static String GetDeviceName(){
        if (mBluetoothCommunication != null && mBluetoothCommunication.IsConnected() && mBluetoothCommunication.GetConnectedDevice() != null)
            return mBluetoothCommunication.GetConnectedDevice().getName();
        else
            return "";
    }

    @SuppressLint("MissingPermission")
    public static String GetDeviceName(String aAdress){
        BluetoothDevice d = mBluetoothDevices.GetDevice(aAdress);
        if (d != null)
            return d.getName();
        else
            return "";
    }

    //Return true if there is some device connected
    public static boolean IsConnected() { return mBluetoothCommunication != null && mBluetoothCommunication.IsConnected(); }

    static byte[] mToSendData = null;

    public static void UpdateDataToSend(byte Engine1, byte Engine2, byte Engine3, byte Engine4){
        mToSendData = new byte[] {Engine1, Engine2, Engine3,  Engine4};
    }

    private static BluetoothCommunicationListener mBluetoothCommunicationListener = new BluetoothCommunicationListener() {
        @Override
        public void OnStartCommunication() {
            UnityPlayer.UnitySendMessage("BLEManager", "OnStartConnection", null);
        }

        @Override
        public void OnConnectionEstablished() {
            UnityPlayer.UnitySendMessage("BLEManager", "OnDeviceConnected", GetDeviceName());
        }

        @Override
        public void OnDataReceived(byte[] aData) {
            String encoddedData = Base64.getEncoder().encodeToString(aData);
            UnityPlayer.UnitySendMessage("BLEManager", "OnDeviceDataReceived", encoddedData);
        }

        @Override
        public void OnConnectionLost() {
            Disconnect();
        }
    };
}

