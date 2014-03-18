package edu.gvsu.cis.dulimarh.checkout;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import edu.gvsu.cis.dulimarh.checkout.DeviceDetailsFragment.DeviceRemovalListener;


public class PhoneListActivity extends Activity implements DeviceRemovalListener {
   // private final String TAG = getClass().getName();
//    private static final int DIALOG_PROGRESS = 2;
    
   // private ListView theList;
    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devlist);
//        theList = getListView();
//        theList.setOnItemLongClickListener(selectionListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater mn_inflater = getMenuInflater();
        mn_inflater.inflate(R.menu.action_menu, menu);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        // TODO Auto-generated method stub
        switch (item.getItemId())
        {
        case R.id.menu_checkout:
            Intent userSelect = new Intent(this, SelectUserActivity.class);
            startActivity(userSelect);
            break;
        case R.id.menu_checkin:
            break;
        }
//        return  super.onMenuItemSelected(featureId, item);
        return true;
    }

    @Override
    public void deviceRemoved(String dev_id) {
    }
    
    
}
