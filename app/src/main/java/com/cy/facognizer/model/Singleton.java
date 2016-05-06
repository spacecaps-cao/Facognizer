package com.cy.facognizer.model;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.cy.facognizer.MainActivity;
import com.cy.facognizer.ProcessActivity;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by CY on 15/11/15.
 */
public class Singleton {

    private static Singleton singleton = null;
    private Database database = null;
    private SQLiteDatabase sqldb = null;

    public List<Double> chns = new ArrayList<Double>();
    public double[][][] channels = new double[4][15][3];

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

    public boolean initDataset(){
        if(ProcessActivity.count != 0){
            return false;
        }
        ProcessActivity.count++;

        return true;
    }

    //
    public Bitmap getFace(String name, int id){

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

}
