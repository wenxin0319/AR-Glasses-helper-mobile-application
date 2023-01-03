package com.example.masgcommunication;

public class Constant {
    public static final int BLUETOOTH_LISTENING = 1;
    public static final int BLUETOOTH_CONNECTED = 2;
    public static final int THROUGHPUT_TEST_STARTED = 3;
    public static final int LATENCY_TEST_STARTED = 4;
    public static final int TEST_ENDED = 5;
    public static final int BLUETOOTH_DISCONNECTED = 6;
    public static final int SERVER_RUNNING = 7;
    public static final int SERVER_STOPPED = 8;
    public static final int MESSAGE_TOAST = -1;

    // for running server
    public static final int SERVER_RUNNING_DOWNSTREAM_STATS = 1;
    public static final int SERVER_RUNNING_UPSTREAM_STATS = 2;
    public static final int CLIENT_RUNNING_DOWNSTREAM_STATS = 3;
    public static final int CLIENT_RUNNING_UPSTREAM_STATS = 4;
    // for throughput test stats
    public static final int THROUGHPUT_UPSTREAM_STATS = 3;
    public static final int THROUGHPUT_DOWNSTREAM_STATS = 4;
    // for latency test stats
    public static final int SERVER_LATENCY_UPSTREAM_STATS = 7;
    public static final int SERVER_LATENCY_DOWNSTREAM_STATS = 8;
    public static final int CLIENT_LATENCY_UPSTREAM_STATS = 9;
    public static final int CLIENT_LATENCY_DOWNSTREAM_STATS = 10;
    // for running server decoder stats
    public static final int DECODER_STATS = 1;

    public static final int RECEIVER_BUFFER_SIZE = 1024;

    public static final byte START_THROUGHPUT_TEST_HEADER = 0x01;
    public static final byte START_LATENCY_TEST_HEADER = 0x02;
    public static final byte START_LATENCY_STATS_TRANSMISSION_HEADER = 0x03;
    public static final byte END_COMMUNICATION_HEADER = 0x04;
    public static final byte START_SERVER_HEADER = 0x07;
    public static final byte END_CONFIRMATION_HEADER = 0x08;
    public static final byte TERMINATE_CONNECTION_HEADER = 0x10;
    public static final byte DATA_HEADER = 0x20;
    public static final byte ACK_HEADER = 0x40;

    public static final byte FORGET_DEVICE_RESULT_CODE = -1;

    public static final String SENDER_THROUGHPUT_KEY = "sender throughput";
    public static final String RECEIVER_THROUGHPUT_KEY = "receiver_throughput";
    public static final String LATENCY_GRAPH_DATA_KEY = "latency graph data";

    // Debug
    public final static boolean VERBOSE = true;
    public final static boolean ENABLE_PREVIEW = true;

    // Congestion Control
    public final static int MAX_ENCODER_BUFFER_ALLOWED = 5;
    public final static double GAMMA = 0.7;
    public final static int ALPHA = 150;    // exponential rate
    public final static int BETA = 100;     // When to start cc

    // Camera size
    public final static int CAM_WIDTH = 640;
    public final static int CAM_HEIGHT = 480;
}
