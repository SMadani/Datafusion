package uk.co.synatronics.Datafusion;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

abstract class TestArtifacts {

    static class TestActivity extends Activity {
        int reqCode, resCode;
        Intent returnedIntent;
        boolean hasResult;
        Bundle resData;

        ResultReceiver callback = new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult (int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);
                hasResult = true;
                resCode = resultCode;
                resData = resultData;
            }
        };

        @Override
        protected void onActivityResult (int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            hasResult = true;
            this.reqCode = requestCode;
            this.resCode = resultCode;
            this.returnedIntent = data;
        }
    }

    static URL testLocationURL;
    static String testName, testLocationString, testRawXML, testRawCSV, testRawTXT;
    static Map<String, Double> testValueMap, testValueMapValid;
    static Map<String, Boolean> testEnabledMap;
    static ArrayList<DoubleDataSource> testDataList;
    static TestActivity testActivity;
    static MainActivity testMainActivity;
    static SettingsActivity testSettingsActivity;
    static DataDetailActivity testDetailActivity;
    static DataMapActivity testMapActivity;
    static DataListFragmentActivity testListFragmentActivity;
    static DataDetailFragmentActivity testDetailFragmentActivity;
    static DataListFragment<DoubleDataSource> testListFragment;
    static DataDetailFragment testDetailFragment;
    static DataRetrieverService testDataRetrieverService;
    static FusionDB testFusionDB;

    static {
        try {
            testActivity = new TestActivity();
            testMainActivity = new MainActivity();
            testSettingsActivity = new SettingsActivity();
            testDetailActivity = new DataDetailActivity();
            testMapActivity = new DataMapActivity();
            testListFragmentActivity = new DataListFragmentActivity();
            testDetailFragmentActivity = new DataDetailFragmentActivity();
            testListFragment = new DataListFragment<>();
            testDetailFragment = new DataDetailFragment();
            testDataRetrieverService = new DataRetrieverService();

            testValueMap = testValueMapValid = new HashMap<>(8);
            testValueMap.put("", 1d);
            testValueMap.put("Variable", 7.3);
            testValueMap.put("Variable0", 0d);
            testValueMap.put("Variable1", 0.5);
            testValueMap.put("Variable3", Double.MIN_VALUE);
            testValueMap.put("Variable4", Double.NaN);
            testValueMap.put("Variable5", Double.MAX_VALUE);
            testValueMap.put("Variable 6", -1024.33333);

            testValueMapValid.put("Variable1", 5.2);
            testValueMapValid.put("Variable2", -89.3);
            testValueMapValid.put("Variable3", 0d);
            testValueMapValid.put("Variable4", 16384.99);
            testValueMapValid.put("Variable5", 0.0000003);
            testValueMapValid.put("Variable6", -0.0000003);

            testEnabledMap = new HashMap<>(6);
            testEnabledMap.put("Variable", null);
            testEnabledMap.put("Variable1", false);
            testEnabledMap.put("Variable2", true);
            testEnabledMap.put("Variable3", null);
            testEnabledMap.put("Variable 4", true);
            testEnabledMap.put("", null);

            testName = "testName";
            testLocationString = "http://"+InetAddress.getLocalHost().getHostName();
            testLocationURL = new URL(testLocationString);
            testLocationString = "http://192.168.0.15/data.";

            DoubleDataSource td0, td1, td2, td3, td4, td5;
            td0 = td1 = td2 = td3 = td4 = td5 = new DoubleDataSource();
            td0.setID(0);
            td1.setID(1);
            td2.setID(2);
            td3.setID(3);
            td4.setID(-1);
            td5.setID(5);
            td0.setLocation(testLocationURL);
            td1.setLocation(testLocationString+DataSource.FileType.TXT.name());
            td2.setLocation(testLocationString+DataSource.FileType.CSV.name());
            td3.setLocation(testLocationString+DataSource.FileType.XML.name());
            td1.setName("TestData1");
            td2.setName("TestData2");
            td3.setName("TestData3");
            td4.setName(null);
            td5.setName("Test Data #5");
            td1.setImportance(DataSource.Importance.LOW);
            td2.setImportance(DataSource.Importance.MEDIUM);
            td3.setImportance(DataSource.Importance.HIGH);
            td2.setUpdatable(false);
            td2.setReading("Variable1", 4.2);
            td2.setReading("Variable2", -0.3);
            td2.setReading("Variable3", 500d);
            td2.setReading("Variable 4", 220.0009);
            td2.setReading("Variable5", Double.NaN);
            td2.setReading("", 0.0);

            for (String key : testValueMap.keySet()) {
                td1.setReading(key, testValueMap.get(key));
                td3.setReading(key, testValueMap.get(key));
                td3.setTarget(key, testValueMap.get(key));
            }

            for (String key: testValueMapValid.keySet()) {
                td5.setReading(key, testValueMapValid.get(key));
                td5.setLowThreshold(key, testValueMapValid.get(key)+Math.random());
                td5.setHighThreshold(key, testValueMapValid.get(key)-Math.random());
            }

            testDataList = new ArrayList<>(6);
            testDataList.add(td0);
            testDataList.add(td1);
            testDataList.add(td2);
            testDataList.add(td3);
            testDataList.add(td4);
            testDataList.add(td5);

            testRawTXT = "Variable0:\"0\"\n"+
                    "Variable1:\"30.8\"\n"+
                    "Variable2:\"-10.25\"\n"+
                    "Variable3:\"53\"\n"+
                    "Variable4:\"-0.0\"\n"+
                    "Variable5:\"7d\"\n"+
                    "Variable-6:\"-1638.003\"\n"+
                    "Variable:\"NaN\"\n"+
                    "Variable7:\"1\"\n"+
                    "Variable8.0:\"0.000000001\"";

            testRawCSV = "Variable0,Variable1,Variable2,Variable3,Variable4,Variable5,Variable-6,Variable,Variable7,Variable8.0\n"+
                    "0,30.8,-10.25,53,-0.0,7d,-1638.003,NaN,1,0.000000001";

            testRawXML = "<Variable0>0</Variable0>\n"+
                    "<Variable1>30.8</Variable1>\n"+
                    "<Variable2>-10.25</Variable2>\n"+
                    "<Variable3>53</Variable3>\n"+
                    "<Variable4>-0.0</Variable4>\n"+
                    "<Variable5>7d</Variable5>\n"+
                    "<Variable-6>-1638.003</Variable-6>\n"+
                    "<Variable>NaN</Variable>\n"+
                    "<Variable7>1</Variable7>\n"+
                    "<Variable8.0>0.000000001</Variable8.0>";

        }
        catch (Exception e) {
            System.err.println("Failed to set up test environment: "+e.getMessage());
        }
    }
}
