package com.google.cloud.backend.sample.guestbook;

import com.google.cloud.backend.R;
import com.google.cloud.backend.core.CloudEntity;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * This ArrayAdapter uses CloudEntities as items and displays them as a post in
 * the guestbook. Layout uses row.xml.
 *
 */
public class PostAdapter extends ArrayAdapter<CloudEntity> {

    private static final SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss ", Locale.US);

    private LayoutInflater mInflater;

    /**
     * Creates a new instance of this adapter.
     *
     * @param context
     * @param textViewResourceId
     * @param objects
     */
    public PostAdapter(Context context, int textViewResourceId, List<CloudEntity> objects) {
        super(context, textViewResourceId, objects);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView != null ?
                convertView : mInflater.inflate(R.layout.row_post, parent, false);

        CloudEntity ce = getItem(position);
        if (ce != null) {
            TextView message = (TextView) view.findViewById(R.id.messageContent);
            TextView signature = (TextView) view.findViewById(R.id.signature);
            if (message != null) {
                message.setText(ce.get("message").toString());
            }
            if (signature != null) {
                signature.setText(getAuthor(ce) + " " + SDF.format(ce.getCreatedAt()));
            }
        }

        return view;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return this.getView(position, convertView, parent);
    }

    /**
     * Gets the author field of the CloudEntity.
     *
     * @param post the CloudEntity
     * @return author string
     */
    private String getAuthor(CloudEntity post) {
        if (post.getCreatedBy() != null) {
            return " " + post.getCreatedBy().replaceFirst("@.*", "");
        } else {
            return "<anonymous>";
        }
    }
}
