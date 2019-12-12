/*
 * Copyright (c) 2017. Shell M. Shrader
 */

package com.shellware.flypadhelper;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.Process;
import android.util.Log;

import com.shellware.flypadhelper.FlypadListener.State;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class FlypadHelper extends BroadcastReceiver {

    private static final String CLASS_NAME = FlypadHelper.class.getSimpleName();


    private static final UUID FLYPAD_CONTROLLER_UUID = UUID.fromString("9e35fa00-4344-44d4-a2e2-0c7f6046878b");
    private static final UUID FLYPAD_CONTROLLER_NOTIFY_UUID = UUID.fromString("9e35fa01-4344-44d4-a2e2-0c7f6046878b");
    private static final UUID FLYPAD_CONTROLLER_NOTIFY_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static final UUID FLYPAD_INFORMATION_UUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");

    private static final UUID SERIAL_NUMBER = UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb");
    private static final UUID HARDWARE_VERSION = UUID.fromString("00002a27-0000-1000-8000-00805f9b34fb");
    private static final UUID FIRMWARE_VERSION = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb");
    private static final UUID SOFTWARE_VERSION = UUID.fromString("00002a28-0000-1000-8000-00805f9b34fb");

//    private static final UUID BATTERY_STATUS_UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");

    private static final int STATE_MESSAGE = 0;
    private static final int ACTION_MESSAGE = 1;

    private final FlypadHelper self;
    private final Context ctx;

    private final DoubleRange sourceRange = new DoubleRange() {
        @Override
        public double getLower() {
            return -110;
        }

        @Override
        public double getUpper() {
            return 110;
        }
    };
    private final DoubleRange targetRange = new DoubleRange() {
        @Override
        public double getLower() {
            return -1;
        }

        @Override
        public double getUpper() {
            return 1;
        }
    };

    private final BluetoothAdapter bluetoothAdapter;
    private final BluetoothLeScanner bluetoothLeScanner;

    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic notifyCharacteristic;
    private BluetoothGattDescriptor notifyDescriptor;

    private State state = State.UNKNOWN;

    private List<BluetoothGattCharacteristic> characteristics;

    private final FlypadInfo flypadInfo;

    private final HandlerThread flypadThread;
    private final FlypadHandler flypadHandler;

    private final Runnable connectingTimeoutRunnable;

    private boolean wasConnected = false;

    public FlypadHelper(final Context ctx) {
        logEvent(Log.INFO, CLASS_NAME, "create");

        this.self = this;
        this.ctx = ctx;
        flypadInfo = new FlypadInfo(ctx, self);

        final BluetoothManager bluetoothManager = (BluetoothManager) ctx.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager != null ? bluetoothManager.getAdapter() : null;
        bluetoothLeScanner = bluetoothAdapter != null ? bluetoothAdapter.getBluetoothLeScanner() : null;

        ctx.registerReceiver(this, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

        flypadThread = new HandlerThread("arpro4-flypad-thread", Process.THREAD_PRIORITY_MORE_FAVORABLE);
        flypadThread.start();

        flypadHandler = new FlypadHandler(self, flypadThread.getLooper());

        connectingTimeoutRunnable = () -> {
            if (state != State.CONNECTED) {
                btleGattCallback.onConnectionStateChange(null, 0, BluetoothAdapter.STATE_DISCONNECTED);
                startLeScan();
            }
        };
    }
    
    public void destroy() {
        logEvent(Log.INFO, CLASS_NAME, "destroy");

        ctx.unregisterReceiver(this);

        stopLeScan();

        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled() && bluetoothGatt != null) {
            if (notifyCharacteristic != null && notifyDescriptor != null) {
                logEvent(CLASS_NAME, "disabling notifications");

                bluetoothGatt.setCharacteristicNotification(notifyCharacteristic, false);
                notifyDescriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                bluetoothGatt.writeDescriptor(notifyDescriptor);
            }

            logEvent(CLASS_NAME, "deallocating gatt");
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
        }

        flypadHandler.removeCallbacks(connectingTimeoutRunnable);
        flypadHandler.removeMessages(STATE_MESSAGE);
        flypadHandler.removeMessages(ACTION_MESSAGE);
        flypadHandler.removeAllListeners();

        flypadThread.quitSafely();
    }

    public FlypadInfo getFlypadInfo() {
        return flypadInfo;
    }

    public State getState() { return state; }

    public boolean isConnected() { return (state == State.CONNECTED); }

    public boolean wasPreviouslyConnected() { return wasConnected; }

    public boolean addFlypadListener(FlypadListener flypadListener) {
        return flypadHandler.addFlypadListener(flypadListener);
    }

    public boolean removeFlypadListener(FlypadListener flypadListener) {
        return flypadHandler.removeFlypadListener(flypadListener);
    }

    public void startLeScan() {
        if (bluetoothAdapter != null &&
                bluetoothAdapter.isEnabled() &&
                bluetoothLeScanner != null &&
                state != State.CONNECTED) {

            final ScanFilter scanFilter = new ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid.fromString(FLYPAD_CONTROLLER_UUID.toString()))
                    .build();

            final ScanSettings scanSettings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                    .setReportDelay(0)
                    .build();

            bluetoothLeScanner.startScan(Collections.singletonList(scanFilter), scanSettings, scanCallback);
            sendStateChange(State.SCANNING);
        }
    }

    public void stopLeScan() {
        if (bluetoothAdapter != null && bluetoothLeScanner != null) {
            bluetoothLeScanner.stopScan(scanCallback);
            if (state == State.SCANNING) sendStateChange(State.DISCONNECTED);
        }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            final BluetoothDevice device = result.getDevice();

            if (device != null) {
                bluetoothLeScanner.stopScan(scanCallback);

                final String msg = String.format(Locale.US, "Found %s - %s",device.getName(), device.getAddress());
                logEvent(Log.INFO, CLASS_NAME, msg);
                flypadInfo.setName(device.getName());

                sendStateChange(State.CONNECTING);
                flypadHandler.removeCallbacks(connectingTimeoutRunnable);
                flypadHandler.postDelayed(connectingTimeoutRunnable, 10000);

                bluetoothGatt = device.connectGatt(ctx, false, btleGattCallback);
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    private final BluetoothGattCallback btleGattCallback = new BluetoothGattCallback() {

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            super.onServicesDiscovered(gatt, status);

            logEvent(CLASS_NAME, "onServicesDiscovered");

            if (BuildConfig.DEBUG) {
                for (BluetoothGattService service : bluetoothGatt.getServices()) {
                    logEvent(CLASS_NAME, "service=" + service.getUuid().toString() + " type=" + service.getType());

                    for (BluetoothGattCharacteristic bgc : service.getCharacteristics()) {
                        logEvent(CLASS_NAME, "     characteristic=" + bgc.getUuid());

                        for (BluetoothGattDescriptor desc : bgc.getDescriptors()) {
                            logEvent(CLASS_NAME, "          descriptor=" + desc.getUuid() + " " + desc.getPermissions());
                        }
                    }
                }
            }

            final BluetoothGattService service = gatt.getService(FLYPAD_INFORMATION_UUID);
            if (service == null) return;

            characteristics = service.getCharacteristics();

            gatt.readCharacteristic(characteristics.get(0));
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            final String value = new String(characteristic.getValue());

            logEvent(CLASS_NAME, characteristic.getUuid() + " = " + value);

            if (characteristic.getUuid().equals(SERIAL_NUMBER)) flypadInfo.setSerial(value);
            if (characteristic.getUuid().equals(HARDWARE_VERSION)) flypadInfo.setHardwareVersion(value);
            if (characteristic.getUuid().equals(SOFTWARE_VERSION)) flypadInfo.setSoftwareVersion(value);
            if (characteristic.getUuid().equals(FIRMWARE_VERSION)) flypadInfo.setFirmwareVersion(value);

            characteristics.remove(0);

            if (characteristics.size() > 0) {
                gatt.readCharacteristic(characteristics.get(0));
            } else {
                logEvent(CLASS_NAME, "enabling notifications");

                final BluetoothGattService service = gatt.getService(FLYPAD_CONTROLLER_UUID);

                notifyCharacteristic = service.getCharacteristic(FLYPAD_CONTROLLER_NOTIFY_UUID);
                notifyDescriptor = notifyCharacteristic.getDescriptor(FLYPAD_CONTROLLER_NOTIFY_DESCRIPTOR_UUID);

                bluetoothGatt.setCharacteristicNotification(notifyCharacteristic, true);
                notifyDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                bluetoothGatt.writeDescriptor(notifyDescriptor);

//                ConfigData.setGamepadPresent(true);
                sendStateChange(State.CONNECTED);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            final byte[] response = characteristic.getValue();

//            final StringBuilder data = new StringBuilder(20);
//
//            for (byte byteChar : response) {
//                data.append(String.format(Locale.US, "[%d] ", byteChar));
//            }
//
//            logEvent(CLASS_NAME, "raw=" + data.toString());

            final Bundle bundle = new Bundle();

            flypadInfo.setBatteryLevel(bundle, response[0]);
            flypadInfo.setAxes(bundle, decodeAxis(response[5]), decodeAxis(response[6]), decodeAxis(response[3]), decodeAxis(response[4]));
            
            flypadInfo.setButtons(bundle,
                                  (response[1] & 16) == 16,
                                  (response[1] & 8) == 8,
                                  (response[1] & 1) == 1, 
                                  (response[1] & 2) == 2,
                                  (response[1] & 4) == 4,
                                  (response[2] & 1) == 1, 
                                  (response[1] & 64) == 64,
                                  (response[1] & 128) == 128,
                                  (response[1] & 32) == 32,
                                  (response[2] & 2) == 2,
                                  (response[2] & 4) == 4);

            if (!bundle.isEmpty()) {
                final Message msg = Message.obtain();
                msg.what = ACTION_MESSAGE;
                msg.setData(bundle);
                flypadHandler.sendMessage(msg);
            }
        }

        private float decodeAxis(final byte value) {
            final int normal;

            // normalize axis to range -127 to +127
            if (value == -128) {
                normal = 0;
            } else {
                if (value > -1) {
                    // left side
                    normal =  Math.abs(127 - value) * -1;
                } else {
                    // right side
                    normal = value + 128;
                }
            }

            // now scale it to float range -1 to +1
            return (float) targetRange.scaleFrom(normal, sourceRange);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);

            logEvent(CLASS_NAME, "onDescriptorWrite");
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);

            logEvent(CLASS_NAME, "onCharacteristicWrite");
        }

        @Override
        public void onConnectionStateChange(@Nullable final BluetoothGatt gatt, final int status, final int newState) {
            if (gatt != null) super.onConnectionStateChange(gatt, status, newState);

            logEvent(CLASS_NAME, "onConnectionStateChange newState=" + newState);

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                bluetoothGatt.discoverServices();
            }

            if (newState == BluetoothProfile.STATE_DISCONNECTED && sendStateChange(State.DISCONNECTED)) {
//                ConfigData.setGamepadPresent(false);
                wasConnected = true;

                bluetoothGatt.disconnect();
                bluetoothGatt.close();
                bluetoothGatt = null;

                startLeScan();
            }
        }
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action == null) return;

        logEvent(CLASS_NAME, "onReceive=" + action);

        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            final int state = bluetoothAdapter.getState();

            if (state == BluetoothAdapter.STATE_ON) {
                if (wasConnected) {
                    startLeScan();
                }
                sendStateChange(State.BLE_ENABLED);
            } else {
                if (state == BluetoothAdapter.STATE_OFF) {
                    if (this.state == State.CONNECTED || this.state == State.CONNECTING) {
                        btleGattCallback.onConnectionStateChange(null, 0, BluetoothAdapter.STATE_DISCONNECTED);
                    } else {
                        stopLeScan();
                    }
                    sendStateChange(State.BLE_DISABLED);
                }
            }
        }
    }

    private boolean sendStateChange(final State newState ) {
        if (newState != state) {
            logEvent(CLASS_NAME, "sendStateChange newState=" + newState.name() + " oldState=" + state);

            final Message msg = Message.obtain();
            msg.what = STATE_MESSAGE;

            final Bundle bundle = new Bundle();
            bundle.putInt("newState", newState.ordinal());
            bundle.putInt("oldState", state.ordinal());
            msg.setData(bundle);

            flypadHandler.sendMessage(msg);

            state = newState;
            return true;
        }
        return false;
    }

    public static String toProper(String s) {

        final String ACTIONABLE_DELIMITERS = " '-/_"; // these cause the character following to be capitalized

        StringBuilder sb = new StringBuilder();
        boolean capNext = true;

        for (char c : s.toCharArray()) {
            c = (capNext)
                    ? Character.toUpperCase(c)
                    : Character.toLowerCase(c);
            sb.append(capNext ? " " : c);
            capNext = (ACTIONABLE_DELIMITERS.indexOf((int) c) >= 0); // explicit cast not needed
        }

        return sb.toString();
    }

    public static void logEvent(final String className, final String message) {
        logEvent(Log.VERBOSE, className, message);
    }
    public static void logEvent(final int severity, final String className, final String message) {
        logEvent(severity, className, message, null);
    }
    public static void logEvent(final int severity, final String className, final String message, final Throwable exception) {
        if (BuildConfig.DEBUG) {
            switch (severity) {
                case Log.VERBOSE:
                    Log.v(className, message, exception);
                    break;
                case Log.DEBUG:
                    Log.d(className, message, exception);
                    break;
                case Log.INFO:
                    Log.i(className, message, exception);
                    break;
                case Log.WARN:
                    Log.w(className, message, exception);
                    break;
                case Log.ERROR:
                    Log.e(className, message, exception);
                    break;
                default:
                    throw new RuntimeException("Invalid log level specified");
            }
        }
    }
}
