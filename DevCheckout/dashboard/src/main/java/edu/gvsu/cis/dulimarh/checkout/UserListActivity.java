package edu.gvsu.cis.dulimarh.checkout;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.transition.Explode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.parse.DeleteCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import org.json.JSONException;
import org.json.JSONObject;

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
import edu.gvsu.cis.dulimarh.checkout.custom_ui.FloatingActionButton;


public class UserListActivity extends Activity implements View
        .OnClickListener, UserAdapter.UserSelectedListener {

    private static final int MENU_DELETE_USER = Menu.FIRST;
    private static final int GET_USER_SIGNATURE = 0xC0DE01;
    private static final int REGISTER_DEVIVE_4_CHECKOUT = 0xC0DE02;
    private ArrayList<ParseProxyObject> allUsers;
    private Map<String,Integer> countMap;
    private UserAdapter uAdapter;
    private int selectedPosition;
    private FloatingActionButton fab;
    private String selectedUid, selectedUname, userObjId, deviceJSONStr;
    private int requestedAction = 0;
    private ProgressDialog progress;
    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.du_list);
        fab = (FloatingActionButton) findViewById(R.id.add_new);
        ImageView img = (ImageView) findViewById(R.id.fab_image);
        fab.setOnClickListener(this);
        setTitle("Users");
        progress = new ProgressDialog(this);
        progress.setIndeterminate(true);

        Intent data = getIntent();
        if (data != null && data.hasExtra("action")) {
            requestedAction = data.getIntExtra("action", 0);
            if (requestedAction == Consts.ACTION_SELECT_USER_FOR_CHECKOUT) {
                img.setImageResource(R.mipmap.ic_checkout);
                fab.setAlpha(0.0f);
                fab.setEnabled(false);
            }
        }

        countMap = new HashMap<String, Integer>();
        if (savedInstanceState != null) {
            Log.d("HANS", "Restoring saved states");
            selectedPosition = savedInstanceState.getInt("selection");
            allUsers =
                    (ArrayList<ParseProxyObject>) savedInstanceState
                            .getSerializable
                            ("allUsers");
            if (savedInstanceState.containsKey("selectedUid"))
                selectedUid = savedInstanceState.getString("selectedUid");
            if (savedInstanceState.containsKey("selectedUname"))
                selectedUname = savedInstanceState.getString
                        ("selectedUname");
            if (savedInstanceState.containsKey("userObjId"))
                userObjId = savedInstanceState.getString("userObjId");
        } else {
            selectedPosition = -1;
            allUsers = new ArrayList<ParseProxyObject>();
        }
        uAdapter = new UserAdapter(allUsers, countMap, this);
        RecyclerView rview = (RecyclerView) findViewById(R.id.the_list);
        rview.setAdapter(uAdapter);
        RecyclerView.LayoutManager mgr = new LinearLayoutManager(this);
        rview.setLayoutManager(mgr);
        registerForContextMenu(rview);
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d("HANS", "UserListActivity onSaveInstanceState");
        //super.onSaveInstanceState(outState);
        outState.putSerializable("allUsers", allUsers);
        outState.putInt("selection", selectedPosition);
        if (selectedUid != null) {
            outState.putString("selectedUid", selectedUid);
        }
        if (selectedUname != null) {
            outState.putString("selectedUname", selectedUname);
        }
        if (userObjId != null) {
            outState.putString("userObjId", userObjId);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAllUsers();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (progress.isShowing())
            progress.dismiss();
    }

    private Task<Void> findUserImageAsync (final ParseObject obj)
            throws ParseException {
        return Task.call(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                ParseFile pf = obj.getParseFile("user_photo");
                if (pf != null && ImageStore.get(pf.getUrl()) == null) {
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
        return Task.call(new Callable<Void>() {
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
        progress.show();
        new ParseQuery<ParseObject>(Consts.USER_TABLE)
        .findInBackground()
        .onSuccessTask(new Continuation<List<ParseObject>,
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
            }
        })
        .onSuccess(new Continuation<Void, Object>() {
            @Override
            public Object then(Task<Void> task) throws Exception {
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

                uAdapter.notifyDataSetChanged();
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR)
        .continueWith(new Continuation<Object, Object>() {
            @Override
            public Object then(Task<Object> task) throws Exception {
                progress.dismiss();
                if (task.isFaulted()) {
                    Toast.makeText(UserListActivity.this,
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
        super.onCreateOptionsMenu(menu);
        MenuInflater pump = getMenuInflater();
        pump.inflate(R.menu.user_list, menu);
        return true;
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_del_user)
        {
            /* TODO: add code to delete user? */
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MENU_DELETE_USER) {
            if (resultCode == RESULT_OK)
                loadAllUsers();
        }
        else if (requestCode == GET_USER_SIGNATURE) {
            /* TODO signature for checkout is complete */
            finish();
        }
        else if (requestCode == REGISTER_DEVIVE_4_CHECKOUT) {
            if (resultCode == RESULT_OK) {
                doCheckOut(deviceJSONStr);
            }
        }
        else {
            /* scan result for device details */
            Log.d("HANS", "Got the device QR code");
            IntentResult scanResult = IntentIntegrator
                    .parseActivityResult(requestCode, resultCode, data);
            if (scanResult == null) return;
            deviceJSONStr = scanResult.getContents();
            if (deviceJSONStr == null) return;
            doCheckOut(deviceJSONStr);
        }
    }

   private void doCheckOut (final String jsonStr)
    {
        Log.d("HANS", "About to checkout");
        final String scannedModel, scannedOS, scannedFF;
        ParseQuery<ParseObject> idQuery = new ParseQuery<ParseObject>
                (Consts.ALL_DEVICE_TABLE);
        final ParseQuery<ParseObject> loanQuery = new ParseQuery<ParseObject>
                (Consts.DEVICE_LOAN_TABLE);
        final String deviceId;
        try {
            /* The device QRcode includes the following attributes:
                  id, model, os, form_factor
               The Parse table includes the following columns:
                  device_id, name, os, type
             */
            JSONObject jObj = new JSONObject(jsonStr);
            deviceId = jObj.getString("id");
            scannedModel = jObj.getString("model");
            scannedOS = jObj.getString("os");
            scannedFF = jObj.getString("form_factor");
            Log.d("HANS", "Device info: " + deviceId + " " +
              scannedModel);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("HANS", "Error in parsing JSON string from QR code");
            Toast.makeText(this, "Error in scanning device information",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        idQuery.whereEqualTo("device_id", deviceId);
        idQuery.findInBackground()
                .onSuccessTask(new Continuation<List<ParseObject>,
                        Task<List<ParseObject>>>() {
                    @Override
                    public Task<List<ParseObject>> then
                            (Task<List<ParseObject>> task)
                            throws Exception {
                        List<ParseObject> result = task.getResult();
                        Log.d("HANS", "Returned from id query");
                        if (result.size() == 0) {
                            Log.d("HANS", "Device is not registered");
                            throw new RuntimeException("UNREG");
                        }
                        /* device is registered, now check if it is checked out */
                        loanQuery.whereEqualTo("dev_id", deviceId);
                        return loanQuery.findInBackground();
                    }
                })
                .onSuccess(new Continuation<List<ParseObject>, Void>() {
                    @Override
                    public Void then(Task<List<ParseObject>> task) throws
                            Exception {
                        Log.d("HANS", "Return from load query");
                        List<ParseObject> result = task.getResult();
                        if (result.size() == 0) {
                            /* device is not checked out */
                            Log.d("HANS", "Device is available");
                            Intent next = new Intent(UserListActivity.this,
                                    UserSignActivity.class);
                            next.putExtra("user_id", selectedUid);
                            next.putExtra("user_name", selectedUname);
                            next.putExtra("user_obj", userObjId);
                            next.putExtra("dev_id", deviceId);
                            Log.d("HANS", "About to obtain user " +
                                    "signature for user_obj " + userObjId);
                            startActivityForResult(next, GET_USER_SIGNATURE);
                        } else {
                            Log.d("HANS", "Device is on loan");
                            throw new RuntimeException("ONLOAN");
                        }
                        return null;
                    }
                })
                .continueWith(new Continuation<Void, Object>() {
                    @Override
                    public Object then(Task<Void> task) throws Exception {
                        Log.d("HANS", "Continue Bolt task");
                        if (task.isFaulted()) {
                            /* we are here becase the device just scanned
                               is not registered.
                             */
                            Exception e = task.getError();
                            String error = e.getMessage();
                            if ("UNREG".equals(error)) {
                                Log.d("HANS", "About to register new dev");
                                Intent ndev = new Intent(UserListActivity
                                        .this, NewDeviceActivity.class);
                                ndev.putExtra("scannedId", deviceId);
                                ndev.putExtra("scannedModel",
                                        scannedModel);
                                ndev.putExtra("scannedOS", scannedOS);
                                ndev.putExtra("scannedFF", scannedFF);
                                startActivityForResult(ndev,
                                        REGISTER_DEVIVE_4_CHECKOUT);
                            }
                            else if ("ONLOAN".equals(error)) {
                                Log.d("HANS", "Toast warning for device " +
                                        "on loan");
                                Toast.makeText (UserListActivity.this,
                                        "Device " + deviceId + " is already" +
                                                " checked out",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                        return null;
                    }
                }, Task.UI_THREAD_EXECUTOR);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        /* TODO: delete this method? */
        if (item.getItemId() == MENU_DELETE_USER) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
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
    public void onClick(View view) {
        if (requestedAction == Consts.ACTION_SELECT_USER_FOR_CHECKOUT) {
            /* Select user for checkout */
            ParseProxyObject usrObj = allUsers.get(selectedPosition);
            selectedUid = usrObj.getString("user_id");
            userObjId = usrObj.getObjectId();
            selectedUname = usrObj.getString("user_name");

            /* scan for device info */
            Log.d("HANS", "Selection: " +
                    selectedUid + " " + userObjId + " " + selectedUname +
                    ". About to scan device QR code");
            IntentIntegrator ii = new IntentIntegrator(this);
            ii.initiateScan();
        }
        else {
            /* The other action is to add a new user */
            Intent i = new Intent(this, NewUserActivity.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setExitTransition(new Explode());
                startActivity(i,
                        ActivityOptions.makeSceneTransitionAnimation
                                (this).toBundle());
            } else {
                startActivity(i);
            }
        }
    }

    @Override
    public void onUserSelected(int position) {
        if (requestedAction == Consts.ACTION_SELECT_USER_FOR_CHECKOUT &&
                fab.getAlpha() == 0.0f) {
            ObjectAnimator anim = ObjectAnimator.ofFloat(fab, "alpha",
                    0, 1);
            anim.setDuration(1000);
            anim.start();
            fab.setEnabled(true);
        }
        selectedPosition = position;
        uAdapter.notifyDataSetChanged();
    }

}
