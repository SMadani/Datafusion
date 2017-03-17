package uk.co.synatronics.Datafusion;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.net.URL;
import java.util.ArrayList;
import static uk.co.synatronics.Datafusion.MainActivity.*;

public class DataDetailFragment extends Fragment {

    private DoubleDataSource dataSource = null;
    private ArrayList<String> keyList = null;
    private long dataID = -1;
    private int index = 0;
    private EditText editURL, editName;
    private ArrayAdapter keyAdapter = null;

    static DataDetailFragment newInstance(DoubleDataSource dataSource, int index) {
        return newInstance(dataSource, index, dataSource != null ? dataSource.getID() : -1);
    }

    static DataDetailFragment newInstance(DoubleDataSource dataSource, int index, long id) {
        Bundle args = new Bundle();
        args.putParcelable(DATASOURCE, dataSource);
        args.putLong(DATA_ID, id);
        args.putInt(DATA_INDEX, index);
        DataDetailFragment fragment = new DataDetailFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Prefer savedInstanceState, fall back to Activity's arguments if null
        Bundle arguments = savedInstanceState != null ? savedInstanceState : getArguments();
        if (arguments != null) {
            dataSource = arguments.getParcelable(DATASOURCE);
            dataID = arguments.getLong(DATA_ID, dataSource != null ? dataSource.getID() : 0);
            index = arguments.getInt(DATA_INDEX, index);
            keyList = dataSource != null ?
                    new ArrayList<>(dataSource.getVariables())
                    : new ArrayList<String>();
        }
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        keyAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, keyList);
        return inflater.inflate(R.layout.fragment_data_detail, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Activity parent = getActivity();
        editURL = (EditText) parent.findViewById(R.id.editURL);
        editName = (EditText) parent.findViewById(R.id.editName);
        final ListView keyListView = (ListView) parent.findViewById(R.id.keyListView);
        keyListView.setAdapter(keyAdapter);
        keyListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                if (dataSource != null) {
                    index = position;
                    String key = keyList.get(position);
                    Double reading, target, lowThreshold, highThreshold;
                    reading = dataSource.getReading(key);
                    target = dataSource.getTarget(key);
                    lowThreshold = dataSource.getLowThreshold(key);
                    highThreshold = dataSource.getHighThreshold(key);

                    Intent editIntent = new Intent(getActivity(), DataMapActivity.class)
                            .setAction("edit"+SEPARATOR+"map")
                            .putExtra(SELECTED_DATA, keyList.get(position))
                            .putExtra(DATA_INDEX, position)
                            .putExtra(DATA_KEY, key)
                            .putExtra(READING, reading)
                            .putExtra(TARGET, target)
                            .putExtra(LOW_THRESHOLD, lowThreshold)
                            .putExtra(HIGH_THRESHOLD, highThreshold)
                            .putExtra(TARGET_ENABLED, dataSource.isTargetEnabled(key))
                            .putExtra(LOW_ENABLED, dataSource.isLowEnabled(key))
                            .putExtra(HIGH_ENABLED, dataSource.isHighEnabled(key))
                            .putExtra(DATA_ID, dataID);
                    startActivityForResult(editIntent, EDIT_REQUEST);
                }
            }
        });
        if (dataSource != null) {
            URL location = dataSource.getLocation();
            String name = dataSource.getName();
            if (location != null)
                editURL.setText(location.toString());
            if (name != null)
                editName.setText(name);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent returnedData) {
        super.onActivityResult(requestCode, resultCode, returnedData);
        if (resultCode == Activity.RESULT_OK && returnedData != null) {
            index = returnedData.getIntExtra(DATA_INDEX, index);
            double target, lowThreshold, highThreshold;
            boolean targetEnabled, lowEnabled, highEnabled;
            String key = returnedData.getStringExtra(SELECTED_DATA);
            if (key == null || key.isEmpty())
                key = keyList.get(index);

            targetEnabled = returnedData.getBooleanExtra(TARGET_ENABLED, dataSource.isTargetEnabled(key));
            lowEnabled = returnedData.getBooleanExtra(LOW_ENABLED, dataSource.isLowEnabled(key));
            highEnabled = returnedData.getBooleanExtra(HIGH_ENABLED, dataSource.isHighEnabled(key));
            target = returnedData.getDoubleExtra(TARGET, dataSource.getTarget(key));
            lowThreshold = returnedData.getDoubleExtra(LOW_THRESHOLD, dataSource.getLowThreshold(key));
            highThreshold = returnedData.getDoubleExtra(HIGH_THRESHOLD, dataSource.getHighThreshold(key));

            if (!Double.isNaN(target))
                dataSource.setTarget(key, target, targetEnabled);
            if (!Double.isNaN(lowThreshold))
                dataSource.setLowThreshold(key, lowThreshold, lowEnabled);
            if (!Double.isNaN(highThreshold))
                dataSource.setHighThreshold(key, highThreshold, highEnabled);

            keyAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onSaveInstanceState (Bundle outState) {
        outState.putParcelable(DATASOURCE, dataSource);
        outState.putLong(DATA_ID, dataID);
        outState.putInt(DATA_INDEX, index);
        super.onSaveInstanceState(outState);
    }

    protected DoubleDataSource getData() {
        dataSource.setName(editName.getText().toString());
        String location = editURL.getText().toString();
        if (location.startsWith("CURRENCY:") && location.length() >= 16) {
            double amount = 1;
            if (location.length() >= 17) try {
                amount = Double.parseDouble(location.substring(17));
            }
            catch (NumberFormatException NaN) {
                System.err.println("Couldn't parse currency amount \""+location.substring(18)+'"');
            }
            dataSource.setLocation(CurrencyConverter.getCurrencyConverterURL(
                    location.substring(9, 12), location.substring(13, 16), amount));
        }
        else if (!dataSource.setLocation(location)) {
            Toast.makeText(getActivity(), "Invalid URL. Please provide the full address, including the protocol (e.g. \"http://example.com/myData.txt\").", Toast.LENGTH_LONG).show();
        }
        return dataSource;
    }
}