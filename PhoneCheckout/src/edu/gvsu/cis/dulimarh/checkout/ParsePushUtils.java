package edu.gvsu.cis.dulimarh.checkout;

import com.parse.ParseInstallation;
import com.parse.ParsePush;
import com.parse.ParseQuery;

/**
 * Created by dulimarh on 04/09/14.
 */
public class ParsePushUtils {
    public static void pushTo (String id, String msg)
    {
        ParsePush pushNotification = new ParsePush();
        ParseQuery pushQuery = ParseInstallation.getQuery();
        pushQuery.whereEqualTo("dev_id", id);

        pushNotification.setQuery(pushQuery);
        pushNotification.setChannel(Consts.PUSH_CHANNEL);
        pushNotification.setMessage(msg);
        pushNotification.sendInBackground();

    }
}
