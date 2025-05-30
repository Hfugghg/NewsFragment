package com.hnjdzy.newsfragment.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    // news_likes 表结构
    public static final String TABLE_LIKES = "news_likes";
    public static final String COLUMN_NID = "nid";         // 新闻ID (主键)
    public static final String COLUMN_LIKE_COUNT = "like_count";  // 点赞数
    // users 表结构
    public static final String TABLE_USERS = "users";
    public static final String COLUMN_ID = "id";           // 用户ID (主键)
    public static final String COLUMN_USERNAME = "username"; // 用户名
    public static final String COLUMN_PASSWORD = "password"; // 密码
    // 数据库信息（统一为一个数据库）
    private static final String DATABASE_NAME = "app_database.db";
    private static final int DATABASE_VERSION = 1;
    // 建表语句 - news_likes
    private static final String SQL_CREATE_LIKES_TABLE =
            "CREATE TABLE " + TABLE_LIKES + " (" +
                    COLUMN_NID + " INTEGER PRIMARY KEY," +
                    COLUMN_LIKE_COUNT + " INTEGER DEFAULT 0)";  // 默认值0

    // 建表语句 - users
    private static final String SQL_CREATE_USERS_TABLE =
            "CREATE TABLE " + TABLE_USERS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_USERNAME + " TEXT UNIQUE NOT NULL," +  // 用户名唯一
                    COLUMN_PASSWORD + " TEXT NOT NULL)";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 同时创建两个表
        db.execSQL(SQL_CREATE_LIKES_TABLE);
        db.execSQL(SQL_CREATE_USERS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 安全升级：删除旧表后重建（生产环境需数据迁移）
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LIKES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    // 注册用户
    public boolean registerUser(String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_PASSWORD, password);
        long result = db.insert(TABLE_USERS, null, values);
        db.close();
        return result != -1; // -1 表示插入失败（例如用户名已存在）
    }

    // 登录验证
    public long checkUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        long userId = -1;
        Cursor cursor = db.query(TABLE_USERS, new String[]{COLUMN_ID},
                COLUMN_USERNAME + "=? AND " + COLUMN_PASSWORD + "=?",
                new String[]{username, password},
                null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            userId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
        }
        if (cursor != null) {
            cursor.close();
        }
        db.close();
        return userId;
    }
}
