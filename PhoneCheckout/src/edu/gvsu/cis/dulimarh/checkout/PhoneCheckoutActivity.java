package edu.gvsu.cis.dulimarh.checkout;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.SaveCallback;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
//import android.util.Log;

public class PhoneCheckoutActivity extends Activity {
    final String TAG = getClass().getName();

    private SignatureView signature;
    private Button checkout, clear;
    private TextView devId, userInfo;
    private String userId, userName, deviceId;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.checkout);
        Bundle param = getIntent().getExtras();
        deviceId = param.getString("dev_id");
        userId = param.getString("user_id");
        userName = param.getString("user_name");
        devId = (TextView) findViewById(R.id.dev_id);
        userInfo = (TextView) findViewById(R.id.user_info);
        devId.setText("What: " + deviceId);
        userInfo.setText("User: " + userName + " (" + userId + ")");
        clear = (Button) findViewById(R.id.clear);
        checkout = (Button) findViewById(R.id.checkout);
        signature = (SignatureView) findViewById(R.id.sig_imgview);
        checkout.setOnClickListener(btnHandler);
        clear.setOnClickListener(btnHandler);
        signature.setDrawingCacheEnabled(true);
    }
    
    protected void saveBitmap(String name) {
        super.onDestroy();
        Bitmap bmp = signature.getDrawingCache();
        String filename = Environment.getExternalStorageDirectory() + "/" + name;
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(filename);
            bmp.compress(CompressFormat.PNG, 90, fos);
            fos.close();
        } catch (FileNotFoundException e) {
//            Log.e(TAG, "FileNotFoundException: " + e);
        } catch (IOException e) {
//            Log.e(TAG, "IOException: " + e);
        }
    }
    
    private void post()
    {
        Bitmap sig = signature.getDrawingCache();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        sig.compress(CompressFormat.PNG, 90, stream);
        ParseFile sigFile = new ParseFile (userId + ".png",
                stream.toByteArray());
        try {
            sigFile.save();
            ParseObject checkout = new ParseObject(Consts.DEVICE_LOAN_TABLE);
            checkout.put("dev_id", deviceId);
            checkout.put("user_id", userId);
            checkout.put("signature", sigFile);
            checkout.saveInBackground(new SaveCallback() {
                
                @Override
                public void done(ParseException e) {
                    if (e == null) {
                        ParsePushUtils.pushTo(deviceId, "ADDED");
                        finish();
                    }
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
    
    private OnClickListener btnHandler = new OnClickListener() {
        
        @Override
        public void onClick(View v) {
            if (v == clear) {
                signature.reset();
            } else {
                post();
            }
        }
    };

//    private class IDComparator implements Comparator<CharSequence> {
//
//        @Override
//        public int compare(CharSequence lhs, CharSequence rhs) {
//            return lhs.toString().compareTo(rhs.toString());
//        }
//        
//    }
}
