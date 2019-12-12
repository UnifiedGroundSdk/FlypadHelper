/*
 * Copyright (c) 2017. Shell M. Shrader
 */

package com.shellware.flypadhelper;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.Locale;

import androidx.preference.PreferenceManager;

import static com.shellware.flypadhelper.FlypadHelper.logEvent;
import static com.shellware.flypadhelper.FlypadHelper.toProper;

public class FlypadInfo {

    private static final String CLASS_NAME = FlypadInfo.class.getSimpleName();

    public enum FlypadButton {
        LEFT_THUMB,
        RIGHT_THUMB,
        ONE,
        TWO,
        A,
        B,
        UP_DOWN,
        LEFT_TOP,
        RIGHT_TOP,
        LEFT_BOTTOM,
        RIGHT_BOTTOM
    }

    public enum FlypadButtonState {
        RELEASED,
        PRESSED
    }

    public enum FlypadAxis {
        LEFT_X,
        LEFT_Y,
        RIGHT_X,
        RIGHT_Y
    }

    public enum FlypadButtonAction {
        NO_ACTION,              // 0
        EMERGENCY,              // 1
        TAKEOFF_OR_LAND,        // 2
        OPEN_SETTINGS,          // 3
        RECORD_VIDEO,           // 4
        TAKE_PICTURE,           // 5
        ANIMATION,              // 6
        FLAT_TRIM,              // 7
        GO_HOME,                // 8
        TOGGLE_HOVER_LOCK,      // 9
        TOGGLE_MAP,             // 10
        TOGGLE_BANKED_TURNS,    // 11
        TOGGLE_TRACK_ME,        // 12
        TOGGLE_FLIGHT_PLAN,
        TOGGLE_HEAD_MOVEMENT,   // 13
        CENTER_FIELD_OF_VIEW,   // 14
        CHANGE_HOME_TYPE,       // 15
        ZOOM_IN,                // 16
        ZOOM_OUT,               // 17
        YAW_LEFT,
        YAW_RIGHT,
        TOGGLE_COPILOT,
        CAMERA_PAN_LEFT,
        CAMERA_PAN_RIGHT,
        CAMERA_TILT_UP,
        CAMERA_TILT_DOWN,
        CHANGE_PREFERRED_STABILIZATION_MODE_MAMBO,
        TOGGLE_STABILIZATION_MODE_MAMBO,
        ACCESSORY_ACTION_MAMBO,
        TOGGLE_VTOL_MODE_WING_X,
        CHANGE_GEARBOX_WING_X,
        LOOP_ANIMATION_WING_X,
        ROLL_RIGHT_ANIMATION_WING_X,
        ROLL_LEFT_ANIMATION_WING_X,
        UPSIDE_DOWN_ANIMATION_WING_X
    }

    public enum FlypadAxisAction {
        NO_ACTION,              // 0
        ROLL,                   // 1
        PITCH,                  // 2
        YAW,                    // 3
        GAZ,                    // 4
        CAMERA_PAN,             // 5
        CAMERA_TILT             // 6
    }


    public class FlypadButtonMapping {
        private FlypadButton button;
        private FlypadButtonAction action;

        private boolean pressed;

        public FlypadButtonMapping(final FlypadButton button, final FlypadButtonAction action) {
            this.button = button;
            this.action = action;
        }

        public FlypadButton getButton() {
            return button;
        }

        public FlypadButtonAction getAction() {
            return action;
        }

        public String getKey() {
            return "FLYPAD_" + button.name();
        }

        public String getTitle() {
            return toProper(button.name());
        }

        public boolean isPressed() {
            return pressed;
        }

        public void setPressed(boolean pressed) {
            this.pressed = pressed;
        }
    }

    public class FlypadAxisMapping {
        private FlypadAxis axis;
        private FlypadAxisAction action;

        public FlypadAxisMapping(final FlypadAxis axis, final FlypadAxisAction action) {
            this.axis = axis;
            this.action = action;
        }

        public FlypadAxis getAxis() {
            return axis;
        }

        public FlypadAxisAction getAction() {
            return action;
        }

        public String getKey() {
            return "FLYPAD_" + axis.name();
        }

        public String getTitle() {
            return toProper(axis.name());
        }
    }

    private final Context ctx;
    private final FlypadHelper flypadHelper;

    private String name;
    private String serial;
    private String hardwareVersion;
    private String firmwareVersion;
    private String softwareVersion;

    private short batteryLevel;

    private float axisLeftY;
    private float axisLeftX;
    private float axisRightY;
    private float axisRightX;

    private boolean buttonLeftThumb;
    private boolean buttonRightThumb;

    private boolean buttonOne;
    private boolean buttonTwo;

    private boolean buttonUpDown;

    private boolean buttonA;
    private boolean buttonB;

    private boolean buttonLeftTop;
    private boolean buttonLeftBottom;
    private boolean buttonRightTop;
    private boolean buttonRightBottom;

    private ArrayList<FlypadAxisMapping> axisMappings;
    private ArrayList<FlypadButtonMapping> buttonMappings;

    public FlypadInfo(final Context ctx, final FlypadHelper flypadHelper) {
        this.ctx = ctx;
        this.flypadHelper = flypadHelper;

        axisMappings = buildAxisMappings();
        buttonMappings = buildButtonMappings();
    }

    public String getName() {
        return name;
    }

   void setName(String name) {
        if (!name.equals(this.name)) {
            this.name = name;
            logEvent(Log.INFO, CLASS_NAME, "name=" + name);
        }
    }

    public String getSerial() {
        return serial;
    }

    void setSerial(String serial) {
        if (!serial.equals(this.serial)) {
            this.serial = serial;
            logEvent(Log.INFO, CLASS_NAME, "serial=" + serial);
        }
    }

    public String getHardwareVersion() {
        return hardwareVersion;
    }

    void setHardwareVersion(String hardwareVersion) {
        if (!hardwareVersion.equals(this.hardwareVersion)) {
            this.hardwareVersion = hardwareVersion;
            logEvent(Log.INFO, CLASS_NAME, "hardwareVersion=" + hardwareVersion);
        }
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    void setFirmwareVersion(String firmwareVersion) {
        if (!firmwareVersion.equals(this.firmwareVersion)) {
            this.firmwareVersion = firmwareVersion;
            logEvent(Log.INFO, CLASS_NAME, "firmwareVersion=" + firmwareVersion);
        }
    }

    public String getSoftwareVersion() {
        return softwareVersion;
    }

    void setSoftwareVersion(String softwareVersion) {
        if (!softwareVersion.equals(this.softwareVersion)) {
            this.softwareVersion = softwareVersion;
            logEvent(Log.INFO, CLASS_NAME, "softwareVersion=" + softwareVersion);
        }
    }

    public short getBatteryLevel() {
        return batteryLevel;
    }

    void setBatteryLevel(final Bundle bundle, final short batteryLevel) {
        if (batteryLevel != this.batteryLevel) {
            this.batteryLevel = batteryLevel;
            bundle.putShort("batteryLevel", batteryLevel);
            logEvent(CLASS_NAME, "batteryLevel=" + batteryLevel);
        }
    }

    void setAxes(final Bundle bundle, final float axisLeftX, final float axisLeftY, final float axisRightX, final float axisRightY) {

        boolean changed = false;
        
        if (axisLeftX != this.axisLeftX) {
            this.axisLeftX = axisLeftX;
            changed = true;
            logEvent(CLASS_NAME, "axisLeftX=" + axisLeftX);
        }

        if (axisLeftY != this.axisLeftY) {
            this.axisLeftY = axisLeftY;
            changed = true;
            logEvent(CLASS_NAME, "axisLeftY=" + axisLeftY);
        }

        if (axisRightX != this.axisRightX) {
            this.axisRightX = axisRightX;
            changed = true;
            logEvent(CLASS_NAME, "axisRightX=" + axisRightX);
        }

        if (axisRightY != this.axisRightY) {
            this.axisRightY = axisRightY;
            changed = true;
            logEvent(CLASS_NAME, "axisRightY=" + axisRightY);
        }

        bundle.putBoolean("axesChanged", changed);

        bundle.putFloat("axisLeftX", axisLeftX);
        bundle.putFloat("axisLeftY", axisLeftY);
        bundle.putFloat("axisRightX", axisRightX);
        bundle.putFloat("axisRightY", axisRightY);
    }

    void setButtons(final Bundle bundle,
                           final boolean buttonA,
                           final boolean buttonB,
                           final boolean buttonUpDown,
                           final boolean buttonOne,
                           final boolean buttonTwo,
                           final boolean buttonLeftBottom,
                           final boolean buttonRightBottom,
                           final boolean buttonLeftTop,
                           final boolean buttonRightTop,
                           final boolean buttonLeftThumb,
                           final boolean buttonRightThumb) {

        boolean changed = false;
        
        if (buttonA != this.buttonA) {
            this.buttonA = buttonA;
            changed = true;
            setButtonPressed(bundle, FlypadButton.A, buttonA);
        }

        if (buttonB != this.buttonB) {
            this.buttonB = buttonB;
            changed = true;
            setButtonPressed(bundle, FlypadButton.B, buttonB);
        }

        if (buttonUpDown != this.buttonUpDown) {
            this.buttonUpDown = buttonUpDown;
            changed = true;
            setButtonPressed(bundle, FlypadButton.UP_DOWN, buttonUpDown);
        }

        if (buttonOne != this.buttonOne) {
            this.buttonOne = buttonOne;
            changed = true;
            setButtonPressed(bundle, FlypadButton.ONE, buttonOne);
        }

        if (buttonTwo != this.buttonTwo) {
            this.buttonTwo = buttonTwo;
            changed = true;
            setButtonPressed(bundle, FlypadButton.TWO, buttonTwo);
        }

        if (buttonLeftBottom != this.buttonLeftBottom) {
            this.buttonLeftBottom = buttonLeftBottom;
            changed = true;
            setButtonPressed(bundle, FlypadButton.LEFT_BOTTOM, buttonLeftBottom);
        }

        if (buttonRightBottom != this.buttonRightBottom) {
            this.buttonRightBottom = buttonRightBottom;
            changed = true;
            setButtonPressed(bundle, FlypadButton.RIGHT_BOTTOM, buttonRightBottom);
        }

        if (buttonLeftTop != this.buttonLeftTop) {
            this.buttonLeftTop = buttonLeftTop;
            changed = true;
            setButtonPressed(bundle, FlypadButton.LEFT_TOP, buttonLeftTop);
        }

        if (buttonRightTop != this.buttonRightTop) {
            this.buttonRightTop = buttonRightTop;
            changed = true;
            setButtonPressed(bundle, FlypadButton.RIGHT_TOP, buttonRightTop);
        }

        if (buttonLeftThumb != this.buttonLeftThumb) {
            this.buttonLeftThumb = buttonLeftThumb;
            changed = true;
            setButtonPressed(bundle, FlypadButton.LEFT_THUMB, buttonLeftThumb);
        }

        if (buttonRightThumb != this.buttonRightThumb) {
            this.buttonRightThumb = buttonRightThumb;
            changed = true;
            setButtonPressed(bundle, FlypadButton.RIGHT_THUMB, buttonRightThumb);
        }
        
        bundle.putBoolean("buttonsChanged", changed);
    }

    private void setButtonPressed(final Bundle bundle, final FlypadButton button, boolean pressed) {
        for (FlypadButtonMapping mapping : buttonMappings) {
            if (mapping.getButton() == button) {
                mapping.setPressed(pressed);
                break;
            }
        }

        bundle.putBoolean(button.name(), pressed);
        logEvent(CLASS_NAME, String.format(Locale.US, "%s pressed=%b",button.name(), pressed));
    }

    public boolean isButtonPressed(FlypadButtonAction action) {

        for (FlypadButtonMapping mapping : buttonMappings) {
            if (action == mapping.getAction() && mapping.isPressed()) {
                return true;
            }
        }

        return false;
    }

    public boolean isMappedYawButtonPressed() {

        for (FlypadButtonMapping mapping : buttonMappings) {
            if ((FlypadButtonAction.YAW_LEFT == mapping.getAction() || FlypadButtonAction.YAW_RIGHT == mapping.getAction()) && mapping.isPressed()) {
                return true;
            }
        }

        return false;

    }

    public ArrayList<FlypadAxisMapping> getAxisMappings() {
        return axisMappings;
    }

    public ArrayList<FlypadButtonMapping> getButtonMappings() {
        return buttonMappings;
    }

    public FlypadAxisMapping getAxisMappingByAxis(final FlypadAxis axis) {
        for (FlypadAxisMapping mapping : axisMappings) {
            if (mapping.getAxis() == axis) {
                return mapping;
            }
        }

        return null;
    }

    public FlypadButtonMapping getButtonMappingByButton(final FlypadButton button) {
        for (FlypadButtonMapping mapping : buttonMappings) {
            if (mapping.getButton() == button) {
                return mapping;
            }
        }

        return null;
    }

    public void refreshMappings() {
        axisMappings = buildAxisMappings();
        buttonMappings = buildButtonMappings();
    }

    private ArrayList<FlypadButtonMapping> buildButtonMappings() {
        final ArrayList<FlypadButtonMapping> mappings = new ArrayList<>();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

        for (FlypadButton button : FlypadButton.values()) {
            String actionName = prefs.getString("FLYPAD_" + button.name(), getFlypadButtonDefaultActionValue(button.name()));

            if (actionName.trim().length() == 0) {
                actionName = getFlypadButtonDefaultActionValue(button.name());
            }

            boolean found = false;
            for (FlypadButtonAction action : FlypadButtonAction.values()) {
                if (action.name().equals(actionName)) {
                    mappings.add(new FlypadButtonMapping(button, action));
                    found = true;
                    break;
                }
            }

            if (!found) {
                logEvent(Log.WARN, CLASS_NAME, "NOT FOUND Flypad button=" + button.name() + " action=" + actionName);
                mappings.add(new FlypadButtonMapping(button, FlypadButtonAction.NO_ACTION));
            }
        }

        return mappings;
    }

    private ArrayList<FlypadAxisMapping> buildAxisMappings() {
        final ArrayList<FlypadAxisMapping> mappings = new ArrayList<>();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

        for (FlypadAxis axis : FlypadAxis.values()) {
            String actionName = prefs.getString("FLYPAD_" + axis.name(), getFlypadAxisDefaultActionValue(axis.name()));

            if (actionName.trim().length() == 0) {
                actionName = getFlypadAxisDefaultActionValue(axis.name());
            }

            boolean found = false;
            for (FlypadAxisAction action : FlypadAxisAction.values()) {
                if (action.name().equals(actionName)) {
                    mappings.add(new FlypadAxisMapping(axis, action));
                    found = true;
                    break;
                }
            }

            if (!found) {
                logEvent(Log.WARN, CLASS_NAME, "NOT FOUND Flypad axis=" + axis.name() + " action=" + actionName);
                mappings.add(new FlypadAxisMapping(axis, FlypadAxisAction.NO_ACTION));
            }
        }

        return mappings;
    }

    public static String[] getFlypadButtonEntries(final boolean formatted) {

        final ArrayList<String> entries = new ArrayList<>();

        for (FlypadButton button : FlypadButton.values()) {
            entries.add(formatted ? toProper(button.name()) : button.name());
        }

        return entries.toArray(new String[entries.size()]);
    }

    public static String[] getFlypadAxisEntries(final boolean formatted) {

        final ArrayList<String> entries = new ArrayList<>();

        for (FlypadAxis axis : FlypadAxis.values()) {
            entries.add(formatted ? toProper(axis.name()) : axis.name());
        }

        return entries.toArray(new String[entries.size()]);
    }


    public static String[] getFlypadButtonActionEntries(final boolean formatted) {

        final ArrayList<String> entries = new ArrayList<>();

        for (FlypadButtonAction action : FlypadButtonAction.values()) {
            entries.add(formatted ? toProper(action.name()) : action.name());
        }

        return entries.toArray(new String[entries.size()]);
    }


    public static String[] getFlypadAxisActionEntries(final boolean formatted) {

        final ArrayList<String> entries = new ArrayList<>();

        for (FlypadAxisAction action : FlypadAxisAction.values()) {
            entries.add(formatted ? toProper(action.name()) : action.name());
        }

        return entries.toArray(new String[entries.size()]);
    }

    public static String getFlypadButtonDefaultActionValue(final String buttonName) {

        FlypadButton button = null;

        for (FlypadButton btn : FlypadButton.values()) {
            if (btn.name().equals(buttonName)) {
                button = btn;
                break;
            }
        }

        if (button == null) {
            logEvent(Log.WARN, CLASS_NAME, "Flypad button not found buttonName=" + buttonName + "]");
            return FlypadButtonAction.NO_ACTION.name();
        }

        switch (button) {
            case LEFT_THUMB:
                return FlypadButtonAction.TOGGLE_BANKED_TURNS.name();
            case RIGHT_THUMB:
                return FlypadButtonAction.TOGGLE_HOVER_LOCK.name();
            case ONE:
                return FlypadButtonAction.TOGGLE_MAP.name();
            case TWO:
                return FlypadButtonAction.OPEN_SETTINGS.name();
            case A:
                return FlypadButtonAction.TAKE_PICTURE.name();
            case B:
                return FlypadButtonAction.RECORD_VIDEO.name();
            case UP_DOWN:
                return FlypadButtonAction.TAKEOFF_OR_LAND.name();
            case LEFT_TOP:
                return FlypadButtonAction.TOGGLE_FLIGHT_PLAN.name();
            case RIGHT_TOP:
                return FlypadButtonAction.TOGGLE_TRACK_ME.name();
            case LEFT_BOTTOM:
                return FlypadButtonAction.CENTER_FIELD_OF_VIEW.name();
            case RIGHT_BOTTOM:
                return FlypadButtonAction.ANIMATION.name();
            default:
                return FlypadButtonAction.NO_ACTION.name();
        }
    }

    public static String getFlypadAxisDefaultActionValue(final String axisName) {
        FlypadAxis axis = null;

        for (FlypadAxis ax : FlypadAxis.values()) {
            if (axisName.equals(ax.name())) {
                axis = ax;
                break;
            }
        }

        if (axis == null) {
            logEvent(Log.WARN, CLASS_NAME, "Flypad axis not found axisName=" + axisName + "]");
            return FlypadAxisAction.NO_ACTION.name();
        }


        switch (axis) {
            case LEFT_X:
                return FlypadAxisAction.YAW.name();
            case LEFT_Y:
                return FlypadAxisAction.GAZ.name();
            case RIGHT_X:
                return FlypadAxisAction.ROLL.name();
            case RIGHT_Y:
                return FlypadAxisAction.PITCH.name();
            default:
                return FlypadAxisAction.NO_ACTION.name();
        }
    }
}
