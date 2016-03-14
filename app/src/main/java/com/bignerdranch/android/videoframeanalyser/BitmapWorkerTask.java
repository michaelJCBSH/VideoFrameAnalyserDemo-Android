package com.bignerdranch.android.videoframeanalyser;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

/**
 * Created by nigelhenshaw on 6/07/2015.
 */

public class BitmapWorkerTask extends AsyncTask<Bitmap, Void, Bitmap> {

    WeakReference<ImageView> imageViewReferences;
    final static int TARGET_IMAGE_VIEW_WIDTH = 200;
    final static int TARGET_IMAGE_VIEW_HEIGHT = 200;
    public BitmapWorkerTask(ImageView imageView) {
        imageViewReferences = new WeakReference<ImageView>(imageView);
    }

    @Override
    protected Bitmap doInBackground(Bitmap... params) {
        // return BitmapFactory.decodeFile(params[0].getAbsolutePath());

        Bitmap bitmap = params[0];
        bitmap = decodeBitmapFromFile(bitmap);
        return bitmap;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        /*
        if(bitmap != null && imageViewReferences != null) {
            ImageView viewImage = imageViewReferences.get();
            if(viewImage != null) {
                viewImage.setImageBitmap(bitmap);
            }
        }
        */
        if(isCancelled()) {
            bitmap = null;
        }
        if(bitmap != null && imageViewReferences != null) {
            ImageView imageView = imageViewReferences.get();
            if(imageView != null) {
                BitmapWorkerTask bitmapWorkerTask = ImageAdapter.getBitmapWorkerTask(imageView);
                if(this == bitmapWorkerTask && imageView != null){
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }

    private int calculateInSampleSize(int inWidth, int inHeight) {
        final int photoWidth = inWidth;
        final int photoHeight = inHeight;
        int scaleFactor = 1;

        if(photoWidth > TARGET_IMAGE_VIEW_WIDTH || photoHeight > TARGET_IMAGE_VIEW_HEIGHT) {
            final int halfPhotoWidth = photoWidth/2;
            final int halfPhotoHeight = photoHeight/2;
            while(halfPhotoWidth/scaleFactor > TARGET_IMAGE_VIEW_WIDTH
                    || halfPhotoHeight/scaleFactor > TARGET_IMAGE_VIEW_HEIGHT) {
                scaleFactor *= 2;
            }
        }
        return scaleFactor;
    }

    private Bitmap decodeBitmapFromFile(Bitmap bitmap) {
        int factor = calculateInSampleSize (bitmap.getWidth(), bitmap.getHeight());
        return Bitmap.createScaledBitmap(bitmap, bitmap.getWidth()/factor, bitmap.getHeight()/factor, false);
    }
}
