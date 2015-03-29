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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import bolts.Continuation;
import bolts.Task;
import edu.gvsu.cis.dulimarh.checkout.DevOutDetailsFragment.DeviceRemovalListener;


public class MainActivity extends Activity implements
        DeviceRemovalListener {
    // private final String TAG = getClass().getName();
    private static final int DIALOG_ALREADY_CHECKEDOUT = 1;
    private static final int DIALOG_DONE_CHECKOUT = 2;

    private static final int SELECT_USER_REQUEST = 421;
    private static final int CHECKOUT_DEVICE_REQUEST = SELECT_USER_REQUEST + 1;
    private static final int REG_DEVICE_FOR_CHECKOUT =
            SELECT_USER_REQUEST + 2;

    private enum DevTask {NONE, CHECKIN, CHECKOUT};
    private String borrowerId, borrowerName, userObjId, deviceId;
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
                userSelect.putExtra("action", Consts.ACTION_SELECT_USER);
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
                userObjId = data.getStringExtra("user_obj");
                borrowerId = data.getStringExtra("user_id");
                borrowerName = data.getStringExtra("user_name");
                IntentIntegrator ii = new IntentIntegrator(this);
                ii.initiateScan();
            }
        }
        else if (requestCode == CHECKOUT_DEVICE_REQUEST) {
            if (resultCode == RESULT_OK) {
                showDialog(DIALOG_DONE_CHECKOUT);
//                Toast.makeText(MainActivity.this,
//                        "Device successfully checked out",
//                        Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(MainActivity.this,
                        "Failed to check out",
                        Toast.LENGTH_SHORT).show();

            }
        }
        else if (requestCode == REG_DEVICE_FOR_CHECKOUT) {
                            /* device is not checked out */
            if (resultCode == RESULT_OK) {
                Intent next = new Intent(MainActivity.this,
                        DeviceCheckoutActivity.class);
                next.putExtra("user_id", borrowerId);
                next.putExtra("user_name", borrowerName);
                next.putExtra("user_obj", userObjId);
                next.putExtra("dev_id", deviceId);
                startActivityForResult(next, CHECKOUT_DEVICE_REQUEST);
            }
            else {
                /* cancelled? */
            }
        }
        else {
            /* scanned device for checkin or checkout */
            IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (scanResult == null) return;
            final String scannedData = scanResult.getContents();
            if (scannedData == null) return;
            switch (currentTask) {
                case CHECKIN:
                    DialogFragment mydialog = CheckinConfirmationDialog
                            .newInstance(scannedData);
                    mydialog.show(getFragmentManager(), "dialog");
                    break;
                case CHECKOUT:
                    doCheckOut (scannedData);
                    break;
            }
//            devListFragment.updateDeviceList();
            currentTask = DevTask.NONE;
        }
    }


    private void doCheckIn(final String devId)
    {
        /*
        devOut::dev_id            log::dev_id
        devOut::device_object
        devOut::signature         log::signature
        devOut::user_id           log::user_id
        devOut::user_obj
        devOut::createdAt         log::checkout_date
         */
        ParseQuery<ParseObject> idQuery = new ParseQuery<ParseObject>(Consts.DEVICE_LOAN_TABLE);
        idQuery.whereEqualTo("dev_id", devId)
            .findInBackground()
            .onSuccess(new Continuation<List<ParseObject>, Task<Void>>() {
                @Override
                public Task<Void> then(Task<List<ParseObject>> task)
                        throws Exception {
                    List<ParseObject> result = task.getResult();
                    if (result.size() == 1) {
                        ParseObject obj = result.get(0);
                        ParseObject logObj = new ParseObject(Consts
                                .LOG_TABLE);
                        logObj.put("dev_id", obj.getString("dev_id"));
                        logObj.put("signature",
                                obj.getParseFile("signature"));
                        logObj.put("user_id", obj.getString("user_id"));
                        logObj.put("checkout_date", obj.getCreatedAt());
                        obj.delete();
                        logObj.save();
                        deviceRemoved(devId);
                    }
                    return null;
                }
            })
        .continueWith(new Continuation<Task<Void>, Object>() {
            @Override
            public Object then(Task<Task<Void>> task) throws Exception {
                if (task.isFaulted()) {
                    Toast.makeText(MainActivity.this,
                            "Failed to checkin device id " + devId,
                            Toast.LENGTH_SHORT).show();
                }
                return null;
            }
        });
    }

    private void doCheckOut (final String jsonStr)
    {
        final String scannedModel, scannedOS, scannedFF;
        ParseQuery<ParseObject> idQuery = new ParseQuery<ParseObject>
                (Consts.ALL_DEVICE_TABLE);
        final ParseQuery<ParseObject> loanQuery = new ParseQuery<ParseObject>
                (Consts.DEVICE_LOAN_TABLE);
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
        } catch (JSONException e) {
            e.printStackTrace();
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
                        if (result.size() == 0) {
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
                        List<ParseObject> result = task.getResult();
                        if (result.size() == 0) {
                            /* device is not checked out */
                            Intent next = new Intent(MainActivity.this,
                                    DeviceCheckoutActivity.class);
                            next.putExtra("user_id", borrowerId);
                            next.putExtra("user_name", borrowerName);
                            next.putExtra("user_obj", userObjId);
                            next.putExtra("dev_id", deviceId);
                            startActivityForResult(next, CHECKOUT_DEVICE_REQUEST);
                        } else {
                            throw new RuntimeException("ONLOAN");
                        }
                        return null;
                    }
                })
                .continueWith(new Continuation<Void, Object>() {
                    @Override
                    public Object then(Task<Void> task) throws Exception {
                        if (task.isFaulted()) {
                            Exception e = task.getError();
                            String error = e.getMessage();
                            if ("UNREG".equals(error)) {
                                Intent ndev = new Intent(MainActivity
                                        .this, NewDeviceActivity.class);
                                ndev.putExtra("scannedId", deviceId);
                                ndev.putExtra("scannedModel",
                                        scannedModel);
                                ndev.putExtra("scannedOS", scannedOS);
                                ndev.putExtra("scannedFF", scannedFF);
                                startActivityForResult(ndev,
                                        REG_DEVICE_FOR_CHECKOUT);
                            }
                            else if ("ONLOAN".equals(error)) {
                                Toast.makeText (MainActivity.this,
                                        "Device " + deviceId + " is already" +
                                                " checked out",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                        return null;
                    }
                }, Task.UI_THREAD_EXECUTOR);
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
            case DIALOG_DONE_CHECKOUT:
                builder.setTitle("Device Checked Out");
                builder.setPositiveButton("OK", null);
        }
        return builder.create();
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        if (id == DIALOG_DONE_CHECKOUT) {
            ((AlertDialog) dialog).setMessage("Device " + deviceId +
                    " is successfully checked out to " + borrowerName);
        }
    }

    @Override
    public void deviceRemoved(String dev_id) {
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
