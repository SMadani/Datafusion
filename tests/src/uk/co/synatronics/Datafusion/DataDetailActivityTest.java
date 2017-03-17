package uk.co.synatronics.Datafusion;

import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import static uk.co.synatronics.Datafusion.TestArtifacts.*;

class DataDetailActivityTest extends ActivityInstrumentationTestCase2<DataDetailActivity> {
    DataDetailActivityTest() {
        super(DataDetailActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        testDetailActivity = getActivity();
    }

    @UiThreadTest
    public void testOnOptionsItemSelected() {
        //
    }
}
