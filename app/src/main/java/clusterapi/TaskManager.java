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

    static final int READY = 0;
    static final int BUSY = 0;
    static final int COMPLETE = 0;

    /* Application Handler */
    Handler handle;
    /* Network Interface */
    private OutputStream oStream;

    /* Utility Class */
    private Serializer serializer = new Serializer();

    /* FIFO Queue */
    private LinkedBlockingDeque<Object> workQ;
    private LinkedBlockingDeque<Object> completeQ;

    public TaskManager(Handler handle, OutputStream stream) {
        workQ = new LinkedBlockingDeque<Object>();
        completeQ = new LinkedBlockingDeque<Object>();
        this.handle = handle;
        this.oStream = stream;

        new Thread(new ObjectReceiver()).start();
        new Thread(new ObjectScheduler()).start();
    }

    void scheduleObj(Object obj) {
        Log.d(TAG, "scheduleObj Entry >>>");
        try {
            workQ.put(obj);
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

    void setTaskReady(){}
    void setTaskBusy(){}
    void setTaskComplete(){}

    private class ObjectScheduler implements Runnable{
        Object object;
        byte[] buffer;

        @Override
        public void run() {
            while(true) {
                try {
                    Log.e(TAG, "ObjectScheduler Take ...");
                    object = workQ.take();
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

}
