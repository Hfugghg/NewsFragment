package com.hnjdzy.newsfragment.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    // 表名和列名
    public static final String TABLE_LIKES = "news_likes";
    public static final String COLUMN_NID = "nid"; // 新闻ID
    public static final String COLUMN_LIKE_COUNT = "like_count"; // 点赞数量
    private static final String DATABASE_NAME = "news_likes.db";
    private static final int DATABASE_VERSION = 1;
    // 创建表的 SQL 语句
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + TABLE_LIKES + " (" +
                    COLUMN_NID + " INTEGER PRIMARY KEY," + // 使用 INTEGER PRIMARY KEY 作为 nid，确保唯一性
                    COLUMN_LIKE_COUNT + " INTEGER DEFAULT 0)"; // 默认点赞数为0

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 当数据库版本升级时，这里可以进行数据迁移或重建表
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LIKES);
        onCreate(db);
    }
}
