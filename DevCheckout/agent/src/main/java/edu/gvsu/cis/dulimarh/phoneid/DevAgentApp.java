package edu.gvsu.cis.dulimarh.phoneid;

import android.app.Application;

import com.parse.Parse;

/**
 * Created by dulimarh on 4/18/15.
 */
public class DevAgentApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Parse.initialize(this, "aLvdYW4Md0neSVfgTxiqsmRSo5sIPJYHDeVcYO3i",
                "CvF1F4yjlyr8X42eE5PXWa0mhMNVKoHhcIwIeSrg");
        Parse.setLogLevel(Parse.LOG_LEVEL_VERBOSE);
    }
}
