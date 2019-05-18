package com.example.macsbtcarcontroller;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    public String btCarAddress = "98:D3:71:FD:58:76";
    public UUID UUIDport = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public BluetoothDevice btCarDevice;
    public OutputStream outStreamToCar;
    public BluetoothSocket btSocket = null;
    public BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    public final int MY_PERMISSION_REQUEST_CONSTANT = 42;
    public boolean firstTransmission = true;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        int REQUEST_ENABLE_BT = 42;

        // Extrag referinte catre View-urile de input din interfata
        Button forwardButton, backwardButton, leftButton, rightButton;
        ImageButton honkImageButton;
        Switch lightsSwitch;

        forwardButton = (Button) findViewById(R.id.buttonForward);
        backwardButton = (Button) findViewById(R.id.buttonBackward);
        leftButton = (Button) findViewById(R.id.buttonLeft);
        rightButton = (Button) findViewById(R.id.buttonRight);
        lightsSwitch = (Switch) findViewById(R.id.switchLights);
        honkImageButton = (ImageButton) findViewById(R.id.imageButtonHonk);

        // Configurez handlerele pentru evenimentele asociate inputurilor
        forwardButton.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                byte[] forwardCommand = new byte[2];
                forwardCommand[0] = (byte)'f';

                byte speedPercentage = 0;
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    speedPercentage = 100;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    speedPercentage = 0;
                } else {
                    Log.d("MyDEBUG", "forwardButton.OnTouchListener() inutil");
                    return false;
                }
                Log.d("MyDEBUG", "forwardButton.OnTouchListener()");

                forwardCommand[1] = speedPercentage;
                forwardCommand[0] = (byte)'f';

                sendBytesToCar(forwardCommand);

                return false;
            }
        });

        backwardButton.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                byte[] backwardCommand = new byte[2];
                backwardCommand[0] = (byte)'f';

                byte speedPercentage = 0;
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    speedPercentage = 100;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    speedPercentage = 0;
                } else {
                    Log.d("MyDEBUG", "backwardButton.OnTouchListener() inutil");
                    return false;
                }
                Log.d("MyDEBUG", "backwardButton.OnTouchListener()");

                backwardCommand[1] = speedPercentage;
                backwardCommand[0] = (byte)'b';

                sendBytesToCar(backwardCommand);

                return false;
            }
        });

        leftButton.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                byte[] leftCommand = new byte[2];

                leftCommand[0] = (byte)'s';

                byte angle = 0;
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    angle = 52;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    angle = 90;
                } else {
                    Log.d("MyDEBUG", "leftButton.OnTouchListener() inutil");
                    return false;
                }
                Log.d("MyDEBUG", "leftButton.OnTouchListener()");

                leftCommand[1] = angle;
                leftCommand[0] = (byte)'s';

                sendBytesToCar(leftCommand);

                return false;
            }
        });

        rightButton.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                byte[] rightCommand = new byte[2];

                rightCommand[0] = (byte)'s';

                byte angle = 0;
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    angle = 115;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    angle = 90;
                } else {
                    Log.d("MyDEBUG", "rightButton.OnTouchListener() inutil");
                    return false;
                }
                Log.d("MyDEBUG", "rightButton.OnTouchListener()");

                rightCommand[1] = angle;
                rightCommand[0] = (byte)'s';

                sendBytesToCar(rightCommand);

                return false;
            }
        });

        lightsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                byte[] lightsOnCommand = new byte[2];

                lightsOnCommand[1] = (byte)42;
                if (isChecked) {
                    // The toggle is enabled
                    Log.d("MyDEBUG", "lightsSwitch.setOnCheckedChange de Activare");
                    lightsOnCommand[0] = 'l';
                } else {
                    // The toggle is disabled
                    Log.d("MyDEBUG", "lightsSwitch.setOnCheckedChange de Dezactivare");
                    lightsOnCommand[0] = 'd';
                }

                sendBytesToCar(lightsOnCommand);
            }
        });

        honkImageButton.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                byte[] honkCommand = new byte[2];
                honkCommand[0] = (byte)'h';

                byte honkState = 0;
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    honkState = 1;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    honkState = 0;
                } else {
                    Log.d("MyDEBUG", "forwardButton.OnTouchListener() inutil");
                    return false;
                }
                Log.d("MyDEBUG", "forwardButton.OnTouchListener()");

                honkCommand[1] = honkState;
                honkCommand[0] = (byte)'h';

                sendBytesToCar(honkCommand);

                return false;
            }
        });

        //Toast.makeText(getApplicationContext(), "Incepem", ////Toast.LENGTH_SHORT).show();

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},MY_PERMISSION_REQUEST_CONSTANT);

        if (bluetoothAdapter == null) {
            // Device-ul nu are bluetooth
            ////Toast.makeText(getApplicationContext(), "Device-ul nu are bluetooth", //Toast.LENGTH_SHORT).show();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            // Device-ul nu are bluetooth-ul pornit
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        // bluetoothAdapter.cancelDiscovery();


        // Register for broadcasts when a device is discovered. -- Receiverul e definit mai jos
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        // filter.addAction(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);

        bluetoothAdapter.startDiscovery();
        // //Toast.makeText(getApplicationContext(), "Am dat drumul la discovery", //Toast.LENGTH_SHORT).show();
        Log.d("MyDEBUG", "Am dat drumul la discovery");
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        switch (requestCode) {
            case MY_PERMISSION_REQUEST_CONSTANT: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    //permission granted!
                }
                return;
            }
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver receiver = new BroadcastReceiver() { // Instanta de clasa anonima
        public void onReceive(Context context, Intent intent) { // Handler de receive pentru eveniment de descoperire a unui nou dispozitiv bluetooth
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address

                //Toast.makeText(getApplicationContext(), "Am descoperit un device", //Toast.LENGTH_SHORT).show();
                Log.d("MyDEBUG", "Am descoperit un device");

                if (deviceHardwareAddress.equals(btCarAddress)) {
                    // Am gasit masinuta bluetooth. Ma conectez ca client bluetooth la ea
                    btCarDevice = device;
                    //Toast.makeText(getApplicationContext(), "Am descoperit masinuta", //Toast.LENGTH_SHORT).show();
                    Log.d("MyDEBUG", "Am descoperit chiar masinuta");

                    /*
                     * Incerc sa obtin o conexiune cu modulul Bluetooth de pe masinuta printr-un
                     * socket bluetooth. Acest socket imi ofera acces la un output stream unde
                     * voi scrie octetii pe care ii trimit masinutei.
                     */
                    try {
                        Log.d("MyDEBUG", "Incerc sa ma conectez prin Bluetooth la masinuta");
                        btSocket = btCarDevice.createRfcommSocketToServiceRecord(UUIDport);
                        btSocket.connect();
                        outStreamToCar = btSocket.getOutputStream();
                    } catch (IOException e) {
                        // Am gasit masinuta, dar nu ma pot conecta la ea
                        Log.d("MyDEBUG", "Conectarea la masinuta a esuat");
                        //Toast.makeText(getApplicationContext(), "Conectarea la masinuta a esuat", //Toast.LENGTH_SHORT).show();
                        bluetoothAdapter.cancelDiscovery();
                    }

                    Log.d("MyDEBUG", "Conectarea la masinuta a reusit");
                    //Toast.makeText(getApplicationContext(), "Conectarea la masinuta a reusit", //Toast.LENGTH_SHORT).show();
                    bluetoothAdapter.cancelDiscovery();
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                // //Toast.makeText(getApplicationContext(), "Descoperirea a inceput", //Toast.LENGTH_SHORT).show();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                // //Toast.makeText(getApplicationContext(), "Descoperirea s-a terminat", //Toast.LENGTH_SHORT).show();
            }
        }
    };

    private void sendBytesToCar(byte[] command) {
        if (outStreamToCar == null) {
            ////Toast.makeText(getApplicationContext(), "Conexiunea cu masinuta inexistenta", //Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            outStreamToCar.write(command);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendStringToCar(String command) {
        if (outStreamToCar == null) {
            ////Toast.makeText(getApplicationContext(), "Conexiunea cu masinuta inexistenta", //Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            outStreamToCar.write(command.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Don't forget to unregister the ACTION_FOUND receiver.
        if (BluetoothAdapter.getDefaultAdapter() != null)
            unregisterReceiver(receiver);
    }
}
