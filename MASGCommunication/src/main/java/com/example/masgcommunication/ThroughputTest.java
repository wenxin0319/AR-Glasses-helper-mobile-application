package com.example.masgcommunication;

import android.os.Handler;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Random;

public class ThroughputTest extends BluetoothCommunicationTemplate{
    private ThroughputCalculator senderCalculator, receiverCalculator;

    private Random random;

    private long lastChangedTime;

    private int sleepTimeInMS;

    public ThroughputTest(InputStream inputStream, OutputStream outputStream, Handler UIHandler) {
        super(inputStream, outputStream, UIHandler);
        random = new Random();
        lastChangedTime = System.currentTimeMillis();
        sleepTimeInMS = 1000;
    }

    @Override
    protected void initializeSenderPerformanceMonitor() {
        senderCalculator = new ThroughputCalculator();
    }

    @Override
    protected void initializeReceiverPerformanceMonitor() {
        receiverCalculator = new ThroughputCalculator();
    }

    @Override
    protected void updateSenderPerformanceStats(int dataLen) {
        senderCalculator.updateStat(dataLen, true);
    }

    @Override
    protected void updateReceiverPerformanceStats(int dataLen) {
        receiverCalculator.updateStat(dataLen, false);
    }

    @Override
    protected byte[] getSenderData() {
        try {
            if (sleepTimeInMS > 0) Thread.sleep(sleepTimeInMS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long currentTime = System.currentTimeMillis();
        if (sleepTimeInMS > 0 && currentTime - lastChangedTime >= 3000) {
            lastChangedTime = currentTime;
            sleepTimeInMS /= 2;
        }

        byte[] data = new byte[1024];
        random.nextBytes(data);

        return data;
    }

    @Override
    protected void handleReceivedData(ByteBuffer dataBuffer) {
        // do nothing
    }

    private class ThroughputCalculator {
        private int totalByte;
        private int currentByte;
        private final long startTime;
        private long lastCheckedTime;

        public ThroughputCalculator() {
            totalByte = 0;
            currentByte = 0;
            startTime = System.currentTimeMillis();
            lastCheckedTime = startTime;
        }

        public void updateStat(int dataLen, boolean isUpstream) {
            totalByte += dataLen;
            currentByte += dataLen;
            long currentTime = System.currentTimeMillis();
            double duration = ((double)(currentTime - startTime)) / 1000;
            double totalThroughput = (totalByte / duration) / 1024;

            ThroughputStats throughputStats = new ThroughputStats(totalByte, duration, totalThroughput);

            if (currentTime - lastCheckedTime >= 500) {
                double intervalDuration = ((double)(currentTime - lastCheckedTime)) / 1000;
                double currentThroughput = (currentByte / intervalDuration) / 1024;
                currentByte = 0;
                lastCheckedTime = currentTime;

                throughputStats.hasCurrentStat = true;
                throughputStats.currentThroughput = currentThroughput;
            }

            UIHandler.obtainMessage(
                isUpstream ? Constant.THROUGHPUT_UPSTREAM_STATS : Constant.THROUGHPUT_DOWNSTREAM_STATS, throughputStats
            ).sendToTarget();
        }
    }
}
