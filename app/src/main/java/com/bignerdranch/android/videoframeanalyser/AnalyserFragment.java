package com.bignerdranch.android.videoframeanalyser;

import android.app.Fragment;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

/**
 * Created by JCBSH on 14/03/2016.
 */
public class AnalyserFragment extends Fragment{
    public static final String EXTRA_VIDEO_FILE_PATH = "com.bignerdranch.android.videoframeanalyser.AnalyserFragment_video_file_path";
    private static final String TAG = AnalyserFragment.class.getSimpleName();
    private String mPath;
    private RecyclerView mFrameRecyclerView;
    private ImageView mCurrentFrame;
    private MediaMetadataRetriever mRetriever;

    public static Fragment getInstance(String path) {
        Bundle bundle = new Bundle();
        bundle.putString(AnalyserFragment.EXTRA_VIDEO_FILE_PATH, path);
        Fragment fragment = new AnalyserFragment();
        fragment.setArguments(bundle);

        return fragment;

    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPath = getArguments().getString(EXTRA_VIDEO_FILE_PATH);
        Log.d(TAG, mPath);
        mRetriever = new MediaMetadataRetriever();
        mRetriever.setDataSource(mPath);
        Log.d(TAG, mRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));


    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_analyser,container, false);
        mCurrentFrame = (ImageView) v.findViewById(R.id.currentFrame);
        mCurrentFrame.setImageBitmap(mRetriever.getFrameAtTime());

        mFrameRecyclerView = (RecyclerView) v.findViewById(R.id.galleryRecyclerView);
        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), 1);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mFrameRecyclerView.setLayoutManager(layoutManager);
        RecyclerView.Adapter imageAdapter = new ImageAdapter(mRetriever);
        mFrameRecyclerView.setAdapter(imageAdapter);


        return v;
    }



    @Override
    public void onPause() {
        super.onPause();
        if (mRetriever != null) {
            mRetriever.release();
            mRetriever = null;
        }
    }
}
