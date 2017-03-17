package uk.co.synatronics.Datafusion;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import static uk.co.synatronics.Datafusion.CurrencyConverter.*;
import static uk.co.synatronics.Datafusion.FileUtils.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class DataSource<D extends Serializable & Comparable<D>> implements Parcelable, Comparable<DataSource> {

    public enum FileType {
        XML,
        CSV,
        TXT,
        CURRENCY
    }

    public enum Importance {
        LOW,
        DEFAULT,
        MEDIUM,
        HIGH
    }

    private Map<String, D> readings, targets;
    private Map<String, Boolean> targetsEnabled;
    private URL location;
    private long _id;
    private String name;
    private boolean updatable;
    private Importance importance;

    protected DataSource(DataSource<D> other) {
        _id = other.getID();
        name = other.getName();
        updatable = other.isUpdatable();
        location = other.getLocation();
        importance = other.getImportance();
        targetsEnabled = other.getTargetsEnabled();
        readings = other.getReadings();
        targets = other.getTargets();
    }

    protected DataSource (int numberOfVariables) {
        if (numberOfVariables < 0)
            numberOfVariables = 0xE;
        _id = 0;
        name = "";
        updatable = true;
        location = null;
        importance = Importance.DEFAULT;
        readings = new HashMap<>(numberOfVariables);
        targets = new HashMap<>(numberOfVariables);
        targetsEnabled = new HashMap<>(numberOfVariables);
    }

    public DataSource() {
        this(12);
    }

    public long getID() {
        return _id;
    }

    public void setID(long id) {
        if (id >= 0)
            this._id = id;
    }

    public boolean isUpdatable() {
        return updatable;
    }

    public void setUpdatable(boolean enabled) {
        this.updatable = enabled;
    }

    public Importance getImportance() {
        return importance;
    }

    public void setImportance(Importance priority) {
        if (priority != null)
            this.importance = priority;
    }

    public boolean isVariable (String key) {
        return getVariables().contains(key);
    }

    public int getNumberOfVariables() {
        return getVariables().size();
    }

    public Set<String> getVariables() {
        return readings != null ? readings.keySet() : new HashSet<String>(0);
    }

    public void removeVariable (String key) {
        readings.remove(key);
        targets.remove(key);
        targetsEnabled.remove(key);
    }

    public Map<String, D> getReadings() {
        return readings;
    }

    public D getReading (String key) {
        return readings.get(key);
    }

    private void setReadings(Map<String, D> values) {
        if (values != null)
            this.readings = values;
    }

    public void setReading(String key, D value) {
        if (value != null && key != null && !key.isEmpty())
            readings.put(key, value);
    }

    public URL getLocation() {
        return location;
    }

    public void setLocation(URL path) {
        if (path != null) {
            this.location = path;
            if (name == null || name.isEmpty())
                setName(inferFileName(location));
        }
    }

    public boolean setLocation(String url) {
        try {
            setLocation(new URL(url));
            return true;
        }
        catch (MalformedURLException mue) {
            System.err.println("Could not parse URL \""+url+"\": "+mue.getMessage());
            return false;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String title) {
        if (title != null && !title.isEmpty())
            this.name = title;
    }

    protected Map<String, Boolean> getTargetsEnabled() {
        return targetsEnabled;
    }

    public boolean isTargetEnabled (String key) {
        Boolean enabled = targetsEnabled.get(key);
        return enabled != null && enabled;
    }

    public Map<String, D> getTargets() {
        return targets;
    }

    public D getTarget (String key) {
        return targets.get(key);
    }

    public void setTargetEnabled (String key, boolean enabled) {
        if (key != null && targets.containsKey(key))
            targetsEnabled.put(key, enabled);
    }

    private void setTargets(Map<String, D> exact) {
        if (exact != null)
            this.targets = exact;
    }

    public void setTarget(String key, D value) {
        setTarget(key, value, isTargetEnabled(key));
    }

    public void setTarget(String key, D value, boolean enable) {
        if (isVariable(key) && value != null) {
            targets.put(key, value);
            setTargetEnabled(key, enable);
        }
    }

    @Override
    public String toString() {
        return getID() + '\n'
                + getName() + '\n'
                + getLocation() + '\n'
                + getImportance();
    }

    protected int safeCompare (D lhs, D rhs) {
        try {
            return lhs.compareTo(rhs);
        } catch (NullPointerException npe) {
            System.err.println("Could not perform comparison: "+npe.getMessage());
            return 0;
        }
    }

    public int compareToTarget (String key) {
        return safeCompare(getReading(key), getTarget(key));
    }

    public boolean differsFromTarget (String key) {
        return differsFromTarget(key, false);
    }

    public boolean differsFromTarget (String key, boolean ignoreEnabled) {
        return (ignoreEnabled || isTargetEnabled(key)) && compareToTarget(key) != 0;
    }

    public Set<String> differsFromTargets() {
        return differsFromTargets(false);
    }

    public Set<String> differsFromTargets (boolean ignoreEnabled) {
        Set<String> diffs = new HashSet<>(targets.size());
        Set<String> targKeys = targets.keySet();
        for (String key : targKeys) {
            if (differsFromTarget(key, ignoreEnabled))
                diffs.add(key);
        }
        return diffs;
    }

    public Map<String, Integer> compareToTargets() {
        return compareToTargets(false);
    }

    public Map<String, Integer> compareToTargets (boolean ignoreEnabled) {
        Map<String, Integer> differs = new HashMap<>(ignoreEnabled ? targets.size() : targetsEnabled.size());
        Set<String> targKeys = targets.keySet();
        for (String key : targKeys) {
            if (ignoreEnabled || isTargetEnabled(key))
                differs.put(key, compareToTarget(key));
        }
        return differs;
    }

    protected D parseValue (String value) throws IllegalFormatException {
        return (D) value;
    }

    protected static FileType inferType (URL url) {
        if (url != null) {
            String ext = getExtension(url.toString()).toUpperCase();
            try {
                return FileType.valueOf(ext);
            }
            catch (IllegalArgumentException iae) {
                System.err.println("Could not infer FileType for \""+url+"\": "+iae.getMessage());
            }
        }
        return FileType.CURRENCY;
    }

    public FileType inferType() {
        return inferType(location);
    }

    public boolean assignFromString (String source, FileType extension) {
        if (source != null && !source.isEmpty()) {
            Map<String, D> converted = null;
            if (extension != null) {
                switch (extension) {
                    case XML:
                        converted = mapFromXML(docFromString(source));
                        break;
                    case CSV:
                        converted = mapFromCSV(source);
                        break;
                    default:
                        converted = mapFromKVP(source);
                        break;
                }
            }
            if (converted != null && !converted.isEmpty()) {
                setReadings(converted);
                return true;
            }
        }
        return false;
    }

    public boolean assignFromFile (File source) {
        String fileName = source != null ? source.getName() : "";
        if (!fileName.isEmpty()) {
            String extension = getExtension(fileName);
            try {
                return assignFromFile(source, FileType.valueOf(extension));
            }
            catch (IllegalArgumentException iae) {
                System.err.println("Unsupported file type \""+extension+"\": "+iae.getMessage());
            }
        }
        return false;
    }

    public boolean assignFromFile (File source, FileType extension) {
        return assignFromString(readFile(source), extension);
    }

    public boolean assignFromURL (String url, FileType extension) {
        try {
            return assignFromURL(new URL(url), extension);
        }
        catch (MalformedURLException mue) {
            System.err.println("Could not parse URL \""+url+"\": "+mue.getMessage());
            return false;
        }
    }

    public boolean assignFromURL() {
        return assignFromURL(location);
    }

    protected boolean assignFromURL (URL source) {
        return assignFromURL(source, inferType(source));
    }

    protected boolean assignFromURL (URL source, FileType extension) {
        if (extension.equals(FileType.CURRENCY)) try {
            CurrencyConverter currencyConverter = new CurrencyConverter(source);
            Double value = currencyConverter.convert();
            this.readings = Collections.singletonMap(currencyConverter.from+'/'+currencyConverter.to+':'+currencyConverter.amount, (D) value);
            return true;
        }
        catch (Exception ex) {
            return false;
        }
        else
            return assignFromString(readURL(source), extension);
    }

    protected Map<String, D> mapFromKVP(String kvp) {
        Map<String, D> transMap = null;
        if (kvp != null && !kvp.isEmpty()) {
            String[] pairs = kvp.split("\"(\\s+)?\\s+");
            int numberOfPairs = pairs.length;
            transMap = new HashMap<>(numberOfPairs+1);

            if (numberOfPairs > 0) {
                for (String pair : pairs) {
                    String key, value;
                    try {
                        key = pair.substring(0, pair.indexOf(":\""));
                        value = pair.substring(pair.indexOf(key)+key.length()+2);
                        transMap.put(key, parseValue(value));
                    }
                    catch (IllegalFormatException | IndexOutOfBoundsException ifb) {
                        System.err.println("Incorrect syntax provided for mapFromKVP: "+ifb.getMessage());
                    }
                }
            }
        }
        return transMap;
    }

    public static Map<String, String> transformCSV (Map<String, List<String>> csvMap) {
        Map<String, String> transMap = new HashMap<>(csvMap.size());
        Set<String> keys = csvMap.keySet();
        for (String key : keys) {
            String transValue = "";
            List<String> values = csvMap.get(key);
            for (String value : values)
                transValue += value + ',';
            int lastComma = transValue.lastIndexOf(',');
            if (lastComma > 0)
                transValue = transValue.substring(0, lastComma);
            transMap.put(key, transValue);
        }
        return transMap;
    }

    public Map<String, List<D>> csvAsMap (String csv) {
        try {
            String[] records = csv.split("\n");
            String[] headers = records[0].split(",");
            Map<String, List<D>> table = new HashMap<>(headers.length);

            for (int h = 0; h < headers.length; h++) {
                List<D> values = new ArrayList<>(records.length);
                for (int r = 1; r < records.length; r++) {
                    String[] cells = records[r].split(",");
                    values.add(parseValue(cells[h]));
                }
                table.put(headers[h], values);
            }
            return table;
        }
        catch (NullPointerException | ArrayIndexOutOfBoundsException nae) {
            System.err.println("Incorrect syntax provided for csvAsMap: " + nae.getMessage());
            return null;
        }
    }

    public Map<String, D> mapFromListElement (Map<String, List<D>> csvMap, int item) {
        if (csvMap != null && item >= 0 && csvMap.size() >= item) {
            Map<String, D> elementMap = new HashMap<>(csvMap.size());
            for (String key : csvMap.keySet())
                elementMap.put(key, csvMap.get(key).get(item));
            return elementMap;
        }
        return null;
    }

    public Map<String, D> mapFromCSV (String csv) {
        return mapFromListElement(csvAsMap(csv), 0);
    }

    public static String mapToCSV (Map<String, List> table) {
        try {
            String csv = "";
            for (String key : table.keySet())
                csv += key + ',';
            int lastComma = csv.lastIndexOf(',');
            if (lastComma > 0)
                csv = csv.substring(0, lastComma);
            csv += '\n';

            int numberOfLines = table.values().size();
            for (int line = 0; line < numberOfLines; line++) {
                for (String key : table.keySet()) {
                    csv += table.get(key).get(line).toString() + ',';
                }
                lastComma = csv.lastIndexOf(',');
                if (lastComma > 0)
                    csv = csv.substring(0, lastComma);
                csv += '\n';
            }
            int lastLine = csv.lastIndexOf('\n');
            return lastLine > 0 ? csv.substring(0, lastLine) : csv;
        }
        catch (NullPointerException | ArrayIndexOutOfBoundsException nae) {
            System.err.println("Incorrect syntax provided for csvAsMap: " + nae.getMessage());
            return null;
        }
    }

    public Map<String, D> mapFromXML(Document xml) {
        if (xml != null) try {
            NodeList children = xml.getDocumentElement().getElementsByTagName("*");
            Map<String, D> table = new HashMap<>(children.getLength());
            for (int c = 0; c < children.getLength(); c++) {
                Node child = children.item(c);
                table.put(child.getNodeName(), parseValue(child.getTextContent()));
            }
            return table;
        }
        catch (Exception ex) {
            System.err.println("Incorrect syntax provided for mapFromXML: "+ex.getMessage());
        }
        return null;
    }

    @Override
    public int compareTo(DataSource another) {
        return importance.compareTo(another.importance);
    }

    public boolean equals(DataSource<D> other) {
        return _id == other.getID() && name.equals(other.getName());
    }

    protected DataSource (Parcel incoming) {
        _id = incoming.readLong();
        name = incoming.readString();
        updatable = Boolean.parseBoolean(incoming.readString());
        try {
            location = new URL(incoming.readString());
        } catch (MalformedURLException mue) {
            location = null;
        }
        try {
            importance = Importance.valueOf(incoming.readString());
        } catch (IllegalArgumentException iae) {
            importance = null;
        }
        readings = mapFromBundle(incoming.readBundle());
        targets = mapFromBundle(incoming.readBundle());
        targetsEnabled = boolMapFromBundle(incoming.readBundle());
    }

    public static final Creator<DataSource> CREATOR = new Creator<DataSource>() {
        @Override
        public DataSource createFromParcel(Parcel incoming) {
            return new DataSource (incoming);
        }

        @Override
        public DataSource[] newArray(int size) {
            return new DataSource[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    protected static Bundle mapToBundle (Map<?, ? extends Serializable> map) {
        Bundle bundle = null;
        if (map != null) {
            bundle = new Bundle(map.size());
            for (Object key : map.keySet())
                bundle.putSerializable(String.valueOf(key), map.get(key));
        }
        return bundle;
    }

    protected static Map<String, Boolean> boolMapFromBundle (Bundle bundle) {
        Map<String, Boolean> map = new HashMap<>(bundle.size());
        for (String key : bundle.keySet())
            map.put(key, (Boolean) bundle.get(key));
        return map;
    }

    protected Map<String, D> mapFromBundle (Bundle bundle) {
        Map<String, D> map = new HashMap<>(bundle.size());
        for (String key : bundle.keySet())
            map.put(key, (D) bundle.get(key));
        return map;
    }

    @Override
    public void writeToParcel(Parcel outgoing, int flags) {
        outgoing.writeLong(_id);
        outgoing.writeString(name);
        outgoing.writeString(String.valueOf(updatable));
        outgoing.writeString(location != null ? location.toString() : null);
        outgoing.writeString(importance != null ? importance.name() : null);
        outgoing.writeBundle(mapToBundle(readings));
        outgoing.writeBundle(mapToBundle(targets));
        outgoing.writeBundle(mapToBundle(targetsEnabled));
    }
}
