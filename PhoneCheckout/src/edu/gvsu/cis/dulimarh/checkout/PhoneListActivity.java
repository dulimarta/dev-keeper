package edu.gvsu.cis.dulimarh.checkout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;


public class PhoneListActivity extends ListActivity {
    private final String TAG = getClass().getName();
    private static final int DIALOG_ALREADY_CHECKEDOUT = 1;
//    private static final int DIALOG_PROGRESS = 2;
    
    private SimpleAdapter adapter;
    private ArrayList<Map<String,String>> checkouts;
    private ListView theList;
    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Parse.initialize(this, "AGs2nPlOxM7rA1BnUAbeVySTSRud6EhL7JF8sd4f",
                "z5CgnppcixOqpAzHOdnTfT6ktKKzk6aicH8p1Rvb");
        setContentView(R.layout.phonelist);
        checkouts = new ArrayList<Map<String,String>>();
        adapter = new SimpleAdapter(this, checkouts, android.R.layout.simple_list_item_2, 
                new String[] {"dev_id", "user_id"},
                new int[] {android.R.id.text1, android.R.id.text2});
        theList = getListView();
        setListAdapter(adapter);
        theList.setOnItemLongClickListener(selectionListener);
    }

    private OnItemLongClickListener selectionListener = new OnItemLongClickListener() {

        @Override
        public boolean onItemLongClick(AdapterView<?> arg0, View itemView,
                int pos, long id) {
            Map<String,String> entry = checkouts.get(pos);
            return false;
        }
    };
    
    /* (non-Javadoc)
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume() {
        super.onResume();
        new CheckTask().execute();
    }
    
    /* (non-Javadoc)
     * @see android.app.ListActivity#onListItemClick(android.widget.ListView, android.view.View, int, long)
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        if (position >= 0 && position < checkouts.size())
        {
            Map<String,String> selected = checkouts.get(position);
            String uid = selected.get("user_id");
            String devid = selected.get("dev_id");
            Intent i = new Intent(this, PhoneCheckinActivity.class);
            i.putExtra("user_id", uid);
            i.putExtra("dev_id", devid);
            startActivity(i);
        }
        super.onListItemClick(l, v, position, id);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onActivityResult(int, int,
     * android.content.Intent)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(
                requestCode, resultCode, data);
        if (scanResult == null)
            return;
        final String contents = scanResult.getContents();
        if (contents == null)
            return;
        ParseQuery idQuery = new ParseQuery("DevOut");
        idQuery.whereEqualTo("dev_id", contents);
        idQuery.findInBackground(new FindCallback() {

            @Override
            public void done(List<ParseObject> result, ParseException ex) {
                if (ex == null) {
                    if (result.size() == 0) {
                        Intent next = new Intent(PhoneListActivity.this,
                                PhoneCheckoutActivity.class);
                        next.putExtra("dev_id", contents);
                        startActivity(next);
                    } else {
                        showDialog(DIALOG_ALREADY_CHECKEDOUT);
                        Toast.makeText(PhoneListActivity.this,
                                "The device is already checkout",
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(PhoneListActivity.this,
                            "Error in querying device id: " + ex.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

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
                Collections.sort(checkouts, new Comparator<Map<String,String>>() {

                    @Override
                    public int compare(Map<String, String> one,
                            Map<String, String> two) {
                        return one.get("user_id").compareTo(two.get("user_id"));
                    }
                });
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
//        case DIALOG_PROGRESS:
//            progress = new ProgressDialog(this);
//            progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//            return progress;
        }
        return builder.create();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater mn_inflater = getMenuInflater();
        mn_inflater.inflate(R.menu.action_menu, menu);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        // TODO Auto-generated method stub
        switch (item.getItemId())
        {
        case R.id.menu_checkout:
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.initiateScan();
//            *                 Intent scan = new Intent("com.google.zxing.client.android.SCAN");
//                scan.putExtra("SCAN_MODE", "QR_CODE_MODE");
//                startActivityForResult(scan, 0);
            break;
        case R.id.menu_user_upload:
           Intent i = new Intent (this, UploadUsersActivity.class);
           startActivity(i);
        }
//        return  super.onMenuItemSelected(featureId, item);
        return true;
    }
    
    
}
