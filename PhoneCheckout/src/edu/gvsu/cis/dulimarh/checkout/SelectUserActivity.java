package edu.gvsu.cis.dulimarh.checkout;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;


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
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        Window win = getWindow();
        win.setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND , WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        LayoutParams params = win.getAttributes();
        params.width = 800;
        params.dimAmount = 0.3f;
        win.setAttributes(params);
        setContentView(R.layout.activity_selectuser);
        setTitle("Select User");
    }
    
    
    /* (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater pump = getMenuInflater();
        pump.inflate(R.menu.select_user_add, menu);
        return super.onCreateOptionsMenu(menu);
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.add_user_menu)
        {
            startActivity(new Intent(this, NewUserActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
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
            public void done(List<ParseObject> result, ParseException ex) {
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
