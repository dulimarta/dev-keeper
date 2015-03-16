package edu.gvsu.cis.dulimarh.checkout;

import android.content.Intent;
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
import android.view.WindowManager.LayoutParams;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Toast;

import com.parse.DeleteCallback;
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
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;


public class DeviceListActivity extends FragmentActivity implements
        DeviceAdapter.DeviceSelectedListener {

//    private static final int MENU_ADD_NEW_USER = Menu.FIRST;
    private final static int MENU_DELETE_DEVICE = Menu.FIRST;
    private ArrayList<ParseProxyObject> allDevices;
    private Set<String> checkedDevices;
    private DeviceAdapter devAdapter;
    private int selectedPosition;
    private String selectedUid, selectedUname;
    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        Window win = getWindow();
        LayoutParams params = win.getAttributes();
        setContentView(R.layout.du_list);
        ImageView img = (ImageView) findViewById(R.id.fab_image);
        img.setImageResource(R.mipmap.ic_checkin);
        setTitle("Devices");

        if (savedInstanceState != null) {
            selectedPosition = savedInstanceState.getInt("selection");
            allDevices =
                    (ArrayList<ParseProxyObject>) savedInstanceState
                            .getSerializable
                            ("allDevices");
        } else {
            selectedPosition = -1;
            allDevices = new ArrayList<ParseProxyObject>();
        }
        checkedDevices = new TreeSet<String>();
        devAdapter = new DeviceAdapter(allDevices, checkedDevices, this);
        RecyclerView rview = (RecyclerView) findViewById(R.id.the_list);
        rview.setAdapter(devAdapter);
        RecyclerView.LayoutManager mgr = new LinearLayoutManager(this);
        rview.setLayoutManager(mgr);
//        registerForContextMenu(getListView());
        loadAllDevs();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("allDevices", allDevices);
        outState.putInt("selection", selectedPosition);
    }

    private void loadAllDevs() {
        new ParseQuery<ParseObject>(Consts.ALL_DEVICE_TABLE)
        .findInBackground()
        .continueWithTask(new Continuation<List<ParseObject>,
                Task<Void>>() {
            @Override
            public Task<Void> then(Task<List<ParseObject>> results) throws
                    Exception {
//                ArrayList<Task<Void>> tasks = new ArrayList<Task<Void>>();
                ParseQuery<ParseObject> loanQuery = new
                        ParseQuery<ParseObject>
                (Consts
                        .DEVICE_LOAN_TABLE);
                allDevices.clear();
                checkedDevices.clear();
                for (ParseObject dev : results.getResult()) {
//                    tasks.add(findUserImageAsync(user));
                    int c = loanQuery.whereEqualTo("device_obj", dev).count();
                    if (c > 0) {
                        checkedDevices.add(dev.getObjectId());
                    }
                    allDevices.add(new ParseProxyObject(dev));
                }
//                return Task.whenAll(tasks);
                return null;
            }
        })
        .onSuccess(new Continuation<Void, Object>() {
            @Override
            public Object then(Task<Void> task) throws Exception {
                if (task.isCompleted()) {
                    Collections.sort(allDevices, deviceComparator);

                    devAdapter.notifyDataSetChanged();
                }
                else {
                    Toast.makeText(DeviceListActivity.this,
                            "Unable to load device data",
                            Toast.LENGTH_SHORT).show();

                }
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);

    }


    private Comparator<ParseProxyObject> deviceComparator = new
            Comparator<ParseProxyObject>() {
        @Override
        public int compare(ParseProxyObject d1, ParseProxyObject d2) {
            boolean d1_out = checkedDevices.contains(d1
                    .getObjectId());
            boolean d2_out = checkedDevices.contains(d2
                    .getObjectId());

            if (d1_out && !d2_out) return -1;
            if (d2_out && !d1_out) return +1;
            return d1.getString("type").compareTo(d2
                    .getString("type"));
        }
    };
    /* (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater pump = getMenuInflater();
        /* TODO fix the menu */
//        pump.inflate(R.menu.select_user_add, menu);
        return super.onCreateOptionsMenu(menu);
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        if (item.getItemId() == R.id.add_user_menu)
//        {
//            startActivityForResult(new Intent(this, NewUserActivity.class), MENU_ADD_NEW_USER);
//            return true;
//        }
        return super.onOptionsItemSelected(item);
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if (requestCode == MENU_ADD_NEW_USER) {
//            if (resultCode == RESULT_OK)
//                loadAllDevs();
//        }
    }

    /* (non-Javadoc)
     * @see android.app.ListActivity#onListItemClick(android.widget.ListView, android.view.View, int, long)
     */
//    @Override
//    public void onListItemClick(ListView l, View v, int position, long id) {
//        selectedPosition = position;
//        String uid = (String) allDevices.get(position).get("user_id");
//        String uname = (String) allDevices.get(position).get("user_name");
//        Intent userInfo = new Intent();
//        userInfo.putExtra("user_id", uid);
//        userInfo.putExtra("user_name", uname);
//        setResult(RESULT_OK, userInfo);
//        finish();
//    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        if (item.getItemId() == MENU_DELETE_DEVICE) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
//            Map<String,Object> umap = allDevices.get(info.position);
            ParseProxyObject ppo = allDevices.get(info.position);
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
                            loadAllDevs();
                        }
                    });
                }
                else {
                    Toast.makeText(this, "Can't delete user who loaned a device",
                            Toast.LENGTH_LONG).show();
                }
            } catch (ParseException e) {
                String username = allDevices.get(info.position).getString("user_name");
                Toast.makeText(this, "Unable to delete " + username, Toast.LENGTH_LONG).show();
            }
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, MENU_DELETE_DEVICE, 0, "Delete Device");
    }

    @Override
    public void onDeviceSelected(int pos) {

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
