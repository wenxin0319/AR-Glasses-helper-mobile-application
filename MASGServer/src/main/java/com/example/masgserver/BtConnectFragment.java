package com.example.masgserver;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.example.masgcommunication.Constant;

import java.net.URI;
import java.util.Map;

public class BtConnectFragment extends Fragment {
    private BluetoothAdapter mBluetoothAdapter;

    private BluetoothCore bluetoothCore;

    private Switch btSwitch;
    private TextView connectToTextView;
    private Button connectButton;
    private Button discoverableButton;
    private TextView discoverableTextView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        FragmentActivity curActivity = getActivity();
        if (mBluetoothAdapter == null && curActivity != null) {
            makeToast("Bluetooth is not available");
            curActivity.finish();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bluetooth_connect, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        connectToTextView = view.findViewById(R.id.connect_to_textview);
        connectButton = view.findViewById(R.id.connect_button);
        discoverableButton = view.findViewById(R.id.discoverable_button);
        discoverableTextView = view.findViewById(R.id.discoverable_textview);
        setListeners();

        bluetoothCore = BluetoothCore.getInstance();
        bluetoothCore.setConnectionFragHandler(mHandler);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.bluetooth_connect, menu);

        // retrieve the bluetooth switch
        MenuItem switchItem = menu.findItem(R.id.bluetooth_switch_item);
        switchItem.setActionView(R.layout.switch_layout);
        btSwitch = switchItem.getActionView().findViewById(R.id.bluetooth_switch);

        // set up the switch
        btSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                String[] permissions = (
                    Build.VERSION.SDK_INT >= 31 ?
                    new String[] {
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_ADVERTISE,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    } :
                    new String[] {
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                    }
                );

                if (b) {
                    for (String permission : permissions) {
                        if (ContextCompat.checkSelfPermission(getContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                            requestPermissionLauncher.launch(permissions);
                            return;
                        }
                    }
                    if(!mBluetoothAdapter.isEnabled()) {
                        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        bluetoothEnableLauncher.launch(enableIntent);
                    } else {
                        setViewsBluetoothOn();
                    }
                } else {
                    bluetoothCore.disconnect();
                    setViewsBluetoothOff();
                }
            }
        });

        setViewsBluetoothOff();
    }

    private ActivityResultLauncher<Intent> bluetoothEnableLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        setViewsBluetoothOn();
                    } else {
                        makeToast("Bluetooth cannot be turned on.");
                        btSwitch.setChecked(false);
                    }
                }
            }
    );

    private ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            grantedResults -> {
                for (Map.Entry<String, Boolean> rs : grantedResults.entrySet()) {
                    if (!rs.getValue()) {
                        makeToast(String.format("Permission %s has to be granted.", rs.getKey()));
                        btSwitch.setChecked(false);
                        showPermissionAlert(rs.getKey());
                        return;
                    }
                }
                if(!mBluetoothAdapter.isEnabled()) {
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    bluetoothEnableLauncher.launch(enableIntent);
                } else {
                    setViewsBluetoothOn();
                }
            }
    );

    private void showPermissionAlert(String permission) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(String.format("Grant permission %s in Settings.", permission));
        builder.setMessage("Open Settings to grant permissions?")
                .setCancelable(true)
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getContext().getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                    }
                });
        builder.create().show();
    }

    private void setViewsBluetoothOff() {
        connectToTextView.setText("Turn on Bluetooth to connect to a device.");
        connectToTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.run_bluetooth_icon, 0, 0, 0);
        connectButton.setText("CONNECT TO A DEVICE");
        connectButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.bluetooth_connect_icon, 0, 0);
        connectButton.setEnabled(false);
        connectButton.setAlpha(0.5f);
        discoverableButton.setText("MAKE DISCOVERABLE");
        discoverableButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.bluetooth_discoverable_icon, 0, 0);
        discoverableButton.setEnabled(false);
        discoverableButton.setAlpha(0.5f);
        discoverableTextView.setText("Turn on Bluetooth to be discoverable from other devices.");
    }

    private void setViewsBluetoothOn() {
        connectToTextView.setText("Not connected to a device.");
        connectToTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.device_not_connected_icon, 0, 0, 0);
        connectButton.setText("CONNECT TO A DEVICE");
        connectButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.bluetooth_connect_icon, 0, 0);
        connectButton.setEnabled(true);
        connectButton.setAlpha(1f);
        discoverableButton.setText("MAKE DISCOVERABLE");
        discoverableButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.bluetooth_discoverable_icon, 0, 0);
        discoverableButton.setEnabled(true);
        discoverableButton.setAlpha(1f);
        discoverableTextView.setText("Currently hidden from other Bluetooth devices or does not accept connections.");
    }

    @SuppressLint("MissingPermission")
    private void setListeners() {
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (connectButton.getText().charAt(0) == 'C') {
                    Intent connectIntent = new Intent(getActivity(), DevicesActivity.class);
                    connectLauncher.launch(connectIntent);
                } else {
                    bluetoothCore.disconnect();
                    connectButton.setText("CONNECT TO A DEVICE");
                    connectButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.bluetooth_connect_icon, 0, 0);
                }
            }
        });

        discoverableButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (discoverableButton.getText().charAt(0) == 'M') {
                    if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                        discoverableLauncher.launch(discoverableIntent);
                    } else {
                        discoverableTextView.setText(
                            String.format(
                                "Now discoverable as \"%s\" and listening for new connections.",
                                mBluetoothAdapter.getName()
                            )
                        );
                        bluetoothCore.startListening();
                        discoverableButton.setText("HIDE DEVICE");
                        discoverableButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.bluetooth_hidden_icon, 0, 0);
                    }
                } else {
                    bluetoothCore.stopListening();
                    discoverableTextView.setText("Currently hidden from other devices or does not accept connections.");
                    discoverableButton.setText("MAKE DISCOVERABLE");
                    discoverableButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.bluetooth_discoverable_icon, 0, 0);
                }
            }
        });
    }

    @SuppressLint("MissingPermission")
    private ActivityResultLauncher<Intent> connectLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_CANCELED) {
                        System.out.println("No device is selected.");
                    } else if (result.getResultCode() == Activity.RESULT_OK) {
                        BluetoothDevice device = result.getData().getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        bluetoothCore.connect(device);
                        System.out.printf("To connect to device %s with address %s\n", device.getName(), device.getAddress());
                    }
                }
            }
    );

    @SuppressLint("MissingPermission")
    private ActivityResultLauncher<Intent> discoverableLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() > 0) {
                        discoverableTextView.setText(
                            String.format(
                                "Now discoverable as \"%s\" and listening for new connections.",
                                mBluetoothAdapter.getName()
                            )
                        );
                        bluetoothCore.startListening();
                        discoverableButton.setText("HIDE DEVICE");
                        discoverableButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.bluetooth_hidden_icon, 0, 0);
                    } else {
                        makeToast("Cannot make device discoverable.");
                    }
                }
            }
    );

    private void makeToast(String msg) {
        Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
    }

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case Constant.BLUETOOTH_CONNECTED:
                    String deviceName = (String) msg.obj;
                    connectToTextView.setText(String.format("Connected to %s.", deviceName));
                    connectToTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.device_connected_icon, 0, 0, 0);
                    connectButton.setText("DISCONNECT");
                    connectButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.bluetooth_disconnect_icon, 0, 0);
                    discoverableButton.setText("MAKE DISCOVERABLE");
                    discoverableButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.bluetooth_discoverable_icon, 0, 0);
                    discoverableButton.setEnabled(false);
                    discoverableButton.setAlpha(0.5f);
                    discoverableTextView.setText("Currently hidden from other devices or does not accept connections.");
                    ((MainActivity) getActivity()).setUpTestFragment();
                    break;
                case Constant.BLUETOOTH_DISCONNECTED:
                    if (btSwitch.isChecked()) {
                        connectToTextView.setText("Not connected to a device.");
                        connectToTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.device_not_connected_icon, 0, 0, 0);
                        connectButton.setText("CONNECT TO A DEVICE");
                        connectButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.bluetooth_connect_icon, 0, 0);
                        connectButton.setEnabled(true);
                        connectButton.setAlpha(1f);
                        discoverableButton.setEnabled(true);
                        discoverableButton.setAlpha(1f);
                    }
                    ((MainActivity) getActivity()).unsetTestFragment();
                    break;
                case Constant.MESSAGE_TOAST:
                    makeToast((String) msg.obj);
                    break;
            }
        }
    };

}
