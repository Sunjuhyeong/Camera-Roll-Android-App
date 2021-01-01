package us.koller.cameraroll.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;

import us.koller.cameraroll.R;

public class SearchAdapter extends BaseAdapter {

    // Ratio to scale a detected face rectangle, the face rectangle scaled up looks more natural.
    private Context mContext;
    private LayoutInflater inflater;
    private ViewHolder viewHolder = null;
    ArrayList<Bitmap> mItems;

    public SearchAdapter (Context context, ArrayList<Bitmap> items) {
        this.mContext = context;
        this.inflater = LayoutInflater.from(mContext);
        this.mItems = items;
    }


    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public Object getItem(int i) {
        return mItems.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            viewHolder = new ViewHolder();
            view = inflater.inflate(R.layout.item_face, null);
            viewHolder.imageView = view.findViewById(R.id.faceThumbnail);
            view.setTag(viewHolder);
        }else {
            viewHolder = (ViewHolder)view.getTag();
        }

        viewHolder.imageView.setTag(i);
        viewHolder.imageView.setImageBitmap(mItems.get(i));
//        viewHolder.imageView.setOnClickListener(buttonClickListener);
        return view;
    }

    private View.OnClickListener buttonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.imageView) {

            }
        }
    };

    static class ViewHolder {
        public ImageView imageView = null;
    }

    @Override
    protected void finalize() throws Throwable {
        free();
        super.finalize();
    }

    private void free(){
        inflater = null;
        mItems = null;
        viewHolder = null;
        mContext = null;
    }

}