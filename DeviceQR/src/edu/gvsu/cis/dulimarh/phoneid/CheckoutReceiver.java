package edu.gvsu.cis.dulimarh.phoneid;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class CheckoutReceiver extends BroadcastReceiver {
    private final String TAG = getClass().getName();
    
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Notification.Builder noteBuilder = new Notification.Builder(context);
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        noteBuilder.setSmallIcon(R.drawable.ic_thumbup);
        noteBuilder.setContentTitle("Parse push");
        noteBuilder.setContentText("Registered " + intent.getAction());
        nm.notify(0xC0DE, noteBuilder.getNotification());
//        Notification alert = new Notification(R.drawable.ic_launcher, "Checkout Message",
//                System.currentTimeMillis());
//        alert.setLatestEventInfo(context, "From Checkout", 
//                intent.getExtras().getString("com.parse.Data"), null);
//        nm.notify(TAG.hashCode(), alert);
    }

}
