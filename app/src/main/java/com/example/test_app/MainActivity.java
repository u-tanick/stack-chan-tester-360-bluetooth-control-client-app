package com.example.test_app;

//https://www.hiramine.com/programming/blecommunicator/index.html

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

@RequiresApi(api = Build.VERSION_CODES.S)
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private final String TAG = "MainActivity";

    // 定数
    private static final int REQUEST_ENABLEBLUETOOTH = 1; // Bluetooth機能の有効化要求時の識別コード
    // メンバー変数
    BluetoothLEWork mBluetoothLEWork;
    private BluetoothAdapter mBluetoothAdapter;    // BluetoothAdapter : Bluetooth処理で必要
    private InputMethodManager imm;

    private Button button_connect;
    private Button button_disconnect;
    private Button button_send;
    private Button button_clear;
    TextView text_devicename;
    private EditText text_tosend;
    private TextView text_received;
    private static final int REQUEST_MULTI_PERMISSIONS = 101;

    private final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    if (result.getData() != null) {
                        Log.i(TAG, "onActivityResult() Bluetooth function is available.");
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        button_connect = findViewById(R.id.button_connect);
        button_connect.setOnClickListener(this);
        button_disconnect = findViewById(R.id.button_disconnect);
        button_disconnect.setOnClickListener(this);
        button_send = findViewById(R.id.button_send);
        button_send.setOnClickListener(this);
        button_clear = findViewById(R.id.button_clear);
        button_clear.setOnClickListener(this);
        text_devicename = findViewById(R.id.text_devicename);
        text_tosend = findViewById(R.id.text_tosend);
        text_tosend.addTextChangedListener(new MyTextWatcher());
        text_received = findViewById(R.id.text_received);
        text_received.setMovementMethod(new ScrollingMovementMethod());
        mBluetoothLEWork = new BluetoothLEWork(MainActivity.this) {
            @Override
            public void onBLEConectionFailed(String str) {
                Toast.makeText(MainActivity.this, "Failed to connect to the device. " + str, Toast.LENGTH_LONG).show();
                button_connect.setEnabled(true);
            }

            @Override
            public void onBLEConnected(String device) {
                button_disconnect.setEnabled(true);
                button_connect.setEnabled(false);
                text_devicename.setText("Device: " + device);
                Toast.makeText(MainActivity.this, "Device: " + device + " Connected.", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onBLEServiseStarted() {
            }

            @Override
            public void onBLEDisconnected() {
                button_connect.setEnabled(true);
                button_disconnect.setEnabled(false);
                text_devicename.setText("no device");
            }

            @Override
            public void onMessageReceived(String text) {
                String tmp = text_received.getText().toString();
                text_received.setText(tmp + text + "\n");
            }

            @Override
            public void onMessageWritten() {
                button_send.setEnabled(true);
            }
        };
        if (!mBluetoothLEWork.checkBluetooth()) {
            Toast.makeText(this, "bluetooth is not available", Toast.LENGTH_SHORT).show();
            finish(); //Bluetoothが使えないので終了
        }
        mBluetoothAdapter = mBluetoothLEWork.getBluetoothAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "mBluetoothAdapter is not available", Toast.LENGTH_SHORT).show();
            finish(); //Bluetoothが使えないので終了
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            Log.i(TAG, "onResume() intent");
            activityResultLauncher.launch(enableBtIntent);
        }
        requestBlePermissions(this,REQUEST_MULTI_PERMISSIONS);
        mBluetoothLEWork.checkpermission();
    }

    private static final String[] BLE_PERMISSIONS = new String[]{
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
    };

    private static final String[] ANDROID_12_BLE_PERMISSIONS = new String[]{
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_CONNECT,
            //android.Manifest.permission.ACCESS_FINE_LOCATION,
    };

    public static void requestBlePermissions(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            ActivityCompat.requestPermissions(activity, ANDROID_12_BLE_PERMISSIONS, requestCode);
        else
            ActivityCompat.requestPermissions(activity, BLE_PERMISSIONS, requestCode);
    }

    // 初回表示時、および、ポーズからの復帰時
    @Override
    protected void onResume() {
        super.onResume();

        // Android端末のBluetooth機能の有効化要求
        if (mBluetoothAdapter.isEnabled()) {
            Log.i(TAG, "onResume() BluetoothAdapter is Enabled");
        }
    }

    // 別のアクティビティ（か別のアプリ）に移行したことで、バックグラウンドに追いやられた時
    @Override
    protected void onPause() {
        super.onPause();
        mBluetoothLEWork.disconnect();
    }

    // アクティビティの終了直前
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBluetoothLEWork.disconnect();
    }

    @Override
    public void onClick(View v) {
        if (v==button_connect) {    //ペアリング済のBTデバイスを選択して接続
            button_connect.setEnabled( false );
            mBluetoothLEWork.selectandconnectDevice();
        } else if (v==button_disconnect) {            //BTデバイスを切断
            mBluetoothLEWork.disconnect();
        } else if (v==button_send) {            //文字列送信
            button_send.setEnabled(false);
            //ソフトウエアキーボードを隠す
            text_devicename.requestFocus();
            imm.hideSoftInputFromWindow(text_tosend.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            String tmp = text_tosend.getText().toString();
            String stringSend = tmp + "\r\n";  // 終端に改行コードを付加
            mBluetoothLEWork.sendtext(stringSend);
        } else if (v==button_clear) {  //文字列消去
            text_devicename.requestFocus();
            imm.hideSoftInputFromWindow(text_tosend.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            text_tosend.setText("");
            text_received.setText("");
        }
    }

    private class MyTextWatcher implements TextWatcher {
        @Override
        public void afterTextChanged(Editable s) {
            String input = s.toString();
            //Log.i(TAG, "MyTextWatcher afterTextChanged >" + input);
            if (input.contains("\n")) {
                input = input.replace("\n", "");
                text_tosend.setText(input);
                text_devicename.requestFocus();
                imm.hideSoftInputFromWindow(text_tosend.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                return;
            }
            button_send.setEnabled(true);
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }

}
