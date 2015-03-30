package edu.gvsu.cis.dulimarh.phoneid;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.PushService;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.List;

import bolts.Continuation;
import bolts.Task;

public class IMEI2QRActivity extends Activity implements OnClickListener {
    private final String TAG = getClass().getName();
    private static final String CHART_URL = 
        "http://chart.apis.google.com/chart?cht=qr&chld=L&choe=UTF-8";
    private static final int DOWNLOAD_DIALOG = 0;
    private static final int NETWORK_ERROR_DIALOG = 1;

    private LinearLayout top;
    private TextView id, user;
    private ImageView qr, signature;
    private ProgressDialog progress;
    private String wifiID, userId;
    private JSONObject jsonID;
    private Bitmap qrCodeImg, signatureImg;
    private URLTask myTask;
    

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Parse.initialize(this, "aLvdYW4Md0neSVfgTxiqsmRSo5sIPJYHDeVcYO3i",
                "CvF1F4yjlyr8X42eE5PXWa0mhMNVKoHhcIwIeSrg");
        Parse.setLogLevel(Parse.LOG_LEVEL_VERBOSE);
        ParseInstallation thisInstall = ParseInstallation.getCurrentInstallation();
        Log.d("HANS", "Installation ID is " + thisInstall.getInstallationId());
        setContentView(R.layout.main);
        top = (LinearLayout) findViewById(R.id.topLayout);
        id = (TextView) findViewById(R.id.id);
        user = (TextView) findViewById(R.id.user);
        qr = (ImageView) findViewById(R.id.qr_code);
        signature = (ImageView) findViewById(R.id.signature_image);
        qr.setOnClickListener(this);
        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        wifiID = wm.getConnectionInfo().getMacAddress();
        try {
            if (savedInstanceState != null) {
                userId = savedInstanceState.getString("userId");
                jsonID = new JSONObject(savedInstanceState.getString("devId"));
                qrCodeImg = savedInstanceState.getParcelable("qrcode");
                signatureImg = savedInstanceState.getParcelable("signature");
                qr.setImageBitmap(qrCodeImg);
                id.setText(wifiID);
                if (userId.length() > 0) {
                    user.setText("checked out by " + userId);
                    signature.setImageBitmap(signatureImg);
                    signature.setVisibility(View.VISIBLE);
                } else {
                    user.setText("is available");
                    signature.setVisibility(View.GONE);
                }
            } else {
                jsonID = new JSONObject();
            /* required field: "id", "model", "os", "form_factor" */
                boolean isTablet = getResources().getBoolean(R.bool
                        .isTablet);
                jsonID.put("id", wifiID);
                jsonID.put("model", Build.MODEL + " " + Build.MANUFACTURER);
                jsonID.put("os", "Android " + Build.VERSION.RELEASE);
                jsonID.put("form_factor", isTablet ? "Tablet" : "Phone");
                qrCodeImg = null;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        id.setText(wifiID);
        thisInstall.put("dev_id", wifiID);
        thisInstall.saveInBackground()
                .continueWith(new Continuation<Void, Object>() {
                    @Override
                    public Object then(Task<Void> task) throws Exception {
                        if (task.isFaulted()) {
                            Log.d("HANS", "Can't update installation " +
                                    "data " + task.getError().getMessage
                                    ());
                        }
                        return null;
                    }
                }, Task.UI_THREAD_EXECUTOR);
//        PushService.setDefaultPushCallback(this, IMEI2QRActivity.class);
//        ParsePush.subscribeInBackground("")
//                .onSuccess(new Continuation<Void, Object>() {
//                    @Override
//                    public Object then(Task<Void> task) throws Exception {
//                        Toast.makeText(IMEI2QRActivity.this,
//                                "Successful ParsePush subscription",
//                                Toast.LENGTH_LONG).show();
//                        return null;
//                    }
//                }, Task.UI_THREAD_EXECUTOR)
//                .continueWith(new Continuation<Object, Object>() {
//                    @Override
//                    public Object then(Task<Object> task) throws Exception {
//                        if (task.isFaulted()) {
//                            Log.d("HANS", "ParsePush subscription error " +
//                                    "" + task.getError().getMessage());
//                            Toast.makeText(IMEI2QRActivity.this,
//                                    "Unable to subscribe to ParsePush "
//                                            + task.getError().getMessage(),
//                                    Toast.LENGTH_LONG).show();
//
//                        }
//                        return null;
//                    }
//                }, Task.UI_THREAD_EXECUTOR);
    }
    
    
    /* (non-Javadoc)
     * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("qrcode", qrCodeImg);
        outState.putParcelable("signature", signatureImg);
        outState.putString("userId", userId);
        outState.putString("devId", jsonID.toString());
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(localReceiver, new IntentFilter(getPackageName() + ".HansLocalBroadcast"));
//        if (userId == null || userId.length() == 0) {
//            top.setBackgroundResource(R.color.background_avail);
//        }
//        else
//            top.setBackgroundResource(R.color.background_onloan);
        if (qrCodeImg != null) return;
        if (isNetworkAvailable()) {
            myTask = new URLTask();
            myTask.execute();
        } else {
            showDialog(NETWORK_ERROR_DIALOG);
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(localReceiver);
    }

    /* (non-Javadoc)
         * @see android.app.Activity#onCreateDialog(int)
         */
    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder;
        switch (id) {
        case DOWNLOAD_DIALOG:
            progress = new ProgressDialog(this);
            progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progress.setOnCancelListener(new OnCancelListener() {

                @Override
                public void onCancel(DialogInterface dialog) {
                    myTask.cancel(true);
                    finish();
                }
            });
            progress.setTitle("Please wait ...");
            return progress;
        case NETWORK_ERROR_DIALOG:
            builder = new AlertDialog.Builder(this);
            builder.setTitle("Network Error");
            builder.setMessage("This app requires Internet connection. "
                    + "Please setup your network and try again.");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            return builder.create();
        default:
            return null;
        }
    }

    private class URLTask extends AsyncTask<Void, Void, Object> {

        @Override
        protected Object doInBackground(Void... params) {
            HttpClient client = new DefaultHttpClient();
            Display d = getWindowManager().getDefaultDisplay();
            int dim = Math.min(d.getWidth(), d.getHeight()) - 150;
            if (dim > 512)
                dim = 512;
            String url = String.format("%s&chs=%dx%d&chl=%s", CHART_URL,
                    dim, dim, URLEncoder.encode(jsonID.toString()));
            Object[] result = new Object[3];
            HttpGet req = new HttpGet(url);
            try {
                HttpResponse res = client.execute(req);
                InputStream istr = res.getEntity().getContent();
                qrCodeImg = BitmapFactory.decodeStream(istr);
                result[0] = qrCodeImg;
                ParseQuery devQuery = new ParseQuery("DevOut");
                devQuery.whereEqualTo("dev_id", wifiID);
                List<ParseObject> qRes = devQuery.find();
                if (qRes.size() > 0) {
                    ParseObject obj = qRes.get(0);
                    result[1] = obj.getString("user_id");
                    result[2] = (ParseFile) obj.get("signature");
                }
                else
                    result[1] = null;
                
            } catch (ClientProtocolException e) {
                Log.e(TAG, "ClientProtocolException " + e);
            } catch (IOException e) {
                Log.e(TAG, "IOException " + e);
            } catch (ParseException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            return result;
        }

        /* (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Object result) {
            dismissDialog(DOWNLOAD_DIALOG);
            Object[] res = (Object[]) result;
            qr.setImageBitmap((Bitmap)res[0]);
            Animation flipAnim = AnimationUtils.loadAnimation(IMEI2QRActivity.this, R.anim.qr_anim);
            if (res[1] != null) {
                user.setText("Checked out by " + (String) res[1]);
                top.setBackgroundResource(R.color.background_onloan);
                userId = (String) res[1];
                ParseFile sig = (ParseFile) res[2];
                try {
                    byte[] sigData = sig.getData();
                    signatureImg = BitmapFactory.decodeByteArray(sigData, 0, sigData.length);
                    signature.setImageBitmap(signatureImg);
                    signature.setVisibility(View.VISIBLE);
                    signature.startAnimation(flipAnim);
                } catch (ParseException e) {
                    Toast.makeText(IMEI2QRActivity.this, "Failed to load signature", Toast.LENGTH_SHORT).show();
                }
            }
            else {
                user.setText("Device is available");
                top.setBackgroundResource(R.color.background_avail);
                signature.setVisibility(View.GONE);
                userId = "";
            }
            qr.startAnimation(flipAnim);
        }

        /* (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            showDialog(DOWNLOAD_DIALOG);
        }
        
        /* (non-Javadoc)
         * @see android.os.AsyncTask#onProgressUpdate(Progress[])
         */
        @Override
        protected void onProgressUpdate(Void... values) {
            // TODO Auto-generated method stub
            super.onProgressUpdate(values);
        }
        
    }

    @Override
    public void onClick(View v) {
        new URLTask().execute();
    }
    
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager 
              = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private BroadcastReceiver localReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            new URLTask().execute();
//            String msg = intent.getStringExtra("message");
//            user.setText(msg);
//            int reg_color = getResources().getColor(R.color.background_onloan);
//            int avail_color = getResources().getColor(R.color.background_avail);
//            ObjectAnimator bgAnim;
//            if (msg.toUpperCase().startsWith("DEREG"))
//                bgAnim = ObjectAnimator.ofObject(top, "backgroundColor",
//                    new ArgbEvaluator(), reg_color, avail_color);
//            else
//                bgAnim = ObjectAnimator.ofObject(top, "backgroundColor",
//                    new ArgbEvaluator(), avail_color, reg_color);
//            bgAnim.setDuration(1000);
//            bgAnim.start();
//            qr.startAnimation(AnimationUtils.loadAnimation(IMEI2QRActivity.this, R.anim.qr_anim));
        }
    };
}
