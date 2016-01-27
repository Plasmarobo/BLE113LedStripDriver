package ledcmd.plasmarobo.com.ledcommand;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.app.Activity;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class FindDevice extends Activity {
    public final int REQUEST_ENABLE_BT = 32;
    BluetoothManager manager;
    BluetoothAdapter adapter;
    BluetoothLeScanner le;
    ListView deviceList;
    ScanCallback sc;
    HashMap<String, BluetoothDevice> devices;
    List<HashMap<String, String>> fillMaps;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_device);
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE Not Supported",
                    Toast.LENGTH_SHORT).show();
            finish();
        }
        manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        adapter = manager.getAdapter();
        fillMaps = new ArrayList<HashMap<String, String>>();
        deviceList = (ListView)findViewById(R.id.deviceListView);
        devices = new HashMap<>();
        deviceList.setClickable(true);
        deviceList.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String name = (String)parent.getItemAtPosition(position);
                BluetoothDevice btDevice = devices.get(name);
                Intent i = new Intent(getApplicationContext(), LEDControl.class);
                i.putExtra("BluetoothDevice", btDevice);
                le.stopScan(sc);
                startActivity(i);
            }
        });
        tryEnableBt();
    }

    @Override
    protected void onResume() {
        super.onResume();
        tryEnableBt();
    }

    protected void tryEnableBt() {

        if (adapter == null) {
            Log.e("Adapter Acq Failed", "unknown error");
            finish();
            System.exit(1);

        } else {
            if (!adapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            } else {
                this.findDevice();
            }
        }
    }

    protected void updateResults(ScanResult r)
    {
        Log.i("ScanResult - Results", r.toString());
        devices.put(r.getDevice().getName(), r.getDevice());
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("devicename", r.getDevice().getName());
        map.put("rssi", Integer.toString(r.getRssi()));
        fillMaps.add(map);
    }

    protected void findDevice()
    {
        List<ScanFilter> filters = new ArrayList<>();
        filters.add(new ScanFilter.Builder()
                .setDeviceName("LED Strip")
                .setServiceUuid(android.os.ParcelUuid.fromString("47f1de41-c535-414f-a747-1184246636c6"))
                .build());
        ScanSettings ss = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        le = adapter.getBluetoothLeScanner();
        final Context c = this;
        sc = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                updateResults(result);
                String[] from = new String[] {"devicename", "rssi"};
                int[] to = new int[] {R.id.devicename, R.id.rssi};
                SimpleAdapter adapter = new SimpleAdapter(c, fillMaps, R.layout.device_row, from, to);
                deviceList.setAdapter(adapter);
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                String[] from = new String[] {"devicename", "rssi"};
                int[] to = new int[] {R.id.devicename, R.id.rssi};

                for (ScanResult r : results) {

                    updateResults(r);
                }

                SimpleAdapter adapter = new SimpleAdapter(c, fillMaps, R.layout.device_row, from, to);
                deviceList.setAdapter(adapter);
            }

            @Override
            public void onScanFailed(int errorCode) {
                Log.e("Scan Failed", "Error Code: " + errorCode);
            }
        };

        le.startScan(filters, ss, sc);

    }

    @Override
    protected void onActivityResult(int req, int res, Intent data)
    {
        if(req == REQUEST_ENABLE_BT)
        {
            if(res == Activity.RESULT_CANCELED)
            {
                Log.e("Adapter Acq Failed", "Permission denied");
                finish();
                System.exit(1);
            }
            tryEnableBt();
        }
    }



}
