/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sublate.gpstracker.Tracker;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.widget.Toast;

import com.sublate.gpstracker.GuiServiceMessageController;
import com.sublate.gpstracker.MainActivity;
import com.sublate.gpstracker.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Location Tracking service
 *
 * Records location updates for all registered location providers, and cell
 * location updates
 */
public class TrackerService extends Service {
    public static final String ACTION = "TrackerServiceAction";
    private List<LocationTrackingListener> mListeners;

    private static final String LOG_TAG = com.sublate.gpstracker.Tracker.TrackerActivity.LOG_TAG;

    // controls which location providers to track
    private Set<String> mTrackedProviders;

    private TrackerDataHelper mTrackerData;

    private TelephonyManager mTelephonyManager;
    private Location mNetworkLocation;

    // Handlers and Receivers for phone and network state
    private NetworkStateBroadcastReceiver mNetwork;
    private static final String CELL_PROVIDER_TAG = "cell";
    // signal strength updates
    private static final String SIGNAL_PROVIDER_TAG = "signal";
    private static final String WIFI_PROVIDER_TAG = "wifi";
    // tracking tag for data connectivity issues
    private static final String DATA_CONN_PROVIDER_TAG = "data";

    // preference constants
    private static final String MIN_TIME_PREF = "mintime_preference";
    private static final String MIN_DIS_PREF = "mindistance_preference";
    private static final String GPS_PREF = "gps_preference";
    private static final String NETWORK_PREF = "network_preference";
    private static final String SIGNAL_PREF = "signal_preference";
    private static final String DEBUG_PREF = "advanced_log_preference";
    private static final String GPS_UPDATES_PREF = "gps_updates_preference";

    private boolean running = false;

    private PreferenceListener mPrefListener;

    private GuiServiceMessageController messageController;
    private BroadcastReceiver mTrackerServiceReceiver;
    private List<ScheduleEntry> mScheduleList;


    private int pref_gps_updates;
    private long latestUpdate;

    public TrackerService() {
    }

    public boolean getRunning()
    {
        return running;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // ignore - nothing to do
        return null;
    }

    /**
     * registers location listeners
     *
     * @param //intent
     * @param //startId
     */
    @Override
    public void onCreate() {
        super.onCreate(); //(intent, startId);


        mNetworkLocation = null;
        messageController = new GuiServiceMessageController(this, TrackerService.class);
        mTrackerServiceReceiver = new TrackerServiceReceiver(this);



        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(messageController.getServiceActionName());
        registerReceiver(mTrackerServiceReceiver, intentFilter);


      //  startAutocheck();

        Toast.makeText(this, "Tracking service started", Toast.LENGTH_SHORT);

        // startForegroundService();
        // startLocationListeners();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
     //  return super.onStartCommand(intent, flags, startId);
    }

    private synchronized void startLocationListeners() {

        LocationManager lm = getLocationManager();

        mTrackerData = new TrackerDataHelper(this);
        mScheduleList = mTrackerData.getGetAllSchedulers();

        mTrackedProviders = getTrackedProviders();

        List<String> locationProviders = lm.getAllProviders();
        mListeners = new ArrayList<LocationTrackingListener>(
                locationProviders.size());

        long minUpdateTime = getLocationUpdateTime();
        float minDistance = getLocationMinDistance();

        pref_gps_updates = getGpsUpdates();

        for (String providerName : locationProviders) {
            if (mTrackedProviders.contains(providerName)) {
                Log.d(LOG_TAG, "Adding location listener for provider " +
                        providerName);
                if (doDebugLogging()) {
                    mTrackerData.writeEntry("init", String.format(
                            "start listening to %s : %d ms; %f meters",
                            providerName, minUpdateTime, minDistance));
                }    
                LocationTrackingListener listener =
                    new LocationTrackingListener();
                lm.requestLocationUpdates(providerName, minUpdateTime,minDistance, listener);
                mListeners.add(listener);
            }
        }
        mTelephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);

        if (doDebugLogging()) {
            // register for cell location updates
            mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CELL_LOCATION);

            // Register for Network (Wifi or Mobile) updates
            mNetwork = new NetworkStateBroadcastReceiver();
            IntentFilter mIntentFilter;
            mIntentFilter = new IntentFilter();
            mIntentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            mIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
            mIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            mIntentFilter.addAction(ACTION);
            Log.d(LOG_TAG, "registering receiver");
            registerReceiver(mNetwork, mIntentFilter);
        }

        if (trackSignalStrength()) {
            mTelephonyManager.listen(mPhoneStateListener,
                    PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        }

        // register for preference changes, so we can restart listeners on
        // pref changes
        mPrefListener = new PreferenceListener();
        getPreferences().registerOnSharedPreferenceChangeListener(mPrefListener);

        running = true;
    }

    private Set<String> getTrackedProviders() {
        Set<String> providerSet = new HashSet();

        if (trackGPS()) {
            providerSet.add(LocationManager.GPS_PROVIDER);
        }
        if (trackNetwork()) {
            providerSet.add(LocationManager.NETWORK_PROVIDER);
        }
        return providerSet;
    }

    private SharedPreferences getPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(this);
    }

    private boolean trackNetwork() {
        return getPreferences().getBoolean(NETWORK_PREF, true);
    }

    private boolean trackGPS() {
        return getPreferences().getBoolean(GPS_PREF, true);
    }

    private boolean doDebugLogging() {
        return getPreferences().getBoolean(DEBUG_PREF, false);
    }

    private boolean trackSignalStrength() {
        return getPreferences().getBoolean(SIGNAL_PREF, false);
    }

    private float getLocationMinDistance() {
        try {
            String disString = getPreferences().getString(MIN_DIS_PREF, "0");
            return Float.parseFloat(disString);
        }
        catch (NumberFormatException e) {
            Log.e(LOG_TAG, "Invalid preference for location min distance", e);
        }
        return 0;
    }

    private int getGpsUpdates() {
        try {
            String disString = getPreferences().getString(GPS_UPDATES_PREF, "10");
            return Integer.parseInt(disString)*1000;
        }
        catch (NumberFormatException e) {
            Log.e(LOG_TAG, "Invalid preference for GPS Updates", e);
        }
        return 0;
    }

    private long getLocationUpdateTime() {
        try {
            String timeString = getPreferences().getString(MIN_TIME_PREF, "0");
            long secondsTime = Long.valueOf(timeString);
            return secondsTime * 1000;
        }
        catch (NumberFormatException e) {
            Log.e(LOG_TAG, "Invalid preference for location min time", e);
        }
        return 0;
    }

    private int getCurrentSchedule()
    {
        if (mScheduleList ==null)
            return 0;

        for (ScheduleEntry entry : mScheduleList)
        {
            long startMiliSecsDate = milliseconds(entry.getTimeStart());
            long endMiliSecsDate = milliseconds(entry.getTimeEnd());
            long nowMiliSecsDate = new Date().getTime();

            if (startMiliSecsDate <= nowMiliSecsDate && nowMiliSecsDate <=endMiliSecsDate )
                return entry.getId();
        }
        return 0;
    }

    public long milliseconds(String HourMin)
    {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd "); //SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); //dd/MM/yyyy
        Date now = new Date();
        String strDate = sdfDate.format(now);

        strDate += HourMin +":00";

        //String date_ = date;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try
        {
            Date mDate = sdf.parse(strDate);
            long timeInMilliseconds = mDate.getTime();
            return timeInMilliseconds;
        }
        catch (ParseException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Shuts down this service
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "Removing location listeners");

        // stop internal/being-for-others listener
        if (mTrackerServiceReceiver != null) {
            unregisterReceiver(mTrackerServiceReceiver);
        }
        mTrackerServiceReceiver = null;
        stopListeners();
        Toast.makeText(this, "Tracking service stopped", Toast.LENGTH_SHORT);
    }

    /**
     * De-registers all location listeners, closes persistent storage
     */
    protected synchronized void stopListeners() {
        LocationManager lm = getLocationManager();
        if (mListeners != null) {
            for (LocationTrackingListener listener : mListeners) {
                lm.removeUpdates(listener);
            }
            mListeners.clear();
        }
        mListeners = null;

        // stop cell state listener
        if (mTelephonyManager != null) {
            mTelephonyManager.listen(mPhoneStateListener, 0);
        }    
        
        // stop network/wifi listener
        if (mNetwork != null) {
            unregisterReceiver(mNetwork);
        }
        mNetwork = null;

        /*
        // stop internal/being-for-others listener
        if (mTrackerServiceReceiver != null) {
            unregisterReceiver(mTrackerServiceReceiver);
        }
        mTrackerServiceReceiver = null;
        */

        mTrackerData = null;
        if (mPrefListener != null) {
            getPreferences().unregisterOnSharedPreferenceChangeListener(mPrefListener);
            mPrefListener = null;
        }
        running = false;
    }

    private LocationManager getLocationManager() {
        return (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    /**
     * Determine the current distance from given location to the last
     * approximated network location
     *
     * @param location - new location
     *
     * @return float distance in meters
     */
    private synchronized float getDistanceFromNetwork(Location location) {
        float value = 0;
        if (mNetworkLocation != null) {
            value = location.distanceTo(mNetworkLocation);
        }
        if (LocationManager.NETWORK_PROVIDER.equals(location.getProvider())) {
            mNetworkLocation = location;
        }
        return value;
    }

    private class LocationTrackingListener implements LocationListener {

        /**
         * Writes details of location update to tracking file, including
         * recording the distance between this location update and the last
         * network location update
         *
         * @param location - new location
         */
        public void onLocationChanged(Location location) {
            if (location == null) {
                return;
            }
            int currentSchedule =  getCurrentSchedule();
            if (currentSchedule==0) {
               // stopListeners();
                return;
            }

            long currentTime = System.currentTimeMillis();
            if ((currentTime - latestUpdate) < (pref_gps_updates) ) {
                return;
            }
            latestUpdate = currentTime;

            onWriteEntry(location,currentSchedule);

            Log.d("Tracking Service:", "onLocationChanged: Savinng " + currentTime);

        }

        public void onWriteEntry(Location location, int scheduleId)
        {
            float distance = getDistanceFromNetwork(location);
            mTrackerData.writeEntry(location, distance, scheduleId);

            double dist = distance;

            Intent message = new Intent();
            message.putExtra("dist", dist/1000);
            //messageController.sendMessageToGUI(MainActivity.COMMANDS.WORKOUT_DISTANCE, message);

            message = new Intent();
            message.putExtra("lat", location.getLatitude());
            message.putExtra("lon", location.getLongitude());
            //messageController.sendMessageToGUI(MainActivity.COMMANDS.GPS_POS, message);

        }

        /**
         * Writes update to tracking file
         *
         * @param provider - name of disabled provider
         */
        public void onProviderDisabled(String provider) {
            if (doDebugLogging()) {
                mTrackerData.writeEntry(provider, "provider disabled");
            }
        }

        /**
         * Writes update to tracking file
         * 
         * @param provider - name of enabled provider
         */
        public void onProviderEnabled(String provider) {
            if (doDebugLogging()) {
                mTrackerData.writeEntry(provider,  "provider enabled");
            }
        }

        /**
         * Writes update to tracking file 
         * 
         * @param provider - name of provider whose status changed
         * @param status - new status
         * @param extras - optional set of extra status messages
         */
        public void onStatusChanged(String provider, int status, Bundle extras) {
            if (doDebugLogging()) {
                mTrackerData.writeEntry(provider,  "status change: " + status);
            }
        }
    }

    PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCellLocationChanged(CellLocation location) {
            try {
                if (location instanceof GsmCellLocation) {
                    GsmCellLocation cellLocation = (GsmCellLocation)location;
                    String updateMsg = "cid=" + cellLocation.getCid() +
                            ", lac=" + cellLocation.getLac();
                    mTrackerData.writeEntry(CELL_PROVIDER_TAG, updateMsg);
                } else if (location instanceof CdmaCellLocation) {
                    CdmaCellLocation cellLocation = (CdmaCellLocation)location;
                    String updateMsg = "BID=" + cellLocation.getBaseStationId() +
                            ", SID=" + cellLocation.getSystemId() +
                            ", NID=" + cellLocation.getNetworkId() +
                            ", lat=" + cellLocation.getBaseStationLatitude() +
                            ", long=" + cellLocation.getBaseStationLongitude() +
                            ", SID=" + cellLocation.getSystemId() +
                            ", NID=" + cellLocation.getNetworkId();
                    mTrackerData.writeEntry(CELL_PROVIDER_TAG, updateMsg);
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "Exception in CellStateHandler.handleMessage:", e);
            }
        }

        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            if (mTelephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA) {
                String updateMsg = "cdma dBM=" + signalStrength.getCdmaDbm();
                mTrackerData.writeEntry(SIGNAL_PROVIDER_TAG, updateMsg);
            } else if  (mTelephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM) {
                String updateMsg = "gsm signal=" + signalStrength.getGsmSignalStrength();
                mTrackerData.writeEntry(SIGNAL_PROVIDER_TAG, updateMsg);
            }
        }
    };

    /**
     * Listener + recorder for mobile or wifi updates
     */
    private class NetworkStateBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Integer command = intent.getIntExtra("command", 0);
            String action = intent.getAction();


            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                WifiManager wifiManager =
                    (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                List<ScanResult> wifiScanResults = wifiManager.getScanResults();
                String updateMsg = "num scan results=" +
                    (wifiScanResults == null ? "0" : wifiScanResults.size());
                mTrackerData.writeEntry(WIFI_PROVIDER_TAG, updateMsg);

            } else if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                String updateMsg;
                boolean noConnectivity =
                    intent.getBooleanExtra(
                            ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
                if (noConnectivity) {
                    updateMsg = "no connectivity";
                }
                else {
                    updateMsg = "connection available";
                }
                mTrackerData.writeEntry(DATA_CONN_PROVIDER_TAG, updateMsg);

            } else if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                    WifiManager.WIFI_STATE_UNKNOWN);

                String stateString = "unknown";
                switch (state) {
                    case WifiManager.WIFI_STATE_DISABLED:
                        stateString = "disabled";
                        break;
                    case WifiManager.WIFI_STATE_DISABLING:
                        stateString = "disabling";
                        break;
                    case WifiManager.WIFI_STATE_ENABLED:
                        stateString = "enabled";
                        break;
                    case WifiManager.WIFI_STATE_ENABLING:
                        stateString = "enabling";
                        break;
                }
                mTrackerData.writeEntry(WIFI_PROVIDER_TAG,
                        "state = " + stateString);
            }
        }
    }

    public class TrackerServiceReceiver extends BroadcastReceiver {
        private Context currentContext;

        public TrackerServiceReceiver(Context context) {
            currentContext = context;
        }
        public void onReceive(Context context, Intent intent) {
            Integer command = intent.getIntExtra("command", 0);

            Log.i("TrackerServiceRec", Integer.toString(command));

            switch (command) {
                case TrackerService.SERVICE_ACTION.WORKOUT_START:
                    //currentRoute.start();
                    startLocationListeners();;

                    break;
                case TrackerService.SERVICE_ACTION.WORKOUT_PAUSE:
                    //currentRoute.pause();
                    break;
                case TrackerService.SERVICE_ACTION.WORKOUT_STOP:
                    //currentRoute.stop();
                    stopListeners();
                    //stopForegroundService();
                    break;
                case TrackerService.SERVICE_ACTION.WORKOUT_UNPAUSE:
                    //currentRoute.unPause();
                    break;
                case TrackerService.SERVICE_ACTION.WORKOUT_STATUS:
                    //stopForegroundService();
                    sendRouteStatus();
                    break;
                case TrackerService.SERVICE_ACTION.WORKOUT_ID:
                    //Intent message = new Intent();
                    /*
                    if (currentRoute.getStatus() == DefaultValues.routeStatus.start || currentRoute.getStatus() == DefaultValues.routeStatus.pause)
                        message.putExtra("workoutId", currentRoute.getCurrentId());
                    else
                        message.putExtra("workoutId", -1);
                    */
                    //messageController.sendMessageToGUI(MainActivityReceiver.COMMANDS.WORKOUT_ID, message);

                    break;
                case SERVICE_ACTION.WORKOUT_STARTFOREGROUND:
                    //startForegroundService();
                    break;

                case SERVICE_ACTION.WORKOUT_CHECK_SCHEDULE:
                    Log.d("From Alarm","Checking Schedule...");
                    if (!getRunning()) {
                        int currentSchedule = getCurrentSchedule();
                        if (currentSchedule != 0) {
                            startLocationListeners();
                            return;
                        }
                    }


                    break;

                default:
                   // currentRoute.stop();


                    break;

            }
            sendRouteStatus();
        }

        public void sendRouteStatus() {


            Intent message = new Intent();
            //message.putExtra("command", MainActivityReceiver.COMMANDS.WORKOUT_STATUS);
            //if (runningcurrentRoute.getStatus() == DefaultValues.routeStatus.pause)
            //    message.putExtra("status", 2);

            if (!running)  //Stop
                message.putExtra("status", 0);
            if (running)  //Status start
                message.putExtra("status", 1);

          //  message.setAction(MainActivity.MAINACTIVITY_ACTION);
          //  currentContext.sendBroadcast(message);


            //messageController.sendMessageToGUI(MainActivity.COMMANDS.WORKOUT_STATUS,message);

        }
    }

    private class PreferenceListener implements OnSharedPreferenceChangeListener {

        public void onSharedPreferenceChanged(
                SharedPreferences sharedPreferences, String key) {
            Log.d(LOG_TAG, "restarting listeners due to preference change");
            synchronized (TrackerService.this) {
                stopListeners();
                startLocationListeners();
            }
        }
    }


    public static class SERVICE_ACTION {
        public static final int WORKOUT_STOP = 0;
        public static final int WORKOUT_START = 1;
        public static final int WORKOUT_PAUSE = 2;
        public static final int WORKOUT_STATUS = 3;
        public static final int WORKOUT_UNPAUSE = 4;
        public static final int WORKOUT_ID = 5;
        public static final int WORKOUT_STARTFOREGROUND = 6;
        public static final int WORKOUT_CHECK_SCHEDULE = 7;


    }

    /**
     * Starts the service as a foreground service.
     *
     * @param //pendingIntent the notification pending intent
     * @param //messageId the notification message id
     */





}