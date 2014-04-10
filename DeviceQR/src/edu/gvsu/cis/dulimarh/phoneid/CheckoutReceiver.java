package edu.gvsu.cis.dulimarh.phoneid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.json.JSONException;
import org.json.JSONObject;

public class CheckoutReceiver extends BroadcastReceiver {
    private final String TAG = getClass().getName();
    
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent localIntent = new Intent(context.getPackageName() + ".HansLocalBroadcast");
        try {
            JSONObject jObj = new JSONObject(intent.getExtras().getString("com.parse.Data"));
            localIntent.putExtra("message", jObj.getString("message"));
            context.sendBroadcast(localIntent);
        } catch (JSONException e) {
            e.printStackTrace();
        }
//        Notification.Builder noteBuilder = new Notification.Builder(context);
//        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//        noteBuilder.setSmallIcon(R.drawable.ic_thumbup);
//        noteBuilder.setContentTitle("Parse push");
//        noteBuilder.setContentText("Registered " + intent.getAction());
//        nm.notify(0xC0DE, noteBuilder.getNotification());
    }

}
