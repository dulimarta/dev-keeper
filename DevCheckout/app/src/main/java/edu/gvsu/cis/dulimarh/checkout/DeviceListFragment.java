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
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

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

public class DeviceListFragment extends Fragment {
    private static final int DEVICE_CHECKIN_REQUEST = 0xBEEF;
    private final String TAG = getClass().getName();
    private ArrayList<ParseProxyObject> checkouts;
    private Map<String,Drawable> user_photos;
    private RecyclerView myrecyclerview;
    private RecyclerView.Adapter myadapter;
    private RecyclerView.LayoutManager mylayoutmgr;
    private boolean isDualPane;
    private int currentPos;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
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
        user_photos = new HashMap<String, Drawable>();
        myadapter = new MyAdapter(checkouts, user_photos);
        myrecyclerview.setAdapter(myadapter);
        Log.d(TAG, "Initiating ASyncTask to fetch Parse data");
        final LayoutInflater inflater = getActivity().getLayoutInflater();
        if (isDualPane)
            showDetails(currentPos);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("checkouts", checkouts);
//        outState.putInt("currDevice", currentPos);
    }

    /* (non-Javadoc)
         * @see android.app.Fragment#onResume()
         */
    @Override
    public void onResume() {
        super.onResume();
//        // TOOO do we have to run this task everytime?
        if (checkouts.isEmpty()) {
            updateDeviceList();
        }
    }

    public void updateDeviceList()
    {
        new DeviceListTask().execute();
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

    /* (non-Javadoc)
     * @see android.app.ListActivity#onListItemClick(android.widget.ListView, android.view.View, int, long)
     */
//    @Override
//    public void onListItemClick(ListView l, View v, int position, long id) {
//        showDetails (position);
//    }

    private void showDetails (int pos)
    {
        Log.d(TAG, "Data size is " + checkouts.size());
        if (pos >= checkouts.size()) return;
        currentPos = pos;
        ParseProxyObject selected = checkouts.get(pos);
        String uid = selected.getString("user_id");
        String devid = selected.getString("dev_id");
        if (isDualPane)
        {
//            getListView().setItemChecked(pos, true);
            DeviceDetailsFragment ddf = (DeviceDetailsFragment)
                    getFragmentManager().findFragmentById(R.id.devdetails);
            if (ddf == null || ddf.getCurrentIndex() != pos)
            {
                ddf = DeviceDetailsFragment.newInstance(pos,  uid,  devid);
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(R.id.devdetails, ddf);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.commit();
            }
        }
        else {
            Intent i = new Intent(getActivity(), DeviceCheckinActivity.class);
            i.putExtra("index", pos);
            i.putExtra("user_id", uid);
            i.putExtra("dev_id", devid);
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

    private class DeviceListTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
//            adapter.notifyDataSetInvalidated();
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                ParseQuery<ParseObject> checkOutQuery = new ParseQuery<ParseObject>(Consts.DEVICE_LOAN_TABLE);
                checkouts.clear();
                for (ParseObject obj : checkOutQuery.find())
                {
                    checkouts.add(new ParseProxyObject(obj));
                }
                for (ParseProxyObject ppo : checkouts) {
                    final ParseObject usr = ppo.getParseObject("user_obj");
                    usr.fetchIfNeededInBackground(new GetCallback<ParseObject>() {
                        @Override
                        public void done(ParseObject parseObject, ParseException e) {
                            if (e == null) {
                                try {
                                    ParseFile upic = parseObject.getParseFile
                                            ("user_photo");
                                    ByteArrayInputStream bis = new
                                            ByteArrayInputStream(upic.getData());
                                    String uid = usr.getObjectId();
                                    user_photos.put(uid,
                                            Drawable.createFromStream(bis, ""));
                                } catch (ParseException e1) {
                                    e1.printStackTrace();
                                }
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
            } catch (ParseException e) {
                // TODO Auto-generated catch block
                Log.e("HANS", "Failed to run query " + e.getMessage());
                e.printStackTrace();
            }
            return null;
        }

        /* (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Void result) {
            myadapter.notifyDataSetChanged();
        }

    }

}
