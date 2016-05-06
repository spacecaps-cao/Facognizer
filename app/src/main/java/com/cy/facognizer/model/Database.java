package com.cy.facognizer.model;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by CY on 15/11/13.
 */
public class Database extends SQLiteOpenHelper{

    public static final String DATABASE_NAME = "facognizer";

    public Database(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL("CREATE TABLE " + "face" +
                " (id INTEGER PRIMARY KEY, bitmap BLOB);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion,
                          int newVersion) {

    }


}
