package dozaengine.nanocluster;

import android.util.Log;

import java.io.Serializable;

/**
 * Created by dozaEngine on 10/21/2014.
 */
public class Generator implements Serializable {

    String TAG = "Generator";

    private int min;
    private int max;
    private StringBuilder result;
    private int count;
    private long compTime;

    public Generator(int _min, int _max){

        if(_min >= _max) {
            this.min = 0;
            this.max = 0;
            this.count = 0;
        }else{
            this.min = _min;
            this.max = _max;
            this.count = 0;
            this.result = new StringBuilder();
            this.compTime = 0;
        }

    }

    public void generatePrimes()
    {
        // loop through the numbers one by one
        long t = System.currentTimeMillis();
        for (int i = min; i<max; i++) {

            boolean isPrimeNumber = true;

            // check to see if the number is prime
            for (int j = 2; j < i; j++) {
                if (i % j == 0) {
                    isPrimeNumber = false;
                    break; // exit the inner for loop
                }
            }

            // TODO: Packetize Prime Number List
            if (isPrimeNumber) {
                ++count;
            }

        }

        if(result != null) {
            result.append(count);
            compTime = System.currentTimeMillis() - t;
            String sResultTime = String.valueOf(compTime);
            Log.d(TAG, "Prime Count: " + result.toString());
            Log.d(TAG, "Time[ms]: " + sResultTime);
        }
    }

    public int readPrimeCount(){
        return count;
    }

    public long readCompTime(){
        return compTime;
    }

}
