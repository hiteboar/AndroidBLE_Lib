package com.example.unityblemanager.Communicatoin;

public abstract class BluetoothCommunicationListener {
    public abstract void OnStartCommunication();
    public abstract  void OnConnectionEstablished();
    public abstract void OnDataReceived(byte[] aData);
    public abstract void OnConnectionLost();
}
