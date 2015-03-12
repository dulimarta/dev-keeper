package edu.gvsu.cis.dulimarh.checkout;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;

import edu.gvsu.cis.dulimarh.checkout.DeviceDetailsFragment.DeviceRemovalListener;

public class DeviceCheckinActivity extends Activity
    implements DeviceRemovalListener
{

    //private final String TAG = getClass().getName();
    
    private String user_id;
    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent data = getIntent();
        int pos = data.getIntExtra("index", -1);
        String obj_id = data.getStringExtra("object_id");
//        Bundle b = data.getExtras();
        setContentView(R.layout.activity_checkin);
        DeviceDetailsFragment ddf = DeviceDetailsFragment.newInstance
                (pos, obj_id);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.devdetails, ddf);
        ft.commit();
        //        showDialog(DIALOG_PROGRESS);
        /* TODO: Add "Send email button"? */
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onCreateDialog(int, android.os.Bundle)
     */
    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        switch (id) {
//        case DIALOG_WRONG_DEVICE:
            builder.setTitle("Error: Unmatched IDs");
            builder.setMessage("Scanned id: " + args.getString("scan_result") + 
                    " device id: " + args.getString("dev_id"));
            builder.setPositiveButton("OK", null);
//        }
        return builder.create();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onPrepareDialog(int, android.app.Dialog, android.os.Bundle)
     */
    @Override
    protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
        AlertDialog d = (AlertDialog) dialog;
//        switch (id) {
//        case DIALOG_CONFIRM_CHECKIN:
            d.setMessage("Check in device " + args.getString("dev_id") + "?");
//            break;
//        case DIALOG_WRONG_DEVICE:
            d.setMessage("Scanned id: " + args.getString("scan_result") + 
                    " device id: " + args.getString("dev_id"));
//        }
    }

    @Override
    public void deviceRemoved(String dev_id) {
        ParsePushUtils.pushTo(dev_id, "Deregister from " + user_id);
        finish();

    }

    
}
