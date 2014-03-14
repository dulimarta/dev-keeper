package edu.gvsu.cis.dulimarh.checkout;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;

public class SelectUserActivity extends ListActivity {
    private static final int DIALOG_ALREADY_CHECKEDOUT = 1;

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    private ArrayList<Map<String, Object>> allUsers;
    private SimpleAdapter uAdapter;
    private int selectedPosition;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        allUsers = new ArrayList<Map<String, Object>>();
        uAdapter = new SimpleAdapter(this, allUsers, 
                android.R.layout.simple_list_item_2, 
                new String[] {"user_name", "user_id"}, 
                new int[] {android.R.id.text1, android.R.id.text2});
        setListAdapter(uAdapter);
        if (savedInstanceState != null) {
            selectedPosition = savedInstanceState.getInt("selection");
            ArrayList<Bundle> bdl = savedInstanceState.getParcelableArrayList("allUsers");
            for (Bundle b : bdl) {
                Map<String,Object> uMap = new TreeMap<String, Object>();
                for (String key : b.keySet())
                    uMap.put(key, b.get(key));
                allUsers.add(uMap);
            }
        } else {
            selectedPosition = -1;
            loadAllUsers();
        }
    }

    
    /* (non-Javadoc)
     * @see android.app.ListActivity#onListItemClick(android.widget.ListView, android.view.View, int, long)
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        selectedPosition = position;
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.initiateScan();
    }

    
    /* (non-Javadoc)
     * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        ArrayList<Bundle> bdl = new ArrayList<Bundle>();
        for (Map<String,Object> u : allUsers) {
            Bundle b = new Bundle();
            b.putString("user_id", (String)u.get("user_id")); 
            b.putString("user_name", (String)u.get("user_name"));
            bdl.add(b);
        }
        outState.putParcelableArrayList("allUsers", bdl);
        outState.putInt("selection", selectedPosition);
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (scanResult == null) return;
        final String contents = scanResult.getContents();
        if (contents == null) return;
        ParseQuery<ParseObject> idQuery = new ParseQuery<ParseObject>("DevOut");
        idQuery.whereEqualTo("dev_id", contents);
        idQuery.findInBackground(new FindCallback<ParseObject>() {

            @Override
            public void done(List result, ParseException ex) {
                if (ex == null) {
                    if (result.size() == 0) {
                        /* Pass userid, username, and device id to the next activity */
                        Intent next = new Intent(SelectUserActivity.this,
                                PhoneCheckoutActivity.class);
                        Map<String,Object> uMap = allUsers.get(selectedPosition);
                        next.putExtra("user_id", (String) uMap.get("user_id")); 
                        next.putExtra("user_name", (String) uMap.get("user_name")); 
                        next.putExtra("dev_id", contents);
                        startActivity(next);
                        finish();
                    } else {
                        showDialog(DIALOG_ALREADY_CHECKEDOUT);
                        Toast.makeText(SelectUserActivity.this,
                                "The device is already checkout",
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(SelectUserActivity.this,
                            "Error in querying device id: " + ex.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            }
        });
        //super.onActivityResult(requestCode, resultCode, data);
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


    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater pump = getMenuInflater();
        pump.inflate(R.menu.add_user_menu, menu);
        return true;
    }
    
    

    /* (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO Auto-generated method stub
        switch (item.getItemId()) {
        case R.id.add_user_menu:
            Intent i = new Intent(this, UploadUsersActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void loadAllUsers() {
        ParseQuery<ParseObject> userQuery = new ParseQuery<ParseObject>("Users");
        userQuery.findInBackground(new FindCallback<ParseObject>() {

            @Override
            public void done(List<ParseObject> uList, ParseException e) {
                if (e == null) {
                    for (ParseObject u : uList) {
                        Map<String, Object> uMap = new TreeMap<String, Object>();
                        uMap.put("user_id", u.getString("user_id"));
                        uMap.put("user_name", u.getString("user_name"));
                        ParseFile uImg = u.getParseFile("user_photo");
                        if (uImg != null) {
                            uMap.put("user_photo", uImg);
                        }
                        allUsers.add(uMap);
                    }
                    uAdapter.notifyDataSetChanged();
                }
                else {
                    Toast.makeText(SelectUserActivity.this,
                            "Unable to retrieve user data: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

}
