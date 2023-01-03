package com.example.masgobjectdetection;

import android.graphics.Rect;
import android.util.Log;

import com.google.mlkit.common.model.LocalModel;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.DetectedObject.Label;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions;
//import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;         //TODO: delete this line

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class MyObjectDetection {
    private final String TAG = "ObjectDetection";
    private final Charset charset = StandardCharsets.UTF_8;
    //TODO: If you want to use the local model start from here
    LocalModel localModel =
            new LocalModel.Builder()
                    .setAssetFilePath("lite-model_object_detection_mobile_object_labeler_v1_1.tflite")   //TODO: change model in assets , then here
                    .build();

    CustomObjectDetectorOptions customObjectDetectorOptions =
            new CustomObjectDetectorOptions.Builder(localModel)
                    .setDetectorMode(CustomObjectDetectorOptions.SINGLE_IMAGE_MODE)
                    .enableMultipleObjects()
                    .enableClassification()
                    .setClassificationConfidenceThreshold(0.4f)
                    .setMaxPerObjectLabelCount(5)
                    .build();

    ObjectDetector objectDetector =
            ObjectDetection.getClient(customObjectDetectorOptions);
    //TODO: If you want to use the local model end by here

    //TODO: If you want to use the default model start from here
    //Multiple object detection in static images
//    ObjectDetectorOptions options =
//            new ObjectDetectorOptions.Builder()
//                    .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
//                    .enableMultipleObjects()
//                    .enableClassification()  // Optional
//                    .build();
//
//    //Get ObjectDetector Instance
//    ObjectDetector objectDetector = ObjectDetection.getClient(options);
    //TODO: If you want to use the default model end by here

    // Image dimension
    private int width;
    private int height;

    BlockingQueue<byte[]> ODResults;

    public MyObjectDetection(BlockingQueue<byte[]> ODResults, int _width, int _height) {
        this.ODResults = ODResults;
        this.width = _width;
        this.height = _height;
    }

    public void detect(int seq, byte[] img) {
        int rotation = 0;
        InputImage image = InputImage.fromByteArray(
                img,
                width,      // image width
                height,     // image height
                rotation,
                InputImage.IMAGE_FORMAT_NV21 // or IMAGE_FORMAT_NV21
        );

        objectDetector.process(image)
            .addOnSuccessListener(
                detectedObjects -> {
                    // Task completed successfully
                    Log.i(TAG, "object detection run successfully, img size = " + img.length);
                    byte[] results = createObjectDetectionPayload(seq, detectedObjects);
                    try {
                        ODResults.put(results);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            )
            .addOnFailureListener(
                e -> {
                    // Task failed with an exception
                    Log.e(TAG, String.valueOf(e));
                }
            );
    }

    private byte[] createObjectDetectionPayload(int seq, List<DetectedObject> detectedObjects) {
        int size = 0;
        int count = 0;
        for (DetectedObject detectedObject : detectedObjects) {
            int size_ = detectedObject.getLabels().size();
            String text;
            if (size_ == 0) {
//                text = "None";
//                Log.d(TAG, "calculate_objects: ==========================" + text);
                    continue;
            } else {
                    text = detectedObject.getLabels().get(0).getText();
                    Log.d(TAG, "calculate_objects: ==========================" + text);
                    size += text.getBytes(charset).length;
                    size += 24;  //label_size,top,bottom,left,right,confidence
                    count += 1;
            }
      //      size += text.getBytes(charset).length;
       //     size += 24;  //label_size,top,bottom,left,right,confidence
        }
        ByteBuffer buffer = ByteBuffer.allocate(4 + 4 + size);
        buffer.putInt(seq);
        //buffer.putInt(detectedObjects.size());
        buffer.putInt(count);
        Log.d(TAG, "objects_size: ==========================" + count);
        Log.d(TAG, "allocate_size: ==========================" + size);

        for (DetectedObject detectedObject : detectedObjects) {
            Rect boundingBox = detectedObject.getBoundingBox();
            int size_ = detectedObject.getLabels().size();
            if (size_ == 0) {
//                String text = "None";
//                int label_size = text.getBytes(charset).length;
//                float confidence = -1;
//                buffer.putInt(label_size);
//                buffer.put(text.getBytes(charset));
//                buffer.putFloat(confidence);
                continue;
            } else {
                    Label label = detectedObject.getLabels().get(0);
                    String text = label.getText();
                    int label_size = text.getBytes(charset).length;
                    buffer.putInt(label_size);
                    buffer.put(text.getBytes(charset));
                    buffer.putFloat(label.getConfidence());
            }
            buffer.putFloat(boundingBox.top);
            buffer.putFloat(boundingBox.bottom);
            buffer.putFloat(boundingBox.left);
            buffer.putFloat(boundingBox.right);

        }
        return buffer.array();
    }
}
