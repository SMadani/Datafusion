package uk.co.synatronics.Datafusion;

import android.os.Parcel;
import java.net.URL;
import java.util.*;

public abstract class NumericDataSource<N extends Number & Comparable<N>> extends DataSource<N> {

    private Map<String, N> lowThresholds, highThresholds;
    private Map<String, Boolean> lowsEnabled, highsEnabled;

    protected NumericDataSource (int numberOfVariables) {
        super(numberOfVariables);
        lowThresholds = new HashMap<>(numberOfVariables);
        highThresholds = new HashMap<>(numberOfVariables);
        lowsEnabled = new HashMap<>(numberOfVariables);
        highsEnabled = new HashMap<>(numberOfVariables);
    }

    protected NumericDataSource(NumericDataSource<N> other) {
        super(other);
        lowsEnabled = other.getLowsEnabled();
        highsEnabled = other.getHighsEnabled();
        lowThresholds = other.getLowThresholds();
        highThresholds = other.getHighThresholds();
    }

    @Override
    public void removeVariable (String key) {
        super.removeVariable(key);
        lowsEnabled.remove(key);
        highsEnabled.remove(key);
        lowThresholds.remove(key);
        highThresholds.remove(key);
    }

    protected Map<String, Boolean> getHighsEnabled() {
        return highsEnabled;
    }

    public boolean isHighEnabled (String key) {
        Boolean enabled = highsEnabled.get(key);
        return enabled != null && enabled;
    }

    public Map<String, N> getHighThresholds() {
        return highThresholds;
    }

    public N getHighThreshold(String key) {
        return highThresholds.get(key);
    }

    public void setHighEnabled (String key, boolean enabled) {
        if (key != null && highThresholds.containsKey(key))
            highsEnabled.put(key, enabled);
    }

    private void setHighThresholds(Map<String, N> highTargs) {
        if (highTargs != null)
            this.highThresholds = highTargs;
    }

    public void setHighThreshold(String key, N value) {
        setHighThreshold(key, value, isHighEnabled(key));
    }

    public void setHighThreshold(String key, N value, boolean enable) {
        if (isVariable(key) && value != null) {
            highThresholds.put(key, value);
            setHighEnabled(key, enable);
        }
    }

    protected Map<String, Boolean> getLowsEnabled() {
        return highsEnabled;
    }

    public boolean isLowEnabled (String key) {
        Boolean enabled = lowsEnabled.get(key);
        return enabled != null && enabled;
    }

    public Map<String, N> getLowThresholds() {
        return lowThresholds;
    }

    public N getLowThreshold(String key) {
        return lowThresholds.get(key);
    }

    public void setLowEnabled(String key, boolean enabled) {
        if (key != null && lowThresholds.containsKey(key))
            lowsEnabled.put(key, enabled);
    }

    private void setLowThresholds(Map<String, N> lowTargs) {
        if (lowTargs != null)
            this.lowThresholds = lowTargs;
    }

    public void setLowThreshold(String key, N value) {
        setLowThreshold(key, value, isLowEnabled(key));
    }

    public void setLowThreshold(String key, N value, boolean enable) {
        if (isVariable(key) && value != null) {
            lowThresholds.put(key, value);
            setLowEnabled(key, enable);
        }
    }

    public int compareToLow (String key) {
        return safeCompare(getReading(key), getLowThreshold(key));
    }

    public boolean exceedsLow (String key) {
        return exceedsLow(key, false);
    }

    public boolean exceedsLow (String key, boolean ignoreEnabled) {
        return (ignoreEnabled || isLowEnabled(key)) && compareToLow(key) < 0;
    }

    public Set<String> exceedsLows() {
        return exceedsLows(false);
    }

    public Set<String> exceedsLows (boolean ignoreEnabled) {
        Set<String> diffs = new HashSet<>(lowThresholds.size());
        Set<String> lowKeys = lowThresholds.keySet();
        for (String key : lowKeys) {
            if (exceedsLow(key, ignoreEnabled))
                diffs.add(key);
        }
        return diffs;
    }

    public Map<String, Integer> compareToLows() {
        return compareToLows(false);
    }

    public Map<String, Integer> compareToLows (boolean ignoreEnabled) {
        Map<String, Integer> differs = new HashMap<>(ignoreEnabled ? lowThresholds.size() : lowsEnabled.size());
        Set<String> lowKeys = lowThresholds.keySet();
        for (String key : lowKeys) {
            if (ignoreEnabled || isLowEnabled(key))
                differs.put(key, compareToLow(key));
        }
        return differs;
    }

    public int compareToHigh (String key) {
        return safeCompare(getReading(key), getHighThreshold(key));
    }

    public boolean exceedsHigh (String key) {
        return exceedsHigh(key, false);
    }

    public boolean exceedsHigh (String key, boolean ignoreEnabled) {
        return (ignoreEnabled || isHighEnabled(key)) && compareToHigh(key) > 0;
    }

    public Set<String> exceedsHighs() {
        return exceedsHighs(false);
    }

    public Set<String> exceedsHighs (boolean ignoreEnabled) {
        Set<String> diffs = new HashSet<>(highThresholds.size());
        Set<String> highKeys = highThresholds.keySet();
        for (String key : highKeys) {
            if (exceedsHigh(key, ignoreEnabled))
                diffs.add(key);
        }
        return diffs;
    }

    public Map<String, Integer> compareToHighs() {
        return compareToHighs(false);
    }

    public Map<String, Integer> compareToHighs (boolean ignoreEnabled) {
        Map<String, Integer> differs = new HashMap<>(ignoreEnabled ? highThresholds.size() : highsEnabled.size());
        Set<String> highKeys = highThresholds.keySet();
        for (String key : highKeys) {
            if (ignoreEnabled || isHighEnabled(key))
                differs.put(key, compareToHigh(key));
        }
        return differs;
    }

    @Override
    public String toString() {
        String name = getName();
        URL location = getLocation();
        Set variables = getVariables();
        String representation =  name != null && !name.isEmpty() ? name : "Unspecified name";
        representation += " \n";
        representation += location != null ? location.toString() : "Unspecified URL";
        representation += " \n Number of variables: ";
        representation += variables != null ? variables.size() : "None";
        return representation;
    }

    protected NumericDataSource (Parcel incoming) {
        super(incoming);
        lowsEnabled = boolMapFromBundle(incoming.readBundle());
        highsEnabled = boolMapFromBundle(incoming.readBundle());
        lowThresholds = mapFromBundle(incoming.readBundle());
        highThresholds = mapFromBundle(incoming.readBundle());
    }

    @Override
    public void writeToParcel(Parcel outgoing, int flags) {
        super.writeToParcel(outgoing, flags);
        outgoing.writeBundle(mapToBundle(lowsEnabled));
        outgoing.writeBundle(mapToBundle(highsEnabled));
        outgoing.writeBundle(mapToBundle(lowThresholds));
        outgoing.writeBundle(mapToBundle(highThresholds));
    }
}
