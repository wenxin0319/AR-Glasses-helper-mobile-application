package com.example.masgcommunication;

public class RunningStats {
    public int numFrames;
    public int totalBytes;
    public double currentThroughput;
    public double latency;

    public boolean hasCurrentStat;

    public RunningStats(int numFrames, int totalBytes) {
        this.numFrames = numFrames;
        this.totalBytes = totalBytes;
    }

    public RunningStats(int numFrames, int totalBytes, double latency) {
        this.numFrames = numFrames;
        this.totalBytes = totalBytes;
        this.latency = latency;
    }
}