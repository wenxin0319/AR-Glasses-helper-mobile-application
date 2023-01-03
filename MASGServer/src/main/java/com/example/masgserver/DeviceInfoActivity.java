package com.example.masgserver;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.bluetooth.BluetoothClass.Device;

import androidx.annotation.Nullable;

import com.example.masgcommunication.Constant;

@SuppressLint("MissingPermission")
public class DeviceInfoActivity extends Activity {
    TextView nameTextView, addressTextView, classTextView, typeTextView;
    Button forgetButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.device_info);
        setResult(Activity.RESULT_CANCELED);

        nameTextView = findViewById(R.id.device_name_textview);
        addressTextView = findViewById(R.id.device_address_textview);
        classTextView = findViewById(R.id.device_class_textview);
        typeTextView = findViewById(R.id.device_type_textview);
        forgetButton = findViewById(R.id.forget_device_button);

        Intent intent = getIntent();
        BluetoothDevice curDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        int position = intent.getIntExtra(DevicesActivity.LISTVIEW_INDEX, -1);

        if (position < 0) {
            forgetButton.setEnabled(false);
            forgetButton.setVisibility(View.GONE);
        }

        nameTextView.setText(curDevice.getName());
        addressTextView.setText(curDevice.getAddress());
        classTextView.setText(getDeviceClass(curDevice.getBluetoothClass().getMajorDeviceClass()));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            typeTextView.setText(getDeviceType(curDevice.getType()));
        } else {
            typeTextView.setText("Unknown Type");
        }

        forgetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent forgetIntent = new Intent();
                forgetIntent.putExtra(DevicesActivity.LISTVIEW_INDEX, position);
                setResult(Constant.FORGET_DEVICE_RESULT_CODE, forgetIntent);
                finish();
            }
        });
    }

    private String getDeviceClass(int majorClassCode) {
        switch (majorClassCode) {
            case Device.AUDIO_VIDEO_HEADPHONES: return "Headphone";
            case Device.AUDIO_VIDEO_VIDEO_CAMERA: return"Video Camera";
            case Device.PHONE_SMART: return "Smart Phone";
            case Device.PHONE_CELLULAR: return "Cellular Phone";
            case Device.PHONE_UNCATEGORIZED: return "Phone";
            case Device.WEARABLE_GLASSES: return "Wearable Glasses";
            default: return "Unknown Class";
        }
    }

    private String getDeviceType(int typeCode) {
        switch (typeCode) {
            case BluetoothDevice.DEVICE_TYPE_CLASSIC: return "Classic - BR/EDR";
            case BluetoothDevice.DEVICE_TYPE_DUAL: return "Dual Mode - BR/EDR/LE";
            case BluetoothDevice.DEVICE_TYPE_LE: return "Low Energy - LE-only";
            default: return "Unknown Type";
        }
    }
}
