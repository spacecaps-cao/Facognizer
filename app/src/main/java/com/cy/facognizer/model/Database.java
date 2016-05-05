package com.cy.facognizer.model;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by CY on 15/11/13.
 */
public class Database extends SQLiteOpenHelper{

    public static final String DATABASE_NAME = "flognizer";

    // white
    public static final String TABLE_NAME_DAISY = "daisy";
    public static final String TABLE_NAME_WIND  = "wind";
    public static final String TABLE_NAME_BUSH  = "silverbush";

    // yellow
    public static final String TABLE_NAME_DAFF  = "daff";
    public static final String TABLE_NAME_SUN   = "sun";
    public static final String TABLE_NAME_SUSAN = "susan";

    // red
    public static final String TABLE_NAME_ANTH  = "anthurium";
    public static final String TABLE_NAME_BOI   = "bishop";
    public static final String TABLE_NAME_GERAN = "geranium";

    // pink
    public static final String TABLE_NAME_OST   = "osteospermum";
    public static final String TABLE_NAME_HIBI  = "hibiscus";
    public static final String TABLE_NAME_PELAR = "pelargonium";

    public static final String[] tabelNames = {
            TABLE_NAME_DAISY, TABLE_NAME_WIND,
            TABLE_NAME_BUSH,

            TABLE_NAME_DAFF, TABLE_NAME_SUN,
            TABLE_NAME_SUSAN,

            TABLE_NAME_ANTH, TABLE_NAME_BOI,
            TABLE_NAME_GERAN,

            TABLE_NAME_OST,
            TABLE_NAME_HIBI, TABLE_NAME_PELAR};

    public Database(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

//        db.execSQL("CREATE TABLE " + TABLE_NAME_DAISY +
//                " (id INTEGER PRIMARY KEY, bitmap BLOB);");
//
//        db.execSQL("CREATE TABLE " + TABLE_NAME_WIND +
//                " (id INTEGER PRIMARY KEY, bitmap BLOB);");
//
//        db.execSQL("CREATE TABLE " + TABLE_NAME_DAFF +
//                " (id INTEGER PRIMARY KEY, bitmap BLOB);");
//
//        db.execSQL("CREATE TABLE " + TABLE_NAME_SUN +
//                " (id INTEGER PRIMARY KEY, bitmap BLOB);");

        for(String name : tabelNames){
            db.execSQL("CREATE TABLE " + name +
                    " (id INTEGER PRIMARY KEY, bitmap BLOB);");
        }

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion,
                          int newVersion) {

    }


}
