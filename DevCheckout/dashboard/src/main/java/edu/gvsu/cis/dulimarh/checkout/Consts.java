package edu.gvsu.cis.dulimarh.checkout;

/**
 * Created by dulimarh on 03/16/14.
 */
public interface Consts {
    public static final String USER_TABLE = "Users";
    public static final String DEVICE_LOAN_TABLE = "DevOut";
    public static final String ALL_DEVICE_TABLE = "Devices";
    public static final String LOG_TABLE = "CheckoutLog";
//   The following are Hans' id and client key
    public static final String PARSE_APP_ID =
            "aLvdYW4Md0neSVfgTxiqsmRSo5sIPJYHDeVcYO3i";
    public static final String PARSE_CLIENT_KEY =
            "CvF1F4yjlyr8X42eE5PXWa0mhMNVKoHhcIwIeSrg";
//   The following are Jonathan's id and client key
//    public static final String PARSE_APP_ID =
//            "3k6jFJ5IYcetRe6tQ24yGms0P78RQuQYXy48idyP";
//    public static final String PARSE_CLIENT_KEY =
//            "yJWzDV3D3p2itdpc86rEYkcpVOLf6pQqdU05qa3m";
    public static final String PUSH_CHANNEL = "HansDulimarta";

    public static final int ACTION_SELECT_USER_FOR_CHECKOUT = 1;
}
