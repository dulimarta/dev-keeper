package edu.gvsu.cis.dulimarh.checkout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;


public class PhoneListActivity extends ListActivity {
    private final String TAG = getClass().getName();
    private static final int DIALOG_ALREADY_CHECKEDOUT = 1;
    
    private Button checkout;
    private SimpleAdapter adapter;
    private ArrayList<Map<String,String>> checkouts;
    
    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.phonelist);
        Parse.initialize(this, "AGs2nPlOxM7rA1BnUAbeVySTSRud6EhL7JF8sd4f",
                "z5CgnppcixOqpAzHOdnTfT6ktKKzk6aicH8p1Rvb");
        checkout = (Button) findViewById(R.id.checkout);
        checkout.setOnClickListener(checker);
        checkouts = new ArrayList<Map<String,String>>();
        adapter = new SimpleAdapter(this, checkouts, android.R.layout.simple_list_item_2, 
                new String[] {"dev_id", "user_id"},
                new int[] {android.R.id.text1, android.R.id.text2});
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
                final String contents = data.getStringExtra("SCAN_RESULT");
//                String format = data.getStringExtra("SCAN_RESULT_FORMAT");
                ParseQuery idQuery = new ParseQuery("DevOut");
                idQuery.whereEqualTo("dev_id",  contents);
                idQuery.findInBackground(new FindCallback() {
                    
                    @Override
                    public void done(List<ParseObject> result, ParseException ex) {
                        if (ex == null) {
                            if (result.size() == 0) {
                                Intent next = new Intent(
                                        PhoneListActivity.this,
                                        PhoneCheckoutActivity.class);
                                next.putExtra("dev_id", contents);
                                startActivity(next);
                            }
                            else {
                                showDialog(DIALOG_ALREADY_CHECKEDOUT);
                                Toast.makeText(PhoneListActivity.this, 
                                        "The device is already checkout", Toast.LENGTH_LONG).
                                        show();
                            }
                        }
                        else {
                            Toast.makeText(PhoneListActivity.this, 
                                    "Error in querying device id: " + ex.getMessage(), 
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
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
                ParseQuery checkOutQuery = new ParseQuery("DevOut");
                checkouts.clear();
                for (ParseObject obj : checkOutQuery.find())
                {
                    Map<String,String> dev_u = new HashMap<String, String>();
                    dev_u.put("dev_id", obj.getString("dev_id"));
                    dev_u.put("user_id", obj.getString("user_id"));
                    checkouts.add(dev_u);
                }
            } catch (ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
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

    /* (non-Javadoc)
     * @see android.app.Activity#onCreateDialog(int)
     */
    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        switch (id) {
        case DIALOG_ALREADY_CHECKEDOUT:
            builder.setMessage("The scanned device is already checked out");
            builder.setTitle("Warning");
            builder.setPositiveButton("OK", null);
            break;
        }
        return builder.create();
    }
    
    
}
