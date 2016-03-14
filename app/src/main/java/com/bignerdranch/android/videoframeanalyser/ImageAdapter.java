package com.bignerdranch.android.videoframeanalyser;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

/**
 * Created by nigelhenshaw on 25/06/2015.
 */
public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {

    public interface RecyclerViewClickPositionInterface {
        public void setImage(long time);
    }

    private RecyclerViewClickPositionInterface mPositionInterface;
    private final int FPS = 4;
    private final int SPF = 1000/FPS;

    private Bitmap placeHolderBitmap;
    private MediaMetadataRetriever mRetriever;

    public static class AsyncDrawable extends BitmapDrawable {
        final WeakReference<BitmapWorkerTask> taskReference;

        public AsyncDrawable(Resources resources, Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask) {
            super(resources, bitmap);
            taskReference = new WeakReference(bitmapWorkerTask);
        }

        public BitmapWorkerTask getBitmapWorkerTask() {
            return taskReference.get();
        }
    }

    public ImageAdapter(MediaMetadataRetriever mediaMetadataRetriever, RecyclerViewClickPositionInterface i) {
        mRetriever = mediaMetadataRetriever;
        mPositionInterface = i;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.gallery_images_relative_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        // File imageFile = imagesFile.listFiles()[position];
        // Bitmap imageBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
        // holder.getImageView().setImageBitmap(imageBitmap);
        // BitmapWorkerTask workerTask = new BitmapWorkerTask(holder.getImageView());
        // workerTask.execute(imageFile);

        BitmapWorkerTask bitmapWorkerTask = new BitmapWorkerTask(holder.getImageView());
        AsyncDrawable asyncDrawable = new AsyncDrawable(holder.getImageView().getResources(),
                placeHolderBitmap,
                bitmapWorkerTask);
        holder.getImageView().setImageDrawable(asyncDrawable);

        holder.getImageView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPositionInterface.setImage(holder.getPosition()*1000*SPF);
                Log.d("onClick", "" + holder.getPosition());
            }
        });

        long time = (position*1000*SPF);
        Log.d("imagesaver", "" + position);
        //holder.getImageView().setImageBitmap(mRetriever.getFrameAtTime(time));
        bitmapWorkerTask.execute(mRetriever.getFrameAtTime(time));
        //holder.getImageView().setImageResource(R.drawable.brian_up_close);

    }

    @Override
    public int getItemCount() {
        //Log.d("imageSaver ", " " + Integer.parseInt(mRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)));
        return (Integer.parseInt(mRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))/(SPF)) ;

        // return imagesFile.listFiles().length;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private ImageView imageView;

        public ViewHolder(View view) {
            super(view);

            view.setOnClickListener(this);
            imageView = (ImageView) view.findViewById(R.id.imageGalleryView);
        }

        public ImageView getImageView() {
            return imageView;
        }

        @Override
        public void onClick(View v) {

        }
    }

    public static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
        Drawable drawable = imageView.getDrawable();
        if(drawable instanceof AsyncDrawable) {
            AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
            return asyncDrawable.getBitmapWorkerTask();
        }
        return null;
    }
}
