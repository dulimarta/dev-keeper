package edu.gvsu.cis.dulimarh.checkout;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import android.R.integer;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ListFragment;
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

public class SelectUserFragment extends ListFragment {
    
    public interface OnUserSelectedListener {
        public void onUserSelected (String uid, String uname);
    }
    

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    private ArrayList<Map<String, Object>> allUsers;
    private SimpleAdapter uAdapter;
    private int selectedPosition;
    private OnUserSelectedListener callback;
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        allUsers = new ArrayList<Map<String, Object>>();
        uAdapter = new SimpleAdapter(getActivity(), allUsers, 
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
     * @see android.app.Fragment#onAttach(android.app.Activity)
     */
    @Override
    public void onAttach(Activity activity) {
        // TODO Auto-generated method stub
        super.onAttach(activity);
        callback = (OnUserSelectedListener) activity;
    }


    /* (non-Javadoc)
     * @see android.app.ListActivity#onListItemClick(android.widget.ListView, android.view.View, int, long)
     */
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        String uid = (String) allUsers.get(position).get("user_id");
        String uname = (String) allUsers.get(position).get("user_name");
        callback.onUserSelected(uid, uname);
    }

    
    /* (non-Javadoc)
     * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
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



    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater pump) {
        pump.inflate(R.menu.select_user_add, menu);
        super.onCreateOptionsMenu(menu, pump);
    }
    
    
    /* (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO Auto-generated method stub
        switch (item.getItemId()) {
        case R.id.add_user_menu:
            Intent i = new Intent(getActivity(), NewUserActivity.class);
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
                    Toast.makeText(getActivity(),
                            "Unable to retrieve user data: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

}
