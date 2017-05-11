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

    private byte[] DataPackage;

    Handler updateTextViewDisplayHandler;
    UdpServerThread udpServerThread;

    private static final String TAG = "debug";
    private float AccThreshold = 1.0f;


    final byte OnOffSwitch = 0b00000001;
    final byte AutoRemoteSwitch = 0b00000010;
    final byte SubmitXYSwitch = 0b00000100;


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

        DataPackage = new byte[5];

        updateTextViewDisplayHandler = new Handler();

        DisableAllButton();

        TargetIpAddress.setText( GetIpAddress() );

    }

    @Override
    protected void onStart(){
        super.onStart();

    }

    @Override
    protected void onResume(){
        super.onResume();

        sensorManager.unregisterListener(this);

        DisableAllButton();
        PORT.setFocusable(true);
        PORT.setEnabled(true);
        ClientIPAddress.setFocusable(true);
        ClientIPAddress.setEnabled(true);
        TextView_AccData.setText("AccData");
        Connect.setText("Connect");


    }

    @Override
    protected void onPause() {

        super.onPause();
        sensorManager.unregisterListener(this);
        DisableAllButton();
        if(udpServerThread != null)
        {
            DataPackageReset();
            udpServerThread.kill();
            udpServerThread = null;
        }


    }

    @Override
    protected void onStop(){
        super.onStop();

        if(udpServerThread != null)
        {
            DataPackageReset();
            udpServerThread.kill();
            udpServerThread = null;
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(udpServerThread != null)
        {
            DataPackageReset();
            udpServerThread.kill();
            udpServerThread = null;
        }

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

        byte Xlevel = 0;
        byte Ylevel = 0;

        // Determine X speed level
        if(Math.abs(x) <= AccThreshold )
        {
            Xlevel = 0;
        }
        else
        {
            double speedX = (Math.abs(x) - AccThreshold) / 0.881;
            speedX = Math.ceil(speedX);
            Xlevel = (byte) ( speedX * Math.copySign(1,x)  );

        }

       // Determine Y speed Level
        if(Math.abs(y) <= AccThreshold)
        {
            Ylevel = 0;
        }
        else
        {
            double speedY = (Math.abs(y) - AccThreshold) / 0.881;
            speedY = Math.ceil(speedY);
            Ylevel = (byte) ( speedY * Math.copySign(1,y) );
        }


        TextView_AccData.setText( "(" + DataPackage[0] + ", "+ DataPackage[1] + ", "+ DataPackage[2] + ", " + DataPackage[3] + "," + DataPackage[4] + ")");
//        TextView_AccData.setText("(" + String.format("%.02f", x) + "," + String.format("%.02f", y) + ")"
//                + "(" + String.valueOf(Xlevel) + "," + String.valueOf(Ylevel) + ")");

        // TODO: Set DataBuffer
        DataPackage[1] = Xlevel;
        DataPackage[2] = Ylevel;

        //TextView_AccData.setText("AccData = " + String.format("%.02f", x) + "," + String.format("%.02f", y) + "," + String.format("%.02f", z));
        //String dString = "1234\n";
        //udpServerThread.setBuf(dString.getBytes());

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    // Button OnClick
    public void Connect(View view)
    {

        updateTextViewDisplayHandler.post(new UpdateUIThread("Connecting..."));


        // Connected
        if (Connect.getText().equals("Connect")) {
            udpServerThread = new UdpServerThread(Integer.parseInt(PORT.getText().toString()), ClientIPAddress.getText().toString() );
            DataPackageReset();
            TextView_AccData.setText( "(" + DataPackage[0] + ", "+ DataPackage[1] + ", "+ DataPackage[2] + ", " + DataPackage[3] + "," + DataPackage[4] + ")");
            udpServerThread.start();



            PORT.setFocusable(false);
            PORT.setEnabled(false);
            ClientIPAddress.setFocusable(false);
            ClientIPAddress.setEnabled(false);

            ONandStopSwitch.setEnabled(true);
            ModeSwitch.setEnabled(true);



            Connect.setText("Disconnect");



        }
        // Not Connected
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

            DataPackageReset();
            TextView_AccData.setText( "(" + DataPackage[0] + ", "+ DataPackage[1] + ", "+ DataPackage[2] + ", " + DataPackage[3] + "," + DataPackage[4] + ")");
            udpServerThread.kill();
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
            DataPackage[0] |= AutoRemoteSwitch;
            DataPackage[1] = (byte) 0;
            DataPackage[2] = (byte) 0;
            TextView_AccData.setText( "(" + DataPackage[0] + ", "+ DataPackage[1] + ", "+ DataPackage[2] + ", " + DataPackage[3] + "," + DataPackage[4] + ")");

        }
        else if(ModeSwitch.getText().equals("Remote Mode"))
        {

            ModeSwitch.setText("Auto Mode");
            XPosition.setEnabled(false);
            YPosition.setEnabled(false);
            Submit.setEnabled(false);
            ONandStopSwitch.setEnabled(true);
            DataPackage[0] ^= AutoRemoteSwitch;
            TextView_AccData.setText( "(" + DataPackage[0] + ", "+ DataPackage[1] + ", "+ DataPackage[2] + ", " + DataPackage[3] + "," + DataPackage[4] + ")");
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
                DataPackage[0] |= OnOffSwitch;
                TextView_AccData.setText( "(" + DataPackage[0] + ", "+ DataPackage[1] + ", "+ DataPackage[2] + ", " + DataPackage[3] + "," + DataPackage[4] + ")");


            }
            else if(ONandStopSwitch.getText().equals("OFF"))
            {
                // Turn off Acc
                ONandStopSwitch.setText("ON");
                ModeSwitch.setEnabled(true);

                sensorManager.unregisterListener(this);
                TextView_AccData.setText("Acc Sensor OFF");

                DataPackage[0] ^= OnOffSwitch;
                DataPackage[1] = (byte) 0;
                DataPackage[2] = (byte) 0;
                TextView_AccData.setText( "(" + DataPackage[0] + ", "+ DataPackage[1] + ", "+ DataPackage[2] + ", " + DataPackage[3] + "," + DataPackage[4] + ")");

            }
        }
        else if(ModeSwitch.getText().equals("Remote Mode"))
        {
            // @ Auto Mode
                // Turn off and back to Remote OFF state
                //XPosition.setEnabled(true);
                //YPosition.setEnabled(true);
                //Submit.setEnabled(true);
                ModeSwitch.setEnabled(true);
                ModeSwitch.setText("Auto Mode");
                ONandStopSwitch.setText("ON");
                DataPackage[0] ^= OnOffSwitch | SubmitXYSwitch | AutoRemoteSwitch;
                DataPackage[3] = (byte) 0;
                DataPackage[4] = (byte) 0;
                TextView_AccData.setText( "(" + DataPackage[0] + ", "+ DataPackage[1] + ", "+ DataPackage[2] + ", " + DataPackage[3] + "," + DataPackage[4] + ")");

        }

    }

    public void Submit( View view)
    {
        String x = XPosition.getText().toString();
        String y = YPosition.getText().toString();

        DataPackage[0] |= SubmitXYSwitch | OnOffSwitch;
        DataPackage[3] = Byte.valueOf(x);
        DataPackage[4] = Byte.valueOf(y);
        TextView_AccData.setText( "(" + DataPackage[0] + ", "+ DataPackage[1] + ", "+ DataPackage[2] + ", " + DataPackage[3] + "," + DataPackage[4] + ")");

        DisableAllButton();
        ONandStopSwitch.setEnabled(true);
        ONandStopSwitch.setText("OFF");
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


    private class UdpServerThread extends Thread
    {

        int serverPort;
        String IPaddress;
        DatagramSocket socket;
        DatagramPacket packet;


        byte[] buf;
        boolean running;

        public UdpServerThread()
        {
            super();
            this.buf = new byte[5];
            System.out.println("UDP server Thread created ");
            this.running = true;

        }

        public UdpServerThread(int serverPort , String IPaddress)
        {
            super();
            this.serverPort = serverPort;
            this.IPaddress = IPaddress;
            this.buf = new byte[5];
            this.running = true;

        }

        public void kill(){
            this.running = false;
        }
        public void setBuf(byte[] buf)
        {
            this.buf = buf;

        }
        public void setServerPort(int serverPort)
        {
            this.serverPort = serverPort;
        }
        public void setIPaddress(String IPaddress)
        {
            this.IPaddress = IPaddress;
        }


        @Override
        public void run()
        {



            try
            {
                updateTextViewDisplayHandler.post(new UpdateUIThread("Starting UDP Server"));

                socket = new DatagramSocket(null);
                InetSocketAddress address = new InetSocketAddress(this.IPaddress.toString(), this.serverPort);


                updateTextViewDisplayHandler.post(new UpdateUIThread("UDP Server is running"));

                socket.connect(address.getAddress(), this.serverPort);


                while(running)
                {

                    DatagramPacket packet = new DatagramPacket(DataPackage,DataPackage.length);
                    socket.send(packet);

                    // Sleep thread
                    try
                    {

                        this.sleep(100);
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }



                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            finally
            {
                if(socket !=null)
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
    private void DataPackageReset()
    {
        DataPackage[0] = (byte) 0;
        DataPackage[1] = (byte) 0;
        DataPackage[2] = (byte) 0;
        DataPackage[3] = (byte) 0;
        DataPackage[4] = (byte) 0;
    }
}
