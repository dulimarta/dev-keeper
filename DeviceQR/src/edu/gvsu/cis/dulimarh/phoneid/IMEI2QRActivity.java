package edu.gvsu.cis.dulimarh.phoneid;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.TextView;

import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.PushService;

public class IMEI2QRActivity extends Activity implements OnClickListener {
    private final String TAG = getClass().getName();
    private static final String CHART_URL = 
        "http://chart.apis.google.com/chart?cht=qr&chld=L&choe=UTF-8";
    
    private TextView id, user;
    private ImageView qr;
    private ProgressDialog progress;
    private Button reload;
    private String devId;
    private URLTask myTask;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Parse.initialize(this, "AGs2nPlOxM7rA1BnUAbeVySTSRud6EhL7JF8sd4f",
                "z5CgnppcixOqpAzHOdnTfT6ktKKzk6aicH8p1Rvb");
//        ParseInstallation.getCurrentInstallation().saveInBackground();
        PushService.subscribe(this, "Hans", IMEI2QRActivity.class);
        PushService.setDefaultPushCallback(this, IMEI2QRActivity.class);
        setContentView(R.layout.main);
        id = (TextView) findViewById(R.id.id);
        user = (TextView) findViewById(R.id.user);
        reload = (Button) findViewById(R.id.refresh);
        qr = (ImageView) findViewById(R.id.qr_code);
        qr.setOnClickListener(this);
        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        devId = wm.getConnectionInfo().getMacAddress() + " " + Build.MODEL;
        id.setText(devId);
        myTask = new URLTask();
        myTask.execute();
        reload.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                URLTask ut = new URLTask();
                ut.execute();
            }
        });
    }
    
    /* (non-Javadoc)
     * @see android.app.Activity#onCreateDialog(int)
     */
    @Override
    protected Dialog onCreateDialog(int id) {
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
                Bitmap img = BitmapFactory.decodeStream(istr);
                result[0] = img;
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
            }
            else
                user.setText("");
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
}