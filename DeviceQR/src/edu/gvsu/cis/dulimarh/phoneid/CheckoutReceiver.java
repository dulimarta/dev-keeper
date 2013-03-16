package edu.gvsu.cis.dulimarh.phoneid;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class CheckoutReceiver extends BroadcastReceiver {
    private final String TAG = getClass().getName();
    
    
    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification alert = new Notification(R.drawable.ic_launcher, "Checkout Message",
                System.currentTimeMillis());
        alert.setLatestEventInfo(context, "From Checkout", 
                intent.getExtras().getString("com.parse.Data"), null);
        nm.notify(TAG.hashCode(), alert);
    }

}
