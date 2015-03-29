package edu.gvsu.cis.dulimarh.checkout;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;

public class DevOutListFragment extends Fragment implements
        DevOutAdapter.DeviceSelectedListener{
    private static final int DEVICE_CHECKIN_REQUEST = 0xBEEF;
    private final String TAG = "HANS";
    private ArrayList<ParseProxyObject> checkouts;
    private RecyclerView myrecyclerview;
    private RecyclerView.Adapter myadapter;
    private RecyclerView.LayoutManager mylayoutmgr;
    private boolean isDualPane;
    private int currentPos;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView ");

        View v = inflater.inflate(R.layout.fragment_devlist, container, false);
        myrecyclerview = (RecyclerView) v.findViewById(R.id.devlist);
        myrecyclerview.setHasFixedSize(true);
        mylayoutmgr = new LinearLayoutManager(getActivity());
        myrecyclerview.setLayoutManager(mylayoutmgr);
        if (savedInstanceState != null)
            checkouts = (ArrayList<ParseProxyObject>) savedInstanceState
                    .getSerializable("checkouts");
        else
            checkouts = new ArrayList<ParseProxyObject>();
        myadapter = new DevOutAdapter(checkouts, this);
        myrecyclerview.setAdapter(myadapter);
        return v;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState)  {
        Log.d(TAG, "Hosting activity created");
        super.onActivityCreated(savedInstanceState);
        View v = getActivity().findViewById(R.id.devdetails);
        isDualPane = v != null && v.getVisibility() == View.VISIBLE;
        Log.d(TAG, "Dual pane = " + isDualPane);
        currentPos = 0;
        //Log.d(TAG, "Initiating ASyncTask to fetch Parse data");
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
//        if (checkouts.isEmpty()) {
            updateDeviceList();
//        }
    }

    private Task<Void> findUserImageAsync (final ParseObject obj)
            throws ParseException {
        return Task.call(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                ParseObject usrObj = obj.getParseObject("user_obj");
                usrObj.fetchIfNeeded();
                ParseFile pf = usrObj.getParseFile("user_photo");
                if (ImageStore.get(pf.getUrl()) == null) {
                    Drawable d = Drawable.createFromStream(new
                            ByteArrayInputStream(pf.getData()), "");
                    ImageStore.put(usrObj.getObjectId(), d);
                }
                return null;
            }
        });
    }

    public void updateDeviceList()
    {
        new ParseQuery<ParseObject>(Consts.DEVICE_LOAN_TABLE)
                .findInBackground()
                .continueWithTask(new Continuation<List<ParseObject>,
                        Task<Void>>() {
                    /* This task (T1) takes List<ParseObject> as input and
                       produce a new task of type Task<Void>
                     */
                    @Override
                    public Task<Void> then(Task<List<ParseObject>> results)
                            throws Exception {

                        /* place the image fetching tasks in an
                        ArrayList */
                        ArrayList<Task<Void>> tasks = new
                                ArrayList<Task<Void>>();
                        checkouts.clear();
                        for (ParseObject p : results.getResult()) {
                            tasks.add(findUserImageAsync(p));
                            checkouts.add(new ParseProxyObject(p));
                        }
                        Collections.sort(checkouts,
                                new Comparator<ParseProxyObject>() {

                                    @Override
                                    public int compare(ParseProxyObject one,
                                                       ParseProxyObject two) {
                                        return one.getString("user_id").compareTo(two.getString("user_id"));
                                    }
                                });

                        /* The next task (T2) will run only when all the
                            background tasks created by T1 complete
                         */
                        return Task.whenAll(tasks);
                    }
                })
                .onSuccess(new Continuation<Void, Object>() {
                    /* This task (T2) takes no input from the previous
                    task
                     */
                    @Override
                    public Void then(Task<Void> task) throws Exception {
                        Log.d("HANS", "Notify dataset changed, " +
                                "dataset size " + checkouts.size());
                        myadapter.notifyDataSetChanged();
                        return null;
                    }
                }, Task.UI_THREAD_EXECUTOR) /* Run T2 on the UI thread,
                 so notifyDatasetChanged updates the UI correctly */
                .continueWith(new Continuation<Object, Object>() {
                    @Override
                    public Object then(Task<Object> task) throws Exception {
                        if (task.isFaulted()) {
                            Toast.makeText(getActivity(),
                                    "Unable to load checkout data",
                                    Toast.LENGTH_SHORT).show();
                        }
                        return null;
                    }
                }, Task.UI_THREAD_EXECUTOR);
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
            DevOutDetailsFragment ddf = (DevOutDetailsFragment)
                    getFragmentManager().findFragmentById(R.id.devdetails);
            if (ddf == null || ddf.getCurrentIndex() != pos)
            {
                ddf = DevOutDetailsFragment.newInstance(pos, selected.getObjectId());
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
//                updateDeviceList();
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
