package edu.gvsu.cis.dulimarh.checkout;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.AdapterView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;


public class SelectUserActivity extends FragmentActivity {

    private static final int MENU_ADD_NEW_USER = Menu.FIRST;
    private final static int MENU_DELETE_USER = Menu.FIRST + 1;
    private ArrayList<ParseProxyObject> allUsers;
    private Map<String,Integer> countMap;
    private UserAdapter uAdapter;
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
//        win.setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND , WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        LayoutParams params = win.getAttributes();
//        params.width = 800;
//        params.dimAmount = 0.3f;
//        win.setAttributes(params);
        setContentView(R.layout.user_list);
        setTitle("Users");

        countMap = new HashMap<String, Integer>();
        if (savedInstanceState != null) {
            selectedPosition = savedInstanceState.getInt("selection");
            allUsers =
                    (ArrayList<ParseProxyObject>) savedInstanceState
                            .getSerializable
                            ("allUsers");
        } else {
            selectedPosition = -1;
            allUsers = new ArrayList<ParseProxyObject>();
        }
        uAdapter = new UserAdapter(allUsers, countMap);
        RecyclerView rview = (RecyclerView) findViewById(R.id.user_list);
        rview.setAdapter(uAdapter);
        RecyclerView.LayoutManager mgr = new LinearLayoutManager(this);
        rview.setLayoutManager(mgr);
//        registerForContextMenu(getListView());
        loadAllUsers();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("allUsers", allUsers);
        outState.putInt("selection", selectedPosition);
    }

    private Task<Void> findUserImageAsync (final ParseObject obj)
            throws ParseException {
        return Task.callInBackground(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                ParseFile pf = obj.getParseFile("user_photo");
                if (ImageStore.get(pf.getUrl()) == null) {
                    Drawable d = Drawable.createFromStream(new
                            ByteArrayInputStream(pf.getData()), "");
                    ImageStore.put(obj.getObjectId(), d);
                }
                return null;
            }
        });
    }

    private Task<Void> countCheckout (final ParseObject usr) throws
            ParseException {
        return Task.callInBackground(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                int co_count = new ParseQuery<ParseObject>(Consts
                        .DEVICE_LOAN_TABLE)
                        .whereEqualTo("user_obj", usr)
                        .count();
                if (co_count > 0)
                    countMap.put(usr.getObjectId(), co_count);
                return null;
            }
        });
    }

    private void loadAllUsers() {
        new ParseQuery<ParseObject>(Consts.USER_TABLE)
        .findInBackground()
        .continueWithTask(new Continuation<List<ParseObject>,
                Task<Void>>() {
            @Override
            public Task<Void> then(Task<List<ParseObject>> results) throws
                    Exception {
                ArrayList<Task<Void>> tasks = new ArrayList<Task<Void>>();
                allUsers.clear();
                for (ParseObject user : results.getResult()) {
                    tasks.add(findUserImageAsync(user));
                    tasks.add(countCheckout(user));
                    allUsers.add(new ParseProxyObject(user));
                }
                return Task.whenAll(tasks);
//                return null;
            }
        })
        .onSuccess(new Continuation<Void, Object>() {
            @Override
            public Object then(Task<Void> task) throws Exception {
                if (task.isCompleted()) {
                    Collections.sort(allUsers, new Comparator<ParseProxyObject>() {

                        @Override
                        public int compare(ParseProxyObject u1,
                                           ParseProxyObject u2) {
                            String id_one = u1.getObjectId();
                            String id_two = u2.getObjectId();
                            Integer count1 = countMap.get(id_one);
                            Integer count2 = countMap.get(id_two);
                            if (count1 == null && count2 != null)
                                return +1;
                            if (count1 != null && count2 == null)
                                return -1;
                            if (count1 == null && count2 == null)
                                return u1.getString("user_id").compareTo(u2
                                        .getString("user_id"));
                            else {
                                int c1 = (int) count1;
                                int c2 = (int) count2;
                                if (c1 < c2) return -1;
                                if (c1 > c2) return +1;
                                return u1.getString("user_id").compareTo(u2
                                        .getString("user_id"));
                            }
                        }
                    });

                    Log.d("HANS", "Notify dataset changed, " +
                            "dataset size " + allUsers.size());
                    uAdapter.notifyDataSetChanged();
                }
                else {
                    Toast.makeText(SelectUserActivity.this,
                            "Unable to load user data",
                            Toast.LENGTH_SHORT).show();

                }
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);

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
//    @Override
//    public void onListItemClick(ListView l, View v, int position, long id) {
//        selectedPosition = position;
//        String uid = (String) allUsers.get(position).get("user_id");
//        String uname = (String) allUsers.get(position).get("user_name");
//        Intent userInfo = new Intent();
//        userInfo.putExtra("user_id", uid);
//        userInfo.putExtra("user_name", uname);
//        setResult(RESULT_OK, userInfo);
//        finish();
//    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        if (item.getItemId() == MENU_DELETE_USER) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
//            Map<String,Object> umap = allUsers.get(info.position);
            ParseProxyObject ppo = allUsers.get(info.position);
            String userId = ppo.getString("user_id");
            ParseQuery<ParseObject> registeredDev = new ParseQuery<ParseObject>(Consts.DEVICE_LOAN_TABLE);
            registeredDev.whereEqualTo("user_id", userId);
            try {
                if (registeredDev.find().isEmpty()) {
                    String delObjetId = ppo.getObjectId();
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
                String username = allUsers.get(info.position).getString("user_name");
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


//    @Override
//    public boolean setViewValue(View view, Object data, String textRepresentation) {
//        if (view.getId() == R.id.item_icon)
//        {
//            ImageView img = (ImageView) view;
//            img.setImageBitmap((Bitmap)data);
//            return true;
//        }
//        else
//            return false;
//    }
}
