package com.example.test_app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.util.Set;
import java.util.UUID;

public class BluetoothLEWork {
    private final String TAG = "BluetoothLEWork";

    // 定数（Bluetooth LE Gatt UUID）
    // Private Service この２つのUUIDは交信相手と合わせる必要がある
    private static final UUID UUID_SERVICE_PRIVATE = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b");
    private static final UUID UUID_CHARACTERISTIC_PRIVATE = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8");
    // for Notification これは変更不可
    private static final UUID UUID_NOTIFY = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private final Context mContext;
    private BluetoothAdapter mBluetoothAdapter;    // BluetoothAdapter : Bluetooth処理で必要
    private String mDeviceAddress = "";    // デバイスアドレス
    private String mDeviceName = "";    // デバイスアドレス
    private BluetoothGatt mBluetoothGatt = null;    // Gattサービスの検索、キャラスタリスティックの読み書き

    public BluetoothLEWork(Context con) {
        mContext = con;
    }

    public boolean checkBluetooth() {
        boolean ready = true;
        // Android端末がBLUETOOTHをサポートしてるかの確認
        if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            Log.i(TAG, "checkBluetooth() PackageManager.FEATURE_BLUETOOTH is not suported");
            ready = false;
        } else {
            Log.i(TAG, "checkBluetooth() PackageManager.FEATURE_BLUETOOTH is suported");
        }
        return ready;
    }

    public BluetoothAdapter getBluetoothAdapter() {
        // Bluetoothアダプタの取得
        BluetoothManager bluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (null == mBluetoothAdapter) {    // Android端末がBluetoothをサポートしていない
            Log.i(TAG, "getBluetoothAdapter() bluetoothManager is not suported");
        } else {
            Log.i(TAG, "getBluetoothAdapter() bluetoothManager is suported");
        }
        return mBluetoothAdapter;
    }

    //callback用methods
    public void onBLEConectionFailed(String str) {
    }

    public void onBLEConnected(String device) {
    }

    public void onBLEServiseStarted() {
    }

    public void onBLEDisconnected() {
    }

    public void onMessageReceived(String text) {
    }

    public void onMessageWritten() {
    }

    public void checkpermission() {
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH)
                != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "checkpermission() BLUETOOTH permission denied");
        } else {
            Log.i(TAG, "checkpermission() BLUETOOTH permission granted");
        }
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_ADMIN)
                != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "checkpermission() BLUETOOTH_ADMIN permission denied");
        } else {
            Log.i(TAG, "checkpermission() BLUETOOTH_ADMIN permission granted");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "checkpermission() BLUETOOTH_ADMIN permission denied");
            } else {
                Log.i(TAG, "checkpermission() BLUETOOTH_ADMIN permission granted");
            }
        }
    }

    public void selectandconnectDevice() {

        //ペアリングされたデバイスの BluetoothDevice オブジェクトのセット
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        int number = pairedDevices.size();
        String[] deviceAddressList = new String[number];
        String[] deviceNameList = new String[number];
        if (pairedDevices.size() > 1) {
            int i = 0;
            for (BluetoothDevice device : pairedDevices) {
                deviceNameList[i] = device.getName();
                deviceAddressList[i] = device.getAddress();
                Log.i(TAG, "selectDevice() paired Device " + deviceNameList[i] + " " + deviceAddressList[i]);
                i++;
            }
            mDeviceName = deviceNameList[0];
            mDeviceAddress = deviceAddressList[0];
        } else {
            return;
        }

        new AlertDialog.Builder(mContext)
                .setTitle("Select Bluetooth Device")
                .setSingleChoiceItems(deviceNameList, 0, (dialog, item) -> {
                    //アイテムを選択したらここに入る
                    mDeviceName = deviceNameList[item];
                    mDeviceAddress = deviceAddressList[item];
                })
                .setPositiveButton("Select", (dialog, id) -> {
                    //Selectを押したらここに入る
                    Log.i(TAG, "selectDevice() Selected Device " + mDeviceName + " " + mDeviceAddress);
                    connect();
                })
                .setNegativeButton("Cancel", (dialog, id) -> {
                    //Cancelを押したらここに入る
                })
                .show();
    }

    // 接続
    public void connect() {
        if (mDeviceAddress.equals("")) {    // DeviceAddressが空の場合は処理しない
            return;
        }
        if (null != mBluetoothGatt) {    // mBluetoothGattがnullでないなら接続済みか、接続中。
            return;
        }

        // 接続
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
        mBluetoothGatt = device.connectGatt(mContext, false, mGattcallback);
    }

    // 切断
    public void disconnect() {
        if( null == mBluetoothGatt ) {
            return;
        }

        // 切断
        //   mBluetoothGatt.disconnect()ではなく、mBluetoothGatt.close()しオブジェクトを解放する。
        //   理由：「ユーザーの意思による切断」と「接続範囲から外れた切断」を区別するため。
        //   ①「ユーザーの意思による切断」は、mBluetoothGattオブジェクトを解放する。再接続は、オブジェクト構築から。
        //   ②「接続可能範囲から外れた切断」は、内部処理でmBluetoothGatt.disconnect()処理が実施される。
        //     切断時のコールバックでmBluetoothGatt.connect()を呼んでおくと、接続可能範囲に入ったら自動接続する。
        mBluetoothGatt.close();
        mBluetoothGatt = null;
        onBLEDisconnected();
    }

    public void sendtext(String str) {
        writeCharacteristic(UUID_SERVICE_PRIVATE, UUID_CHARACTERISTIC_PRIVATE, str);
    }

    // キャラクタリスティックの読み込み
    private void readCharacteristic(UUID uuid_service, UUID uuid_characteristic ) {
        if( null == mBluetoothGatt ) {
            return;
        }
        BluetoothGattCharacteristic blechar = mBluetoothGatt.getService( uuid_service ).getCharacteristic( uuid_characteristic );
        mBluetoothGatt.readCharacteristic( blechar );
    }

    // キャラクタリスティック通知の設定
    private void setCharacteristicNotification( UUID uuid_service, UUID uuid_characteristic, boolean enable ) {
        if( null == mBluetoothGatt ) {
            return;
        }
        BluetoothGattCharacteristic blechar = mBluetoothGatt.getService( uuid_service ).getCharacteristic( uuid_characteristic );
        mBluetoothGatt.setCharacteristicNotification( blechar, enable );
        BluetoothGattDescriptor descriptor = blechar.getDescriptor( UUID_NOTIFY ); //ここがnullになってしまう　P10liteで生ずる
        if (descriptor != null ) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        } else {
            onBLEConectionFailed("NOTIFICATION trouble - BlueTooth should be restart!");
        }
    }

    // キャラクタリスティックの書き込み
    private void writeCharacteristic( UUID uuid_service, UUID uuid_characteristic, String string ) {
        if( null == mBluetoothGatt ) {
            return;
        }
        BluetoothGattCharacteristic blechar = mBluetoothGatt.getService( uuid_service ).getCharacteristic( uuid_characteristic );
        blechar.setValue( string );
        mBluetoothGatt.writeCharacteristic( blechar );
    }

    // BluetoothGattコールバックオブジェクト
    private final BluetoothGattCallback mGattcallback = new BluetoothGattCallback() {
        // 接続状態変更（connectGatt()の結果として呼ばれる。）
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState ) {
            if( BluetoothGatt.GATT_SUCCESS != status ) {
                return;
            }
            if( BluetoothProfile.STATE_CONNECTED == newState ) {    // 接続完了
                //Log.i(TAG, "onConnectionStateChange() P11");
                mBluetoothGatt.discoverServices();    // サービス検索
                ((Activity)mContext).runOnUiThread(() -> {
                    //Log.i(TAG, "onConnectionStateChange() P12");
                    onBLEConnected(mDeviceName);
                });
                return;
            }
            if( BluetoothProfile.STATE_DISCONNECTED == newState ) {    // 切断完了（接続可能範囲から外れて切断された）
                // 接続可能範囲に入ったら自動接続するために、mBluetoothGatt.connect()を呼び出す。
                mBluetoothGatt.connect();
                ((Activity)mContext).runOnUiThread(() -> onBLEDisconnected());
            }
        }

        // サービス検索が完了したときの処理（mBluetoothGatt.discoverServices()の結果として呼ばれる。）
        @Override
        public void onServicesDiscovered( BluetoothGatt gatt, int status ) {
            if( BluetoothGatt.GATT_SUCCESS != status ) {
                return;
            }

            // 発見されたサービスのループ
            for( BluetoothGattService service : gatt.getServices() ) {
                // サービスごとに個別の処理
                if( ( null == service ) || ( null == service.getUuid() ) ) {
                    continue;
                }
                if( UUID_SERVICE_PRIVATE.equals( service.getUuid() ) ) {    // プライベートサービス
                    ((Activity)mContext).runOnUiThread(() -> {
                        onBLEServiseStarted();
                        readCharacteristic( UUID_SERVICE_PRIVATE, UUID_CHARACTERISTIC_PRIVATE);
                        new Handler(Looper.getMainLooper()).postDelayed(() -> setCharacteristicNotification( UUID_SERVICE_PRIVATE, UUID_CHARACTERISTIC_PRIVATE, true ), 500);    //0.5秒後
                    });
                }
            }
        }

        // キャラクタリスティックが読み込まれたときの処理
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status ) {
            if( BluetoothGatt.GATT_SUCCESS != status ) {
                return;
            }
            if( UUID_CHARACTERISTIC_PRIVATE.equals( characteristic.getUuid() ) ) {    // キャラクタリスティック1：データサイズは、8バイト（文字列を想定。半角文字8文字）
                final String strChara = characteristic.getStringValue( 0 );
                ((Activity)mContext).runOnUiThread(() -> {
                    // GUIアイテムへの反映
                    onMessageReceived(strChara);
                });
            }
        }

        // キャラクタリスティック変更が通知されたときの処理
        @Override
        public void onCharacteristicChanged( BluetoothGatt gatt, BluetoothGattCharacteristic characteristic ) {
            // キャラクタリスティックごとに個別の処理
            if( UUID_CHARACTERISTIC_PRIVATE.equals( characteristic.getUuid() ) ) {
                final String strChara = characteristic.getStringValue( 0 );
                ((Activity)mContext).runOnUiThread(() -> {
                    // GUIアイテムへの反映
                    onMessageReceived(strChara);
                });
            }
        }

        // キャラクタリスティックが書き込まれたときの処理
        @Override
        public void onCharacteristicWrite( BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status ) {
            if( BluetoothGatt.GATT_SUCCESS != status ) {
                return;
            }
            if( UUID_CHARACTERISTIC_PRIVATE.equals( characteristic.getUuid() ) ) {
                ((Activity)mContext).runOnUiThread(() -> onMessageWritten());
            }
        }
    };
}
