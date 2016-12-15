package ca.sheridancollege.pedomopal;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends Activity implements SensorEventListener {

    private SensorManager mSensorManager;
    private TextView count;
    private TextView humid;
    private TextView ambientTemp;
    private TextView timer;
    boolean activityRunning;

    private Handler customHandler = new Handler();

    private Button start;
    private Button stop;
    private Button save;
    private Button history;

    private long startTime = 0;
    private long timeInMilliSeconds = 0;
    private long updatedTime = 0;
    private long timeSwapBuff = 0;

    private Sensor countSensor;
    private Sensor temperatureSensor;
    private Sensor humiditySensor;

    private int stepsThisWalk;

    private DatabaseConnector db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new DatabaseConnector(this);
        count = (TextView)findViewById(R.id.stepsTaken);
        timer = (TextView)findViewById(R.id.timer);
        ambientTemp = (TextView)findViewById(R.id.ambientTemp);
        humid = (TextView)findViewById(R.id.humid);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        countSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if(countSensor != null) {
            mSensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_UI);
        } else {
            count.setText("Sensor not available");
        }

        temperatureSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        if(temperatureSensor != null) {
            mSensorManager.registerListener(this, temperatureSensor, SensorManager.SENSOR_DELAY_UI);
        } else {
            ambientTemp.setText("Sensor not available");
        }

        humiditySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);
        if(humiditySensor != null) {
            mSensorManager.registerListener(this, humiditySensor, SensorManager.SENSOR_DELAY_UI);
        } else {
            humid.setText("Sensor not available");
        }

        stepsThisWalk = 0;
        start = (Button)findViewById(R.id.start);
        stop = (Button)findViewById(R.id.stop);
        save = (Button)findViewById(R.id.save);
        history = (Button)findViewById(R.id.history);

        start.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                activityRunning = true;
                start.setEnabled(false);
                save.setEnabled(false);
                stop.setEnabled(true);
                startTime = SystemClock.uptimeMillis();
                customHandler.postDelayed(updateTimerThread, 0);
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start.setEnabled(true);
                save.setEnabled(true);
                stop.setEnabled(false);
                activityRunning = false;
                timeSwapBuff += timeInMilliSeconds;
                customHandler.removeCallbacks(updateTimerThread);
            }
        });

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timeSwapBuff = 0;
                stepsThisWalk = 0;
            }
        });

        addData();
        viewHistory();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, temperatureSensor, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, humiditySensor, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        if(activityRunning && sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            stepsThisWalk ++;
            count.setText(String.valueOf(stepsThisWalk));
        } else if(sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            ambientTemp.setText(String.valueOf(event.values[0]) + "°C");
        } else if(sensor.getType() == Sensor.TYPE_RELATIVE_HUMIDITY) {
            humid.setText(String.valueOf(event.values[0]) + "%");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private Runnable updateTimerThread = new Runnable() {
        @Override
        public void run() {
            timeInMilliSeconds = SystemClock.uptimeMillis() - startTime;

            updatedTime = timeSwapBuff + timeInMilliSeconds;

            int secs = (int) (updatedTime / 1000);
            int mins = secs / 60;
            int milliseconds = (int) (updatedTime % 1000);

            timer.setText("" + mins + ":" + String.format("%02d", secs%60) +
             ":" + String.format("%03d", milliseconds));
            customHandler.postDelayed(this, 0);
        }
    };

    public void addData(){
        save.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                       boolean isInserted = db.insertStat(String.valueOf(stepsThisWalk),
                                timer.getText().toString(), getDateAsString());
                        if(isInserted == true) {
                            Toast.makeText(MainActivity.this, "Data Inserted", Toast.LENGTH_LONG).show();
                            timeSwapBuff = 0;
                            stepsThisWalk = 0;
                            count.setText(String.valueOf(stepsThisWalk));
                            timer.setText("00:00:00");
                        } else {
                            Toast.makeText(MainActivity.this, "Data not Inserted", Toast.LENGTH_LONG).show();
                        }
                    }
                }
        );
    }

    public void viewHistory() {
        history.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v){
                        Cursor res = db.getAllStats();
                        if (res.getCount() == 0) {
                            // Show message if there are 0 fields in the database
                            showMessage("Error","No data found");
                            return;
                        }
                        StringBuffer buffer = new StringBuffer();
                        while (res.moveToNext()){
                            buffer.append("Id: " + res.getString(0)+"\n");
                            buffer.append("Steps: " + res.getString(1)+"\n");
                            buffer.append("Time: " + res.getString(2)+"\n");
                            buffer.append("Date: " + res.getString(3)+"\n\n");
                        }
                        //Show all data
                        showMessage("Walking History",buffer.toString());
                    }
                });
    }
    public void showMessage(String title, String Message){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setTitle(title);
        builder.setMessage(Message);
        builder.show();
    }

    public String getDateAsString() {
        Date date = Calendar.getInstance().getTime();

        DateFormat df = new SimpleDateFormat("MM/dd/yy");

        return df.format(date);
    }
}
