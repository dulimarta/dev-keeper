package edu.gvsu.cis.dulimarh.checkout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

public class DeviceListFragment extends ListFragment {
    private final String TAG = getClass().getName();
    private ArrayList<Map<String,String>> checkouts;
    private SimpleAdapter adapter;
    private boolean isDualPane;
    private int currentPos;
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        Log.d(TAG, "Hosting activity created");
        super.onActivityCreated(savedInstanceState);
        View v = getActivity().findViewById(R.id.devdetails);
        isDualPane = v != null && v.getVisibility() == View.VISIBLE;
        Log.d(TAG, "Dual pane = " + isDualPane);
            currentPos = 0;
            checkouts = new ArrayList<Map<String,String>>();
            Log.d(TAG, "Initiating ASyncTask to fetch Parse data");
            adapter = new SimpleAdapter(getActivity(), checkouts, android.R.layout.simple_list_item_2, 
                    new String[] {"dev_id", "user_id"},
                    new int[] {android.R.id.text1, android.R.id.text2});
            setListAdapter(adapter);
            if (isDualPane)
                showDetails (currentPos);
    }

    /* (non-Javadoc)
     * @see android.app.Fragment#onResume()
     */
    @Override
    public void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        new CheckTask().execute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_devlist, container, false);
        return v;
    }

//    @Override
//    public void onSaveInstanceState(Bundle outState) {
//        super.onSaveInstanceState(outState);
//        outState.putInt("currDevice", currentPos);
//        outState.putSerializable("deviceList", checkouts);
//    }

    /* (non-Javadoc)
     * @see android.app.ListActivity#onListItemClick(android.widget.ListView, android.view.View, int, long)
     */
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        showDetails (position);
    }
    
    private void showDetails (int pos)
    {
        Log.d(TAG, "Data size is " + checkouts.size());
        if (pos >= checkouts.size()) return;
        currentPos = pos;
        Map<String,String> selected = checkouts.get(pos);
        String uid = selected.get("user_id");
        String devid = selected.get("dev_id");
        if (isDualPane)
        {
            getListView().setItemChecked(pos, true);
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
            Intent i = new Intent(getActivity(), PhoneCheckinActivity.class);
            i.putExtra("index", pos);
            i.putExtra("user_id", uid);
            i.putExtra("dev_id", devid);
            startActivity(i);
        }
//        super.onListItemClick(l, v, position, id);
    }
    
    private class CheckTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                ParseQuery checkOutQuery = new ParseQuery("DevOut");
                checkouts.clear();
                for (ParseObject obj : checkOutQuery.find())
                {
                    Map<String,String> dev_u = new HashMap<String, String>();
                    dev_u.put("dev_id", obj.getString("dev_id"));
                    dev_u.put("user_id", obj.getString("user_id"));
                    checkouts.add(dev_u);
                }
                Collections.sort(checkouts, new Comparator<Map<String,String>>() {

                    @Override
                    public int compare(Map<String, String> one,
                            Map<String, String> two) {
                        return one.get("user_id").compareTo(two.get("user_id"));
                    }
                });
            } catch (ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return null;
        }

        /* (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Void result) {
            adapter.notifyDataSetChanged();
        }
        
    }

}
