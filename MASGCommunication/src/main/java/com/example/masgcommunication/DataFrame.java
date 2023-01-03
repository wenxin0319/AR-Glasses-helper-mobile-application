package com.example.masgcommunication;

public class DataFrame {
    public int seq;
    public byte[] data;

    public DataFrame(int _seq, byte[] _data){
        seq = _seq;
        data = _data;
    }
}
