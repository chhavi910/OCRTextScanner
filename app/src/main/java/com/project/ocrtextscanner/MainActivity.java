package com.project.ocrtextscanner;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.theartofdev.edmodo.cropper.CropImage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static android.Manifest.permission.CAMERA;

public class MainActivity extends AppCompatActivity {
    FrameLayout frameLayout;
    Button capture, flash, rotateCamera, gallery;

    Camera camera;
    ShowCamera showCamera;
    private Camera.PictureCallback mPicture = getPictureCallback();

    static final int REQUEST_STORAGE = 1;
    public  Bitmap bitmap;
    public Uri imageUri;
    public  static Uri resultUri;
    public static Bitmap cropBitmap;

    private boolean cameraFront = false;
    private boolean flashmode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        frameLayout = findViewById(R.id.frameLayout);
        capture = findViewById(R.id.buttonCapture);
        flash = findViewById(R.id.buttonFlash);
        rotateCamera = findViewById(R.id.buttonRotate);
        gallery=findViewById(R.id.buttonGallery);

        if (checkPermission() && checkStoragePermission()) {
            camera = Camera.open();
            Camera.Parameters parameters = camera.getParameters();
            List<Camera.Size> sizes = parameters.getSupportedPictureSizes();
            int w = 0, h = 0;
            for (Camera.Size size : sizes) {
                if (size.width > w || size.height > h) {
                    w = size.width;
                    h = size.height;
                }

            }
            parameters.setPictureSize(w, h);
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            camera.setParameters(parameters);
            showCamera = new ShowCamera(this, camera);
            frameLayout.addView(showCamera);
        } else {
            // This is Case 1 again as Permission is not granted by user

            //Now further we check if used denied permanently or not
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.CAMERA)){

                // case 4 User has denied permission but not permanently
               requestPermission();
            }
            else if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)){

                requestExternalStoragePermission();
            }
            else {
                // case 5. Permission denied permanently.
                // You can open Permission setting's page from here now.
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                // Set the message show for the Alert time
                builder.setMessage(R.string.alert_dialog);

                // Set Alert Title
                builder.setTitle(R.string.alert_title);

                // Set Cancelable false
                // for when the user clicks on the outside
                // the Dialog Box then it will remain show
                builder.setCancelable(false);

                // Set the positive button with yes name
                // OnClickListener method is use of
                // DialogInterface interface.

                builder.setPositiveButton(R.string.alert_settings, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // If user click no
                        // then dialog box is canceled.
                        dialog.cancel();
                        Intent i=new Intent(android.provider.Settings.ACTION_SETTINGS);
                        startActivity(i);
                    }
                });
                builder.setNegativeButton(R.string.alert_deny, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which){
                        // When the user click yes button
                        // then app will close
                        finish(); }
                });

                // Create the Alert dialog
                AlertDialog alertDialog = builder.create();

                // Show the Alert Dialog box
                alertDialog.show();

            }
        }

        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkPermission() && checkStoragePermission())
                    captureImage();
            }
        });
        flash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flashOnButton();
            }
        });
        rotateCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                releaseCamera();
                chooseCamera();
            }
        });
        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkStoragePermission()) selectImage();
                else requestExternalStoragePermission();
            }
        });
    }
    public void selectImage(){
        Intent i=new Intent();
        i.setType("image/*");
        i.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(i,5);
    }

    private boolean checkPermission() {
        int cameraPermission = ContextCompat.checkSelfPermission(getApplicationContext(), CAMERA);
        return cameraPermission == PackageManager.PERMISSION_GRANTED;
    }

    private boolean checkStoragePermission() {
        int storagePermission= ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
         return storagePermission == PackageManager.PERMISSION_GRANTED;
    }
    private void requestExternalStoragePermission() {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_STORAGE);
    }

    private void requestPermission() {
        int PERMISSION_CODE = 200;
        ActivityCompat.requestPermissions(this, new String[]{CAMERA}, PERMISSION_CODE);
    }

    private Camera.PictureCallback getPictureCallback() {
        Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                // convert byte array into bitmap
                bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                File pictureFile = getOutputMediaFile();
                pictureFile.getPath();
                imageUri=Uri.fromFile(pictureFile);
                if (pictureFile == null) {
                    return;
                }
                try {
                    FileOutputStream fos = new FileOutputStream(pictureFile);
                    fos.write(data);
                    fos.close();
                } catch (FileNotFoundException e) {

                } catch (IOException e) {
                }
                flashmode=false;
                flash.setBackgroundResource(R.drawable.ic_baseline_flash_off_24);
                CropImage.activity(imageUri).start(MainActivity.this);

            }
        };
        return mPictureCallback;
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                resultUri = result.getUri();
                try {
                    cropBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),resultUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Intent intent = new Intent(MainActivity.this,PictureActivity.class);
                startActivity(intent);

            }
        }
        if(data!=null && requestCode==5 && resultCode==RESULT_OK){
            imageUri = data.getData();
            CropImage.activity(imageUri).start(MainActivity.this);
        }
    }
    private void captureImage() {
        Camera.Parameters parameters = camera.getParameters();
        List<Camera.Size> sizes = parameters.getSupportedPictureSizes();
        int w = 0, h = 0;
        for (Camera.Size size : sizes) {
            if (size.width > w || size.height > h) {
                w = size.width;
                h = size.height;
            }

        }
        parameters.setPictureSize(w, h);
        camera.setParameters(parameters);
        if (camera != null) {
            camera.takePicture(null, null, mPicture);
        }

    }

    private static File getOutputMediaFile() {
        File mediaStorageDir = new File(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                "OCRTextScanner");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.e("OCRTextScanner", "failed to create directory");
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new Date());
        File mediaFile;
        mediaFile = new File(mediaStorageDir.getPath() + File.separator
                + "IMG_" + timeStamp + ".jpg");

        return mediaFile;
    }
    @Override
    protected void onPause() {
        super.onPause();
        //when on Pause, release camera in order to be used from other applications
        releaseCamera();
    }

    private void releaseCamera() {
        // stop and release camera
        if (camera != null) {
            camera.stopPreview();
            camera.setPreviewCallback(null);
            camera.release();
            camera = null;
        }
    }

    public void onResume() {

        super.onResume();

        if (camera == null && checkPermission() && checkStoragePermission()) {
            camera = Camera.open();
            camera.setDisplayOrientation(90);
            Camera.Parameters parameters = camera.getParameters();
            List<Camera.Size> sizes = parameters.getSupportedPictureSizes();
            int w = 0, h = 0;
            for (Camera.Size size : sizes) {
                if (size.width > w || size.height > h) {
                    w = size.width;
                    h = size.height;
                }

            }
            parameters.setPictureSize(w, h);
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            camera.setParameters(parameters);
            flash.setBackgroundResource(R.drawable.ic_baseline_flash_off_24);
            flashmode=false;
            mPicture = getPictureCallback();
            if(showCamera!=null)showCamera.refreshCamera(camera);
            else  {
                showCamera = new ShowCamera(this, camera);
                frameLayout.addView(showCamera);
            }

        }
        if(!checkPermission() || !checkStoragePermission()){
            // This is Case 1 again as Permission is not granted by user

            //Now further we check if used denied permanently or not
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                // case 4 User has denied permission but not permanently
                requestPermission();
            }
            else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                requestExternalStoragePermission();
            } else {
                // case 5. Permission denied permanently.
                // You can open Permission setting's page from here now.
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                // Set the message show for the Alert time
                builder.setMessage(R.string.alert_dialog);

                // Set Alert Title
                builder.setTitle(R.string.alert_title);

                // Set Cancelable false
                // for when the user clicks on the outside
                // the Dialog Box then it will remain show
                builder.setCancelable(false);

                // Set the positive button with yes name
                // OnClickListener method is use of
                // DialogInterface interface.

                builder.setPositiveButton(R.string.alert_settings, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // If user click no
                        // then dialog box is canceled.
                        dialog.cancel();
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", getPackageName(), null));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                });
                builder.setNegativeButton(R.string.alert_deny, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // When the user click yes button
                        // then app will close
                        finish();
                    }
                });

                // Create the Alert dialog
                AlertDialog alertDialog = builder.create();

                // Show the Alert Dialog box
                alertDialog.show();

            }
        }
    }

    private void flashOnButton() {
        if (camera != null) {
            try {
                Camera.Parameters param = camera.getParameters();
                if (cameraFront) {
                    boolean hasFlash = this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
                    if (hasFlash) {
                        param.setFlashMode(!flashmode ? Camera.Parameters.FLASH_MODE_TORCH
                                : Camera.Parameters.FLASH_MODE_OFF);
                        camera.setParameters(param);
                        flashmode = !flashmode;
                        if (flashmode)
                            flash.setBackgroundResource(R.drawable.ic_baseline_flash_on_24);
                    } else return;
                }
                param.setFlashMode(!flashmode ? Camera.Parameters.FLASH_MODE_TORCH
                        : Camera.Parameters.FLASH_MODE_OFF);
                camera.setParameters(param);
                flashmode = !flashmode;
                if (flashmode) flash.setBackgroundResource(R.drawable.ic_baseline_flash_on_24);
                else flash.setBackgroundResource(R.drawable.ic_baseline_flash_off_24);
            } catch (Exception e) {
                Toast.makeText(this, R.string.flash, Toast.LENGTH_LONG).show();
            }

        }
    }

    public void chooseCamera() {
        //if the camera preview is the front
        if (cameraFront) {
            int cameraId = findBackFacingCamera();
            if (cameraId >= 0) {
                //open the backFacingCamera
                //set a picture callback
                //refresh the preview

                camera = Camera.open(cameraId);
                camera.setDisplayOrientation(90);
                Camera.Parameters parameters = camera.getParameters();
                List<Camera.Size> sizes = parameters.getSupportedPictureSizes();
                int w = 0, h = 0;
                for (Camera.Size size : sizes) {
                    if (size.width > w || size.height > h) {
                        w = size.width;
                        h = size.height;
                    }

                }
                parameters.setPictureSize(w, h);
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                camera.setParameters(parameters);
                mPicture = getPictureCallback();
                showCamera.refreshCamera(camera);
            }
        } else {
            int cameraId = findFrontFacingCamera();
            if (cameraId >= 0) {
                //open the backFacingCamera
                //set a picture callback
                //refresh the preview
                flash.setBackgroundResource(R.drawable.ic_baseline_flash_off_24);
                camera = Camera.open(cameraId);
                camera.setDisplayOrientation(90);
                Camera.Parameters parameters = camera.getParameters();
                List<Camera.Size> sizes = parameters.getSupportedPictureSizes();
                int w = 0, h = 0;
                for (Camera.Size size : sizes) {
                    if (size.width > w || size.height > h) {
                        w = size.width;
                        h = size.height;
                    }

                }
                parameters.setPictureSize(w, h);
                camera.setParameters(parameters);
                mPicture = getPictureCallback();
                showCamera.refreshCamera(camera);
                camera.getParameters().setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                flashmode = false;
            }
        }
    }

    private int findBackFacingCamera() {
        int cameraId = -1;
        //Search for the back facing camera
        //get the number of cameras
        int numberOfCameras = Camera.getNumberOfCameras();
        //for every camera check
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                cameraFront = false;
                break;

            }

        }
        return cameraId;
    }

    private int findFrontFacingCamera() {

        int cameraId = -1;
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraId = i;
                cameraFront = true;
                break;
            }
        }
        return cameraId;

    }
}

