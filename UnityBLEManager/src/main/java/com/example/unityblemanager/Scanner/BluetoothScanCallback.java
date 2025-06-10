package com.example.unityblemanager.Scanner;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;

import java.util.Hashtable;
import java.util.List;

public abstract class BluetoothScanCallback extends ScanCallback {

    Hashtable<String, BluetoothDevice> mDevices;

    public BluetoothScanCallback(){
        mDevices = new Hashtable<String, BluetoothDevice>();
    }

    @Override
    public void onBatchScanResults(List<ScanResult> results) {
        super.onBatchScanResults(results);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onScanResult(int callbackType, ScanResult result) {
        super.onScanResult(callbackType, result);
        if (result == null ||
            result.getDevice() == null ||
            result.getDevice().getName() == null ||
            result.getDevice().getAddress() == null)
            return;

        String lAdress = result.getDevice().getAddress();

        if (!mDevices.containsKey(lAdress)){
            mDevices.put(lAdress, result.getDevice());
            OnDeviceFound(result.getDevice());
        }

        System.out.println("Found new device: " + result.getDevice().getName());
    }

    public abstract void OnDeviceFound(BluetoothDevice aDevice);

    public BluetoothDevice GetDevice(String aAdress){
        if (mDevices.containsKey(aAdress)) return mDevices.get(aAdress);
        System.out.println("Device " + aAdress + " not found");
        return null;
    }

    public int GetDeviceCount(){
        return mDevices.size();
    }

    public void Reset(){
        mDevices.clear();
    }
}
