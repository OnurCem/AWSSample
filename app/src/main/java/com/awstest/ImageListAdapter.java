package com.awstest;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;

public class ImageListAdapter extends ArrayAdapter {

    private Activity activity;
    private List<Bitmap> entries;

    public ImageListAdapter(Activity a, int resource, List<Bitmap> objects) {
        super(a, resource);
        entries = objects;
        activity = a;
    }

    public static class ViewHolder {
        protected TextView header;
        protected ImageView image;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        final ViewHolder viewHolder;

        if (v == null) {
            LayoutInflater vi = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.image_list_item, null);

            viewHolder = new ViewHolder();
            viewHolder.header = (TextView) v.findViewById(R.id.image_list_item_header);
            viewHolder.image = (ImageView) v.findViewById(R.id.image_list_item_image);

            v.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) v.getTag();
        }

        viewHolder.header.setText("Picture Header");
        viewHolder.image.setImageBitmap(entries.get(position));

        return v;
    }

    /**
     * Get size of user list
     * @return userList size
     */
    @Override
    public int getCount() {
        return  entries.size();
    }

    /**
     * Get specific item from user list
     * @param i item index
     * @return list item
     */
    @Override
    public Bitmap getItem(int i) {
        return entries.get(i);
    }

    /**
     * Get user list item id
     * @param i item index
     * @return current item id
     */
    @Override
    public long getItemId(int i) {
        return i;
    }
}
