package com.th_nuernberg.homeekg.bluetooth_classic;

import android.app.Activity;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.github.mikephil.charting.components.Legend;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphView.LegendAlign;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewSeries.GraphViewSeriesStyle;
import com.jjoe64.graphview.LineGraphView;
import com.th_nuernberg.homeekg.Constants;
import com.th_nuernberg.homeekg.R;

import static com.th_nuernberg.homeekg.Constants.*;

public class SignalActivity extends Activity implements View.OnClickListener {

    //***Variables and Constants***//
    static boolean Lock = true;
    static boolean AutoScrollX = true;
    static boolean Stream = true;

    //Buttons
    Button bConnect, bDisconnect, bxMinus, bxPlus;
    ToggleButton tbLock, tbScroll, tbStream, darkMode;

    //GraphView
    static LinearLayout GraphView;
    LinearLayout background;
    static GraphView graphView;
    static GraphViewSeries Series;
    private static double graphLastXValue = 0;

    //TODO xAxis Bug Fix
    private static int xView = 10;


    //***Methods***//
    //onCreate
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Layout Setup
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.
                FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_signal);

        background = (LinearLayout) findViewById(R.id.bg);
        background.setBackgroundColor(Color.WHITE);

        //Set Handler
        BluetoothListActivity.setHandler(mHandler);

        //Initialize GraphView
        GraphView = (LinearLayout) findViewById(R.id.Graph);
        Series = new GraphViewSeries("EKG Signal",
                //Color and thickness of the line
                new GraphViewSeriesStyle(Color.RED, 4),
                new GraphViewData[] {new GraphViewData(0, 0)});

        graphView = new LineGraphView(this, "");
        graphView.setViewPort(0, xView);
        graphView.setScrollable(true);
        graphView.setScalable(true);
        graphView.setManualYAxis(true);
        graphView.setManualYAxisBounds(4095, 0);
        graphView.addSeries(Series);
        GraphView.addView(graphView);

        //Initialize Buttons
        bConnect = (Button)findViewById(R.id.bConnect);
        bConnect.setOnClickListener(this);

        bDisconnect = (Button)findViewById(R.id.bDisconnect);
        bDisconnect.setOnClickListener(this);

        bxMinus = (Button)findViewById(R.id.xMinus);
        bxMinus.setOnClickListener(this);

        bxPlus = (Button)findViewById(R.id.xPlus);
        bxPlus.setOnClickListener(this);

        tbLock = (ToggleButton)findViewById(R.id.tbLock);
        tbLock.setOnClickListener(this);

        tbScroll = (ToggleButton)findViewById(R.id.tbScroll);
        tbScroll.setOnClickListener(this);

        tbStream = (ToggleButton)findViewById(R.id.tbStream);
        tbStream.setOnClickListener(this);

        darkMode = (ToggleButton)findViewById(R.id.darkMode);
        darkMode.setOnClickListener(this);

        //TODO DEBUG GRAPH
        Thread thread = new Thread() {
            @Override
            public void run() {
                byte i[] = new byte[20];
                i[0] = 50;
                i[1] = 49;
                i[2] = 57;
                i[3] = 55;
                i[4] = 13;
                i[5] = 10;
                i[6] = 44;  //Comma
                i[7] = 50;
                i[8] = 57;
                i[9] = 49;
                i[10] = 55;
                i[11] = 13;
                i[12] = 10;
                i[13] = 44;  //Comma
                i[14] = 50;
                i[15] = 49;
                i[16] = 57;
                i[17] = 55;
                i[18] = 13;
                i[19] = 10;

                while (true) {
                    try {
                        try {
                            sleep(516);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        Message readMsg = mHandler.obtainMessage(
                                Constants.MESSAGE_READ, 0, -1,  i);
                        readMsg.sendToTarget();
                    } catch (Exception e) {
                        Log.d(TAG, "Input stream was disconnected", e);
                        break;
                    }
                }
            }
        };
        thread.start();
        //TODO DEBUG GRAPH
    }

    //Handler
    Handler mHandler = new Handler(Looper.myLooper()){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch(msg.what){
                case SUCCESS_CONNECT:
                    BluetoothListActivity.connectedThread =
                            new BluetoothListActivity.ConnectedThread((BluetoothSocket) msg.obj);
                    Toast.makeText(getApplicationContext(),
                            "Connected!", Toast.LENGTH_SHORT).show();
                    BluetoothListActivity.connectedThread.start();
                    break;

                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    int numBytes = (int) msg.arg1;

                    //TODO DEBUG
                    numBytes = 20;
                    //TODO DEBUG

                    String incomeString = new String(readBuf, 0, 20);
                    Log.d("Incoming String Value", incomeString);
                    Log.d("Incoming String Length", Integer.toString(numBytes));

                    incomeString = incomeString.replace("\r", "")
                            .replace("\n", "");

                    String[] string_parts = incomeString.split(",", 3);

                    for(int i = 0; i<3; i++) {
                        if (isIntegerNumber(string_parts[i])){
                            int yValue = Integer.parseInt(string_parts[i]);

                            if((yValue>0) && (yValue<4096) && (numBytes==20)) {
                                Series.appendData(new GraphViewData(graphLastXValue,
                                        yValue), AutoScrollX);
                            } else {
                                Log.d("Corrupted Bytes", Integer.toString(yValue));
                            }

                            //X-Axis Control
                            if (graphLastXValue >= xView && Lock == true){
                                Series.resetData(new GraphViewData[] {});
                                graphLastXValue = 0;
                            }

                            //TODO Set True Value
                            else graphLastXValue += 0.04;

                            if(Lock == true)
                                graphView.setViewPort(0, xView);
                            else
                                graphView.setViewPort(graphLastXValue - xView, xView);
                    }
                    //Update
                    GraphView.removeView(graphView);
                    GraphView.addView(graphView);
                    }
                break;
            }
        }

        public boolean isIntegerNumber(String num){
            try{
                Integer.parseInt(num);
            } catch(NumberFormatException nfe) {
                return false;
            }
            return true;
        }
    };

    //onClick
    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.bConnect:
                startActivity(new Intent(this, BluetoothListActivity.class));
                break;

            case R.id.bDisconnect:
                BluetoothListActivity.disconnect(this);
                break;

            case R.id.tbStream:
                if (tbStream.isChecked()){
                    if (BluetoothListActivity.connectedThread != null) {
                        byte[] message = new byte[1];
                        message[0] = 1;
                        BluetoothListActivity.connectedThread.write(message);
                    }
                } else {
                    if (BluetoothListActivity.connectedThread != null) {
                        byte[] message = new byte[1];
                        message[0] = 0;
                        BluetoothListActivity.connectedThread.write(message);
                    }
                }
                break;

            case R.id.tbScroll:
                if (tbScroll.isChecked()){
                    AutoScrollX = true;
                } else {
                    AutoScrollX = false;
                }
                break;

            case R.id.tbLock:
                if (tbLock.isChecked()){
                    Lock = true;
                } else {
                    Lock = false;
                }
                break;

            case R.id.darkMode:
                if (darkMode.isChecked()){
                    background.setBackgroundColor(Color.BLACK);
                } else {
                    background.setBackgroundColor(Color.WHITE);
                }
                break;

            case R.id.xMinus:
                if (xView > 1) xView--;
                break;

            case R.id.xPlus:
                if (xView < 30) xView++;
                break;
        }
    }

    //onBackPressed
    @Override
    public void onBackPressed() {
        if (BluetoothListActivity.connectedThread != null) {
            byte[] message = new byte[1];
            message[0] = 0;
            BluetoothListActivity.connectedThread.write(message);
        }
        super.onBackPressed();
    }
}
