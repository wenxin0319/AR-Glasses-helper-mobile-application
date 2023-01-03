package ucla.cs211.masgvideoprocessing;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class AVCEncoder {
    private final static String TAG = AVCEncoder.class.getSimpleName();
    private final static String MIME_TYPE = Constant.MIME_TYPE;
    private final static int I_FRAME_INTERVAL = Constant.I_FRAME_INTERVAL;

    MediaCodec mediaCodec;
    int width;
    int height;
    int timeoutUSec = 10000;
    long frameIndex = 0;
    byte[] yuv420 = null;
    int frameRate;
    int yStride;
    int cStride;
    int ySize;
    int cSize;
    int halfWidth;
    int halfHeight;

    // output
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    int inBuffer = 0;

    public AVCEncoder() {}

    public boolean init(int width, int height, int framerate, int bitrate) {
        try {
            mediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
        }
        catch (IOException e) {
            return false;
        }

        boolean isSupport = false;
        int colorFormat = 0;
        MediaCodecInfo codecInfo = selectCodec(MIME_TYPE);
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(MIME_TYPE);
        for (int i = 0; i < capabilities.colorFormats.length && colorFormat == 0; i++) {
            int format = capabilities.colorFormats[i];
            if (format == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) {
                isSupport = true;
                break;
            }
        }
        if (!isSupport)
            return false;

        this.width  = width;
        this.height = height;
        this.halfWidth = width / 2;
        this.halfHeight = height / 2;
        this.frameRate = framerate;

        this.yStride = (int) Math.ceil(width/16.0f) * 16;
        this.cStride = (int) Math.ceil(width/32.0f)  * 16;
        this.ySize = yStride * height;
        this.cSize = cStride * height / 2;

        this.yuv420 = new byte[width*height*3/2];

        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, framerate);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL);

        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mediaCodec.start();
        return true;
    }

    public void close() {
        if (mediaCodec == null) return;
        try {
            mediaCodec.stop();
            mediaCodec.release();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public byte[] offerEncoder(byte[] input) {
        YV12toYUV420PackedSemiPlanar(input, yuv420, width, height);
        //Log.d("Encoder", "YUV420 Semi Planar size is: " + yuv420.length);
        try {
            ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
            ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
            int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
            if (inputBufferIndex >= 0) {
                long pts = computePresentationTime(frameIndex, frameRate);
                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();
                inputBuffer.put(yuv420, 0, yuv420.length);
                mediaCodec.queueInputBuffer(inputBufferIndex, 0, yuv420.length, pts, 0);
                frameIndex++;
                inBuffer++;
            }

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, timeoutUSec);

            while (outputBufferIndex >= 0) {
                inBuffer--;

                ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                byte[] outData = new byte[bufferInfo.size];
                outputBuffer.get(outData);

                outputStream.write(outData);

                mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, timeoutUSec);
            }

        }
        catch (Throwable t) {
            t.printStackTrace();
        }
        byte[] ret = outputStream.toByteArray();
        outputStream.reset();
        return ret;
    }

    private byte[] YV12toYUV420PackedSemiPlanar(final byte[] input, final byte[] output, final int width, final int height) {
        for (int i=0; i<height; i++)
            System.arraycopy(input, yStride*i, output, yStride*i, yStride); // Y

        for (int i=0; i<halfHeight; i++) {
            for (int j=0; j<halfWidth; j++) {
                output[ySize + (i*halfWidth + j)*2] = input[ySize + cSize + i*cStride + j]; // Cb (U)
                output[ySize + (i*halfWidth + j)*2 + 1] = input[ySize + i*cStride + j]; // Cr (V)
            }
        }
        return output;
    }


    private static MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            if (!codecInfo.isEncoder())
                continue;

            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType))
                    return codecInfo;
            }
        }
        return null;
    }

    private long computePresentationTime(long frameIndex, int framerate) {
        return 132 + frameIndex * 1000000 / framerate;
    }

    public int getInBufferCount(){
        return inBuffer;
    }
}
