package com.example.masgcommunication;

import android.media.Image;

public class ImageFrame {
    public int seq;
    public Image image;

    public ImageFrame(int _seq, Image _image){
        this.seq = _seq;
        this.image = _image;
    }
}
