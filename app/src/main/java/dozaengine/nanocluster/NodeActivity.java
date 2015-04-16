package dozaengine.nanocluster;

import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import clusterapi.ClusterConnect;
import clusterapi.WiFiServiceDiscoveryActivity;

/**
 * Created by dozaEngine on 10/19/2014.
 */
public class NodeActivity extends WiFiServiceDiscoveryActivity {

    private PrimeNumManager primeNumManager;

    private TextView statusTxtView;

    //TODO: Evaluate the following properties
    private static int TotalPrimeNumCount = 0;
    private static float TotalComputationTime = 0.0f;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(dozaengine.nanocluster.R.layout.main);
        statusTxtView = (TextView) findViewById(dozaengine.nanocluster.R.id.status_text);
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MESSAGE_READ:
                Generator generator = (Generator) msg.obj;
                if(groupOwner) {

                    Log.d(TAG, "Received Results");
                    TotalPrimeNumCount += generator.readPrimeCount();
                    TotalComputationTime = generator.readCompTime();
                    Log.d(TAG, "Prime Numbers Identified: " + String.valueOf(TotalPrimeNumCount));
                    Log.d(TAG, "Computation Time: " + String.valueOf(TotalComputationTime));

                    if(primeNumManager != null) {
                        (primeNumManager).pushMessage("Prime Numbers Identified: " + String.valueOf(TotalPrimeNumCount));
                        (primeNumManager).pushMessage("Computation Time: " + String.valueOf(TotalComputationTime));
                        primeNumManager.computationComplete();
                    }else Log.d(TAG, "PrimeNumManager is NULL");
                }else{

                    Log.d(TAG, "Received Object for Processing");
                    if(primeNumManager != null) {
                        // TODO: Thread to Generate Primes
                        (primeNumManager).pushMessage("Computation In Progress..");
                        (primeNumManager).thread(generator);
                    }else Log.d(TAG, "PrimeNumManager is NULL");
                }
                break;

            case MY_HANDLE:
                Log.d(TAG,"Setting Handle");
                Object obj = msg.obj;
                if(((ClusterConnect)obj).nodeProperties != null)
                    Log.d(TAG,"CPU Freq:" + ((ClusterConnect)obj).nodeProperties.cpuFrequency);
                (primeNumManager).setConnection((ClusterConnect) obj);
        }
        return true;
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo p2pInfo) {
        super.onConnectionInfoAvailable(p2pInfo);

        // TODO: Refactor: PrimeNumberGenerator Class: Initiate Object: Set Ready for Generation
        if(primeNumManager == null){
            primeNumManager = new PrimeNumManager();
            (primeNumManager).setType(groupOwner);

            getFragmentManager().beginTransaction()
                    .replace(dozaengine.nanocluster.R.id.container_root, primeNumManager).commit();
            statusTxtView.setVisibility(View.GONE);
        }
    }

    @Override
    public void appendStatus(String status) {
        String current = statusTxtView.getText().toString();
        statusTxtView.setText(current + "\n" + status);
    }
}
