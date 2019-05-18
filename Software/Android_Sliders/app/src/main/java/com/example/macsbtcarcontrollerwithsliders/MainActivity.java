package com.example.macsbtcarcontrollerwithsliders;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Switch;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    public String btCarAddress = "98:D3:71:FD:58:76";
    public UUID UUIDport = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public BluetoothDevice btCarDevice;
    public OutputStream outStreamToCar;
    public BluetoothSocket btSocket = null;
    public BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    public final int MY_PERMISSION_REQUEST_CONSTANT = 42;
    public final int STEERING_ANGLE_MIN = 60;
    public final int STEERING_ANGLE_MAX = 110;
    public final int STEERING_ANGLE_IDLE = 85;
    public boolean firstTransmission = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        int REQUEST_ENABLE_BT = 42;

        // Extrag referinte catre View-urile de input din interfata
        ImageButton honkImageButton;
        Switch lightsSwitch;
        SeekBar steeringSeekBar, speedSeekBar;

        lightsSwitch = (Switch) findViewById(R.id.switchLights);
        honkImageButton = (ImageButton) findViewById(R.id.imageButtonHonk);
        speedSeekBar = (SeekBar) findViewById(R.id.seekBarSpeed);

        // Din pacate modificarile prea dese dau probleme lui HC-05 / Microcontroller-ului
//        steeringSeekBar = (SeekBar) findViewById(R.id.seekBarSteering);
//
//        steeringSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//            byte[] steeringCommand = new byte[2];
//
//            @Override
//            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                if (!fromUser) return;
//
//                Log.d("MyDEBUG", "steeringSeekBar.setOnSeekBarChangeListener modificare progress de catre user");
//
//                steeringCommand[0] = 's';
//                byte angle;
//                angle = (byte)(STEERING_ANGLE_MIN + (float)progress / 100.0f * (STEERING_ANGLE_MAX - STEERING_ANGLE_MIN));
//                steeringCommand[1] = angle;
//                sendBytesToCar(steeringCommand);
//            }
//
//            @Override
//            public void onStartTrackingTouch(SeekBar seekBar) {
//
//            }
//
//            @Override
//            public void onStopTrackingTouch(SeekBar seekBar) {
//
//                Log.d("MyDEBUG", "steeringSeekBar.setOnSeekBarChangeListener oprire atingere de catre user");
//
//                seekBar.setProgress(50);
//                steeringCommand[0] = 's';
//                steeringCommand[1] = 85;
//                sendBytesToCar(steeringCommand);
//            }
//        });

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
                    Log.d("MyDEBUG", "honkImageButton.OnTouchListener() inutil");
                    return false;
                }
                Log.d("MyDEBUG", "honkImageButton.OnTouchListener()");

                honkCommand[1] = honkState;
                honkCommand[0] = (byte)'h';

                sendBytesToCar(honkCommand);

                return false;
            }
        });

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
