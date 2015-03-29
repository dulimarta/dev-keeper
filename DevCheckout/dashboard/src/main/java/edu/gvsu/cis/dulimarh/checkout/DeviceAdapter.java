package edu.gvsu.cis.dulimarh.checkout;

import android.graphics.drawable.Drawable;
import android.media.Image;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/**
 * Created by dulimarh on 3/11/15.
 */
public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter
        .DeviceViewHolder>  {

    public interface DeviceSelectedListener {
        void onDeviceSelected(int pos, boolean available);
    }

    private ArrayList<ParseProxyObject> datasource;
    private Map<String,String> checkedOutIds;
    private DeviceSelectedListener mylistener;
    private int selectedIndex;

    public DeviceAdapter(ArrayList<ParseProxyObject> data,
                         Map<String,String> chkOutSet,
                         DeviceSelectedListener listener) {
        datasource = data;
        checkedOutIds = chkOutSet;
        mylistener = listener;
        selectedIndex = -1;
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
        if (i > datasource.size()) return;
        ParseProxyObject dev = datasource.get(i);
        String devId = dev.getString("device_id");
        viewHolder.deviceName.setText (dev.getString("name"));
        viewHolder.deviceId.setText (devId);
        if (checkedOutIds.containsKey(dev.getObjectId())) {
            viewHolder.statusIcon.setVisibility(View.VISIBLE);
            viewHolder.devStatus.setText("Checked Out by " +
                    checkedOutIds.get(dev.getObjectId()));
        }
        else {
            viewHolder.statusIcon.setVisibility(View.GONE);
            viewHolder.devStatus.setText("Available");
        }
    }

    @Override
    public int getItemCount() {
        return datasource.size();
    }

    public class DeviceViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        public TextView deviceName, deviceId, devStatus;
        public ImageView devIcon, statusIcon;

        public DeviceViewHolder(View cellView) {
            super(cellView);
            deviceId = (TextView) cellView.findViewById(R.id.dev_id);
            deviceName = (TextView) cellView.findViewById(R.id.devicename);
            devStatus = (TextView) cellView.findViewById(R.id.dev_status_text);
            devIcon = (ImageView) cellView.findViewById(R.id.dev_icon);
            statusIcon = (ImageView) cellView.findViewById(R.id
                    .dev_status_icon);

            cellView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            selectedIndex = getPosition();
            boolean onLoan = checkedOutIds.containsKey(datasource.get
                    (selectedIndex).getObjectId());
            mylistener.onDeviceSelected(selectedIndex, !onLoan);
        }
    }
}
