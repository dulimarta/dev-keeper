package edu.gvsu.cis.dulimarh.checkout;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.transition.Explode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.List;

import bolts.Continuation;
import bolts.Task;
import edu.gvsu.cis.dulimarh.checkout.DevOutDetailsFragment.DeviceRemovalListener;


public class MainActivity extends Activity implements
        DeviceRemovalListener {
    // private final String TAG = getClass().getName();
    private static final int DIALOG_ALREADY_CHECKEDOUT = 1;
    private static final int DIALOG_CONFIRM_CHECKOUT = 2;

    private static final int SELECT_USER_REQUEST = 421;
    private static final int CHECKOUT_DEVICE_REQUEST = SELECT_USER_REQUEST + 1;

    private enum DevTask {NONE, CHECKIN, CHECKOUT};
    private String borrowerId, borrowerName;
    private DevOutListFragment devListFragment;
    private ImageView userMenu, deviceMenu;
//    private static final int DIALOG_PROGRESS = 2;

    private DevTask currentTask;

    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devlist);
        devListFragment = (DevOutListFragment) getFragmentManager().findFragmentById(R.id.devlistFragment);
        userMenu = (ImageView) findViewById(R.id.user_menu);
        deviceMenu = (ImageView) findViewById(R.id.device_menu);
        userMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent userIntent = new Intent(MainActivity.this,
                        UserListActivity.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getWindow().setExitTransition(new Explode());
                    startActivity(userIntent,
                            ActivityOptions.makeSceneTransitionAnimation
                                    (MainActivity.this).toBundle());
                }
                else {
                    startActivity(userIntent);
                }
            }
        });
        deviceMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this,
                        DeviceListActivity.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getWindow().setExitTransition(new Explode());
                    startActivity(i,
                            ActivityOptions.makeSceneTransitionAnimation
                                    (MainActivity.this).toBundle());
                }
                else {
                    startActivity(i);
                }
            }
        });
        if (savedInstanceState == null)
            currentTask = DevTask.NONE;
        else {
            currentTask = DevTask.valueOf(savedInstanceState.getString("currentTask"));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("currentTask", currentTask.toString());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater mn_inflater = getMenuInflater();
        mn_inflater.inflate(R.menu.action_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_checkout:
                Intent userSelect = new Intent(this, UserListActivity.class);
                currentTask = DevTask.CHECKOUT;
                startActivityForResult(userSelect, SELECT_USER_REQUEST);
                break;
            case R.id.menu_checkin:
                currentTask = DevTask.CHECKIN;
                IntentIntegrator ii = new IntentIntegrator(this);
                ii.initiateScan();
                break;
        }
//        return  super.onMenuItemSelected(featureId, item);
        return true;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SELECT_USER_REQUEST) {
            if (resultCode == RESULT_OK && data != null) {
            /* We got the info of the user who is checking out the device */
                borrowerId = data.getStringExtra("user_id");
                borrowerName = data.getStringExtra("user_name");
                IntentIntegrator ii = new IntentIntegrator(this);
                ii.initiateScan();
            }
        }
        else if (requestCode == CHECKOUT_DEVICE_REQUEST) {
            if (resultCode == RESULT_OK)
                devListFragment.updateDeviceList();
        }
        else {
            //super.onActivityResult(requestCode, resultCode, data);
            IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (scanResult == null) return;
            final String scannedDevId = scanResult.getContents();
            if (scannedDevId == null) return;
            switch (currentTask) {
                case CHECKIN:
                    DialogFragment mydialog = CheckinConfirmationDialog.newInstance (scannedDevId);
                    mydialog.show(getFragmentManager(), "dialog");
                    break;
                case CHECKOUT:
                    doCheckOut (scannedDevId, borrowerId, borrowerName);
                    break;
            }
            devListFragment.updateDeviceList();
            currentTask = DevTask.NONE;
        }
    }


    private void doCheckIn(final String devId)
    {
        ParseQuery<ParseObject> idQuery = new ParseQuery<ParseObject>(Consts.DEVICE_LOAN_TABLE);
        idQuery.whereEqualTo("dev_id", devId)
            .findInBackground()
            .continueWith(new Continuation<List<ParseObject>, Task<Void>>() {
                @Override
                public Task<Void> then(Task<List<ParseObject>> task)
                        throws Exception {
                    if (task.isCompleted()) {
                        List<ParseObject> result = task.getResult();
                        if (result.size() == 1) {
                            ParseObject obj = result.get(0);
//                            String user_id = obj.getString("user_id");
                            //String user_name = obj.getString("user_name");
                            try {
                                obj.delete();
                                deviceRemoved(devId);
                            } catch (ParseException e1) {
                                Toast.makeText(MainActivity.this,
                                        "Failed to remove device " + devId,
                                        Toast.LENGTH_SHORT).show();
                            }

                        }
                    }
                    else {
                        Toast.makeText(MainActivity.this,
                                "Failed to query device id " + devId,
                                Toast.LENGTH_SHORT).show();

                    }
                    return null;
                }
            });
    }

    private void doCheckOut (final String devId, String uid, String uname)
    {
        ParseQuery<ParseObject> idQuery = new ParseQuery<ParseObject>(Consts.DEVICE_LOAN_TABLE);
        idQuery.whereEqualTo("dev_id", devId);
        List<ParseObject> result = null;
        try {
            result = idQuery.find();
            if (result.size() == 0) {
                /* Pass userid, username, and device id to the next activity */
                Intent next = new Intent(MainActivity.this,
                        PhoneCheckoutActivity.class);
                next.putExtra("user_id", borrowerId);
                next.putExtra("user_name", borrowerName);
                next.putExtra("dev_id", devId);
                startActivityForResult(next, CHECKOUT_DEVICE_REQUEST);
                //finish();
            } else {
                //showDialog(DIALOG_ALREADY_CHECKEDOUT);
                Toast.makeText(MainActivity.this,
                        "The device is already checkout",
                        Toast.LENGTH_LONG).show();
            }
        } catch (ParseException e) {
            Toast.makeText(MainActivity.this,
                    "Error in querying device id: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
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


    @Override
    public void deviceRemoved(String dev_id) {
        /* TODO: Complete this method */
        devListFragment.updateDeviceList();
        ParsePushUtils.pushTo(dev_id, "Deregistered from " + borrowerId);
    }


    public static class CheckinConfirmationDialog extends DialogFragment {

        public static CheckinConfirmationDialog newInstance (String id)
        {
            CheckinConfirmationDialog diag = new CheckinConfirmationDialog();
            Bundle args = new Bundle();
            args.putString("devId", id);
            diag.setArguments(args);
            return diag;
        }

        public CheckinConfirmationDialog() {
            /* this empty default constructor is required */
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Bundle args = getArguments();
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Confirmation Required");
            final String deviceId = args.getString("devId");
            builder.setMessage("Deregister " + deviceId + "?");
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ((MainActivity) getActivity()).doCheckIn(deviceId);
                }
            });
            return builder.create();
        }
    }

}
