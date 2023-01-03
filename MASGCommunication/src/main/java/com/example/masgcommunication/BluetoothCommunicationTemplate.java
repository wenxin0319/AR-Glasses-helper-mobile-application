package com.example.masgcommunication;

import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public abstract class BluetoothCommunicationTemplate extends Thread{
    InputStream inputStream;
    OutputStream outputStream;

    Handler UIHandler;

    volatile boolean finished, shouldDisconnect;

    public BluetoothCommunicationTemplate(InputStream inputStream, OutputStream outputStream, Handler UIHandler) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.UIHandler = UIHandler;
        this.finished = false;
        this.shouldDisconnect = false;
    }

    public void run() {
        sender.start();
        receiver.start();

        try {
            sender.join();
            receiver.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
            if (UIHandler != null)
                UIHandler.obtainMessage(Constant.MESSAGE_TOAST, "Thread interrupted!").sendToTarget();
        }
    }

    public void cancel(boolean shouldDisconnect) {
        if (this.finished) return;

        this.finished = true;
        this.shouldDisconnect = shouldDisconnect;

        try {
            sender.join();
            outputStream.write(new byte[]{Constant.END_COMMUNICATION_HEADER});
            outputStream.flush();
            receiver.join();
        } catch (IOException e) {
            e.printStackTrace();
            if (UIHandler != null)
                UIHandler.obtainMessage(Constant.MESSAGE_TOAST, "Cannot write end header.").sendToTarget();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setUIHandler(Handler UIHandler) { this.UIHandler = UIHandler; }

    public boolean shouldDisconnect() {
        return shouldDisconnect;
    }

    protected abstract void initializeSenderPerformanceMonitor();

    protected abstract void initializeReceiverPerformanceMonitor();

    protected abstract void updateSenderPerformanceStats(int dataLen);

    protected abstract void updateReceiverPerformanceStats(int dataLen);

    protected abstract byte[] getSenderData();

    protected abstract void handleReceivedData(ByteBuffer dataBuffer);

    private final Thread sender = new Thread(() -> {
        initializeSenderPerformanceMonitor();
        ByteBuffer buffer;

        while (!finished) {
            byte[] data = getSenderData();
            try {
                if (data == null) {
                    finished = true;
                    outputStream.write(new byte[]{Constant.END_COMMUNICATION_HEADER});
                    outputStream.flush();
                    return;
                }

                if (data.length == 0) continue;

                int dataLen = data.length;
                buffer = ByteBuffer.allocate(dataLen + 5);
                buffer.put(Constant.DATA_HEADER);
                buffer.putInt(dataLen);
                buffer.put(data);

                outputStream.write(buffer.array());

                updateSenderPerformanceStats(dataLen);
            } catch (IOException e) {
                e.printStackTrace();
                finished = true;
                shouldDisconnect = true;
                return;
            }
        }
    });

    private final Thread receiver = new Thread(() -> {
        initializeReceiverPerformanceMonitor();
        byte[] tempBuffer = new byte[Constant.RECEIVER_BUFFER_SIZE];
        int bytes;

        boolean currentDataIncomplete = false;
        int remainingBytes = 0;
        // use the offset to get the complete length bytes
        int offset = 0;

        ByteBuffer currentDataBuffer = null;

        while (true) {
            try {
                bytes = inputStream.read(tempBuffer, offset, Constant.RECEIVER_BUFFER_SIZE - offset);
            } catch (IOException e) {
                e.printStackTrace();
                finished = true;
                shouldDisconnect = true;
                return;
            }

            if (bytes == 0) continue;

            ByteBuffer receiverBuffer = ByteBuffer.wrap(tempBuffer, 0, bytes + offset);

            while (receiverBuffer.hasRemaining()) {
                if (!currentDataIncomplete) {
                    byte controlByte = receiverBuffer.get();

                    if (controlByte == Constant.END_COMMUNICATION_HEADER) {
                        finished = true;
                        // wait for sender to finish and then send confirmation
                        try {
                            sender.join();
                            outputStream.write(new byte[] {Constant.END_CONFIRMATION_HEADER});
                            outputStream.flush();
                            System.out.println("Sent end confirmation!");
                        } catch (InterruptedException | IOException e) {
                            e.printStackTrace();
                        }
                        return;
                    } else if (controlByte == Constant.END_CONFIRMATION_HEADER) {
                        System.out.println("Received confirmation! Stop receiver!");
                        return;
                    } else if (controlByte == Constant.TERMINATE_CONNECTION_HEADER) {
                        finished = true;
                        shouldDisconnect = true;
                        return;
                    } else if (controlByte != Constant.DATA_HEADER) {
                        System.out.printf("Received unrecognized data 0x%02X\n", controlByte);
                        if (UIHandler != null)
                            UIHandler.obtainMessage(Constant.MESSAGE_TOAST, "Received unrecognized data").sendToTarget();
                        finished = true;
                        return;
                    }

                    currentDataIncomplete = true;
                    remainingBytes = -1;
                }

                // complete data length has not been received
                if (remainingBytes == -1) {
                    if (receiverBuffer.remaining() < 4) {
                        int i = 0;
                        while (receiverBuffer.hasRemaining())
                            tempBuffer[i++] = receiverBuffer.get();
                        offset = i;
                        break;
                    } else {
                        remainingBytes = receiverBuffer.getInt();
                        currentDataBuffer = ByteBuffer.allocate(remainingBytes);
                        offset = 0;
                    }
                }

                // data length is retrieved and there are more data to read
                if (remainingBytes <= receiverBuffer.remaining()) {
                    currentDataBuffer.put(receiverBuffer.array(), receiverBuffer.position(), remainingBytes);
                    receiverBuffer.position(receiverBuffer.position() + remainingBytes);
                    remainingBytes = 0;
                    currentDataIncomplete = false;
                    int receivedDataLen = currentDataBuffer.capacity();
                    currentDataBuffer.rewind();
                    handleReceivedData(currentDataBuffer);
                    updateReceiverPerformanceStats(receivedDataLen);
                } else {
                    remainingBytes -= receiverBuffer.remaining();
                    currentDataBuffer.put(receiverBuffer.array(), receiverBuffer.position(), receiverBuffer.remaining());
                    receiverBuffer.position(receiverBuffer.position() + receiverBuffer.remaining());
                }
            }
        }
    });
}
