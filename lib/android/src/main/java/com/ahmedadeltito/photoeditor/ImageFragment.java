package com.ahmedadeltito.photoeditor;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import ui.photoeditor.R;
/**
 * Created by Ahmed Adel on 5/4/17.
 */

public class ImageFragment extends Fragment implements ImageAdapter.OnImageClickListener {

    private ArrayList<StickerModel> stickersArray;
    private ArrayList<StickerModel> filterArray;
    private PhotoEditorActivity photoEditorActivity;
    RecyclerView imageRecyclerView;
    private String filterKeyword = "";
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        photoEditorActivity = (PhotoEditorActivity) getActivity();

        TypedArray images = getResources().obtainTypedArray(R.array.photo_editor_photos);


    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main_photo_edit_image, container, false);

        imageRecyclerView = (RecyclerView) rootView.findViewById(R.id.fragment_main_photo_edit_image_rv);
        imageRecyclerView.setLayoutManager(new GridLayoutManager(photoEditorActivity, 3));

        stickersArray = (ArrayList<StickerModel>) getActivity().getIntent().getExtras().getSerializable("stickers");
        filterArray = new ArrayList<>();
        new Thread()
        {
            public void run()
            {
                for (int i = 0;i < stickersArray.size();i++) {
                    new AsyncTaskLoadImage(i).execute(stickersArray.get(i).mUrl);
                }
            }
        }.run();
        return rootView;
    }

    public void filter(String keyword)
    {
        filterKeyword = keyword;
        setAdapter();
    }
    public void setAdapter()
    {
        filterArray.clear();
        for (int i = 0;i < stickersArray.size();i++)
        {
            if (filterKeyword.equals(""))
            {
                filterArray.add(stickersArray.get(i));
            }
            else
            {
                for (int k = 0; k < stickersArray.get(i).mTags.size();k++)
                {
                    if (stickersArray.get(i).mTags.get(k).contains(filterKeyword))
                    {
                        filterArray.add(stickersArray.get(i));
                        break;
                    }
                }

            }
        }
        ImageAdapter adapter = new ImageAdapter(photoEditorActivity, filterArray);
        adapter.setOnImageClickListener(this);
        imageRecyclerView.setAdapter(adapter);
    }
    public class AsyncTaskLoadImage  extends AsyncTask<String, String, Bitmap> {
        private final static String TAG = "AsyncTaskLoadImage";
        private int index;
        public AsyncTaskLoadImage(int index) {
            this.index = index;
        }
        @Override
        protected Bitmap doInBackground(String... params) {
            Bitmap bitmap = null;
            try {
                URL url = new URL(params[0]);
                bitmap = BitmapFactory.decodeStream((InputStream)url.getContent());
                return scaleDown(bitmap,400,false);

            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
            return bitmap;
        }
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            stickersArray.get(this.index).mBitmap = bitmap;
            setAdapter();
        }
    }


    public Bitmap decodeSampledBitmapFromUrl(String Url, int reqWidth, int reqHeight) {

        try {
            URL url = new URL(Url);
            Bitmap image = BitmapFactory.decodeStream(url.openConnection().getInputStream());
            return scaleDown(image,reqWidth,false);
        } catch(Exception e) {
        }
        return null;
    }
    public Bitmap scaleDown(Bitmap realImage, float maxImageSize,
                                   boolean filter) {
        float ratio = Math.min(
                (float) maxImageSize / realImage.getWidth(),
                (float) maxImageSize / realImage.getHeight());
        int width = Math.round((float) ratio * realImage.getWidth());
        int height = Math.round((float) ratio * realImage.getHeight());

        Bitmap newBitmap = Bitmap.createScaledBitmap(realImage, width,
                height, filter);
        return newBitmap;
    }
    public Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    public int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    @Override
    public void onImageClickListener(Bitmap image) {
        photoEditorActivity.addImage(image);
    }
}
