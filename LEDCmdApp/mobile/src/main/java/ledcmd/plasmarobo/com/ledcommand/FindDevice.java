package ledcmd.plasmarobo.com.ledcommand;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.app.Activity;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class FindDevice extends Activity {
    public final int REQUEST_ENABLE_BT = 32;
    BluetoothManager manager;
    BluetoothAdapter adapter;
    BluetoothLeScanner le;
    ListView deviceList;
    ScanCallback sc;
    HashMap<String, BluetoothDevice> devices;
    List<HashMap<String, String>> fillMaps;
    SimpleAdapter listadapter;

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
        le = adapter.getBluetoothLeScanner();
        sc = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {

                updateResults(result);
                listadapter.notifyDataSetChanged();
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {

                for (ScanResult r : results) {

                    updateResults(r);
                }

                listadapter.notifyDataSetChanged();
            }

            @Override
            public void onScanFailed(int errorCode) {
                Log.e("Scan Failed", "Error Code: " + errorCode);
            }
        };
        fillMaps = new ArrayList<HashMap<String, String>>();
        deviceList = (ListView)findViewById(R.id.deviceListView);
        devices = new HashMap<>();
        deviceList.setClickable(true);
        deviceList.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String mac = ((HashMap<String, String>)parent.getItemAtPosition(position)).get("mac");
                BluetoothDevice btDevice = devices.get(mac);
                Intent i = new Intent(getApplicationContext(), LEDControl.class);
                i.putExtra("BluetoothDevice", btDevice);
                startActivity(i);
                le.stopScan(sc);
            }
        });
        tryEnableBt();
    }

    @Override
    protected void onResume() {
        super.onResume();
        tryEnableBt();
    }

    protected void onPause(){
        super.onPause();
        this.le.stopScan(this.sc);
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
        devices.put(r.getDevice().getAddress(), r.getDevice());
        HashMap<String, String> map;
        for(HashMap<String, String> row : fillMaps) {
            String addr1 = row.get("mac");
            String addr2 = r.getDevice().getAddress();
            if (addr1.equals(addr2)) {
                row.put("devicename", r.getDevice().getName());
                row.put("rssi", Integer.toString(r.getRssi()));
                row.put("mac", r.getDevice().getAddress());
                return;
            }

        }
        //String name = r.getDevice().getName();
        //Log.v("LED", "Found device: <" + name + ">");
        //if(name != null && name.equals("LED Strip"))
        //{
            map = new HashMap<String, String>();
            map.put("devicename", r.getDevice().getName());
            map.put("rssi", Integer.toString(r.getRssi()));
            map.put("mac", r.getDevice().getAddress());
            fillMaps.add(map);
        //}

    }

    protected void findDevice()
    {
        List<ScanFilter> filters = new ArrayList<>();
        filters.add(new ScanFilter.Builder()
                //.setServiceUuid(ParcelUuid.fromString("8f192a8d-6cd2-4611-9f8f-b4e8bcb5e650"))
                .build());
        ScanSettings ss = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        String[] from = new String[] {"devicename", "rssi", "mac"};
        int[] to = new int[] {R.id.devicename, R.id.rssi, R.id.mac};
        listadapter = new SimpleAdapter(this, fillMaps, R.layout.device_row, from, to);
        deviceList.setAdapter(listadapter);

        this.le.startScan(filters, ss, this.sc);

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
