package com.bignerdranch.android.videoframeanalyser;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.Comparator;
import java.util.concurrent.BlockingQueue;

/**
 * Created by JCBSH on 15/04/2016.
 */
public class Frame {
    private Bitmap mBitmap;
    private long mTimeStamp;
    private String mName;
    public Frame(Bitmap bitmap, long timeStamp) {
        mBitmap = bitmap;
        mTimeStamp = timeStamp;
    }

    public Frame(long timeStamp, String name) {
        mTimeStamp = timeStamp;
        mName = name;
    }

    public Frame(Bitmap bmp) {
        this(bmp, System.currentTimeMillis());
    }

    public long getTimeStamp() {
        return mTimeStamp;
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public String getName() {
        return mName;
    }


    public static class VideoFrameConsumer implements Runnable {
        private Handler mUiHandler;
        private BlockingQueue<Frame> mQueue;
        private boolean mPlay;
        private boolean mNextFrameModeFlag = false;
        private boolean mDisplayNextFlag = false;

        public VideoFrameConsumer(BlockingQueue<Frame> queue, Handler uiHandler) {
            mQueue = queue;;
            mUiHandler = uiHandler;
        }

        @Override
        public void run() {
            while (!Thread.interrupted()) {
                try {
                    //Log.d("FrameConsumer", "queue size "+ mQueue.size());
                    if (mNextFrameModeFlag) {
                        if (mDisplayNextFlag) {
                            Frame frame = mQueue.take();
                            if (frame.getBitmap() == null) {
                                Message bitmapMessage = mUiHandler.obtainMessage(AnalyserFragment.WHAT_VIDEO_FINISHED);
                                bitmapMessage.sendToTarget();
                                break;
                            } else {
                                Message bitmapMessage = mUiHandler.obtainMessage(AnalyserFragment.WHAT_SET_IMAGE_BITMAP
                                        , frame.getBitmap());
                                bitmapMessage.sendToTarget();
                            }
                            mDisplayNextFlag = false;
                        }

                    } else {
                        if (mPlay) {
                            Frame frame = mQueue.take();
                            //33 FPS
                            Thread.sleep(30);
                            if (frame.getBitmap() == null) {
                                Message bitmapMessage = mUiHandler.obtainMessage(AnalyserFragment.WHAT_VIDEO_FINISHED);
                                bitmapMessage.sendToTarget();
                                break;
                            } else {
                                Message bitmapMessage = mUiHandler.obtainMessage(AnalyserFragment.WHAT_SET_IMAGE_BITMAP
                                        , frame.getBitmap());
                                bitmapMessage.sendToTarget();
                            }
                        }
                    }


                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Log.d("Frame", "VideoFrameConsumer runnable InterruptedException");
                    break;
                }
            }

            Log.d("Frame", "VideoFrameConsumer runnable finished");
        }

        public void setPlay(boolean play) {
            mPlay = play;
        }

        public void nextFrameModeEnable(boolean flag) {
            mNextFrameModeFlag = flag;
        }

        public void displayNextFrame(boolean flag) {
            mDisplayNextFlag = flag;
        }

    }

    public static class FrameComparator implements Comparator<Frame> {

        @Override
        public int compare(Frame lhs, Frame rhs) {
            if (lhs.getTimeStamp() < rhs.getTimeStamp()) {
                return -1;
            } else if (lhs.getTimeStamp() > rhs.getTimeStamp()) {
                return 1;
            } else {
                return 0;
            }
        }
    }
}
