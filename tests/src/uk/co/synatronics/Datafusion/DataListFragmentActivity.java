package uk.co.synatronics.Datafusion;

import android.app.Activity;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.List;

class DataListFragmentActivity extends Activity implements DataListChangeListener<DoubleDataSource> {

    boolean
            onDataAdded,
            onDataAddition,
            onDataSelection,
            onDataChanged,
            onDataRemoved,
            postDataList,
            getDataList,
            getDataLimit;

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_list);
        getFragmentManager().beginTransaction().add(R.id.data_list_container, new DataListFragment<>()).commit();
    }

    @Override
    public DoubleDataSource onDataSelection (DoubleDataSource data, int index) {
        onDataSelection = true;
        System.out.println("onDataSelection("+data+','+index+')');
        return data;
    }

    @Override
    public DoubleDataSource onDataAddition (DoubleDataSource data) {
        onDataAddition = true;
        System.out.println("onDataAddition("+data+')');
        return data;
    }

    @Override
    public void onDataAdded() {
        onDataAdded = true;
        System.out.println("onDataAdded()");
    }

    @Override
    public DoubleDataSource onDataChanged (DoubleDataSource oldData, DoubleDataSource newData, int index) {
        System.out.println("onDataChanged("+oldData+','+newData+','+index+')');
        return newData;
    }

    @Override
    public DoubleDataSource onDataChanged (DoubleDataSource newData, int index) {
        onDataChanged = true;
        System.out.println("onDataChanged("+newData+','+index+')');
        return newData;
    }

    @Override
    public boolean onDataRemoved (DoubleDataSource removedData, int index) {
        onDataRemoved = true;
        return Math.random() > Math.random();
    }

    @Override
    public boolean postDataList (List<DoubleDataSource> dataList) {
        postDataList = true;
        return Math.random() > Math.random();
    }

    @Override
    public List<DoubleDataSource> getDataList() {
        getDataList = true;
        return new ArrayList<>(5);
    }

    @Override
    public int getDataLimit() {
        getDataLimit = true;
        return 5;
    }
}
