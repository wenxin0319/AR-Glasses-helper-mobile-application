package com.example.masgserver;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.masgcommunication.Constant;
import com.example.masgcommunication.DataFrame;
import com.example.masgcommunication.DecoderStats;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import ucla.cs211.masgvideoprocessing.AVCDecoder;

import com.example.masgcommunication.ImageFrame;
import com.example.masgobjectdetection.MyObjectDetection;

public class DecodeViewActivity extends AppCompatActivity {
    private final static String TAG = DecodeViewActivity.class.getSimpleName();

    Context mContext;
    private volatile boolean isServerRunning;

    // Decoder
    AVCDecoder decoder;
    SurfaceView decodeSurfacePreview;
    TextView receivedFrameView, receivedFrameRateView, downStreamBWView;
    BlockingQueue<DataFrame> decodedFrames;

    private final int SP_CAM_WIDTH = Constant.CAM_WIDTH;
    private final int SP_CAM_HEIGHT = Constant.CAM_HEIGHT;

    // Access bluetooth socket
    BluetoothCore bluetoothCore;

    // Monitor variables
    long startTime;
    int frameCount;
    int totalSize;

    // Object Detection
    MyObjectDetection objectDetection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.decode_view);
        this.mContext = this;
        this.isServerRunning = true;

        bluetoothCore = BluetoothCore.getInstance();

        objectDetection = new MyObjectDetection(bluetoothCore.getODResultsQueue(), Constant.CAM_WIDTH, Constant.CAM_HEIGHT);

        decodeSurfacePreview = findViewById(R.id.decodePreview);
        decodeSurfacePreview.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
                surfaceHolder.setFixedSize(SP_CAM_WIDTH/2, SP_CAM_HEIGHT/2);
                decodedFrames = new LinkedBlockingQueue<>();
                decoder = new AVCDecoder();
                decoder.init(SP_CAM_WIDTH, SP_CAM_HEIGHT, surfaceHolder.getSurface(), decodedFrames);
                startTime = System.currentTimeMillis();
                frameCount = 0;
                totalSize = 0;
                decodeThread.start();
                objectDetectionThread.start();
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
                decoder.close();
            }
        });

        receivedFrameView = this.findViewById(R.id.ReceivedFrame);
        receivedFrameRateView = this.findViewById(R.id.ReceivedFrameRate);
        downStreamBWView = this.findViewById(R.id.DownStreamBW);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        decodeThread.interrupt();
        objectDetectionThread.interrupt();
        this.isServerRunning = false;
    }

    @SuppressLint("HandlerLeak")
    Handler mHandler = new Handler() {
        @SuppressLint({"SetTextI18n", "DefaultLocale"})
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case Constant.DECODER_STATS:
                    DecoderStats stats = (DecoderStats)msg.obj;
                    if (stats != null){
                        receivedFrameView.setText("Received: "+frameCount+"Packets");
                        receivedFrameRateView.setText("Received: "+String.format("%,.2f", stats.receivedFR)+"fps");
                        downStreamBWView.setText("Downstream BW: "+ String.format("%,.2f", stats.receivedBW)+"KBps");
                    }
            }
        }
    };

    Thread decodeThread = new Thread(new Runnable() {
        @Override
        public void run() {
            // TODO: Setting decode view on top, remove when release
            decodeSurfacePreview.setZOrderOnTop(true);

            // Get Data from socket
            while (isServerRunning) {
                try {
                    DataFrame currentDataFrame = bluetoothCore.pollEncodedFrame();

                    if (currentDataFrame == null) continue;

                    frameCount++;
                    totalSize+=currentDataFrame.data.length;
                    if (Constant.VERBOSE) {
                        Log.d(TAG, "Get encoded frame, size = " + currentDataFrame.data.length);
                        long currentTime = System.currentTimeMillis();
                        double duration = ((double)(currentTime - startTime)) / 1000;
                        double receivedFR = frameCount/duration;
                        double receivedBW = (totalSize/duration) / 1024;      // measured in KBps
                        DecoderStats decoderStats = new DecoderStats(receivedFR, receivedBW);
                        mHandler.obtainMessage(Constant.DECODER_STATS, decoderStats).sendToTarget();
                    }

                    decoder.offerDecoder(currentDataFrame);
                    Log.d(TAG, "decoder buffer size: " + decoder.getBufferCount());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
    });

    Thread objectDetectionThread = new Thread(new Runnable() {
        @Override
        public void run() {
            while (isServerRunning) {
                try {
                    DataFrame frame = decodedFrames.poll(100, TimeUnit.MILLISECONDS);
                    if (frame != null) {
                        // NV12 to NV21
                        for (int i = 0; i < frame.data.length; i += 2) {
                            if (i >= SP_CAM_WIDTH * SP_CAM_HEIGHT) {
                                byte tmp = frame.data[i];
                                frame.data[i] = frame.data[i+1];
                                frame.data[i+1] = tmp;
                            }
                        }

                        objectDetection.detect(frame.seq, frame.data);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    });
}
