package com.example.masgcommunication;

import android.os.Handler;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.List;

public class ClientLatencyStatsTransmission extends BluetoothCommunicationTemplate{

    List<Double> graphData;

    int index;

    public ClientLatencyStatsTransmission(InputStream inputStream,
                                          OutputStream outputStream,
                                          Handler UIHandler,
                                          List<Double> graphData) {
        super(inputStream, outputStream, UIHandler);
        this.graphData = graphData;
        index = 0;
    }

    @Override
    protected void initializeSenderPerformanceMonitor() {
        // do nothing
    }

    @Override
    protected void initializeReceiverPerformanceMonitor() {
        // do nothing
    }

    @Override
    protected void updateSenderPerformanceStats(int dataLen) {
        // do nothing
    }

    @Override
    protected void updateReceiverPerformanceStats(int dataLen) {
        // do nothing
    }

    @Override
    protected byte[] getSenderData() {
        if (index == graphData.size()) return null;
        return ByteBuffer.allocate(8).putDouble(graphData.get(index++)).array();
    }

    @Override
    protected void handleReceivedData(ByteBuffer dataBuffer) {
        // do nothing
    }
}
