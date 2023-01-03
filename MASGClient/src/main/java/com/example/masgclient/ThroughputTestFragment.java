package com.example.masgclient;


import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.annotation.Nullable;

import com.example.masgcommunication.Constant;
import com.example.masgcommunication.ThroughputStats;

public class ThroughputTestFragment extends Fragment {

    TextView duration, sentBytes, totalUpstreamThroughput,
        currentUpstreamThroughput, receivedBytes, totalDownstreamThroughput,
        currentDownstreamThroughput;

    boolean viewCreated = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_throughput_test, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        duration = view.findViewById(R.id.duration);
        sentBytes = view.findViewById(R.id.sent_bytes);
        totalUpstreamThroughput = view.findViewById(R.id.sent_total_tp);
        currentUpstreamThroughput = view.findViewById(R.id.sent_current_tp);
        receivedBytes = view.findViewById(R.id.receive_bytes);
        totalDownstreamThroughput = view.findViewById(R.id.receive_total_tp);
        currentDownstreamThroughput = view.findViewById(R.id.receive_current_tp);

        viewCreated = true;
    }

    private void makeToast(String msg) {
        Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
    }

    public Handler getHandler() {return mHandler;}

    private Handler mHandler = new Handler(msg -> {
        if (!viewCreated) return true;
        switch (msg.what) {
            case Constant.THROUGHPUT_UPSTREAM_STATS:
                ThroughputStats stats = (ThroughputStats) msg.obj;
                if (stats == null) break;
                duration.setText(String.format("%.2f", stats.duration));
                sentBytes.setText(String.valueOf(stats.totalByte));
                totalUpstreamThroughput.setText(String.format("%.2f", stats.totalThroughput));
                if (stats.hasCurrentStat) currentUpstreamThroughput.setText(String.format("%.2f", stats.currentThroughput));
                break;
            case Constant.THROUGHPUT_DOWNSTREAM_STATS:
                stats = (ThroughputStats) msg.obj;
                if (stats == null) break;
                receivedBytes.setText(String.valueOf(stats.totalByte));
                totalDownstreamThroughput.setText(String.format("%.2f", stats.totalThroughput));
                if (stats.hasCurrentStat) currentDownstreamThroughput.setText(String.format("%.2f", stats.currentThroughput));
                break;
            case Constant.MESSAGE_TOAST:
                makeToast((String) msg.obj);
        }
        return true;
    });
}
