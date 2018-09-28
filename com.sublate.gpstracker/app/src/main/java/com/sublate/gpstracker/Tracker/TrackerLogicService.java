package com.sublate.gpstracker.Tracker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.sublate.gpstracker.GuiServiceMessageController;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Alvaro on 9/14/2016.
 */
public class TrackerLogicService extends Service {
    private TrackerLogicBroadcastReceiver mReceiver;
    private GuiServiceMessageController messageController;
    private AlarmManager alarmManager;
    private Context mContext;
    private PendingIntent pendingIntent;
    private IBinder mBinder;
    private Intent serviceIntent;


    private static int startTimeHour = 8; //Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
    private static int startTimeMinute = 0; //Calendar.getInstance().get(Calendar.MINUTE);

    private static int endTimeHour = 20; //Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
    private static int endTimeMinute = 0; //Calendar.getInstance().get(Calendar.MINUTE);



    private boolean ifShouldBeOn()
    {

        long startMiliSecsDate = milliseconds(startTimeHour,startTimeMinute);
        long endMiliSecsDate = milliseconds(endTimeHour,endTimeMinute);
        long nowMiliSecsDate = new Date().getTime();

        if (startMiliSecsDate <= nowMiliSecsDate && nowMiliSecsDate <=endMiliSecsDate )
            return true;
        else
            return false;
    }

    public static String getCurrentTimeStamp() {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd"); //SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); //dd/MM/yyyy
        Date now = new Date();
        String strDate = sdfDate.format(now);
        return strDate;
    }

    public long milliseconds(String date)
    {
        //String date_ = date;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try
        {
            Date mDate = sdf.parse(date);
            long timeInMilliseconds = mDate.getTime();
            System.out.println("Date in milli :: " + timeInMilliseconds);
            return timeInMilliseconds;
        }
        catch (java.text.ParseException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return 0;
    }

    public long milliseconds(int hour, int minute)
    {
        Calendar calendar= Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 00);
        return calendar.getTimeInMillis();
    }

    private void setAlarm(){


        // Set the alarm to start at 21:32 PM
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, startTimeHour);
        calendar.set(Calendar.MINUTE, startTimeMinute);

        // setRepeating() lets you specify a precise custom interval--in this case,
        // 1 day
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, pendingIntent);
    }
    private void cancelAlarm() {
        if (alarmManager!= null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        messageController = new GuiServiceMessageController(this, TrackerLogicService.class);
        mReceiver = new TrackerLogicBroadcastReceiver(this);

        mContext = getApplicationContext();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(messageController.getServiceActionName());
        registerReceiver(mReceiver, intentFilter);

        if (ifShouldBeOn())
            Log.d("TAG", "entering schedule...");
        else
            Log.d("TAG", "leaving schedule...");

//        startSchedule();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("GPSRunnerService", "Received start id " + startId + ": " + intent);
        return START_STICKY_COMPATIBILITY;
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "Service Stop", Toast.LENGTH_SHORT).show();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void startSchedule()
    {
        /*  ELAPSED_REALTIME : It considers the amount of time since the phone is booted and it does not wake the device.
            ELAPSED_REALTIME_WAKEUP : Same as ELAPSED_REALTIME, but phone wakes up.
            RTC : It is used to fire pending intent at specified time but phone does not wake up.
            RTC_WAKEUP : Same as RTC but phone wakes up.
        */

        Intent myIntent = new Intent(mContext, TrackerService.class);

        pendingIntent = PendingIntent.getService(mContext, 0, myIntent, 0);
        alarmManager = (AlarmManager)mContext.getSystemService(mContext.ALARM_SERVICE);
        Calendar calendar = Calendar.getInstance();

        /*
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.SECOND, 10);
        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        */
        setAlarm();
        Toast.makeText(mContext, "Start Alarm", Toast.LENGTH_LONG).show();
    }

    private void stopSchedule()
    {
        cancelAlarm();
        // Tell the user about what we did.
        Toast.makeText(mContext, "Cancel!", Toast.LENGTH_LONG).show();
    }

    private class TrackerLogicBroadcastReceiver extends BroadcastReceiver {
        private Context currentContext;

        public TrackerLogicBroadcastReceiver(Context context) {
            currentContext = context;
        }

        @Override
        public void onReceive(Context context, Intent intent)
        {
            mContext = context;
            String action = intent.getAction();
            Integer command = intent.getIntExtra("command", 0);
            Log.i("TrackerLogicService.Rec", Integer.toString(command));
            switch (command) {
                case SERVICE_ACTION.WORKOUT_START:
                    break;
                case SERVICE_ACTION.WORKOUT_STOP:

                    break;
                default:
                    stopSchedule();
                    break;
            }
        }
    }

    public static class SCHEDULE_ACTION {
        public static final int SCHEDULE_STOP = 0;
        public static final int SCHEDULE_START = 1;
        public static final int SCHEDULE_STATUS = 3;
        public static final int SCHEDULE_ID = 4;
    }
    public static class SERVICE_ACTION {
        public static final int WORKOUT_STOP = 0;
        public static final int WORKOUT_START = 1;
        public static final int WORKOUT_PAUSE = 2;
        public static final int WORKOUT_STATUS = 3;
        public static final int WORKOUT_UNPAUSE = 4;
        public static final int WORKOUT_ID = 5;
    }


}


