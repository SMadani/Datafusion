package uk.co.synatronics.Datafusion;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import java.net.URL;
import java.util.*;

class FusionDB extends SQLiteOpenHelper {

    private static final int DB_VERSION = 1;
    private static final String
        DATABASE_NAME = "DataFusion.db",
        MASTER_TABLE = "M_DataSources",
        DBC_KEY = "Key",
        DBC_ID = "_id",
        DBC_NAME = "Name",
        DBC_LOCATION = "Url",
        DBC_PRIORITY = "Importance",
        DBC_READING = "Reading",
        DBC_TARGET = "Target",
        DBC_LOW = "LowThreshold",
        DBC_HIGH = "HighThreshold",
        DBC_ENABLED = "Enabled",
        DBC_TARGET_ENABLED = DBC_TARGET+DBC_ENABLED,
        DBC_LOW_ENABLED = DBC_LOW+DBC_ENABLED,
        DBC_HIGH_ENABLED = DBC_HIGH+DBC_ENABLED,
        DBC_TYPE = "REAL",
        DBC_ENABLED_TYPE = "BOOLEAN",
        DBC_TABLE_PREFIX = "dst_",
        DBC_CREATE_TABLE = "CREATE TABLE IF NOT EXISTS ",
        DBC_DELETE_TABLE = "DROP TABLE IF EXISTS ",
        CREATE_DB_SQL = DBC_CREATE_TABLE+MASTER_TABLE+'('+DBC_ID+" INTEGER PRIMARY KEY, "+DBC_NAME+" TEXT, "+DBC_LOCATION+" TEXT, "+DBC_PRIORITY+" INTEGER, "+DBC_ENABLED+' '+DBC_ENABLED_TYPE+");",
        CREATE_ENTRY_SQL = '('+ DBC_KEY +" TEXT,"+ DBC_READING +' '+ DBC_TYPE +','+ DBC_TARGET_ENABLED +' '+ DBC_ENABLED_TYPE+','+DBC_TARGET+' '+DBC_TYPE+','+ DBC_LOW_ENABLED+' '+DBC_ENABLED_TYPE+','+DBC_LOW+' '+DBC_TYPE+','+DBC_HIGH_ENABLED+' '+DBC_ENABLED_TYPE+','+DBC_HIGH+' '+DBC_TYPE+");";

    FusionDB(Context context) {
        this (context, DATABASE_NAME, null, DB_VERSION);
    }

    private FusionDB(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name != null && !name.isEmpty() && name.endsWith(".db") ? name : DATABASE_NAME, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_DB_SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //TODO: implement
        Log.d("Database upgrade", this.getClass().getName()+" upgrade from version "+oldVersion+" to "+newVersion);
    }

    DoubleDataSource getData(long id) {
        List<DoubleDataSource> single = getDataSources(id, id);
        if (single != null && !single.isEmpty())
            return single.get(0);
        return null;
    }

    ArrayList<DoubleDataSource> getDataSources() {
        return getDataSources(getNumberOfEntries());
    }

    ArrayList<DoubleDataSource> getDataSources(long toID) {
        return getDataSources(0, toID);
    }

    ArrayList<DoubleDataSource> getDataSources(long fromID, long toID) {
        if (fromID < 0 || toID < 0 || fromID > toID)
            return null;

        SQLiteDatabase db = getReadableDatabase();
        Cursor mtCursor = db.query(MASTER_TABLE, null, DBC_ID +" >= "+fromID+" AND "+ DBC_ID +" <= "+toID, null, null, null, DBC_ID);
        ArrayList<DoubleDataSource> dataList = new ArrayList<>((int) (toID-fromID));

        while (mtCursor.moveToNext()) try {
            int idIndex, nameIndex, importanceIndex, locationIndex;
            idIndex = mtCursor.getColumnIndexOrThrow(DBC_ID);
            nameIndex = mtCursor.getColumnIndexOrThrow(DBC_NAME);
            importanceIndex = mtCursor.getColumnIndex(DBC_PRIORITY);
            locationIndex = mtCursor.getColumnIndex(DBC_LOCATION);

            long id = mtCursor.getLong(idIndex);
            DataSource.Importance importance = null;
            String name = null, location = null;

            if (nameIndex >= 0 && !mtCursor.isNull(nameIndex))
                name = mtCursor.getString(nameIndex);
            if (importanceIndex >= 0 && !mtCursor.isNull(importanceIndex))
                importance = DataSource.Importance.valueOf(mtCursor.getString(importanceIndex));
            if (locationIndex >= 0 && !mtCursor.isNull(locationIndex))
                location = mtCursor.getString(locationIndex);

            Cursor dsCursor = db.query(DBC_TABLE_PREFIX+id, null, null, null, null, null, DBC_KEY);
            DoubleDataSource ds = new DoubleDataSource(dsCursor.getCount());
            ds.setID(id);

            if (name != null)
                ds.setName(name);
            if (location != null)
                ds.setLocation(location);
            if (importance != null)
                ds.setImportance(importance);

            while (dsCursor.moveToNext()) {
                String key = dsCursor.getString(dsCursor.getColumnIndexOrThrow(DBC_KEY));
                int rValIndex = dsCursor.getColumnIndex(DBC_READING);

                if (rValIndex >= 0 && !dsCursor.isNull(rValIndex)) {
                    ds.setReading(key, dsCursor.getDouble(rValIndex));

                    int tEnabledIndex, lEnabledIndex, hEnabledIndex, tValIndex, lValIndex, hValIndex;
                    tEnabledIndex = dsCursor.getColumnIndex(DBC_TARGET_ENABLED);
                    tValIndex = dsCursor.getColumnIndex(DBC_TARGET);
                    lEnabledIndex = dsCursor.getColumnIndex(DBC_LOW_ENABLED);
                    lValIndex = dsCursor.getColumnIndex(DBC_LOW);
                    hEnabledIndex = dsCursor.getColumnIndex(DBC_HIGH_ENABLED);
                    hValIndex = dsCursor.getColumnIndex(DBC_HIGH);

                    boolean tEnabled = false, lEnabled = false, hEnabled = false;
                    if (tEnabledIndex >= 0 && !dsCursor.isNull(tEnabledIndex))
                        tEnabled = dsCursor.getInt(tEnabledIndex) > 0;
                    if (lEnabledIndex >= 0 && !dsCursor.isNull(lEnabledIndex))
                        lEnabled = dsCursor.getInt(lEnabledIndex) > 0;
                    if (hEnabledIndex >= 0 && !dsCursor.isNull(hEnabledIndex))
                        hEnabled = dsCursor.getInt(hEnabledIndex) > 0;

                    if (tValIndex >= 0 && !dsCursor.isNull(tValIndex))
                        ds.setTarget(key, dsCursor.getDouble(tValIndex), tEnabled);
                    if (lValIndex >= 0 && !dsCursor.isNull(lValIndex))
                        ds.setLowThreshold(key, dsCursor.getDouble(lValIndex), lEnabled);
                    if (hValIndex >= 0 && !dsCursor.isNull(hValIndex))
                        ds.setHighThreshold(key, dsCursor.getDouble(hValIndex), hEnabled);
                }
            }
            dsCursor.close();
            dataList.add(ds);
        }
        catch (Exception e) {
            System.err.println("Could not get data sources: "+e.getMessage());
        }
        mtCursor.close();
        return dataList;
    }

    <N extends NumericDataSource> N createData(N data) {
        return createData(data, -1);
    }

    <N extends NumericDataSource> N createData (N data, long assignedID) {
        if (data != null) {
            SQLiteDatabase db = getWritableDatabase();
            if (!isRegistered(assignedID)) {
                ContentValues metaCV = metaContentValues(data);
                if (metaCV != null && metaCV.size() > 0)
                    data.setID(db.insert(MASTER_TABLE, null, metaCV));
            }
            String table = getTableName(data.getID());
            db.execSQL(DBC_CREATE_TABLE+'"'+table+'"'+CREATE_ENTRY_SQL);
            Map<String, ContentValues> valuesMap = sourceToValues(data);
            for (ContentValues row : valuesMap.values())
                db.insert(table, DBC_KEY, row);
        }
        return data;
    }

    <N extends NumericDataSource> boolean updateReadings (N dataSource) {
        return dataSource != null && updateReadings(dataSource.getID(), dataSource.getReadings());
    }

    <N extends Number & Comparable<N>> boolean updateReadings(long id, Map<String, N> readings) {
        return updateColumn(getTableName(id), readings, DBC_READING);
    }

    boolean editData (NumericDataSource replacement) {
        return replacement != null && editData(replacement.getID(), replacement);
    }

    boolean editData(long id, NumericDataSource replacement) {
        if (isRegistered(id)) try {
            ContentValues metaCV = metaContentValues(replacement);
            if (metaCV != null && metaCV.size() > 0) {
                SQLiteDatabase db = getWritableDatabase();
                if (deleteData(id, false) && db.update(MASTER_TABLE, metaCV, DBC_ID+'='+id, null) > 0)
                    return createData(replacement, id) != null;
            }
        }
        catch (Exception e) {
            System.err.println("Could not edit data with id '"+id+"': "+e.getMessage());
        }
        return false;
    }

    boolean deleteData(long id) {
        return deleteData(id, true);
    }

    private boolean deleteData (long id, boolean deleteFromMaster) {
        String table = null;
        try {
            SQLiteDatabase db = getWritableDatabase();
            if (!deleteFromMaster || db.delete(MASTER_TABLE, DBC_ID+'='+id, null) > 0) {
                table = getTableName(id);
                if (table != null) {
                    db.execSQL(DBC_DELETE_TABLE+'\''+table+"';");
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println("Could not delete "+table+": "+e.getMessage());
        }
        return false;
    }

    private boolean isRegistered(long id) {
        return getReadableDatabase().query
                (MASTER_TABLE, new String[]{DBC_ID}, DBC_ID+'='+id, null, null, null, null)
                .getCount() > 0;
    }

    protected Set<String> getColumns (String table) {
        return new HashSet<>(Arrays.asList(
                getReadableDatabase().query(table, null, null, null, null, null, null).getColumnNames()));
    }

    private int getNumberOfEntries() {
        return getReadableDatabase().query(true, MASTER_TABLE, new String[]{DBC_ID}, null, null, null, null, DBC_ID, null).getCount();
    }

    private String getTableName(long id) {
        /*Cursor cursor = null;
        String table = null;
        try {
            cursor = getReadableDatabase().query(MASTER_TABLE, new String[]{DBC_NAME}, DBC_ID+'='+id, null, null, null, DBC_ID, "1");
            if (cursor.moveToNext())
                table = sanitizeTableName(cursor.getString(cursor.getColumnIndexOrThrow(DBC_NAME)));
        }
        catch (Exception e) {
            System.err.println("Could not get table name for id '"+id+"': "+e.getMessage());
        }
        finally {
            if (cursor != null)
                cursor.close();
        }*/
        return DBC_TABLE_PREFIX+id;
    }

    protected String sanitizeTableName (String name) {
        return name != null ?
                DBC_TABLE_PREFIX + name.replaceAll("\\s+", "")
                        .replaceAll("[^A-Za-z0-9]", "")
                : DBC_TABLE_PREFIX+getNumberOfEntries()+1;
    }

    private static Map<String, ContentValues> sourceToValues(NumericDataSource data) {
        return data != null ? mapRows(putValues(data), data.getVariables(), DBC_KEY) : null;
    }

    private static ContentValues metaContentValues(DataSource data) {
        ContentValues values = new ContentValues(4);
        if (data != null) {
            String name = data.getName();
            DataSource.Importance importance = data.getImportance();
            URL location = data.getLocation();
            boolean updatable = data.isUpdatable();
            values.put(DBC_ENABLED, boolToInt(updatable));
            if (name != null)
                values.put(DBC_NAME, name);
            if (importance != null)
                values.put(DBC_PRIORITY, importance.toString());
            if (location != null)
                values.put(DBC_LOCATION, location.toString());
        }
        return values;
    }

    private static int boolToInt(boolean b) {
        return b ? 1 : 0;
    }

    private static Map<String, Integer> mapBoolToInt (Map<String, Boolean> boolMap) {
        if (boolMap != null) {
            Map<String, Integer> intMap = new HashMap<>(boolMap.size());
            Set<String> keys = boolMap.keySet();
            for (String key : keys)
                intMap.put(key, boolToInt(boolMap.get(key)));
            return intMap;
        }
        return null;
    }

    /**
     * Maps column names to the map of column values. This effectively contains the information to map an entire table.
     * @param data The data whose values will be mapped.
     * @return A Map containing the column names as keys and the map of row values.
     */
    private static Map<String, Map> putValues(NumericDataSource data) {
        Map<String, Map> tableMap = new HashMap<>(7);
        if (data != null) {
            tableMap.put(DBC_READING, data.getReadings());
            tableMap.put(DBC_TARGET_ENABLED, mapBoolToInt(data.getTargetsEnabled()));
            tableMap.put(DBC_TARGET, data.getTargets());
            tableMap.put(DBC_LOW_ENABLED, mapBoolToInt(data.getLowsEnabled()));
            tableMap.put(DBC_LOW, data.getLowThresholds());
            tableMap.put(DBC_HIGH_ENABLED, mapBoolToInt(data.getHighsEnabled()));
            tableMap.put(DBC_HIGH, data.getHighThresholds());
        }
        return tableMap;
    }

    private static ContentValues putWithInference(String key, Object value, ContentValues cv) {
        if (cv == null) cv = new ContentValues(1);

        if (value != null) {
            if (value instanceof Double)
                cv.put(key, (Double) value);
            else if (value instanceof Integer)
                cv.put(key, (Integer) value);
            else if (value instanceof String)
                cv.put(key, (String) value);
            else if (value instanceof Long)
                cv.put(key, (Long) value);
            else if (value instanceof Boolean)
                cv.put(key, (Boolean) value);
            else if (value instanceof Float)
                cv.put(key, (Float) value);
            else if (value instanceof byte[])
                cv.put(key, (byte[]) value);
            else if (value instanceof Byte)
                cv.put(key, (Byte) value);
        }
        return cv;
    }

    private static Map<String, ContentValues> mapRows(Map<String, Map> kvpMaps) {
        return mapRows(kvpMaps, true);
    }

    private static Map<String, ContentValues> mapRows(Map<String, Map> kvpMaps, boolean includeKey) {
        return mapRows(kvpMaps, null, includeKey ? DBC_KEY : null);
    }

    /**
     * Converts a collection of maps which share a common key into a number ContentValues equal to the number of keys. The keys in every (nested) Map in kvpMap should all be equal to the keyset "keys" provided.
     * @param kvpMaps The mapping containing the column name and the map of data to populate the rows with.
     * @param keys The common set of keys used by the maps; which will represent the first column.
     * @param keyColumn The name to use for representing the keys column.
     * @return A map of row name to ContentValues containing the value of the row and the columns to apply it to. This is equal in size to the number of rows as represented by the number of keys.
     */
    private static Map<String, ContentValues> mapRows (Map<String, Map> kvpMaps, Set keys, String keyColumn) {
        Map<String, ContentValues> rowMaps = null;
        if (kvpMaps != null) {
            if (keys == null || keys.isEmpty()) {
                Iterator<Map> iterator = kvpMaps.values().iterator();
                keys = iterator.hasNext() ? iterator.next().keySet() : null;
            }
            if (keys != null) {
                rowMaps = new HashMap<>(keys.size());
                Set<String> columnNames = kvpMaps.keySet();
                boolean includeKey = keyColumn != null && !keyColumn.isEmpty();

                for (Object key : keys) {
                    ContentValues row = new ContentValues(columnNames.size()+1);
                    if (includeKey)
                        row = putWithInference(keyColumn, key, row);
                    for (String column : columnNames)
                        row = putWithInference(column, kvpMaps.get(column).get(key), row);

                    rowMaps.put(String.valueOf(key), row);
                }
            }
        }
        return rowMaps;
    }

    private boolean updateColumn (String table, Map columnValues, String column) {
        try {
            Map<String, Map> colMaps = new HashMap<>(1);
            colMaps.put(column, columnValues);
            Map<String, ContentValues> rowsMap = mapRows(colMaps);
            SQLiteDatabase db = getWritableDatabase();
            Set<String> rowNames = rowsMap.keySet();
            for (String row : rowNames) {
                if (db.update(table, rowsMap.get(row), DBC_KEY+" = ?", new String[]{row}) <= 0)
                    db.insert(table, column, rowsMap.get(row));
            }

            return true;
        }
        catch (Exception e) {
            System.err.println("Could not update \""+column+"\" in \""+table+"\": "+e.getMessage());
        }
        return false;
    }

    private boolean updateTable (DoubleDataSource data) {
        return data != null && updateTable(data, getTableName(data.getID()));
    }

    private boolean updateTable (NumericDataSource data, String table) {
        boolean uReadings = updateColumn(table, data.getReadings(), DBC_READING);
        boolean uTargetsEnabled = updateColumn(table, data.getTargetsEnabled(), DBC_TARGET_ENABLED);
        boolean uTargets = updateColumn(table, data.getTargets(), DBC_TARGET);
        boolean uLowsEnabled = updateColumn(table, data.getLowsEnabled(), DBC_LOW_ENABLED);
        boolean uLows = updateColumn(table, data.getLowThresholds(), DBC_LOW);
        boolean uHighsEnabled = updateColumn(table, data.getHighsEnabled(), DBC_HIGH_ENABLED);
        boolean uHighs = updateColumn(table, data.getHighThresholds(), DBC_HIGH);
        return uReadings && uTargetsEnabled && uTargets && uLowsEnabled && uLows && uHighsEnabled && uHighs;
    }
/*
    *//**
     * Creates a Set of ContentValues which are populated with mappings. The number of keys must equal the number of elements in each List of the mapping. In a sense, this method provides a multi-dimensional mapping between the keys and multiple values as defined in the Map.
     * @param keys The values to be inserted into each row of the "Key" column
     * @param columnsMap The keys should be the column names (excluding "Key") and the values are a List of elements. The nth item in the list will be inserted into the nth column.
     * @return A set of ContentValues, where each one contains a mapping between a column name (either "Key" or a key specified in the mapping) and a value specified in the list.
     *//*
    protected static Set<ContentValues> dataContentValues(List keys, Map<String, List> columnsMap) {
        if (columnsMap != null && keys != null && !keys.isEmpty() && columnsMap.size() == keys.size()) {
            Collection<List> rowValues = columnsMap.values();
            Set<String> columnNames = columnsMap.keySet();
            List rowVal = rowValues.iterator().next();
            if (rowVal != null && !rowVal.isEmpty()) {
                for (List rVal : rowValues)
                    if (rVal == null || rVal.size() != rowVal.size())
                        return null;

                Set<ContentValues> cvSet = new HashSet<>(keys.size());
                for (int k = 0; k < keys.size(); k++) {
                    ContentValues kvp = new ContentValues(rowVal.size()+1);
                    kvp.put(DBC_KEY, String.valueOf(keys.get(k)));
                    for (String column : columnNames)
                        kvp.put(column, String.valueOf(columnsMap.get(column).get(k)));
                    cvSet.add(kvp);
                }
                return cvSet;
            }
        }
        return null;
    }*/
}
