package com.shellware.flypadtestproject;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.shellware.flypadhelper.FlypadHelper;
import com.shellware.flypadhelper.FlypadInfo;
import com.shellware.flypadhelper.FlypadInfo.FlypadAxisAction;
import com.shellware.flypadhelper.FlypadInfo.FlypadAxisMapping;
import com.shellware.flypadhelper.FlypadInfo.FlypadButtonMapping;
import com.shellware.flypadhelper.FlypadInfo.FlypadButtonState;
import com.shellware.flypadhelper.FlypadListener;

import java.util.ArrayList;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import static com.shellware.flypadhelper.FlypadHelper.toProper;
import static com.shellware.flypadhelper.FlypadInfo.FlypadButton;
import static com.shellware.flypadhelper.FlypadInfo.FlypadButtonAction;

public class MainActivity extends AppCompatActivity implements FlypadListener {
    private final String CLASS_NAME = this.getClass().getSimpleName();

    private FlypadHelper flypadHelper = null;
    private ArrayList<FlypadAxisMapping> axisMappings = null;

    private TextView yawLabel;
    private TextView gazLabel;
    private TextView rollLabel;
    private TextView pitchLabel;

    private TextView battery;
    private TextView state;
    private TextView yaw;
    private TextView gaz;
    private TextView roll;
    private TextView pitch;
    private TextView buttons;

    private float lastX;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(CLASS_NAME, "onCreate");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        yawLabel = findViewById(R.id.yaw_label);
        gazLabel = findViewById(R.id.gaz_label);
        rollLabel = findViewById(R.id.roll_label);
        pitchLabel = findViewById(R.id.pitch_label);
        
        battery = findViewById(R.id.battery);
        state = findViewById(R.id.state);
        yaw = findViewById(R.id.yaw);
        gaz = findViewById(R.id.gaz);
        roll = findViewById(R.id.roll);
        pitch = findViewById(R.id.pitch);
        buttons = findViewById(R.id.buttons);

        flypadHelper = new FlypadHelper(this);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onResume() {
        Log.d(CLASS_NAME, "onResume");

        super.onResume();

        final FlypadInfo fpi = new FlypadInfo(this, flypadHelper);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor edit = prefs.edit();

        // map bottom buttons to yaw
        edit.putString(fpi.getButtonMappingByButton(FlypadButton.LEFT_BOTTOM).getKey(), FlypadButtonAction.YAW_LEFT.name());
        edit.putString(fpi.getButtonMappingByButton(FlypadButton.RIGHT_BOTTOM).getKey(), FlypadButtonAction.YAW_RIGHT.name());

        // map yaw to right x axis and roll to left x axis
        edit.putString(fpi.getAxisMappingByAxis(FlypadInfo.FlypadAxis.RIGHT_X).getKey(), FlypadAxisAction.YAW.name());
        edit.putString(fpi.getAxisMappingByAxis(FlypadInfo.FlypadAxis.LEFT_X).getKey(), FlypadAxisAction.ROLL.name());

        edit.apply();

        fpi.refreshMappings();
        axisMappings = fpi.getAxisMappings();

        // update our axes labels with mapping results
        for (FlypadAxisMapping axis : fpi.getAxisMappings()) {
            switch (axis.getAction()) {
                case ROLL:
                    rollLabel.setText("roll (" + axis.getTitle() + ")");
                    break;
                case PITCH:
                    pitchLabel.setText("pitch (" + axis.getTitle() + ")");
                    break;
                case YAW:
                    yawLabel.setText("yaw (" + axis.getTitle() + ")");
                    break;
                case GAZ:
                    gazLabel.setText("gaz (" + axis.getTitle() + ")");
                    break;
            }
        }

        flypadHelper.addFlypadListener(this);

        if (flypadHelper.getState() == State.BLE_ENABLED ||
                flypadHelper.getState() == State.DISCONNECTED ||
                flypadHelper.getState() == State.UNKNOWN) {

            flypadHelper.startLeScan();
        }
    }

    @Override
    public void onPause() {
        Log.d(CLASS_NAME, "onPause");

        if (flypadHelper.getState() == State.SCANNING) {
            flypadHelper.stopLeScan();
        }

        flypadHelper.removeFlypadListener(this);

        super.onPause();
    }

    @Override
    public void onDestroy() {
        Log.d(CLASS_NAME, "onDestroy");

        flypadHelper.destroy();
        super.onDestroy();
    }

    @Override
    public void onFlypadStateChanged(FlypadHelper flypadHelper, State newState, State oldState) {
        Log.d(CLASS_NAME, "onFlypadStateChanged new=" + newState + " old=" + oldState);
        state.setText(newState.name());

        switch (newState) {
            case BLE_ENABLED:
                // FlypadHelper has built-in logic for recovering
                // a device that was previously disconnected whle running
                if (!flypadHelper.wasPreviouslyConnected()) flypadHelper.startLeScan();
                break;

            case BLE_DISABLED:
            case SCANNING:
            case CONNECTING:
            case UNKNOWN:
            case CONNECTED:
            case DISCONNECTED:
                break;
        }
    }

    @Override
    public void onFlypadBatteryLevelChanged(final FlypadHelper flypadHelper, short batteryLevel) {
        Log.d(CLASS_NAME, "onFlypadBatteryLevelChanged level=" + batteryLevel);
        battery.setText(String.format(Locale.US, "%d%%", batteryLevel));
    }

    @Override
    public void onFlypadAxisValuesChanged(final FlypadHelper flypadHelper, float leftX, float leftY, float rightX, float rightY) {
        Log.d(CLASS_NAME, "onFlypadAxisValuesChanged lx=" + leftX + " ly=" + leftY + " rx=" + rightX + " ry=" + rightY);
        
        final float[] values = remapAxesBasedOnMappings(leftX, leftY, rightX, rightY);
        lastX = values[0];

        yaw.setText(String.format(Locale.US, "%.5f", lastX));
        gaz.setText(String.format(Locale.US, "%.5f", values[1]));
        roll.setText(String.format(Locale.US, "%.5f", values[2]));
        pitch.setText(String.format(Locale.US, "%.5f", values[3]));
    }

    @Override
    public void onFlypadButtonChanged(final FlypadHelper flypadHelper, FlypadButton button, FlypadButtonState state) {
        Log.d(CLASS_NAME, "onFlypadButtonChanged button=" + button.name() + " state=" + state.name());

        final FlypadButtonMapping mapping = flypadHelper.getFlypadInfo().getButtonMappingByButton(button);

        buttons.setText(String.format(Locale.US, "%s button mapped to %s is %s", mapping.getTitle(), toProper(mapping.getAction().name()), toProper(state.name())));

        if (state == FlypadButtonState.PRESSED) {
            // handle special case for mapping yaw to buttons
            switch (mapping.getAction()) {
                case YAW_LEFT:
                    lastX = -1f;
                    yaw.setText(String.format(Locale.US, "%.5f", lastX));
                    break;

                case YAW_RIGHT:
                    lastX = 1f;
                    yaw.setText(String.format(Locale.US, "%.5f", lastX));
                    break;
            }
        } else {
            switch (mapping.getAction()) {
                case YAW_LEFT:
                case YAW_RIGHT:
                    lastX = 0f;
                    yaw.setText(String.format(Locale.US, "%.5f", lastX));
                    break;

                //
                // other release events you probably want to trap
                //
                case CAMERA_PAN_LEFT:
                    break;
                case CAMERA_PAN_RIGHT:
                    break;

                case CAMERA_TILT_UP:
                    break;
                case CAMERA_TILT_DOWN:
                    break;

                case ZOOM_IN:
                    break;
                case ZOOM_OUT:
                    break;
            }
        }
    }

    private float[] remapAxesBasedOnMappings(final float leftX, final float leftY, final float rightX, final float rightY) {

        float yaw = leftX;
        float gaz = leftY;
        float roll = rightX;
        float pitch = rightY;

        // zero out any remapped axes
        for (FlypadAxisMapping mapping : axisMappings) {
            switch (mapping.getAxis()) {
                case LEFT_X:
                    if (mapping.getAction() != FlypadAxisAction.YAW) yaw = 0;
                    break;
                case LEFT_Y:
                    if (mapping.getAction() != FlypadAxisAction.GAZ) gaz = 0;
                    break;
                case RIGHT_X:
                    if (mapping.getAction() != FlypadAxisAction.ROLL) roll = 0;
                    break;
                case RIGHT_Y:
                    if (mapping.getAction() != FlypadAxisAction.PITCH) pitch = 0;
                    break;
            }
        }

        // now move any that are remapped
        for (FlypadAxisMapping mapping : axisMappings) {
            switch (mapping.getAction()) {
                case NO_ACTION:
                    break;

                case ROLL:
                    switch (mapping.getAxis()) {
                        case LEFT_X:
                            roll = leftX;
                            break;
                        case LEFT_Y:
                            roll = leftY;
                            break;
                        case RIGHT_Y:
                            roll = rightY;
                            break;
                    }
                    break;

                case PITCH:
                    switch (mapping.getAxis()) {
                        case LEFT_X:
                            pitch = leftX;
                            break;
                        case LEFT_Y:
                            pitch = leftY;
                            break;
                        case RIGHT_X:
                            pitch = rightX;
                            break;
                    }
                    break;

                case YAW:
                    if (flypadHelper.getFlypadInfo().isMappedYawButtonPressed()) {
                        // special handling if yaw is overriden via a button mapping
                        // in this case you could assign yaw to whatever the last value
                        // of X was according to the mapped button
                        yaw = lastX;
                        break;
                    }

                    switch (mapping.getAxis()) {
                        case LEFT_Y:
                            yaw = leftY;
                            break;
                        case RIGHT_X:
                            yaw = rightX;
                            break;
                        case RIGHT_Y:
                            yaw = rightY;
                            break;
                    }
                    break;

                case GAZ:
                    switch (mapping.getAxis()) {
                        case LEFT_X:
                            gaz = leftX;
                            break;
                        case RIGHT_X:
                            gaz = rightX;
                            break;
                        case RIGHT_Y:
                            gaz = rightY;
                            break;
                    }
                    break;

                case CAMERA_PAN:
                case CAMERA_TILT:
                    //
                    // camera pan and tilt handling belongs here
                    //
                    break;
            }
        }

        return new float[]{yaw, gaz, roll, pitch};
    }
}
