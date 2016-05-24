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

    // 单例模式
    private static Singleton singleton = null;
    // 获取andorid sqlite对象
    private Database database = null;
    private SQLiteDatabase sqldb = null;

    public List<Double> chns = new ArrayList<Double>();

    private Singleton(Context context){
        this.database = new Database(context);
    }
    public static Singleton getSingleton(Context context){
        if(singleton == null){
            singleton = new Singleton(context);
        }
        return singleton;
    }

    public boolean initDataset(){
        if(ProcessActivity.count != 0){
            return false;
        }
        ProcessActivity.count++;

        return true;
    }

    // 从数据库当中获取一张图片，根据给定索引
    public Bitmap getFace(String name, int id){

        // 获取数据库对象
        sqldb = database.getReadableDatabase();
        // 给定范围
        String[] cols = {"bitmap"};

        // 结果集
        Cursor c = sqldb.query(name, cols,
                "id = " + id, null, null, null, null, null);

        // 移动结果集指针，指向下一条纪录
        c.moveToLast();
        // 创建一个bitmap，存储结果
        byte[] binaryBitmap = c.getBlob(0);
        Bitmap bitmap = BitmapFactory.decodeByteArray(
                binaryBitmap, 0, binaryBitmap.length);

        // 关闭数据库
        c.close();
        sqldb.close();

        return bitmap;
    }

}
