package com.example.masgcommunication;

import android.os.Handler;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ServerCommunication extends BluetoothCommunicationTemplate{
    BlockingQueue<byte[]> ODResults;
    BlockingQueue<byte[]> receivedFrames;

    long lastUpstreamCheckedTime, lastDownstreamCheckedTime;
    int numSentFrames, totalSentBytes, currentSentBytes, numReceivedFrames, totalReceivedBytes, currentReceivedBytes;

    public ServerCommunication(InputStream inputStream, OutputStream outputStream, Handler UIHandler) {
        super(inputStream, outputStream, UIHandler);
        ODResults = new LinkedBlockingQueue<>();
        receivedFrames = new LinkedBlockingQueue<>();
    }

    public BlockingQueue<byte[]> getODResultsQueue() {
        return this.ODResults;
    }

    public byte[] pollEncodedFrame() throws InterruptedException {
        return receivedFrames.poll(100, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void initializeSenderPerformanceMonitor() {
        lastUpstreamCheckedTime = System.currentTimeMillis();
        numSentFrames = 0;
        totalSentBytes = 0;
        currentSentBytes = 0;
    }

    @Override
    protected void initializeReceiverPerformanceMonitor() {
        lastDownstreamCheckedTime = System.currentTimeMillis();
        numReceivedFrames = 0;
        totalReceivedBytes = 0;
        currentReceivedBytes = 0;
    }

    @Override
    protected void updateSenderPerformanceStats(int dataLen) {
        numSentFrames++;
        totalSentBytes += dataLen;
        currentSentBytes += dataLen;

        RunningStats stats = new RunningStats(numSentFrames, totalSentBytes);

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpstreamCheckedTime >= 1000) {
            double interval = ((double)(currentTime - lastUpstreamCheckedTime)) / 1000;
            double currentThroughput = currentSentBytes / interval / 1024;
            currentSentBytes = 0;
            lastUpstreamCheckedTime = currentTime;

            stats.hasCurrentStat = true;
            stats.currentThroughput = currentThroughput;
        }

        UIHandler.obtainMessage(Constant.SERVER_RUNNING_UPSTREAM_STATS, stats).sendToTarget();
    }

    @Override
    protected void updateReceiverPerformanceStats(int dataLen) {
        numReceivedFrames++;
        totalReceivedBytes += dataLen;
        currentReceivedBytes += dataLen;

        RunningStats stats = new RunningStats(numReceivedFrames, totalReceivedBytes);

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastDownstreamCheckedTime >= 1000) {
            double interval = ((double)(currentTime - lastDownstreamCheckedTime)) / 1000;
            double currentThroughput = currentReceivedBytes / interval / 1024;
            currentReceivedBytes = 0;
            lastDownstreamCheckedTime = currentTime;

            stats.hasCurrentStat = true;
            stats.currentThroughput = currentThroughput;
        }

        UIHandler.obtainMessage(Constant.SERVER_RUNNING_DOWNSTREAM_STATS, stats).sendToTarget();
    }

    @Override
    protected byte[] getSenderData() {
        try {
            byte[] data = ODResults.poll(100, TimeUnit.MILLISECONDS);

            return data == null ? new byte[0] : data;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void handleReceivedData(ByteBuffer dataBuffer) {
        try {
            receivedFrames.put(dataBuffer.array());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
