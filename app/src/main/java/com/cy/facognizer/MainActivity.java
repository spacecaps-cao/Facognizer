package com.cy.facognizer;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import com.cy.facognizer.model.Database;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    // 北邮的图标
    private ImageView imageView;
    // 开始按钮
    private Button button;

    // 资源文件的索引
    public static int[] RES = {
            R.drawable.f01, R.drawable.f02, R.drawable.f03, R.drawable.f04, R.drawable.f05,
            R.drawable.f11, R.drawable.f12, R.drawable.f13, R.drawable.f14, R.drawable.f15,
            R.drawable.f21, R.drawable.f22, R.drawable.f23, R.drawable.f24, R.drawable.f25,
            R.drawable.f31, R.drawable.f32, R.drawable.f33, R.drawable.f34, R.drawable.f35,
            R.drawable.f41, R.drawable.f42, R.drawable.f43, R.drawable.f44, R.drawable.f45,
            R.drawable.f51, R.drawable.f52, R.drawable.f53, R.drawable.f54, R.drawable.f55,
            R.drawable.f61, R.drawable.f62, R.drawable.f63, R.drawable.f64, R.drawable.f65,
            R.drawable.f71, R.drawable.f72, R.drawable.f73, R.drawable.f74, R.drawable.f75,
            R.drawable.f81, R.drawable.f82, R.drawable.f83, R.drawable.f84, R.drawable.f85,
            R.drawable.f91, R.drawable.f92, R.drawable.f93, R.drawable.f94, R.drawable.f95,
            R.drawable.f101, R.drawable.f102, R.drawable.f103, R.drawable.f104, R.drawable.f105,
            R.drawable.f111, R.drawable.f112, R.drawable.f113, R.drawable.f114, R.drawable.f115
    };

    // 生成的头像的索引
    public static int[] POR = {
            R.drawable.p00, R.drawable.p01,
            R.drawable.p10, R.drawable.p11,
            R.drawable.p20, R.drawable.p21,
            R.drawable.p30, R.drawable.p31,
            R.drawable.p40, R.drawable.p41,
            R.drawable.p50, R.drawable.p51,
            R.drawable.p60, R.drawable.p61,
            R.drawable.p70, R.drawable.p71,
            R.drawable.p80, R.drawable.p81,
            R.drawable.p90, R.drawable.p91,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 判断 opencv 是否加载成功
        if (OpenCVLoader.initDebug()) {
            imageView = (ImageView) findViewById(R.id.imageView);
            imageView.setImageResource(R.drawable.logo);
        }

        // 创建一个开是按钮
        button = (Button) findViewById(R.id.button);
        // 按下按钮，跳转到拍照界面
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 跳转到拍照界面
                Intent intent = new Intent(MainActivity.this,
                        CameraActivity.class);
                startActivity(intent);
            }
        });

//        registLoop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 执行注册
        registLoop();
    }

    // 初始化库，将参考照片写入数据库
    private void registLoop() {

        // 用于计算时间
        long t = System.currentTimeMillis();

        // 获取数据库对象
        Database database = new Database(this.getApplicationContext());
        SQLiteDatabase sqldb = database.getWritableDatabase();

        // 创建键值对
        ContentValues cv = new ContentValues();

        // 创建二进制输出流
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // 遍历所有参考图片
        for (int r : RES) {
            // 将将库中的图片转化为bitmap，以便存入数据库
            Bitmap bitmap =
                    BitmapFactory.decodeResource(
                            getResources(), r);

            // 按照png格式解压
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            byte[] binaryBitmap = baos.toByteArray();
            // 在表bitmap中加入此bitmap
            cv.put("bitmap", binaryBitmap);
            try {
                baos.flush();
                // clean
                baos.reset();
            } catch (IOException e) {
                e.printStackTrace();
            }
            sqldb.insert("face", null, cv);
        }

        // 将生成的卡通头像的集合存入数据库，遍历所有卡通头像
        // 与上一步相似，不做过多注释
        for (int r : POR) {

            Bitmap bitmap =
                    BitmapFactory.decodeResource(
                            getResources(), r);

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] binaryBitmap = baos.toByteArray();
            cv.put("bitmap", binaryBitmap);
            try {
                baos.flush();
                // clean
                baos.reset();
            } catch (IOException e) {
                e.printStackTrace();
            }
            sqldb.insert("face", null, cv);
        }
        sqldb.close();

        long t2 = System.currentTimeMillis() - t;
        Log.v("fuck", "time: " + t2);
    }
}
