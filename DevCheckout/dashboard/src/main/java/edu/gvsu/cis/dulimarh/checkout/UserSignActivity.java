package edu.gvsu.cis.dulimarh.checkout;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import bolts.Continuation;
import bolts.Task;
//import android.util.Log;

public class UserSignActivity extends Activity implements OnClickListener{
    final String TAG = getClass().getName();

    private SignatureView signature;
    private Button checkout, clear;
    private TextView devId, userInfo;
    private String userId, userObj, userName, deviceId;

    private ParseObject userPO, devicePO;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.checkout);
        Bundle param;
        if (savedInstanceState != null)
            param = savedInstanceState;
        else
            param = getIntent().getExtras();
        deviceId = param.getString("dev_id");
        userId = param.getString("user_id");
        userName = param.getString("user_name");
        userObj = param.getString("user_obj");
        Log.d("HANS", "Inside UserSignActivity: user_obj is " + userObj);
        devId = (TextView) findViewById(R.id.dev_id);
        userInfo = (TextView) findViewById(R.id.user_info);
        devId.setText("Device: " + deviceId);
        userInfo.setText("User: " + userName + " (" + userId + ")");
        clear = (Button) findViewById(R.id.clear);
//        clear.setEnabled(false);
        checkout = (Button) findViewById(R.id.checkout);
//        checkout.setEnabled(false);
        signature = (SignatureView) findViewById(R.id.sig_imgview);
        checkout.setOnClickListener(this);
        clear.setOnClickListener(this);
        signature.setDrawingCacheEnabled(true);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("dev_id", deviceId);
        outState.putString("user_id", userId);
        outState.putString("user_name", userName);
        outState.putString("user_obj", userObj);
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
            Log.e(TAG, "FileNotFoundException: " + e);
        } catch (IOException e) {
            Log.e(TAG, "IOException: " + e);
        }
    }
    
    private void post()
    {
        Log.d("HANS", "About to save user signature");
        Bitmap sig = signature.getDrawingCache();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        sig.compress(CompressFormat.PNG, 90, stream);
        final ParseFile sigFile = new ParseFile (userId + ".png",
                stream.toByteArray());
        sigFile.saveInBackground()
                .onSuccess(new Continuation<Void, Void>() {
                    @Override
                    public Void then(Task<Void> task) throws
                            Exception {
                        Log.d("HANS", "Signature is saved, " +
                                "about to store checkout record for " +
                                deviceId);
                        ParseQuery<ParseObject> q;
                        q = new ParseQuery<ParseObject>(Consts
                                .ALL_DEVICE_TABLE);
                        q.whereEqualTo("device_id", deviceId);
                        List<ParseObject> qRes = q.find();
                        Log.d("HANS", "Query to " + Consts
                                .ALL_DEVICE_TABLE + " return " + qRes
                                        .size());
                        devicePO = qRes.get(0);
                        q = new ParseQuery<ParseObject>(Consts.USER_TABLE);
                        Log.d("HANS", "Query to " + Consts
                                .USER_TABLE + " for user " + userObj);
                        userPO = q.get(userObj);
                        ParseObject checkout = new ParseObject(Consts.DEVICE_LOAN_TABLE);
                        checkout.put("dev_id", deviceId);
                        checkout.put("device_obj", devicePO);
                        checkout.put("user_id", userId);
                        checkout.put("user_obj", userPO);
                        checkout.put("signature", sigFile);
                        checkout.save();
                        return null;
                    }
                })
        .onSuccess(new Continuation<Void, Void>() {
            @Override
            public Void then(Task<Void> task) throws Exception {
                Log.d("HANS", "Checkout record is saved, " +
                        "sending push notification");
                ParsePushUtils.pushTo(deviceId, "Registered to " + userId);
                setResult(RESULT_OK);
                finish();
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR)
        .continueWith(new Continuation<Void, Object>() {
            @Override
            public Object then(Task<Void> task) throws Exception {
                Log.d("HANS", "Continue at UserSignActivity line 138");
                if (task.isFaulted()) {
                    Log.d("HANS", "Error in checking out " + task
                            .getError().getMessage());
//                    Toast.makeText(DeviceCheckoutActivity.this,
//                            "Cannot checkout device: " + task.getError()
//                            .getMessage(),
//                            Toast.LENGTH_LONG).show();
                    setResult(RESULT_CANCELED);
                    finish();
                }
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);
    }
    
    @Override
    public void onClick(View view) {
        if (view == clear) {
            signature.reset();
        } else {
            post();
        }
    }

}
