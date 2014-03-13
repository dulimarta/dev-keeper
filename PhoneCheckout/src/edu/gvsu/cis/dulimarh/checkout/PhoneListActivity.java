package edu.gvsu.cis.dulimarh.checkout;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import edu.gvsu.cis.dulimarh.checkout.DeviceDetailsFragment.DeviceRemovalListener;


public class PhoneListActivity extends Activity implements DeviceRemovalListener {
    private final String TAG = getClass().getName();
    private static final int DIALOG_ALREADY_CHECKEDOUT = 1;
//    private static final int DIALOG_PROGRESS = 2;
    
    private ListView theList;
    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devlist);
//        theList = getListView();
//        theList.setOnItemLongClickListener(selectionListener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onActivityResult(int, int,
     * android.content.Intent)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(
                requestCode, resultCode, data);
        if (scanResult == null)
            return;
        final String contents = scanResult.getContents();
        if (contents == null)
            return;
        ParseQuery idQuery = new ParseQuery("DevOut");
        idQuery.whereEqualTo("dev_id", contents);
        idQuery.findInBackground(new FindCallback() {

            @Override
            public void done(List result, ParseException ex) {
                if (ex == null) {
                    if (result.size() == 0) {
                        Intent next = new Intent(PhoneListActivity.this,
                                PhoneCheckoutActivity.class);
                        next.putExtra("dev_id", contents);
                        startActivity(next);
                    } else {
                        showDialog(DIALOG_ALREADY_CHECKEDOUT);
                        Toast.makeText(PhoneListActivity.this,
                                "The device is already checkout",
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(PhoneListActivity.this,
                            "Error in querying device id: " + ex.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            }
        });
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
//        case DIALOG_PROGRESS:
//            progress = new ProgressDialog(this);
//            progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//            return progress;
        }
        return builder.create();
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
        switch (item.getItemId())
        {
        case R.id.menu_checkout:
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.initiateScan();
//            *                 Intent scan = new Intent("com.google.zxing.client.android.SCAN");
//                scan.putExtra("SCAN_MODE", "QR_CODE_MODE");
//                startActivityForResult(scan, 0);
            break;
        case R.id.menu_user_upload:
           Intent i = new Intent (this, UploadUsersActivity.class);
           startActivity(i);
        }
//        return  super.onMenuItemSelected(featureId, item);
        return true;
    }

    @Override
    public void deviceRemoved(String dev_id) {
    }
    
    
}
