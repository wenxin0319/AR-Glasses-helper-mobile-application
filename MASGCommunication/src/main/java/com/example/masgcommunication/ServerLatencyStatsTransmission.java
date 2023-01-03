package com.example.masgcommunication;

import android.os.Handler;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ServerLatencyStatsTransmission extends BluetoothCommunicationTemplate{

    List<Number> graphData;

    public ServerLatencyStatsTransmission(InputStream inputStream, OutputStream outputStream, Handler UIHandler) {
        super(inputStream, outputStream, UIHandler);
        graphData = new ArrayList<>();
    }

    public List<Number> getGraphData() { return graphData; }

    @Override
    protected void initializeSenderPerformanceMonitor() {
        // do not start sender
        finished = true;
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
        return new byte[0];
    }

    @Override
    protected void handleReceivedData(ByteBuffer dataBuffer) {
        dataBuffer.rewind();
        graphData.add(dataBuffer.getDouble());
    }
}
