package edu.gvsu.cis.dulimarh.checkout;

/**
 * Created by dulimarh on 03/16/14.
 */
public interface Consts {
    public static final String USER_TABLE = "Users";
    public static final String DEVICE_LOAN_TABLE = "DevOut";
    public static final String ALL_DEVICE_TABLE = "Devices";
    public static final String LOG_TABLE = "ChekoutLog";
//   The following are Hans' id and client key
    public static final String PARSE_APP_ID =
            "C8rZuZwkSAFnH2vmgCn4mrtkyh89qFmqXw8pIPpB";
    public static final String PARSE_CLIENT_KEY =
            "LmGNHi2nXy2BoZfoLiBohrGVX5lgZqyIp4ft0n2A";
//   The following are Jonathan's id and client key
//    public static final String PARSE_APP_ID =
//            "3k6jFJ5IYcetRe6tQ24yGms0P78RQuQYXy48idyP";
//    public static final String PARSE_CLIENT_KEY =
//            "yJWzDV3D3p2itdpc86rEYkcpVOLf6pQqdU05qa3m";
    public static final String PUSH_CHANNEL = "HansDulimarta";

    public static final int ACTION_SELECT_USER = 1;
}
