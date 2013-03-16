package edu.gvsu.cis.dulimarh.checkout;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.SendCallback;

public class PhoneCheckinActivity extends Activity {
    private final String TAG = getClass().getName();
    
    private final static int DIALOG_CONFIRM_CHECKIN = 1;
    private final static int DIALOG_WRONG_DEVICE = 2;
    private String user_id, dev_id, parseObjId;
    private TextView uid, devid, date;
    private ImageView sig;
    private ParseFile parseSignatureFile;
    private Button checkin, ping;
    
    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ParseQuery devQuery = new ParseQuery("DevOut");
        Intent data = getIntent();
        user_id = data.getStringExtra("user_id");
        dev_id = data.getStringExtra("dev_id");
        setContentView(R.layout.checkin);
        uid = (TextView) findViewById(R.id.user_id);
        devid = (TextView) findViewById(R.id.dev_id);
        date = (TextView) findViewById(R.id.out_date);
        sig = (ImageView) findViewById(R.id.sig_imgview);
        sig.setVisibility(View.INVISIBLE);
        checkin = (Button) findViewById(R.id.checkin);
        ping = (Button) findViewById(R.id.ping);
        checkin.setEnabled(false);
        devQuery.whereEqualTo("user_id", user_id);
        //        showDialog(DIALOG_PROGRESS);
        /* TODO: Add "Send email button"? */
        checkin.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                Intent scan = new Intent("com.google.zxing.client.android.SCAN");
                scan.putExtra("SCAN_MODE", "QR_CODE_MODE");
                startActivityForResult(scan, 0);
            }
        });
        ping.setOnClickListener(new PingHandler());
        devQuery.findInBackground(new FindCallback() {
            
            @Override
            public void done(List<ParseObject> arg0, ParseException arg1) {
                if (arg1 == null) {
                    if (arg0.size() == 1)
                        try {
                            ParseObject obj = arg0.get(0);
                            parseObjId = obj.getObjectId();
                            parseSignatureFile = (ParseFile) obj.get("signature");
                            uid.setText(obj.getString("user_id"));
                            devid.setText(obj.getString("dev_id"));
                            date.setText(obj.getCreatedAt().toLocaleString());
                            ByteArrayInputStream bis = new ByteArrayInputStream(parseSignatureFile.getData());
                            Drawable d = Drawable.createFromStream(bis, "");
                            sig.setImageDrawable(d);
                            sig.setVisibility(View.VISIBLE);
                            checkin.setEnabled(true);
                            
                        } catch (ParseException e) {
                            Toast.makeText(
                                    PhoneCheckinActivity.this,
                                    "Failed to download signature of "
                                            + user_id, Toast.LENGTH_LONG)
                                    .show();
                        }
                    else {
                        Toast.makeText(
                                PhoneCheckinActivity.this,
                                "Could not find a unique record", Toast.LENGTH_LONG)
                                .show();
                    }
                }
            }
        });
        
    }
    
    /* Use an inner class in place of direct instance variable declaration
     * to avoid "Parse.Initialize" error when this class is loaded by JVM
     */
    private class PingHandler implements OnClickListener {

        @Override
        public void onClick(View v) {
            try {
                JSONObject pingMsg = new JSONObject(
                        "{\"action\": \"edu.gvsu.cis.checkout.UPDATE\"," +
                        "\"message\" : \"Ping\"}"
                        );
                ParsePush notify = new ParsePush();
                notify.setChannel("Hans");
                notify.setData(pingMsg);
                Log.d(TAG, "Sending " + pingMsg.toString(3));
                notify.sendInBackground(new SendCallback() {
                    
                    @Override
                    public void done(ParseException arg0) {
                        if (arg0 == null)
                            Toast.makeText(PhoneCheckinActivity.this, "Ping delivered", 
                                Toast.LENGTH_LONG).show();
                        else
                            Toast.makeText(PhoneCheckinActivity.this, 
                                    "ParsePush error: " + arg0.getMessage(), 
                                    Toast.LENGTH_LONG).show();
                    }
                });
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
    }
    /* (non-Javadoc)
     * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                final String contents = data.getStringExtra("SCAN_RESULT");
                Bundle dialogData = new Bundle();
                dialogData.putString("dev_id", dev_id);
                if (contents.equals(dev_id))
                    showDialog(DIALOG_CONFIRM_CHECKIN, dialogData);
                else {
                    dialogData.putString("scan_result", contents);
                    showDialog(DIALOG_WRONG_DEVICE, dialogData);
                }
            }
            else if (resultCode == RESULT_CANCELED) {
                Log.d(TAG, "Scanning cancelled");
            }
        }
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onCreateDialog(int, android.os.Bundle)
     */
    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        switch (id) {
        case DIALOG_CONFIRM_CHECKIN:
            builder.setTitle("Confirmation Required");
            builder.setMessage("Check in device " + args.getString("dev_id") + "?");
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ParseQuery qr = new ParseQuery("DevOut");
                    qr.getInBackground(parseObjId, new GetCallback() {

                        @Override
                        public void done(ParseObject arg0, ParseException arg1) {
                            arg0.deleteInBackground();
                            finish();
                        }
                    });
                }
            });
            builder.setNegativeButton("No", null);
            break;
        case DIALOG_WRONG_DEVICE:
            builder.setTitle("Error: Unmatched IDs");
            builder.setMessage("Scanned id: " + args.getString("scan_result") + 
                    " device id: " + args.getString("dev_id"));
            builder.setPositiveButton("OK", null);
        }
        return builder.create();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onPrepareDialog(int, android.app.Dialog, android.os.Bundle)
     */
    @Override
    protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
        AlertDialog d = (AlertDialog) dialog;
        switch (id) {
        case DIALOG_CONFIRM_CHECKIN:
            d.setMessage("Check in device " + args.getString("dev_id") + "?");
            break;
        case DIALOG_WRONG_DEVICE:
            d.setMessage("Scanned id: " + args.getString("scan_result") + 
                    " device id: " + args.getString("dev_id"));
        }
    }

    
}
