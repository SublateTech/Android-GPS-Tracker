package com.sublate.gpstracker;


import android.app.Service;
import android.content.Context;
import android.content.Intent;

public class GuiServiceMessageController extends ServiceMessageController {

    public GuiServiceMessageController(Context context, final Class<? extends Service> className) {
        super(context, className);
    }

    @Override
    public void sendMessageToGUI(String command, Intent intent) {
        intent.putExtra("command", command);
       // intent.setAction(MainActivity.MAINACTIVITY_ACTION);
        getContext().sendBroadcast(intent);
    }

    public void sendAreaNotification(String message){

        /*
        AreaNotificationManager notificationManager = new AreaNotificationManager(getContext(), R.drawable.ic_notif, getContext().getString(R.string.app_name));
        notificationManager.setContent(message);
        notificationManager.sendNotify();
        */

    }

}
