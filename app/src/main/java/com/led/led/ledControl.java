package com.led.led;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.AsyncTask;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;


public class ledControl extends AppCompatActivity {

    Button btnOn, btnOff, btnDis, btnPercent2, btnPercent20, btnPercent100;
    TextView SensorRedValue, SensorGreenValue, SensorBlueValue, SensorIntensityValue;
    TextView RedLable, GreenLable, BlueLable, IntensityLable;
    String address = null;
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    //SPP UUID. Look for it
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    volatile boolean handByteReceiverHandlerIsOn;
    byte[] handIncomingBytesReadBuffer;
    Thread handByteReceiverHandler;
    int handIncomingBytesBufferPosition;
    int j=0;



    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Intent newint = getIntent();
        address = newint.getStringExtra(DeviceList.EXTRA_ADDRESS); //receive the address of the bluetooth device

        //view of the ledControl
        setContentView(R.layout.activity_led_control);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //call the widgtes
        btnOn = (Button)findViewById(R.id.led_on_button);
        btnOff = (Button)findViewById(R.id.led_off_button);
        btnPercent2 = (Button)findViewById(R.id.percent_button_2);
        btnPercent20 = (Button)findViewById(R.id.percent_button_20);
        btnPercent100 = (Button)findViewById(R.id.percent_button_100);
        btnDis = (Button)findViewById(R.id.button4);
        SensorRedValue = (TextView)findViewById(R.id.red_value_window);
        SensorGreenValue = (TextView)findViewById(R.id.green_value_window);
        SensorBlueValue = (TextView)findViewById(R.id.blue_value_window);
        SensorIntensityValue = (TextView)findViewById(R.id.intensity_value_window);
        RedLable = (TextView)findViewById(R.id.red_value);
        GreenLable = (TextView)findViewById(R.id.green_value);
        BlueLable = (TextView)findViewById(R.id.blue_value);
        IntensityLable = (TextView)findViewById(R.id.intensity_value);

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);


        new ConnectBT().execute(); //Call the class to connect

        //commands to be sent to bluetooth
        btnOn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                turnOnLed();      //method to turn on
            }
        });

        btnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                turnOffLed();   //method to turn off
            }
        });

        btnPercent2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                Set2Percent();
            }
        });
        btnPercent20.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                Set20Percent();
            }
        });
        btnPercent100.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                Set100Percent();
            }
        });
        btnDis.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Disconnect(); //close connection
            }
        });


    }

    private void Disconnect()
    {
        if (btSocket!=null) //If the btSocket is busy
        {
            try
            {
                handByteReceiverHandlerIsOn=false;
                btSocket.close(); //close connection
            }
            catch (IOException e)
            { msg("Error");}
        }
        finish(); //return to the first layout

    }

    private void turnOffLed()
    {
        if (btSocket!=null)
        {
            try
            {
                btSocket.getOutputStream().write(49);
            }
            catch (IOException e)
            {
                msg("Error");
            }
        }
    }

    private void turnOnLed()
    {
        if (btSocket!=null)
        {
            try
            {
                btSocket.getOutputStream().write(48);
            }
            catch (IOException e)
            {
                msg("Error");
            }
        }
    }

    private void Set2Percent()
    {
        if (btSocket!=null)
        {
            try
            {
                btSocket.getOutputStream().write(50);
            }
            catch (IOException e)
            {
                msg("Error");
            }
        }
    }
    private void Set20Percent()
    {
        if (btSocket!=null)
        {
            try
            {
                btSocket.getOutputStream().write(51);
            }
            catch (IOException e)
            {
                msg("Error");
            }
        }
    }
    private void Set100Percent()
    {
        if (btSocket!=null)
        {
            try
            {
                btSocket.getOutputStream().write(52);
            }
            catch (IOException e)
            {
                msg("Error");
            }
        }
    }


    // fast way to call Toast
    private void msg(String s)
    {
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_led_control, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute()
        {
            progress = ProgressDialog.show(ledControl.this, "Connecting...", "Please wait!!!");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            try
            {
                if (btSocket == null)// || !isBtConnected)
                {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();//start connection
                    //beginListenForData(); //Listen for bytes receive
                }
            }
            catch (IOException e)
            {
                ConnectSuccess = false;//if the try failed, you can check the exception here
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (!ConnectSuccess)
            {
                msg("Connection Failed. Is it a SPP Bluetooth? Try again.");
                finish();
            }
            else
            {
                msg("Connected.");
                isBtConnected = true;
                beginListenForData(); //Listen for bytes receive
            }
            progress.dismiss();
        }
    }

    //The BroadcastReceiver that listens for bluetooth broadcasts
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //Device found
            }
            else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {

                //if(!handByteReceiverHandlerIsOn) beginListenForData(); //Listen for bytes receive
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {

            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {

            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {

            }
        }
    };

    void beginListenForData()
    {
        final Handler handler = new Handler();
        final byte delimiter = 0x0A; //This is the ASCII code for a newline character

        handByteReceiverHandlerIsOn = true;
        handIncomingBytesBufferPosition = 0;
        handIncomingBytesReadBuffer = new byte[1024];
        SensorGreenValue.invalidate();
        handByteReceiverHandler = new Thread(new Runnable() {
            public void run() {
                while (!Thread.currentThread().isInterrupted() && handByteReceiverHandlerIsOn) {
                    try {
                        int bytesAvailable = btSocket.getInputStream().available();
                        if (bytesAvailable > 0) {
                            byte[] packetBytes = new byte[1024];
                            bytesAvailable = btSocket.getInputStream().read(packetBytes);
                            byte b;
                            if(j>7) j=0;
                            for (int i = 0; i < bytesAvailable; i++) {
                                b = packetBytes[i];
                                if (b == delimiter) {
                                    j++;
                                    byte[] encodedBytes = new byte[handIncomingBytesBufferPosition];
                                    System.arraycopy(handIncomingBytesReadBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    String data = new String(encodedBytes);
                                    data=data.trim();
                                    if(data.contentEquals("Green")) j=1;
                                    //handIncomingBytesBufferPosition = 0;
                                   // handler.post(new Runnable() {
                                   //     public void run() {
                                            if (j == 1) SensorRedValue.setText(data.toString());
                                            if (j == 2) RedLable.setText(data.toString());
                                            if (j == 3) SensorGreenValue.setText(data.toString());
                                            if (j == 4) GreenLable.setText(data.toString());
                                            if (j == 5) SensorBlueValue.setText(data.toString());
                                            if (j == 6) BlueLable.setText(data.toString());
                                            if (j == 7) SensorIntensityValue.setText(data.toString());
                                            if (j == 8) IntensityLable.setText(data.toString());
                                            SensorBlueValue.invalidate();
                                            SensorGreenValue.invalidate();
                                            SensorRedValue.invalidate();
                                            GreenLable.invalidate();
                                            BlueLable.invalidate();
                                            RedLable.invalidate();
                                    handIncomingBytesBufferPosition = 0;
                                     //   }
                                   // });

                                } else {
                                    handIncomingBytesReadBuffer[handIncomingBytesBufferPosition++] = b;
                                }
                            }
                        }
                    } catch (IOException ex) {
                        msg("Error");
                    }
                }
            }
        });

        handByteReceiverHandler.start();

    }







}
