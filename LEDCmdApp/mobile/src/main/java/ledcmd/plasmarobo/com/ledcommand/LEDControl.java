package ledcmd.plasmarobo.com.ledcommand;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import java.io.Console;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LEDControl extends Activity {
    private SeekBar red;
    private SeekBar green;
    private SeekBar blue;
    private SeekBar.OnSeekBarChangeListener onColorChanged;
    private ImageView preview;
    private Button add;
    private Button write;
    private Button clear;
    private Button set_led_count;
    private EditText led_count;
    private int red_value;
    private int green_value;
    private int blue_value;
    private List<Byte> color_buffer;
    private LinearLayout stripPreview;
    private BluetoothDevice bt;
    private BluetoothGatt gatt_service;
    private BluetoothGattCharacteristic rgb_gatt;
    private BluetoothGattCharacteristic cmd_gatt;
    private final UUID LED_STRIP_SERVICE = UUID.fromString("47f1de41-c535-414f-a747-1184246636c6");
    private final UUID LED_DATA_CHARACTERISTIC = UUID.fromString("f408b6c7-06c0-4b4a-8493-50bc261ea9e7");
    private final UUID LED_CMD_CHARACTERISTIC = UUID.fromString("f408b6c7-06c0-4b4a-8493-50bc261ea9e8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bt = null;

        if(getIntent().getExtras() != null) {
            bt = getIntent().getExtras().getParcelable("BluetoothDevice");

        }
        if(bt == null)
        {
            finish();
        }
        rgb_gatt = null;
        cmd_gatt = null;

        gatt_service = bt.connectGatt(this, false, new BluetoothGattCallback() {
                    @Override
                    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                        super.onConnectionStateChange(gatt, status, newState);
                        if(status == BluetoothGatt.GATT_SUCCESS)
                        {
                            switch (newState)
                            {
                                case BluetoothProfile.STATE_CONNECTED:
                                    Log.i("BLE", "Connected to GATT server.");
                                    Log.i("BLE", "Attempting to start service discovery:" +
                                            gatt_service.discoverServices());
                                    write.setEnabled(true);
                                    clear.setEnabled(true);
                                    set_led_count.setEnabled(true);
                                    break;
                                case BluetoothProfile.STATE_DISCONNECTED:
                                    break;
                            }
                        }
                    }

                    @Override
                    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                        super.onServicesDiscovered(gatt, status);
                        rgb_gatt = gatt.getService(LED_STRIP_SERVICE).getCharacteristic(LED_DATA_CHARACTERISTIC);
                        rgb_gatt.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                        cmd_gatt = gatt.getService(LED_STRIP_SERVICE).getCharacteristic(LED_CMD_CHARACTERISTIC);
                        cmd_gatt.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                    }

                    @Override
                    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                        super.onCharacteristicRead(gatt, characteristic, status);
                    }

                    @Override
                    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                        super.onCharacteristicWrite(gatt, characteristic, status);
                    }
        });
        //gatt_service.connect();
        color_buffer = new ArrayList<Byte>();
        setContentView(R.layout.activity_ledcontrol);
        red = (SeekBar)findViewById(R.id.red);
        green = (SeekBar)findViewById(R.id.green);
        blue = (SeekBar)findViewById(R.id.blue);
        red_value = 0; green_value = 0; blue_value = 0;
        preview = (ImageView)findViewById(R.id.preview);
        stripPreview = (LinearLayout)findViewById(R.id.stripPreview);
        add = (Button)findViewById(R.id.add);
        write = (Button)findViewById(R.id.write);
        clear = (Button)findViewById(R.id.clear);
        set_led_count = (Button)findViewById(R.id.set_count);
        led_count = (EditText) findViewById(R.id.led_count);
        this.onColorChanged = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                red_value = red.getProgress();
                green_value = green.getProgress();
                blue_value = blue.getProgress();
                preview.setBackgroundColor(Color.rgb(red_value, green_value, blue_value));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        };
        red.setOnSeekBarChangeListener(this.onColorChanged);
        green.setOnSeekBarChangeListener(this.onColorChanged);
        blue.setOnSeekBarChangeListener(this.onColorChanged);
        add.setOnClickListener(new Button.OnClickListener() {

            @Override
            public void onClick(View v) {
                color_buffer.add((byte)red_value);
                color_buffer.add((byte)green_value);
                color_buffer.add((byte)blue_value);
                ImageView cSquare = new ImageView(getBaseContext());
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(20,20);
                p.setMargins(2,0,2,0);
                cSquare.setLayoutParams(p);
                cSquare.setMinimumHeight(20);
                cSquare.setMinimumWidth(20);
                cSquare.setBackgroundColor(Color.rgb(red_value, green_value, blue_value));
                stripPreview.addView(cSquare);

            }
        });
        write.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Write massive string
                Log.d("LED data:", color_buffer.toString());
                //gatt_service.beginReliableWrite();
                Log.d("GATT", "RGB: " + rgb_gatt.setValue(color_buffer.toString()));
                Log.d("GATT", "gatt service write: " + gatt_service.writeCharacteristic(rgb_gatt));

                color_buffer.clear();
                stripPreview.removeAllViews();

                Log.d("GATT", "CMD: " + cmd_gatt.setValue(new byte[]{0x00}));
                Log.d("GATT", "gatt service write: " + gatt_service.writeCharacteristic(cmd_gatt));
                //gatt_service.executeReliableWrite();
            }
        });
        clear.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                //gatt_service.beginReliableWrite();
                cmd_gatt.setValue(new byte[]{2});
                gatt_service.writeCharacteristic(cmd_gatt);
                //gatt_service.executeReliableWrite();
            }
        });
        set_led_count.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                int leds = Integer.parseInt(led_count.getText().toString());
                //gatt_service.beginReliableWrite();
                cmd_gatt.setValue((short) leds, BluetoothGattCharacteristic.FORMAT_UINT16, 0);
                gatt_service.writeCharacteristic(cmd_gatt);
                //gatt_service.executeReliableWrite();
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_ledcontrol, menu);
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

    @Override
    protected void onDestroy() {
        if ( gatt_service == null) {
            return;
        }
        gatt_service.close();
        gatt_service = null;
        super.onDestroy();
    }

    protected void onPause() {
        super.onPause();
        gatt_service.disconnect();
        gatt_service.close();
    }

    protected void onResume(){
        super.onResume();
        gatt_service.connect();
    }
}
