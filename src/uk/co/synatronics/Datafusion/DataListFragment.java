package uk.co.synatronics.Datafusion;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.*;
import android.widget.*;
import java.util.ArrayList;
import java.util.List;

import static uk.co.synatronics.Datafusion.MainActivity.*;

public class DataListFragment<D extends DataSource> extends ListFragment {

    private ArrayList<D> dataList;
    private ArrayAdapter<D> dataListViewAdapter;
    private DataListChangeListener<D> listener;

    private void assignListener (Context context) {
        if (!(context instanceof DataListChangeListener))
            throw new ClassCastException("Container activity must implement the DataListChangeListener interface.");
        listener = (DataListChangeListener) context;
    }

    public static <D extends DataSource> DataListFragment<D> newInstance (List<D> sources) {
        DataListFragment<D> instance = new DataListFragment<>();
        Bundle args = new Bundle();
        args.putParcelableArrayList(DATA_LIST, new ArrayList<>(sources));
        instance.setArguments(args);
        return instance;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        assignListener(context);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        assignListener(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            dataList = savedInstanceState.getParcelableArrayList(DATA_LIST);
        }
        else {
            Bundle args = getArguments();
            if (args != null)
                dataList = args.getParcelableArrayList(DATA_LIST);
        }
        setRetainInstance(true);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) dataList = savedInstanceState.getParcelableArrayList(DATA_LIST);
        else dataList = (ArrayList<D>) listener.getDataList();
        dataListViewAdapter = new ArrayAdapter<> (getActivity(), android.R.layout.simple_list_item_1, dataList);
        setListAdapter(dataListViewAdapter);
        registerForContextMenu(getListView());
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getActivity().getMenuInflater().inflate(R.menu.contextmenu_list, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        D selected = (D) getListView().getItemAtPosition(info.position);

        switch (item.getItemId()) {
            case R.id.action_edit: {
                dataList.remove(selected);
                dataList.add(info.position, listener.onDataChanged(selected, info.position));
                return true;
            }
            case R.id.action_delete: {
                if (listener.onDataRemoved(selected, info.position)) {
                    dataList.remove(selected);
                    dataListViewAdapter.notifyDataSetChanged();
                    return true;
                }
            }
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);
        listener.onDataSelection(((D) listView.getAdapter().getItem(position)), position);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_list, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add: {
                listener.onDataAdded();
                //dataListViewAdapter.notifyDataSetChanged();
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList(DATA_LIST, dataList);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDetach() {
        listener = null;
        super.onDetach();
    }

    void setDataList (List<D> sourceList) {
        if (sourceList != null) {
            this.dataList = new ArrayList<>(sourceList);
            dataListViewAdapter.notifyDataSetChanged();
        }
    }

    ArrayList<D> getDataList() {
        return dataList;
    }
}