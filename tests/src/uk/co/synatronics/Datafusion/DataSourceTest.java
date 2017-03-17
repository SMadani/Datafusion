package uk.co.synatronics.Datafusion;

import android.test.suitebuilder.annotation.SmallTest;
import junit.framework.TestCase;
import static uk.co.synatronics.Datafusion.DoubleDataSource.*;
import static uk.co.synatronics.Datafusion.TestArtifacts.*;
import java.util.HashSet;
import java.util.Set;

public class DataSourceTest extends TestCase {

    DoubleDataSource testSource;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        testSource = new DoubleDataSource();
        testSource.setReading("Var1", 3.2);
        testSource.setReading("Var2", 90d);
        testSource.setReading("Var3", 67.0);
        testSource.setReading("Var4", -9001d);
        testSource.setReading("Var5", 0d);
        testSource.setReading("Var6", 1.00000000000001);
        testSource.setReading("Var7", -15.6);
    }

    @SmallTest
    public void testExceedLows() {
        testSource.setLowThreshold("Var1", 3.2);
        testSource.setLowThreshold("Var2", 91d);
        testSource.setLowThreshold("Var3", 66.5);
        testSource.setLowThreshold("Var4", -9001.36);
        testSource.setLowThreshold("Var5", -0d);
        testSource.setLowThreshold("Var6", 1d);
        testSource.setLowThreshold("Var7", 15.6);
        Set<String> expected = new HashSet<>(2);
        expected.add("Var2");
        expected.add("Var7");
        Set<String> actual = testSource.exceedsLows(true);
        assertEquals(expected, actual);
    }
}
