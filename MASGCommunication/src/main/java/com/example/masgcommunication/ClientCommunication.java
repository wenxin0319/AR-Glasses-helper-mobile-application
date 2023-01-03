package com.example.masgcommunication;

import android.os.Handler;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ClientCommunication extends BluetoothCommunicationTemplate{
    private final BlockingQueue<byte[]> encodedFrames;
    private final BlockingQueue<List<ODResult>> odResults;

    private long lastUpstreamCheckedTime, lastDownstreamCheckedTime;
    private int numSentFrames, currentSentBytes, totalSentBytes, numReceivedFrames, currentReceivedBytes, totalReceivedBytes;
    private float currentLatency;

    private int seq;

    private final ConcurrentMap<Integer, Long> frameSentTimes;

    public ClientCommunication(InputStream inputStream, OutputStream outputStream, Handler UIHandler) {
        super(inputStream, outputStream, UIHandler);
        encodedFrames = new LinkedBlockingQueue<>();
        odResults = new LinkedBlockingQueue<>();
        frameSentTimes = new ConcurrentHashMap<>();
        seq = 0;
        currentLatency = 0;
    }

    public void enqueueEncodedFrame(byte[] data) {
        try {
            encodedFrames.put(data);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public List<ODResult> pollODResult() {
        try {
            return odResults.poll(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void initializeSenderPerformanceMonitor() {
        numSentFrames = 0;
        totalSentBytes = 0;
        currentSentBytes = 0;
        lastUpstreamCheckedTime = System.currentTimeMillis();
    }

    @Override
    protected void initializeReceiverPerformanceMonitor() {
        numReceivedFrames = 0;
        totalReceivedBytes = 0;
        currentReceivedBytes = 0;
        lastDownstreamCheckedTime = System.currentTimeMillis();
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

        if (UIHandler != null) UIHandler.obtainMessage(Constant.CLIENT_RUNNING_UPSTREAM_STATS, stats).sendToTarget();
//        System.out.print("framesenttime");
//        System.out.println(frameSentTimes.size());
    }

    @Override
    protected void updateReceiverPerformanceStats(int dataLen) {
        long currentTime = System.currentTimeMillis();
        numReceivedFrames++;
        totalReceivedBytes += dataLen;
        currentReceivedBytes += dataLen;

        RunningStats stats = new RunningStats(numReceivedFrames, totalReceivedBytes, currentLatency);

        if (currentTime - lastDownstreamCheckedTime >= 1000) {
            double interval = ((double)(currentTime - lastDownstreamCheckedTime)) / 1000;
            double currentThroughput = currentReceivedBytes / interval / 1024;
            currentReceivedBytes = 0;
            lastDownstreamCheckedTime = currentTime;

            stats.hasCurrentStat = true;
            stats.currentThroughput = currentThroughput;
        }

        if (UIHandler != null) UIHandler.obtainMessage(Constant.CLIENT_RUNNING_DOWNSTREAM_STATS, stats).sendToTarget();
    }

    @Override
    protected byte[] getSenderData() {
        try {
            byte[] data = encodedFrames.poll(100, TimeUnit.MILLISECONDS);

            if (data == null) return new byte[0];

            ByteBuffer dataBuffer = ByteBuffer.allocate(4 + data.length);
            dataBuffer.putInt(seq);
            dataBuffer.put(data);
            frameSentTimes.put(seq, System.currentTimeMillis());
            seq = (seq + 1) % 10000;

            return dataBuffer.array();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void handleReceivedData(ByteBuffer dataBuffer) {
//        System.out.printf("Received data with %d bytes!\n", dataBuffer.remaining());
        List<ODResult> curResults = new ArrayList<>();

        int ack = dataBuffer.getInt();

        Long sentTime = frameSentTimes.remove(ack);

        if (sentTime != null) currentLatency = (float) (Constant.GAMMA * (System.currentTimeMillis() - sentTime) + (1 - Constant.GAMMA) * currentLatency);

        int numObjects = dataBuffer.getInt();

        System.out.printf("Received %d objects:\n", numObjects);

        for (int i = 0; i < numObjects; i++) {
            int labelLength = dataBuffer.getInt();
            byte[] labelBytes = new byte[labelLength];
            dataBuffer.get(labelBytes);
            String label = new String(labelBytes, StandardCharsets.UTF_8);
            float confidence = dataBuffer.getFloat();
            float top = dataBuffer.getFloat();
            float bottom = dataBuffer.getFloat();
            float left = dataBuffer.getFloat();
            float right = dataBuffer.getFloat();

            System.out.printf("Object %d[label: %s, confidence: %f, (%f, %f, %f, %f)]\n", i, label, confidence, top, bottom, left, right);

            curResults.add(new ODResult(label, confidence, top, bottom, left, right));
        }

        try {
            odResults.put(curResults);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public float getCurrentLatency(){
        return this.currentLatency;
    }
}