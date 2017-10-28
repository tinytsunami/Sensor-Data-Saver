package com.example.tinytsunami.sensorsaver;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements SensorEventListener, SeekBar.OnSeekBarChangeListener {
    //========================================================
    // Other-Setting UI
    //========================================================
    private TextView TextViewFilename;
    private Spinner  SpinnerFileTag;
    private SeekBar  SeekBarFrequency;
    private TextView TextViewFrequency;
    private TextView TextViewTimestamp;
    private CheckBox CheckBoxNetworkUsed;
    private EditText EditTextNetworkAddress;
    private EditText EditTextNetworkAccount;
    private EditText EditTextNetworkPassword;
    private Button   ButtonSaveStart;
    private Button   ButtonSaveEnd;

    // Used for show timestamp UI
    private Handler UIHandler = new Handler();

    //========================================================
    // Temp Used
    //========================================================
    private int    delay;           // Frequency(ms) of save-file
    private String filename;        // Filename of save-file
    private String data;            // Data of save-file
    private String timestamp;       // Timestamp of save-file
    private String networkAddress;  // Address for using network
    private String fileTag;         // Used for filename

    //========================================================
    // Data Used
    //========================================================
    private float[] accelerometer;  // Include data for x, y, z
    private float[] gyroscope;      // Same as above
    private float[] magneticField;  // Same as above
    private float[] orientation;    // Same as above

    //========================================================
    // System Controller
    //========================================================
    private Resources resources;         // System Resources
    private SensorManager sensorManager; // Used for sensor
    private Calendar calender;           // Used for timer

    //========================================================
    // File Controller
    //========================================================
    private FileOutputStream fileStream;             // Used for save-file
    private Handler saveHandler = new Handler();     // Same as above
    private Runnable saveRunnable = new Runnable()   // Same as above
    {
        @Override
        public void run()
        {
            timestamp = getTimestamp();
            data = createData(timestamp);
            try
            {
                fileStream.write(data.getBytes());
                Log.i("Writing", data);
            }
            catch (Exception  e)
            {
                Log.d("Exception", e.getMessage());
            }
            saveHandler.postDelayed(this, delay);
        }
    };

    //========================================================
    // Permission Process
    //========================================================
    protected void processPermission()
    {
        String[] permissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        for(String permission : permissions)
        {
            int check = ContextCompat.checkSelfPermission(this, permission);
            if (check != PackageManager.PERMISSION_GRANTED)
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, 1);
            else
                Toast.makeText(this, "已取得權限", Toast.LENGTH_SHORT).show();
        }
    }

    //========================================================
    // Get UI Object
    //========================================================
    protected void getUIObject()
    {
        TextViewFilename = (TextView) findViewById(R.id.TextViewFilename);
        SpinnerFileTag = (Spinner) findViewById(R.id.SpinnerFileTag);
        SeekBarFrequency = (SeekBar) findViewById(R.id.SeekBarFrequency);
        TextViewFrequency = (TextView) findViewById(R.id.TextViewFrequency);
        TextViewTimestamp = (TextView) findViewById(R.id.TextViewTimestamp);
        CheckBoxNetworkUsed = (CheckBox) findViewById(R.id.CheckBoxNetworkUsed);
        EditTextNetworkAddress = (EditText) findViewById(R.id.EditTextNetworkAddress);
        EditTextNetworkAccount = (EditText) findViewById(R.id.EditTextNetworkAccount);
        EditTextNetworkPassword = (EditText) findViewById(R.id.EditTextNetworkPassword);
        ButtonSaveStart = (Button) findViewById(R.id.ButtonSaveStart);
        ButtonSaveEnd = (Button) findViewById(R.id.ButtonSaveEnd);

        // Sensor setting
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    }

    //========================================================
    // Create File Stream
    //========================================================
    protected String getFilename()
    {
        return String.format("%s%s.txt", fileTag.substring(0, 2).toUpperCase(), getTimestamp()).replace(":", "");
    }

    //========================================================
    // Create File
    //========================================================
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    protected File createFile(String filename)
    {
        try {
            if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
            {
                File sdCard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                if (!sdCard.exists()) sdCard.mkdirs();
                return (new File(sdCard, filename));
            }
            else
                Toast.makeText(this, "SD卡讀取失敗", Toast.LENGTH_SHORT).show();
        } catch (Exception  e) {
            Log.d("Exception", e.getMessage());
        }
        return null;
    }

    //========================================================
    // Create File Stream
    //========================================================
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    protected void createFileStream(String filename) throws FileNotFoundException {
        fileStream = new FileOutputStream(this.createFile(filename));
    }

    //========================================================
    // Remove File Stream
    //========================================================
    protected void removeFileStream()
    {
        try {
            fileStream.close();
        } catch (Exception  e) {
            Log.d("Exception", e.getMessage());
        }
    }

    //========================================================
    // Get Timestamp
    //========================================================
    private String getTimestamp()
    {
        calender = Calendar.getInstance();
        int hour = calender.get(Calendar.HOUR_OF_DAY);
        int minute = calender.get(Calendar.MINUTE);
        int second = calender.get(Calendar.SECOND);
        int millisecond = calender.get(Calendar.MILLISECOND);
        return String.format("%d:%d:%d:%d", hour, minute, second, millisecond);
    }

    //========================================================
    // Create Data
    //========================================================
    private String createData(String timestamp)
    {
        String tmp = timestamp + ", ";
        tmp += String.format("%.6f, %.6f, %.6f, ", accelerometer[0], accelerometer[1], accelerometer[2]);
        tmp += String.format("%.6f, %.6f, %.6f, ", gyroscope[0], gyroscope[1], gyroscope[2]);
        tmp += String.format("%.6f, %.6f, %.6f, ", orientation[0], orientation[1], orientation[2]);
        tmp += String.format("%.6f, %.6f, %.6f\n", magneticField[0], magneticField[1], magneticField[2]);
        return tmp;
    }

    //========================================================
    // Create Spinner
    //========================================================
    private void createSpinner()
    {
        final String[] items = resources.getStringArray(R.array.file_tags);
        ArrayAdapter<String> fileTagList = new ArrayAdapter<>(MainActivity.this,
                android.R.layout.simple_spinner_dropdown_item,
                items);
        SpinnerFileTag.setAdapter(fileTagList);
        SpinnerFileTag.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                fileTag = items[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    //========================================================
    // send FTP
    //========================================================
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    void ftpSend(String filename) throws IOException {
        //init.
        FTPClient ftpClient = new FTPClient();
        String address = String.valueOf(EditTextNetworkAddress.getText());
        String account = String.valueOf(EditTextNetworkAccount.getText());
        String password = String.valueOf(EditTextNetworkPassword.getText());

        //connection
        Log.i("FTP", address);
        ftpClient.connect(InetAddress.getByName(address), 21);
        ftpClient.login(account, password);
        ftpClient.changeWorkingDirectory("/");
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

        //send file
        FileInputStream stream = new FileInputStream(this.createFile(filename));
        BufferedInputStream buff = new BufferedInputStream(stream);
        //ftpClient.enterLocalPassiveMode();
        ftpClient.storeFile(filename, buff);

        //disconnection
        buff.close();
        ftpClient.logout();
        ftpClient.disconnect();
    }

    //========================================================
    // Override Keyboard key down/up event
    //========================================================
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try
        {
            fileStream.write(String.format("%s, Touch\n", getTimestamp()).getBytes());
            Log.i("Writing", data);
        }
        catch (Exception  e)
        {
            Log.d("Exception", e.getMessage());
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Pre-process
        resources = getResources();
        this.processPermission();
        this.getUIObject();
        this.createSpinner();

        // To set onchange function
        SeekBarFrequency.setOnSeekBarChangeListener(this);

        // Timer for UI timestamp
        UIHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                TextViewTimestamp.setText(getTimestamp());
                UIHandler.postDelayed(this, 1);
            }
        }, 1);

        // Timer for Start Save-file
        ButtonSaveStart.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            public void onClick(View v) {
                // Process file
                filename = getFilename();
                try {
                    createFileStream(filename);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                // Process UI
                TextViewFilename.setText(filename);
                ButtonSaveStart.setEnabled(false);
                ButtonSaveEnd.setEnabled(true);
                // Process handler
                saveHandler.removeCallbacks(saveRunnable);
                saveHandler.postDelayed(saveRunnable, delay);
            }
        });

        // Timer for End Save-file
        ButtonSaveEnd.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            public void onClick(View v) {
                // Process file
                removeFileStream();
                // Process UI
                TextViewFilename.setText(String.format("%s (saved)", filename));
                ButtonSaveStart.setEnabled(true);
                ButtonSaveEnd.setEnabled(false);
                // Process handler
                saveHandler.removeCallbacks(saveRunnable);
                //use network?
                if(CheckBoxNetworkUsed.isChecked())
                {
                    Thread thread = new Thread(){
                        public void run(){
                            try {
                                ftpSend(filename);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    };
                    thread.start();
                }
            }
        });
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onStop()
    {
        sensorManager.unregisterListener(this);
        this.removeFileStream();
        super.onStop();
    }

    @Override
    protected void onPause()
    {
        sensorManager.unregisterListener(this);
        super.onPause();
    }

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        float[] values = event.values;
        switch (event.sensor.getType())
        {
            case Sensor.TYPE_ACCELEROMETER:
                accelerometer = values;
                break;
            case Sensor.TYPE_GYROSCOPE:
                gyroscope = values;
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                magneticField = values;
                break;
        }
        // Calc. Orientation
        if(accelerometer != null && magneticField != null)
        {
            float[] tmp = new float[3];
            float[] R = new float[9];
            SensorManager.getRotationMatrix(R, null, accelerometer, magneticField);
            SensorManager.getOrientation(R, tmp);
            orientation = tmp;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b)
    {
        delay = i;
        TextViewFrequency.setText(String.format("%04dms", delay));
    }

    //========================================================
    // Override and Not used (pass)
    //========================================================
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}
}