package com.bignerdranch.android.videoframeanalyser;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.concurrent.locks.Lock;

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
    private ImageView mFrameView;
    private Handler mHandler;
    private Lock mLock;
    private MyLock mMyLock;

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

        //Log.d(TAG, mPath);
        mMyLock = new MyLock();


    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_media_codec,container, false);

        mTextureView = (TextureView) v.findViewById(R.id.texture);
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

                Bitmap bitmap = mTextureView.getBitmap();
                Log.d(TAG, "videoBlah: bitmap " + bitmap.getWidth() + "X" + bitmap.getHeight());
                mFrameView.setImageBitmap(bitmap);

            }
        });

        mFrameView = (ImageView) v.findViewById(R.id.frameView);



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

                        ExtractMpegFramesTest test = new ExtractMpegFramesTest(mPath, mHandler);
                        try {
                            test.testExtractMpegFrames();
                        } catch (Throwable throwable) {
                            throwable.printStackTrace();
                        }
                        //mVideoDecoder.start();
                    }
                });
                return true;
            case R.id.nextFrame:
                synchronized (mVideoDecoder) {
                    mVideoDecoder.notifyAll();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }

    }

    @Override
    public void onResume() {
        super.onResume();

        openBackgroundThread();

//        mVideoDecoder = new VideoDecoder(mPath, mTextureView, mMyLock);
        //mVideoDecoder.prepare();
        //mVideoDecoder.start();
    }

    @Override
    public void onPause() {
        super.onPause();
//        mVideoDecoder.release();

        closeBackgroundThread();
    }


    public static final int WHAT_GREYSCALE_BITMAP = 0;

    private void openBackgroundThread() {
        mBackgroundThread =  new HandlerThread("background thread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        mHandler = new Handler(Looper.getMainLooper()) {

            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                switch (msg.what) {
                    case WHAT_GREYSCALE_BITMAP:
                        if (mFrameView == null) return;
                        Bitmap bitmap = (Bitmap) msg.obj;
                        Log.d(TAG, bitmap.getWidth() + "X" + bitmap.getHeight());
//                    if (((BitmapDrawable)mJpegImageView.getDrawable()) != null) {
//                        Bitmap bitmapOld = ((BitmapDrawable)mJpegImageView.getDrawable()).getBitmap();
//                        if (bitmapOld != null && !bitmapOld.isRecycled())   {
//                            bitmapOld.recycle();
//                        }
//                    }

                        mFrameView.setImageBitmap(bitmap);
                        //detail3DScan();

                        //mReadyToTakePhotoFlag = true;
                        break;
                }


            }


        };
    }

    private void closeBackgroundThread() {

        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
            mHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
