package uk.co.synatronics.Datafusion;

import android.os.Parcelable;
import java.io.Serializable;
import java.util.List;

public interface DataListChangeListener<D extends Parcelable> {

    D onDataSelection(D data, int index);

    D onDataAddition (D data);

    void onDataAdded();

    D onDataChanged(D oldData, D newData, int index);

    D onDataChanged(D newData, int index);

    boolean onDataRemoved(D removedData, int index);

    boolean postDataList(List<D> dataList);

    List<D> getDataList();

    int getDataLimit();
}
