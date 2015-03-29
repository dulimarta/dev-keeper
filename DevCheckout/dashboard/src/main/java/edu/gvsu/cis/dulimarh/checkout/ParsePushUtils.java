package edu.gvsu.cis.dulimarh.checkout;

import android.util.Log;

import com.parse.ParseInstallation;
import com.parse.ParsePush;
import com.parse.ParseQuery;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by dulimarh on 04/09/14.
 */
public class ParsePushUtils {
    public static void pushTo (String id, String msg)
    {
        ParsePush pushNotification = new ParsePush();
        ParseQuery pushQuery = ParseInstallation.getQuery();
        pushQuery.whereEqualTo("dev_id", id);

        try {
            JSONObject data = new JSONObject("{\"action\":\"edu.gvsu.cis.checkout.UPDATE\"," +
                    "\"message\":\"" + msg + "\"}");
            pushNotification.setQuery(pushQuery);
            pushNotification.setData(data);
            /* expiration time is currently ignored by parse.com??? */
            pushNotification.setExpirationTimeInterval(180); /* expire in 3 minutes */
            pushNotification.sendInBackground();
        } catch (JSONException e) {
            Log.e("HANS", "Unable to parse JSON string: " + e.getMessage());
        }

    }
}
