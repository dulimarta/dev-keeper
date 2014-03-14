package edu.gvsu.cis.dulimarh.checkout;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;

public class SelectUserActivity extends ListActivity {

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    private ArrayList<Map<String, Object>> allUsers;
    private SimpleAdapter uAdapter;

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
        } else {
            loadAllUsers();
        }
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
