package com.sublate.gpstracker;

import android.content.BroadcastReceiver; 
import android.content.Context; 
import android.content.Intent; 
 
public class BootCompletedReceiver extends BroadcastReceiver { 
  @Override 
  public void onReceive(Context context, Intent intent) {

    if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) 
    { 
      Intent newIntent = new Intent(context, WakeLockTimeService.class);
      newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      context.startService(newIntent);       
    }

  } 
} 