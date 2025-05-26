package com.hnjdzy.newsfragment.Dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.hnjdzy.newsfragment.db.DatabaseHelper;

public class LikeDao {

    private DatabaseHelper dbHelper;
    private SQLiteDatabase database;

    public LikeDao(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    // 打开数据库
    public void open() {
        database = dbHelper.getWritableDatabase();
    }

    // 关闭数据库
    public void close() {
        dbHelper.close();
    }

    /**
     * 获取指定新闻的当前点赞数量
     *
     * @param nid 新闻ID
     * @return 点赞数量，如果新闻不存在则返回0
     */
    public int getLikeCount(int nid) {
        int likeCount = 0;
        Cursor cursor = null;
        try {
            cursor = database.query(
                    DatabaseHelper.TABLE_LIKES,
                    new String[]{DatabaseHelper.COLUMN_LIKE_COUNT},
                    DatabaseHelper.COLUMN_NID + " = ?",
                    new String[]{String.valueOf(nid)},
                    null, null, null
            );

            if (cursor != null && cursor.moveToFirst()) {
                likeCount = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LIKE_COUNT));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return likeCount;
    }

    /**
     * 更新或插入点赞数量
     * 如果新闻已存在，则更新点赞数量；否则插入新记录。
     *
     * @param nid          新闻ID
     * @param newLikeCount 更新后的点赞数量
     * @return 成功更新或插入的行数，-1 表示失败
     */
    public long updateOrInsertLikeCount(int nid, int newLikeCount) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_LIKE_COUNT, newLikeCount);

        // 尝试更新
        int rowsAffected = database.update(
                DatabaseHelper.TABLE_LIKES,
                values,
                DatabaseHelper.COLUMN_NID + " = ?",
                new String[]{String.valueOf(nid)}
        );

        // 如果没有更新任何行（说明该nid不存在），则插入新行
        if (rowsAffected == 0) {
            values.put(DatabaseHelper.COLUMN_NID, nid);
            return database.insert(DatabaseHelper.TABLE_LIKES, null, values);
        } else {
            return rowsAffected;
        }
    }

    /**
     * 增加指定新闻的点赞数量
     *
     * @param nid 新闻ID
     * @return 增加后的点赞数量，如果操作失败则返回-1
     */
    public int incrementLikeCount(int nid) {
        int currentCount = getLikeCount(nid);
        int newCount = currentCount + 1;
        long result = updateOrInsertLikeCount(nid, newCount);
        if (result != -1) {
            return newCount;
        } else {
            return -1; // 操作失败
        }
    }

    /**
     * 减少指定新闻的点赞数量 (如果需要取消点赞功能)
     *
     * @param nid 新闻ID
     * @return 减少后的点赞数量，如果操作失败则返回-1
     */
    public int decrementLikeCount(int nid) {
        int currentCount = getLikeCount(nid);
        if (currentCount > 0) { // 确保点赞数不会是负数
            int newCount = currentCount - 1;
            long result = updateOrInsertLikeCount(nid, newCount);
            if (result != -1) {
                return newCount;
            } else {
                return -1; // 操作失败
            }
        }
        return currentCount; // 如果当前点赞数为0，则不进行操作
    }
}