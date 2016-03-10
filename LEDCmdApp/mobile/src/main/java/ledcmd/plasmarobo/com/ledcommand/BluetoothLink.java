package ledcmd.plasmarobo.com.ledcommand;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by austen on 2/27/16.
 * Forked from https://github.com/adafruit/Adafruit_Android_BLE_UART
 */

public class BluetoothLink extends BluetoothGattCallback implements BluetoothAdapter.LeScanCallback {

    // Debug
    public final String TAG = "LED Control";

    // UUIDs for UART service and associated characteristics.
    public static UUID UART_UUID = UUID.fromString("8f192a8d-6cd2-4611-9f8f-b4e8bcb5e650");
    public static UUID TX_UUID   = UUID.fromString("e7add780-b042-4876-aae1-112855353cc1");

    // UUIDs for the Device Information service and associated characeristics.
    public static UUID DIS_UUID       = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
    public static UUID DIS_MANUF_UUID = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb");
    public static UUID DIS_MODEL_UUID = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb");

    // UUIDs for LED Control
    public static UUID LED_UUID         = UUID.fromString("47f1de41-c535-414f-a747-1184246636c6");
    public static UUID LED_DATA_UUID    = UUID.fromString("f408b6c7-06c0-4b4a-8493-50bc261ea9e7");
    public static UUID LED_COMMAND_UUID = UUID.fromString("f408b6c7-06c0-4b4a-8493-50bc261ea9e8");

    // Internal UART state.
    private Context context;
    private WeakHashMap<Callback, Object> callbacks;
    private BluetoothAdapter adapter;
    private BluetoothGatt gatt;

    // UART
    private BluetoothGattCharacteristic uart;
    //private BluetoothGattCharacteristic rx;
    private boolean connectFirst;
    private boolean writeInProgress; // Flag to indicate a write is currently in progress

    // Device Information state.
    private BluetoothGattCharacteristic disManuf;
    private BluetoothGattCharacteristic disModel;
    private boolean disAvailable;

    // LED Interface
    private BluetoothGattCharacteristic ledData;
    private BluetoothGattCharacteristic ledCommand;

    // Queues for characteristic read (synchronous)
    private Queue<BluetoothGattCharacteristic> readQueue;
    private Queue<Pair<BluetoothGattCharacteristic,byte[]>> writeQueue;
    private short ledCount;

    // Interface for a BluetoothLink client to be notified of UART actions.
    public interface Callback {
        public void onConnected(BluetoothLink link);
        public void onConnectFailed(BluetoothLink uart);
        public void onDisconnected(BluetoothLink uart);
        public void onReceive(BluetoothLink uart, BluetoothGattCharacteristic rx);
        public void onDeviceFound(BluetoothDevice device);
        public void onDeviceInfoAvailable();
    }

    // Timeouts
    private Handler timeoutHandler;
    private final int WRITE_TIMEOUT = 10000;
    private final int DEVICE_PAYLOAD_LIMIT = 9; // For debug firmware

    public BluetoothLink(Context context) {
        super();
        this.context = context;
        this.callbacks = new WeakHashMap<Callback, Object>();
        this.adapter = BluetoothAdapter.getDefaultAdapter();
        this.gatt = null;
        this.uart = null;
        this.disManuf = null;
        this.disModel = null;
        this.ledData = null;
        this.ledCommand = null;
        this.disAvailable = false;
        this.connectFirst = false;
        this.writeInProgress = false;
        this.readQueue = new ConcurrentLinkedQueue<BluetoothGattCharacteristic>();
        this.writeQueue = new ConcurrentLinkedQueue<Pair<BluetoothGattCharacteristic, byte[]>>();
        this.timeoutHandler = new Handler();
        this.ledCount = 160;
    }

    // Return instance of BluetoothGatt.
    public BluetoothGatt getGatt() {
        return gatt;
    }

    // Construct Link from Device
    public void setDevice(BluetoothDevice device)
    {
        gatt = device.connectGatt(context, true, this);
    }

    // Return true if connected to UART device, false otherwise.
    public boolean isConnected() {
        return (uart != null || ledData != null);
    }

    public String getDeviceInfo() {
        if (uart == null || ledData == null || !disAvailable ) {
            // Do nothing if there is no connection.
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Manufacturer : " + disManuf.getStringValue(0) + "\n");
        sb.append("Model        : " + disModel.getStringValue(0) + "\n");
        //sb.append("Firmware     : " + disSWRev.getStringValue(0) + "\n");
        return sb.toString();
    };

    public boolean deviceInfoAvailable() { return disAvailable; }

    public void write(byte[] data, BluetoothGattCharacteristic characteristic)
    {
        if (characteristic == null || data == null || data.length == 0) {
            // Do nothing if there is no connection or message to send.
            return;
        }
        int index = 0;
        int offset = 0;
        while (offset < data.length) {
            int length = Math.min(data.length - offset, DEVICE_PAYLOAD_LIMIT);
            byte[] buffer = new byte[length];
            for (int i = 0; i < length; ++i) {
                buffer[i] = data[offset + i];
            }
            Log.d(TAG, "Enqueued " + buffer.length + " bytes");
            writeQueue.add(new Pair(characteristic, buffer));
            offset += length;
        }

        if (writeInProgress == false) {
            Pair<BluetoothGattCharacteristic, byte[]> p = writeQueue.remove();
            send(p.second, p.first);
        }

    }

    // Send data to connected UART device.
    public void send(byte[] data, BluetoothGattCharacteristic characteristic) {
        if (characteristic == null || data == null || data.length == 0) {
            // Do nothing if there is no connection or message to send.
            return;
        }
        Log.d(TAG, "Sent " + data.length + " bytes");
        characteristic.setValue(data);
        writeInProgress = true; // Set the write in progress flag
        gatt.writeCharacteristic(characteristic);
    }

    // Send data to connected device.
    public void send(String data, BluetoothGattCharacteristic characteristic) {
        if (data != null && !data.isEmpty()) {
            send(data.getBytes(Charset.forName("UTF-8")), characteristic);
        }
    }

    // Send to UART
    public void sendUART(String data)
    {
        if(data != null && !data.isEmpty())
        {
            send(data.getBytes(Charset.forName("UTF-8")), uart);
        }
    }

    // Send Color Array
    public void writeColor(byte[] data)
    {
        if((data.length % 3) == 0)
        {
            write(data, ledData);
        }
    }

    // Send Write Command
    public void writeUpdate()
    {

        write(new byte[]{0x00}, ledCommand);
    }

    // Send Clear Command
    public void writeClear()
    {
        write(new byte[]{0x02}, ledCommand);
    }

    // Set LED Count
    public void writeLEDCount(short ledCount)
    {
        this.ledCount = ledCount;
        ByteBuffer b = ByteBuffer.allocate(3);
        b.put((byte)0x01);
        b.putShort(ledCount);
        write(b.array(), ledCommand);
    }

    public short getLedCount()
    {
        return ledCount;
    }

    // Register the specified callback to receive UART callbacks.
    public void registerCallback(Callback callback) {
        callbacks.put(callback, null);
    }

    // Unregister the specified callback.
    public void unregisterCallback(Callback callback) {
        callbacks.remove(callback);
    }

    // Disconnect to a device if currently connected.
    public void disconnect() {
        if (gatt != null) {
            gatt.disconnect();
        }
        gatt = null;
        uart = null;
        //rx = null;
    }

    // Connect to the first available UART device.
    public void connectFirstAvailable() {
        // Disconnect to any connected device.
        disconnect();
        // Stop any in progress device scan.
        //stopScan();
        // Start scan and connect to first available device.
        connectFirst = true;
        //startScan();
    }

    // Handlers for BluetoothGatt and LeScan events.
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        if (newState == BluetoothGatt.STATE_CONNECTED) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Connected to device, start discovering services.
                if (!gatt.discoverServices()) {
                    // Error starting service discovery.
                    connectFailure();
                }
            }
            else {
                // Error connecting to device.
                connectFailure();
            }
        }
        else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
            // Disconnected, notify callbacks of disconnection.
            //rx = null;
            uart = null;
            notifyOnDisconnected(this);
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        // Notify connection failure if service discovery failed.
        if (status == BluetoothGatt.GATT_FAILURE) {
            connectFailure();
            return;
        }

        // Save reference to each UART characteristic.
        uart = gatt.getService(UART_UUID).getCharacteristic(TX_UUID);

        // Save reference to each DIS characteristic.
        disManuf = gatt.getService(DIS_UUID).getCharacteristic(DIS_MANUF_UUID);
        disModel = gatt.getService(DIS_UUID).getCharacteristic(DIS_MODEL_UUID);

        // Get LED references
        ledData = gatt.getService(LED_UUID).getCharacteristic(LED_DATA_UUID);
        ledCommand = gatt.getService(LED_UUID).getCharacteristic(LED_COMMAND_UUID);

        // Add device information characteristics to the read queue
        // These need to be queued because we have to wait for the response to the first
        // read request before a second one can be processed (which makes you wonder why they
        // implemented this with async logic to begin with???)
        readQueue.offer(disManuf);
        readQueue.offer(disModel);

        // Request a dummy read to get the device information queue going
        gatt.readCharacteristic(disManuf);

        // Setup notifications on RX characteristic changes (i.e. data received).
        // First call setCharacteristicNotification to enable notification.

        // Notify of connection completion.
        notifyOnConnected(this);
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);
        notifyOnReceive(this, characteristic);
    }

    @Override
    public void onCharacteristicRead (BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);

        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.w(TAG, characteristic.getStringValue(0));
            // Check if there is anything left in the queue
            BluetoothGattCharacteristic nextRequest = readQueue.poll();
            if(nextRequest != null){
                // Send a read request for the next item in the queue
                gatt.readCharacteristic(nextRequest);
            }
            else {
                // We've reached the end of the queue
                disAvailable = true;
                notifyOnDeviceInfoAvailable();
            }
        }
        else {
            Log.w(TAG, "Failed reading characteristic " + characteristic.getUuid().toString());
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);

        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.d(TAG, "Characteristic write successful");
        }
        if(writeQueue.size() > 0) {
           //Align to thee bytes
            Log.d(TAG, "Writing next queued element");
            Pair<BluetoothGattCharacteristic, byte[]> p = writeQueue.remove();
            send(p.second, p.first);
        }
        else
        {
            Log.d(TAG, "Write Queue Exhausted");
            writeInProgress = false;
        }
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        // Stop if the device doesn't have the UART service.
        if (!parseUUIDs(scanRecord).contains(UART_UUID)) {
            return;
        }
        // Notify registered callbacks of found device.
        notifyOnDeviceFound(device);
        // Connect to first found device if required.
        if (connectFirst) {
            // Stop scanning for devices.
            //stopScan();
            // Prevent connections to future found devices.
            connectFirst = false;
            // Connect to device.
            gatt = device.connectGatt(context, true, this);
        }
    }

    // Private functions to simplify the notification of all callbacks of a certain event.
    private void notifyOnConnected(BluetoothLink uart) {
        for (Callback cb : callbacks.keySet()) {
            if (cb != null) {
                cb.onConnected(uart);
            }
        }
    }

    private void notifyOnConnectFailed(BluetoothLink uart) {
        for (Callback cb : callbacks.keySet()) {
            if (cb != null) {
                cb.onConnectFailed(uart);
            }
        }
    }

    private void notifyOnDisconnected(BluetoothLink uart) {
        for (Callback cb : callbacks.keySet()) {
            if (cb != null) {
                cb.onDisconnected(uart);
            }
        }
    }

    private void notifyOnReceive(BluetoothLink uart, BluetoothGattCharacteristic rx) {
        for (Callback cb : callbacks.keySet()) {
            if (cb != null ) {
                cb.onReceive(uart, rx);
            }
        }
    }

    private void notifyOnDeviceFound(BluetoothDevice device) {
        for (Callback cb : callbacks.keySet()) {
            if (cb != null) {
                cb.onDeviceFound(device);
            }
        }
    }

    private void notifyOnDeviceInfoAvailable() {
        for (Callback cb : callbacks.keySet()) {
            if (cb != null) {
                cb.onDeviceInfoAvailable();
            }
        }
    }

    // Notify callbacks of connection failure, and reset connection state.
    private void connectFailure() {
        //rx = null;
        uart = null;
        notifyOnConnectFailed(this);
    }

    // Filtering by custom UUID is broken in Android 4.3 and 4.4, see:
    //   http://stackoverflow.com/questions/18019161/startlescan-with-128-bit-uuids-doesnt-work-on-native-android-ble-implementation?noredirect=1#comment27879874_18019161
    // This is a workaround function from the SO thread to manually parse advertisement data.
    private List<UUID> parseUUIDs(final byte[] advertisedData) {
        List<UUID> uuids = new ArrayList<UUID>();

        int offset = 0;
        while (offset < (advertisedData.length - 2)) {
            int len = advertisedData[offset++];
            if (len == 0)
                break;

            int type = advertisedData[offset++];
            switch (type) {
                case 0x02: // Partial list of 16-bit UUIDs
                case 0x03: // Complete list of 16-bit UUIDs
                    while (len > 1) {
                        int uuid16 = advertisedData[offset++];
                        uuid16 += (advertisedData[offset++] << 8);
                        len -= 2;
                        uuids.add(UUID.fromString(String.format("%08x-0000-1000-8000-00805f9b34fb", uuid16)));
                    }
                    break;
                case 0x06:// Partial list of 128-bit UUIDs
                case 0x07:// Complete list of 128-bit UUIDs
                    // Loop through the advertised 128-bit UUID's.
                    while (len >= 16) {
                        try {
                            // Wrap the advertised bits and order them.
                            ByteBuffer buffer = ByteBuffer.wrap(advertisedData, offset++, 16).order(ByteOrder.LITTLE_ENDIAN);
                            long mostSignificantBit = buffer.getLong();
                            long leastSignificantBit = buffer.getLong();
                            uuids.add(new UUID(leastSignificantBit,
                                    mostSignificantBit));
                        } catch (IndexOutOfBoundsException e) {
                            // Defensive programming.
                            //Log.e(LOG_TAG, e.toString());
                            continue;
                        } finally {
                            // Move the offset to read the next uuid.
                            offset += 15;
                            len -= 16;
                        }
                    }
                    break;
                default:
                    offset += (len - 1);
                    break;
            }
        }
        return uuids;
    }
}
