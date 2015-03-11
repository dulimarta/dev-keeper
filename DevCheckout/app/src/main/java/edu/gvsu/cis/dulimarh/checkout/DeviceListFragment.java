package edu.gvsu.cis.dulimarh.checkout;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.Fragment;
import android.content.Intent;
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

import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class DeviceListFragment extends Fragment {
    private static final int DEVICE_CHECKIN_REQUEST = 0xBEEF;
    private final String TAG = getClass().getName();
    private ArrayList<Bundle> checkouts;
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
            checkouts = savedInstanceState.getParcelableArrayList("checkouts");
        else
            checkouts = new ArrayList<Bundle>();
        myadapter = new MyAdapter(checkouts);
        myrecyclerview.setAdapter(myadapter);
        Log.d(TAG, "Initiating ASyncTask to fetch Parse data");
//        adapter = new SimpleAdapter(getActivity(), checkouts, android.R.layout.simple_list_item_2,
//                new String[]{"dev_id", "user_id"},
//                new int[]{android.R.id.text1, android.R.id.text2});
        final LayoutInflater inflater = getActivity().getLayoutInflater();
//        adapter = new ArrayAdapter<Bundle>(getActivity(), android.R.layout.simple_list_item_2, checkouts) {
//            @Override
//            public View getView(int position, View convertView, ViewGroup parent) {
//                Pair<TextView,TextView> cache;
//                if (convertView == null) {
//                    convertView = inflater.inflate(android.R.layout.simple_list_item_2, parent, false);
//                    TextView t1 = (TextView) convertView.findViewById(android.R.id.text1);
//                    TextView t2 = (TextView) convertView.findViewById(android.R.id.text2);
//                    cache = new Pair<TextView, TextView>(t1, t2);
//                    convertView.setTag(cache);
//                }
//                else {
//                    cache = (Pair<TextView, TextView>) convertView.getTag();
//                }
//                cache.first.setText(checkouts.get(position).getString("dev_id"));
//                cache.second.setText(checkouts.get(position).getString("user_id"));
//                return convertView;
//            }
//        };
//        setListAdapter(adapter);
        if (isDualPane)
            showDetails(currentPos);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("checkouts", checkouts);
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
        Bundle selected = checkouts.get(pos);
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
                    Bundle dev_u = new Bundle();
                    dev_u.putString("dev_id", obj.getString("dev_id"));
                    dev_u.putString("user_id", obj.getString("user_id"));
//                    dev_u.put
                    checkouts.add(dev_u);
                }
                Collections.sort(checkouts, new Comparator<Bundle>() {

                    @Override
                    public int compare(Bundle one, Bundle two) {
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
