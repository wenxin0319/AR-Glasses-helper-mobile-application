package com.example.masgclient;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.masgcommunication.Constant;
import com.example.masgcommunication.ODResult;
import com.example.masgcommunication.RunningStats;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import ucla.cs211.masgvideoprocessing.AVCEncoder;

public class StreamActivity extends AppCompatActivity implements SurfaceHolder.Callback, Camera.PreviewCallback{
    private final static String TAG = StreamActivity.class.getSimpleName();

    private static int SP_CAM_WIDTH = 0;
    private static int SP_CAM_HEIGHT = 0;

    private final static int DEFAULT_FRAME_RATE = 15;
    private final static int DEFAULT_BIT_RATE = 500000;

    Camera camera;
    Context mContext;

//    Bitmap bitmap;
//    Canvas canvas;
    SurfaceHolder previewHolder;
    SurfaceView svCameraPreview;
    ImageView drawobject;

    // SurfaceTexture svCameraTexture;
    byte[] previewBuffer;
    boolean isStreaming = false;
    Random r = new Random();
    AVCEncoder encoder;

    // Bluetooth connection buffer
    BluetoothCore bluetoothCore;

    // Monitor parameters
    long startTime = -1;
    int encodedSize;
    int frameCount;
    int onPreviewCount;

    // Debug
    TextView textView;
    TextView averageSizeView;
    TextView encRateView;
    TextView onPreviewRateView;
    TextView upstreamThroughput;
    TextView numSentFrames;
    TextView sentBytes;
    TextView downstreamThroughput;
    TextView numReceivedFrames;
    TextView receivedBytes;
    TextView runningLatency;

//    private static final float TEXT_SIZE = 54.0f;
//    private static final float STROKE_WIDTH = 4.0f;
//    private static final int NUM_COLORS = 10;
//    private static final int[][] COLORS =
//            new int[][] {
//                    // {Text color, background color}
//                    {Color.BLACK, Color.WHITE},
//                    {Color.WHITE, Color.MAGENTA},
//                    {Color.BLACK, Color.LTGRAY},
//                    {Color.WHITE, Color.RED},
//                    {Color.WHITE, Color.BLUE},
//                    {Color.WHITE, Color.DKGRAY},
//                    {Color.BLACK, Color.CYAN},
//                    {Color.BLACK, Color.YELLOW},
//                    {Color.WHITE, Color.BLACK},
//                    {Color.BLACK, Color.GREEN}
//            };
//
//    int numColors = COLORS.length;
//    private Paint[] boxPaints = new Paint[numColors];
//    private Paint[] textPaints = new Paint[numColors];
//    private Paint[] labelPaints = new Paint[numColors];
//
//    for(int i = 0; i < numColors; i++) {
//        textPaints[i] = new Paint();
//        textPaints[i].setColor(COLORS[i][0] /* text color */);
//        textPaints[i].setTextSize(TEXT_SIZE);
//
//        boxPaints[i] = new Paint();
//        boxPaints[i].setColor(COLORS[i][1] /* background color */);
//        boxPaints[i].setStyle(Paint.Style.STROKE);
//        boxPaints[i].setStrokeWidth(STROKE_WIDTH);
//
//        labelPaints[i] = new Paint();
//        labelPaints[i].setColor(COLORS[i][1] /* background color */);
//        labelPaints[i].setStyle(Paint.Style.FILL);
//    }
//
//



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.activity_stream);

        mContext = this;

        this.svCameraPreview = (SurfaceView) this.findViewById(R.id.svCameraPreview);
        //this.svCameraTexture = new SurfaceTexture(10);       // Hide preview view
        this.previewHolder = svCameraPreview.getHolder();
        this.previewHolder.addCallback(this);
        this.drawobject = this.findViewById(R.id.drawobjects);


        textView = this.findViewById(R.id.textView);
        averageSizeView =  this.findViewById(R.id.textView2);
        encRateView = this.findViewById(R.id.textView4);
        onPreviewRateView = this.findViewById(R.id.textView3);

        upstreamThroughput = findViewById(R.id.running_upstream_throughput);
        numSentFrames = findViewById(R.id.running_num_sent_frame);
        sentBytes = findViewById(R.id.running_sent_bytes);
        downstreamThroughput = findViewById(R.id.running_downstream_throughput);
        numReceivedFrames = findViewById(R.id.running_num_received_frame);
        receivedBytes = findViewById(R.id.running_received_bytes);
        runningLatency = findViewById(R.id.running_latency);

        if (!Constant.VERBOSE){
            textView.setVisibility(View.INVISIBLE);
            averageSizeView.setVisibility(View.INVISIBLE);
            encRateView.setVisibility(View.INVISIBLE);
            onPreviewRateView.setVisibility(View.INVISIBLE);
        }

        this.bluetoothCore = BluetoothCore.getInstance();
        bluetoothCore.setClientCommunicationUIHandler(mHandler);
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        // Camera size width: 1920, height 932
        this.camera.addCallbackBuffer(this.previewBuffer);

        if (startTime == -1){
            startTime = System.currentTimeMillis();
            encodedSize = 0;
            frameCount = 0;
            onPreviewCount = 0;
        }

        long currentTime = System.currentTimeMillis();
        double duration = ((double)(currentTime - startTime)) / 1000;
        onPreviewCount++;

        // Congestion Control
        if (!isAllowedToEncode()){
            return;
        }

        if (this.isStreaming){
            byte[] encData = this.encoder.offerEncoder(bytes);

            encodedSize += encData.length;
            double avgPreviewCount = onPreviewCount / duration;
            double requiredBW = (encodedSize / duration) / 1024;   //Measured in KBps

            if (Constant.VERBOSE){
                Log.i(TAG, "streaming; byte size = " + bytes.length + " --> Encoded size = " + encData.length);
                Log.i(TAG, "Required Bandwidth is: " + String.format("%,.2f", requiredBW));
                textView.setText(String.format("%,.2f", requiredBW)+"KB/s");
                onPreviewRateView.setText("Preview:"+String.format("%,.2f", avgPreviewCount)+"/s");
            }

            if (encData.length > 0){
                frameCount++;
                double avgSize = encodedSize/frameCount;
                double avgEncDataRate = frameCount/duration;
                if (Constant.VERBOSE){
                    averageSizeView.setText(String.format("%,.2f", avgSize)+"bytes");
                    encRateView.setText("Encoded: "+ String.format("%,.2f", avgEncDataRate)+"/s");
                }

                // enqueue data to client communication blocking queue
                boolean rs = bluetoothCore.enqueueEncodedFrame(encData);
                if (!rs) {
                    stopCamera();
                    finish();
                }
            }

        }
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
        if (!isStreaming){
            startCamera(surfaceHolder);
            startStream();
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {}

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
        if (isStreaming){
            stopCamera();
            stopStream();
        }
    }

    private void startCamera(SurfaceHolder surfaceHolder) {
        if (SP_CAM_WIDTH == 0) {
            Camera tmpCam = Camera.open();
            Camera.Parameters params = tmpCam.getParameters();
            final List<Camera.Size> prevSizes = params.getSupportedPreviewSizes();

            // Get suitable prevSize
            for (Camera.Size previewSize : prevSizes){
                System.out.println(previewSize.width + " , " + previewSize.height);
                if (previewSize.height == Constant.CAM_HEIGHT && previewSize.width == Constant.CAM_WIDTH){
                    SP_CAM_WIDTH = previewSize.width;
                    SP_CAM_HEIGHT = previewSize.height;
                    break;
                }
            }

            Log.i(TAG, "width is: "+SP_CAM_HEIGHT);
            Log.i(TAG, "height is: "+SP_CAM_WIDTH);

            tmpCam.release();
            tmpCam = null;
        }

        // Preview holder need swap the height and width because of the orientation
        this.previewHolder.setFixedSize(SP_CAM_HEIGHT, SP_CAM_WIDTH);
        //this.previewHolder.setFixedSize(SP_CAM_WIDTH, SP_CAM_HEIGHT);
//        this.drawobject.setLayoutParams((new ImageView.LayoutParams(SP_CAM_HEIGHT, SP_CAM_WIDTH));
        Log.d(TAG, "previewHolder: width is: "+SP_CAM_HEIGHT + " height is: " + SP_CAM_WIDTH);


        int stride = (int) Math.ceil(SP_CAM_WIDTH/16.0f) * 16;
        int cStride = (int) Math.ceil(SP_CAM_WIDTH/32.0f)  * 16;
        final int frameSize = stride * SP_CAM_HEIGHT;
        final int qFrameSize = cStride * SP_CAM_HEIGHT / 2;

//        this.bitmap = Bitmap.createBitmap(SP_CAM_WIDTH,SP_CAM_HEIGHT, Bitmap.Config.ARGB_8888);
//        this.canvas = new Canvas(bitmap);

        this.previewBuffer = new byte[(frameSize + qFrameSize * 2) * 10];

        try {
            camera = Camera.open();
//            camera.setDisplayOrientation(90);
            camera.setPreviewDisplay(surfaceHolder);
//            if (Constant.ENABLE_PREVIEW){
//                camera.setPreviewDisplay(this.previewHolder);
//            }else{
//                camera.setPreviewTexture(this.svCameraTexture);
//            }

            Camera.Parameters params = camera.getParameters();
            params.setPreviewSize(SP_CAM_WIDTH, SP_CAM_HEIGHT);
            params.setPreviewFormat(ImageFormat.YV12);
            camera.setParameters(params);
            camera.addCallbackBuffer(previewBuffer);
            camera.setPreviewCallbackWithBuffer(this);
            camera.startPreview();

            DrawodThread.start();
        }
        catch (IOException e) {
            Log.e(TAG, String.valueOf(e));
        }
        catch (RuntimeException e) {
            Log.e(TAG, String.valueOf(e));
        }
    }

    private void stopCamera() {
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    private void startStream() {
        this.encoder = new AVCEncoder();
        Log.d(TAG, "SP_CAM_WIDTH: " + SP_CAM_WIDTH + " SP_CAM_HEIGHT: " + SP_CAM_HEIGHT);
        this.encoder.init(SP_CAM_WIDTH, SP_CAM_HEIGHT, DEFAULT_FRAME_RATE, DEFAULT_BIT_RATE);
        this.isStreaming = true;
    }

    private void stopStream(){
        encoder.close();
        startTime = -1;
        this.isStreaming = false;
    }

    private boolean isAllowedToEncode(){
        long currentTime = System.currentTimeMillis();
        double duration = ((double)(currentTime - startTime)) / 1000;

        // Stabilize frame rate
        if (frameCount/duration > DEFAULT_FRAME_RATE){
            int coin = r.nextInt((int) (onPreviewCount/duration)+1);
            if (coin >= DEFAULT_FRAME_RATE-1){
                return false;
            }
        }
//        Log.d("encoder", String.valueOf(encoder.getInBufferCount()));

        // Monitor encoder buffer
        if (encoder != null && encoder.getInBufferCount() > Constant.MAX_ENCODER_BUFFER_ALLOWED){
            return false;
        }

        // Congestion control on latency
        float latency = bluetoothCore.getCurrentLatency();
        float p = getProbabilityFromLatency(latency);
        Log.d("CC", "Current Latency = " + latency + "; prob = " + p);
        if (r.nextDouble() > p){
            Log.d("CC", "Dropping frame ...");
            return false;
        }

        return true;
    }

    private float getProbabilityFromLatency(double latency){
        return latency <= Constant.BETA ? 1 : (float) Math.exp((-1 * (latency - Constant.BETA)) / Constant.ALPHA);
    }

    private Handler mHandler = new Handler(msg -> {
        switch (msg.what) {
            case Constant.CLIENT_RUNNING_UPSTREAM_STATS:
                RunningStats stats = (RunningStats) msg.obj;
                if (stats == null) break;
                numSentFrames.setText(String.valueOf(stats.numFrames));
                sentBytes.setText(String.valueOf(stats.totalBytes));
                if (stats.hasCurrentStat) upstreamThroughput.setText(String.format("%.2f", stats.currentThroughput));
                break;
            case Constant.CLIENT_RUNNING_DOWNSTREAM_STATS:
                stats = (RunningStats) msg.obj;
                if (stats == null) break;
                numReceivedFrames.setText(String.valueOf(stats.numFrames));
                receivedBytes.setText(String.valueOf(stats.totalBytes));
                runningLatency.setText(String.valueOf(stats.latency));
                if (stats.hasCurrentStat) downstreamThroughput.setText(String.format("%.2f", stats.currentThroughput));
        }
        return true;
    });

    private final int[] COLOR_MAP = {
            Color.RED,
            Color.GREEN,
            Color.YELLOW,
            Color.BLUE,
            Color.CYAN
    };

    Thread DrawodThread = new Thread(new Runnable() {
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Paint boxPaints = new Paint();
                    boxPaints.setColor(Color.WHITE /* background color */);
                    boxPaints.setStyle(Paint.Style.STROKE);
                    boxPaints.setStrokeWidth(4.0f);

                    Paint textPaints = new Paint();
                    textPaints.setColor(Color.WHITE /* background color */);
                    textPaints.setTextSize(50.0f);

                    String LABEL_FORMAT = "%s";
                    List<ODResult> results = bluetoothCore.pollODResult();
                    // Log.d("poll od result", results.label);
                    if (results != null) {
                        Bitmap bitmap = Bitmap.createBitmap(SP_CAM_WIDTH,SP_CAM_HEIGHT, Bitmap.Config.ARGB_8888);
                        Canvas canvas = new Canvas(bitmap);
                        int idx = 0;
                        for (ODResult result : results) {
                            Log.d("poll od result", result.label);
                            boxPaints.setColor(COLOR_MAP[idx]);
                            textPaints.setColor(COLOR_MAP[idx++]);
                            canvas.drawRect(result.left, result.top, result.right, result.bottom, boxPaints);
                            canvas.drawText(
                                    String.format(Locale.US, LABEL_FORMAT, result.label),
                                    result.left,
                                    result.bottom - 4.0f,
                                    textPaints);
                        }
                        drawobject.setImageBitmap(bitmap);

                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    });
};