package com.example.stackchanbluetoothcontrolapp;

import androidx.appcompat.app.AppCompatActivity;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
    TextView text_device_name;
    private Button button_f_speed_low;
    private Button button_f_speed_mid;
    private Button button_f_speed_high;
    private Button button_b_speed_low;
    private Button button_b_speed_mid;
    private Button button_b_speed_high;
    private Button button_stop;
    private TextView control_log;

    private static final String F_HIGH = "0";
    private static final String F_MID  = "30";
    private static final String F_LOW  = "60";
    private static final String STOP   = "90";
    private static final String B_LOW  = "120";
    private static final String B_MID  = "150";
    private static final String B_HIGH = "180";
    private static final int REQUEST_MULTI_PERMISSIONS = 101;

    //
    private final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    if (result.getData() != null) {
                        Log.i(TAG, "onActivityResult() Bluetooth function is available.");
                    }
                }
            });

    /**
     * Activityの初期化処理
     * 各ボタンやフィールド、BLE(Bluetooth Low Energy)などの設定
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ImaveViewに設定した画像を読み込む処理
        ImageView myimage = findViewById(R.id.imageView);
        myimage.setImageResource(R.drawable.stk);

        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        button_connect = findViewById(R.id.button_connect);
        button_connect.setOnClickListener(this);
        button_disconnect = findViewById(R.id.button_disconnect);
        button_disconnect.setOnClickListener(this);
        text_device_name = findViewById(R.id.text_device_name);

        button_f_speed_low = findViewById(R.id.f_speed_low);
        button_f_speed_low.setOnClickListener(this);
        button_f_speed_mid = findViewById(R.id.f_speed_mid);
        button_f_speed_mid.setOnClickListener(this);
        button_f_speed_high = findViewById(R.id.f_speed_high);
        button_f_speed_high.setOnClickListener(this);

        button_b_speed_low = findViewById(R.id.b_speed_low);
        button_b_speed_low.setOnClickListener(this);
        button_b_speed_mid = findViewById(R.id.b_speed_mid);
        button_b_speed_mid.setOnClickListener(this);
        button_b_speed_high = findViewById(R.id.b_speed_high);
        button_b_speed_high.setOnClickListener(this);

        button_stop = findViewById(R.id.stop);
        button_stop.setOnClickListener(this);

        /*
        button_send = findViewById(R.id.button_send);
        button_send.setOnClickListener(this);
        button_clear = findViewById(R.id.button_clear);
        button_clear.setOnClickListener(this);
        text_tosend = findViewById(R.id.text_tosend);
        text_tosend.addTextChangedListener(new MyTextWatcher());
         */

        control_log = findViewById(R.id.control_log);
        control_log.setMovementMethod(new ScrollingMovementMethod());

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
                text_device_name.setText("Device: " + device);
                Toast.makeText(MainActivity.this, "Device: " + device + " Connected.", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onBLEServiseStarted() {
            }

            @Override
            public void onBLEDisconnected() {
                button_connect.setEnabled(true);
                button_disconnect.setEnabled(false);
                text_device_name.setText("no device");
            }

            @Override
            public void onMessageReceived(String text) {
                String tmp = control_log.getText().toString();
                control_log.setText(tmp + text + "\n");
            }

            @Override
            public void onMessageWritten() {
                button_f_speed_low.setEnabled(true);
                button_f_speed_mid.setEnabled(true);
                button_f_speed_high.setEnabled(true);
                button_b_speed_low.setEnabled(true);
                button_b_speed_mid.setEnabled(true);
                button_b_speed_high.setEnabled(true);
                button_stop.setEnabled(true);
                // button_send.setEnabled(true);
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

    /**
     * Androidのバージョン 12より前のBluetooth権限設定
     */
    private static final String[] BLE_PERMISSIONS = new String[]{
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
    };

    /**
     * Androidのバージョン 12以上向けのBluetooth権限設定
     */
    private static final String[] ANDROID_12_BLE_PERMISSIONS = new String[]{
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_CONNECT,
            //android.Manifest.permission.ACCESS_FINE_LOCATION,
    };

    /**
     * Androidのバージョン（12以上 or 12より前）によってBluetooth権限の処理を分岐
     */
    public static void requestBlePermissions(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            ActivityCompat.requestPermissions(activity, ANDROID_12_BLE_PERMISSIONS, requestCode);
        else
            ActivityCompat.requestPermissions(activity, BLE_PERMISSIONS, requestCode);
    }


    /**
     * 初回表示時、および、ポーズからの復帰時
     */
    @Override
    protected void onResume() {
        super.onResume();

        // Android端末のBluetooth機能の有効化要求
        if (mBluetoothAdapter.isEnabled()) {
            Log.i(TAG, "onResume() BluetoothAdapter is Enabled");
        }
    }

    /**
     * 別のアクティビティ（か別のアプリ）に移行したことで、バックグラウンドに追いやられた時
     */
    @Override
    protected void onPause() {
        super.onPause();
        mBluetoothLEWork.disconnect();
    }

    /**
     * アクティビティの終了直前
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBluetoothLEWork.disconnect();
    }

    /**
     * 各ボタンが押された時の処理
     * 各ボタンの識別子がsetOnClickListener(this)で渡されるので分岐処理
     */
    @Override
    public void onClick(View v) {
        if (v==button_connect) {                     //ペアリング済のBTデバイスを選択して接続
            button_connect.setEnabled( false );
            mBluetoothLEWork.selectandconnectDevice();
        } else if (v==button_disconnect) {            //BTデバイスを切断
            mBluetoothLEWork.disconnect();
        } else if (v==button_f_speed_low) {
            button_f_speed_low.setEnabled(false);
            mBluetoothLEWork.sendtext(F_LOW);
            mBluetoothLEWork.onMessageReceived("forward speed low : " + F_LOW + " degree\n");
        } else if (v==button_f_speed_mid) {
            button_f_speed_mid.setEnabled(false);
            mBluetoothLEWork.sendtext(F_MID);
            mBluetoothLEWork.onMessageReceived("forward speed mid : " + F_MID + " degree\n");
        } else if (v==button_f_speed_high) {
            button_f_speed_high.setEnabled(false);
            mBluetoothLEWork.sendtext(F_HIGH);
            mBluetoothLEWork.onMessageReceived("forward speed high : " + F_HIGH + " degree\n");
        } else if (v==button_b_speed_low) {
            button_b_speed_low.setEnabled(false);
            mBluetoothLEWork.sendtext(B_LOW);
            mBluetoothLEWork.onMessageReceived("back speed low : " + B_LOW + " degree\n");
        } else if (v==button_b_speed_mid) {
            button_b_speed_mid.setEnabled(false);
            mBluetoothLEWork.sendtext(B_MID);
            mBluetoothLEWork.onMessageReceived("back speed mid : " + B_MID + " degree\n");
        } else if (v==button_b_speed_high) {
            button_b_speed_high.setEnabled(false);
            mBluetoothLEWork.sendtext(B_HIGH);
            mBluetoothLEWork.onMessageReceived("back speed high : " + B_HIGH + " degree\n");
        } else if (v==button_stop) {
            button_stop.setEnabled(false);
            mBluetoothLEWork.sendtext("0");
            mBluetoothLEWork.onMessageReceived("STOP speed : " + STOP + " degree\n");
        }
    }
}