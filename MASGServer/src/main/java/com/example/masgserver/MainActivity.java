package com.example.masgserver;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    BluetoothCore bluetoothCore;

    ServerCommunicationFragment serverTestFrag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothCore = BluetoothCore.getInstance();

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        BtConnectFragment btcFrag = new BtConnectFragment();
        transaction.replace(R.id.sample_content_fragment, btcFrag);
        transaction.commit();
    }

    public void setUpTestFragment() {
        TextView testFragmentTextView = findViewById(R.id.test_fragment_textview);
        testFragmentTextView.setVisibility(View.GONE);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        serverTestFrag = new ServerCommunicationFragment();
        transaction.replace(R.id.test_framelayout, serverTestFrag);
        transaction.commit();
    }

    public void unsetTestFragment() {
        TextView testFragmentTextView = findViewById(R.id.test_fragment_textview);
        testFragmentTextView.setVisibility(View.VISIBLE);

        if (serverTestFrag != null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.remove(serverTestFrag);
            transaction.commit();
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
            serverTestFrag = null;
        }
    }
}