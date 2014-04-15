package edu.gvsu.cis.dulimarh.checkout;

import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class ListBundleAdapter extends BaseAdapter {
    private LayoutInflater inflater;
    private List<Bundle> data;
    private int resource;
    private String[] from;
    private int[] to;
    public ListBundleAdapter(Context context,
            List<Bundle> data, int resource, String[] from,
            int[] to) {
        this.data = data;
        this.resource = resource;
        this.from = from;
        this.to = to;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View itemView;
        if (convertView == null) {
            itemView = inflater.inflate(resource, parent, false);
        }
        else
            itemView = convertView;
        //Bundle b = data.get(position);
        for (int k = 0; k < from.length; k++) {
            final View v = itemView.findViewById(to[k]);
            if (v == null) continue;
        }
        return null;
    }

}
