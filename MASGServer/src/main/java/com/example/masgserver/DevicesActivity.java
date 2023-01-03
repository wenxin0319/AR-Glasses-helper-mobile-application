package com.example.masgserver;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.masgcommunication.Constant;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@SuppressLint("MissingPermission")
public class DevicesActivity extends AppCompatActivity {
    public static final String LISTVIEW_INDEX = "Position";

    BluetoothAdapter mBluetoothAdapter;

    TextView noPairedDeviceTextView;
    ListView pairedListView;
    TextView newDevicesTextView;
    ProgressBar discoveryProgressBar;
    TextView noNewDeviceTextView;
    ListView newListView;

    List<String> pairedDevicesNames;
    List<BluetoothDevice> pairedDevices;
    DeviceListAdapter pairedListAdapter;
    List<String> newDevicesNames;
    List<BluetoothDevice> newDevices;
    DeviceListAdapter newListAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.devices_list);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setResult(Activity.RESULT_CANCELED);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        setViews();

        startNewDevicesDiscovery();

        Set<BluetoothDevice> bondedDevices = mBluetoothAdapter.getBondedDevices();

        if (!bondedDevices.isEmpty()) {
            for (BluetoothDevice device : bondedDevices) {
                pairedDevicesNames.add(device.getName());
                pairedDevices.add(device);
                pairedListAdapter.notifyDataSetChanged();
            }
        } else {
            noPairedDeviceTextView.setVisibility(View.VISIBLE);
        }

    }

    private void setViews() {
        noPairedDeviceTextView = findViewById(R.id.no_paired_devices_text);
        pairedListView = findViewById(R.id.paired_devices_listview);
        newDevicesTextView = findViewById(R.id.new_devices_textview);
        discoveryProgressBar = findViewById(R.id.discovery_prograssbar);
        noNewDeviceTextView = findViewById(R.id.no_new_devices_text);
        newListView = findViewById(R.id.new_devices_listview);

        pairedDevicesNames = new ArrayList<>();
        pairedDevices = new ArrayList<>();
        pairedListAdapter = new DeviceListAdapter(this, pairedDevicesNames);
        newDevicesNames = new ArrayList<>();
        newDevices = new ArrayList<>();
        newListAdapter = new DeviceListAdapter(this, newDevicesNames);

        pairedListView.setAdapter(pairedListAdapter);
        newListView.setAdapter(newListAdapter);

        pairedListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent();
                intent.putExtra(BluetoothDevice.EXTRA_DEVICE, pairedDevices.get(i));

                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        });
        newListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent();
                intent.putExtra(BluetoothDevice.EXTRA_DEVICE, newDevices.get(i));

                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        });
    }

    private void startNewDevicesDiscovery() {
        registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED));
        registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));

        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        mBluetoothAdapter.startDiscovery();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {
                discoveryProgressBar.setVisibility(View.VISIBLE);
            } else if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice curDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (curDevice != null && curDevice.getBondState() != BluetoothDevice.BOND_BONDED) {
                    newDevicesNames.add(curDevice.getName());
                    newDevices.add(curDevice);
                    newListAdapter.notifyDataSetChanged();
                }
            } else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                if (newDevices.isEmpty()) noNewDeviceTextView.setVisibility(View.VISIBLE);
                discoveryProgressBar.setVisibility(View.GONE);
            }
        }
    };


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return true;
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBluetoothAdapter != null) mBluetoothAdapter.cancelDiscovery();
        unregisterReceiver(mReceiver);
    }

    private class DeviceListAdapter extends BaseAdapter implements ListAdapter {
        private List<String> deviceNames;
        private Context context;

        public DeviceListAdapter(Context context, List<String> deviceNames) {
            this.deviceNames = deviceNames;
            this.context = context;
        }

        @Override
        public int getCount() {
            return deviceNames.size();
        }

        @Override
        public Object getItem(int pos) {
            return deviceNames.get(pos);
        }

        @Override
        public long getItemId(int pos) {
            return pos;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View view = convertView == null ?
                    ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                            .inflate(R.layout.device_item, null) : convertView;

            TextView deviceNameTextView = view.findViewById(R.id.device_name);
            deviceNameTextView.setText(deviceNames.get(position));

            Button infoButton = view.findViewById(R.id.device_info_button);

            infoButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent checkDeviceInfoIntent = new Intent(getApplicationContext(), DeviceInfoActivity.class);
                    if (deviceNames == pairedDevicesNames) {
                        checkDeviceInfoIntent.putExtra(BluetoothDevice.EXTRA_DEVICE, pairedDevices.get(position));
                        checkDeviceInfoIntent.putExtra(LISTVIEW_INDEX, position);
                    } else {
                        checkDeviceInfoIntent.putExtra(BluetoothDevice.EXTRA_DEVICE, newDevices.get(position));
                    }

                    checkDeviceInfoLauncher.launch(checkDeviceInfoIntent);
                }
            });

            return view;
        }

        private ActivityResultLauncher<Intent> checkDeviceInfoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Constant.FORGET_DEVICE_RESULT_CODE) {
                            int position = result.getData().getIntExtra(LISTVIEW_INDEX, -1);
                            if (position < 0) return;
                            BluetoothDevice forgetDevice = pairedDevices.get(position);
                            try {
                                forgetDevice.getClass().getMethod("removeBond", (Class[]) null)
                                        .invoke(forgetDevice, (Object[]) null);
                            } catch (Exception e) {
                                e.printStackTrace();
                                System.out.println("Cannot forget device.");
                            }
                            pairedDevicesNames.remove(position);
                            pairedDevices.remove(position);
                            pairedListAdapter.notifyDataSetChanged();

                            if (pairedDevicesNames.isEmpty())
                                noPairedDeviceTextView.setVisibility(View.VISIBLE);
                        }
                    }
                }
        );
    }
}
