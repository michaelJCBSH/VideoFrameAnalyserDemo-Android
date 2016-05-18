package com.bignerdranch.android.videoframeanalyser;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by JCBSH on 31/03/2016.
 */
public class RecordVideoFragment extends AbstractCameraFragment {

    public static Fragment getInstance() {

        Fragment fragment = new RecordVideoFragment();
        return fragment;
    }

    private static final String TAG = RecordVideoFragment.class.getSimpleName();
    private static final String LIFE_TAG = "life_" + RecordVideoFragment.class.getSimpleName();



    private boolean mFileUsedFlag;
    private File mCurrentVideoFile;
    private ImageButton mButtonVideo;
    private boolean mRecordingFlag = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

    }

    private TextureView mScanPreViewTextureView;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_record_video, container, false);
        mScanPreViewTextureView = (TextureView) view.findViewById(R.id.texture);


        mButtonVideo = (ImageButton) view.findViewById(R.id.video);
        mButtonVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mRecordingFlag) {
                    startRecordingVideo();
                    mRecordingFlag = true;
                } else {
                    stopRecordingVideo();
                    mRecordingFlag = false;
                }

            }
        });
        return view;
    }



    @Override
    public void onResume() {
        super.onResume();
        mFileUsedFlag = true;


    }

    @Override
    public void onPause() {
        super.onPause();
        if (mFileUsedFlag == false) {
            Log.d(LIFE_TAG, "delete() mFileUsedFlag == false");
            mCurrentVideoFile.delete();
        }


    }


////////////////////////////////////////////////////////////////////////////////
//////////////////////////////MEDIA RELATED CODES///////////////////////////////
//////////////////////////////MEDIA RELATED CODES///////////////////////////////
//////////////////////////////MEDIA RELATED CODES///////////////////////////////
////////////////////////////////////////////////////////////////////////////////
    private static final SparseIntArray ORIENTATIONS_VIDEO = new SparseIntArray();
    static {
        ORIENTATIONS_VIDEO.append(Surface.ROTATION_0, 90);
        ORIENTATIONS_VIDEO.append(Surface.ROTATION_90, 0);
        ORIENTATIONS_VIDEO.append(Surface.ROTATION_180, 270);
        ORIENTATIONS_VIDEO.append(Surface.ROTATION_270, 180);
    }

    private static final int MAX_VIDEO_WIDTH = 1920;
    private static final int MAX_VIDEO_HEIGHT = 1080;
    private CameraCaptureSession mCameraRecordSession;
    private CaptureRequest.Builder mPreviewBuilder;
    private MediaRecorder mMediaRecorder;



    @Override
    protected Size CaptureSizeSetup(StreamConfigurationMap map) {
        Size largestVideoSize = Collections.max(Arrays.asList(map.getOutputSizes(MediaRecorder.class)),
                new CompareSizesByArea());
        Size optimalVideoSize = chooseOptimalSize(map.getOutputSizes(MediaRecorder.class), MAX_VIDEO_WIDTH, MAX_VIDEO_HEIGHT,
                MAX_VIDEO_WIDTH, MAX_VIDEO_HEIGHT, largestVideoSize);


        return optimalVideoSize;
    }

    private void setUpCaptureRequestBuilder(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
    }


////---------------------------------------------////
////---------------------------------------------////
////            CAMERA CREATION CODES            ////
////---------------------------------------------////
////---------------------------------------------////

    @Override
    protected void openCamera() {
        mMediaRecorder = new MediaRecorder();
        super.openCamera();

    }

    @Override
    protected void closeCamera() {
        if (null != mMediaRecorder) {
            mMediaRecorder.release();
            mMediaRecorder = null;
        }

        if(mCameraRecordSession != null) {
            mCameraRecordSession.close();
            mCameraRecordSession = null;
        }
        super.closeCamera();
    }

////---------------------------------------------////
////---------------------------------------------////
////             VIDEO RELATED CODES             ////
////---------------------------------------------////
////---------------------------------------------////


    @Override
    protected void createCameraSession() {
        if (null == mCameraDevice || !mScanPreViewTextureView.isAvailable() || null == mPreviewSize) {
            return;
        }
        try {
            setUpMediaRecorder();
            SurfaceTexture texture = mScanPreViewTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            List<Surface> surfaces = new ArrayList<Surface>();

            Surface previewSurface = new Surface(texture);
            surfaces.add(previewSurface);
            mPreviewBuilder.addTarget(previewSurface);

            Surface recorderSurface = mMediaRecorder.getSurface();
            surfaces.add(recorderSurface);
            mPreviewBuilder.addTarget(recorderSurface);

            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    mCameraRecordSession = cameraCaptureSession;
                    if (null == mCameraDevice) {
                        return;
                    }
                    try {
                        setUpCaptureRequestBuilder(mPreviewBuilder);
                        mCameraRecordSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                    Activity activity = getActivity();
                    if (null != activity) {
                        Toast.makeText(activity, "Failed", Toast.LENGTH_SHORT).show();
                    }
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startRecordingVideo() {
        try {
            // UI

            mButtonVideo.setImageResource(android.R.drawable.ic_media_pause);

            // Start recording
            mMediaRecorder.start();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }


    private void stopRecordingVideo() {
        // UI
        mButtonVideo.setImageResource(android.R.drawable.ic_media_play);

        try {
            mCameraRecordSession.stopRepeating();
            mCameraRecordSession.abortCaptures();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }


        mMediaRecorder.stop();
        mMediaRecorder.reset();
        mFileUsedFlag = true;

        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (null != mMediaRecorder) {
            mMediaRecorder.release();
            mMediaRecorder = null;
        }

        openCamera();

        Activity activity = getActivity();
        if (null != activity) {
            Toast.makeText(activity, "Video saved: " + mCurrentVideoFile,
                    Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Video saved: " + mCurrentVideoFile);
        }

    }


    private void setUpMediaRecorder() throws IOException {
        final Activity activity = getActivity();
        if (null == activity) {
            return;
        }
        //mMediaRecorder.setPreviewDisplay();
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mCurrentVideoFile = getVideoFile(activity);
        mMediaRecorder.setOutputFile(mCurrentVideoFile.getAbsolutePath());
        mMediaRecorder.setVideoEncodingBitRate(10000000);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoSize(mOptimalCaputureSize.getWidth(), mOptimalCaputureSize.getHeight());
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

        int deviceOrientation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int videoOrientation = ORIENTATIONS_VIDEO.get(deviceOrientation);

        mMediaRecorder.setOrientationHint(videoOrientation);
        mMediaRecorder.prepare();
    }

    private File getVideoFile(Context context) {
        Activity activity = getActivity();
        if (activity == null) return null;
        mFileUsedFlag = false;
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String prepend = "VIDEO_" + timestamp + ".mp4";
        return new File(getActivity().getExternalFilesDir(null), prepend);
    }

    /* In this sample, we choose a video size with 3x4 aspect ratio. Also, we don't use sizes
        * larger than 1080p, since MediaRecorder cannot handle such a high-resolution video.
        *
        * @param choices The list of available sizes
    * @return The video size
    */
    private static Size chooseVideoSize(Size[] choices) {
        for (Size size : choices) {
            if (size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080) {

                return size;
            }
        }
        Log.e(TAG, "Couldn't find any suitable video size");
        return choices[choices.length - 1];
    }

////////////////////////////////////////////////////////////////////////////////
////////////////////////SIMPLE ABSTRACT IMPLEMENTATION//////////////////////////
////////////////////////SIMPLE ABSTRACT IMPLEMENTATION//////////////////////////
////////////////////////SIMPLE ABSTRACT IMPLEMENTATION//////////////////////////
////////////////////////////////////////////////////////////////////////////////
    @Override
    protected String getFragmentTag() {
        return TAG;
    }

    @Override
    protected String getFragmentLifeTag() {
        return LIFE_TAG;
    }

    @Override
    protected TextureView getPreViewTextureView() {
        return mScanPreViewTextureView;
    }

    @Override
    protected  Size[] getAllCaptureTypeSizes(StreamConfigurationMap map) {
        return map.getOutputSizes(MediaRecorder.class);
    }

    @Override
    protected List<Surface> getCameraPreviewSurfaces() {
        return null;
    }

    @Override
    protected void captureStillImage() {

    }

}
