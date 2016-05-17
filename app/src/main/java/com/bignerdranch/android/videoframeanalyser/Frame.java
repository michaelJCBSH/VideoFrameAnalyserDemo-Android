package com.bignerdranch.android.videoframeanalyser;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;

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

        public VideoFrameConsumer(BlockingQueue<Frame> queue, Handler uiHandler) {
            mQueue = queue;;
            mUiHandler = uiHandler;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    //Log.d("FrameConsumer", "queue size "+ mQueue.size());
                    Frame frame = mQueue.take();
                    if (frame.getBitmap() == null) {
                        Message bitmapMessage = mUiHandler.obtainMessage(AbstractScanFragment.WHAT_VIDEO_FINISHED);
                        bitmapMessage.sendToTarget();
                    } else {
                        Message bitmapMessage = mUiHandler.obtainMessage(AbstractScanFragment.WHAT_SET_IMAGE_BITMAP
                                , frame.getBitmap());
                        bitmapMessage.sendToTarget();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }


    public static class ImageFrameConsumer implements Runnable {
        private Handler mUiHandler;
        private BlockingQueue<Frame> mQueue;

        public ImageFrameConsumer(BlockingQueue<Frame> queue, Handler uiHandler) {
            mQueue = queue;;
            mUiHandler = uiHandler;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    //Log.d("FrameConsumer", "queue size "+ mQueue.size());
                    Frame frame = mQueue.take();
                    Message bitmapMessage = mUiHandler.obtainMessage(AbstractScanFragment.WHAT_SET_IMAGE_BITMAP
                            , frame.getBitmap());
                    bitmapMessage.sendToTarget();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }

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