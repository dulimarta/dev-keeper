package edu.gvsu.cis.dulimarh.checkout;

import android.app.ListActivity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


public class SelectUserActivity extends ListActivity implements SimpleAdapter.ViewBinder {

    private static final int MENU_ADD_NEW_USER = Menu.FIRST;
    private final static int MENU_DELETE_USER = Menu.FIRST + 1;
    private ArrayList<Map<String, Object>> allUsers;
    private SimpleAdapter uAdapter;
    private int selectedPosition;
    private String selectedUid, selectedUname;
    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        Window win = getWindow();
        win.setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND , WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        LayoutParams params = win.getAttributes();
        params.width = 800;
        params.dimAmount = 0.3f;
        win.setAttributes(params);
//        setContentView(R.layout.activity_selectuser);
        setTitle("Select User");
        allUsers = new ArrayList<Map<String, Object>>();
        uAdapter = new SimpleAdapter(this, allUsers,
                R.layout.user_list_item,
                new String[] {"user_name", "user_id", "user_photo"},
                new int[] {R.id.main_text, R.id.sub_text, R.id.item_icon});
        setListAdapter(uAdapter);
        uAdapter.setViewBinder(this);
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
        registerForContextMenu(getListView());
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
            for (String key : u.keySet()) {
                Object obj = u.get(key);
                if (obj instanceof Bitmap)
                    b.putParcelable(key, (Bitmap) obj);
                else
                    b.putString(key, (String) u.get(key));
            }
            bdl.add(b);
        }
        outState.putParcelableArrayList("allUsers", bdl);
        outState.putInt("selection", selectedPosition);
    }

    private void loadAllUsers() {
        ParseQuery<ParseObject> userQuery = new ParseQuery<ParseObject>(Consts.USER_TABLE);
        userQuery.findInBackground(new FindCallback<ParseObject>() {

            @Override
            public void done(List<ParseObject> uList, ParseException e) {
                if (e == null) {
                    allUsers.clear();
                    uAdapter.notifyDataSetInvalidated();
                    for (ParseObject u : uList) {
                        Map<String, Object> uMap = new TreeMap<String, Object>();
                        uMap.put("user_id", u.getString("user_id"));
                        uMap.put("user_name", u.getString("user_name"));
                        uMap.put("objectId", u.getObjectId());
                        ParseFile uImg = u.getParseFile("user_photo");
                        if (uImg != null) {
                            try {
                                byte[] imgData = uImg.getData();
                                uMap.put("user_photo", BitmapFactory.decodeByteArray(imgData, 0, imgData.length));
                            } catch (ParseException e1) {
                                uMap.put("user_photo", BitmapFactory.decodeResource(getResources(), R.drawable.male_user_icon));
                            }
                        }
                        allUsers.add(uMap);
                    }
                    Collections.sort(allUsers, new Comparator<Map<String, Object>>() {
                        @Override
                        public int compare(Map<String, Object> one, Map<String, Object> two) {
                            String one_id = (String) one.get("user_id");
                            String two_id = (String) two.get("user_id");
                            return one_id.compareTo(two_id);
                        }
                    });
                    uAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(SelectUserActivity.this,
                            "Unable to retrieve user data: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater pump = getMenuInflater();
        pump.inflate(R.menu.select_user_add, menu);
        return super.onCreateOptionsMenu(menu);
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.add_user_menu)
        {
            startActivityForResult(new Intent(this, NewUserActivity.class), MENU_ADD_NEW_USER);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MENU_ADD_NEW_USER) {
            if (resultCode == RESULT_OK)
                loadAllUsers();
        }
    }

    /* (non-Javadoc)
     * @see android.app.ListActivity#onListItemClick(android.widget.ListView, android.view.View, int, long)
     */
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        selectedPosition = position;
        String uid = (String) allUsers.get(position).get("user_id");
        String uname = (String) allUsers.get(position).get("user_name");
        Intent userInfo = new Intent();
        userInfo.putExtra("user_id", uid);
        userInfo.putExtra("user_name", uname);
        setResult(RESULT_OK, userInfo);
        finish();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        if (item.getItemId() == MENU_DELETE_USER) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            Map<String,Object> umap = allUsers.get(info.position);
            String userId = (String) umap.get("user_id");
            ParseQuery<ParseObject> registeredDev = new ParseQuery<ParseObject>(Consts.DEVICE_LOAN_TABLE);
            registeredDev.whereEqualTo("user_id", userId);
            try {
                if (registeredDev.find().isEmpty()) {
                    String delObjetId = (String) umap.get("objectId");
                    ParseQuery<ParseObject> delUser = new ParseQuery<ParseObject>(Consts.USER_TABLE);
                    delUser.get(delObjetId).deleteInBackground(new DeleteCallback() {
                        @Override
                        public void done(ParseException e) {
                            loadAllUsers();
                        }
                    });
                }
                else {
                    Toast.makeText(this, "Can't delete user who loaned a device",
                            Toast.LENGTH_LONG).show();
                }
            } catch (ParseException e) {
                String username = (String) allUsers.get(info.position).get("user_name");
                Toast.makeText(this, "Unable to delete " + username, Toast.LENGTH_LONG).show();
            }
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, MENU_DELETE_USER, 0, "Delete User");
    }


    @Override
    public boolean setViewValue(View view, Object data, String textRepresentation) {
        if (view.getId() == R.id.item_icon)
        {
            ImageView img = (ImageView) view;
            img.setImageBitmap((Bitmap)data);
            return true;
        }
        else
            return false;
    }
}
