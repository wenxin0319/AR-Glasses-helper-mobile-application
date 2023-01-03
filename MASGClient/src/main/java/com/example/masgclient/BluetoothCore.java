package com.example.masgclient;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;

import com.example.masgcommunication.ClientCommunication;
import com.example.masgcommunication.ClientLatencyStatsTransmission;
import com.example.masgcommunication.ClientLatencyTest;
import com.example.masgcommunication.Constant;
import com.example.masgcommunication.ODResult;
import com.example.masgcommunication.ThroughputTest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;
import android.os.Looper;

public class BluetoothCore {
    private static final UUID MY_UUID = UUID.fromString("52706509-65cd-4ace-8ab8-8f17da359990");

    private static BluetoothCore instance = new BluetoothCore();

    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private BluetoothSocket socket;

    private Handler mainHandler, throughputTestHandler, latencyTestHandler;

    private List<Double> latencyTestGraphData;

    private AcceptThread acceptThread;
    private ConnectedThread connectedThread;

    private ClientCommunication clientCommunication;

    private BluetoothCore(){}

    public static BluetoothCore getInstance() {
        return instance;
    }

    public void setMainHandler(Handler mainHandler) {
        this.mainHandler = mainHandler;
    }

    public void setThroughputTestHandler(Handler testHandler) { this.throughputTestHandler = testHandler; }

    public void setLatencyTestHandler(Handler testHandler) { this.latencyTestHandler = testHandler; }

    public void startListening() {
        if (acceptThread != null && acceptThread.isAlive()) return;
        acceptThread = new AcceptThread();
        acceptThread.start();
    }

    public void stopListening() {
        if (acceptThread != null && acceptThread.isAlive()) acceptThread.stopListening();
        acceptThread = null;
    }

    public void disconnect() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            socket = null;
            mainHandler.obtainMessage(Constant.BLUETOOTH_DISCONNECTED).sendToTarget();
            connectedThread = null;
        }
    }

    public void setClientCommunicationUIHandler(Handler UIHandler) {
        clientCommunication.setUIHandler(UIHandler);
    }

    public boolean enqueueEncodedFrame(byte[] data) {
        if (clientCommunication == null) {
            mainHandler.obtainMessage(Constant.MESSAGE_TOAST, "Server is not running. Cannot enqueue frame!")
                    .sendToTarget();
            return false;
        }
        clientCommunication.enqueueEncodedFrame(data);
        return true;
    }

    public List<ODResult> pollODResult() {
        if (clientCommunication == null) {
            mainHandler.obtainMessage(Constant.MESSAGE_TOAST, "Server is not running. Cannot enqueue frame!")
                    .sendToTarget();
            return null;
        }
        return clientCommunication.pollODResult();
    }

    public float getCurrentLatency() {
        if (clientCommunication == null) {
            mainHandler.obtainMessage(Constant.MESSAGE_TOAST, "Server stopped!")
                    .sendToTarget();
            return -1;
        }
        return clientCommunication.getCurrentLatency();
    }

    private class ConnectedThread extends Thread {
        InputStream inputStream;
        OutputStream outputStream;

        @Override
        public void run() {
            if (socket == null) {
                mainHandler.obtainMessage(Constant.MESSAGE_TOAST, "Not connected!").sendToTarget();
                return;
            }
            System.out.println("Connected");
            try {
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();

                byte[] controlMessageBuffer = new byte[1];

                while (!Thread.currentThread().isInterrupted()) {
                    int len = inputStream.read(controlMessageBuffer);

                    if (len == 0) continue;

                    switch (controlMessageBuffer[0]) {
                        case Constant.START_SERVER_HEADER:
                            outputStream.write(new byte[]{Constant.ACK_HEADER});
                            outputStream.flush();
                            if (clientCommunication != null) clientCommunication.cancel(false);
                            clientCommunication = new ClientCommunication(inputStream, outputStream, null);
                            clientCommunication.start();
                            mainHandler.obtainMessage(Constant.SERVER_RUNNING).sendToTarget();
                            clientCommunication.join();
                            mainHandler.obtainMessage(Constant.TEST_ENDED).sendToTarget();
                            if (clientCommunication.shouldDisconnect()) {
                                disconnect();
                                clientCommunication = null;
                                return;
                            }
                            clientCommunication = null;
                            break;
                        case Constant.START_THROUGHPUT_TEST_HEADER:
                            mainHandler.obtainMessage(Constant.THROUGHPUT_TEST_STARTED).sendToTarget();
                            outputStream.write(new byte[]{Constant.ACK_HEADER});
                            outputStream.flush();
                            ThroughputTest throughputTest = new ThroughputTest(inputStream, outputStream, throughputTestHandler);
                            throughputTest.start();
                            throughputTest.join();
                            mainHandler.obtainMessage(Constant.TEST_ENDED).sendToTarget();
                            if (throughputTest.shouldDisconnect()) {
                                disconnect();
                                return;
                            }
                            break;
                        case Constant.START_LATENCY_TEST_HEADER:
                            mainHandler.obtainMessage(Constant.LATENCY_TEST_STARTED).sendToTarget();
                            outputStream.write(new byte[]{Constant.ACK_HEADER});
                            outputStream.flush();
                            ClientLatencyTest clientLatencyTest = new ClientLatencyTest(inputStream, outputStream, latencyTestHandler);
                            clientLatencyTest.start();
                            clientLatencyTest.join();
                            latencyTestGraphData = clientLatencyTest.getGraphData();
                            mainHandler.obtainMessage(Constant.TEST_ENDED).sendToTarget();
                            if (clientLatencyTest.shouldDisconnect()) {
                                disconnect();
                                return;
                            }
                            break;
                        case Constant.START_LATENCY_STATS_TRANSMISSION_HEADER:
                            outputStream.write(new byte[]{Constant.ACK_HEADER});
                            outputStream.flush();

                            if (latencyTestGraphData == null) {
                                outputStream.write(new byte[]{Constant.END_COMMUNICATION_HEADER});
                                outputStream.flush();
                                // no graph data ready
                                byte[] endACKBuffer = new byte[1];
                                inputStream.read(endACKBuffer);
                                break;
                            }

                            ClientLatencyStatsTransmission clientLatencyStatsTransmission =
                                    new ClientLatencyStatsTransmission(inputStream, outputStream, latencyTestHandler, latencyTestGraphData);
                            clientLatencyStatsTransmission.start();
                            clientLatencyStatsTransmission.join();
                            if (clientLatencyStatsTransmission.shouldDisconnect()) {
                                disconnect();
                                return;
                            }
                            break;
                        case Constant.TERMINATE_CONNECTION_HEADER:
                            disconnect();
                            return;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private class AcceptThread extends Thread {
        BluetoothServerSocket serverSocket;

        @Override
        public void run() {
            if (connectedThread != null) {
                connectedThread.interrupt();
                connectedThread = null;
            }

            try {
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("masgclient", MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
                mainHandler.obtainMessage(Constant.MESSAGE_TOAST, "Cannot create server socket").sendToTarget();
                return;
            }
            System.out.println("Started Listening!");
            try {
                socket = serverSocket.accept();
                mainHandler.obtainMessage(Constant.BLUETOOTH_CONNECTED).sendToTarget();
                connectedThread = new ConnectedThread();
                connectedThread.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
            stopListening();
        }

        public void stopListening() {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                    System.out.println("Stopped Listening!");
                } catch (IOException e) {
                    e.printStackTrace();
                    mainHandler.obtainMessage(Constant.MESSAGE_TOAST, "Cannot close server socket").sendToTarget();
                }
            }
        }
    }
}