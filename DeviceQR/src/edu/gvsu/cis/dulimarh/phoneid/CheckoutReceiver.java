package edu.gvsu.cis.dulimarh.phoneid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.json.JSONException;
import org.json.JSONObject;

public class CheckoutReceiver extends BroadcastReceiver {

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
    }

}
