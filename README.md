[![](https://jitpack.io/v/synman/FlypadHelper.svg)](https://jitpack.io/#synman/FlypadHelper)

# Parrot Flypad Helper Library

![Parrot Flypad](https://user-images.githubusercontent.com/1299716/70670725-df21a000-1c47-11ea-9510-9b8d49c4c859.jpg)

This is an Android Library project meant to provide support for the Parrot Flypad on Android devices running Lollipop or greater.

### Usage:

```java
public class MainActivity extends AppCompatActivity implements FlypadListener {

    private FlypadHelper flypadHelper = null;
    private ArrayList<FlypadAxisMapping> axisMappings = null;

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
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        battery = findViewById(R.id.battery);
        state = findViewById(R.id.state);
        yaw = findViewById(R.id.yaw);
        gaz = findViewById(R.id.gaz);
        roll = findViewById(R.id.roll);
        pitch = findViewById(R.id.pitch);
        buttons = findViewById(R.id.buttons);

        flypadHelper = new FlypadHelper(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        final FlypadInfo fpi = new FlypadInfo(this, flypadHelper);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor edit = prefs.edit();

        edit.putString(fpi.getButtonMappingByButton(FlypadButton.LEFT_BOTTOM).getKey(), FlypadButtonAction.YAW_LEFT.name());
        edit.putString(fpi.getButtonMappingByButton(FlypadButton.RIGHT_BOTTOM).getKey(), FlypadButtonAction.YAW_RIGHT.name());

        edit.apply();

        fpi.refreshMappings();
        axisMappings = fpi.getAxisMappings();

        flypadHelper.addFlypadListener(this);

        if (flypadHelper.getState() == State.UNKNOWN || flypadHelper.getState() == State.DISCONNECTED) {
            flypadHelper.startLeScan();
        }
    }

    @Override
    public void onPause() {
        if (flypadHelper.getState() == State.SCANNING) {
            flypadHelper.stopLeScan();
        }

        flypadHelper.removeFlypadListener(this);

        super.onPause();
    }

    @Override
    public void onDestroy() {
        flypadHelper.destroy();
        super.onDestroy();
    }

    @Override
    public void onFlypadStateChanged(FlypadHelper flypadHelper, State newState, State oldState) {
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
        battery.setText(String.format(Locale.US, "%d%%", batteryLevel));
    }

    @Override
    public void onFlypadAxisValuesChanged(final FlypadHelper flypadHelper, float leftX, float leftY, float rightX, float rightY) {
        final float[] values = remapAxesBasedOnMappings(leftX, leftY, rightX, rightY);

        yaw.setText(String.format(Locale.US, "%.5f", values[0]));
        gaz.setText(String.format(Locale.US, "%.5f", values[1]));
        roll.setText(String.format(Locale.US, "%.5f", values[2]));
        pitch.setText(String.format(Locale.US, "%.5f", values[3]));

    }

    @Override
    public void onFlypadButtonChanged(final FlypadHelper flypadHelper, FlypadButton button, FlypadButtonState state) {
        final FlypadButtonMapping mapping = flypadHelper.getFlypadInfo().getButtonMappingByButton(button);

        buttons.setText(String.format(Locale.US, "button %s mapped to %s is %s", mapping.getButton().name(), mapping.getAction().name(), state.name()));

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
