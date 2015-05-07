package clusterapi;

import android.app.Fragment;
import android.os.Handler;
import android.util.Log;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by dozaEngine on 4/21/2015.
 */
public class NodeThreadManager extends Fragment {

    private static int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
    // Sets the amount of time an idle thread waits before terminating
    private static final int KEEP_ALIVE_TIME = 1;
    // Sets the Time Unit to seconds
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;

    public final int HEARTBEAT_TO = 1500; // 1.5 seconds time-out (TO)

    public Handler handle = null;

    // A queue of Runnables
    private final BlockingQueue<Runnable> mDecodeWorkQueue;
    private final ThreadPoolExecutor mDecodeThreadPool;

    public static ClusterConnect clusterConnect;

    public NodeThreadManager() {
        this.mDecodeWorkQueue = new LinkedBlockingQueue<Runnable>();
        mDecodeThreadPool = new ThreadPoolExecutor(
                NUMBER_OF_CORES,       // Initial pool size
                NUMBER_OF_CORES,       // Max pool size
                KEEP_ALIVE_TIME,
                KEEP_ALIVE_TIME_UNIT,
                mDecodeWorkQueue);
    }

    public void loadWork(Runnable runnable) {
        Log.d("NodeThreadManager","Loading runnable");
        mDecodeThreadPool.execute(runnable);
    }

    public void setHandler(Handler _handle) {
        Log.d("NodeThreadManager","Set handler");
        handle = _handle;
    }

    public void sendComplete(Object object)
    {
        Log.e("NodeThreadManager","Sending complete message to handler");

        if(handle != null)
            handle.obtainMessage(WiFiServiceDiscoveryActivity.CLIENT_TASK_COMPLETE,
                object).sendToTarget();
    }

    public void sendClusterComplete(Object object){
        Log.e("NodeThreadManager"," >>> Sending complete message to cluster <<<");
        clusterConnect.write(object);
    }

    public void startHeartbeat()
    {
        Log.e("NodeThreadManager"," >>> Starting Heartbeat <<<");
        Thread heartbeat = new Thread(new Heartbeat(clusterConnect));
        heartbeat.setPriority(Thread.MAX_PRIORITY);
        heartbeat.start();
    }

    // TODO - Make into High Priority Thread
    private class Heartbeat implements Runnable {

        private ClusterConnect m_clusterConnect;

        public Heartbeat(ClusterConnect clusterConnectIn){
            m_clusterConnect = clusterConnectIn;
        }

        @Override
        public void run() {
            // TODO - make dynamic setting - loop count
            for (int i = 0; i < 100000; i++) {

                if(m_clusterConnect != null)
                    m_clusterConnect.write(m_clusterConnect.nodeProperties);

                // Escape early if cancel() is called
                if (Thread.interrupted()) break;

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }

}
