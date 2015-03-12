package edu.gvsu.cis.dulimarh.checkout;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class DeviceListFragment extends Fragment implements
        DevOutAdapter.DeviceSelectedListener{
    private static final int DEVICE_CHECKIN_REQUEST = 0xBEEF;
    private final String TAG = getClass().getName();
    private ArrayList<ParseProxyObject> checkouts;
//    private Map<String,Drawable> user_photos;
    private RecyclerView myrecyclerview;
    private RecyclerView.Adapter myadapter;
    private RecyclerView.LayoutManager mylayoutmgr;
    private boolean isDualPane;
    private int currentPos;

    @Override
    public void onActivityCreated(Bundle savedInstanceState)  {
        Log.d(TAG, "Hosting activity created");
        super.onActivityCreated(savedInstanceState);
        View v = getActivity().findViewById(R.id.devdetails);
        isDualPane = v != null && v.getVisibility() == View.VISIBLE;
        Log.d(TAG, "Dual pane = " + isDualPane);
        currentPos = 0;
        if (savedInstanceState != null)
            checkouts = (ArrayList<ParseProxyObject>) savedInstanceState
                    .getSerializable("checkouts");
        else
            checkouts = new ArrayList<ParseProxyObject>();
//        user_photos = new HashMap<String, Drawable>();
        myadapter = new DevOutAdapter(checkouts, this);
        myrecyclerview.setAdapter(myadapter);
        Log.d(TAG, "Initiating ASyncTask to fetch Parse data");
        final LayoutInflater inflater = getActivity().getLayoutInflater();
        if (isDualPane && checkouts.size() > 0)
            showDetails(currentPos);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("checkouts", checkouts);
//        outState.putSerializable("", user_photos);
//        outState.putInt("currDevice", currentPos);
    }

    /* (non-Javadoc)
         * @see android.app.Fragment#onResume()
         */
    @Override
    public void onResume() {
        super.onResume();
        // TOOO do we have to run this task everytime?
        if (checkouts.isEmpty()) {
            updateDeviceList();
        }
    }

    public void updateDeviceList()
    {

        try {
            ParseQuery<ParseObject> checkOutQuery = new ParseQuery<ParseObject>(Consts.DEVICE_LOAN_TABLE);
            checkouts.clear();
            for (ParseObject obj : checkOutQuery.find())
            {
                checkouts.add(new ParseProxyObject(obj));
            }
            for (ParseProxyObject ppo : checkouts) {
                final String usr = ppo.getParseObject("user_obj");
                ParseQuery<ParseObject> query = ParseQuery.getQuery
                        ("Users");
                query.getInBackground("usr", new GetCallback<ParseObject>
                        () {
                    @Override
                    public void done(ParseObject parseObject, ParseException e) {
                        if (e == null) {
                            ImageStore.extractFrom (parseObject);
                        }
                    }
                });
            }
            Collections.sort(checkouts, new Comparator<ParseProxyObject>() {

                @Override
                public int compare(ParseProxyObject one,
                                   ParseProxyObject two) {
                    return one.getString("user_id").compareTo(two.getString("user_id"));
                }
            });
            myadapter.notifyDataSetChanged();
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            Log.e("HANS", "Failed to run query " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_devlist, container, false);
        myrecyclerview = (RecyclerView) v.findViewById(R.id.devlist);
        myrecyclerview.setHasFixedSize(true);
        mylayoutmgr = new LinearLayoutManager(getActivity());
        myrecyclerview.setLayoutManager(mylayoutmgr);
        return v;
    }

    private void showDetails (int pos)
    {
        Log.d(TAG, "Data size is " + checkouts.size());
        if (pos >= checkouts.size()) return;
        currentPos = pos;
        ParseProxyObject selected = checkouts.get(pos);
//        String uid = selected.getString("user_id");
//        String devid = selected.getString("dev_id");
//        String devname = selected.getString()
        if (isDualPane)
        {
//            getListView().setItemChecked(pos, true);
            DeviceDetailsFragment ddf = (DeviceDetailsFragment)
                    getFragmentManager().findFragmentById(R.id.devdetails);
            if (ddf == null || ddf.getCurrentIndex() != pos)
            {
                ddf = DeviceDetailsFragment.newInstance(pos, selected.getObjectId());
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(R.id.devdetails, ddf);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.commit();
            }
        }
        else {
            Intent i = new Intent(getActivity(), DeviceCheckinActivity.class);
//            Bundle b = new Bundle();
//            b.putSerializable("selected", selected);
//            i.putExtras(b);
            i.putExtra("index", pos);
            i.putExtra("object_id", selected.getObjectId());
//            i.putExtra("dev_name", )
//            i.putExtra("dev_id", devid);
            startActivityForResult(i, DEVICE_CHECKIN_REQUEST);
        }
//        super.onListItemClick(l, v, position, id);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DEVICE_CHECKIN_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                updateDeviceList();
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDeviceSelected(int pos) {
        showDetails(pos);

    }
}
