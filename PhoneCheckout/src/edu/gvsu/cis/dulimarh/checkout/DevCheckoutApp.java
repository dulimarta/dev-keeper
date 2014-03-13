package edu.gvsu.cis.dulimarh.checkout;

import com.parse.Parse;

import android.app.Application;

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
    }
    
}
