package com.cy.facognizer;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.cy.facognizer.model.Database;
import com.cy.facognizer.model.Singleton;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgproc.Imgproc;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ProcessActivity extends AppCompatActivity {

    public static final String PHOTO_FILE_EXTENSION = ".png";
    public static final String PHOTO_MIME_TYPE = "image/png";

    public static final String EXTRA_PHOTO_URI =
            "com.cy.flognizer.view.ProcessActivity.extra.PHOTO_URI";
    public static final String EXTRA_PHOTO_DATA_PATH =
            "com.cy.flognizer.view.ProcessActivity.extra.PHOTO_DATA_PATH";

    private Uri uri;
    private String dataPath;
    public static final int IMPORT_PHOTO = 0;

    // Save the mat image that got from import pic.
    private Mat img;
    private Bitmap bitmap;

    // Save the mat image that processed.
    private Mat processedImg;

    // Save the image that got from camera activity.
    private ImageView imageView;

    private boolean isGray = false;

    // Singleton
    private Singleton singleton = Singleton.getSingleton(ProcessActivity.this);

    public static int count = 0;

    private Bitmap thisBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();

        // get the intent to deliver the picture.
        uri = intent.getParcelableExtra(EXTRA_PHOTO_URI);
        dataPath = intent.getStringExtra(EXTRA_PHOTO_DATA_PATH);
        imageView = new ImageView(this);
        imageView.setImageURI(uri);

        Bitmap bitmap = convertImgViewToBmp(imageView);
        this.bitmap = bitmap;

        img = new Mat(bitmap.getHeight(),
                bitmap.getWidth(), CvType.CV_8UC4);
        Utils.bitmapToMat(bitmap, img);


        setContentView(imageView);
        registerForContextMenu(imageView);

        singleton.initDataset();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        imageView = new ImageView(this);
        if(requestCode == IMPORT_PHOTO){
            try {
                if(data == null){
                    return;
                }

                final Uri uri = data.getData();
                final InputStream imageStream =
                        getContentResolver().openInputStream(uri);

                Bitmap selectedImage = null;
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2;

                // limit the size of a picture.
                // If greater than 1.2m minification
                try {
                    if(imageStream.available() >= 1200000){
                        selectedImage =
                                BitmapFactory.decodeStream(imageStream,
                                        null, options);
                    } else {
                        selectedImage =
                                BitmapFactory.decodeStream(imageStream);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Add image to the activity.
                // And judge the orientation of image.
                if(selectedImage.getHeight() > selectedImage.getWidth()){

                    Matrix matrix = new Matrix();
                    matrix.postRotate(270);
                    selectedImage = Bitmap.createBitmap(selectedImage, 0, 0,
                            selectedImage.getWidth(), selectedImage.getHeight(),
                            matrix, true);
                }

                imageView.setImageBitmap(selectedImage);
                setContentView(imageView);
                this.thisBitmap = selectedImage;

                img = new Mat(selectedImage.getHeight(),
                        selectedImage.getWidth(), CvType.CV_8UC4);
//                        selectedImage.getWidth(), CvType.CV_32F);

                Utils.bitmapToMat(selectedImage, img);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        menu.add(3, 200, Menu.NONE, "DELETE");
        menu.add(3, 201, Menu.NONE, "EDIT");
        menu.add(3, 202, Menu.NONE, "SHARE");
        menu.add(3, 203, Menu.NONE, "SHARP");
        super.onCreateContextMenu(menu, view, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case 200:
                deletePhoto();
                break;
            case 201:
                editPhoto();
                break;
            case 202:
                sharePhoto();
                break;
            case 203:
//                sharp();
                break;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_process, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_import:
                Intent photoPickerIntent =
                        new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, IMPORT_PHOTO);
                return true;
            case R.id.menu_register:

                return true;
            case R.id.menu_match:
                double[] results = matchAllRef(getFace(1));

                for (double d: results){

                }

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private double[] matchTwoFace(Bitmap face, Bitmap refFace) {

        // Store the result to be a return value
        double[] result = {0, 0, 0, 0};

        // Get two mat images of two flowers
        Mat image = new Mat(face.getHeight(),
                face.getWidth(), CvType.CV_32F);
        Utils.bitmapToMat(face, image);

        Mat refImage = new Mat(face.getHeight(),
                face.getWidth(), CvType.CV_32F);
        Utils.bitmapToMat(refFace, refImage);

        // Get a FeatureDetector object
        FeatureDetector detector =
                FeatureDetector.create(FeatureDetector.ORB);

        MatOfKeyPoint keyPoint1 = new MatOfKeyPoint();
        MatOfKeyPoint keyPoint2 = new MatOfKeyPoint();

        // To detect the image
        detector.detect(image, keyPoint1);
        detector.detect(refImage, keyPoint2);

//        if(!isGray) {
//            Imgproc.cvtColor(image, image,
//                    Imgproc.COLOR_BGRA2GRAY);
//
//            Imgproc.cvtColor(refImage, refImage,
//                    Imgproc.COLOR_BGRA2GRAY);
//            isGray = true;
//        }

        // Get a descriptorExtractor of two flowers.
        DescriptorExtractor descriptorExtractor =
                DescriptorExtractor.
                        create(DescriptorExtractor.ORB);

        // declare two descriptors to contain the features
        Mat descriptor = new Mat();
        Mat refDescriptor = new Mat();

        // Use descriptor extractor to get the descriptors
        descriptorExtractor.compute(image, keyPoint1, descriptor);
        descriptorExtractor.compute(refImage, keyPoint2, refDescriptor);

        // get the matcher for BF matching
        DescriptorMatcher descriptorMatcher =
                DescriptorMatcher.create(
                        DescriptorMatcher.BRUTEFORCE_HAMMING);

        // normal match:

        // Declare a mat to contain matches
        MatOfDMatch matches = new MatOfDMatch();

        long t1 = System.currentTimeMillis();

        // match with normal
        descriptorMatcher.match(descriptor, refDescriptor, matches);

        double sumOfMatch = 0;

        // to include the matches
        List<DMatch> matchList = matches.toList();

        // to embrace the good matches
        List<DMatch> goodMatchList = new ArrayList<DMatch>();

        // Calculate the max and min
        double max = 0.0;
        double min = 100.0;
        for (int i = 0; i < matchList.size(); i++) {
            Double dist = (double) matchList.get(i).distance;
            if (dist < min && dist != 0) {
                min = dist;
            }
            if (dist > max) {
                max = dist;
            }
        }

        // select out the good matches
        for (DMatch match : matches.toList()) {
            if (match.distance < min * 3) {
                goodMatchList.add(match);
                sumOfMatch += match.distance;
            }
        }

        result[0] = min;
        result[1] = goodMatchList.size();
        result[2] = sumOfMatch / goodMatchList.size();

        long t = System.currentTimeMillis() - t1;
        result[3] = t;

//         Draw matches :

//        MatOfDMatch gm = new MatOfDMatch();
//        gm.fromList(goodMatchList);
////        Log.v("fuck", "count: " + gm.size());
//        drawMatches(image, keyPoint,
//                refImage, refKeyPoint, gm);

        return result;
    }

    private double[] matchAllRef(Bitmap face) {
        Bitmap refFace;
        double[] d = new double[50];
        for(int i = 0; i < 50; i++){
            refFace = getFace(i + 1);

            double[] result =
                    matchTwoFace(face, refFace);

            // get the mean
            d[i] = result[2];
//            Log.v("fuck", "distance: " + d[i]);
        }

        Log.v("fuck", "|*mean of good match is : " + d + " *|");
        return d;
    }

    private void drawMatches(Mat image, MatOfKeyPoint keyPoint,
                             Mat refImage, MatOfKeyPoint refKeyPoint,
                             MatOfDMatch gm){
        // declare a outputImage
        Mat outputImage = new Mat();

        Features2d.drawMatches(image, keyPoint,
                refImage, refKeyPoint,
                gm, outputImage);

        // Display keyPoints
        Bitmap bmp = Bitmap.createBitmap(outputImage.cols(),
                outputImage.rows(), Bitmap.Config.ARGB_8888);

        Utils.matToBitmap(outputImage, bmp);
        imageView.setImageBitmap(bmp);
    }

    private Bitmap convertImgViewToBmp(ImageView imageView){

        Drawable drawable = imageView.getDrawable();
        if(drawable == null){
            Log.d("debug", "drawable is null--------");
            this.imageView = new ImageView(this);
            this.imageView.setImageResource(R.drawable.logo);

            drawable = this.imageView.getDrawable();

            Bitmap exampleBitmap = convertImgViewToBmp(this.imageView);

            img = new Mat(exampleBitmap.getHeight(),
                    exampleBitmap.getWidth(), CvType.CV_8UC4);
            Utils.bitmapToMat(exampleBitmap, img);

            setContentView(this.imageView);
        }

        BitmapDrawable bitmapDrawable = (BitmapDrawable)drawable;
//        if(bitmapDrawable == null){
//            Log.d("debug", "bitmapDrawable is null--------");
//        }
        Bitmap sequenceBitmap = bitmapDrawable.getBitmap();

//        if(sequenceBitmap == null){
//            Log.d("debug", "sequenceBitmap is null--------");
//        }
        return sequenceBitmap;
    }


    private Bitmap getFace(int id){

        Database database = new Database(this.getApplicationContext());
        SQLiteDatabase sqldb = database.getReadableDatabase();

        String[] cols = {"bitmap"};

        Cursor c = sqldb.query("face", cols,
                "id = " + id, null, null, null, null, null);

        c.moveToLast();

        byte[] binaryBitmap = c.getBlob(0);
        Bitmap bitmap = BitmapFactory.decodeByteArray(
                binaryBitmap, 0, binaryBitmap.length);

        c.close();
        sqldb.close();

        return bitmap;
    }

    private void afterProcess(Mat image, Mat procImg){

        this.img = procImg;

        // Create a frame to save bitmap
        Bitmap bitmap = Bitmap.createBitmap(image.cols(),
                image.rows(), Bitmap.Config.ARGB_8888);

        Utils.matToBitmap(procImg, bitmap);
        imageView.setImageBitmap(bitmap);
    }

    /*
  * Show a confirmation dialog. On confirmation, the photo is
  * deleted and the activity finishes.
  */
    private void deletePhoto() {
        final AlertDialog.Builder alert = new AlertDialog.Builder(
                ProcessActivity.this);
        alert.setTitle(R.string.photo_delete_prompt_title);
        alert.setMessage(R.string.photo_delete_prompt_message);
        alert.setCancelable(false);
        alert.setPositiveButton(R.string.delete,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog,
                                        final int which) {
                        getContentResolver().delete(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                MediaStore.MediaColumns.DATA + "=?",
                                new String[] { dataPath });
                        finish(); }
                });
        alert.setNegativeButton(android.R.string.cancel, null);
        alert.show();
    }

    /*
      * Show a chooser so that the user may pick an app for editing
      * the photo.
      */
    private void editPhoto() {
        final Intent intent = new Intent(Intent.ACTION_EDIT);
        intent.setDataAndType(uri, PHOTO_MIME_TYPE);
        startActivity(Intent.createChooser(intent,
                getString(R.string.photo_edit_chooser_title)));
    }

    /*
      * Show a chooser so that the user may pick an app for sending
      * the photo.
      */
    private void sharePhoto() {
        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(PHOTO_MIME_TYPE);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.putExtra(Intent.EXTRA_SUBJECT,
                getString(R.string.photo_send_extra_subject));
        intent.putExtra(Intent.EXTRA_TEXT,
                getString(R.string.photo_send_extra_text));
        startActivity(Intent.createChooser(intent,
                getString(R.string.photo_send_chooser_title)));
    }
}
