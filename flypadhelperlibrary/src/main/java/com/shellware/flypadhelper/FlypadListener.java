/*
 * Copyright (c) 2017. Shell M. Shrader
 */

package com.shellware.flypadhelper;

import com.shellware.flypadhelper.FlypadInfo.FlypadButtonState;

public interface FlypadListener {

    // indicates the state our service:
    enum State {
        BLE_ENABLED,
        BLE_DISABLED,
        SCANNING,
        CONNECTING,
        CONNECTED,
        DISCONNECTED,
        UNKNOWN
    }

    void onFlypadStateChanged(final FlypadHelper flypadHelper, final State newState, final State oldState);
    void onFlypadBatteryLevelChanged(final FlypadHelper flypadHelper, final short batteryLevel);
    void onFlypadAxisValuesChanged(final FlypadHelper flypadHelper, final float leftX, final float leftY, final float rightX, final float rightY);
    void onFlypadButtonChanged(final FlypadHelper flypadHelper, final FlypadInfo.FlypadButton button, final FlypadButtonState state);
}
