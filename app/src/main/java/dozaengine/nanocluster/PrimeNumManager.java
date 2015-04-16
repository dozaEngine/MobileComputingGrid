
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

/**
 * This fragment handles chat related UI which includes a list view for messages
 * and a message entry field with send button.
 */
public class PrimeNumManager extends Fragment {

    private String TAG = "PrimeNumManager";

    private View view;
    private static ClusterConnect primeNumGenConnect;
    private TextView chatLine;
    private ListView listView;
    MessageAdapter adapter = null;
    private List<String> items = new ArrayList<String>();
    LinkedList<ClusterConnect> clusterNodes = new LinkedList<ClusterConnect>();

    // TODO: Evaluate properties
    private boolean taskLead = false;
    long timeStart,totalTime;

    int finalNum = 100000;
    int bNum = 0, aNum = 1;
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

                        if(taskLead && clusterNodes.size() != 0) {
                            Log.d(getTag(), "taskLead");

                            int partition = finalNum/clusterNodes.size();
                            bNum += partition+ (finalNum%clusterNodes.size());

                            // Load Balance by providing easiest work load
                            // to lowers frequency processor
                            int cpuFreq = 0;

                            // Order Nodes by CPU Frequency to Load Balance when scheduling tasks
                            if(notSorted) {
                                for (int i = 1; i < clusterNodes.size(); i++) {
                                    ClusterConnect sortNode = clusterNodes.get(i);
                                    int j = i;
                                    while( j > 0 && (sortNode.nodeProperties.cpuFrequency < clusterNodes.get(j-1).nodeProperties.cpuFrequency)){
                                        clusterNodes.set(j,clusterNodes.get(j-1));
                                        j--;
                                    }
                                    clusterNodes.set(j,sortNode);
                                }
                                notSorted = false;
                            }


                            // TODO: Create Active Work Queue
                            timeStart = System.currentTimeMillis();
                            for (int i = 0; i < clusterNodes.size(); i++) {

                                //Packaged Task
                                Generator generator = new Generator(aNum, bNum);

                                // Increase Search Floor - prevent from using previously used node
                                cpuFreq = clusterNodes.get(i).nodeProperties.cpuFrequency;

                                // Send Task
                                clusterNodes.get(i).write(generator);

                                // TODO: Remove: Using for Debug
                                pushMessage(chatLine.getText().toString() + "\n" +
                                        " Node KHz:"+cpuFreq+" Search Range> Init: " + aNum + " Final: " + bNum);
                                chatLine.setText("");
                                chatLine.clearFocus();

                                aNum = bNum+1;
                                bNum +=partition;
                            }

                            /* Reset */
                            aNum = 1;
                            bNum = 0;

                        }else if (primeNumGenConnect != null) {
                            Log.d(getTag(), "peer");

                            // TODO: Remove: Using for Debug
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

    public void computationComplete(){
        totalTime = System.currentTimeMillis() - timeStart;
        pushMessage("Total Time: " + String.valueOf(totalTime));
    }

    public void setConnection(ClusterConnect obj) {
        Log.d(TAG,"Setting Connection");
        if (obj != null) {
            primeNumGenConnect = obj;
            if(taskLead) {
                clusterNodes.add(obj);
                notSorted = true;
            }else{
                obj.write(obj.nodeProperties);
                Log.e(TAG,"Client Sent Node Properties!");
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
        if(generator != null) {
            generator.generatePrimes();
            primeNumGenConnect.write(generator);
            pushMessage("Computation Completed in " + String.valueOf(generator.readCompTime()) + "ms!");
//        GeneratorCrank generatorCrank = new GeneratorCrank(generator);
//        new Thread(generatorCrank);
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
                primeNumGenConnect.write(generator);
                pushMessage("Computation Completed in " + String.valueOf(generator.readCompTime()) + "ms!");
        }
    }
}
