package com.sublate.gpstracker.Tracker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.sublate.gpstracker.BootCompletedReceiver;
import com.sublate.gpstracker.WakeLockTimeService;

public class ContinuousGps 
{
	private Context mContext;
	private AlarmManager mAlarmManager;
	private PendingIntent pi;
	
	public ContinuousGps(Context context) {
		mContext = context; 
		mAlarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(mContext, BootCompletedReceiver.class);
		pi = PendingIntent.getBroadcast(mContext, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

	}// end of setup alarm manager
	public void cancelContinuous(){ mAlarmManager.cancel(pi); }
	
	public void setContinuous(Long taskId, long alarmtime) {
		        
        mAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000,
        		alarmtime, pi);
	}
}//end of continuous gps locations class
