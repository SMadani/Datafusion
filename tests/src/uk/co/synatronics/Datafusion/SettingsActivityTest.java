package uk.co.synatronics.Datafusion;

import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.widget.EditText;
import static uk.co.synatronics.Datafusion.TestArtifacts.*;
import static uk.co.synatronics.Datafusion.MainActivity.*;

public class SettingsActivityTest extends ActivityInstrumentationTestCase2<SettingsActivity> {
    SettingsActivityTest() {
        super(SettingsActivity.class);
    }

    private void startWithInput(int requestCode) {
        startWithInput(600000, true, false, requestCode);
    }

    private void startWithInput(long frequency, boolean wifi, boolean notifications, int requestCode) {
        testActivity.startActivityForResult(
            new Intent(testActivity, SettingsActivity.class)
                .putExtra(UPDATE_FREQUENCY, frequency)
                .putExtra(UPDATE_WIFI_ONLY, wifi)
                .putExtra(ALLOW_NOTIFICATIONS, notifications), requestCode);
    }

    private boolean testUIFrequency (int testReq, long frequency, boolean equal) {
        startWithInput(testReq);
        EditText freqView = (EditText) testSettingsActivity.findViewById(R.id.frequencyValueView);
        freqView.setText(String.valueOf(frequency));
        testSettingsActivity.findViewById(R.id.action_done).performClick();
        long savedFreq = testActivity.returnedIntent.getLongExtra(UPDATE_FREQUENCY, frequency);

        return testActivity.hasResult &&
                testActivity.reqCode == testReq &&
                testActivity.resCode == RESULT_OK &&
                equal ?
                    savedFreq == frequency :
                    savedFreq != frequency;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        testSettingsActivity = getActivity();
    }

    @UiThreadTest
    public void testUIZeroFrequency() {
        assertTrue(testUIFrequency(1, 0, false));
    }

    @UiThreadTest
    public void testUI45secsFrequency() {
        assertTrue(testUIFrequency(2, 45, true));
    }
}
