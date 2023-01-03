package com.example.masgcommunication;

public class LatencyStats {
    public int numSentPackets;
    public int numReceivedACKs;
    public int currentLatency;
    public double averageLatency;

    public boolean hasThroughputInfo;
    public double currentThroughput;

    public LatencyStats(int numSentPackets, int numReceivedACKs, int currentLatency, double averageLatency) {
        this.numSentPackets = numSentPackets;
        this.numReceivedACKs = numReceivedACKs;
        this.currentLatency = currentLatency;
        this.averageLatency = averageLatency;
    }
}
