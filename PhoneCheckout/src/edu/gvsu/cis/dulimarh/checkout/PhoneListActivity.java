package edu.gvsu.cis.dulimarh.checkout;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;


public class PhoneListActivity extends ListActivity {
    private final String TAG = getClass().getName();
    
    private Button checkout;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> checkouts;
    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.phonelist);
        checkout = (Button) findViewById(R.id.checkout);
        checkout.setOnClickListener(checker);
        checkouts = new ArrayList<String>();
        adapter = new ArrayAdapter<String>(this, 
                android.R.layout.simple_list_item_1, checkouts);
        setListAdapter(adapter);
    }

    
    /* (non-Javadoc)
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume() {
        super.onResume();
        new CheckTask().execute();
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                String contents = data.getStringExtra("SCAN_RESULT");
                String format = data.getStringExtra("SCAN_RESULT_FORMAT");
                Intent next = new Intent (PhoneListActivity.this, PhoneCheckoutActivity.class);
                next.putExtra("dev_id", contents);
                startActivity(next);
                Log.d(TAG, contents + " => " + format);
            }
            else if (resultCode == RESULT_CANCELED) {
                Log.d(TAG, "Scanning cancelled");
            }
        }
    }
    
    private OnClickListener checker = new OnClickListener() {
        
        @Override
        public void onClick(View v) {
            Intent scan = new Intent ("com.google.zxing.client.android.SCAN");
            scan.putExtra("SCAN_MODE", "QR_CODE_MODE");
            startActivityForResult(scan, 0);

        }
    };
    
    private class CheckTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                URL url = new URL("http://www.cis.gvsu.edu/~dulimarh/CS163H/CheckOut/checkout.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                Scanner scan = new Scanner(conn.getInputStream());
                String str = "";
                while (scan.hasNextLine()) {
                    str += scan.nextLine();
                }
                JSONObject obj = new JSONObject(str);
                JSONArray users = obj.getJSONArray("checkouts");
                checkouts.clear();
                for (int k = 0; k < users.length(); k++) {
                    JSONObject us = users.getJSONObject(k);
                    String userid = us.getString("name");
                    String device = us.getString("device_id");
                    checkouts.add(device + " " + userid);
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
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Void result) {
            adapter.notifyDataSetChanged();
        }
        
    }
}
