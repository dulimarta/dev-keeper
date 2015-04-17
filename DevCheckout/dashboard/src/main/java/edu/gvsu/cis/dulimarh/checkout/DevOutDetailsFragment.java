package edu.gvsu.cis.dulimarh.checkout;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;

import edu.gvsu.cis.dulimarh.checkout.custom_ui.FloatingActionButton;

public class DevOutDetailsFragment extends Fragment {
    private final static int DIALOG_CONFIRM_CHECKIN = 1;
    private final static int DIALOG_WRONG_DEVICE = 2;

    private final String TAG = getClass().getName();
    private String user_id, dev_id, obj_id;
    private int currentIndex;
    private TextView uid, devid, devname, devtype, date;
    private ImageView sig, userphoto;
    //private ParseFile parseSignatureFile;
    private FloatingActionButton checkin; //, ping;
    private DeviceRemovalListener callback;
    
    public interface DeviceRemovalListener {
        public void deviceRemoved (String dev_id);
    }
    
    public static DevOutDetailsFragment newInstance (int index,
            String object_id)
    {
        DevOutDetailsFragment frag = new DevOutDetailsFragment();
        Bundle args = new Bundle();
        args.putString("object_id", object_id);
        args.putInt("index", index);
        frag.setArguments(args);
        return frag;
    }

    
    /* (non-Javadoc)
     * @see android.app.Fragment#onAttach(android.app.Activity)
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            callback = (DeviceRemovalListener) activity;
        }
        catch (ClassCastException cce) {
            throw new ClassCastException(activity.toString() + 
                    " must implement DeviceRemovalListener");
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Bundle args; // = savedInstanceState != null? savedInstanceState :
                getArguments();
        if (savedInstanceState == null) {
            args = getArguments();
            obj_id = args.getString("object_id");
            currentIndex = args.getInt("index");
        }
        else {
            user_id = savedInstanceState.getString("user_id");
            dev_id = savedInstanceState.getString("dev_id");
            currentIndex = savedInstanceState.getInt("currentIndex");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("user_id", user_id);
        outState.putString("dev_id", dev_id);
        outState.putInt("currentIndex", currentIndex);
    }

    public int getCurrentIndex ()
    {
        return currentIndex;
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_devdetails, container, false);
        ParseQuery<ParseObject> devQuery = new ParseQuery<ParseObject>(Consts.DEVICE_LOAN_TABLE);
        uid = (TextView) v.findViewById(R.id.user_id);
        devid = (TextView) v.findViewById(R.id.dev_id);
        devname = (TextView) v.findViewById(R.id.dev_name);
        devtype = (TextView) v.findViewById(R.id.dev_type);
        date = (TextView) v.findViewById(R.id.out_date);

        sig = (ImageView) v.findViewById(R.id.sig_imgview);
        userphoto = (ImageView) v.findViewById(R.id.user_photo);
        sig.setVisibility(View.INVISIBLE);
        checkin = (FloatingActionButton) v.findViewById(R.id.checkin);
//        ping = (Button) v.findViewById(R.id.ping);
        checkin.setEnabled(false);
//        devQuery.whereEqualTo("dev_id", dev_id);
        checkin.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                IntentIntegrator scan = new IntentIntegrator
                        (DevOutDetailsFragment.this);
                scan.initiateScan();
            }
        });
//        ping.setOnClickListener(new PingHandler());

//        findDev(devQuery).ons


        devQuery.getInBackground(obj_id, new GetCallback<ParseObject>() {

            @Override
            public void done(ParseObject obj, ParseException err) {
                if (err == null) {
                    try {
                        user_id = obj.getString("user_id");
                        uid.setText(user_id);
                        dev_id = obj.getString("dev_id");
                        devid.setText(dev_id);
                        ParseObject devObj = obj.getParseObject
                                ("device_obj");
                        devObj.fetchIfNeeded();
                        ParseObject userObj = obj.getParseObject
                                ("user_obj");
                        userObj.fetchIfNeeded();
                        devname.setText(devObj.getString("name"));
                        devtype.setText(devObj.getString("type"));
                        date.setText(obj.getCreatedAt().toLocaleString());
                        ParseFile pf = (ParseFile) obj
                                .getParseFile("signature");
                        Drawable storedDrawable = ImageStore.get
                                (pf.getUrl());
                        if (storedDrawable == null) {
                            ByteArrayInputStream bis = new
                                    ByteArrayInputStream(pf.getData());
                            Drawable d = Drawable.createFromStream(bis, "");
                            ImageStore.put(pf.getUrl(), d);
                            sig.setImageDrawable(d);
                        } else
                            sig.setImageDrawable(storedDrawable);
                        pf = (ParseFile) userObj.getParseFile
                                ("user_photo");
                        if (pf != null) {
                            storedDrawable = ImageStore.get(pf.getUrl());
                            if (storedDrawable == null) {
                                ByteArrayInputStream bis = new
                                        ByteArrayInputStream(pf.getData());
                                Drawable d = Drawable.createFromStream(bis,
                                        "");
                                ImageStore.put(pf.getUrl(), d);
                                userphoto.setImageDrawable(d);
                            } else
                                userphoto.setImageDrawable(storedDrawable);
                        }
                        sig.setVisibility(View.VISIBLE);
                        checkin.setEnabled(true);

                    } catch (ParseException e) {
                        Toast.makeText(
                                getActivity(),
                                "Failed to download signature of "
                                        + user_id, Toast.LENGTH_LONG)
                                .show();
                    }
                }
            }
        });
        
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.d(TAG, "Hosting activity is created");
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {

            IntentResult scanResult = IntentIntegrator.parseActivityResult
                    (requestCode, resultCode, data);
            if (scanResult != null) {
                final String contents = data.getStringExtra("SCAN_RESULT");
                if (contents == null) return;
            /* JSON keys: id, model, os, form_factor */

                try {
                    JSONObject jObj = new JSONObject(contents);
                    String scanned = jObj.getString("id");
                    if (scanned.equals(dev_id))
                        showDialog(DIALOG_CONFIRM_CHECKIN, dev_id, obj_id);
                    else {
                        showDialog(DIALOG_WRONG_DEVICE, dev_id, contents);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return;
            }
        }
        else
            super.onActivityResult(requestCode, resultCode, data);
    }

    private void showDialog (int which, String ... parms)
    {
        DialogFragment diafrag;
        if (which == DIALOG_CONFIRM_CHECKIN) {
            diafrag = CheckinConfirmDialog.newInstance(callback, parms[0],
                    parms[1]);
            diafrag.show(getFragmentManager(), "dialog");
        }
        else if (which == DIALOG_WRONG_DEVICE) {
            Toast.makeText(getActivity(), "Unmatch device id scanned",
                    Toast.LENGTH_SHORT).show();
        }
    }

    public static class CheckinConfirmDialog extends DialogFragment {
        private static DeviceRemovalListener hostActivity;
        public static CheckinConfirmDialog newInstance (DeviceRemovalListener act,
                String devId, String parseId)
        {
            hostActivity = act;
            CheckinConfirmDialog frag = new CheckinConfirmDialog();
            Bundle args = new Bundle();
            args.putString("dev_id", devId);
            args.putString("parse_id", parseId);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle unused) {
            final String dev_id = getArguments().getString("dev_id");
            final String parseId = getArguments().getString("parse_id");
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Confirmation Required");
            builder.setMessage("Deregister device " + dev_id + "?");
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ParseQuery<ParseObject> qr = new ParseQuery<ParseObject>(Consts.DEVICE_LOAN_TABLE);
                    qr.getInBackground(parseId, new GetCallback<ParseObject>() {

                        @Override
                        public void done(ParseObject obj,
                                         ParseException arg1) {
                            ParseObject logObj = new ParseObject(Consts
                                    .LOG_TABLE);
                            logObj.put("dev_id", obj.getString("dev_id"));
                            logObj.put("signature",
                                    obj.getParseFile("signature"));
                            logObj.put("user_id", obj.getString("user_id"));
                            logObj.put("checkout_date", obj.getCreatedAt());
                            logObj.saveInBackground();
                            obj.deleteInBackground();
                            hostActivity.deviceRemoved(dev_id);
                        }
                    });
                }
            });
            builder.setNegativeButton("No", null);
            return builder.create();
        }
                        
    }
}
