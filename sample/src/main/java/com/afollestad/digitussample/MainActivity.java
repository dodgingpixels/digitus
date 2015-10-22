package com.afollestad.digitussample;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.afollestad.digitus.Digitus;
import com.afollestad.digitus.DigitusCallback;

/**
 * @author Aidan Follestad (afollestad)
 */
public class MainActivity extends AppCompatActivity implements DigitusCallback {

    private TextView mStatus;
    private Button mButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mStatus = (TextView) findViewById(R.id.status);
        mButton = (Button) findViewById(R.id.beginAuthentication);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Digitus.init(this, getString(R.string.app_name), 6969);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Digitus.get().beginAuthentication();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        Digitus.deinit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Digitus.get().handleResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onDigitusReady(Digitus digitus) {
        mStatus.setText(R.string.status_ready);
        mButton.setEnabled(true);
    }

@Override
public void onDigitusRegistrationNeeded(Digitus digitus) {
    mStatus.setText(R.string.status_registration_needed);
    mButton.setText(R.string.open_security_settings);
    mButton.setEnabled(true);
    mButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mButton.setText(R.string.begin_authentication);
            Digitus.get().openSecuritySettings();
        }
    });
}

    @Override
    public void onDigitusAuthenticated(Digitus digitus) {
        mStatus.setText(R.string.status_authenticated);
        mButton.setEnabled(true);
    }

    @Override
    public void onDigitusError(Digitus digitus, Exception e) {
        mStatus.setText(getString(R.string.status_error, e.getMessage()));
    }

    @Override
    public void onDigitusValidatePassword(Digitus digitus, final String password) {
        // Start a background thread to simulate 3 seconds of background processing, e.g. contacting a server
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Digitus.get().notifyPasswordValidation(password.equals("password"));
            }
        }).start();
    }
}
