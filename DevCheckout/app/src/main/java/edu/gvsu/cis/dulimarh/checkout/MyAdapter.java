package edu.gvsu.cis.dulimarh.checkout;

import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.parse.GetCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import bolts.Task;

/**
 * Created by dulimarh on 3/11/15.
 */
public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
    private ArrayList<ParseProxyObject> datasource;
    private Map<String,Drawable> photoMap;
    public MyAdapter (ArrayList<ParseProxyObject> data, Map<String,
            Drawable> photos) {
        datasource = data;
        photoMap = photos;
    }

    public MyAdapter() {
        super();
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
        ParseObject user = p.getParseObject("user_obj");
        if (user != null) {
            Drawable photo = photoMap.get(user.getObjectId());

            viewHolder.photo.setImageDrawable(photo);
        }
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
        public ImageView photo;
        public ViewHolder(View cellView) {
            super(cellView);
            checkOutDate = (TextView) cellView.findViewById(R.id.checkout_date);
            deviceName = (TextView) cellView.findViewById(R.id.devicename);
            photo = (ImageView) cellView.findViewById(R.id.userphoto);
        }
    }
}
