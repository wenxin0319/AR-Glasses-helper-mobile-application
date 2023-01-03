package com.example.masgcommunication;

import android.os.Handler;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class ServerLatencyTest extends BluetoothCommunicationTemplate{
    int numSentAck;
    int numReceivedPacket;

    BlockingDeque<Integer> receivedPackets;

    public ServerLatencyTest(InputStream inputStream, OutputStream outputStream, Handler UIHandler) {
        super(inputStream, outputStream, UIHandler);

        receivedPackets = new LinkedBlockingDeque<>();
    }

    @Override
    protected void initializeSenderPerformanceMonitor() {
        numSentAck = 0;
    }

    @Override
    protected void initializeReceiverPerformanceMonitor() {
        numReceivedPacket = 0;
    }

    @Override
    protected void updateSenderPerformanceStats(int dataLen) {
        numSentAck++;
        UIHandler.obtainMessage(Constant.SERVER_LATENCY_UPSTREAM_STATS, numSentAck, receivedPackets.size()).sendToTarget();
    }

    @Override
    protected void updateReceiverPerformanceStats(int dataLen) {
        numReceivedPacket++;
        UIHandler.obtainMessage(Constant.SERVER_LATENCY_DOWNSTREAM_STATS, numReceivedPacket, receivedPackets.size()).sendToTarget();
    }

    @Override
    protected byte[] getSenderData() {
        try {
            return ByteBuffer.allocate(4).putInt(receivedPackets.take()).array();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void handleReceivedData(ByteBuffer dataBuffer) {
        try {
            receivedPackets.put(numReceivedPacket);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
