package uk.co.synatronics.Datafusion;

import android.os.Parcel;
import static java.lang.Double.*;
import java.util.Collections;
import java.util.Map;

public class DoubleDataSource extends NumericDataSource<Double> {

    public DoubleDataSource() {
        super(6);
    }

    public DoubleDataSource (int numberOfVariables) {
        super(numberOfVariables);
    }

    @Override
    public Double getLowThreshold (String key) {
        Double low = super.getLowThreshold(key);
        return low != null ? low : NaN;
    }

    @Override
    public Double getHighThreshold (String key) {
        Double high = super.getHighThreshold(key);
        return high != null ? high : NaN;
    }

    @Override
    public Double getTarget (String key) {
        Double target = super.getTarget(key);
        return target != null ? target : NaN;
    }

    @Override
    protected Double parseValue(String value) {
        try {
            return parseDouble(value);
        } catch (NumberFormatException nfe) {
            return NaN;
        }
    }

    protected DoubleDataSource(Parcel other) {
        super(other);
    }

    public static final Creator<DoubleDataSource> CREATOR = new Creator<DoubleDataSource>() {
        @Override
        public DoubleDataSource createFromParcel(Parcel incoming) {
            return new DoubleDataSource(incoming);
        }

        @Override
        public DoubleDataSource[] newArray(int size) {
            return new DoubleDataSource[size];
        }
    };

    public static double parseWithDefault(String value, double fallback) {
        try {
            return parseDouble(value);
        } catch (NumberFormatException nfe) {
            return fallback;
        }
    }

    public static double parseWithDefault(String value) {
        return parseWithDefault(value, NaN);
    }
}
