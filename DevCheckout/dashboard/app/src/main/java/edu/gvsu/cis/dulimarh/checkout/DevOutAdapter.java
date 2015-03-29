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
public class DevOutAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>  {

    private final int TYPE_MESSAGE = 1;
    private final int TYPE_ENTRY = 2;

    public interface DeviceSelectedListener {
        void onDeviceSelected (int pos);
    }

    private ArrayList<ParseProxyObject> datasource;
    private DeviceSelectedListener mylistener;
    public DevOutAdapter(ArrayList<ParseProxyObject> data,
                         DeviceSelectedListener listener) {
        datasource = data;
        mylistener = listener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup,
                                               int type) {
        if (type == TYPE_ENTRY) {
            View v = LayoutInflater.from(viewGroup.getContext()).inflate(R
                    .layout.checkout_item, viewGroup, false);
            DeviceViewHolder vh = new DeviceViewHolder(v);
            return vh;
        }
        else {
            TextView tv = new TextView(viewGroup.getContext());
            tv.setText("No checked out devices.");
            return new MessageViewHolder (tv);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        if (viewHolder instanceof DeviceViewHolder){
            ParseProxyObject p = datasource.get(i);
            String user = p.getParseObject("user_obj");
            DeviceViewHolder dvh = (DeviceViewHolder) viewHolder;
            if (user != null) {
                Drawable photo = ImageStore.get(user);

                dvh.photo.setImageDrawable(photo);
            }
            dvh.deviceName.setText(p.getString("dev_id"));
            dvh.checkOutDate.setText("Checkout on: " + DateFormat
                    .format("yyyy-MM-dd", p.getCreatedAt()));
        }
    }

    @Override
    public int getItemCount() {
//        Log.d("HANS", "getItemCount() " + datasource.size());
        if (datasource.isEmpty())
            return 1;
        else
            return datasource.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (datasource.isEmpty())
            return TYPE_MESSAGE;
        else
            return TYPE_ENTRY;
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

    public class MessageViewHolder extends RecyclerView.ViewHolder {
        public TextView msg;

        public MessageViewHolder(View itemView) {
            super(itemView);
            msg = (TextView) itemView;
        }
    }
}
