package edu.gvsu.cis.dulimarh.checkout;

import java.util.List;
import java.util.Map;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;


public class SelectUserActivity extends Activity implements SelectUserFragment.OnUserSelectedListener {

    private static final int DIALOG_ALREADY_CHECKEDOUT = 1;
    
    private String selectedUid, selectedUname;
    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selectuser);
    }
    
    /* (non-Javadoc)
     * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (scanResult == null) return;
        final String contents = scanResult.getContents();
        if (contents == null) return;
        ParseQuery<ParseObject> idQuery = new ParseQuery<ParseObject>("DevOut");
        idQuery.whereEqualTo("dev_id", contents);
        idQuery.findInBackground(new FindCallback<ParseObject>() {

            @Override
            public void done(List result, ParseException ex) {
                if (ex == null) {
                    if (result.size() == 0) {
                        /* Pass userid, username, and device id to the next activity */
                        Intent next = new Intent(SelectUserActivity.this,
                                PhoneCheckoutActivity.class);
                        //Map<String,Object> uMap = allUsers.get(selectedPosition);
                        next.putExtra("user_id", selectedUid); 
                        next.putExtra("user_name", selectedUname); 
                        next.putExtra("dev_id", contents);
                        startActivity(next);
                        finish();
                    } else {
                        showDialog(DIALOG_ALREADY_CHECKEDOUT);
                        Toast.makeText(SelectUserActivity.this,
                                "The device is already checkout",
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(SelectUserActivity.this,
                            "Error in querying device id: " + ex.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            }
        });
        //super.onActivityResult(requestCode, resultCode, data);
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
    public void onUserSelected(String uid, String uname) {
        selectedUid = uid;
        selectedUname = uname;
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.initiateScan();
    }


}
