package edu.gvsu.cis.dulimarh.checkout;

import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.parse.ParseObject;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by dulimarh on 3/11/15.
 */
public class DevOutAdapter extends RecyclerView.Adapter<DevOutAdapter
        .DeviceViewHolder>  {

    public interface DeviceSelectedListener {
        void onDeviceSelected (int pos);
    }

    private ArrayList<ParseProxyObject> datasource;
    private DeviceSelectedListener mylistener;
//    private Map<String,Drawable> photoMap;
    public DevOutAdapter(ArrayList<ParseProxyObject> data,
                         DeviceSelectedListener listener) {
        datasource = data;
        mylistener = listener;
//        photoMap = photos;
    }

    @Override
    public DeviceViewHolder onCreateViewHolder(ViewGroup viewGroup,
                                               int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R
                .layout.checkout_item, viewGroup, false);
        DeviceViewHolder vh = new DeviceViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(DeviceViewHolder viewHolder, int i) {
        ParseProxyObject p = datasource.get(i);
        String user = p.getParseObject("user_obj");
        if (user != null) {
            Drawable photo = ImageStore.get(user);

            viewHolder.photo.setImageDrawable(photo);
        }
        viewHolder.deviceName.setText (p.getString("dev_id"));
        viewHolder.checkOutDate.setText ("Checkout on: " + DateFormat
                .format("yyyy-MM-dd", p.getCreatedAt()));
    }

    @Override
    public int getItemCount() {
        Log.d("HANS", "getItemCount() " + datasource.size());
        return datasource.size();
    }

    public class DeviceViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        public TextView deviceName, checkOutDate;
        public ImageView photo;

        public DeviceViewHolder(View cellView) {
            super(cellView);
            checkOutDate = (TextView) cellView.findViewById(R.id.checkout_date);
            deviceName = (TextView) cellView.findViewById(R.id.devicename);
            photo = (ImageView) cellView.findViewById(R.id.user_photo);
            cellView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            mylistener.onDeviceSelected(getPosition());
        }
    }
}
