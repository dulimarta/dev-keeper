package edu.gvsu.cis.dulimarh.checkout;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

public class PhoneCheckoutActivity extends Activity {
    final String TAG = getClass().getName();

    private SignatureView signature;
    private Button checkout, clear;
    private TextView devId;
    private Spinner uid;
    private ArrayList<CharSequence> allUsers;
    private ArrayAdapter<CharSequence> adapt;
    private String deviceId;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Bundle param = getIntent().getExtras();
        deviceId = param.getString("dev_id");
        allUsers = new ArrayList<CharSequence>();
        devId = (TextView) findViewById(R.id.dev_id);
        devId.setText("Device ID: " + deviceId);
        uid = (Spinner) findViewById(R.id.userid);
        clear = (Button) findViewById(R.id.clear);
        checkout = (Button) findViewById(R.id.checkin);
        signature = (SignatureView) findViewById(R.id.sig_imgview);
        adapt = new ArrayAdapter<CharSequence>(PhoneCheckoutActivity.this, 
                android.R.layout.simple_spinner_item, allUsers);
        adapt.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        uid.setAdapter(adapt);
        uid.setOnItemSelectedListener(uidChooser);
        UserTask uname = new UserTask();
        uname.execute(/*USER_URL*/);
        checkout.setOnClickListener(btnHandler);
        clear.setOnClickListener(btnHandler);
        signature.setDrawingCacheEnabled(true);
    }
    
    protected void saveBitmap(String name) {
        super.onDestroy();
        Bitmap bmp = signature.getDrawingCache();
        Log.d (TAG, "Bitmap dimension: " + bmp.getHeight() + "x" + bmp.getWidth());
        String filename = Environment.getExternalStorageDirectory() + "/" + name;
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(filename);
            bmp.compress(CompressFormat.PNG, 90, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "FileNotFoundException: " + e);
        } catch (IOException e) {
            Log.e(TAG, "IOException: " + e);
        }
    }
    
    private void post()
    {
        Bitmap sig = signature.getDrawingCache();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        sig.compress(CompressFormat.PNG, 90, stream);
        ParseFile sigFile = new ParseFile ((String) uid.getSelectedItem() + ".png",
                stream.toByteArray());
        try {
            sigFile.save();
            ParseObject checkout = new ParseObject("DevOut");
            checkout.put("dev_id", deviceId);
            checkout.put("user_id", (String)uid.getSelectedItem());
            checkout.put("signature", sigFile);
            checkout.saveInBackground(new SaveCallback() {
                
                @Override
                public void done(ParseException e) {
                    if (e == null)
                        finish();
                    else
                        Toast.makeText(PhoneCheckoutActivity.this, 
                                "Cannot checkout device: " + e.getMessage(), 
                                Toast.LENGTH_LONG).show();
                }
            });
        } catch (ParseException e1) {
            e1.printStackTrace();
            Toast.makeText(this, "Cannot save signature file: " + e1.getMessage(), 
                    Toast.LENGTH_LONG).show();
        }
    }
    
    private OnItemSelectedListener uidChooser = new OnItemSelectedListener() {

        @Override
        public void onItemSelected(AdapterView<?> arg0, View arg1, int pos,
                long arg3) {
//            uid.setPrompt(allUsers.get(pos));
            Log.d(TAG, "User: " + allUsers.get(pos));
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            // TODO Auto-generated method stub
            
        }
    };
    private OnClickListener btnHandler = new OnClickListener() {
        
        @Override
        public void onClick(View v) {
            if (v == clear) {
                signature.reset();
            } else {
                post();
                finish();
            }
        }
    };

    private class UserTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {
            try {
                ParseQuery userQuery = new ParseQuery("Users");
                allUsers.clear();
                for (ParseObject obj : userQuery.find())
                {
                    allUsers.add(obj.getString("user_id"));
                }
            } catch (ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return null;
        }

        /* (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPostExecute(Void result) {
            Log.d(TAG, "List has " + allUsers.size() + " items");
            adapt.notifyDataSetChanged();
        }
        
    }
}