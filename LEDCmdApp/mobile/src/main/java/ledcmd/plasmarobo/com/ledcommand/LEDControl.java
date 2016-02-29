package ledcmd.plasmarobo.com.ledcommand;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;

import java.util.ArrayList;
import java.util.List;

import ledcmd.plasmarobo.com.ledcommand.Patterns.PatternManager;

public class LEDControl extends Activity implements BluetoothLink.Callback {
    private BluetoothDevice device;
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
    private PatternManager patternManager;

    private BluetoothLink link;
    private final String TAG = "LEDStrip";

    private final int RESULT_CONNECTION_FAILURE = 5;
    private final int RESULT_DISCONNECTED = 6;
    private final int RESULT_NULL_DEVICE = 7;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        link = new BluetoothLink(this);
        link.registerCallback(this);
        device = null;
        if(getIntent().getExtras() != null) {
            device = (BluetoothDevice) getIntent().getExtras().getParcelable("BluetoothDevice");

        }

        if(device == null)
        {
            Log.e(TAG, "No device");
            finishActivity(RESULT_NULL_DEVICE);
        }
        link.setDevice(device);
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

        setUIEnabled(false);

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
                color_buffer.add((byte) red_value);
                color_buffer.add((byte) green_value);
                color_buffer.add((byte) blue_value);
                ImageView cSquare = new ImageView(getBaseContext());
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(20, 20);
                p.setMargins(2, 0, 2, 0);
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
                Log.d(TAG, "Payload: " + color_buffer.toString());
                byte[] data = new byte[color_buffer.size()];
                for(int index = 0; index < color_buffer.size(); index++)
                {
                    data[index] = color_buffer.get(index);
                }
                link.writeColor(data);
                color_buffer.clear();
                stripPreview.removeAllViews();
                link.writeUpdate();
            }
        });

        clear.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                //gatt_service.beginReliableWrite();
                link.writeClear();
                //gatt_service.executeReliableWrite();
            }
        });
        set_led_count.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                link.writeLEDCount(Short.parseShort(led_count.getText().toString()));
            }
        });

        patternManager = new PatternManager(link);
        patternManager.populateListView(this, (ListView)findViewById(R.id.patternListView));

    }

    private void setUIEnabled(final boolean enable)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                add.setEnabled(enable);
                write.setEnabled(enable);
                clear.setEnabled(enable);
                set_led_count.setEnabled(enable);
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
        link.disconnect();
        super.onDestroy();
    }

    protected void onPause() {
        super.onPause();
        link.disconnect();
    }

    protected void onResume(){
        super.onResume();
        link.setDevice(device);
    }

    public void onConnected(BluetoothLink link) {
        Log.d(TAG, "Connected");
        setUIEnabled(true);
    }
    public void onConnectFailed(BluetoothLink uart) {
        Log.d(TAG, "Connection Failed");
        finishActivity(RESULT_CONNECTION_FAILURE);
    }
    public void onDisconnected(BluetoothLink uart) {
        Log.d(TAG, "Disconnected");
        finishActivity(RESULT_DISCONNECTED);
    }
    public void onReceive(BluetoothLink uart, BluetoothGattCharacteristic rx) {
        Log.d(TAG, "Recieved Data");
    }
    public void onDeviceFound(BluetoothDevice device) {
        Log.d(TAG, "Device Detected");
    }
    public void onDeviceInfoAvailable() {
        Log.d(TAG, "Device Info Available");
        setUIEnabled(true);
    }

}
