package com.cy.facognizer;

import android.app.AlertDialog;
import android.content.ContentValues;
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
import android.os.Environment;
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
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class ProcessActivity extends AppCompatActivity {

    public static final String PHOTO_FILE_EXTENSION = ".png";
    public static final String PHOTO_MIME_TYPE = "image/png";

    // A matrix that is used when saving photos.
    private Mat photoMatrix;

    // 输出头像的存放地址
    public static final String EXTRA_PHOTO_URI =
            "com.cy.facognizer.view.ProcessActivity.extra.PHOTO_URI";
    public static final String EXTRA_PHOTO_DATA_PATH =
            "com.cy.facognizer.view.ProcessActivity.extra.PHOTO_DATA_PATH";

    private Uri uri;
    private String dataPath;
    public static final int IMPORT_PHOTO = 0;

    // 存储Mat和Bitmap
    private Mat img;
    private Bitmap bitmap;

    // 保存后的图片
    private Mat processedImg;

    // 当前显示的图片
    private ImageView imageView;
    // 判断是否为灰度图
    private boolean isGray = false;

    // Singleton，单例模式的数据库操作对象
    private Singleton singleton = Singleton.getSingleton(ProcessActivity.this);

    // 计数器
    public static int count = 0;

    // 当前图片的 Bitmap
    private Bitmap thisBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();

        // 获取从照相机界面传递来的照片
        uri = intent.getParcelableExtra(EXTRA_PHOTO_URI);
        dataPath = intent.getStringExtra(EXTRA_PHOTO_DATA_PATH);
        imageView = new ImageView(this);
        imageView.setImageURI(uri);

        //转化为bitmap
        Bitmap bitmap = convertImgViewToBmp(imageView);
        this.bitmap = bitmap;

        // 转化为mat
        img = new Mat(bitmap.getHeight(),
                bitmap.getWidth(), CvType.CV_8UC4);
        Utils.bitmapToMat(bitmap, img);

        // 显示在当前界面
        setContentView(imageView);
        // 为当前图片添加菜单，长按之后就可以删除或者编辑
        registerForContextMenu(imageView);

        // 初始化数据库
        singleton.initDataset();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        imageView = new ImageView(this);
        if(requestCode == IMPORT_PHOTO){
            try {
                // 如果有数据
                if(data == null){
                    return;
                }

                // 创建输入流，对图片进行解码
                final Uri uri = data.getData();
                final InputStream imageStream =
                        getContentResolver().openInputStream(uri);

                // 设置解码清晰度
                Bitmap selectedImage = null;
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2;

                // 把图片限制在1.2m以内，太大了没用
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

                    // 创建一个矩阵
                    Matrix matrix = new Matrix();
                    // 旋转图片，适应横屏
                    matrix.postRotate(270);
                    selectedImage = Bitmap.createBitmap(selectedImage, 0, 0,
                            selectedImage.getWidth(), selectedImage.getHeight(),
                            matrix, true);
                }

                // 当前界面显示拍照的结果
                imageView.setImageBitmap(selectedImage);
                setContentView(imageView);
                this.thisBitmap = selectedImage;

                // 将bitmap转化为opencv矩阵，4通道
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
            // 导入图片
            case R.id.menu_import:
                Intent photoPickerIntent =
                        new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, IMPORT_PHOTO);
                return true;
            // 匹配图片
            case R.id.menu_match:
                // 保存60个匹配结果
                double[] r60 = matchAllRef(this.bitmap);
                // 保存12 个生成的头像的索引
                double[] r12 = new double[12];

                // 求最小值
                double mean = 500;
                for (int i = 0; i < 12; i++){
                    r12[i] += r60[i * 5];
                    r12[i] += r60[i * 5 + 1];
                    r12[i] += r60[i * 5 + 2];
                    r12[i] += r60[i * 5 + 3];
                    r12[i] += r60[i * 5 + 4];

                    // 正则表达式
                    mean = r12[i] < mean ? r12[i] : mean;
                }

                // 选出最小值对应的头像
                for (int i = 0; i < 12; i++){
                    if (r12[i] == mean){
//                        Log.v("fuck", "index: " + i);
                        output(i);
                    }
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void grabCut(Mat src){

        // 首先，搞一个矩形框。外边默认是背景，里边的内容将会被执行grabcut。
        int w = src.cols() - 10;
        int h = src.rows() - 10;
        Rect rect = new Rect(5, 5, w, h);

        // 创建两个Mat：前景和背景。注意，前景不等于结果，只是一个内部中间量。
        Mat bg = new Mat();
        Mat fg = new Mat();

        // 创建一个用来装结果的Mat，并开始grabcut：
        Mat result = new Mat();
        Imgproc.grabCut(src, result, rect,
                bg, fg, 1, Imgproc.GC_INIT_WITH_RECT);
        Mat source = new Mat(1, 1, CvType.CV_8U, new Scalar(Imgproc.GC_PR_FGD));
//        Mat source = new Mat(1, 1, CvType.CV_8U, new Scalar(3.0));
//        Core.compare(result, new Scalar(Imgproc.GC_PR_FGD),
//                result, Core.CMP_EQ);

        Core.compare(result, source, result, Core.CMP_EQ);

//        //
//        Mat foreground = new Mat(src.size(),
//                CvType.CV_8UC3, new Scalar(238,138,120));
//        src.copyTo(foreground, result);
//
//        Bitmap b = Bitmap.createBitmap(foreground.cols(),
//                foreground.rows(), Bitmap.Config.RGB_565);
//
//        Utils.matToBitmap(foreground, b);
//        imageView.setImageBitmap(b);

        //
        Mat background = new Mat(src.size(),
                CvType.CV_8UC3, new Scalar(100,200,200));
        src.copyTo(background, result);

        Bitmap b = Bitmap.createBitmap(background.cols(),
                background.rows(), Bitmap.Config.RGB_565);

        Utils.matToBitmap(background, b);
        imageView.setImageBitmap(b);
    }

    // 输入卡通头像
    private void output(int i){
        int r = 0;
        if(i == 10){
            r = MainActivity.POR[0];
        } else if (i == 11) {
            r = MainActivity.POR[4];
        } else {
            // 产生随机数，0 ～ 1 之间
            // （因为每个结果对应2个随机头像）
            double random = Math.random();

            r = random > 0.5 ?
                    MainActivity.POR[i * 2] :
                    MainActivity.POR[i * 2 + 1];
        }

        // 显示提示
        Toast.makeText(this, "So Cute!", Toast.LENGTH_LONG).show();

        // 转化为mat，以便保存
        Bitmap bitmap =
                BitmapFactory.decodeResource(
                        getResources(), r);
        this.imageView.setImageBitmap(bitmap);
        Mat mat = new Mat(bitmap.getHeight(),
                bitmap.getHeight(), CvType.CV_8UC4);
        Utils.bitmapToMat(bitmap, mat);

        // 保存结果头像，写入相册里
        takePhoto(mat);

        // save output
//        File dir = new File(Environment.getExternalStorageDirectory() +
//                File.separator + "drawable");
//        File dir = new File(albumPath);
//
//        boolean b = saveBitmapToFile(dir,
//                "output-" + System.currentTimeMillis() + ".png",
//                bitmap,Bitmap.CompressFormat.PNG, 100);
//        Log.v("fuck", "succ ? " + b);
//        Toast.makeText(this, "succ ? " + b,
//                Toast.LENGTH_LONG).show();

//        boolean doSave = true;
//        if (!dir.exists()) {
//            Log.v("fuck", " not exists");
//            doSave = dir.mkdirs();
//        }
//
//        if (doSave) {
//            boolean b = saveBitmapToFile(dir,
//        "output-" + System.currentTimeMillis() + ".png",
//                bitmap,Bitmap.CompressFormat.PNG, 100);
//        Log.v("fuck", "succ ? " + b);
//        }
//        else {
//            Log.v("fuck","Couldn't create target directory.");
//        }

    }

    // The "Take Photo" method
    public void takePhoto( Mat rgba){

        // 获取当前上下文的路径.
        final long currentTimeMills = System.currentTimeMillis();
        final String appName = getString(R.string.app_name);
        final String galleryPath =
                Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES).toString();
        final String albumPath = galleryPath + File.separator +
                appName;
        final String photoPath = albumPath + File.separator +
                currentTimeMills + ProcessActivity.PHOTO_FILE_EXTENSION;

        // 创建一个键值对
        final ContentValues values = new ContentValues();

        // 读取照片到内存
        values.put(MediaStore.MediaColumns.DATA, photoPath);
        values.put(MediaStore.Images.Media.MIME_TYPE,
                ProcessActivity.PHOTO_MIME_TYPE);
        values.put(MediaStore.Images.Media.TITLE, appName);
        values.put(MediaStore.Images.Media.DESCRIPTION, appName);
        values.put(MediaStore.Images.Media.DATE_TAKEN, currentTimeMills);

        // 确保目录存在
        File album = new File(albumPath);
        if(!album.isDirectory() && !album.mkdirs()){
            Log.v("fuck","Failed to create album directory at " +
                    albumPath);
            return;
        }

        // 开始保存
        if(!Imgcodecs.imwrite(photoPath, rgba)){
//            log("Failed to save photo to " + photoPath);
        }
        Toast.makeText(this, "Photo saved successfully to " + photoPath,
                Toast.LENGTH_LONG).show();

        // 加入媒体库
        Uri uri;
        try {
            uri = getContentResolver().insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        } catch (final Exception e) {
            Log.v("fuck", "Failed to insert photo into MediaStore");
            e.printStackTrace();

            // Since the insertion failed, delete the photo.
            File photo = new File(photoPath);
            if (!photo.delete()){
                Log.v("fuck", "Failed to delete non-inserted photo");
            }
            return;
        }

    }

    // 匹配两张照片，返回一个double数，以描述相似程度
    private double[] matchTwoFace(Bitmap face, Bitmap refFace) {

        // 创建一个空的数组，以储存结果
        double[] result = {0, 0, 0, 0};

        // 获取两张图片的矩阵
        Mat image = new Mat(face.getHeight(),
                face.getWidth(), CvType.CV_32F);
        Utils.bitmapToMat(face, image);

        Mat refImage = new Mat(face.getHeight(),
                face.getWidth(), CvType.CV_32F);
        Utils.bitmapToMat(refFace, refImage);

        // 创建FeatureDetector对象
        FeatureDetector detector =
                FeatureDetector.create(FeatureDetector.ORB);

        MatOfKeyPoint keyPoint1 = new MatOfKeyPoint();
        MatOfKeyPoint keyPoint2 = new MatOfKeyPoint();

        // 开始为每个图片探测500个特征点
        detector.detect(image, keyPoint1);
        detector.detect(refImage, keyPoint2);

        // 转化为灰度图
        Imgproc.cvtColor(image, image,
                    Imgproc.COLOR_BGRA2GRAY);

        // 创建descriptorExtractor
        DescriptorExtractor descriptorExtractor =
                DescriptorExtractor.
                        create(DescriptorExtractor.ORB);

        // 分别创建两个描述算子
        Mat descriptor = new Mat();
        Mat refDescriptor = new Mat();

        // 开始将两张图的特征点进行描述
        descriptorExtractor.compute(
                image, keyPoint1, descriptor);
        descriptorExtractor.compute(
                refImage, keyPoint2, refDescriptor);

        // 创建一个Brute-Force Hamming 匹配器对象
        DescriptorMatcher descriptorMatcher =
                DescriptorMatcher.create(
                        DescriptorMatcher.BRUTEFORCE_HAMMING);

        // 创建一个容器，以保存匹配结果
        MatOfDMatch matches = new MatOfDMatch();

        // 开始匹配
        descriptorMatcher.match(descriptor, refDescriptor, matches);

        double sumOfMatch = 0;

        // 使用集合保存结果
        List<DMatch> matchList = matches.toList();

        // 创建集合保存筛选后的结果
        List<DMatch> goodMatchList = new ArrayList<DMatch>();

        // 计算500个匹配结果中的最大值和最小值
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

        // 计算时间
        long t1 = System.currentTimeMillis();
        // 筛选出好的匹配点，并且存入前边定义的集合
        for (DMatch match : matches.toList()) {
            if (match.distance < min * 3) {
                goodMatchList.add(match);
                sumOfMatch += match.distance;
            }
        }

        // 纪录4个结果，返回
        result[0] = min;
        result[1] = goodMatchList.size();
        result[2] = sumOfMatch / goodMatchList.size();
        long t = System.currentTimeMillis() - t1;
        result[3] = t;

        Log.v("fuck", "time: " + t);

//         Draw matches :

//        MatOfDMatch gm = new MatOfDMatch();
//        gm.fromList(goodMatchList);
////        Log.v("fuck", "count: " + gm.size());
//        drawMatches(image, keyPoint,
//                refImage, refKeyPoint, gm);

        return result;
    }

    // 匹配所有的库中的图片，该方法旨在将前边的方法迭代
    private double[] matchAllRef(Bitmap face) {
        Bitmap refFace;
        double[] d = new double[60];
        // 循环60次，因为库中有60张图片
        for(int i = 0; i < 60; i++){
            refFace = getFace(i + 1);

            // 存储结果
            double[] result =
                    matchTwoFace(face, refFace);

            // 获取最小值
            d[i] = result[2];
//            Log.v("fuck", "No. " + i + "distance: " + d[i]);
        }

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

    // 把当前显示的图片转化为bitmap
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

    // 从数据库当中提取一个图片
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


    public boolean saveBitmapToFile(File dir, String fileName, Bitmap bm,
                                    Bitmap.CompressFormat format, int quality) {

        File imageFile = new File(dir,fileName);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(imageFile);

            bm.compress(format,quality,fos);

            fos.close();

            return true;
        }
        catch (IOException e) {
            Log.e("app",e.getMessage());
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return false;
    }


}

