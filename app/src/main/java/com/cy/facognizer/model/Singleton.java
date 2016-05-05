package com.cy.facognizer.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.cy.facognizer.ProcessActivity;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by CY on 15/11/15.
 */
public class Singleton {

    // Path
    private static final String path = "/storage/sdcard/Pictures";
    private static final String FOLDER_NAME = "/dataset";

    private static final String DAISY = "/daisy";
    private static final String WIND = "/windflower";
    private static final String LILY  = "/arum lily";
    private static final String BUSH  = "/silverbush";

    private static final String DAFF = "/daffodil";
    private static final String SUN = "/sunflower";
    private static final String SUSAN = "/black-eyed susan";
    private static final String CUP = "/buttercup";

    private static final String ANTH = "/anthurium";
    private static final String BOI = "/bishop of llandaff";
    private static final String GERAN = "/geranium";

    private static final String OST = "/osteospermum";
    private static final String HIBI = "/hibiscus";
    private static final String PELAR = "/pelargonium";

    private static Singleton singleton = null;
    private Database database = null;
    private SQLiteDatabase sqldb = null;

    public List<Double> chns = new ArrayList<Double>();
    public double[][][] channels = new double[4][15][3];

//      this.database.getReadableDatabase();

//    // contains all reference
//    public static Flower[][] references = new Flower[4][5];
//
//    //contains all sample
//    public static Flower[][] samples = new Flower[4][5];

    private Singleton(Context context){
        this.database = new Database(context);
    }

    public static Singleton getSingleton(Context context){
        if(singleton == null){
            singleton = new Singleton(context);
        }
        return singleton;
    }

    private void getColor(Mat img) {
        int w = img.rows() / 4;
        int h = img.cols() / 4;
        Mat m = img.submat(w, w * 2, h, h * 2);

        Scalar chnn = Core.mean(m);
        chns.add(chnn.val[2]);
        chns.add(chnn.val[1]);
        chns.add(chnn.val[0]);
//        Log.v("fuck", "B: " + chnn.val[2]);//B
//        Log.v("fuck", "G: " + chnn.val[1]);//G
//        Log.v("fuck", "R: " + chnn.val[0]);//R

    }

    private void registLoop(String name, String[] ids, String folder){

        sqldb = database.getWritableDatabase();

        ContentValues cv = new ContentValues();

        String realPath = path + FOLDER_NAME +
                folder + "/image_";

        String extension = ".jpg";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        for(int i = 0; i < 5; i++) {

            //To convert the bitmap to binary strings
            Bitmap bitmap =
                    BitmapFactory.decodeFile(
                            realPath + ids[i] + extension);

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

            Mat m = new Mat();
            Utils.bitmapToMat(bitmap, m);
            getColor(m);

            byte[] binaryBitmap = baos.toByteArray();

            cv.put("bitmap", binaryBitmap);

            try {
                baos.flush();
                // clean
                baos.reset();
            } catch (IOException e) {
                e.printStackTrace();
            }

            sqldb.insert(name, null, cv);
        }
        Log.v("fuck", "-------------------------------");
        sqldb.close();
    }

    private void recgLoop(String name, String[] ids, String folder){

        sqldb = database.getReadableDatabase();


        String realPath = path + FOLDER_NAME +
                folder + "/image_";

        String extension = ".jpg";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        for(int i = 0; i < 5; i++) {

            //To convert the bitmap to binary strings
            Bitmap bitmap =
                    BitmapFactory.decodeFile(
                            realPath + ids[i] + extension);

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] binaryBitmap = baos.toByteArray();

            try {
                baos.flush();
                // clean
                baos.reset();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        sqldb.close();
    }

    public boolean initDataset(){
        if(ProcessActivity.count != 0){
            return false;
        }
        ProcessActivity.count++;

        String[] daisy = {"0806", "0813", "0814", "0831", "0832"};
        String[] wind  = {"1204", "1206", "1213", "1214", "1217"};
        String[] bush  = {"06098", "06101", "06102", "06108", "06111"};

        String[] daff  = {"0005", "0006", "0014", "0021", "0024"};
        String[] sun   = {"0721", "0723", "0728", "0731", "0737"};
        String[] susan   = {"05849", "05850", "05853", "05861", "05873"};

        String[] anth   = {"01973", "01978", "01979", "01980", "01981"};
        String[] boi   = {"02759", "02763", "02776", "02781", "02799"};
        String[] geran   = {"02640", "02643", "02649", "02651", "02652"};

        String[] ost   = {"05525", "05529", "05536", "05543", "05561"};
        String[] hibi   = {"02872", "02881", "02889", "02890", "02893"};
        String[] pelar   = {"04700", "04714", "04718", "04720", "04721"};

        registLoop(database.TABLE_NAME_DAISY, daisy, DAISY);
        registLoop(database.TABLE_NAME_WIND, wind, WIND);
        registLoop(database.TABLE_NAME_BUSH, bush, BUSH);

        registLoop(database.TABLE_NAME_DAFF, daff, DAFF);
        registLoop(database.TABLE_NAME_SUN, sun, SUN);
        registLoop(database.TABLE_NAME_SUSAN, susan, SUSAN);

        registLoop(database.TABLE_NAME_ANTH, anth, ANTH);
        registLoop(database.TABLE_NAME_BOI, boi, BOI);
        registLoop(database.TABLE_NAME_GERAN, geran, GERAN);

        registLoop(database.TABLE_NAME_OST, ost, OST);
        registLoop(database.TABLE_NAME_HIBI, hibi, HIBI);
        registLoop(database.TABLE_NAME_PELAR, pelar, PELAR);



        return true;
    }

//
    public Bitmap getFlower(String name, int id){

        sqldb = database.getReadableDatabase();

        String[] cols = {"bitmap"};

        Cursor c = sqldb.query(name, cols,
                "id = " + id, null, null, null, null, null);

        c.moveToLast();

        byte[] binaryBitmap = c.getBlob(0);
        Bitmap bitmap = BitmapFactory.decodeByteArray(
                binaryBitmap, 0, binaryBitmap.length);


        c.close();
        sqldb.close();

        return bitmap;
    }

    public List<Integer> queryAll(){

        List<Integer> list = new LinkedList<Integer>();

        Cursor c = sqldb.query("daisy", null, null, null, null, null, null);

        c.moveToFirst();

        sqldb.close();

//        while(c.moveToNext()){
//            result = c.getBlob(1);
//            ByteArrayInputStream bais = new ByteArrayInputStream(result);
//            ObjectInputStream ois = null;
//            try{
//                ois = new ObjectInputStream(bais);
//
//                flower = (Flower)ois.readObject();
//
//                list.add(flower);
//
//            } catch(Exception e){
//                e.printStackTrace();
//            }finally {
//                try {
//                    ois.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                try {
//                    bais.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
        return list;
    }

}
