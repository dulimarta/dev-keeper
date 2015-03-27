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

import bolts.Continuation;
import bolts.Task;
//import android.util.Log;

public class DeviceCheckoutActivity extends Activity implements OnClickListener{
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
        Bundle param = getIntent().getExtras();
        deviceId = param.getString("dev_id");
        userId = param.getString("user_id");
        userName = param.getString("user_name");
        userObj = param.getString("user_obj");
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
                        ParseQuery<ParseObject> q;
                        q = new ParseQuery<ParseObject>(Consts
                                .ALL_DEVICE_TABLE);
                        q.whereEqualTo("device_id", deviceId);
                        devicePO = q.find().get(0);
                        q = new ParseQuery<ParseObject>(Consts.USER_TABLE);
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
                ParsePushUtils.pushTo(deviceId, "Registered to " + userId);
                setResult(RESULT_OK);
                finish();
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR)
        .continueWith(new Continuation<Void, Object>() {
            @Override
            public Object then(Task<Void> task) throws Exception {
                if (task.isFaulted()) {
//                    Toast.makeText(DeviceCheckoutActivity.this,
//                            "Cannot checkout device: " + task.getError()
//                            .getMessage(),
//                            Toast.LENGTH_LONG).show();
                    setResult(RESULT_CANCELED);
                    finish();
                }
                return null;
            }
        });
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
