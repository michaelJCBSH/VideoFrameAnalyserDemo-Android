package com.bignerdranch.android.videoframeanalyser;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by JCBSH on 1/04/2016.
 */
abstract class AbstractScanFragment extends Fragment {


    abstract protected String getFragmentTag();
    abstract protected String getFragmentLifeTag();
    abstract protected TextureView getPreViewTextureView();
    abstract protected Size CaptureSizeSetup(StreamConfigurationMap map);
    abstract protected Size[] getAllCaptureTypeSizes(StreamConfigurationMap map);
    abstract protected List<Surface> getCameraPreviewSurfaces();
    abstract protected void captureStillImage();
    abstract protected void createCameraSession();



    private static final int REQUEST_CAMERA_PERMISSION_RESULT = 0;
    private static final int REQUEST_ACCESS_COARSE_LOCATION_PERMISSION_RESULT = 1;
    private static final int REQUEST_ACCESS_FINE_LOCATION_PERMISSION_RESULT = 2;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT = 3;

    private static final int REQUEST_ENABLE_BT = 1;


    protected HandlerThread mImageSaverThread;
    protected Handler mImageSaverHandler;
    protected HandlerThread mBackgroundThread;
    protected Handler mBackgroundHandler;
    protected HandlerThread mFrameQueueThread;
    protected Handler mFrameQueueHandler;
    protected HandlerThread mFrameDeQueueThread;
    protected Handler mFrameDeQueueHandler;
    protected Handler mHandler;

    protected boolean mMenuAvailable = false;


////---------------------------------------------////
////---------------------------------------------////
////                LIFE CYCLE CODE              ////
////---------------------------------------------////
////---------------------------------------------////

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(getFragmentLifeTag(), "onAttach() ");
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.d(getFragmentLifeTag(), "onActivityCreated() ");
        super.onActivityCreated(savedInstanceState);
    }




    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(getFragmentLifeTag(), "onCreate() ");
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onStart() {
        Log.d(getFragmentLifeTag(), "onStart() ");
        super.onStart();
        //showPhoto();
    }

    @Override
    public void onResume() {
        Log.d(getFragmentLifeTag(), "onResume() ");
        super.onResume();

        checkWRITE_EXTERNAL_STORAGE();
        openBackgroundThread();

        invalidateOptionsMenu(false);

        if(getPreViewTextureView().isAvailable()) {
            setupCamera(getPreViewTextureView().getWidth(), getPreViewTextureView().getHeight());
            transformPreview(getPreViewTextureView().getWidth(), getPreViewTextureView().getHeight());
            openCamera();
        } else {
            getPreViewTextureView().setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }




    protected void openBackgroundThread() {
        mBackgroundThread =  new HandlerThread("background thread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());

        mImageSaverThread =  new HandlerThread("imageSaver background thread");
        mImageSaverThread.start();
        mImageSaverHandler = new Handler(mImageSaverThread.getLooper());

        mFrameQueueThread =  new HandlerThread("Frame Queue background thread");
        mFrameQueueThread.start();
        mFrameQueueHandler = new Handler(mFrameQueueThread.getLooper());


        mFrameDeQueueThread =  new HandlerThread("Frame DeQueue background thread");
        mFrameDeQueueThread.start();
        mFrameDeQueueHandler = new Handler(mFrameDeQueueThread.getLooper());

        mHandler = new Handler(Looper.getMainLooper());
    }

    protected void closeBackgroundThread() {

        mFrameDeQueueThread.interrupt();
        mFrameQueueHandler.removeCallbacksAndMessages(null);
        mFrameQueueThread.interrupt();

        mBackgroundThread.quitSafely();
        mImageSaverThread.quitSafely();
        mFrameQueueThread.quitSafely();
        mFrameDeQueueThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mImageSaverThread.join();
            mImageSaverThread = null;
            mFrameQueueThread.join();
            mFrameQueueThread = null;
            mFrameDeQueueThread.join();
            mFrameDeQueueThread = null;
            mImageSaverHandler = null;
            mBackgroundHandler = null;
            mFrameQueueHandler = null;
            mFrameDeQueueHandler = null;

            mHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onPause() {
        Log.d(getFragmentLifeTag(), "onPause() ");

        super.onPause();

        closeBackgroundThread();
        closeCamera();
    }

    @Override
    public void onDestroy() {
        Log.d(getFragmentLifeTag(), "onDestroy() ");
        super.onDestroy();
    }


    @Override
    public void onStop() {
        Log.d(getFragmentLifeTag(), "onStop() ");
        super.onStop();

    }

    @Override
    public void onDestroyView() {
        Log.d(getFragmentLifeTag(), "onDestroyView() ");
        super.onDestroyView();
    }


    @Override
    public void onDetach() {
        Log.d(getFragmentLifeTag(), "onDetach() ");
        super.onDetach();
    }

////---------------------------------------------////
////---------------------------------------------////
////                CHILD SHARED CODE            ////
////---------------------------------------------////
////---------------------------------------------////
    protected void invalidateOptionsMenu(boolean b) {
        mMenuAvailable = b;
        getActivity().invalidateOptionsMenu();
    }

////---------------------------------------------////
////---------------------------------------------////
////            UTILITY RELATED CODE             ////
////---------------------------------------------////
////---------------------------------------------////
    protected void showAlert(String title, String msg) {
        new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setMessage(msg)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        getActivity().finish();
                    }
                })
                .show();
    }

////---------------------------------------------////
////---------------------------------------------////
////            PERMISSION RELATED CODE          ////
////---------------------------------------------////
////---------------------------------------------////

    protected void checkWRITE_EXTERNAL_STORAGE() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
            } else {
                if(shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(getActivity(), "app needs to be able to save videos", Toast.LENGTH_SHORT).show();
                }
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT);
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CAMERA_PERMISSION_RESULT) {
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getActivity(),
                        "Application will not run without camera services", Toast.LENGTH_SHORT).show();
            }
        }

        if(requestCode == REQUEST_ACCESS_COARSE_LOCATION_PERMISSION_RESULT) {
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getActivity(),
                        "Application will not run without coarse location access", Toast.LENGTH_SHORT).show();
            }
        }

        if(requestCode == REQUEST_ACCESS_FINE_LOCATION_PERMISSION_RESULT) {
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getActivity(),
                        "Application will not run without fine location access", Toast.LENGTH_SHORT).show();
            }
        }

        if(requestCode == REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT) {
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getActivity(),
                        "Application will not run without EXTERNAL STORAGE access", Toast.LENGTH_SHORT).show();
            }
        }
    }


////////////////////////////////////////////////////////////////////////////////
//////////////////////////////MEDIA RELATED SECTION/////////////////////////////
//////////////////////////////MEDIA RELATED SECTION/////////////////////////////
//////////////////////////////MEDIA RELATED SECTION/////////////////////////////
////////////////////////////////////////////////////////////////////////////////



    protected static final int MAX_PREVIEW_WIDTH = 1920;
    protected static final int MAX_PREVIEW_HEIGHT = 1080;

    protected static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    protected static final SparseIntArray ORIENTATIONS_IMAGE = new SparseIntArray();
    static {
        ORIENTATIONS_IMAGE.append(Surface.ROTATION_0, 90);
        ORIENTATIONS_IMAGE.append(Surface.ROTATION_90, 0);
        ORIENTATIONS_IMAGE.append(Surface.ROTATION_180, 270);
        ORIENTATIONS_IMAGE.append(Surface.ROTATION_270, 180);
    }


    protected static final HashMap<Integer, Integer> ORIENTATIONS_SCREEN = new HashMap<Integer, Integer>();
    static {
        ORIENTATIONS_SCREEN.put(Surface.ROTATION_0, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        ORIENTATIONS_SCREEN.put(Surface.ROTATION_90, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        ORIENTATIONS_SCREEN.put(Surface.ROTATION_180, ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
        ORIENTATIONS_SCREEN.put(Surface.ROTATION_270, ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
    }

    private TextureView.SurfaceTextureListener mSurfaceTextureListener =
            new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                    setupCamera(width, height);
                    transformPreview(width, height);
                    openCamera();
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                    transformPreview(width, height);
                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                    return false;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surface) {

                }
            };
    private CameraDevice.StateCallback mCameraDeviceStateCallback
            = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            // Start the preview session if the TextureView has been set up already.
            if (mPreviewSize != null && getPreViewTextureView().isAvailable()) {
                createCameraSession();
            }
            //showCameraConnectionFail();
            // Toast.makeText(getApplicationContext(), "Camera Opened!", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            camera.close();
            mCameraDevice = null;
            showAlert(getActivity().getResources().getString(R.string.camera_error_alert_title)
                    , "camera connection onDisconnected");

        }

        @Override
        public void onError(CameraDevice camera, int error) {
            camera.close();
            mCameraDevice = null;
            showAlert(getActivity().getResources().getString(R.string.camera_error_alert_title)
                    , "camera connection onError " + error);
        }
    };

////---------------------------------------------////
////---------------------------------------------////
////            CAMERA CREATION CODES            ////
////---------------------------------------------////
////---------------------------------------------////
    protected CameraDevice mCameraDevice;
    protected Size mPreviewSize;
    protected Size mOptimalCaputureSize;

    private String mCameraId;
    private CameraCharacteristics mCameraCharacteristics;
    private void setupCamera(int width, int height) {

        final Activity activity = getActivity();
        if (null == activity || activity.isFinishing()) {
            return;
        }
        CameraManager cameraManager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
        try {
            for(String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                if(cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) ==
                        CameraCharacteristics.LENS_FACING_FRONT){
                    continue;
                }
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                Size optimalCaptureSize = CaptureSizeSetup(map);

                mCameraId = cameraId;
                mCameraCharacteristics = cameraCharacteristics;
                mOptimalCaputureSize = optimalCaptureSize;
                Log.d(getFragmentTag(), "mOptimalCaputureSize width: " + mOptimalCaputureSize.getWidth() + " height: " + mOptimalCaputureSize.getHeight());
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    protected void openCamera() {
        CameraManager cameraManager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
        try {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if(ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED) {
                    cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundHandler);
                } else {
                    if(shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                        Toast.makeText(getActivity(),
                                "Video app required access to camera", Toast.LENGTH_SHORT).show();
                    }
                    requestPermissions(new String[] {Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION_RESULT);
                }

            } else {
                cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    protected void closeCamera() {
        if(mCameraCaptureSession != null) {
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        }
        if(mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

////---------------------------------------------////
////---------------------------------------------////
////            TRANSFORM PREVIEW CODES          ////
////---------------------------------------------////
////---------------------------------------------////
    protected void transformPreview(int viewWidth, int viewHeight) {

        Activity activity = getActivity();
        if (null == getPreViewTextureView() || null == activity) {
            return;
        }

        StreamConfigurationMap map = mCameraCharacteristics.get(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        // For still image captures, we always use the largest available size.
        Size largestCaputureSize = Collections.max(Arrays.asList(getAllCaptureTypeSizes(map)),
                new CompareSizesByArea());

        Point displaySize = new Point();
        activity.getWindowManager().getDefaultDisplay().getSize(displaySize);


        // Find the rotation of the device relative to the native device orientation.
        int deviceRotation = activity.getWindowManager().getDefaultDisplay().getRotation();

        // Swap the view dimensions for calculation as needed if they are rotated relative to
        // the sensor.
        boolean swappedDimensions = deviceRotation == Surface.ROTATION_0 || deviceRotation == Surface.ROTATION_180;
        int rotatedViewWidth = viewWidth;
        int rotatedViewHeight = viewHeight;
        int maxPreviewWidth = displaySize.x;
        int maxPreviewHeight = displaySize.y;

        if (swappedDimensions) {
            rotatedViewWidth = viewHeight;
            rotatedViewHeight = viewWidth;
            maxPreviewWidth = displaySize.y;
            maxPreviewHeight = displaySize.x;
        }

        // Preview should not be larger than display size and 1080p.
        if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
            maxPreviewWidth = MAX_PREVIEW_WIDTH;
        }

        if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
            maxPreviewHeight = MAX_PREVIEW_HEIGHT;
        }

        Size previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                rotatedViewWidth, rotatedViewHeight, maxPreviewWidth, maxPreviewHeight,
                largestCaputureSize);


        Size cropPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                rotatedViewWidth, rotatedViewHeight, maxPreviewWidth, maxPreviewHeight,
                mOptimalCaputureSize);

        int rotation = (360 - ORIENTATIONS.get(deviceRotation)) % 360;

        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();

        int width;
        int height;
        int cropWidth;
        int cropHeight;
        RectF bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());
        bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
        matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);

        if (Surface.ROTATION_90 == deviceRotation || Surface.ROTATION_270 == deviceRotation) {
            width = viewWidth;
            height = viewHeight;

            cropWidth = cropPreviewSize.getWidth();
            cropHeight = cropPreviewSize.getHeight();
        } else {
            width = viewHeight;
            height = viewWidth;

            cropWidth = cropPreviewSize.getHeight();
            cropHeight = cropPreviewSize.getWidth();
        }

        if (!checkMatchingAspectRatio(width, height, cropPreviewSize)) {
            if (checkViewCanContainCropPreview(width, height, cropPreviewSize)) {
                getPreViewTextureView().setLayoutParams(new FrameLayout.LayoutParams(cropPreviewSize.getHeight(), cropPreviewSize.getWidth()));
            } else {
                float previewAspect = cropPreviewSize.getWidth() * 1.0f/cropPreviewSize.getHeight();
                if (isWidthResizeTarget(width, height, cropPreviewSize)) {
                    getPreViewTextureView().setLayoutParams(new FrameLayout.LayoutParams(width, (int) (width / previewAspect)));
                } else {
                    getPreViewTextureView().setLayoutParams(new FrameLayout.LayoutParams((int) (height * previewAspect), height));
                }
            }
        }


        float scale = Math.min(
                (float) viewHeight / cropHeight,
                (float) viewWidth / cropWidth);
        matrix.postScale(scale, scale, centerX, centerY);
        matrix.postRotate(rotation, centerX, centerY);
        getPreViewTextureView().setTransform(matrix);

        mPreviewSize = previewSize;



    }

    private boolean isWidthResizeTarget(int viewWidth, int viewHeight, Size cropPreviewSize) {
        float viewAspect = viewWidth * 1.0f/ viewHeight;
        float previewAspect = cropPreviewSize.getWidth() * 1.0f/cropPreviewSize.getHeight();
        if (viewAspect < previewAspect) {
            return true;
        } else {
            return false;
        }
    }

    private boolean checkViewCanContainCropPreview(int viewWidth, int viewHeight, Size cropPreviewSize) {
        if (viewWidth >= cropPreviewSize.getWidth() && viewHeight >= cropPreviewSize.getHeight()) {
            return true;
        }
        return false;
    }

    private boolean checkMatchingAspectRatio(int viewWidth, int viewHeight, Size cropPreviewSize) {
        float viewAspect = viewWidth * 1.0f/ viewHeight;
        float previewAspect = cropPreviewSize.getWidth() * 1.0f/cropPreviewSize.getHeight();
        float diff = viewAspect - previewAspect;
        if (diff < 0.02) {
            return true;
        }
        return false;
    }

    protected Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            //Log.d(getFragmentTag(), "chooseOptimalSize option width: " + option.getWidth() + " height: " + option.getHeight());
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                //Log.d(getFragmentTag(), "chooseOptimalSize option width: " + option.getWidth() + " height: " + option.getHeight());
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e(getFragmentTag(), "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    protected Size chooseLargestSizeOfAspectRatio(Size[] choices, Size aspectRatio) {

        List<Size> matchRatio = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            //Log.d(getFragmentTag(), "chooseOptimalSize option width: " + option.getWidth() + " height: " + option.getHeight());
            if (option.getHeight() == option.getWidth() * h / w) {
                //Log.d(getFragmentTag(), "chooseOptimalSize option width: " + option.getWidth() + " height: " + option.getHeight());
                matchRatio.add(option);
            }
        }
        if (matchRatio.size() > 0) {
            return Collections.max(matchRatio, new CompareSizesByArea());
        } else {
            Log.e(getFragmentTag(), "chooseLargestSizeOfAspectRatio Couldn't find any suitable size");
            return choices[0];
        }
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }


////---------------------------------------------////
////---------------------------------------------////
////             PREVIEW CREATION CODES          ////
////---------------------------------------------////
////---------------------------------------------////
//    private static final int STATE_PREVIEW = 0;
////    private static final int STATE__WAIT_LOCK = 1;
////    private static final int STATE__PICTURE_CAPTURED = 2;
//    private int mState = STATE_PREVIEW;

    protected CameraCaptureSession mCameraCaptureSession;

//    private CaptureRequest mPreviewCaptureRequest;
//    private CaptureRequest.Builder mPreviewCaptureRequestBuilder;
//    private CameraCaptureSession.CaptureCallback mSessionCaptureCallback = new CameraCaptureSession.CaptureCallback() {
//            private void process(CaptureResult result) {
//                //Log.d(TAG, "process " + mState);
//                switch(mState) {
//                    case STATE_PREVIEW:
//                        // Do nothing
//                        break;
//                    case STATE__WAIT_LOCK:
//
//                        Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
//                        //Log.d(TAG, "process STATE__WAIT_LOCK " + afState);
//                        //if(afState == CaptureRequest.CONTROL_AF_STATE_FOCUSED_LOCKED) {
//                        if(afState == CaptureRequest.CONTROL_AF_STATE_FOCUSED_LOCKED ||
//                                afState == CaptureRequest.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
//                        /*
//                        unLockFocus();
//                        Toast.makeText(getApplicationContext(), "Focus Lock Successful", Toast.LENGTH_SHORT).show();
//                        */
//                            mState = STATE__PICTURE_CAPTURED;
//                            captureStillImage();
//                        }
//                        break;
//                }
//            }
//        @Override
//        public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
//            super.onCaptureStarted(session, request, timestamp, frameNumber);
//        }
//
//        @Override
//        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
//            super.onCaptureCompleted(session, request, result);
//
//            process(result);
//        }
//
//        @Override
//        public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
//            super.onCaptureFailed(session, request, failure);
//
//            Toast.makeText(getActivity().getApplicationContext(), "Focus Lock Unsuccessful", Toast.LENGTH_SHORT).show();
//        }
//    };

//    protected void createCameraPreviewSession() {
//        try {
//            SurfaceTexture surfaceTexture = getPreViewTextureView().getSurfaceTexture();
//            surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
//            Surface previewSurface = new Surface(surfaceTexture);
//            mPreviewCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
//            mPreviewCaptureRequestBuilder.addTarget(previewSurface);
//            ArrayList<Surface> targetSurfaces = new ArrayList<Surface>();
//            targetSurfaces.add(previewSurface);
//            mCameraDevice.createCaptureSession(targetSurfaces,
//                    new CameraCaptureSession.StateCallback() {
//                        @Override
//                        public void onConfigured(CameraCaptureSession session) {
//                            if (mCameraDevice == null) {
//                                return;
//                            }
//                            try {
//                                mPreviewCaptureRequest = mPreviewCaptureRequestBuilder.build();
//                                mCameraCaptureSession = session;
//                                mCameraCaptureSession.setRepeatingRequest(
//                                        mPreviewCaptureRequest,
//                                        mSessionCaptureCallback,
//                                        mBackgroundHandler
//                                );
//                            } catch (CameraAccessException e) {
//                                e.printStackTrace();
//                            }
//                        }
//
//                        @Override
//                        public void onConfigureFailed(CameraCaptureSession session) {
//                            Toast.makeText(
//                                    getActivity().getApplicationContext(),
//                                    "create camera session failed!",
//                                    Toast.LENGTH_SHORT
//                            ).show();
//                        }
//                    }, null);
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }
//    }

//
//    protected void lockFocus() {
//        try {
//            mState = STATE__WAIT_LOCK;
//            mPreviewCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
//                    CaptureRequest.CONTROL_AF_TRIGGER_START);
//            mCameraCaptureSession.capture(mPreviewCaptureRequestBuilder.build(),
//                    mSessionCaptureCallback, mBackgroundHandler);
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }
//    }
//
//    protected void unLockFocus() {
//        try {
//            mState = STATE_PREVIEW;
//            mPreviewCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
//                    CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
//            mCameraCaptureSession.setRepeatingRequest(mPreviewCaptureRequestBuilder.build(),
//                    mSessionCaptureCallback, mBackgroundHandler);
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }
//    }

    protected File getImageFile() {
        Activity activity = getActivity();
        if (activity == null) return null;
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String prepend = "IMAGE_" + timestamp + ".jpg";
        return new File(getActivity().getExternalFilesDir(null), prepend);
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
//                case WHAT_GREY_SCALE_BITMAP:
//                    bitmap = (Bitmap) msg.obj;
//
//
//                    break;
//                case WHAT_SET_IMAGE_BITMAP:
//                    if (getGreyScaleView() == null) return;
//                    bitmap = (Bitmap) msg.obj;
//                    Log.d(getFragmentTag(), "WHAT_SET_IMAGE_BITMAP   setImageBitmap");
//                    getGreyScaleView().setImageBitmap(bitmap);
//                    break;
//                case WHAT_VIDEO_FINISHED:
//                    getGreyScaleView().setVisibility(View.INVISIBLE);
//                    break;
            }
        }
    }
}
