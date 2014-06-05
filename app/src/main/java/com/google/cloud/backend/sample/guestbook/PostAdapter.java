package com.google.cloud.backend.sample.guestbook;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HttpStack;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.google.cloud.backend.R;
import com.google.cloud.backend.core.CloudEntity;

import java.io.File;
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

    private ImageLoader.ImageCache imageCache = new BitmapLruCache();
    private ImageLoader mImageLoader = new ImageLoader(newRequestQueue(getContext()), imageCache);

    // Default maximum disk usage in bytes
    private static final int DEFAULT_DISK_USAGE_BYTES = 25 * 1024 * 1024;

    // Default cache folder name
    private static final String DEFAULT_CACHE_DIR = "photos";

    private static RequestQueue newRequestQueue(Context context) {
        // define cache folder
        File rootCache = context.getExternalCacheDir();
        if (rootCache == null) {
            rootCache = context.getCacheDir();
        }

        File cacheDir = new File(rootCache, DEFAULT_CACHE_DIR);
        cacheDir.mkdirs();

        HttpStack stack = new HurlStack();
        Network network = new BasicNetwork(stack);
        DiskBasedCache diskBasedCache = new DiskBasedCache(cacheDir, DEFAULT_DISK_USAGE_BYTES);
        RequestQueue queue = new RequestQueue(diskBasedCache, network);
        queue.start();

        return queue;
    }

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
            String entityMessage = ce.get("message").toString();

            TextView message = (TextView) view.findViewById(R.id.messageContent);
            TextView signature = (TextView) view.findViewById(R.id.signature);
            System.out.printf("Position %d, entityMessage", position, entityMessage);
            if (message != null) {
                if (entityMessage.startsWith(GuestbookActivity.BLOB_PICTURE_MESSAGE_PREFIX +
                        GuestbookActivity.BLOB_PICTURE_DELIMITER)) {
                    String imageUrl = entityMessage.split(GuestbookActivity.BLOB_PICTURE_DELIMITER)[1];
                    NetworkImageView imageView = (NetworkImageView) view.findViewById(R.id.messagePicture);
                    imageView.setImageUrl(imageUrl, mImageLoader);
                    imageView.setDefaultImageResId(R.drawable.abc_spinner_ab_default_holo_light);
                    imageView.setVisibility(View.VISIBLE);
                    message.setVisibility(View.GONE);
                } else {
                    message.setText(ce.get("message").toString());
                    message.setVisibility(View.VISIBLE);
                    NetworkImageView imageView = (NetworkImageView) view.findViewById(R.id.messagePicture);
                    imageView.setVisibility(View.GONE);
                }
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
