package edu.gvsu.cis.dulimarh.checkout;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

/**
 * Created by dulimarh on 3/13/15.
 */
public class UserAdapter extends RecyclerView.Adapter<UserAdapter
        .UserViewHolder> {

    public interface UserSelectedListener {
        void onUserSelected (int position);
    }

    private List<ParseProxyObject> datasource;
    private Map<String,Integer> checkoutCounter;
    private UserSelectedListener uListener;
    private int selectedIndex;

    public UserAdapter(List<ParseProxyObject> ds, Map<String,Integer> cc,
                       UserSelectedListener ulistener)
    {
        datasource = ds;
        checkoutCounter = cc;
        this.uListener = ulistener;
        selectedIndex = -1;
    }

    @Override
    public UserAdapter.UserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.user_list_item, parent, false);
        return new UserViewHolder(v);
    }

    @Override
    public void onBindViewHolder(UserAdapter.UserViewHolder holder, int position) {
        ParseProxyObject user = datasource.get(position);
        String name = user.getParseObject("user_name");
        holder.uname.setText(name);
        String uid = user.getString("user_id");
        holder.uid.setText(uid);
        Integer checked_count = checkoutCounter.get(user.getObjectId());
        if (checked_count != null)
            holder.count.setText(String.valueOf(checked_count));
        else
            holder.count.setText("");
        Drawable d = ImageStore.get(user.getObjectId());
        if (d != null) {
            holder.photo.setImageDrawable(d);
        }
        if (position == selectedIndex)
            holder.cell.setBackgroundResource(R.color.fab_color_muted);
        else
            holder.cell.setBackgroundColor(Color.WHITE);
    }

    @Override
    public int getItemCount() {
        return datasource.size();
    }

    public class UserViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener{
        public ImageView photo;
        public TextView uname, uid, count;
        public View cell;

        public UserViewHolder(View itemView) {
            super(itemView);
            cell = itemView;
            photo = (ImageView) itemView.findViewById(R.id.user_photo);
            uname = (TextView) itemView.findViewById(R.id.user_name);
            uid = (TextView) itemView.findViewById(R.id.user_id);
            count = (TextView) itemView.findViewById(R.id.checkout_count);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            selectedIndex = getPosition();
            uListener.onUserSelected(getPosition());
        }
    }
}
