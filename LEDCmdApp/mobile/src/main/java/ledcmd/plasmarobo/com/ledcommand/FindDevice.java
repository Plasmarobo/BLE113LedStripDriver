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
import android.os.Bundle;
import android.app.Activity;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class FindDevice extends Activity {
    public final int REQUEST_ENABLE_BT = 32;
    BluetoothLeScanner le;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_device);
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE Not Supported",
                    Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    protected void tryEnableBt()
    {
        BluetoothManager manager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = manager.getAdapter();
        if (adapter == null)
        {
            Log.e("Adapter Acq Failed", "unknown error");
            finish();
            System.exit(1);

        }
        else
        {
            if (!adapter.isEnabled())
            {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            }
            else
            {
                this.findDevice(adapter);
            }
        }
    }

    protected void findDevice(BluetoothAdapter adapter)
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

        le.startScan(filters, ss, new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                Log.i("callbackType", String.valueOf(callbackType));
                Log.i("result", result.toString());
                BluetoothDevice btDevice = result.getDevice();
                Intent i = new Intent(getApplicationContext(), LEDControl.class);
                i.putExtra("BluetoothDevice", btDevice);
                le.stopScan(this);
                startActivity(i);
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                for (ScanResult sr : results) {
                    Log.i("ScanResult - Results", sr.toString());
                }
                onScanResult(0, results.get(0));
            }

            @Override
            public void onScanFailed(int errorCode) {
                Log.e("Scan Failed", "Error Code: " + errorCode);
            }
        });

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
