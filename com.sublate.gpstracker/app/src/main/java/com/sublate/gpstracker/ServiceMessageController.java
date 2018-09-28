package com.sublate.gpstracker;


import android.app.Service;
import android.content.Context;
import android.content.Intent;

public abstract class ServiceMessageController {
    private String mAction;
    private Context mContext;
    private Class<? extends Service> mClassName;

    public ServiceMessageController(Context context) {
        mContext = context;
    }

    public ServiceMessageController(Context context, final Class<? extends Service> className) {
        mContext = context;
        mClassName = className;
        mAction =  mClassName.getSimpleName()+"Action";
    }

    public String getServiceActionName()
    {
        return mAction;
    }

    public Context getContext() {
        return mContext;
    }

    public void sendMessageToService(Integer messageAction) {
        Intent message = new Intent();
        message.putExtra("command", messageAction);
        message.setAction(getServiceActionName());
        mContext.sendBroadcast(message);
    }

    abstract public void sendMessageToGUI(String command, Intent intent) ;

    public void sendMessageToGUI(String command) {
        sendMessageToGUI(command, new Intent());
    }
}
