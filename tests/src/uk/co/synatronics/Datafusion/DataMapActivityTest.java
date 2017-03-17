package uk.co.synatronics.Datafusion;

import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import static uk.co.synatronics.Datafusion.TestArtifacts.*;

public class DataMapActivityTest extends ActivityInstrumentationTestCase2<DataMapActivity> {
    DataMapActivityTest() {
        super(DataMapActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        testMapActivity = getActivity();
    }

    @UiThreadTest
    public void testOnOptionsItemSelected() {
        //
    }
}
