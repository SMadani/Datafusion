package uk.co.synatronics.Datafusion;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import static uk.co.synatronics.Datafusion.MainActivity.*;

public class DataDetailActivity extends Activity {
    private DoubleDataSource data;
    private int index = 0;
    private long dataID = -1;
    private DataDetailFragment detailFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_detail);
        Intent caller = getIntent();
        if (caller != null) {
            Bundle bundle = caller.getBundleExtra(DATA_BUNDLE);
            if (bundle == null || bundle.isEmpty()) {
                data = caller.getParcelableExtra(DATASOURCE);
                if (data == null) data = caller.getParcelableExtra(SELECTED_DATA);
                index = caller.getIntExtra(DATA_INDEX, index);
                dataID = caller.getLongExtra(DATA_ID, data != null ? data.getID() : dataID);
            }
            else {
                data = bundle.getParcelable(DATASOURCE);
                if (data == null) data = bundle.getParcelable(SELECTED_DATA);
                index = bundle.getInt(DATA_INDEX, index);
                dataID = caller.getLongExtra(DATA_ID, data != null ? data.getID() : dataID);
            }
        }

        if (savedInstanceState == null) {
            detailFragment = DataDetailFragment.newInstance(data, index, dataID);
            getFragmentManager().beginTransaction().add(R.id.data_detail_container, detailFragment).commit();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(DATASOURCE, data);
        outState.putInt(DATA_INDEX, index);
        outState.putLong(DATA_ID, dataID);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_data, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        data = detailFragment.getData();
        Intent result = new Intent("Done")
                .putExtra (SAVED_DATA, detailFragment.getData())
                .putExtra(DATA_INDEX, index)
                .putExtra(DATA_ID, dataID);

        switch (item.getItemId()) {
            case R.id.action_done: {
                setResult(RESULT_OK, result);
                break;
            }
            default: {
                setResult(RESULT_CANCELED, result);
                break;
            }
        }
        finish();
        return true;
    }
}