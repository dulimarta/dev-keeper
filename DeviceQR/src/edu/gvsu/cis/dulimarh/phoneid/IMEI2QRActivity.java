package edu.gvsu.cis.dulimarh.phoneid;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.PushService;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.List;

public class IMEI2QRActivity extends Activity implements OnClickListener {
    private final String TAG = getClass().getName();
    private static final String CHART_URL = 
        "http://chart.apis.google.com/chart?cht=qr&chld=L&choe=UTF-8";

    private RelativeLayout top;
    private TextView id, user;
    private ImageView qr;
    private ProgressDialog progress;
    private Button reload;
    private String devId, userId;
    private Bitmap qrCodeImg;
    private URLTask myTask;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Parse.initialize(this, "AGs2nPlOxM7rA1BnUAbeVySTSRud6EhL7JF8sd4f",
                "z5CgnppcixOqpAzHOdnTfT6ktKKzk6aicH8p1Rvb");
        Parse.setLogLevel(Parse.LOG_LEVEL_VERBOSE);
        ParseInstallation thisInstall = ParseInstallation.getCurrentInstallation();
        Log.d("HANS", "Installation ID is " + thisInstall.getInstallationId());
        setContentView(R.layout.main);
        top = (RelativeLayout) findViewById(R.id.topLayout);
        id = (TextView) findViewById(R.id.id);
        user = (TextView) findViewById(R.id.user);
        reload = (Button) findViewById(R.id.refresh);
        qr = (ImageView) findViewById(R.id.qr_code);
        qr.setOnClickListener(this);
        if (savedInstanceState != null) {
            userId = savedInstanceState.getString("userId");
            devId = savedInstanceState.getString("devId");
            qrCodeImg = savedInstanceState.getParcelable("qrcode");
            qr.setImageBitmap(qrCodeImg);
            id.setText(devId);
            if (userId.length() > 0)
                user.setText("Checked out by " + userId);
            else
                user.setText ("Device is not checked out");
        }
        else {
            WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
            devId = wm.getConnectionInfo().getMacAddress() + " " + Build.MODEL;
            qrCodeImg = null;
        }

        id.setText(devId);
        /* TODO: Make sure the channel name here is the SAME as the companion app */
        //PushService.subscribe(this, "HansDulimarta", IMEI2QRActivity.class);
        thisInstall.put("dev_id", devId);
        thisInstall.saveInBackground();
        PushService.setDefaultPushCallback(this, IMEI2QRActivity.class);
        reload.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                URLTask ut = new URLTask();
                ut.execute();
            }
        });
    }
    
    
    /* (non-Javadoc)
     * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("qrcode", qrCodeImg);
        outState.putString("userId", userId);
        outState.putString("devId", devId);
    }

    

    /* (non-Javadoc)
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        registerReceiver(localReceiver, new IntentFilter(getPackageName() + ".HansLocalBroadcast"));
        if (qrCodeImg != null) return;
        if (isNetworkAvailable()) {
            myTask = new URLTask();
            myTask.execute();
        } else {
            showDialog(1);
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
        case 0:
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
        case 1:
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
                    dim, dim, URLEncoder.encode(devId));
            Object[] result = new Object[2];
            HttpGet req = new HttpGet(url);
            try {
                HttpResponse res = client.execute(req);
                InputStream istr = res.getEntity().getContent();
                qrCodeImg = BitmapFactory.decodeStream(istr);
                result[0] = qrCodeImg;
                ParseQuery devQuery = new ParseQuery("DevOut");
                devQuery.whereEqualTo("dev_id", devId);
                List<ParseObject> qRes = devQuery.find();
                if (qRes.size() > 0) {
                    ParseObject obj = qRes.get(0);
                    result[1] = obj.getString("user_id");
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
            dismissDialog(0);
            Object[] res = (Object[]) result;
            qr.setImageBitmap((Bitmap)res[0]);
            if (res[1] != null) {
                user.setText("Checked out by " + (String) res[1]);
                top.setBackgroundResource(R.color.background_reg);
                userId = (String) res[1];
            }
            else {
                user.setText("Device is not checked out");
                top.setBackgroundResource(R.color.background_dereg);
                userId = "";
            }
        }

        /* (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            showDialog(0);
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

            String msg = intent.getStringExtra("message");
            user.setText(msg);
            int reg_color = getResources().getColor(R.color.background_reg);
            int der_color = getResources().getColor(R.color.background_dereg);
            ObjectAnimator bgAnim;
            if (msg.toUpperCase().startsWith("DEREG"))
                bgAnim = ObjectAnimator.ofObject(top, "backgroundColor",
                    new ArgbEvaluator(), reg_color, der_color);
            else
                bgAnim = ObjectAnimator.ofObject(top, "backgroundColor",
                    new ArgbEvaluator(), der_color, reg_color);
            bgAnim.setDuration(3000);
            bgAnim.start();
        }
    };
}
