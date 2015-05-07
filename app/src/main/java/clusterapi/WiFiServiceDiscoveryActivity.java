
package clusterapi;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdServiceResponseListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdTxtRecordListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import dozaengine.nanocluster.R;

/**
 * The main activity for the sample. This activity registers a local service and
 * perform discovery over Wi-Fi p2p network. It also hosts a couple of fragments
 * to manage chat operations. When the app is launched, the device publishes a
 * chat service and also tries to discover services published by other peers. On
 * selecting a peer published service, the app initiates a Wi-Fi P2P (Direct)
 * connection with the peer. On successful connection with a peer advertising
 * the same service, the app opens up sockets to initiate a chat.
 * {@code WiFiChatFragment} is then added to the the main activity which manages
 * the interface and messaging needs for a chat session.
 */
public class WiFiServiceDiscoveryActivity extends Activity implements Handler.Callback, ConnectionInfoListener{

    public static final String TAG = "WiFiServiceDiscoveryActivity";

    // TXT RECORD properties
    public static final String TXTRECORD_PROP_AVAILABLE = "available";
    public static final String SERVICE_INSTANCE = "_wifidemotest";
    public static final String SERVICE_REG_TYPE = "_presence._tcp";

    public static final int MESSAGE_READ = 0x400 + 1;
    public static final int MY_HANDLE = 0x400 + 2;
    public static final int CLIENT_TASK_COMPLETE = 0x400 + 3;
    public static final int HEARTBEAT = 0x400 + 4;
    private WifiP2pManager wifiP2pManager;

    static final int SERVER_PORT = 4545;

    private final IntentFilter intentFilter = new IntentFilter();
    private Channel wifiDirectChannel;
    private BroadcastReceiver receiver = null;
    private WifiP2pDnsSdServiceRequest serviceRequest;

    public Handler handler = new Handler(this);
    private static Thread socketThread = null;

    // Node Properties
    NodeProperties nodeProperties;
    public static boolean groupOwner = false;
    public static float batteryLife = 0.0f;

    private boolean isWifiP2pEnabled = false;
    private boolean retryChannel = false;

    private WiFiDirectServicesList servicesList;

    /**
     * Gets the number of cores available in this device, across all processors.
     * Requires: Ability to peruse the filesystem at "/sys/devices/system/cpu"
     * @return The number of cores, or 1 if failed to get result
     */
    private int getNumCores() { return Runtime.getRuntime().availableProcessors(); }

    private void initializeWiFiDirect() {
        wifiP2pManager =
                (WifiP2pManager)getSystemService(Context.WIFI_P2P_SERVICE);

        wifiDirectChannel = wifiP2pManager.initialize(this, getMainLooper(),
                new WifiP2pManager.ChannelListener() {
                    public void onChannelDisconnected() {
                        initializeWiFiDirect();
                    }
                }
        );
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        // Check Current Battery Life
        getBatterLife();

        // Get CPU Info
        getCPUInfo();

        // Get Freq Info
        String strCpuFreq = getFreqInfo();
        Log.d(TAG, "CPU Freq String:" + strCpuFreq);
        int maxCpuFreq = Integer.parseInt(strCpuFreq.trim());
        nodeProperties = new NodeProperties(maxCpuFreq,getNumCores(),batteryLife);
        nodeProperties.setHash(((Object)nodeProperties).hashCode());

        initializeWiFiDirect();

        startRegistrationAndDiscovery();

        servicesList = new WiFiDirectServicesList();
        getFragmentManager().beginTransaction()
                .add(R.id.container_root, servicesList, "services").commit();
    }
/*    *//**
     * Remove all peers and clear all fields. This is called on
     * BroadcastReceiver receiving a state change event.
     *//*
    public void resetData() {
        DeviceListFragment fragmentList = (DeviceListFragment) getFragmentManager()
                .findFragmentById(R.id.frag_list);
        DeviceDetailFragment fragmentDetails = (DeviceDetailFragment) getFragmentManager()
                .findFragmentById(R.id.frag_detail);
        if (fragmentList != null) {
            fragmentList.clearPeers();
        }
        if (fragmentDetails != null) {
            fragmentDetails.resetViews();
        }
    }
    */

    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }

    private void getBatterLife() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = this.registerReceiver(null, ifilter);

        assert batteryStatus != null;
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        batteryLife = level / (float)scale;
        Log.d(TAG, "Batter Percent: " + String.valueOf(batteryLife));
    }

    private void getWifiRssi(){
        /// Functionality not yet available on Android (12/5/2014)
    }

    // Access Kernel CPU Info
    private String getFreqInfo() {
        StringBuffer sb = new StringBuffer();
        if (new File("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq").exists()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(new File("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq")));
                String aLine;
                while ((aLine = br.readLine()) != null) {
                    sb.append(aLine + "\n");
                }
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Log.d(TAG, "CPU Frequency Info: " + sb.toString());
        return sb.toString();
    }

    // Access Kernel CPU Info
    private String getCPUInfo() {
        StringBuffer sb = new StringBuffer();
        sb.append("abi: ").append(Build.CPU_ABI).append("\n");
        if (new File("/proc/cpuinfo").exists()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(new File("/proc/cpuinfo")));
                String aLine;
                while ((aLine = br.readLine()) != null) {
                    sb.append(aLine + "\n");
                }
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Log.d(TAG, "CPU Info: " + sb.toString());
        return sb.toString();
    }

    @Override
    protected void onRestart() {
        Log.d(TAG,"onRestart > Entry");

        // Check Current Battery Life
        getBatterLife();

        Fragment frag = getFragmentManager().findFragmentByTag("services");
        if (frag != null) {
            getFragmentManager().beginTransaction().remove(frag).commit();
        }

        super.onRestart();
        Log.d(TAG,"onRestart > Exit");
    }

    @Override
    protected void onStop() {
        Log.d(TAG,"onStop > Entry");
        if (wifiP2pManager != null && wifiDirectChannel != null) {
            wifiP2pManager.removeGroup(wifiDirectChannel, new ActionListener() {

                @Override
                public void onFailure(int reasonCode) {
                    Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);
                }

                @Override
                public void onSuccess() {
                }

            });
        }
        super.onStop();
        Log.d(TAG,"onStop > Exit");
    }

    /**
     * Registers a local service and then initiates a service discovery
     */
    private void startRegistrationAndDiscovery() {
        Log.d(TAG,"startRegistrationAndDiscovery > Entry");
        Map<String, String> record = new HashMap<String, String>();
        record.put(TXTRECORD_PROP_AVAILABLE, "visible");

        WifiP2pDnsSdServiceInfo service = WifiP2pDnsSdServiceInfo.newInstance(
                SERVICE_INSTANCE, SERVICE_REG_TYPE, record);
        wifiP2pManager.addLocalService(wifiDirectChannel, service, new ActionListener() {

            @Override
            public void onSuccess() {
                appendStatus("Added Local Service");
            }

            @Override
            public void onFailure(int error) {
                appendStatus("Failed to add a service");
            }
        });

        discoverService();
        Log.d(TAG,"startRegistrationAndDiscovery > Exit");
    }

    private void discoverService() {

        Log.d(TAG,"discoverService > Entry");

        /*
         * Register listeners for DNS-SD services. These are callbacks invoked
         * by the system when a service is actually discovered.
         */
        wifiP2pManager.setDnsSdResponseListeners(wifiDirectChannel,
                new DnsSdServiceResponseListener() {

                    @Override
                    public void onDnsSdServiceAvailable(String instanceName,
                            String registrationType, WifiP2pDevice srcDevice) {

                        Log.d(TAG, "onDnsSdServiceAvailable > Entry");
                        // A service has been discovered. Is this our app?
                        if (instanceName.equalsIgnoreCase(SERVICE_INSTANCE)) {

                            // update the UI and add the item the discovered
                            // device.
                            WiFiDirectServicesList fragment = (WiFiDirectServicesList) getFragmentManager()
                                    .findFragmentByTag("services");
                            if (fragment != null) {
                                WiFiDirectServicesList.WiFiDevicesAdapter adapter = ((WiFiDirectServicesList.WiFiDevicesAdapter) fragment
                                        .getListAdapter());
                                WiFiP2pService service = new WiFiP2pService();
                                service.device = srcDevice;
                                service.instanceName = instanceName;
                                service.serviceRegistrationType = registrationType;
                                adapter.add(service);
                                adapter.notifyDataSetChanged();
                                Log.d(TAG, "onBonjourServiceAvailable "
                                        + instanceName);
                            }
                        }

                        Log.d(TAG, "onDnsSdServiceAvailable > Exit");
                    }
                }, new DnsSdTxtRecordListener() {

                    /**
                     * A new TXT record is available. Pick up the advertised
                     * buddy name.
                     */
                    @Override
                    public void onDnsSdTxtRecordAvailable(
                            String fullDomainName, Map<String, String> record,
                            WifiP2pDevice device) {
                        Log.d(TAG,
                                device.deviceName + " is "
                                        + record.get(TXTRECORD_PROP_AVAILABLE));
                    }
                });

        // After attaching listeners, create a service request and initiate
        // discovery.
        serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        wifiP2pManager.addServiceRequest(wifiDirectChannel, serviceRequest,
                new ActionListener() {

                    @Override
                    public void onSuccess() {
                        appendStatus("Added service discovery request");
                    }

                    @Override
                    public void onFailure(int arg0) {
                        appendStatus("Failed adding service discovery request");
                    }
                });

        wifiP2pManager.discoverServices(wifiDirectChannel, new ActionListener() {

            @Override
            public void onSuccess() {
                appendStatus("Service discovery initiated");
            }

            @Override
            public void onFailure(int arg0) { appendStatus("Service discovery failed");}
        });
        Log.d(TAG,"discoverService > Exit");
    }

    public void connectP2p(WiFiP2pService service) {
        Log.d(TAG,"connectP2p > Entry");
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = service.device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;

        if (serviceRequest != null)
            wifiP2pManager.removeServiceRequest(wifiDirectChannel, serviceRequest,
                    new ActionListener() {

                        @Override
                        public void onSuccess() {
                        }

                        @Override
                        public void onFailure(int arg0) {
                        }
                    });

        wifiP2pManager.connect(wifiDirectChannel, config, new ActionListener() {

            @Override
            public void onSuccess() {
                appendStatus("Connecting to service");
            }

            @Override
            public void onFailure(int errorCode) {
                appendStatus("Failed connecting to service");
            }
        });

        Log.d(TAG,"connectP2p > Exit");
    }

    @Override
    public boolean handleMessage(Message msg) {
        return true;
    }

    @Override
    public void onResume() {
        Log.d(TAG,"onResume > Entry");
        super.onResume();
        receiver = new WiFiDirectBroadcastReceiver(wifiP2pManager, wifiDirectChannel, this);
        registerReceiver(receiver, intentFilter);
        Log.d(TAG,"onResume > Exit");
    }

    @Override
    public void onPause() {
        Log.d(TAG,"onPause > Entry");
        super.onPause();
        unregisterReceiver(receiver);
        Log.d(TAG,"onPause > Exit");
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo p2pInfo) {
        Log.d(TAG,"onConnectionInfoAvailable > Entry");
        /*
         * The group owner accepts connections using a server socket and then spawns a
         * client socket for every client. This is handled by {@code
         * GroupOwnerSocketHandler}
         */
        // TODO: Refactor: Node Resource has been identified. Assign Characteristic/State/Task.
        if (p2pInfo.isGroupOwner) {
            Log.d(TAG, "Connected as group owner");
            groupOwner = true;

            try {
                if(socketThread == null) {
                    socketThread = new GroupOwnerSocketHandler(this.handler);

                    Log.d(TAG, "Thread State: " + ((socketThread.getState() == Thread.State.RUNNABLE) ? "Runnable" :
                            (socketThread.getState() == Thread.State.NEW) ? "New" :
                                    (socketThread.getState() == Thread.State.TERMINATED) ? "Terminated" :
                                            (socketThread.getState() == Thread.State.BLOCKED) ? "Blocked" :
                                                    (socketThread.getState() == Thread.State.TIMED_WAITING) ? "Time Waiting" :
                                                            (socketThread.getState() == Thread.State.WAITING) ? "Waiting" : "Unknown"));

                    //if(socketThread.getState() == Thread.State.NEW)
                    socketThread.start();
                }
            } catch (IOException e) {
                Log.d(TAG, "Failed to create a server thread - " + e.getMessage());
            }
        } else {
            Log.d(TAG, "Connected as peer");
            groupOwner = false;
            //if(socketThread == null)
                socketThread = new ClientSocketHandler(this.handler, p2pInfo.groupOwnerAddress, nodeProperties);

            Log.d(TAG, "Thread State: " + ((socketThread.getState() == Thread.State.RUNNABLE) ? "Runnable" :
                    (socketThread.getState() == Thread.State.NEW) ? "New" :
                            (socketThread.getState() == Thread.State.TERMINATED) ? "Terminated" :
                                    (socketThread.getState() == Thread.State.BLOCKED) ? "Blocked" :
                                            (socketThread.getState() == Thread.State.TIMED_WAITING) ? "Time Waiting" :
                                                    (socketThread.getState() == Thread.State.WAITING) ? "Waiting" : "Unknown"));

            //if(socketThread.getState() == Thread.State.NEW)
                socketThread.start();
            /*
            else if(socketThread.getState() == Thread.State.TERMINATED){
                socketThread = null;
                socketThread = new ClientSocketHandler(this.handler, p2pInfo.groupOwnerAddress);
                Log.d(TAG, "Now Thread State: " + ((socketThread.getState() == Thread.State.RUNNABLE) ? "Runnable" :
                        (socketThread.getState() == Thread.State.NEW) ? "New" :
                                (socketThread.getState() == Thread.State.TERMINATED) ? "Terminated" :
                                        (socketThread.getState() == Thread.State.BLOCKED) ? "Blocked" :
                                                (socketThread.getState() == Thread.State.TIMED_WAITING) ? "Time Waiting" :
                                                        (socketThread.getState() == Thread.State.WAITING) ? "Waiting" : "Unknown"));
                socketThread.start();
            }
            */
        }

        Log.d(TAG,"onConnectionInfoAvailable > Exit");
    }

    public void appendStatus(String status) {
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        /*
        if((socketThread != null) &&
                (socketThread.getState() != Thread.State.TERMINATED &&
                 socketThread.getState() != Thread.State.NEW))
            socketThread.interrupt();
        */
    }
}