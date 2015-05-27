package com.awstest;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;

public class ImageListAdapter extends ArrayAdapter {

    private Activity activity;
    private List<Image> entries;
    AWSController awsCtrl = new AWSController();

    public ImageListAdapter(Activity a, int resource, List<Image> objects) {
        super(a, resource);
        entries = objects;
        activity = a;
    }

    public static class ViewHolder {
        protected TextView header;
        protected ImageView image;
        protected TextView hashtags;
        protected Button like;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View v = convertView;
        final ViewHolder viewHolder;

        if (v == null) {
            LayoutInflater vi = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.image_list_item, null);

            viewHolder = new ViewHolder();
            viewHolder.header = (TextView) v.findViewById(R.id.image_list_item_header);
            viewHolder.image = (ImageView) v.findViewById(R.id.image_list_item_image);
            viewHolder.hashtags = (TextView) v.findViewById(R.id.image_list_item_hashtags);
            viewHolder.like = (Button) v.findViewById(R.id.like_image_button);

            v.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) v.getTag();
        }

        String queryResult = awsCtrl.executeSQLQuerySync("SELECT COUNT(UserId) FROM LIKE_IMAGE WHERE UserId = " +
                ConstantValues.userId + " AND ImageId = '" + entries.get(position).getId() + "'");

        if (queryResult.contains("1")) {
            viewHolder.like.setClickable(false);
            viewHolder.like.setText("Liked");
        } else {
            viewHolder.like.setClickable(true);
            viewHolder.like.setText("Like");

            viewHolder.like.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    awsCtrl.likeImage(entries.get(position).getId(), ConstantValues.userId);
                    viewHolder.like.setClickable(false);
                    viewHolder.like.setText("Liked");
                }
            });
        }

        viewHolder.header.setText(entries.get(position).getUsername());
        viewHolder.image.setImageBitmap(entries.get(position).getBitmap());
        viewHolder.hashtags.setText(entries.get(position).getHashtags());

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
    public Image getItem(int i) {
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
