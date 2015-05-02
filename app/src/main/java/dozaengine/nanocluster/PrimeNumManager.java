
package dozaengine.nanocluster;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


import clusterapi.ClusterConnect;
import clusterapi.NodeThreadManager;

/**
 * This fragment handles chat related UI which includes a list view for messages
 * and a message entry field with send button.
 */
public class PrimeNumManager extends NodeThreadManager {

    private boolean multithreaded = true;

    private String TAG = "PrimeNumManager";

    final private long PRIMES100K = 9592;
    final private long PRIMESHALFMIL = 41538;
    final private long PRIMES1MIL = 78498;
    final private long HUNDREDK = 100000;
    final private long HALFMIL = 500000;
    final private long ONEMIL = 1000000;
    private long PRIMES = 0;

    private View view;
    private TextView chatLine;
    private ListView listView;
    MessageAdapter adapter = null;
    private List<String> items = new ArrayList<String>();
    LinkedList<WorkNodeEntry> clusterNodes = new LinkedList<WorkNodeEntry>();

    // TODO: Evaluate properties
    private boolean taskLead = false;
    long timeStart,totalTime;

    long primeNumberCount = 0;
    private long finalNum = 100000;
    public int bNum = 0, aNum = 1;
    boolean notSorted = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        view = inflater.inflate(dozaengine.nanocluster.R.layout.fragment_chat, container, false);
        chatLine = (TextView) view.findViewById(dozaengine.nanocluster.R.id.txtChatLine);
        listView = (ListView) view.findViewById(android.R.id.list);
        adapter = new MessageAdapter(getActivity(), android.R.id.text1,
                items);
        listView.setAdapter(adapter);

        /* Output: UI Thread to Socket Connection */
        view.findViewById(dozaengine.nanocluster.R.id.button1).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View arg0) {
                        Log.d(getTag(), "Send Clicked");

                        if(taskLead && chatLine.getText().toString().equalsIgnoreCase("1MIL"))
                        {
                            PRIMES = PRIMES1MIL;
                            finalNum = ONEMIL;
                            chatLine.setText("Set to search to One Million.");
                        }
                        else if(taskLead && chatLine.getText().toString().equalsIgnoreCase("HALFMIL"))
                        {
                            PRIMES = PRIMESHALFMIL;
                            finalNum = HALFMIL;
                            chatLine.setText("Set to search to Half Million.");
                        }
                        else if(taskLead && chatLine.getText().toString().equalsIgnoreCase("100K"))
                        {
                            PRIMES = PRIMES100K;
                            finalNum = HUNDREDK;
                            chatLine.setText("Set to search to One Hundred Thousand.");
                        }

                        if(taskLead && clusterNodes.size() != 0) {
                            Log.d(getTag(), "taskLead");

                            int totalGens = clusterNodes.size();
                            long partition = finalNum/(totalGens);
                            bNum += partition+ (finalNum%totalGens);

                            // Load Balance by providing easiest work load
                            // to lowers frequency processor
                            int cpuFreq = 0;

                            // Order Nodes by CPU Frequency to Load Balance when scheduling tasks
                            if(notSorted) {
                                for (int i = 1; i < clusterNodes.size(); i++) {
                                    WorkNodeEntry sortNode = clusterNodes.get(i);
                                    int j = i;
                                    while( j > 0 && (sortNode.getClusterConnect().nodeProperties.cpuFrequency
                                            < clusterNodes.get(j-1).getClusterConnect().nodeProperties.cpuFrequency)){

                                        clusterNodes.set(j,clusterNodes.get(j-1));
                                        j--;
                                    }
                                    clusterNodes.set(j,sortNode);
                                }
                                notSorted = false;
                            }

                            // TODO: Create Active Work Queue
                            timeStart = System.currentTimeMillis();
                            for (int n = 0; n < clusterNodes.size(); n++) {

                                int cores = clusterNodes.get(n).getClusterConnect().nodeProperties.processorsAvail;

                                //Packaged Task
                                Generator generator = new Generator(aNum, bNum);
                                clusterNodes.get(n).setGenerator(generator);
                                clusterNodes.get(n).setTaskComplete(false);

                                // Send Task
                                clusterNodes.get(n).getClusterConnect().write(generator);

                                // TODO: Remove: Using for Debug
                                // Increase Search Floor - prevent from using previously used node
                                cpuFreq = clusterNodes.get(n).getClusterConnect().nodeProperties.cpuFrequency;
                                pushMessage(chatLine.getText().toString() + "\n" +
                                        " Node KHz:"+cpuFreq+" Cores:"+cores+" Search Range> Init: " + aNum + " Final: " + bNum);
                                chatLine.setText("");
                                chatLine.clearFocus();

                                aNum = bNum + 1;
                                bNum += partition;
                            }

                            /* Reset */
                            aNum = 1;
                            bNum = 0;

                            /**
                             *  Start task monitor
                             * */


                        }else if (clusterConnect != null) {
                            Log.d(getTag(), "peer");

                            // TODO: Remove: Using for Debug
                            if(chatLine.getText().toString().equalsIgnoreCase("single"))
                            {
                                multithreaded = false;
                                chatLine.setText("Set to Single Threaded.");
                            }
                            else if(chatLine.getText().toString().equalsIgnoreCase("multi"))
                            {
                                multithreaded = true;
                                chatLine.setText("Set to Multi Threaded.");
                            }
                            else multithreaded = false;

                            pushMessage(chatLine.getText().toString() + "\n" +
                                        " Waiting on task... ");
                            chatLine.setText("");
                            chatLine.clearFocus();
                        }
                    }

                });
        return view;
    }

    public void setType(boolean on){
        this.taskLead = on;
    }

    public void updateHeartbeat(int hash)
    {
        for(int n = 0; n < clusterNodes.size(); n++)
        {
            // find node and update time
            if(clusterNodes.get(n).getClusterConnect().nodeProperties.hash == hash)
            {
                clusterNodes.get(n).setHeartbeatTime(System.currentTimeMillis());
                return;
            }
        }
    }

    public boolean checkComplete()
    {
        if(PRIMES <= primeNumberCount)
        {
            primeNumberCount = 0;

            for(int n = 0; n < clusterNodes.size(); n++)
            {
                // find node and update time
                clusterNodes.get(n).setTaskComplete(true);
            }

            /**
             * Stop task monitor
             * */

            return true;
        }
        return false;
    }

    public void nodeComputationComplete(){
        totalTime = System.currentTimeMillis() - timeStart;
        pushMessage("Total Time: " + String.valueOf(totalTime));
    }

    public void setConnection(ClusterConnect obj) {
        Log.d(TAG,"Setting Connection");
        if (obj != null) {
            clusterConnect = obj;
            if(taskLead) {
                clusterNodes.add(new WorkNodeEntry(obj,System.currentTimeMillis()));
                notSorted = true;
            }else{
                this.startHeartbeat();
            }
        }else
            Log.d(TAG, "Object Null");
    }

    public void pushMessage(String readMessage) {
        if(readMessage != null) {
            adapter.add(readMessage);
            adapter.notifyDataSetChanged();
        }else Log.d(TAG, "Push Message NULL");
    }

    // TODO: Run in thread
    //run the generator in a thread
    public void thread(Generator generator){
        if(!taskLead && generator != null) {
            if(!multithreaded) {
                generator.generatePrimes();
                clusterConnect.write(generator);
                pushMessage("Computation Completed in " + String.valueOf(generator.readCompTime()) + "ms!");
            }
            else{
                int cores = clusterConnect.nodeProperties.processorsAvail;
                aNum = generator.readMin();
                bNum = generator.readMax();
                int partition = (generator.readMax() - generator.readMin()) / cores;
                int numA = generator.readMin();
                int numB = numA + partition;

                for (int t = 0; t < cores; t++) {
                    GeneratorCrank generatorCrank = new GeneratorCrank(new Generator(numA, numB));

                    //Use API to queue work
                    this.loadWork(generatorCrank);

                    numA = numB + 1;
                    numB += partition;
                }
            }
        }
    }

    /**
     * ArrayAdapter to manage chat messages.
     */
    public class MessageAdapter extends ArrayAdapter<String> {

        public MessageAdapter(Context context, int textViewResourceId,
                List<String> items) {
            super(context, textViewResourceId, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;

            if (v == null) {
                LayoutInflater vi = (LayoutInflater) getActivity()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(android.R.layout.simple_list_item_1, null);
            }

            String message = items.get(position);
            if (message != null && !message.isEmpty()) {
                TextView nameText = (TextView) v
                        .findViewById(android.R.id.text1);

                if (nameText != null) {
                    nameText.setText(message);
                    if (message.startsWith("Me: ")) {
                        nameText.setTextAppearance(getActivity(),
                                dozaengine.nanocluster.R.style.normalText);
                    } else {
                        nameText.setTextAppearance(getActivity(),
                                dozaengine.nanocluster.R.style.boldText);
                    }
                }
            }
            return v;
        }
    }

    /**
     *
     * Class: GeneratorCrank
     * Implements: Runnable
     *
     * Description: Class to run prime number generation loops
     *
     * */
    private class GeneratorCrank implements Runnable {

        private Generator generator;

        public GeneratorCrank(Generator generator) {
            this.generator = generator;
        }

        @Override
        public void run() {
            generator.generatePrimes();
            if(multithreaded) sendComplete(generator);
            else clusterConnect.write(generator);
        }
    }

    private class WorkNodeEntry{
        private ClusterConnect clusterConnect;
        private long lastHeartbeat;
        private Generator generator;
        private boolean taskComplete = false;

        public WorkNodeEntry(ClusterConnect _clusterConnect, long _time)
        {
            clusterConnect = _clusterConnect;
            lastHeartbeat = _time;
        }

        public ClusterConnect getClusterConnect() {
            return clusterConnect;
        }

        public long getLastHeartbeat() {
            return lastHeartbeat;
        }

        public void setHeartbeatTime(long lastHeartbeat) {
            this.lastHeartbeat = lastHeartbeat;
        }

        public boolean compare(Generator _generator) {
            if(generator == null || _generator == null) return false;

            if((generator.readMin() == _generator.readMin())&&
               (generator.readMax() == _generator.readMax() ))
                return true;
            else
                return false;

        }

        public void setGenerator(Generator generator) {
            this.generator = generator;
        }

        public boolean isTaskComplete() {
            return taskComplete;
        }

        public void setTaskComplete(boolean taskComplete) {
            this.taskComplete = taskComplete;
        }
    }
}
