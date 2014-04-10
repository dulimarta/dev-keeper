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

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.io.ByteArrayInputStream;
import java.util.List;

public class DeviceDetailsFragment extends Fragment {
    private final static int DIALOG_CONFIRM_CHECKIN = 1;
    private final static int DIALOG_WRONG_DEVICE = 2;

    private final String TAG = getClass().getName();
    private String user_id, dev_id, parseObjId;
    private int currentIndex;
    private TextView uid, devid, date;
    private ImageView sig;
    private ParseFile parseSignatureFile;
    private Button checkin; //, ping;
    private DeviceRemovalListener callback;
    
    public interface DeviceRemovalListener {
        public void deviceRemoved (String dev_id);
    }
    
    public static DeviceDetailsFragment newInstance (int index, 
            String user_id, String dev_id)
    {
        DeviceDetailsFragment frag = new DeviceDetailsFragment();
        Bundle args = new Bundle();
        args.putString("user_id", user_id);
        args.putString("dev_id", dev_id);
        args.putInt("currentIndex", index);
        frag.setArguments(args);
        return frag;
    }

    
    /* (non-Javadoc)
     * @see android.app.Fragment#onAttach(android.app.Activity)
     */
    @Override
    public void onAttach(Activity activity) {
        // TODO Auto-generated method stub
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
        
        Bundle args = savedInstanceState != null? savedInstanceState : getArguments();
        user_id = args.getString("user_id");
        dev_id = args.getString("dev_id");
        currentIndex = args.getInt("currentIndex");
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
        date = (TextView) v.findViewById(R.id.out_date);
        sig = (ImageView) v.findViewById(R.id.sig_imgview);
        sig.setVisibility(View.INVISIBLE);
        checkin = (Button) v.findViewById(R.id.checkin);
//        ping = (Button) v.findViewById(R.id.ping);
        checkin.setEnabled(false);
        devQuery.whereEqualTo("user_id", user_id);
        checkin.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                Intent scan = new Intent("com.google.zxing.client.android.SCAN");
                scan.putExtra("SCAN_MODE", "QR_CODE_MODE");
                startActivityForResult(scan, 0);
            }
        });
//        ping.setOnClickListener(new PingHandler());
        devQuery.findInBackground(new FindCallback<ParseObject>() {
            
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
                                    getActivity(),
                                    "Failed to download signature of "
                                            + user_id, Toast.LENGTH_LONG)
                                    .show();
                        }
                    else {
                        Toast.makeText(
                                getActivity(),
                                "Could not find a unique record", Toast.LENGTH_LONG)
                                .show();
                    }
                }
            }
        });
        
//        if (container == null)
//            return null;
        return v;
    }

    
    /* Use an inner class in place of direct instance variable declaration
     * to avoid "Parse.Initialize" error when this class is loaded by JVM
     */
//    private class PingHandler implements OnClickListener {
//
//        @Override
//        public void onClick(View v) {
//            try {
//                JSONObject pingMsg = new JSONObject(
//                        "{\"action\": \"edu.gvsu.cis.checkout.UPDATE\"," +
//                        "\"message\" : \"Ping\"}"
//                        );
//                ParsePush notify = new ParsePush();
//                notify.setChannel("Hans");
//                notify.setData(pingMsg);
//                Log.d(TAG, "Sending " + pingMsg.toString(3));
//                notify.sendInBackground(new SendCallback() {
//                    
//                    @Override
//                    public void done(ParseException arg0) {
//                        if (arg0 == null)
//                            Toast.makeText(getActivity(), "Ping delivered", 
//                                Toast.LENGTH_LONG).show();
//                        else
//                            Toast.makeText(getActivity(), 
//                                    "ParsePush error: " + arg0.getMessage(), 
//                                    Toast.LENGTH_LONG).show();
//                    }
//                });
//            } catch (JSONException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//        }
//        
//    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.d(TAG, "Hosting activity is created");
        // TODO Auto-generated method stub
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0) {
            if (resultCode == Activity.RESULT_OK) {
                final String contents = data.getStringExtra("SCAN_RESULT");
//                final String expected_dev_id = dev_id;
                if (contents.equals(dev_id))
                    showDialog(DIALOG_CONFIRM_CHECKIN, dev_id, parseObjId);
                else {
                    showDialog(DIALOG_WRONG_DEVICE, dev_id, contents);
                }
            }
            return;
        }
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
            builder.setMessage("Deregister device + " + dev_id + "?");
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ParseQuery<ParseObject> qr = new ParseQuery<ParseObject>(Consts.DEVICE_LOAN_TABLE);
                    qr.getInBackground(parseId, new GetCallback<ParseObject>() {

                        @Override
                        public void done(ParseObject arg0, ParseException arg1) {
                            arg0.deleteInBackground();
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
