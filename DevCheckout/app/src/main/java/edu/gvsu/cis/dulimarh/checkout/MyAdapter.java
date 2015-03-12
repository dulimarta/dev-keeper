package edu.gvsu.cis.dulimarh.checkout;

import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by dulimarh on 3/11/15.
 */
public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
    private ArrayList<ParseProxyObject> datasource;

    public MyAdapter (ArrayList<ParseProxyObject> data) {
        datasource = data;
    }
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R
                .layout.checkout_item, viewGroup, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int i) {
        ParseProxyObject p = datasource.get(i);
        viewHolder.deviceName.setText (p.getString("dev_id"));
        viewHolder.checkOutDate.setText (DateFormat.format("yyyy-MM-dd",
                p.getCreatedAt()));
    }

    @Override
    public int getItemCount() {
        return datasource.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView deviceName, checkOutDate;
        public ViewHolder(View cellView) {
            super(cellView);
            checkOutDate = (TextView) cellView.findViewById(R.id.checkout_date);
            deviceName = (TextView) cellView.findViewById(R.id.devicename);
        }
    }
}
