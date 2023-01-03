package ucla.cs211.masgvideoprocessing;

import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import com.example.masgcommunication.DataFrame;
import com.example.masgcommunication.ImageFrame;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;

public class AVCDecoder {
    private final static String TAG = AVCDecoder.class.getSimpleName();
    private final static String MIME_TYPE = Constant.MIME_TYPE;
    private final static int TIMEOUT_USEC = Constant.TIMEOUT_USEC;     // TIMEOUT for inactive buffer

    MediaCodec decoder;
    MediaFormat format;

    BlockingQueue<DataFrame> decodedFrames;
    private Surface surface;
    int count = 0;

    public AVCDecoder() {}

    public boolean init(int mWidth, int mHeight, Surface surface, BlockingQueue<DataFrame> decodedFrames){
        try{
            decoder = MediaCodec.createDecoderByType(MIME_TYPE);
        } catch (IOException e) {
            return false;
        }

        this.decodedFrames = decodedFrames;

        format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 500000);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        this.surface = surface;
        // decoder.configure(format, this.surface, null, 0);   // Enable preview
        decoder.configure(format, null, null, 0);   // disable preview
        Log.i(TAG, "Output color format " + decoder.getOutputFormat().getInteger(MediaFormat.KEY_COLOR_FORMAT));

        decoder.start();

        return true;
    }

    public void close() {
        if (decoder == null) return;
        try {
            decoder.stop();
            decoder.release();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void offerDecoder(DataFrame frame) {        // Decode format: YUV420SemiPlanar
        if (this.surface == null){
            Log.d(TAG, "null surface");
        }

        try {
            byte[] data = frame.data;
            // put current data in queue
            ByteBuffer[] decoderInputBuffers = decoder.getInputBuffers();
            ByteBuffer[] decoderOutputBuffers = decoder.getOutputBuffers();
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

            int inputBufIndex = decoder.dequeueInputBuffer(-1);
            count++;

            ByteBuffer inputBuf = decoderInputBuffers[inputBufIndex];
            inputBuf.clear();
            inputBuf.put(data);
            decoder.queueInputBuffer(inputBufIndex, 0, data.length, 0, 0);

            int decoderStatus = decoder.dequeueOutputBuffer(info, TIMEOUT_USEC);

            while (decoderStatus >= 0) {
                count--;
                ByteBuffer outputFrame = decoderOutputBuffers[decoderStatus];
                outputFrame.position(info.offset);
                outputFrame.limit(info.offset + info.size);

                byte[] outData = new byte[info.size];
                outputFrame.get(outData);
                frame.data = outData;
                decodedFrames.put(frame);

                decoder.releaseOutputBuffer(decoderStatus, false /*render*/);    // TODO: change to false if not using surface
                decoderStatus = decoder.dequeueOutputBuffer(info, TIMEOUT_USEC);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
//        byte[] ret = outputStream.toByteArray();
//        //need to add the
//        outputStream.reset();
//        return ret;
    }

    public int getBufferCount(){
        return this.count;
    }
}
