package com.bignerdranch.android.videoframeanalyser;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by JCBSH on 14/03/2016.
 */
public class AnalyserFragment extends Fragment {
    public static final String EXTRA_VIDEO_FILE_PATH = "com.bignerdranch.android.videoframeanalyser.AnalyserFragment_video_file_path";
    private static final String TAG = AnalyserFragment.class.getSimpleName();
    private Frame.VideoFrameConsumer mVideoConsumer;


    public static Fragment getInstance(String path) {
        Bundle bundle = new Bundle();
        bundle.putString(AnalyserFragment.EXTRA_VIDEO_FILE_PATH, path);
        Fragment fragment = new AnalyserFragment();
        fragment.setArguments(bundle);

        return fragment;

    }


    private String mPath;
    private ImageView mFrameView;
    private boolean mPlayingFlag = false;
    private boolean mVideoFinishedFlag = true;
    private ImageButton mPlayVideoButton;
    private ImageButton mNextFrameButton;
    private BlockingQueue<Frame> mQueue;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mQueue = new ArrayBlockingQueue<Frame>(10);

        mPath = getArguments().getString(EXTRA_VIDEO_FILE_PATH);




    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_analyser,container, false);
        mFrameView = (ImageView) v.findViewById(R.id.currentFrame);
        //mFrameView.setVisibility(View.INVISIBLE);

        mPlayVideoButton = (ImageButton) v.findViewById(R.id.play_video);
        mPlayVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mPlayingFlag) {
                    mVideoConsumer.setPlay(true);
                    if (mVideoFinishedFlag) {
                        decodeVideo();
                        mVideoFinishedFlag = false;
                    }

                    mPlayingFlag = true;
                } else {
                    mVideoConsumer.setPlay(false);
                    mPlayingFlag = false;
                }

            }
        });

        mNextFrameButton = (ImageButton) v.findViewById(R.id.next_frame);
        mNextFrameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mVideoFinishedFlag) {
                    decodeVideo();
                    mVideoFinishedFlag = false;
                }

                mVideoConsumer.nextFrameModeEnable(true);
                mVideoConsumer.displayNextFrame(true);
            }
        });



        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        openBackgroundThread();
        //decodeVideo();
    }

    @Override
    public void onPause() {
        super.onPause();
        closeBackgroundThread();
    }


    private HandlerThread mVideoDecoderThread;
    private Handler mVideoDecoderHandler;
    private HandlerThread mFrameQueueThread;
    private Handler mFrameQueueHandler;
    private HandlerThread mFrameDeQueueThread;
    private Handler mFrameDeQueueHandler;

    private Thread th;
    private final UiHandler mUiHandler = new UiHandler(Looper.getMainLooper());

    private void openBackgroundThread() {

        mFrameQueueThread =  new HandlerThread("Frame Queue background thread");
        mFrameQueueThread.start();
        mFrameQueueHandler = new Handler(mFrameQueueThread.getLooper());

        mFrameDeQueueThread =  new HandlerThread("Frame DeQueue background thread");
        mFrameDeQueueThread.start();
        mFrameDeQueueHandler = new Handler(mFrameDeQueueThread.getLooper());

        mVideoDecoderThread = new HandlerThread("VideoDecoder background thread");
        mVideoDecoderThread.start();
        mVideoDecoderHandler = new Handler(mVideoDecoderThread.getLooper());




        mVideoConsumer = new Frame.VideoFrameConsumer(mQueue, mUiHandler);

    }

    private void closeBackgroundThread() {

        mFrameDeQueueThread.interrupt();
        mFrameQueueHandler.removeCallbacksAndMessages(null);
        mFrameQueueThread.interrupt();

        if (th != null) th.interrupt();
        mVideoDecoderThread.quitSafely();
        mFrameQueueThread.quitSafely();
        mFrameDeQueueThread.quitSafely();
        try {
            mFrameQueueThread.join();
            mFrameQueueThread = null;
            mFrameDeQueueThread.join();
            mFrameDeQueueThread = null;
            mVideoDecoderThread.join();
            mVideoDecoderThread = null;
            mVideoDecoderHandler = null;
            mFrameQueueHandler = null;
            mFrameDeQueueHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void decodeVideo() {
        //mFrameView.setVisibility(View.VISIBLE);
        mFrameDeQueueHandler.post(mVideoConsumer);
        mVideoDecoderHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "decodeVideo mCurrentVideoFile.getPath() " + mPath);
                ExtractMpegFrames frameExtractor = new ExtractMpegFrames(mPath, getActivity(), mQueue, mUiHandler);
                try {
                    th = new Thread(new ExtractMpegFrames.ExtractMpegFramesRunnable(frameExtractor), "video frame decoder thread");;
                    th.start();
                    th.join();
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        });

    }

////////////////////////////////////////////////////////////////////////////////
////////////////////////////////UI HANDLER SECTION//////////////////////////////
////////////////////////////////UI HANDLER SECTION//////////////////////////////
////////////////////////////////UI HANDLER SECTION//////////////////////////////
////////////////////////////////////////////////////////////////////////////////

    public static final int WHAT_GREY_SCALE_BITMAP = 0;
    public static final int WHAT_SET_IMAGE_BITMAP = 1;
    public static final int WHAT_VIDEO_FINISHED = 2;
    protected class UiHandler extends Handler {
        public UiHandler(Looper mainLooper) {
            super(mainLooper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            Bitmap bitmap = null;
            switch (msg.what) {
                case WHAT_GREY_SCALE_BITMAP:
                    bitmap = (Bitmap) msg.obj;
                    final Frame frame = new Frame(bitmap);
                    mFrameQueueHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            while (true){
                                Log.d(TAG, "waiting to offer " +mQueue.remainingCapacity());
                                try {
                                    boolean b = mQueue.offer(frame, 100, TimeUnit.MILLISECONDS);

                                    if (b) {
                                        break;
                                    }
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                    break;
                                }

                            }
                        }
                    });
                    break;
                case WHAT_SET_IMAGE_BITMAP:
                    if (mFrameView == null) return;
                    bitmap = (Bitmap) msg.obj;
                    Log.d(TAG, "WHAT_SET_IMAGE_BITMAP   setImageBitmap");
                    mFrameView.setImageBitmap(bitmap);
                    break;
                case WHAT_VIDEO_FINISHED:
                    //mFrameView.setVisibility(View.INVISIBLE);
                    mVideoFinishedFlag = true;
                    break;
            }
        }
    }


}
