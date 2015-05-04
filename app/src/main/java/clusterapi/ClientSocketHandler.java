
package clusterapi;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ClientSocketHandler extends Thread {

    private static final String TAG = "ClientSocketHandler";
    private Handler handler;
    private ClusterConnect clusterRunnable;
    private InetAddress mAddress;
    private NodeProperties nodeProperties;

    public ClientSocketHandler(Handler handler, InetAddress groupOwnerAddress, NodeProperties nodeProperties) {
        this.handler = handler;
        this.mAddress = groupOwnerAddress;
        this.nodeProperties = nodeProperties;
    }

    @Override
    public void run() {
        Socket socket = new Socket();
        try {
            socket.bind(null);
            socket.connect(new InetSocketAddress(mAddress.getHostAddress(),
                    WiFiServiceDiscoveryActivity.SERVER_PORT), 1000000);
            Log.d(TAG, "Launching the I/O handler");
            clusterRunnable = new ClusterConnect(socket, handler, nodeProperties);
            new Thread(clusterRunnable).start();
        } catch (IOException e) {
            e.printStackTrace();
            try {
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return;
        }
    }
}
