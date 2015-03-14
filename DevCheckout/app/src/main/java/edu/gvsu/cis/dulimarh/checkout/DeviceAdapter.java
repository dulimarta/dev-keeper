package edu.gvsu.cis.dulimarh.checkout;

import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by dulimarh on 3/11/15.
 */
public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter
        .DeviceViewHolder>  {

    public interface DeviceSelectedListener {
        void onDeviceSelected(int pos);
    }

    private ArrayList<ParseProxyObject> datasource;
    private DeviceSelectedListener mylistener;
    public DeviceAdapter(ArrayList<ParseProxyObject> data,
                         DeviceSelectedListener listener) {
        datasource = data;
        mylistener = listener;
    }

    @Override
    public DeviceViewHolder onCreateViewHolder(ViewGroup viewGroup,
                                               int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R
                .layout.dev_list_item, viewGroup, false);
        DeviceViewHolder vh = new DeviceViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(DeviceViewHolder viewHolder, int i) {
        ParseProxyObject dev = datasource.get(i);
        viewHolder.deviceName.setText (dev.getString("name"));
        viewHolder.deviceId.setText (dev.getString("device_id"));
    }

    @Override
    public int getItemCount() {
//        Log.d("HANS", "getItemCount() " + datasource.size());
        return datasource.size();
    }

    public class DeviceViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        public TextView deviceName, deviceId;
        public ImageView devIcon;

        public DeviceViewHolder(View cellView) {
            super(cellView);
            deviceId = (TextView) cellView.findViewById(R.id.dev_id);
            deviceName = (TextView) cellView.findViewById(R.id.devicename);
            devIcon = (ImageView) cellView.findViewById(R.id.dev_icon);
            cellView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            mylistener.onDeviceSelected(getPosition());
        }
    }
}
