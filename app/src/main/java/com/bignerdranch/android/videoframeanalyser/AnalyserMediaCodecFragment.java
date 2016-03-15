package com.bignerdranch.android.videoframeanalyser;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by JCBSH on 15/03/2016.
 */
public class AnalyserMediaCodecFragment extends Fragment{

    private static final String TAG = AnalyserMediaCodecFragment.class.getSimpleName();
    private String mPath;
    private VideoDecoder mVideoDecoder;
    private TextureView mTextureView;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    public static Fragment getInstance(String path) {
        Bundle bundle = new Bundle();
        bundle.putString(AnalyserFragment.EXTRA_VIDEO_FILE_PATH, path);
        Fragment fragment = new AnalyserMediaCodecFragment();
        fragment.setArguments(bundle);

        return fragment;

    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mPath = getArguments().getString(AnalyserFragment.EXTRA_VIDEO_FILE_PATH);

        Log.d(TAG, mPath);


    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_media_codec,container, false);

        mTextureView = (TextureView) v.findViewById(R.id.texture);



        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.analyser_media_codec_fragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.playVideo:
                mBackgroundHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mVideoDecoder.setGetNextFrameFlag(true);
                        mVideoDecoder.start();
                    }
                });

                return true;
            case R.id.nextFrame:

                mVideoDecoder.setGetNextFrameFlag(true);
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }

    }

    @Override
    public void onResume() {
        super.onResume();

        openBackgroundThread();

        mVideoDecoder = new VideoDecoder(mPath, mTextureView);
        mVideoDecoder.prepare();
        //mVideoDecoder.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        mVideoDecoder.release();

        closeBackgroundThread();
    }


    private void openBackgroundThread() {
        mBackgroundThread =  new HandlerThread("background thread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void closeBackgroundThread() {

        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
