package edu.gvsu.cis.dulimarh.checkout;

import android.app.Application;

import com.parse.Parse;

public class DevCheckoutApp extends Application {

    /* (non-Javadoc)
     * @see android.app.Application#onCreate()
     */
    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        Parse.initialize(this, "AGs2nPlOxM7rA1BnUAbeVySTSRud6EhL7JF8sd4f",
                "z5CgnppcixOqpAzHOdnTfT6ktKKzk6aicH8p1Rvb");
        Parse.setLogLevel(Parse.LOG_LEVEL_VERBOSE);
    }
    
}
