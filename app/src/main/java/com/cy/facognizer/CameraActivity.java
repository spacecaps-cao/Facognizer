package com.cy.facognizer;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;

public class CameraActivity extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2 {

    // The camera view.
    private CameraBridgeViewBase cameraView;

    // A matrix that is used when saving photos.
    private Mat photoMatrix;

    // The index of the active camera.
    private int cameraIndex = 0;

    // The index of the active image size.
    private int imageSizeIndex;

    // The number of cameras
    private int camerasNumber;

    // Judgement of the camera
    // Whether the active camera is front-facing
    private boolean isCameraFrontFacing;

    // Next frame. take the photo
    private boolean isPhotoPending = false;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // new a matrix instance
        photoMatrix = new Mat();

        // Get a window instance
        final Window window = getWindow();

        // To keep the screen turn on and bright
        window.addFlags(WindowManager.
                LayoutParams.FLAG_KEEP_SCREEN_ON);
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Get the number of
        if (savedInstanceState != null){
            cameraIndex = savedInstanceState.getInt(
                    "cameraIndex", 0);
            imageSizeIndex = savedInstanceState.getInt(
                    "imageSizeIndex", 0);
        } else {
            cameraIndex = 0;
            imageSizeIndex = 0;
        }

        // Call the camera of device
        final Camera camera;
//
        // Limit the version of OS
        if(Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.KITKAT) {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(cameraIndex, cameraInfo);

            isCameraFrontFacing =
                    (cameraInfo.facing ==
                            cameraInfo.CAMERA_FACING_FRONT);
            camerasNumber = Camera.getNumberOfCameras();

            camera = Camera.open(cameraIndex);
        }else {
            isCameraFrontFacing = false;
            camerasNumber = 1;
            camera = Camera.open();
        }

        // Set a parameter to camera
        //final Camera.Parameters parameters = camera.getParameters();

        // Release the camera.
        camera.release();

//        supportedImageSizes =
//                parameters.getSupportedPreviewSizes();

        // This size is tend to limit the max frame size of camera
//        if do not need it, annotate it.
//        final Size size = supportedImageSizes.get(imageSizeIndex);

        cameraView = new JavaCameraView(CameraActivity.this, cameraIndex);

        // Enable the camera view
        cameraView.enableView();

        cameraView.setVisibility(View.VISIBLE);

        // Set the parameter of size.
        // cameraView.setMaxFrameSize(size.width, size.height);

        cameraView.setCvCameraViewListener(CameraActivity.this);
        setContentView(cameraView);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_camera, menu);
        return true;
    }

    @SuppressLint("NewApi")
    @Override
    public boolean onOptionsItemSelected(final MenuItem item){

        switch(item.getItemId()){
            case R.id.menu_take_photo:
                // Do sth.
                isPhotoPending = true;
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        final Mat rgba = inputFrame.rgba();

        if (isPhotoPending) {
            isPhotoPending = false;
            takePhoto(rgba);
        }
        return rgba;
    }

    // The "Take Photo" method
    public void takePhoto(final Mat rgba){

        // Determine the path and metadata for the photo.
        final long currentTimeMills = System.currentTimeMillis();
        final String appName = getString(R.string.app_name);
        final String galleryPath =
                Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES).toString();
        final String albumPath = galleryPath + File.separator +
                appName;
        final String photoPath = albumPath + File.separator +
                currentTimeMills + ProcessActivity.PHOTO_FILE_EXTENSION;

        // Use the content provider to deliver the photo to ProcessActivity.
        final ContentValues values = new ContentValues();

        // Put the values to the ContentProvider
        values.put(MediaStore.MediaColumns.DATA, photoPath);
        values.put(MediaStore.Images.Media.MIME_TYPE,
                ProcessActivity.PHOTO_MIME_TYPE);
        values.put(MediaStore.Images.Media.TITLE, appName);
        values.put(MediaStore.Images.Media.DESCRIPTION, appName);
        values.put(MediaStore.Images.Media.DATE_TAKEN, currentTimeMills);

        // Ensure that the album directory exists
        File album = new File(albumPath);
        if(!album.isDirectory() && !album.mkdirs()){
            log("Failed to create album directory at " +
                    albumPath);
            onTakePhotoFailed();
            return;
        }

        // Try to create the photo
        Imgproc.cvtColor(rgba, photoMatrix, Imgproc.COLOR_RGBA2BGR, 3);
        if(!Imgcodecs.imwrite(photoPath, photoMatrix)){
            log("Failed to save photo to " + photoPath);
            onTakePhotoFailed();
        }
        toast("Photo saved successfully to " + photoPath);

        // Try to insert the photo into the MediaStore.
        Uri uri;
        try {
            uri = getContentResolver().insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        } catch (final Exception e) {
            log("Failed to insert photo into MediaStore");
            e.printStackTrace();

            // Since the insertion failed, delete the photo.
            File photo = new File(photoPath);
            if (!photo.delete()){
                log("Failed to delete non-inserted photo");
            }
            onTakePhotoFailed();
            return;
        }

        // Open the photo in ProcessActivity
        final Intent intent =
                new Intent(CameraActivity.this, ProcessActivity.class);
        intent.putExtra(ProcessActivity.EXTRA_PHOTO_URI, uri);
        intent.putExtra(ProcessActivity.EXTRA_PHOTO_DATA_PATH, photoPath);
        startActivity(intent);
    }

    private void onTakePhotoFailed(){
        log("enter the onTakePhotoFailed method");
        // Show a error message.
        final String errorMessage = "Failed to save photo";

        toast(errorMessage);
    }

    private void log(String msg){
        Log.d("OpenCVdebug", msg);
    }

    public void onSaveInstanceState(Bundle savedInstanceState){
        //  Save the current camera index.
        savedInstanceState.putInt("cameraIndex", cameraIndex);

        // Save the current image size index
        savedInstanceState.putInt("imageSizeIndex", imageSizeIndex);

        super.onSaveInstanceState(savedInstanceState);
    }

    // On recreate
    @SuppressLint("NewApi")
    @Override
    public void recreate(){
        if(Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.HONEYCOMB){
            super.recreate();
        } else {
            finish();
            startActivity(getIntent());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        cameraView.enableView();
    }

    @Override
    protected void onPause() {
        if(cameraView != null) {
            cameraView.disableView();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if(cameraView != null) {
            cameraView.disableView();
        }
        super.onDestroy();

    }

    private void toast(final String str){

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(CameraActivity.this, str,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
