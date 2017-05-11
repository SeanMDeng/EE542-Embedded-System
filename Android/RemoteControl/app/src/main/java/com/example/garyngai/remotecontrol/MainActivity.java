package com.example.garyngai.remotecontrol;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.hardware.SensorEventListener;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.support.v7.app.ActionBarActivity;
import android.app.Activity;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class MainActivity extends AppCompatActivity implements SensorEventListener{

    private SensorManager sensorManager;
    private Sensor Acc_sensor;
    TextView TextView_AccData, TextView_Display, TargetIpAddress,PORTTextView,ClientIPAddressTextView;
    Button  Connect, ModeSwitch;
    EditText PORT,ClientIPAddress;

    private int DataPackage;

    Handler updateTextViewDisplayHandler;
    UdpServerThread udpServerThread;

   private static final String TAG = "debug";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView_AccData = (TextView) findViewById(R.id.AccDataDisplay);
        TextView_Display = (TextView) findViewById(R.id.CommandDisplay);
        TargetIpAddress = (TextView) findViewById(R.id.TargetIPAddress);

        Connect = (Button) findViewById(R.id.Connect);
        ModeSwitch = (Button) findViewById(R.id.ModeSwitch);

        PORT = (EditText) findViewById(R.id.PORT);
        ClientIPAddress = (EditText) findViewById(R.id.ClientIPAddress);

        PORTTextView = (TextView) findViewById(R.id.PORTTextView);
        ClientIPAddressTextView = (TextView) findViewById(R.id.ClientIPAddressTextView);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Acc_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, Acc_sensor,SensorManager.SENSOR_DELAY_NORMAL);


        updateTextViewDisplayHandler = new Handler();

        DisableAllButton();

        TargetIpAddress.setText( GetIpAddress() );

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

        //TextView_AccData.setText("AccData = " + String.format("%.02f", x) + "," + String.format("%.02f", y) + "," + String.format("%.02f", z));


    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void Connect(View view)
    {

        updateTextViewDisplayHandler.post(new UpdateUIThread("Connecting..."));
        udpServerThread = new UdpServerThread(Integer.parseInt(PORT.getText().toString()), ClientIPAddress.getText().toString() );
        if (Connect.getText().equals("Connect")) {

            PORT.setFocusable(false);
            PORT.setEnabled(false);
            ClientIPAddress.setFocusable(false);
            ClientIPAddress.setEnabled(false);
            Connect.setText("Disconnect");


            udpServerThread.start();


        }
        else if (Connect.getText().equals("Disconnect"))
        {
            PORT.setFocusable(true);
            PORT.setFocusable(true);
            ClientIPAddress.setFocusable(true);
            ClientIPAddress.setFocusable(true);

            Connect.setText("Connect");

            udpServerThread.setRunning(false);


        }
    }

    public void SwitchMode(View view)
    {
        if(ModeSwitch.getText().equals("Auto Mode"))
        {
            ModeSwitch.setText("Manual Mode");
        }
        else if(ModeSwitch.getText().equals("Manual Mode"))
        {
            ModeSwitch.setText("Auto Mode");
        }
    }

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
        boolean running;

        public UdpServerThread(int serverPort , String IPaddress) {
            super();
            this.serverPort = serverPort;
            this.IPaddress = IPaddress;
            this.buf = new byte[4];
            System.out.println("UDP server Thread created ");
        }

        public void setRunning(boolean running){
            this.running = running;
        }


        public void setBuf(byte[] buf) {
            this.buf = buf;

        }

        @Override
        public void run() {

            running = true;

            try {
                updateTextViewDisplayHandler.post(new UpdateUIThread("Starting UDP Server"));

                socket = new DatagramSocket(null);
                InetSocketAddress address = new InetSocketAddress(this.IPaddress.toString(), this.serverPort);


                updateTextViewDisplayHandler.post(new UpdateUIThread("UDP Server is running"));

                while(running){

                    socket.connect(address.getAddress(),this.serverPort);


                    String dString = "LOL\n";
                    buf = dString.getBytes();


                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.send(packet);

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
                if(socket != null)
                {
                    socket.close();
                }
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

    }
    private void DisableAllButton()
    {

    }
}
