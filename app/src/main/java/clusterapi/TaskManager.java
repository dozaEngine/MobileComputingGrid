package clusterapi;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by dozaEngine on 11/20/2014.
 */
public class TaskManager {

    String TAG = "TaskManager";

    /* Application Handler */
    Handler handle;
    /* Network Interface */
    private OutputStream oStream;

    /* Utility Class */
    private Serializer serializer = new Serializer();

    /* FIFO Queue */
    private LinkedBlockingDeque<Object> sendQ;
    private LinkedBlockingDeque<Object> completeQ;
    private LinkedBlockingDeque<Object> heartbeatQ;

    public TaskManager(Handler handle, OutputStream stream) {
        sendQ = new LinkedBlockingDeque<Object>();
        completeQ = new LinkedBlockingDeque<Object>();
        heartbeatQ = new LinkedBlockingDeque<Object>();
        this.handle = handle;
        this.oStream = stream;

        new Thread(new ObjectReceiver()).start();
        new Thread(new ObjectScheduler()).start();
        new Thread(new HeartbeatReceiver()).start();
    }

    void scheduleObj(Object obj) {
        Log.d(TAG, "scheduleObj Entry >>>");
        try {
            sendQ.put(obj);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "scheduleObj Exit <<<");
    }

    void completedObj(Object obj){
        Log.d(TAG, "completedObj Entry >>>");

        try {
            completeQ.put(obj);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "completedObj Exit <<<");
    }

    void heartbeatObj(Object obj){
        Log.d(TAG, "completedObj Entry >>>");

        try {
            heartbeatQ.put(obj);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "completedObj Exit <<<");
    }

    private class ObjectScheduler implements Runnable{
        Object object;
        byte[] buffer;

        @Override
        public void run() {
            while(true) {
                try {
                    Log.e(TAG, "ObjectScheduler Take ...");
                    object = sendQ.take();
                    Log.e(TAG, "ObjectScheduler Take Complete...");
                    buffer = serializer.serialize(object);
                    oStream.write(buffer);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }



    private class ObjectReceiver implements Runnable{
        Object object;
        @Override
        public void run() {
            while(true) {
                try {
                    Log.e(TAG, "ObjectReceiver Take Start...");
                    object = completeQ.take();
                    Log.e(TAG, "ObjectReceiver Take Complete...");
                    handle.obtainMessage(WiFiServiceDiscoveryActivity.MESSAGE_READ,
                            object).sendToTarget();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class HeartbeatReceiver implements Runnable{
        Object object;
        @Override
        public void run() {
            while(true) {
                try {
                    Log.e(TAG, "HeartbeatReceiver Take Start...");
                    object = heartbeatQ.take();
                    Log.e(TAG, "HeartbeatReceiver Take Complete...");
                    handle.obtainMessage(WiFiServiceDiscoveryActivity.HEARTBEAT,
                            object).sendToTarget();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


}
