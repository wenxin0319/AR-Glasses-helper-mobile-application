package com.example.masgcommunication;

import android.os.Handler;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ClientLatencyTest extends BluetoothCommunicationTemplate{
    private int seq, numSentPacket, numReceivedAck, sleepTimeInMS, currentSentBytes;

    private long latencySum, lastThroughputChangedTime, lastThroughputCheckedTime;

    private ConcurrentMap<Integer, Long> packetsSentTime;

    private List<Double> latencyThroughputData;

    Random random = new Random();

    public ClientLatencyTest(InputStream inputStream, OutputStream outputStream, Handler UIHandler) {
        super(inputStream, outputStream, UIHandler);
        seq = 1;
        latencySum = 0;
        sleepTimeInMS = 1000;
        packetsSentTime = new ConcurrentHashMap<>();
        lastThroughputChangedTime = System.currentTimeMillis();
        lastThroughputCheckedTime = System.currentTimeMillis();
        currentSentBytes = 0;
        latencyThroughputData = new ArrayList<>();
    }

    public List<Double> getGraphData() { return latencyThroughputData; };

    @Override
    protected void initializeSenderPerformanceMonitor() { numSentPacket = 0; }

    @Override
    protected void initializeReceiverPerformanceMonitor() { numReceivedAck = 0; }

    @Override
    protected void updateSenderPerformanceStats(int dataLen) {
        numSentPacket++;
        currentSentBytes += dataLen;
        UIHandler.obtainMessage(Constant.CLIENT_LATENCY_UPSTREAM_STATS, numSentPacket, 0).sendToTarget();
    }

    @Override
    protected void updateReceiverPerformanceStats(int dataLen) { numReceivedAck++; }

    @Override
    protected byte[] getSenderData() {
        try {
            if (sleepTimeInMS > 0) Thread.sleep(sleepTimeInMS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long currentTime = System.currentTimeMillis();
        packetsSentTime.put(seq++, currentTime);
        if (sleepTimeInMS > 0 && currentTime - lastThroughputChangedTime >= 3000) {
            lastThroughputChangedTime = currentTime;
            sleepTimeInMS -= 100;
        }

        byte[] data = new byte[1024];
        random.nextBytes(data);

        return data;
    }

    @Override
    protected void handleReceivedData(ByteBuffer dataBuffer) {
        dataBuffer.rewind();
        int ack = dataBuffer.getInt();
        long currentTime = System.currentTimeMillis();
        int currentLatency = (int) (currentTime - packetsSentTime.remove(ack));
        latencySum += currentLatency;
        double currentAverageLatency = latencySum / (double)numReceivedAck;

        LatencyStats latencyStats = new LatencyStats(numSentPacket, numReceivedAck, currentLatency, currentAverageLatency);

        if (currentTime - lastThroughputCheckedTime >= 500) {
            double intervalDuration = ((double)(currentTime - lastThroughputCheckedTime)) / 1000;
            double currentThroughput = (currentSentBytes / intervalDuration) / 1024;
            latencyStats.hasThroughputInfo = true;
            latencyStats.currentThroughput = currentThroughput;
            lastThroughputCheckedTime = currentTime;
            currentSentBytes = 0;
            latencyThroughputData.add(currentThroughput);
            latencyThroughputData.add((double)currentLatency);
        }

        UIHandler.obtainMessage(Constant.CLIENT_LATENCY_DOWNSTREAM_STATS, latencyStats).sendToTarget();
    }
}
