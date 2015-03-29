package edu.gvsu.cis.dulimarh.checkout;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

import java.io.ByteArrayOutputStream;
import java.util.List;

import bolts.Continuation;
import bolts.Task;

public class NewDeviceActivity extends Activity implements View.OnClickListener {
    //private String TAG = getClass().getName();
    private TextView devId;
    private EditText devName, devOS, devType;
    private Button saveBtn;


    /* The device QRcode includes the following attributes:
                  id, model, os, form_factor

       The Parse table includes the following columns:
                  device_id, name, os, type
     */
    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.activity_newdevice);
        devId = (TextView) findViewById(R.id.dev_id);
        devName = (EditText) findViewById(R.id.dev_name);
        saveBtn = (Button) findViewById(R.id.save_button);
        devOS = (EditText) findViewById(R.id.dev_os);
        devType = (EditText) findViewById(R.id.dev_form);

        saveBtn.setOnClickListener(this);
        Intent data = getIntent();
        devId.setText(data.getStringExtra("scannedId"));
        devName.setText(data.getStringExtra("scannedModel"));
        devOS.setText(data.getStringExtra("scannedOS"));
        devType.setText(data.getStringExtra("scannedFF"));
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public void onClick(View v) {
        ParseObject newDev = new ParseObject(Consts.ALL_DEVICE_TABLE);
        newDev.put("device_id", devId.getText());
        newDev.put("name", devName.getText().toString());
        newDev.put("os", devOS.getText().toString());
        newDev.put("type", devType.getText().toString());
        newDev.saveInBackground()
                .onSuccess(new Continuation<Void, Object>() {
                    @Override
                    public Object then(Task<Void> task) throws Exception {
//                        Toast.makeText(NewDeviceActivity.this,
//                                "New device saved",
//                                Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                        return null;
                    }
                })
                .continueWith(new Continuation<Object, Object>() {
                    @Override
                    public Object then(Task<Object> task) throws Exception {
                        if (task.isFaulted()) {
                            Toast.makeText(NewDeviceActivity.this,
                                    "Unable to save device information",
                                    Toast.LENGTH_SHORT).show();
                        }
                        return null;
                    }
                });
    }
}
