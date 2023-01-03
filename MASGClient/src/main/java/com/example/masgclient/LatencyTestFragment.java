package com.example.masgclient;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.masgcommunication.Constant;
import com.example.masgcommunication.LatencyStats;

public class LatencyTestFragment extends Fragment {

    TextView numSentPackets, numReceivedAcks, currentLatency, currentThroughput, averageLatency;

    boolean viewCreated = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_latency_test, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        numSentPackets = view.findViewById(R.id.num_sent_packets);
        numReceivedAcks = view.findViewById(R.id.num_received_acks);
        currentLatency = view.findViewById(R.id.current_latency);
        currentThroughput = view.findViewById(R.id.current_throughput);
        averageLatency = view.findViewById(R.id.average_latency);

        viewCreated = true;
    }

    private void makeToast(String msg) {
        Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
    }

    public Handler getHandler() {return mHandler;}

    Handler mHandler = new Handler(msg -> {
        if (!viewCreated) return true;
        switch (msg.what) {
            case Constant.CLIENT_LATENCY_UPSTREAM_STATS:
                numSentPackets.setText(String.valueOf(msg.arg1));
                break;
            case Constant.CLIENT_LATENCY_DOWNSTREAM_STATS:
                LatencyStats latencyStats = (LatencyStats) msg.obj;
                if (latencyStats == null) break;
                numReceivedAcks.setText(String.valueOf(latencyStats.numReceivedACKs));
                currentLatency.setText(String.valueOf(latencyStats.currentLatency));
                averageLatency.setText(String.format("%.2f", latencyStats.averageLatency));
                if (latencyStats.hasThroughputInfo)
                    currentThroughput.setText(String.format("%.2f", latencyStats.currentThroughput));
                break;
            case Constant.MESSAGE_TOAST:
                makeToast((String)msg.obj);
                break;
        }
        return true;
    });

}
