package com.example.masgserver;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;

import com.example.masgcommunication.BluetoothCommunicationTemplate;
import com.example.masgcommunication.Constant;
import com.example.masgcommunication.DataFrame;
import com.example.masgcommunication.ServerCommunication;
import com.example.masgcommunication.ServerLatencyStatsTransmission;
import com.example.masgcommunication.ServerLatencyTest;
import com.example.masgcommunication.ThroughputTest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;

@SuppressLint("MissingPermission")
public class BluetoothCore {
    private static final UUID MY_UUID = UUID.fromString("52706509-65cd-4ace-8ab8-8f17da359990");

    private static final BluetoothCore instance = new BluetoothCore();

    private final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private Handler connectionFragHandler, testFragHandler;

    private BluetoothSocket socket;

    private AcceptThread acceptThread;

    private ServerCommunication serverCommunication;

    private ThroughputTest throughputTest;

    private ServerLatencyTest serverLatencyTest;

    private ServerLatencyStatsTransmission serverLatencyStatsTransmission;

    private BluetoothCore() {}

    public static BluetoothCore getInstance() {
        return instance;
    }

    public void setConnectionFragHandler(Handler cfHandler) {
        connectionFragHandler = cfHandler;
    }

    public void setTestFragHandler(Handler tfHandler) {
        testFragHandler = tfHandler;
    }

    public void startListening() {
        if (acceptThread == null) {
            acceptThread = new AcceptThread();
            acceptThread.start();
        }
        System.out.println("Started Listening!");
    }

    public synchronized void stopListening() {
        if (acceptThread != null) {
            acceptThread.closeServerSocket();
            acceptThread = null;
        }
        System.out.println("Stopped Listening!");
    }

    public synchronized void connect(BluetoothDevice device) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
                connectionFragHandler.obtainMessage(
                    Constant.MESSAGE_TOAST,
                    "Cannot close current connection."
                ).sendToTarget();
            }
        }

        try {
            socket = device.createRfcommSocketToServiceRecord(MY_UUID);
            socket.connect();
        } catch (IOException e) {
            e.printStackTrace();
            connectionFragHandler.obtainMessage(
                Constant.MESSAGE_TOAST,
                String.format("Cannot connect to %s", device.getName())
            ).sendToTarget();
            connectionFragHandler.obtainMessage(Constant.BLUETOOTH_DISCONNECTED).sendToTarget();
            return;
        }

        System.out.println("Debug: is connected!");

        mBluetoothAdapter.cancelDiscovery();
        stopListening();
        connectionFragHandler.obtainMessage(Constant.BLUETOOTH_CONNECTED, device.getName()).sendToTarget();
    }

    public synchronized void disconnect() {
        System.out.println("Terminate the connection now!");
        if (socket == null) return;

        stopThroughputTest(true);
        stopLatencyTest(true);

        try {
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(Constant.TERMINATE_CONNECTION_HEADER);
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
            connectionFragHandler.obtainMessage(Constant.MESSAGE_TOAST, "Cannot close current connection.").sendToTarget();
            return;
        }

        socket = null;
        connectionFragHandler.obtainMessage(Constant.BLUETOOTH_DISCONNECTED).sendToTarget();
    }

    public DataFrame pollEncodedFrame() throws InterruptedException {
        if (serverCommunication == null) {
            testFragHandler.obtainMessage(Constant.MESSAGE_TOAST, "Server is not running. Cannot fetch frame!")
                    .sendToTarget();
            return null;
        }

        byte[] receivedData = serverCommunication.pollEncodedFrame();

        if (receivedData == null) return null;

        ByteBuffer encodedFrameBuffer = ByteBuffer.wrap(receivedData);
        int frameSeq = encodedFrameBuffer.getInt();
        byte[] encodedFrame = new byte[encodedFrameBuffer.remaining()];
        encodedFrameBuffer.get(encodedFrame);

        return new DataFrame(frameSeq,encodedFrame);
    }

    public BlockingQueue<byte[]> getODResultsQueue() {
        if (serverCommunication == null) {
            testFragHandler.obtainMessage(Constant.MESSAGE_TOAST, "Server is not running. Cannot fetch frame!")
                .sendToTarget();
            return null;
        }
        return serverCommunication.getODResultsQueue();
    }

    public void startServer() {
        startCommunication(Constant.START_SERVER_HEADER);
    }

    public void stopServer(boolean shouldDisconnect) {
        stopCommunication(Constant.START_SERVER_HEADER, shouldDisconnect);
    }

    public void startThroughputTest() {
        startCommunication(Constant.START_THROUGHPUT_TEST_HEADER);
    }

    public void stopThroughputTest(boolean shouldDisconnect) {
        stopCommunication(Constant.START_THROUGHPUT_TEST_HEADER, shouldDisconnect);
    }

    public void startLatencyTest() {
       startCommunication(Constant.START_LATENCY_TEST_HEADER);
    }

    public void stopLatencyTest(boolean shouldDisconnect) {
        stopCommunication(Constant.START_LATENCY_TEST_HEADER, shouldDisconnect);
    }

    public List<Number> getLatencyGraphData() {
        startCommunication(Constant.START_LATENCY_STATS_TRANSMISSION_HEADER);
        try {
            serverLatencyStatsTransmission.join();
            return serverLatencyStatsTransmission.getGraphData();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return new ArrayList<>();
    }

    private void startCommunication(byte initHeader) {
        if (serverCommunication != null) {
            serverCommunication.cancel(false);
            serverCommunication = null;
        }
        if (throughputTest != null) {
            throughputTest.cancel(false);
            throughputTest = null;
        }
        if (serverLatencyTest != null) {
            serverLatencyTest.cancel(false);
            serverLatencyTest = null;
        }
        if (serverLatencyStatsTransmission != null) {
            serverLatencyStatsTransmission.cancel(false);
            serverLatencyStatsTransmission = null;
        }

        try {
            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();

            outputStream.write(new byte[]{initHeader});
            outputStream.flush();

            byte[] ackBuffer = new byte[1];
            int len = inputStream.read(ackBuffer);
            if (len == 0 || ackBuffer[0] != Constant.ACK_HEADER) {
                testFragHandler.obtainMessage(Constant.MESSAGE_TOAST, "Did not receive ack").sendToTarget();
                return;
            }

            if (initHeader == Constant.START_SERVER_HEADER) {
                serverCommunication = new ServerCommunication(inputStream, outputStream, testFragHandler);
                serverCommunication.start();
            } else if (initHeader == Constant.START_THROUGHPUT_TEST_HEADER) {
                throughputTest = new ThroughputTest(inputStream, outputStream, testFragHandler);
                throughputTest.start();
            } else if (initHeader == Constant.START_LATENCY_TEST_HEADER) {
                serverLatencyTest = new ServerLatencyTest(inputStream, outputStream, testFragHandler);
                serverLatencyTest.start();
            } else if (initHeader == Constant.START_LATENCY_STATS_TRANSMISSION_HEADER) {
                serverLatencyStatsTransmission = new ServerLatencyStatsTransmission(inputStream, outputStream, testFragHandler);
                serverLatencyStatsTransmission.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
            testFragHandler.obtainMessage(Constant.MESSAGE_TOAST, e.getMessage()).sendToTarget();
        }
    }

    private void stopCommunication(byte initHeader, boolean shouldDisconnect) {
        BluetoothCommunicationTemplate communication = null;

        switch (initHeader) {
            case Constant.START_SERVER_HEADER:
                communication = serverCommunication;
                break;
            case Constant.START_THROUGHPUT_TEST_HEADER:
                communication = throughputTest;
                break;
            case Constant.START_LATENCY_TEST_HEADER:
                communication = serverLatencyTest;
                break;
            case Constant.START_LATENCY_STATS_TRANSMISSION_HEADER:
                communication = serverLatencyStatsTransmission;
                break;
        }

        if (communication != null) communication.cancel(shouldDisconnect);
    }

    private class AcceptThread extends Thread {
        private BluetoothServerSocket serverSocket;

        public AcceptThread() {
            try {
                serverSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(
                    mBluetoothAdapter.getName(),
                    MY_UUID
                );
            } catch (IOException e) {
                e.printStackTrace();
                connectionFragHandler.obtainMessage(
                    Constant.MESSAGE_TOAST,
                    "Cannot create server socket to listen for connections."
                ).sendToTarget();
            }
        }

        @Override
        public void run() {
            synchronized (this) {
                if (socket != null) {
                    if (socket.isConnected()) return;
                    else {
                        socket = null;
                        connectionFragHandler.obtainMessage(Constant.BLUETOOTH_DISCONNECTED).sendToTarget();
                    }
                }
            }

            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            closeServerSocket();

            connectionFragHandler.obtainMessage(Constant.BLUETOOTH_CONNECTED, socket.getRemoteDevice().getName()).sendToTarget();
        }

        private void closeServerSocket() {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
                connectionFragHandler.obtainMessage(
                    Constant.MESSAGE_TOAST,
                    "Cannot turn off server socket."
                ).sendToTarget();
            }
        }
    }
}
