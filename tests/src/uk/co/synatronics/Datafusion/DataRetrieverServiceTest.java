package uk.co.synatronics.Datafusion;

import android.app.Activity;
import android.content.Intent;
import android.test.ServiceTestCase;
import static uk.co.synatronics.Datafusion.TestArtifacts.*;
import static uk.co.synatronics.Datafusion.MainActivity.*;
import java.util.ArrayList;

public class DataRetrieverServiceTest extends ServiceTestCase<DataRetrieverService> {
    DataRetrieverServiceTest() {
        super(DataRetrieverService.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        testDataRetrieverService = getService();
    }

    private Intent makeIntent (Class<? extends Activity> target, ArrayList<DoubleDataSource> dlist, long freq, boolean wifi, boolean notification) {
        return new Intent(testActivity, DataRetrieverService.class)
                .putExtra(DATA_HANDLER, target)
                .putExtra(DATA_RECEIVER, testActivity.callback)
                .putExtra(DATA_LIST, dlist)
                .putExtra(UPDATE_FREQUENCY, freq)
                .putExtra(UPDATE_WIFI_ONLY, wifi)
                .putExtra(ALLOW_NOTIFICATIONS, notification);
    }
}
