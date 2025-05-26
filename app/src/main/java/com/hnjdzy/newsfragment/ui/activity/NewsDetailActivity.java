package com.hnjdzy.newsfragment.ui.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSON;
import com.hnjdzy.newsfragment.Dao.LikeDao;
import com.hnjdzy.newsfragment.R;
import com.hnjdzy.newsfragment.model.NewsDetail;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NewsDetailActivity extends AppCompatActivity {

    private TextView tvDetailTitle; // 新闻标题TextView
    private TextView tvDetailAuthor; // 新闻作者TextView
    private TextView tvDetailDate;   // 新闻日期TextView
    private TextView tvDetailContent; // 新闻内容TextView
    private final OkHttpClient okHttpClient = new OkHttpClient(); // OkHttpClient 实例，用于发送网络请求
    private final Handler mainHandler = new Handler(Looper.getMainLooper()); // 主线程Handler，用于更新UI
    private int nid; // 新闻ID
    private LikeDao likeDao;
    private int currentNewsId = -1; // 存储当前新闻的ID

    private ImageButton btnLike; // 点赞按钮
    private TextView tvLikeCount; // 点赞数量显示
    private LinearLayout llLikeSection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news_detail); // 设置布局文件

        // 初始化所有TextView
        tvDetailTitle = findViewById(R.id.tv_detail_title);
        tvDetailAuthor = findViewById(R.id.tv_detail_author);
        tvDetailDate = findViewById(R.id.tv_detail_date);
        tvDetailContent = findViewById(R.id.tv_detail_content);

        btnLike = findViewById(R.id.btn_like);
        tvLikeCount = findViewById(R.id.tv_like_count);
        llLikeSection = findViewById(R.id.ll_like_section);

        likeDao = new LikeDao(this);
        likeDao.open();

        // 启用 ActionBar 的返回按钮
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // 获取传递过来的新闻ID (nid)
        nid = getIntent().getIntExtra("nid", -1);
        currentNewsId = nid; // 存储当前新闻的ID

        if (nid != -1) {
            // 如果成功获取到nid，则请求新闻详情
            fetchNewsDetail(nid);

            updateLikeStatus(); // 更新点赞状态
        } else {
            // 如果未能获取到nid，则显示提示并关闭当前Activity
            Toast.makeText(this, "未能获取新闻ID", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnLike.setOnClickListener(v -> toggleLike());
    }

    // 重写 onResume() 和 onPause() 方法来管理数据库连接
    @Override
    protected void onResume() {
        super.onResume();
//        if (likeDao != null) {
//            likeDao.open(); // 在 Activity resume 时打开数据库
//        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (likeDao != null) {
            likeDao.close(); // 在 Activity pause 时关闭数据库
        }
    }

    // 处理 ActionBar 的返回按钮点击事件
    private void updateLikeStatus() {
        if (currentNewsId != -1) {
            // 从数据库获取当前新闻的点赞数量
            int count = likeDao.getLikeCount(currentNewsId);
            // 更新点赞数量 TextView
            tvLikeCount.setText(String.valueOf(count));

            // 根据点赞数量来判断是否已点赞，并设置图标
            // 这里的逻辑是：如果点赞数大于0，就认为用户已点赞，显示填充图标；否则显示空心图标
            if (count > 0) {
                btnLike.setImageResource(R.drawable.ic_thumb_up_filled);
            } else {
                btnLike.setImageResource(R.drawable.ic_thumb_up);
            }
        }
    }

    // 点赞/取消点赞
    private void toggleLike() {
        if (currentNewsId != -1) {
            // 获取当前新闻的点赞数量
            int currentCount = likeDao.getLikeCount(currentNewsId);
            int newCount;

            // 简单的点赞/取消点赞逻辑：
            // 如果当前点赞数为0，点击则执行点赞操作，点赞数+1；
            // 否则（点赞数>0），点击则执行取消点赞操作，点赞数-1。
            if (currentCount == 0) {
                newCount = likeDao.incrementLikeCount(currentNewsId);
            } else {
                newCount = likeDao.decrementLikeCount(currentNewsId);
            }

            // 判断操作是否成功
            if (newCount != -1) {
                // 操作成功，更新 UI
                tvLikeCount.setText(String.valueOf(newCount));
                // 根据新的点赞数量更新图标
                if (newCount > 0) {
                    btnLike.setImageResource(R.drawable.ic_thumb_up_filled);
                } else {
                    btnLike.setImageResource(R.drawable.ic_thumb_up);
                }
            } else {
                // 操作失败，可以显示一个Toast提示用户
                Toast.makeText(this, "点赞操作失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 根据新闻ID获取新闻详情
    private void fetchNewsDetail(int nid) {
        // 构建请求URL
        String detailUrl = "http://182.42.154.48:8088/news/servlet/ApiServlet?opr=readnews&nid=" + nid;
        // 创建Request对象
        Request request = new Request.Builder().url(detailUrl).build();

        // 使用OkHttpClient发送异步网络请求
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // 请求失败时的回调
                e.printStackTrace(); // 打印异常堆栈信息
                // 在主线程显示Toast提示
                mainHandler.post(() -> Toast.makeText(NewsDetailActivity.this, "获取新闻详情失败: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                // 请求成功时的回调
                if (response.isSuccessful()) {
                    // 如果响应成功，获取响应体数据
                    String responseData = response.body().string();
                    try {
                        // 使用FastJSON将JSON字符串解析成NewsDetail对象
                        NewsDetail newsDetail = JSON.parseObject(responseData, NewsDetail.class);
                        if (newsDetail != null) {
                            // 在主线程更新UI
                            mainHandler.post(() -> {
                                if (getSupportActionBar() != null) {
                                    getSupportActionBar().setTitle(newsDetail.getNtitle()); // 设置 ActionBar 标题为新闻标题
                                }
                                tvDetailTitle.setText(newsDetail.getNtitle()); // 设置新闻标题
                                tvDetailAuthor.setText(newsDetail.getNauthor()); // 设置新闻作者

                                // 格式化日期
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                                String formattedDate = sdf.format(new Date(newsDetail.getNcreatedate()));
                                tvDetailDate.setText(formattedDate); // 设置新闻日期

                                tvDetailContent.setText(newsDetail.getNcontent()); // 设置新闻内容
                            });
                        } else {
                            // 如果解析到的数据为空，则显示提示
                            mainHandler.post(() -> Toast.makeText(NewsDetailActivity.this, "解析新闻详情失败，数据为空", Toast.LENGTH_SHORT).show());
                        }
                    } catch (Exception e) {
                        // JSON解析异常处理
                        mainHandler.post(() -> Toast.makeText(NewsDetailActivity.this, "解析新闻详情 JSON 失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                } else {
                    // 如果服务器响应不成功，则显示错误码
                    mainHandler.post(() -> Toast.makeText(NewsDetailActivity.this, "获取新闻详情失败，服务器响应: " + response.code(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // 处理ActionBar的菜单项点击事件
        if (item.getItemId() == android.R.id.home) {
            finish(); // 如果点击的是返回按钮，则关闭当前Activity
            return true;
        }
        return super.onOptionsItemSelected(item); // 调用父类方法处理其他菜单项
    }
}