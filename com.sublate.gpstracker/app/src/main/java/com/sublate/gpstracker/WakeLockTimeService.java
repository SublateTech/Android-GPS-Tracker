package com.sublate.gpstracker;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.sublate.gpstracker.Tracker.ScreenActionReceiver;
import com.sublate.gpstracker.Tracker.TrackerService;

public class WakeLockTimeService extends Service {
	/**
	 * It can start when device starts.
	 * It can save one_day screen_on and screen_off time.
	 * Also,it can save your use time everyday;
	 * @author hzy
	 * @since 1.0
	 */


	public static final String TAG = "WakeLockTimeService";
	public static final String ACTION_UPDATE = "com.lzmy.tellmewakeandlock.action.update";
	public static final String ACTION_SEND_HISTORY = "com.lzmy.tellmewakeandlock.action.send_history";
	public static final String ACTION_SEND_LIST = "com.lzmy.tellmewakeandlock.action.send_list";
	public static final String ACTION_INIT_FINISHED = "com.lzmy.tellmewakeandlock.action.init_finished";

	private boolean screenopen = false;

	private Context mAppContext = null;
	private ScreenActionReceiver mScreenActionReceiver = null;



	SharedPreferences sharedPreferences = null;
	SharedPreferences.Editor editor = null;


	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub

		 IBinder x = null;
		return x ;
	}
	

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		mAppContext = getApplicationContext();

		screenopen = true;

		mScreenActionReceiver = new ScreenActionReceiver();
		mScreenActionReceiver.setAlarm(mAppContext);


		mScreenActionReceiver.registerScreenActionReceiver(mAppContext);


		/*
		Intent intent = new Intent(WakeLockTimeService.this, MainActivity.class);
		intent.setAction(ACTION_INIT_FINISHED);
		sendBroadcast(intent);
		*/

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		DateFormat mFormat = DateFormat.getInstance();
		if(intent == null){
			Log.d(TAG, "intent is null!");
			return super.onStartCommand(intent, flags, startId);
		}
		if(intent.getAction() == null){
			return super.onStartCommand(intent, flags, startId);
		}

		if((intent.getAction().equals(ScreenActionReceiver.SCREEN_ON_TIME)
				|| intent.getAction().equals(ScreenActionReceiver.USER_PRESENT_TIME))
				&& screenopen == false){
			Log.d(TAG, "startTime--new---");
			screenopen = true;

		}else if(intent.getAction().equals(ScreenActionReceiver.SCREEN_OFF_TIME)
					&& screenopen == true){
			screenopen = false;

			Log.d(TAG, "endTime--new---");
			Log.d(TAG, "save the list");

		}else if (intent.getAction().equals(MainActivity.ACTION_GET_HISTORY)){
			Intent mIntent = new Intent();
			mIntent.setAction(ACTION_SEND_HISTORY);
			mIntent.putExtra("current_history", "");
			sendBroadcast(mIntent);
			Log.d(TAG, "send current history");

		}else if(intent.getAction().equals(MainActivity.ACTION_GET_LIST)){
			Intent mIntent = new Intent();
			mIntent.setAction(ACTION_SEND_LIST);
 			mIntent.putExtra("current_list", "");
			sendBroadcast(mIntent);
			Log.d(TAG, "send current list");
		}

		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub


		Log.d(TAG, "save the list");
		
		mScreenActionReceiver.unRegisterScreenActionReceiver(getApplicationContext());
		super.onDestroy();
	}

}

