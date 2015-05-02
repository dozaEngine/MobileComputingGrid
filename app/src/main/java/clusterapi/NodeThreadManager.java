package clusterapi;

import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
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
        (new Heartbeat(clusterConnect)).execute(1000); // TODO - make dynamic setting
    }

    private class Heartbeat extends AsyncTask<Integer, Integer, Long> {

        private ClusterConnect m_clusterConnect;

        public Heartbeat(ClusterConnect clusterConnectIn){
            m_clusterConnect = clusterConnectIn;
        }

        protected Long doInBackground(Integer ... duration) {
            long totalSize = 0;
            for (int i = 0; i < duration[0]; i++) {

                if(m_clusterConnect != null)
                    publishProgress((int) ((i / duration[0]) * 100));

                // Escape early if cancel() is called
                if (isCancelled()) break;

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return totalSize;
        }

        protected void onProgressUpdate(Integer... progress) {
            m_clusterConnect.write(m_clusterConnect.nodeProperties);
        }

    }

}
