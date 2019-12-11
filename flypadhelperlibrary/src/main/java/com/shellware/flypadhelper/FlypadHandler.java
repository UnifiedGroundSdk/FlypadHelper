/*
 * Copyright (c) 2017. Shell M. Shrader
 */

package com.shellware.flypadhelper;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.shellware.flypadhelper.FlypadListener.State;
import com.shellware.flypadhelper.FlypadInfo.FlypadButtonState;
import com.shellware.flypadhelper.FlypadInfo.FlypadButton;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

public class FlypadHandler extends Handler {
    private static final String CLASS_NAME = FlypadHandler.class.getSimpleName();

    private final FlypadHelper helper;
    private final Collection<FlypadListener> listeners = new ArrayList<>();
    private final Handler mainThreadHandler;

    FlypadHandler(@NotNull final FlypadHelper helper, @NotNull Looper looper) {
        super(looper);
        this.helper = helper;
        mainThreadHandler =  new Handler(Looper.getMainLooper());
    }

    synchronized boolean addFlypadListener(FlypadListener flypadListener) {
        return !listeners.contains(flypadListener) && listeners.add(flypadListener);
    }

    synchronized boolean removeFlypadListener(FlypadListener flypadListener) {
        return listeners.remove(flypadListener);
    }

    synchronized void removeAllListeners() {
        listeners.clear();
    }

    @Override
    public synchronized void handleMessage(Message msg) {
        final Bundle bundle = msg.getData();

        if (bundle.containsKey("newState")) {
            final State newState = State.values()[bundle.getInt("newState")];
            final State oldState = State.values()[bundle.getInt("oldState")];

            mainThreadHandler.post(() -> {
                for (FlypadListener flypadListener : listeners) {
                    flypadListener.onFlypadStateChanged(helper, newState, oldState);
                }
            });

            // state messages do not contain any other bundle data
            return;
        }

        if (bundle.containsKey("batteryLevel")) {
            mainThreadHandler.post(() -> {
                for (FlypadListener flypadListener : listeners) {
                    flypadListener.onFlypadBatteryLevelChanged(helper, bundle.getShort("batteryLevel"));
                }
            });
        }

        if (bundle.getBoolean("axesChanged")) {
            final float lx = bundle.getFloat("axisLeftX");
            final float ly = bundle.getFloat("axisLeftY");
            final float rx = bundle.getFloat("axisRightX");
            final float ry = bundle.getFloat("axisRightY");

//            logEvent(CLASS_NAME, String.format(Locale.US, "lx=%d ly=%d rx=%d ry=%d", lx, ly, rx, ry));

            mainThreadHandler.post(() -> {
                for (FlypadListener flypadListener : listeners) {
                    flypadListener.onFlypadAxisValuesChanged(helper, lx , ly, rx, ry);
                }
            });
        }

        if (bundle.getBoolean("buttonsChanged")) {
            for (FlypadButton button : FlypadButton.values()) {
                if (bundle.containsKey(button.name())) {
                    final boolean pressed = bundle.getBoolean(button.name());
                    final FlypadButtonState state = pressed ? FlypadButtonState.PRESSED : FlypadButtonState.RELEASED;

                    mainThreadHandler.post(() -> {
                        for (FlypadListener flypadListener : listeners) {
                            flypadListener.onFlypadButtonChanged(helper, button, state);
                        }
                    });
                }
            }
        }
    }
}