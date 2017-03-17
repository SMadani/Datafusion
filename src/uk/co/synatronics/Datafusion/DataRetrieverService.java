package uk.co.synatronics.Datafusion;

import android.app.*;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.*;
import android.telephony.SmsManager;
import android.util.Log;
import static java.lang.Thread.*;
import static uk.co.synatronics.Datafusion.MainActivity.*;
import java.io.Serializable;
import java.util.*;

public class DataRetrieverService extends Service implements Runnable {

    private WifiManager wifiManager;
    private NotificationManager notificationManager;
    private SmsManager smsManager;
    private final DataBinder binder;
    static FusionDB fusionDB;
    private static final String THREAD_NAME = "DataFusion Retriever Service worker thread";
    private Intent instruction;
    private ArrayList<? extends NumericDataSource> dataSources;
    private long refresh;
    private Class<? extends Activity> targetActivity;
    private ResultReceiver channel;
    private volatile boolean stop;
    private boolean wifiOnly, notificationsEnabled, smsEnabled;
    private String phoneNumber;
    private static DataRetrieverService instance;
    private Thread workerThread;

    public DataRetrieverService() {
        binder = new DataBinder();
        instruction = null;
        dataSources = new ArrayList<>();
        refresh = 600000;
        stop = wifiOnly = notificationsEnabled = true;
        channel = null;
        targetActivity = MainActivity.class;
        instance = this;
        workerThread = new Thread(this);
        if (fusionDB == null)
            fusionDB = new FusionDB(this);
    }

    String getPhoneNumber() {
        return phoneNumber;
    }

    void setPhoneNumber (String number) {
        if (number != null)
            this.phoneNumber = number;
    }

    boolean isSmsEnabled () {
        return smsEnabled;
    }

    void setSmsEnabled (boolean enabled) {
        this.smsEnabled = enabled;
    }

    static DataRetrieverService getInstance() {
        return instance;
    }

    boolean isWifiOnly() {
        return wifiOnly;
    }

    protected void setWiFiOnly (boolean update) {
        wifiOnly = update;
    }

    boolean canNotify() {
        return notificationsEnabled;
    }

    protected void setNotificationsEnabled (boolean notifiable) {
        notificationsEnabled = notifiable;
    }

    ArrayList<? extends NumericDataSource> getDataSources() {
        return dataSources;
    }

    protected void setDataSources (Collection<? extends NumericDataSource> dataList) {
        if (dataList != null)
            dataSources = new ArrayList<>(dataList);
    }

    long getFrequency() {
        return refresh;
    }

    protected void setFrequency(long freq) {
        if (freq > 5000)
            refresh = freq;
    }

    protected void setResultReceiver (ResultReceiver receiver) {
        if (receiver != null)
            channel = receiver;
    }

    ResultReceiver getResultReceiver() {
        return channel;
    }

    protected void setTargetActivity(Class<? extends Activity> destination) {
        if (destination != null)
            targetActivity = destination;
    }

    protected Class<? extends Activity> getTargetActivity() {
        return targetActivity;
    }

    private class DataBinder extends Binder {
        public DataRetrieverService getService() {
            return DataRetrieverService.getInstance();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent request, int flags, int startId) {
        instance = this;
        stop = true;
        if (notificationManager == null)
            notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (wifiManager == null)
            wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        if (smsManager == null)
            smsManager = SmsManager.getDefault();
        instruction = request;
        if (instruction != null) {
            if (workerThread.isAlive())
                workerThread.interrupt();
            String threadName = instruction.getAction();
            if (threadName == null || threadName.isEmpty())
                threadName = THREAD_NAME;
            workerThread = new Thread(this, threadName);
            workerThread.start();
        }
        return super.onStartCommand(instruction, flags, startId);
    }

    @Override
    public void run() {
        if (instruction != null) {
            refresh = instruction.getLongExtra(UPDATE_FREQUENCY, refresh);
            wifiOnly = instruction.getBooleanExtra(UPDATE_WIFI_ONLY, wifiOnly);
            notificationsEnabled = instruction.getBooleanExtra(ALLOW_NOTIFICATIONS, notificationsEnabled);
            smsEnabled = instruction.getBooleanExtra(SMS_ENABLED, smsEnabled);
            phoneNumber = instruction.getStringExtra(PHONE_NUMBER);

            ArrayList<? extends NumericDataSource> intentData = instruction.getParcelableArrayListExtra(DATA_LIST);
            if (intentData != null && !intentData.isEmpty())
                dataSources = intentData;

            ResultReceiver callback = instruction.getParcelableExtra(DATA_RECEIVER);
            if (callback != null)
                channel = callback;

            Serializable serialData = instruction.getSerializableExtra(DATA_HANDLER);
            if (serialData instanceof Class)
                targetActivity = (Class<? extends Activity>) serialData;

            stop = dataSources == null || dataSources.isEmpty();

            while (!stop) try {
                if (!wifiOnly || (wifiManager != null && wifiManager.isWifiEnabled())) {
                    for (NumericDataSource data : dataSources) {
                        if (data.isUpdatable() && data.assignFromURL()) {
                            if (smsEnabled)
                                sendText(phoneNumber, data.toString());

                            if (notificationsEnabled) {
                                if (!data.differsFromTargets().isEmpty())
                                    produceNotification(1, dataSources.indexOf(data), "Target thresholds exceeded!", data);

                                if (!data.exceedsLows().isEmpty())
                                    produceNotification(2, dataSources.indexOf(data), "Low thresholds exceeded!", data);

                                else if (!data.exceedsHighs().isEmpty())
                                    produceNotification(3, dataSources.indexOf(data), "High thresholds exceeded!", data);
                            }

                            Bundle bundle = new Bundle();
                            bundle.putParcelable(UPDATED_DATA, data);
                            bundle.putInt(DATA_INDEX, dataSources.indexOf(data));
                            bundle.putLong(DATA_ID, data.getID());
                            channel.send(UPDATE_REQUEST, bundle);
                        }
                    }
                }
                sleep(refresh);
            }
            catch (InterruptedException ie) {
                Log.i("DataRetrieverService", currentThread().getName()+" interrupted: "+ie.getMessage());
                break;
            }
            //stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        stop = true;
        if (workerThread.isAlive())
            workerThread.interrupt();
        workerThread = null;
        instance = null;
        super.onDestroy();
    }

    private void sendText (String phoneNumber, String message) {
        if (smsManager != null && phoneNumber != null && !phoneNumber.isEmpty() && message != null && !message.isEmpty())
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
    }

    private void produceNotification(int requestCode, int notificationID, String message, NumericDataSource data) {
        Intent dnt = new Intent(this, targetActivity)
                .setAction(THRESHOLD_EXCEEDED)
                .putExtra(DATASOURCE, data);

        Notification.Builder builder = new Notification.Builder(this)
                .setContentTitle(THRESHOLD_EXCEEDED)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_description_white_24dp)
                .setContentIntent(PendingIntent.getActivity(this, requestCode, dnt, PendingIntent.FLAG_CANCEL_CURRENT));

        notificationManager.notify(notificationID, builder.getNotification());
    }
}
