package edu.gvsu.cis.dulimarh.checkout;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.List;

import edu.gvsu.cis.dulimarh.checkout.DeviceDetailsFragment.DeviceRemovalListener;


public class DeviceListActivity extends Activity implements DeviceRemovalListener {
    // private final String TAG = getClass().getName();
    private static final int DIALOG_ALREADY_CHECKEDOUT = 1;
    private static final int DIALOG_CONFIRM_CHECKOUT = 2;
    private static final int SELECT_USER = 421;
    private enum DevTask {NONE, CHECKIN, CHECKOUT};
    private String borrowerId, borrowerName;
    private DeviceListFragment devListFragment;
//    private static final int DIALOG_PROGRESS = 2;

    private DevTask currentTask;

    // private ListView theList;
    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devlist);
        devListFragment = (DeviceListFragment) getFragmentManager().findFragmentById(R.id.devlistFragment);
        if (savedInstanceState == null)
            currentTask = DevTask.NONE;
        else {
            currentTask = DevTask.valueOf(savedInstanceState.getString("currentTask"));
        }
//        theList = getListView();
//        theList.setOnItemLongClickListener(selectionListener);
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
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        // TODO Auto-generated method stub
        switch (item.getItemId()) {
            case R.id.menu_checkout:
                Intent userSelect = new Intent(this, SelectUserActivity.class);
                currentTask = DevTask.CHECKOUT;
                startActivityForResult(userSelect, SELECT_USER);
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
        if (requestCode == SELECT_USER) {
            if (resultCode == RESULT_OK && data != null) {
            /* We got the info of the user who is checking out the device */
                borrowerId = data.getStringExtra("user_id");
                borrowerName = data.getStringExtra("user_name");
                IntentIntegrator ii = new IntentIntegrator(this);
                ii.initiateScan();
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
            IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (scanResult == null) return;
            final String scannedDevId = scanResult.getContents();
            if (scannedDevId == null) return;
            switch (currentTask) {
                case CHECKIN:
                    doCheckIn (scannedDevId);
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
        idQuery.whereEqualTo("dev_id", devId);
        idQuery.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> parseObjects, ParseException e) {
                if (e == null) {
                    if (parseObjects.size() == 1) {
                        ParseObject obj = parseObjects.get(0);
                        String user_id = obj.getString("user_id");
                        //String user_name = obj.getString("user_name");
//                        DialogFragment mydialog = CheckinConfirmationDialog.newInstance (obj,
//                                "Checkin " + devId + " registered to " + user_id + "?");
                        try {
                            obj.delete();
                            deviceRemoved(devId);
                        } catch (ParseException e1) {
                            Toast.makeText(DeviceListActivity.this,
                                    "Failed to remove device " + devId,
                                    Toast.LENGTH_SHORT).show();
                        }

                    } else
                        Toast.makeText(DeviceListActivity.this,
                                "Device " + devId + " not checked out",
                                Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(DeviceListActivity.this,
                            "Failed to query device id " + devId,
                            Toast.LENGTH_SHORT).show();
                }
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
                Intent next = new Intent(DeviceListActivity.this,
                        PhoneCheckoutActivity.class);
                next.putExtra("user_id", borrowerId);
                next.putExtra("user_name", borrowerName);
                next.putExtra("dev_id", devId);
                startActivity(next);
                //finish();
            } else {
                //showDialog(DIALOG_ALREADY_CHECKEDOUT);
                Toast.makeText(DeviceListActivity.this,
                        "The device is already checkout",
                        Toast.LENGTH_LONG).show();
            }
        } catch (ParseException e) {
            Toast.makeText(DeviceListActivity.this,
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


    private static class CheckinConfirmationDialog extends DialogFragment {

        public static CheckinConfirmationDialog newInstance (ParseObject obj, String msg)
        {
            CheckinConfirmationDialog diag = new CheckinConfirmationDialog();
            Bundle args = new Bundle();
            args.putString("message", msg);
            diag.setArguments(args);
            return diag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Bundle args = getArguments();
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Confirmation Required");
            builder.setMessage(args.getString("message"));
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            return builder.create();
        }
    }
}
