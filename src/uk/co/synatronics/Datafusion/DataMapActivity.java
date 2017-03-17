package uk.co.synatronics.Datafusion;

import android.app.Activity;
import android.widget.RelativeLayout;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.Button;
import android.widget.TextView;
import static uk.co.synatronics.Datafusion.DoubleDataSource.*;
import static uk.co.synatronics.Datafusion.MainActivity.*;

public class DataMapActivity extends Activity implements View.OnTouchListener, SwipeDetector.OnSwipeListener {

    private TextView targetValueView;
    private TextView lowValueView;
    private TextView highValueView;
    private double reading, target, lowThreshold, highThreshold;
    private boolean targetEnabled, lowEnabled, highEnabled;
    private int index;
    private long dataID;
    private String key;
    private GestureDetector gDetector;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_map);
        gDetector = new GestureDetector(this, new SwipeDetector(this));
        RelativeLayout rootView = (RelativeLayout) findViewById(R.id.mapRootView);
        rootView.setOnTouchListener(this);
        Button targetBtn = (Button) findViewById(R.id.targetBtn);
        Button lowBtn = (Button) findViewById(R.id.lowBtn);
        Button highBtn = (Button) findViewById(R.id.highBtn);
        final TextView readingValueView = (TextView) findViewById(R.id.readingValueView);
        targetValueView = (TextView) findViewById(R.id.targetValueView);
        lowValueView = (TextView) findViewById(R.id.lowValueView);
        highValueView = (TextView) findViewById(R.id.highValueView);

        Intent caller = getIntent();
        if (savedInstanceState != null) {
            reading = savedInstanceState.getDouble(READING);
            target = savedInstanceState.getDouble(TARGET);
            lowThreshold = savedInstanceState.getDouble(LOW_THRESHOLD);
            highThreshold = savedInstanceState.getDouble(HIGH_THRESHOLD);
            targetEnabled = savedInstanceState.getBoolean(TARGET_ENABLED);
            lowEnabled = savedInstanceState.getBoolean(LOW_ENABLED);
            highEnabled = savedInstanceState.getBoolean(HIGH_ENABLED);
            index = savedInstanceState.getInt(DATA_INDEX);
            dataID = savedInstanceState.getLong(DATA_ID);
            key = savedInstanceState.getString(DATA_KEY);
        }
        else if (caller != null) {
            reading = caller.getDoubleExtra(READING, reading);
            target = caller.getDoubleExtra(TARGET, target);
            lowThreshold = caller.getDoubleExtra(LOW_THRESHOLD, lowThreshold);
            highThreshold = caller.getDoubleExtra(HIGH_THRESHOLD, highThreshold);
            targetEnabled = caller.getBooleanExtra(TARGET_ENABLED, true);
            lowEnabled = caller.getBooleanExtra(LOW_ENABLED, true);
            highEnabled = caller.getBooleanExtra(HIGH_ENABLED, true);
            index = caller.getIntExtra(DATA_INDEX, index);
            dataID = caller.getLongExtra(DATA_ID, dataID);
            key = caller.getStringExtra(DATA_KEY);
        }

        targetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v) {
                targetValueView.setEnabled(!targetValueView.isEnabled());
            }
        });
        lowBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v) {
                lowValueView.setEnabled(!lowValueView.isEnabled());
            }
        });
        highBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v) {
                highValueView.setEnabled(!highValueView.isEnabled());
            }
        });

        assignText(readingValueView, reading);
        assignText(targetValueView, target);
        assignText(lowValueView, lowThreshold);
        assignText(highValueView, highThreshold);

        targetValueView.setEnabled(targetEnabled);
        lowValueView.setEnabled(lowEnabled);
        highValueView.setEnabled(highEnabled);
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu) {
        getMenuInflater().inflate(R.menu.menu_data, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        assignFromViews();
        outState.putDouble(READING, reading);
        outState.putDouble(TARGET, target);
        outState.putDouble(LOW_THRESHOLD, lowThreshold);
        outState.putDouble(HIGH_THRESHOLD, highThreshold);
        outState.putBoolean(TARGET_ENABLED, targetEnabled);
        outState.putBoolean(LOW_ENABLED, lowEnabled);
        outState.putBoolean(HIGH_ENABLED, highEnabled);
        outState.putInt(DATA_INDEX, index);
        outState.putLong(DATA_ID, dataID);
        outState.putString(DATA_KEY, key);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        assignFromViews();
        Intent result = new Intent(Intent.ACTION_DEFAULT)
                .putExtra(TARGET, target)
                .putExtra(LOW_THRESHOLD, lowThreshold)
                .putExtra(HIGH_THRESHOLD, highThreshold)
                .putExtra(TARGET_ENABLED, targetEnabled)
                .putExtra(LOW_ENABLED, lowEnabled)
                .putExtra(HIGH_ENABLED, highEnabled)
                .putExtra(DATA_INDEX, index)
                .putExtra(DATA_ID, dataID)
                .putExtra(DATA_KEY, key);

        setResult (item != null && item.getItemId() == R.id.action_done
                ? RESULT_OK
                : RESULT_CANCELED
                , result);
        finish();
        return true;
    }

    @Override
    public void onPause() {
        assignFromViews();
        super.onPause();
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

    private void assignFromViews() {
        targetEnabled = targetValueView.isEnabled();
        lowEnabled = lowValueView.isEnabled();
        highEnabled = highValueView.isEnabled();
        target = parseWithDefault(targetValueView.getText().toString(), target);
        lowThreshold = parseWithDefault(lowValueView.getText().toString(), lowThreshold);
        highThreshold = parseWithDefault(highValueView.getText().toString(), highThreshold);
    }

    private static void assignText(TextView textView, double value) {
        if (!Double.isNaN(value))
            textView.setText(String.valueOf(value));
    }
}