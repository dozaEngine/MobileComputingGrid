package clusterapi;

import java.io.Serializable;

/**
 * Created by dozaEngine on 12/5/2014.
 */
public class NodeProperties implements Serializable {

    public int cpuFrequency = 0;
    public float cpuBatteryPerc = 0.0f;
    public int processorsAvail = 0;
    public int hash = 0;

    public NodeProperties(int cpuFrequency, int processorsAvail, float cpuBatteryPerc){
        this.cpuFrequency = cpuFrequency;
        this.processorsAvail = processorsAvail;
        this.cpuBatteryPerc = cpuBatteryPerc;
    }

    void setHash(int _hash){
        hash = _hash;
    }

}
