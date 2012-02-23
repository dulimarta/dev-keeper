package edu.gvsu.cis.dulimarh.checkout;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

public class PhoneCheckoutActivity extends Activity {
    final String TAG = getClass().getName();
    private static final String BASE_URL = "http://www.cis.gvsu.edu/~dulimarh/CS163H/CheckOut/";
    private static final String CHECKOUT_URL = BASE_URL + "checkout.php";
    private static final String USER_URL = BASE_URL + "users.php";

    private SignatureView signature;
    private Button checkout, clear;
    private TextView devId;
    private Spinner uid;
    private ArrayList<CharSequence> allUsers;
    private ArrayAdapter<CharSequence> adapt;
    private String deviceId;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Bundle param = getIntent().getExtras();
        deviceId = param.getString("dev_id");
        allUsers = new ArrayList<CharSequence>();
        devId = (TextView) findViewById(R.id.dev_id);
        devId.setText("Device ID: " + deviceId);
        uid = (Spinner) findViewById(R.id.userid);
        clear = (Button) findViewById(R.id.clear);
        checkout = (Button) findViewById(R.id.checkout);
        signature = (SignatureView) findViewById(R.id.signature);
        adapt = new ArrayAdapter<CharSequence>(PhoneCheckoutActivity.this, 
                android.R.layout.simple_spinner_item, allUsers);
        adapt.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        uid.setAdapter(adapt);
        uid.setOnItemSelectedListener(uidChooser);
        UserTask uname = new UserTask();
        uname.execute(USER_URL);
        checkout.setOnClickListener(btnHandler);
        clear.setOnClickListener(btnHandler);
        signature.setDrawingCacheEnabled(true);
    }
    
    protected void saveBitmap(String name) {
        super.onDestroy();
        Bitmap bmp = signature.getDrawingCache();
        Log.d (TAG, "Bitmap dimension: " + bmp.getHeight() + "x" + bmp.getWidth());
        String filename = Environment.getExternalStorageDirectory() + "/" + name;
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(filename);
            bmp.compress(CompressFormat.PNG, 90, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "FileNotFoundException: " + e);
        } catch (IOException e) {
            Log.e(TAG, "IOException: " + e);
        }
    }
    
    private void post () {
        saveBitmap ("sig.png");
        HttpClient client = new DefaultHttpClient();
        HttpContext ctx = new BasicHttpContext();
        HttpPost poster = new HttpPost(CHECKOUT_URL);
        MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        try {
            entity.addPart("name", new StringBody((String) uid.getSelectedItem()));
            entity.addPart("device", new StringBody(deviceId));
            entity.addPart("time", new StringBody(String.valueOf(System.currentTimeMillis())));
            File img = new File (Environment.getExternalStorageDirectory() + "/sig.png");
            entity.addPart("image", new FileBody(img));
            poster.setEntity(entity);
            HttpResponse resp = client.execute(poster, ctx);
            Log.d(TAG, "After client.execute() status is " + resp.getStatusLine().toString());
            if (resp.getStatusLine().getStatusCode() == 200) {
                Scanner scan = new Scanner(resp.getEntity().getContent());
                while (scan.hasNextLine())
                    Log.d(TAG, scan.nextLine());
            }
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Unsupported exception " + e);
        } catch (ClientProtocolException e) {
            Log.e(TAG, "Client Protocol exception " + e);
        } catch (IOException e) {
            Log.e(TAG, "IO exception " + e);
        }
    }
    
    private OnItemSelectedListener uidChooser = new OnItemSelectedListener() {

        @Override
        public void onItemSelected(AdapterView<?> arg0, View arg1, int pos,
                long arg3) {
//            uid.setPrompt(allUsers.get(pos));
            Log.d(TAG, "User: " + allUsers.get(pos));
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            // TODO Auto-generated method stub
            
        }
    };
    private OnClickListener btnHandler = new OnClickListener() {
        
        @Override
        public void onClick(View v) {
            if (v == clear) {
                signature.reset();
            } else {
                post();
                finish();
            }
        }
    };

    private class UserTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {
            try {
                URL url = new URL(params[0]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                Scanner scan = new Scanner(conn.getInputStream());
                String str = "";
                while (scan.hasNextLine()) {
                    str += scan.nextLine();
                }
                Log.d(TAG, "JSON string " + str);
                JSONObject obj = new JSONObject(str);
                JSONArray users = obj.getJSONArray("users");
                for (int k = 0; k < users.length(); k++) {
                    JSONObject us = users.getJSONObject(k);
                    String userid = us.getString("userid");
                    allUsers.add(userid);
                }
            } catch (MalformedURLException e) {
                Log.e(TAG, "MalformedURL " + e);
            } catch (IOException e) {
                Log.e(TAG, "IOException " + e);
            } catch (JSONException e) {
                Log.e(TAG, "JSONException " + e);
            }
            return null;
        }

        /* (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPostExecute(Void result) {
            Log.d(TAG, "List has " + allUsers.size() + " items");
            adapt.notifyDataSetChanged();
        }
        
    }
}