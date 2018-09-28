package com.sublate.gpstracker.Tracker;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.sublate.gpstracker.GuiServiceMessageController;
import com.sublate.gpstracker.WakeLockTimeService;

/**
 * Created by Alvaro on 9/18/2016.
 */
public class ScreenActionReceiver extends BroadcastReceiver {

    public static final String SCREEN_ON_TIME = "com.lzmy.tellmewakeandlock.action.SCREEN_ON_TIME";
    public static final String SCREEN_OFF_TIME = "com.lzmy.tellmewakeandlock.action.SCREEN_OFF_TIME";
    public static final String USER_PRESENT_TIME = "com.lzmy.tellmewakeandlock.action.USER_PRESENT_TIME";

    private String TAG = "ScreenActionReceiver";
    private boolean isRegisterReceiver = false;

    private GuiServiceMessageController messageController;


    @Override
    public void onReceive(Context context, Intent intent) {


        messageController = new GuiServiceMessageController(context, TrackerService.class);


        String action = intent.getAction();
        if(action == null){
            Log.d(TAG, "action is null!");
            return ;
        }


        if (action.equals(Intent.ACTION_SCREEN_ON)) {
            Log.d(TAG, "Secreen On...");
        /*
            Intent mIntent = new Intent();
            mIntent.setClass(context, WakeLockTimeService.class);
            mIntent.setAction(SCREEN_ON_TIME);
            mIntent.putExtra("on_time", System.currentTimeMillis());
            context.startService(mIntent);
            */
        }else if(action.equals(Intent.ACTION_USER_PRESENT)){
            Log.d(TAG, "Preset...");
            /*
            Intent mIntent = new Intent();
            mIntent.setClass(context, WakeLockTimeService.class);
            mIntent.setAction(USER_PRESENT_TIME);
            mIntent.putExtra("on_time", System.currentTimeMillis());
            context.startService(mIntent);
            */
        } else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
            Log.d(TAG, "Screen Off...");
            /*
            Intent mIntent = new Intent();
            mIntent.setClass(context, WakeLockTimeService.class);
            mIntent.setAction(SCREEN_OFF_TIME);
            mIntent.putExtra("off_time", System.currentTimeMillis());
            context.startService(mIntent);
            */
        } else if (action.equals(WakeLockTimeService.ACTION_UPDATE)) {
            Intent serviceIntent = new Intent(context, TrackerService.class);
            Log.d(TAG, "Update checking...");
            if (!isMyServiceRunning(TrackerService.class, context)) {
                serviceIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startService(serviceIntent);

                Log.d("OnAlarmReceiver", "onReceive: " + "Starting Service...");
            }
        }




        messageController.sendMessageToService(TrackerService.SERVICE_ACTION.WORKOUT_CHECK_SCHEDULE);



    }

    private boolean isMyServiceRunning(Class<?> serviceClass, Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void registerScreenActionReceiver(Context mContext) {
        if (!isRegisterReceiver) {
            isRegisterReceiver = true;

            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_USER_PRESENT);
            Log.d(TAG, "Register Receiver...");
            mContext.registerReceiver(ScreenActionReceiver.this, filter);
        }
    }

    public void unRegisterScreenActionReceiver(Context mContext) {
        if (isRegisterReceiver) {
            isRegisterReceiver = false;
            Log.d(TAG, "UnRegister Receiver...");
            mContext.unregisterReceiver(ScreenActionReceiver.this);
        }
    }

    public void setAlarm(Context context)
    {
        AlarmManager am =( AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, ScreenActionReceiver.class);
        i.setAction(WakeLockTimeService.ACTION_UPDATE);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * 15, pi); // Millisec * Second * Minute
    }

    public void cancelAlarm(Context context)
    {
        Intent intent = new Intent(context, ScreenActionReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }

}