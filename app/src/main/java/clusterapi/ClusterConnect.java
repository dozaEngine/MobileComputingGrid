package clusterapi;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.Socket;

/**
 * Created by dozaEngine on 10/19/2014.
 */
public class ClusterConnect implements Runnable  {

    private Socket socket = null;
    private Handler handler;
    private InputStream iStream;
    private OutputStream oStream;
    private Serializer serializer = new Serializer();

    // Node Characteristics
    public NodeProperties nodeProperties;

    // Task Queue Handler
    TaskManager taskManager;

    private static final String TAG = "ClusterRunnable";

    public ClusterConnect(Socket socket, Handler handler, NodeProperties nodeProperties) {
        this.socket = socket;
        this.handler = handler;
        this.nodeProperties = nodeProperties;
    }

    @Override
    public void run() {
        try {
            iStream = socket.getInputStream();
            oStream = socket.getOutputStream();

            taskManager = new TaskManager(handler, oStream);

            //TODO: Investigate Buffer Size: Potential Memory Issues
            byte[] buffer = new byte[1024];
            int bytes;

            Log.d(TAG,"Sending Handle");
            if(nodeProperties != null) {
                Log.e(TAG, "Heartbeat Received: " + nodeProperties.hash);
                Log.d(TAG, "CPU Freq:" + nodeProperties.cpuFrequency);
                Log.d(TAG, "Cores:" + nodeProperties.processorsAvail);
            }

            handler.obtainMessage(WiFiServiceDiscoveryActivity.MY_HANDLE, this)
                    .sendToTarget();

            while (true) {
                try {
                    // Read from the InputStream
                    bytes = iStream.read(buffer);
                    if (bytes == -1) {
                        break;
                    }

                    Object object = serializer.deserialize(buffer);
                    Log.e(TAG, "Received Object!");

                    // Expected to be received as first message
                    if(object.getClass() == NodeProperties.class){
                        if(nodeProperties == null) nodeProperties = (NodeProperties)object;
                        Log.e(TAG, "Heartbeat Received: " + nodeProperties.hash);
                        Log.e(TAG, "CPU Freq:" + String.valueOf(nodeProperties.cpuFrequency));
                        Log.e(TAG, "Cores:" + String.valueOf(nodeProperties.processorsAvail));
                        taskManager.heartbeatObj(object);
                    }
                    else {
                        // Send the obtained bytes to the UI Activity
                        Log.d(TAG, "Rec["+buffer.length+"]: "+ String.valueOf(buffer.hashCode()));
                        taskManager.completedObj(object);
                    }

                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            Log.e(TAG, ">> Disconnected <<", e);
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /* Serialized Implementation */
    public void write(Object obj) {
//        byte[] buffer;
//        try {
            Log.e(TAG, "About to write to queue.");
            taskManager.scheduleObj(obj);
            Log.e(TAG, "Completed writing to queue.");
//            buffer = serializer.serialize(obj);
//            oStream.write(buffer);
//        } catch (IOException e) {
//            Log.e(TAG, "Exception during write", e);
//        }
    }

    /* Byte Stream Implementation */
    public void write(byte[] buffer) {
        try {
            oStream.write(buffer);
        } catch (IOException e) {
            Log.e(TAG, "Exception during write", e);
        }
    }

}
