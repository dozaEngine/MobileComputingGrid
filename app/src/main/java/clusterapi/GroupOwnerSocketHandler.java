
package clusterapi;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * The implementation of a ServerSocket handler. This is used by the wifi p2p
 * group owner.
 */
public class GroupOwnerSocketHandler extends Thread {

    ServerSocket socket = null;
    private final int THREAD_COUNT = 10;
    private final int MAX_THREAD_COUNT = 15;
    private Handler handler;
    private static final String TAG = "GroupOwnerSocketHandler";
    LinkedList<Socket> clients = new LinkedList<Socket>();

    public GroupOwnerSocketHandler(Handler handler) throws IOException {
        try {
            if(socket == null) {
                socket = new ServerSocket(); /* TODO: TCP ports can only have one listener thread */
                socket.setReuseAddress(true);
                socket.bind(new InetSocketAddress(4545)); /* WIFI Direct Server Port Number */
            }
            this.handler = handler;
            Log.d("GroupOwnerSocketHandler", "Socket Started");
        } catch (IOException e) {
            e.printStackTrace();
            pool.shutdownNow();
            throw e;
        }
    }

    /**
     * A ThreadPool for client sockets.
     */
    private final ThreadPoolExecutor pool = new ThreadPoolExecutor(
            THREAD_COUNT, MAX_THREAD_COUNT, 10, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>());

    @Override
    public void run() {
        while (true) {
            try {

                // A blocking operation: socket.accept()
                clients.add(socket.accept());
                Log.d(TAG, "Client IP address: "+clients.getLast().getInetAddress());
                pool.execute(new ClusterConnect(clients.getLast(), handler, null));
                Log.d(TAG, "Launching the I/O handler");

            } catch (Exception e) {
                try {
                    if (socket != null && !socket.isClosed())
                        socket.close();
                } catch (IOException ioe) {
                    Log.d(TAG, "Unhandled exception.");
                }
                e.printStackTrace();
                pool.shutdownNow();
                break;
            }
        }
    }

}
