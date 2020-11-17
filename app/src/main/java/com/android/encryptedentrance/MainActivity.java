package com.android.encryptedentrance;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;
import pub.devrel.easypermissions.PermissionRequest;

public class MainActivity extends AppCompatActivity implements SensorEventListener, EasyPermissions.PermissionCallbacks {

    private static final String TAG = "oof";
    private static final String MY_PHONE_NUMBER = "055-222-4444";

    //LBL
    private TextView main_LBL_steps;

    //BTN
    private Button main_BTN_enter;

    //EDT
    private EditText main_EDT_input;

    //Data
    private boolean isFlashLightOn;
    private boolean isMoving;
    private int steps = -1;
    private int battery_level;

    //Sensors
    private SensorManager sensorManager;
    private Sensor stepsSensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        findViews();
        bindButtonsToListeners();
        regFlashLightCallback();

        //ACTIVITY_RECOGNITION requires an active permission check since API 29
        if (Build.VERSION.SDK_INT >= 29) {
            requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION, Manifest.permission.READ_CONTACTS});
        } else {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS});
        }

        main_LBL_steps.setText("" + 0);
        this.registerReceiver(broadcastReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    }

    private void findViews() {
        Log.d(TAG, "findViews");
        main_BTN_enter = findViewById(R.id.main_BTN_enter);
        main_EDT_input = findViewById(R.id.main_EDT_input);
        main_LBL_steps = findViewById(R.id.main_LBL_steps);
    }

    private void bindButtonsToListeners() {
        Log.d(TAG, "bindButtonsToListeners");
        main_BTN_enter.setOnClickListener(onClickListener);
    }

    private void regFlashLightCallback() {
        CameraManager cm = getSystemService(CameraManager.class);
        cm.registerTorchCallback(myTorchCallBack, null);
    }

    private void requestPermissions(String[] perms) {
        if (EasyPermissions.hasPermissions(this, perms)) {
            Log.d("TAG", "Permission granted!");
        } else {
            Log.d("TAG", "Permission denied!");

            int rationale;
            if (Build.VERSION.SDK_INT >= 29) {
                rationale = R.string.permission_contacts_and_activity_recognition_rationale;
            } else {
                rationale = R.string.permission_contacts_rationale;
            }

            EasyPermissions.requestPermissions(new PermissionRequest.Builder(this, 1111, perms)
                    .setRationale(rationale)
                    .setPositiveButtonText(R.string.persmission_positive)
                    .setNegativeButtonText(R.string.permission_negative)
                    .setTheme(R.style.Theme_AppCompat)
                    .build());
        }
    }

    //The button listener for the single button of the application
    //Allowing the user to "enter" the application only if:
    //
    //Device flashlight is on.
    //(Device Android Version - Device Volume Level) % the difference between the clock's minutes equals 0
    //The phone number 055-222-4444 exists in the device's contact list
    //User's input in the EditText is the current battery level
    //User walked at least 10 steps before attempting to enter
    //
    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "onClick");
            Log.d(TAG, "    Checking secret parameters");
            int volume_level = getVolumeDifference();
            int android_version = Build.VERSION.SDK_INT;
            int minutes = getMinuteDifference();
            String input = main_EDT_input.getText().toString().trim();
            int input_int;
            if (input.equals("")) {
                input_int = -1;
            } else {
                input_int = Integer.parseInt(input);
            }

            //Change minute difference to 1 in case its actually 0 to be able to do % with it
            if (minutes == 0) {
                minutes = 1;
            }
            if ((android_version - volume_level) % minutes == 0 && checkPhoneNumber(getApplicationContext(), MY_PHONE_NUMBER) && input_int == battery_level && isFlashLightOn && steps >= 10) {
                Log.d(TAG, "        Secret parameters are valid");
                Toast.makeText(getApplicationContext(), "Congratulations! You're in!", Toast.LENGTH_SHORT).show();
            } else {
                Log.d(TAG, "        Secret parameters are invalid");
                Toast.makeText(getApplicationContext(), "WRONG!", Toast.LENGTH_SHORT).show();
            }
        }
    };

    //Get the difference between the music and the notification volume levels
    private int getVolumeDifference() {
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        return Math.abs(am.getStreamVolume(AudioManager.STREAM_MUSIC) - am.getStreamVolume(AudioManager.STREAM_NOTIFICATION));
    }

    //Get the difference between the current minutes
    private int getMinuteDifference() {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        String str = sdf.format(new Date());
        CharSequence minutes_singles = str.subSequence(4, 5);
        CharSequence minutes_dozens = str.subSequence(3, 4);
        return Math.abs(Integer.parseInt(minutes_dozens.toString()) - Integer.parseInt(minutes_singles.toString()));
    }

    //Check if a specific phone-number exists in your Contact list
    private boolean checkPhoneNumber(Context context, String num) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(num));
        String[] phone_number_projection = {ContactsContract.PhoneLookup._ID, ContactsContract.PhoneLookup.NUMBER, ContactsContract.PhoneLookup.DISPLAY_NAME};

        Cursor cur = context.getContentResolver().query(uri, phone_number_projection, null, null, null);
        try {
            if (cur.moveToFirst()) {
                return true;
            }
        } finally {
            if (cur != null)
                cur.close();
        }
        return false;
    }

    //Utilizing the flashlight callback to know when the flashlight is on or off so the program will be able to act accordingly
    private CameraManager.TorchCallback myTorchCallBack = new CameraManager.TorchCallback() {
        @Override
        public void onTorchModeUnavailable(@NonNull String cameraId) {
            super.onTorchModeUnavailable(cameraId);
        }

        @Override
        public void onTorchModeChanged(@NonNull String cameraId, boolean enabled) {
            super.onTorchModeChanged(cameraId, enabled);
            isFlashLightOn = enabled;
            Log.d(TAG, "onTorchModeChanged: " + enabled);
        }
    };

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            battery_level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        }
    };

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Log.d(TAG, "onPermissionsDenied:" + requestCode + ":" + perms.size());

        // Check whether the user denied any permissions and checked "NEVER ASK AGAIN."
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).build().show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
            // Do something after user returned from app settings screen
            Toast.makeText(this, "Returning from Setting tab", Toast.LENGTH_SHORT).show();
        }
    }

    //Register the step-count sensor when inside the application
    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null) {
            isMoving = true;
            stepsSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
            sensorManager.registerListener(this, stepsSensor, SensorManager.SENSOR_DELAY_GAME);
        } else {
            Toast.makeText(this, "StepCounter sensor not found!", Toast.LENGTH_SHORT).show();
            isMoving = false;
        }
    }

    //Unregister the step-count sensor when not inside the application
    @Override
    protected void onPause() {
        super.onPause();
        isMoving = false;
        if (sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null) {
            sensorManager.unregisterListener(this, stepsSensor);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.d(TAG, "onSensorChanged");
        if (isMoving && event.sensor == stepsSensor) {
            steps++;
            main_LBL_steps.setText("" + steps);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }
}