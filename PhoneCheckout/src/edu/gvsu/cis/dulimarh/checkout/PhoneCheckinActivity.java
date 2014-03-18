package edu.gvsu.cis.dulimarh.checkout;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import edu.gvsu.cis.dulimarh.checkout.DeviceDetailsFragment.DeviceRemovalListener;

public class PhoneCheckinActivity extends Activity 
    implements DeviceRemovalListener
{

    //private final String TAG = getClass().getName();
    
//    private String user_id, dev_id, parseObjId;
    
    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent data = getIntent();
        int pos = data.getIntExtra("index", -1);
        String user_id = data.getStringExtra("user_id");
        String dev_id = data.getStringExtra("dev_id");
        setContentView(R.layout.activity_checkin);
        DeviceDetailsFragment ddf = DeviceDetailsFragment.newInstance(pos, user_id, dev_id);
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
        finish();
    }

    
}
