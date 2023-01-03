package com.example.masgserver;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.masgcommunication.Constant;
import com.example.masgcommunication.RunningStats;
import com.example.masgcommunication.ThroughputStats;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ServerCommunicationFragment extends Fragment {

    BluetoothCore bluetoothCore = BluetoothCore.getInstance();

    Button startServerButton, stopServerButton;
    Button startThroughputTestButton, stopThroughputTestButton;
    Button startLatencyTestButton, stopLatencyTestButton;

    LinearLayout serverRunningDetailLayout, serverThroughputTestDetailLayout, serverLatencyTestDetailLayout;

    TextView runningNumSentFrames, runningSentBytes, runningUpstreamThroughput,
        runningNumReceivedFrames, runningReceivedBytes, runningDownstreamThroughput;

    TextView serverDuration, serverSentBytes, serverTotalUpstreamThroughput,
        serverCurrentUpstreamThroughput, serverReceivedBytes, serverTotalDownstreamThroughput,
        serverCurrentDownstreamThroughput;

    TextView serverReceivedPackets, serverSentAcks, packetsBufferSize;

    private final List<Number> senderThroughput = new ArrayList<>();
    private final List<Number> receiverThroughput = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bluetoothCore.setTestFragHandler(mHandler);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_server_communication, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        startServerButton = view.findViewById(R.id.start_server_button);
        startThroughputTestButton = view.findViewById(R.id.start_throughput_test_button);
        startLatencyTestButton = view.findViewById(R.id.start_latency_test_button);

        serverRunningDetailLayout = view.findViewById(R.id.server_running_detail_layout);
        serverRunningDetailLayout.setVisibility(View.GONE);
        runningNumSentFrames = view.findViewById(R.id.running_num_sent_frames);
        runningSentBytes = view.findViewById(R.id.running_sent_bytes);
        runningUpstreamThroughput = view.findViewById(R.id.running_send_throughput);
        runningNumReceivedFrames = view.findViewById(R.id.running_num_received_frames);
        runningReceivedBytes = view.findViewById(R.id.running_received_bytes);
        runningDownstreamThroughput = view.findViewById(R.id.running_receive_throughput);
        stopServerButton = view.findViewById(R.id.stop_server_button);

        serverThroughputTestDetailLayout = view.findViewById(R.id.server_throughput_test_detail_layout);
        serverThroughputTestDetailLayout.setVisibility(View.GONE);
        serverDuration = view.findViewById(R.id.server_duration);
        serverSentBytes = view.findViewById(R.id.server_sent_bytes);
        serverTotalUpstreamThroughput = view.findViewById(R.id.server_send_total_tp);
        serverCurrentUpstreamThroughput = view.findViewById(R.id.server_send_current_tp);
        serverReceivedBytes = view.findViewById(R.id.server_receive_bytes);
        serverTotalDownstreamThroughput = view.findViewById(R.id.server_receive_total_tp);
        serverCurrentDownstreamThroughput = view.findViewById(R.id.server_receive_current_tp);
        stopThroughputTestButton = view.findViewById(R.id.stop_throughput_test_button);

        serverLatencyTestDetailLayout = view.findViewById(R.id.server_latency_test_detail_layout);
        serverLatencyTestDetailLayout.setVisibility(View.GONE);
        serverReceivedPackets = view.findViewById(R.id.server_num_received_packets);
        serverSentAcks = view.findViewById(R.id.server_num_sent_ack);
        packetsBufferSize = view.findViewById(R.id.packet_buffer_size);
        stopLatencyTestButton = view.findViewById(R.id.stop_latency_test_button);

        setListeners();
    }

    public void setListeners() {
        startServerButton.setOnClickListener(view -> {
            bluetoothCore.startServer();
            serverRunningDetailLayout.setVisibility(View.VISIBLE);
            startServerButton.setVisibility(View.GONE);
            startThroughputTestButton.setEnabled(false);
            startThroughputTestButton.setAlpha(0.5f);
            startLatencyTestButton.setEnabled(false);
            startLatencyTestButton.setAlpha(0.5f);

            Intent decodeViewActivityIntent = new Intent(getContext(), DecodeViewActivity.class);
            decodeViewActivityLauncher.launch(decodeViewActivityIntent);
        });

        stopServerButton.setOnClickListener(view -> {
            bluetoothCore.stopServer(false);
            serverRunningDetailLayout.setVisibility(View.GONE);
            startServerButton.setVisibility(View.VISIBLE);
            startThroughputTestButton.setEnabled(true);
            startThroughputTestButton.setAlpha(1f);
            startLatencyTestButton.setAlpha(1f);
            startLatencyTestButton.setEnabled(true);
        });

        startThroughputTestButton.setOnClickListener(view -> {
            bluetoothCore.startThroughputTest();
            serverThroughputTestDetailLayout.setVisibility(View.VISIBLE);
            startThroughputTestButton.setVisibility(View.GONE);
            stopThroughputTestButton.setVisibility(View.VISIBLE);
            startServerButton.setEnabled(false);
            startServerButton.setAlpha(0.5f);
            startLatencyTestButton.setEnabled(false);
            startLatencyTestButton.setAlpha(0.5f);
        });

        stopThroughputTestButton.setOnClickListener(view -> {
            bluetoothCore.stopThroughputTest(false);
            Intent viewThroughputPlotIntent = new Intent(getContext(), ViewThroughputPlotActivity.class);
            viewThroughputPlotIntent.putExtra(Constant.SENDER_THROUGHPUT_KEY, (Serializable) senderThroughput);
            viewThroughputPlotIntent.putExtra(Constant.RECEIVER_THROUGHPUT_KEY, (Serializable) receiverThroughput);
            viewThroughputPlotLauncher.launch(viewThroughputPlotIntent);
        });

        startLatencyTestButton.setOnClickListener(view -> {
            bluetoothCore.startLatencyTest();
            serverLatencyTestDetailLayout.setVisibility(View.VISIBLE);
            stopLatencyTestButton.setVisibility(View.VISIBLE);
            startLatencyTestButton.setVisibility(View.GONE);
            startServerButton.setEnabled(false);
            startServerButton.setAlpha(0.5f);
            startThroughputTestButton.setEnabled(false);
            startThroughputTestButton.setAlpha(0.5f);
        });

        stopLatencyTestButton.setOnClickListener(view -> {
            bluetoothCore.stopLatencyTest(false);
            List<Number> latencyGraphData = bluetoothCore.getLatencyGraphData();
            Intent viewLatencyPlotIntent = new Intent(getContext(), ViewLatencyPlotActivity.class);
            viewLatencyPlotIntent.putExtra(Constant.LATENCY_GRAPH_DATA_KEY, (Serializable) latencyGraphData);
            viewLatencyPlotLauncher.launch(viewLatencyPlotIntent);
        });
    }

    ActivityResultLauncher<Intent> viewThroughputPlotLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                startThroughputTestButton.setVisibility(View.VISIBLE);
                stopThroughputTestButton.setVisibility(View.GONE);
                serverThroughputTestDetailLayout.setVisibility(View.GONE);
                startServerButton.setAlpha(1f);
                startServerButton.setEnabled(true);
                startLatencyTestButton.setAlpha(1f);
                startLatencyTestButton.setEnabled(true);
            }
        }
    );

    ActivityResultLauncher<Intent> viewLatencyPlotLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                startLatencyTestButton.setVisibility(View.VISIBLE);
                serverLatencyTestDetailLayout.setVisibility(View.GONE);
                stopLatencyTestButton.setVisibility(View.GONE);
                startServerButton.setAlpha(1f);
                startServerButton.setEnabled(true);
                startThroughputTestButton.setAlpha(1f);
                startThroughputTestButton.setEnabled(true);
            }
        }
    );

    ActivityResultLauncher<Intent> decodeViewActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    startThroughputTestButton.setAlpha(1f);
                    startThroughputTestButton.setEnabled(true);
                    startLatencyTestButton.setAlpha(1f);
                    startLatencyTestButton.setEnabled(true);
                }
            }
    );

    private void makeToast(String msg) {
        Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
    }

    Handler mHandler = new Handler(message -> {
        switch (message.what) {
            case Constant.SERVER_RUNNING_UPSTREAM_STATS:
                RunningStats stats = (RunningStats) message.obj;
                if (stats == null) break;
                runningNumSentFrames.setText(String.valueOf(stats.numFrames));
                runningSentBytes.setText(String.valueOf(stats.totalBytes));
                if (stats.hasCurrentStat)
                    runningUpstreamThroughput.setText(String.format("%.2f", stats.currentThroughput));
                break;
            case Constant.SERVER_RUNNING_DOWNSTREAM_STATS:
                stats = (RunningStats) message.obj;
                if (stats == null) break;
                runningNumReceivedFrames.setText(String.valueOf(stats.numFrames));
                runningReceivedBytes.setText(String.valueOf(stats.totalBytes));
                if (stats.hasCurrentStat)
                    runningDownstreamThroughput.setText(String.format("%.2f", stats.currentThroughput));
                break;
            case Constant.THROUGHPUT_UPSTREAM_STATS:
                ThroughputStats upstreamStats = (ThroughputStats) message.obj;
                if (upstreamStats == null) break;
                serverSentBytes.setText(String.valueOf(upstreamStats.totalByte));
                serverDuration.setText(String.format("%.2f", upstreamStats.duration));
                serverTotalUpstreamThroughput.setText(String.format("%.2f", upstreamStats.totalThroughput));
                if (upstreamStats.hasCurrentStat) {
                    serverCurrentUpstreamThroughput.setText(String.format("%.2f", upstreamStats.currentThroughput));
                    senderThroughput.add(upstreamStats.currentThroughput);
                }
                break;
            case Constant.THROUGHPUT_DOWNSTREAM_STATS:
                ThroughputStats downstreamStats = (ThroughputStats) message.obj;
                if (downstreamStats == null) break;
                serverReceivedBytes.setText(String.valueOf(downstreamStats.totalByte));
                serverTotalDownstreamThroughput.setText(String.format("%.2f", downstreamStats.totalThroughput));
                if (downstreamStats.hasCurrentStat) {
                    serverCurrentDownstreamThroughput.setText(String.format("%.2f", downstreamStats.currentThroughput));
                    receiverThroughput.add(downstreamStats.currentThroughput);
                }
                break;
            case Constant.SERVER_LATENCY_UPSTREAM_STATS:
                serverSentAcks.setText(String.valueOf(message.arg1));
                packetsBufferSize.setText(String.valueOf(message.arg2));
                break;
            case Constant.SERVER_LATENCY_DOWNSTREAM_STATS:
                serverReceivedPackets.setText(String.valueOf(message.arg1));
                packetsBufferSize.setText(String.valueOf(message.arg2));
                break;
            case Constant.MESSAGE_TOAST:
                makeToast((String)message.obj);
                break;
        }
        return true;
    });

}
