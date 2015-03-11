package edu.gvsu.cis.dulimarh.checkout;

import android.app.Application;

import com.parse.Parse;

public class DevCheckoutApp extends Application {

    /* (non-Javadoc)
     * @see android.app.Application#onCreate()
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Parse.initialize(this, Consts.PARSE_APP_ID, Consts.PARSE_CLIENT_KEY);
        Parse.setLogLevel(Parse.LOG_LEVEL_DEBUG);
    }
    
}
