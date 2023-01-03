package com.example.masgclient;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.masgcommunication.Constant;

import java.util.Map;

public class MainActivity extends AppCompatActivity {

    TextView connectTextView;

    BluetoothCore bluetoothCore;

    ThroughputTestFragment throughputTestFragment;
    LatencyTestFragment latencyTestFragment;

    boolean throughputTestFragmentOn, latencyTestFragmentOn;

    String[] permissions = (
        Build.VERSION.SDK_INT >= 31 ?
            new String[] {
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.INTERNET,
                Manifest.permission.WAKE_LOCK,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_MEDIA_LOCATION
            } :
            new String[] {
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.INTERNET,
                Manifest.permission.WAKE_LOCK,
                Manifest.permission.READ_EXTERNAL_STORAGE,
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectTextView = findViewById(R.id.connect_status_textview);
        throughputTestFragment = new ThroughputTestFragment();
        latencyTestFragment = new LatencyTestFragment();
        throughputTestFragmentOn = false;
        latencyTestFragmentOn = false;

        bluetoothCore = BluetoothCore.getInstance();
        bluetoothCore.setMainHandler(mHandler);

        bluetoothCore.setThroughputTestHandler(throughputTestFragment.getHandler());
        bluetoothCore.setLatencyTestHandler(latencyTestFragment.getHandler());

        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) System.out.println("Adapter not enabled!");

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(permissions);
                return;
            }
        }

        makeDiscoverableAndStartListening();
    }

    private ActivityResultLauncher<Intent> streamActivityLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                connectTextView.setText("Connected to another device. Waiting for tests to start.");
            }
        }
    );

    private void makeToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private Handler mHandler = new Handler((msg) -> {
        switch (msg.what) {
            case Constant.BLUETOOTH_CONNECTED:
            case Constant.TEST_ENDED:
                connectTextView.setText("Connected to another device. Waiting for tests to start.");
                break;
            case Constant.SERVER_RUNNING:
                connectTextView.setText("Server running!");
                if (throughputTestFragmentOn) {
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.remove(throughputTestFragment);
                    transaction.commit();
                    transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
                    throughputTestFragmentOn = false;
                }

                if (latencyTestFragmentOn) {
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.remove(latencyTestFragment);
                    transaction.commit();
                    transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
                    latencyTestFragmentOn = false;
                }

                Intent streamActivityIntent = new Intent(this, StreamActivity.class);
                streamActivityLauncher.launch(streamActivityIntent);
                break;
            case Constant.THROUGHPUT_TEST_STARTED:
                connectTextView.setText("Throughput test started:");
                if (latencyTestFragmentOn) {
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.remove(latencyTestFragment);
                    transaction.commit();
                    transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
                    latencyTestFragmentOn = false;
                }
                if (!throughputTestFragmentOn) {
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.replace(R.id.sample_content_fragment, throughputTestFragment);
                    transaction.commit();
                    throughputTestFragmentOn = true;
                }
                break;
            case Constant.LATENCY_TEST_STARTED:
                connectTextView.setText("Latency test started:");
                if (throughputTestFragmentOn) {
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.remove(throughputTestFragment);
                    transaction.commit();
                    transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
                    throughputTestFragmentOn = false;
                }
                if (!latencyTestFragmentOn) {
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.replace(R.id.sample_content_fragment, latencyTestFragment);
                    transaction.commit();
                    latencyTestFragmentOn = true;
                }
                break;
            case Constant.BLUETOOTH_DISCONNECTED:
                connectTextView.setText("Not connected to a device.");
                makeDiscoverableAndStartListening();
                break;
            case Constant.MESSAGE_TOAST:
                makeToast((String) msg.obj);
        }
        return true;
    });

    private ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(
        new ActivityResultContracts.RequestMultiplePermissions(),
        grantedResults -> {
            for (Map.Entry<String, Boolean> rs : grantedResults.entrySet()) {
                if (!rs.getValue()) {
                    makeToast(String.format("Permission %s has to be granted.", rs.getKey()));
                    showPermissionAlert(rs.getKey());
                    return;
                }
            }
            makeDiscoverableAndStartListening();
        }
    );

    private void showPermissionAlert(String permission) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(String.format("Grant permission %s in Settings.", permission));
        builder.setMessage("Open Settings to grant permissions?")
            .setCancelable(true)
            .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getApplicationContext().getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                }
            });
        builder.create().show();
    }

    private void makeDiscoverableAndStartListening() {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        discoverableLauncher.launch(discoverableIntent);
    }

    @SuppressLint("MissingPermission")
    private ActivityResultLauncher<Intent> discoverableLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if (result.getResultCode() > 0) {
                    bluetoothCore.startListening();
                } else {
                    makeToast("Cannot make device discoverable.");
                }
            }
        }
    );
}