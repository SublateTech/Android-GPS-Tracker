package com.sublate.gpstracker;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.text.StaticLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.sublate.gpstracker.Tracker.ContinuousGps;

public class MainActivity extends Activity {

	TextView mainTextView = null;
	TextView mainTextView2 = null;

	private Timer timer = null;
	private Handler handler = null;

	private ServiceConnection conn = null;
	private DateFormat dayFormat = DateFormat.getDateInstance(DateFormat.MEDIUM); 
	
  	public static final String ACTION_UPDATE = "com.lzmy.tellmewakeandlock.action.update";
  	public static final String ACTION_GET_HISTORY = "com.lzmy.tellmewakeandlock.action.get_history";
 	public static final String ACTION_GET_LIST = "com.lzmy.tellmewakeandlock.action.get_list";
	
	
 	UpdateReceiver mUpdateReceiver = new UpdateReceiver();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initView();

		Intent newIntent = new Intent(this, WakeLockTimeService.class);
	    newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    startService(newIntent); 
	    
	    IntentFilter filter = new IntentFilter();
	    filter.addAction(WakeLockTimeService.ACTION_SEND_LIST);
	    filter.addAction(WakeLockTimeService.ACTION_UPDATE);
	    filter.addAction(WakeLockTimeService.ACTION_SEND_HISTORY);
	    filter.addAction(WakeLockTimeService.ACTION_INIT_FINISHED);

	    registerReceiver(mUpdateReceiver, filter);

		/*
		ContinuousGps mContinuousGpsG = new ContinuousGps(this);
		long alarm = 1000*15; //5 minute
		mContinuousGpsG.setContinuous((long) 0, alarm);
		*/
	}
	
	private void initView(){
		mainTextView = (TextView)findViewById(R.id.mainText);
		mainTextView2 = (TextView)findViewById(R.id.mainText2);
		
		handler = new Handler(){

			@Override
			public void handleMessage(Message msg) {
				// TODO Auto-generated method stub
				if(msg.what == 0x123){
				}
			}
			
		};
		Log.d("MainActivity", "initView finished");
	}
	
	private void refreshList(){

		Intent intent = new Intent(MainActivity.this, WakeLockTimeService.class);
		intent.setAction(ACTION_GET_LIST);
		startService(intent);
	}
	
	private void refreshMap(){

		Intent intent = new Intent(MainActivity.this, WakeLockTimeService.class);
		intent.setAction(ACTION_GET_HISTORY);
		startService(intent);
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		// TODO Auto-generated method stub
		switch(item.getItemId()){
			case R.id.get_history:
				refreshMap();
			    break;
			case R.id.renren_share:
				refreshMap();
				long today_time = 0; 


				Log.d("MainActivity", "start ShareRenrenActivity");
				break;
		}
		return super.onMenuItemSelected(featureId, item);
	}
	
	
	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
//		timer = new Timer();
//		timer.schedule(new TimerTask() {
//			
//			@Override
//			public void run() {
//				// TODO Auto-generated method stub
//				handler.sendEmptyMessage(0x123);
//				}
//		}, 60000);
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
//		timer.cancel();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
//		if(serviceBinder != null){
//			mainTextView.setText(ListToString(list));
//		}
		refreshList();
	}


	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		unregisterReceiver(mUpdateReceiver);
//		unbindService(conn);
		super.onDestroy();
	}


	public class UpdateReceiver extends BroadcastReceiver{

		@SuppressWarnings("unchecked")
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			Log.d("MainActivity", intent.getAction());
			if(intent.getAction().equals(WakeLockTimeService.ACTION_SEND_LIST)){
				Log.d("MainActivity", "get_update_intent");
			}else if(intent.getAction().equals(WakeLockTimeService.ACTION_SEND_HISTORY)){
				Log.d("MainActivity", "get_update_history");
			}else if(intent.getAction().equals(WakeLockTimeService.ACTION_INIT_FINISHED)){
				
				conn = new ServiceConnection() {
					
					@Override
					public void onServiceDisconnected(ComponentName name) {
						// TODO Auto-generated method stub

						Log.d("MainActivity", "WLTimeService ServiceDisconnected");
					}
					
					@Override
					public void onServiceConnected(ComponentName name, IBinder service) {
						// TODO Auto-generated method stub

						Log.d("MainActivity", "WLTimeService ServiceConnected");
					}
				};
				
				bindService(new Intent(MainActivity.this, WakeLockTimeService.class), conn,  0);
				
			}
		}

	}
	
	@SuppressWarnings("rawtypes")
	private CharSequence MapToString(HashMap<String, Long> map) {
		if(map == null){
			return null;
		}
		String todayString = dayFormat.format(System.currentTimeMillis());
		StringBuilder builder = new StringBuilder();
		builder.append("--------------\n");
		Iterator iter = map.entrySet().iterator();  
		String key = null;
		long val = 0;
		while (iter.hasNext()) {  
		    Map.Entry entry = (Map.Entry) iter.next();  
		    key = (String)entry.getKey(); 
		    if(key.equals(todayString)){
		    	continue;
		    }
		    val = (Long)entry.getValue();  
		    builder.append(key+" "+val/60000+"min\n");
		}  
		
		return builder.toString();
	}
	

	

	
}
