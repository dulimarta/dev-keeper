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
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;
import edu.gvsu.cis.dulimarh.checkout.custom_ui.FloatingActionButton;


public class UserListActivity extends Activity implements View
        .OnClickListener, UserAdapter.UserSelectedListener {

    private static final int MENU_DELETE_USER = Menu.FIRST;
    private ArrayList<ParseProxyObject> allUsers;
    private Map<String,Integer> countMap;
    private UserAdapter uAdapter;
    private int selectedPosition;
    private FloatingActionButton fab;
    private String selectedUid, selectedUname;
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
        if (data.hasExtra("action")) {
            requestedAction = data.getIntExtra("action", 0);
            if (requestedAction == Consts.ACTION_SELECT_USER) {
                img.setImageResource(R.mipmap.ic_checkout);
                fab.setAlpha(0.0f);
                fab.setEnabled(false);
            }
        }

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
        super.onSaveInstanceState(outState);
        outState.putSerializable("allUsers", allUsers);
        outState.putInt("selection", selectedPosition);
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

                Log.d("HANS", "Notify dataset changed, " +
                        "dataset size " + allUsers.size());
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
        if (requestedAction == Consts.ACTION_SELECT_USER) {
            /* Select user for checkout */
            Intent result = new Intent();
            ParseProxyObject ppObj = allUsers.get(selectedPosition);
            result.putExtra ("user_obj", ppObj.getObjectId());
            result.putExtra ("user_id", ppObj.getString("user_id"));
            result.putExtra ("user_name", ppObj.getString("user_name"));
            setResult(RESULT_OK, result);
            finish();
        }
        else {
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
        if (requestedAction == Consts.ACTION_SELECT_USER &&
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
