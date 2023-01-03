package com.example.masgcommunication;

public class ODResult {
    public String label;
    public float confidence;
    public float top;
    public float bottom;
    public float left;
    public float right;

    public ODResult(String label, float confidence, float top, float bottom, float left, float right) {
        this.label = label;
        this.confidence = confidence;
        this.top = top;
        this.bottom = bottom;
        this.left = left;
        this.right = right;
    }
}
