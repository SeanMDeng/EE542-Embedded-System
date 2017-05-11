package com.example.garyngai.remotecontrol;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.hardware.SensorEventListener;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.support.v7.app.ActionBarActivity;
import android.app.Activity;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class MainActivity extends AppCompatActivity implements SensorEventListener{

    private SensorManager sensorManager;
    private Sensor Acc_sensor;
    TextView TextView_AccData, TextView_Display, TargetIpAddress,PORTTextView,ClientIPAddressTextView;
    Button  Connect, ModeSwitch, ONandStopSwitch, Submit;
    EditText PORT,ClientIPAddress, XPosition, YPosition;

    private int DataPackage;

    Handler updateTextViewDisplayHandler;
    UdpServerThread udpServerThread;

    private static final String TAG = "debug";

    boolean WriteDataSwitch =false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView_AccData        = (TextView) findViewById(R.id.AccDataDisplay);
        TextView_Display        = (TextView) findViewById(R.id.CommandDisplay);
        TargetIpAddress         = (TextView) findViewById(R.id.TargetIPAddress);

        Connect                 = (Button) findViewById(R.id.Connect);
        ModeSwitch              = (Button) findViewById(R.id.ModeSwitch);
        ONandStopSwitch         = (Button) findViewById(R.id.ONandStopSwitch);
        Submit                  = (Button) findViewById(R.id.Submit);

        PORT                    = (EditText) findViewById(R.id.PORT);
        ClientIPAddress         = (EditText) findViewById(R.id.ClientIPAddress);
        XPosition               = (EditText) findViewById(R.id.Xposition);
        YPosition               = (EditText) findViewById(R.id.Yposition);

        PORTTextView            = (TextView) findViewById(R.id.PORTTextView);
        ClientIPAddressTextView = (TextView) findViewById(R.id.ClientIPAddressTextView);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Acc_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        sensorManager.unregisterListener(this,Acc_sensor);
        

        //sensorManager.registerListener(this, Acc_sensor,SensorManager.SENSOR_DELAY_NORMAL);




        updateTextViewDisplayHandler = new Handler();

        DisableAllButton();

        TargetIpAddress.setText( GetIpAddress() );

    }

    @Override
    protected void onStart(){
        super.onStart();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume(){
        super.onResume();
        sensorManager.registerListener(this, Acc_sensor, SensorManager.SENSOR_DELAY_NORMAL);
        PORT.setFocusableInTouchMode(true);

    }

    @Override
    protected void onPause() {
        // unregister listener
        super.onPause();
        sensorManager.unregisterListener(this);
        PORT.setFocusableInTouchMode(true);

    }

    @Override
    protected void onStop(){
        super.onStop();

        if(udpServerThread != null){
            udpServerThread.setRunning(false);
            udpServerThread = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();


    }


    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            getAccelerometer(event);
        }


    }

    private void getAccelerometer(SensorEvent event) {
        float[] values = event.values;
        // Save the values from the three axes into their corresponding variables
        float x = values[0];
        float y = values[1];
        float z = values[2];

        // TODO: Set DataBuffer
        // TODO: Send to RPI

        TextView_AccData.setText("AccData = " + String.format("%.02f", x) + "," + String.format("%.02f", y) + "," + String.format("%.02f", z));
        //String dString = "123\n";
        //udpServerThread.setBuf(dString.getBytes());
        //udpServerThread.setWriteDataSwitch(true);
        //udpServerThread.setWriteDataSwitch(false);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    // Button OnClick
    public void Connect(View view)
    {

        updateTextViewDisplayHandler.post(new UpdateUIThread("Connecting..."));
        udpServerThread = new UdpServerThread(Integer.parseInt(PORT.getText().toString()), ClientIPAddress.getText().toString() );


        if (Connect.getText().equals("Connect")) {


            udpServerThread.start();



            PORT.setFocusable(false);
            PORT.setEnabled(false);
            ClientIPAddress.setFocusable(false);
            ClientIPAddress.setEnabled(false);

            ONandStopSwitch.setEnabled(true);
            ModeSwitch.setEnabled(true);



            Connect.setText("Disconnect");



        }
        else if (Connect.getText().equals("Disconnect"))
        {
            DisableAllButton();
            PORT.setFocusable(true);
            PORT.setEnabled(true);
            ClientIPAddress.setFocusable(true);
            ClientIPAddress.setEnabled(true);

            Connect.setText("Connect");
            ONandStopSwitch.setText("ON");
            ModeSwitch.setText("Auto Mode");

            udpServerThread.setRunning(false);
            updateTextViewDisplayHandler.post(new UpdateUIThread("UDP Disconnected"));

        }
    }

    public void SwitchMode(View view)
    {
        // TODO: set DataBuffer the stop command
        // TODO: send to RPI

        if(ModeSwitch.getText().equals("Auto Mode"))
        {
            sensorManager.unregisterListener(this);
            ModeSwitch.setText("Remote Mode");

            XPosition.setEnabled(true);
            YPosition.setEnabled(true);
            ONandStopSwitch.setEnabled(false);
            Submit.setEnabled(true);
        }
        else if(ModeSwitch.getText().equals("Remote Mode"))
        {

            ModeSwitch.setText("Auto Mode");
            XPosition.setEnabled(false);
            YPosition.setEnabled(false);
            Submit.setEnabled(false);
            ONandStopSwitch.setEnabled(true);

        }
    }

    public void ONandStopSwitch(View view)
    {
        // TODO: check if Auto or Remote mode
        if(ModeSwitch.getText().equals("Auto Mode"))
        {
            // @ Remote Mode
            if(ONandStopSwitch.getText().equals("ON"))
            {
                // Turn on Acc
                DisableAllButton();
                ONandStopSwitch.setEnabled(true);
                ONandStopSwitch.setText("OFF");
                sensorManager.registerListener(this, Acc_sensor, SensorManager.SENSOR_DELAY_NORMAL);


            }
            else if(ONandStopSwitch.getText().equals("OFF"))
            {
                // Turn off Acc
                ONandStopSwitch.setText("ON");
                ModeSwitch.setEnabled(true);

                sensorManager.unregisterListener(this);
                TextView_AccData.setText("Acc Sensor OFF");
            }
        }
        else if(ModeSwitch.getText().equals("Remote Mode"))
        {
            // @ Auto Mode
            XPosition.setEnabled(true);
            YPosition.setEnabled(true);
            Submit.setEnabled(true);
            ModeSwitch.setEnabled(true);
            ONandStopSwitch.setText("ON");



        }
            // TODO: Remote mode
                // TODO: check if ON
                    // TODO: Turn on the ACC sensor
                    // TODO: set ONandStopSwitch.settext("OFF")

                // TODO: check if OFF
                    // TODO: Turn off the ACC sensor
                    // TODO: set ONandStopSwitch.settext("ON")


        // TODO: set DataBuffer the stop command
        // TODO: send to RPI

        // TODO: Check if Auto mode
    }

    public void Submit( View view)
    {
        DisableAllButton();
        ONandStopSwitch.setEnabled(true);
        ONandStopSwitch.setText("OFF");

        // TODO: write (X,Y) to DataBuffer
        // TODO: Send to RPI
        // TODO: Disable Submit Buttom
    }

    //Thread
    class UpdateUIThread implements Runnable {
        private String msg;

        public UpdateUIThread(String str)
        {
            this.msg = str;
        }

        @Override
        public void run()
        {
            TextView_Display.setText(msg);
        }
    }


    private class UdpServerThread extends Thread{

        int serverPort;
        String IPaddress;
        DatagramSocket socket;

        byte[] buf;
        boolean running, WriteDataSwitch;

        public UdpServerThread(int serverPort , String IPaddress) {
            super();
            this.serverPort = serverPort;
            this.IPaddress = IPaddress;
            this.buf = new byte[4];
            this.WriteDataSwitch = false;
            System.out.println("UDP server Thread created ");
        }

        public void setRunning(boolean running){
            this.running = running;
        }


        public void setBuf(byte[] buf) {
            this.buf = buf;

        }

        public void setWriteDataSwitch(boolean WriteDataSwitch) {
            this.WriteDataSwitch = WriteDataSwitch;
        }

        @Override
        public void run() {

            running = true;

            try
            {
                updateTextViewDisplayHandler.post(new UpdateUIThread("Starting UDP Server"));

                socket = new DatagramSocket(null);
                InetSocketAddress address = new InetSocketAddress(this.IPaddress.toString(), this.serverPort);


                updateTextViewDisplayHandler.post(new UpdateUIThread("UDP Server is running"));

                socket.connect(address.getAddress(), this.serverPort);

                while(running) {


                    if(this.WriteDataSwitch)
                    {

                        /*
                        String dString = "LOL\n";
                        buf = dString.getBytes();
                        */

                        DatagramPacket packet = new DatagramPacket(buf, buf.length);

                        socket.send(packet);

                    }

                }
            }
            catch (SocketException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            finally
            {

                    socket.close();
                }
            }
        }



    private String GetIpAddress()
    {
        String ip = "";
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces
                        .nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface
                        .getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        ip += "SiteLocalAddress: "
                                + inetAddress.getHostAddress() + "\n";
                    }

                }

            }

        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ip += "Something Wrong! " + e.toString() + "\n";
        }

        return ip;
    }
    private  void EnableAllButton()
    {
        ONandStopSwitch.setEnabled(true);
        ModeSwitch.setEnabled(true);
    }
    private void DisableAllButton()
    {
        ONandStopSwitch.setEnabled(false);
        ModeSwitch.setEnabled(false);
        XPosition.setEnabled(false);
        YPosition.setEnabled(false);
        Submit.setEnabled(false);
    }
}
