package com.android.encryptedentrance;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "oof";

    //BTN
    private Button main_BTN_enter;

    //EDT
    private EditText main_EDT_input;

    //Data
    private boolean isFlashLightOn;
    private boolean isMoving;
    private int steps = 0;

    //Sensors
    private SensorManager sensorManager;
    private Sensor countSensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        findViews();
        bindButtonsToListeners();
        regFlashLightCallback();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    }

    private void findViews() {
        Log.d(TAG, "findViews");
        main_BTN_enter = findViewById(R.id.main_BTN_enter);
        main_EDT_input = findViewById(R.id.main_EDT_input);
    }

    private void bindButtonsToListeners() {
        Log.d(TAG, "bindButtonsToListeners");
        main_BTN_enter.setOnClickListener(onClickListener);
    }

    private void regFlashLightCallback() {
        CameraManager cm = getSystemService(CameraManager.class);
        cm.registerTorchCallback(myTorchCallBack, null);
    }

    //The button listener for the single button of the application
    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "onClick");
            Log.d(TAG, "    Checking secret parameters");
            int volume_level = getVolumeDifference();
            int android_version = Build.VERSION.SDK_INT;
            int minutes = getMinuteDifference();

            Log.d(TAG, "    Parameters: \n" + "VOLUME: " + volume_level + "\nVERSION: " + android_version + "\nMINUTES: " + minutes + "\nSTEPS: " + steps);

            if (volume_level == 0 && isFlashLightOn) {
                Log.d(TAG, "        Secret parameters are valid");
                //finish();
            } else {
                Log.d(TAG, "        Secret parameters are invalid");
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

    //Counting steps when inside the application
    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null) {
            isMoving = true;
            countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            sensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Toast.makeText(this, "StepCounter sensor not found!", Toast.LENGTH_SHORT).show();
            isMoving = false;
        }
    }

    //Not counting steps when not inside the application
    @Override
    protected void onPause() {
        super.onPause();
        isMoving = false;
        if (sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null) {
            sensorManager.unregisterListener(this, countSensor);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (isMoving && event.sensor == countSensor) {
            steps = (int) event.values[0];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}