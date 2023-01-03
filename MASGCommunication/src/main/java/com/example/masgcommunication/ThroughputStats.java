package com.example.masgcommunication;

public class ThroughputStats {
    public int totalByte;
    public double duration;
    public double totalThroughput;

    public boolean hasCurrentStat = false;
    public double currentThroughput;

    public ThroughputStats(int tb, double d, double tp) {
        totalByte = tb;
        duration = d;
        totalThroughput = tp;
    }
}
