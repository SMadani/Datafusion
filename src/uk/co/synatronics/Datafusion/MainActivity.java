package uk.co.synatronics.Datafusion;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import java.util.ArrayList;
import java.util.List;
import static android.widget.Toast.*;
import static uk.co.synatronics.Datafusion.DataRetrieverService.fusionDB;

public class MainActivity extends Activity implements DataListChangeListener<DoubleDataSource> {

    static final String
        DATASOURCE = "data",
        SEPARATOR = "-",
        LIST = "list",
        INDEX = "index",
        ID = "id",
        ENABLED = "enabled",
        DATA_ID = DATASOURCE + SEPARATOR + ID,
        SELECTED_DATA = "selected" + SEPARATOR + DATASOURCE,
        EDIT_DATA = "edit"+SEPARATOR+DATASOURCE,
        ADD_DATA = "add"+SEPARATOR+DATASOURCE,
        DATA_INDEX = DATASOURCE +SEPARATOR+INDEX,
        DATA_LIST = DATASOURCE+SEPARATOR+LIST,
        DATA_KEY = DATASOURCE+SEPARATOR+"key",
        DATA_RECEIVER = DATASOURCE+SEPARATOR+"receiver",
        DATA_HANDLER = DATASOURCE+SEPARATOR+"handler",
        DATA_BUNDLE = DATASOURCE+SEPARATOR+"bundle",
        SAVED_DATA = "saved"+ SEPARATOR+DATASOURCE,
        ORIGINAL_DATA = "original"+ SEPARATOR+DATASOURCE,
        UPDATED_DATA = "updated"+SEPARATOR+DATASOURCE,
        UPDATE_FREQUENCY = "frequency",
        UPDATE_WIFI_ONLY = "update-only-on-wifi",
        ALLOW_NOTIFICATIONS = "allow-notifications",
        SMS_ENABLED = "send-text",
        PHONE_NUMBER = "phone-number",
        TARGET = "target",
        READING = "reading",
        LOW_THRESHOLD = "low"+SEPARATOR+"threshold",
        HIGH_THRESHOLD = "high"+SEPARATOR+"threshold",
        TARGET_ENABLED = TARGET+SEPARATOR+ENABLED,
        LOW_ENABLED = LOW_THRESHOLD+SEPARATOR+ENABLED,
        HIGH_ENABLED = HIGH_THRESHOLD+SEPARATOR+ENABLED,
        THRESHOLD_EXCEEDED = "Threshold exceeded!";
    static final int
        DATA_LIMIT = 5,
        CREATE_REQUEST = 1,
        EDIT_REQUEST = 2,
        UPDATE_REQUEST = 3,
        SETTINGS_REQUEST = 4;

    private boolean wideLayout = false, updateWiFi = true, allowNotifications = true, smsEnabled = false;
    private DataListFragment<DoubleDataSource> dataListFragment;
    private ArrayList<DoubleDataSource> dataList = new ArrayList<>(DATA_LIMIT);
    private SharedPreferences sharedPrefs;
    private long frequency = 1200000L;
    private String phoneNumber;
    private ResultReceiver dataResultReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPrefs = getPreferences(MODE_PRIVATE);
        final DataRetrieverService service = DataRetrieverService.getInstance();
        final FragmentManager fragManager = getFragmentManager();
        final String listFragmentTag = "main_dataListFragment";
        wideLayout = findViewById(R.id.data_detail_container) != null;
        setContentView(R.layout.activity_main);
        dataListFragment = (DataListFragment<DoubleDataSource>) fragManager.findFragmentByTag(listFragmentTag);
        if (fusionDB == null)
            fusionDB = new FusionDB(getApplicationContext());

        if (savedInstanceState != null) {
            dataList = savedInstanceState.getParcelableArrayList(DATA_LIST);
            dataResultReceiver = savedInstanceState.getParcelable(DATA_RECEIVER);
            frequency = savedInstanceState.getLong(UPDATE_FREQUENCY, frequency);
            updateWiFi = savedInstanceState.getBoolean(UPDATE_WIFI_ONLY);
            allowNotifications = savedInstanceState.getBoolean(ALLOW_NOTIFICATIONS);
            smsEnabled = savedInstanceState.getBoolean(SMS_ENABLED);
            phoneNumber = savedInstanceState.getString(PHONE_NUMBER);
        }
        else if (service != null) {
            frequency = service.getFrequency();
            updateWiFi = service.isWifiOnly();
            allowNotifications = service.canNotify();
            smsEnabled = service.isSmsEnabled();
            phoneNumber = service.getPhoneNumber();
            dataResultReceiver = service.getResultReceiver();
            try {
                dataList = (ArrayList<DoubleDataSource>) service.getDataSources();
            } catch (ClassCastException cce) {
                System.err.println("Service is using a different DataSource type: "+cce.getMessage());
            }
        }
        else {
            dataList = fusionDB.getDataSources();
            frequency = sharedPrefs.getLong(UPDATE_FREQUENCY, frequency);
            updateWiFi = sharedPrefs.getBoolean(UPDATE_WIFI_ONLY, updateWiFi);
            allowNotifications = sharedPrefs.getBoolean(ALLOW_NOTIFICATIONS, allowNotifications);
            smsEnabled = sharedPrefs.getBoolean(SMS_ENABLED, smsEnabled);
            phoneNumber = sharedPrefs.getString(PHONE_NUMBER, phoneNumber);
        }

        if (dataListFragment == null) {
            dataListFragment = DataListFragment.newInstance(dataList);
            fragManager.beginTransaction()
                    .add(R.id.data_list_container, dataListFragment, listFragmentTag)
                    .commit();
        }

        if (dataResultReceiver == null) {
            dataResultReceiver = new ResultReceiver(null) {
                @Override
                protected void onReceiveResult(int resultCode, Bundle resultData) {
                    super.onReceiveResult(resultCode, resultData);
                    if (resultCode == UPDATE_REQUEST && resultData != null) {
                        final DoubleDataSource updatedData = resultData.getParcelable(UPDATED_DATA);
                        if (updatedData != null) {
                            final long returnedID = resultData.getLong(DATA_ID, updatedData.getID());
                            if (replaceData(returnedID, updatedData, true)) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run () {
                                        dataListFragment.setDataList(dataList);
                                    }
                                });
                            }
                        }
                    }
                }
            };
        }

        if (!dataList.isEmpty() && !handleNotification(getIntent()) && savedInstanceState == null && service == null)
            informService(true);
    }

    @Override
    protected void onNewIntent (Intent intent) {
        super.onNewIntent(intent);
        handleNotification(intent);
    }

    private boolean handleNotification (Intent caller) {
        if (caller != null) {
            String command = caller.getAction();
            if (command != null && command.equals(THRESHOLD_EXCEEDED)) {
                DoubleDataSource data = caller.getParcelableExtra(DATASOURCE);
                if (data != null) {
                    startDetailActivity(data, EDIT_REQUEST, command, indexFromID(data.getID()));
                    return true;
                }
            }
        }
        return false;
    }

    private void informService (boolean start) {
        Intent instruction = new Intent(this, DataRetrieverService.class)
                .putExtra(DATA_LIST, dataList)
                .putExtra(DATA_RECEIVER, dataResultReceiver)
                .putExtra(DATA_HANDLER, this.getClass())
                .putExtra(UPDATE_FREQUENCY, frequency)
                .putExtra(UPDATE_WIFI_ONLY, updateWiFi)
                .putExtra(ALLOW_NOTIFICATIONS, allowNotifications)
                .putExtra(SMS_ENABLED, smsEnabled)
                .putExtra(PHONE_NUMBER, phoneNumber);
        stopService(instruction);
        if (start)
            startService(instruction);
    }

    private boolean replaceData(long id, DoubleDataSource newData, boolean update) {
        if (newData != null) {
            if (update)
                fusionDB.updateReadings(id, newData.getReadings());
            else
                fusionDB.editData(id, newData);

            for (int i = 0, size = dataList.size(); i < size; i++) {
                if (dataList.get(i).getID() == id) {
                    dataList.set(i, newData);
                    return true;
                }
            }
        }
        return false;
    }

    private int indexFromID (long targetID) {
        for (int i = 0, size = dataList.size(); i < size; i++) {
            DoubleDataSource data = dataList.get(i);
            if (data != null && data.getID() == targetID)
                return i;
        }
        return -1;
    }

    private void startDetailActivity (DoubleDataSource data, int request, String action, int index) {
        informService(false);
        if (wideLayout)
            getFragmentManager().beginTransaction()
                .replace(R.id.data_detail_container, DataDetailFragment.newInstance(data, index))
                .commit();
        else
            startActivityForResult(new Intent(this, DataDetailActivity.class)
                    .setAction(action)
                    .putExtra(SELECTED_DATA, data)
                    .putExtra(DATA_INDEX, index)
                    .putExtra(DATA_ID, data != null ? data.getID() : -1), request);
    }

    @Override
    public DoubleDataSource onDataSelection(DoubleDataSource data, int index) {
        startDetailActivity(data, EDIT_REQUEST, EDIT_DATA, index);
        return data;
    }

    @Override
    public DoubleDataSource onDataAddition (DoubleDataSource data) {
        data = fusionDB.createData(data);
        dataList.add(data);
        informService(true);
        return data;
    }

    @Override
    public void onDataAdded() {
        if (dataList.size() < DATA_LIMIT)
            startDetailActivity(new DoubleDataSource(), CREATE_REQUEST, ADD_DATA, dataList.size());
        else
            makeText(this, "You can only have up to " + DATA_LIMIT + " data sources.", LENGTH_LONG).show();
    }

    @Override
    public DoubleDataSource onDataChanged(DoubleDataSource oldData, DoubleDataSource newData, int index) {
        if (newData != null) {
            long id = oldData != null ? oldData.getID() : newData.getID();
            if (replaceData(id, newData, false))
                informService(true);
            return newData;
        }
        return index >= 0 && index < dataList.size() ? dataList.get(index) : null;
    }

    @Override
    public DoubleDataSource onDataChanged(DoubleDataSource oldData, int index) {
        return onDataSelection(oldData, index);
    }

    @Override
    public boolean onDataRemoved(DoubleDataSource data, int index) {
        try {
            fusionDB.deleteData(data.getID());
            dataList.remove(data);
            informService(!dataList.isEmpty());
            return true;
        } catch (Exception ex) {
            Log.e("onDataRemoved", ex.getMessage());
            return false;
        }
    }

    @Override
    public List<DoubleDataSource> getDataList() {
        return dataList;
    }

    @Override
    public boolean postDataList(List<DoubleDataSource> sourceList) {
        if (sourceList != null) {
            this.dataList = (ArrayList<DoubleDataSource>) sourceList;
            informService(!dataList.isEmpty());
            return true;
        }
        return false;
    }

    @Override
    public int getDataLimit() {
        return DATA_LIMIT;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (resultCode == RESULT_OK && intent != null) {
            switch (requestCode) {
                case SETTINGS_REQUEST: {
                    frequency = intent.getLongExtra(UPDATE_FREQUENCY, frequency);
                    updateWiFi = intent.getBooleanExtra(UPDATE_WIFI_ONLY, updateWiFi);
                    allowNotifications = intent.getBooleanExtra(ALLOW_NOTIFICATIONS, allowNotifications);
                    smsEnabled = intent.getBooleanExtra(SMS_ENABLED, smsEnabled);
                    phoneNumber = intent.getStringExtra(PHONE_NUMBER);
                    informService(true);
                    break;
                }

                case CREATE_REQUEST: {
                    DoubleDataSource returnedData = (DoubleDataSource) intent.getExtras().get(SAVED_DATA);
                    if (returnedData != null && returnedData.getName() != null && !returnedData.getName().trim().isEmpty()) {
                        onDataAddition(returnedData);
                        dataListFragment.setDataList(dataList);
                    }
                    break;
                }

                case EDIT_REQUEST: {
                    DoubleDataSource returnedData = (DoubleDataSource) intent.getExtras().get(SAVED_DATA);
                    DoubleDataSource oldData = intent.getParcelableExtra(ORIGINAL_DATA);
                    int index = intent.getIntExtra(DATA_INDEX, oldData != null ? indexFromID(oldData.getID()) : -1);
                    if (index >= 0) {
                        if (returnedData != null && returnedData.getName() != null && !returnedData.getName().trim().isEmpty()) {
                            onDataChanged(oldData, returnedData, index);
                            dataListFragment.setDataList(dataList);
                        }
                        break;
                    }
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings: {
                informService(false);
                startActivityForResult(
                    new Intent(this, SettingsActivity.class)
                        .putExtra(UPDATE_FREQUENCY, frequency)
                        .putExtra(UPDATE_WIFI_ONLY, updateWiFi)
                        .putExtra(ALLOW_NOTIFICATIONS, allowNotifications)
                        .putExtra(SMS_ENABLED, smsEnabled)
                        .putExtra(PHONE_NUMBER, phoneNumber)
                        , SETTINGS_REQUEST);
                return true;
            }
            case R.id.action_info: {
                //makeText(this, "Operating instructions will be released in the next version.", LENGTH_SHORT).show();
                startActivity(new Intent(this, HelpActivity.class));
                return true;
            }
            default: return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList(DATA_LIST, dataList);
        outState.putParcelable(DATA_RECEIVER, dataResultReceiver);
        outState.putLong(UPDATE_FREQUENCY, frequency);
        outState.putBoolean(UPDATE_WIFI_ONLY, updateWiFi);
        outState.putBoolean(ALLOW_NOTIFICATIONS, allowNotifications);
        outState.putBoolean(SMS_ENABLED, smsEnabled);
        outState.putString(PHONE_NUMBER, phoneNumber);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStop() {
        sharedPrefs.edit()
                .putLong(UPDATE_FREQUENCY, frequency)
                .putBoolean(UPDATE_WIFI_ONLY, updateWiFi)
                .putBoolean(ALLOW_NOTIFICATIONS, allowNotifications)
                .putBoolean(SMS_ENABLED, smsEnabled)
                .putString(PHONE_NUMBER, phoneNumber)
                .apply();
        super.onStop();
    }
}
