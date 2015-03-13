package edu.gvsu.cis.dulimarh.checkout;

import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by dulimarh on 3/13/15.
 */
public class UserAdapter extends RecyclerView.Adapter<UserAdapter
        .UserViewHolder> {
    private List<ParseProxyObject> datasource;

    public UserAdapter(List<ParseProxyObject> ds)
    {
        datasource = ds;
    }

    @Override
    public UserAdapter.UserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.user_list_item, parent, false);
        return new UserViewHolder(v);
    }

    @Override
    public void onBindViewHolder(UserAdapter.UserViewHolder holder, int position) {
        ParseProxyObject p = datasource.get(position);
        String name = p.getParseObject("user_name");
        holder.uname.setText(name);
        String uid = p.getString("user_id");
        holder.uid.setText(uid);
        Drawable d = ImageStore.get(p.getObjectId());
        if (d != null) {
            holder.photo.setImageDrawable(d);
        }
    }

    @Override
    public int getItemCount() {
        Log.d("HANS", "User count is " + datasource.size());
        return datasource.size();
    }

    public class UserViewHolder extends RecyclerView.ViewHolder {
        public ImageView photo;
        public TextView uname, uid;
        public UserViewHolder(View itemView) {
            super(itemView);
            photo = (ImageView) itemView.findViewById(R.id.user_photo);
            uname = (TextView) itemView.findViewById(R.id.user_name);
            uid = (TextView) itemView.findViewById(R.id.user_id);
        }
    }
}
