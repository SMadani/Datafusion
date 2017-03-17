package uk.co.synatronics.Datafusion;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.*;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Switch;
import static uk.co.synatronics.Datafusion.MainActivity.*;
import java.util.concurrent.TimeUnit;

public class SettingsActivity extends Activity implements View.OnTouchListener, SwipeDetector.OnSwipeListener {

    private long frequency;
    private boolean updateOnWiFi, allowNotifications, smsEnabled;
    private String phoneNumber;
    private EditText frequencyValueView, phoneNumberValueView;
    private Switch wifiSwitch, notificationSwitch, smsSwitch;
    private GestureDetector gDetector;

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        gDetector = new GestureDetector(this, new SwipeDetector(this));
        RelativeLayout rootView = (RelativeLayout) findViewById(R.id.settingsRootView);
        rootView.setOnTouchListener(this);
        frequencyValueView = (EditText) findViewById(R.id.frequencyValueView);
        phoneNumberValueView = (EditText) findViewById(R.id.phoneNumberValueView);
        wifiSwitch = (Switch) findViewById(R.id.wifiSwitch);
        notificationSwitch = (Switch) findViewById(R.id.notificationsSwitch);
        smsSwitch = (Switch) findViewById(R.id.smsSwitch);

        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        Intent caller = getIntent();

        frequency = prefs.getLong(UPDATE_FREQUENCY, 3600000);
        updateOnWiFi = prefs.getBoolean(UPDATE_WIFI_ONLY, true);
        allowNotifications = prefs.getBoolean(ALLOW_NOTIFICATIONS, true);
        smsEnabled = prefs.getBoolean(SMS_ENABLED, smsEnabled);
        phoneNumber = prefs.getString(PHONE_NUMBER, phoneNumber);

        if (savedInstanceState != null) {
            frequency = savedInstanceState.getLong(UPDATE_FREQUENCY, frequency);
            updateOnWiFi = savedInstanceState.getBoolean(UPDATE_WIFI_ONLY, updateOnWiFi);
            allowNotifications = savedInstanceState.getBoolean(ALLOW_NOTIFICATIONS, allowNotifications);
            smsEnabled = savedInstanceState.getBoolean(SMS_ENABLED, smsEnabled);
            phoneNumber = savedInstanceState.getString(PHONE_NUMBER, phoneNumber);
        }

        else if (caller != null) {
            frequency = caller.getLongExtra(UPDATE_FREQUENCY, frequency);
            updateOnWiFi = caller.getBooleanExtra(UPDATE_WIFI_ONLY, updateOnWiFi);
            allowNotifications = caller.getBooleanExtra(ALLOW_NOTIFICATIONS, allowNotifications);
            smsEnabled = caller.getBooleanExtra(SMS_ENABLED, smsEnabled);
            phoneNumber = caller.getStringExtra(PHONE_NUMBER);
        }

        frequencyValueView.setText(toFrequencyText());
        phoneNumberValueView.setText(phoneNumber);
        wifiSwitch.setChecked(updateOnWiFi);
        notificationSwitch.setChecked(allowNotifications);
        smsSwitch.setChecked(smsEnabled);
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onSaveInstanceState (Bundle outState) {
        outState.putLong(UPDATE_FREQUENCY, frequency);
        outState.putBoolean(UPDATE_WIFI_ONLY, updateOnWiFi);
        outState.putBoolean(ALLOW_NOTIFICATIONS, allowNotifications);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        assignFromViews();
        Intent result = new Intent(Intent.ACTION_DEFAULT)
                .putExtra(UPDATE_FREQUENCY, frequency)
                .putExtra(UPDATE_WIFI_ONLY, updateOnWiFi)
                .putExtra(ALLOW_NOTIFICATIONS, allowNotifications)
                .putExtra(SMS_ENABLED, smsEnabled)
                .putExtra(PHONE_NUMBER, phoneNumber);

        setResult (item != null && item.getItemId() == R.id.action_done
                ? RESULT_OK
                : RESULT_CANCELED
                , result);
        finish();
        return true;
    }

    @Override
    public boolean onTouch (View v, MotionEvent event) {
        return gDetector.onTouchEvent(event);
    }

    @Override
    public boolean onSwipeLeft (float distance, float velocity) {
        return false;
    }

    @Override
    public boolean onSwipeRight (float distance, float velocity) {
        onOptionsItemSelected(null);
        return true;
    }

    @Override
    public boolean onSwipeDown (float distance, float velocity) {
        return false;
    }

    @Override
    public boolean onSwipeUp (float distance, float velocity) {
        return false;
    }

    private String toFrequencyText() {
        return String.format("%.2f", (double) TimeUnit.MILLISECONDS.toSeconds(frequency)/60);
    }

    private long fromFrequencyText() {
        try {
            return (long) (Double.parseDouble(frequencyValueView.getText().toString())*60000);
        }
        catch (Exception e) {
            System.err.println("Could not parse frequency: "+e.getMessage());
            return frequency;
        }
    }

    private void assignFromViews() {
        frequency = fromFrequencyText();
        updateOnWiFi = wifiSwitch.isChecked();
        allowNotifications = notificationSwitch.isChecked();
        smsEnabled = smsSwitch.isChecked();
        phoneNumber = phoneNumberValueView.getText().toString();
    }
}