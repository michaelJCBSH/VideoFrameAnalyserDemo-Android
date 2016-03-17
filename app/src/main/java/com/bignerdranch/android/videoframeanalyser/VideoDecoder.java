package com.bignerdranch.android.videoframeanalyser;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by JCBSH on 15/03/2016.
 */
public class VideoDecoder {
    private static final String TAG = VideoDecoder.class.getSimpleName();
    private final MediaExtractor mMediaExtractor;
    private TextureView mTextureView;
    private String mPath;
    private MediaCodec mCodec;
    private MediaCodecInfo mCodecInfo;
    private MediaFormat mVideoFormat;
    private int mSampleSize;


    public VideoDecoder(String path) {
        mPath = path;
        mMediaExtractor = new MediaExtractor();
    }

    public VideoDecoder(String path, TextureView textureView) {
        this(path);
        mTextureView = textureView;
    }

    public boolean prepare() {
        int k = 0;
        try {
            mMediaExtractor.setDataSource(mPath);
            for (int i = 0; i < mMediaExtractor.getTrackCount(); ++i) {
                MediaFormat format = mMediaExtractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                Log.d(TAG, mime);
                if (mime.startsWith("video/")) {
                    mMediaExtractor.selectTrack(i);
                    MediaCodecInfo codecInfo =  getMediaCodecInfo();
                    if (codecInfo == null) {
                        Log.d(TAG, "no MediaCodecInfo");
                        return false;
                    }
                    mVideoFormat = format;
                    mCodecInfo = codecInfo;
                    mCodec = MediaCodec.createByCodecName(mCodecInfo.getName());
                    return true;

                }

            }


            //mMediaExtractor.readSampleData()
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    public void start() {


            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mVideoFormat.getInteger(MediaFormat.KEY_WIDTH),
                    mVideoFormat.getInteger(MediaFormat.KEY_HEIGHT));
            Surface surface = new Surface(texture);
            mCodec.configure(mVideoFormat, surface, null, 0);

            //mCodec.configure(mVideoFormat, null, null, 0);
            MediaFormat outputFormat = mCodec.getOutputFormat(); // option B
            mCodec.start();
            boolean sawInputEOS = false;
            boolean sawOutputEOS = false;
            int frameCount = 0;
            int timeStamp = -1;
            SpeedControlCallback callback = new SpeedControlCallback();
            while (!sawOutputEOS) {
                int inputBufferId = mCodec.dequeueInputBuffer(200000);
                if (inputBufferId >= 0) {
                        ByteBuffer inputBuffer = mCodec.getInputBuffer(inputBufferId);
                        // fill inputBuffer with valid data
                        int sampleSize = mMediaExtractor.readSampleData(inputBuffer, 0);
                        long presentationTimeUs = 0;
                        if (sampleSize < 0) {
                            sawInputEOS = true;
                            sampleSize = 0;
                        } else {
                            presentationTimeUs = mMediaExtractor.getSampleTime();
                        }

                        mSampleSize = sampleSize;

                        mCodec.queueInputBuffer(inputBufferId,
                                0, //offset
                                sampleSize,
                                presentationTimeUs,
                                sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);


                        if (!sawInputEOS) {
                            mMediaExtractor.advance();
                        }
                    }

                }

                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                int outputBufferId = mCodec.dequeueOutputBuffer(bufferInfo, 200000);
                if (outputBufferId >= 0) {
                    //ByteBuffer outputBuffer = mCodec.getOutputBuffer(outputBufferId);
                    //MediaFormat bufferFormat = mCodec.getOutputFormat(outputBufferId); // option A

                    // bufferFormat is identical to outputFormat
                    // outputBuffer is ready to be processed or rendered.
                    if ((bufferInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM)) {
                        mCodec.releaseOutputBuffer(outputBufferId, false);
                        sawOutputEOS = true;
                    } else {
                        mCodec.releaseOutputBuffer(outputBufferId, bufferInfo.presentationTimeUs);
                    }
                    mCodec.releaseOutputBuffer(outputBufferId, bufferInfo.presentationTimeUs);
//                    Log.d(TAG, "frameCount: " + frameCount);
//                    ++frameCount;

                } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // Subsequent data will conform to new format.
                    // Can ignore if using getOutputFormat(outputBufferId)
                    outputFormat = mCodec.getOutputFormat(); // option B
                }




    }

    public MediaCodecInfo getMediaCodecInfo() {

        MediaCodecList codecList = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
        Log.d(TAG, "codecList size: " + codecList.getCodecInfos().length);
        for (MediaCodecInfo codecInfo: codecList.getCodecInfos()) {
            if (codecInfo.isEncoder()) {
                continue;
            }

            String[] types = codecInfo.getSupportedTypes();
            for (String type: types) {

                if (type.equalsIgnoreCase("video/avc")) {
                    Log.d(TAG, "inner " + type);
                    MediaCodecInfo.CodecCapabilities caps = codecInfo.getCapabilitiesForType(type);
                    for (int i:caps.colorFormats) {
                        if (i == caps.COLOR_FormatYUV420Planar) {
                            return codecInfo;
                        }
                    }
                }
            }
        }
        return null;
    }

    public void release () {
        mMediaExtractor.release();
        mCodec.stop();
        mCodec.release();
        //mCodec.stop();
        //mCodec.release();
    }

    public void next() {

    }
}
