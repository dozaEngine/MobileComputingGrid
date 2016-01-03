package dozaengine.nanocluster;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Random;


public class LoginActivity extends Activity {

    static final String SUDO_PIN = "2525";

    EditText mPin;
    TextView mAutokey;
    EditText mPasscode;

    Button mSecureLogin;

    Random random;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(dozaengine.nanocluster.R.layout.activity_login);

        // Security login objects
        mPin = (EditText) findViewById(dozaengine.nanocluster.R.id.pin_input);
        mAutokey =(TextView) findViewById(dozaengine.nanocluster.R.id.passcode_key);
        mPasscode = (EditText) findViewById(dozaengine.nanocluster.R.id.passcode_input);
        mSecureLogin = (Button) findViewById(dozaengine.nanocluster.R.id.button);

        mPasscode.setEnabled(false);

        mPin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String userInput = s.toString();
                if(userInput.contentEquals(SUDO_PIN)){
                    mAutokey.setText("");

                    mPasscode.setEnabled(true);

                    random = new Random(Calendar.getInstance().getTimeInMillis());

                    /* Function based security */
                    // The user must know the global secret equation for login
                    Long autokey = Math.abs(random.nextLong() % 100);
                    mAutokey.setText(autokey.toString());
                } else {
                    mAutokey.setText(dozaengine.nanocluster.R.string.dashhint);
                    mPasscode.getText().clear();
                    mPasscode.setEnabled(false);
                }
            }
        });
        mSecureLogin.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("Login Click", "Passcode Value: " + mPasscode.getText().toString());

                if(!mPasscode.getText().toString().isEmpty()){

                    Long autokey = Long.parseLong((mAutokey.getText().toString()));
                    Long passcode = Long.parseLong(mPasscode.getText().toString());
                    Log.i("CALCS", "AutoKey Value: " + autokey.toString());
                    Log.i("CALCS", "Passcode Value: " + passcode.toString());

                    /* Secret Login Formula */
                    if(passcode.longValue()/5 == autokey.longValue()){
                        startActivity(new Intent(LoginActivity.this, NetworkDeviceConfig.class));
                    }

                }
                else
                {
                    Toast toast = Toast.makeText(getApplicationContext(), "Login Failed!", Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(dozaengine.nanocluster.R.menu.login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == dozaengine.nanocluster.R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
