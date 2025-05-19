package com.hnjdzy.newsfragment.ui.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.alibaba.fastjson.JSON;
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

    private TextView tvDetailTitle;
    private TextView tvDetailAuthor;
    private TextView tvDetailDate;
    private TextView tvDetailContent;
    private final OkHttpClient okHttpClient = new OkHttpClient();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private int nid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news_detail);

        tvDetailTitle = findViewById(R.id.tv_detail_title);
        tvDetailAuthor = findViewById(R.id.tv_detail_author);
        tvDetailDate = findViewById(R.id.tv_detail_date);
        tvDetailContent = findViewById(R.id.tv_detail_content);

        // 启用 ActionBar 的返回按钮
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // 获取传递过来的 nid
        nid = getIntent().getIntExtra("nid", -1);
        if (nid != -1) {
            fetchNewsDetail(nid);
        } else {
            Toast.makeText(this, "未能获取新闻ID", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void fetchNewsDetail(int nid) {
        String detailUrl = "http://182.42.154.48:8088/news/servlet/ApiServlet?opr=readnews&nid=" + nid;
        Request request = new Request.Builder().url(detailUrl).build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                mainHandler.post(() -> Toast.makeText(NewsDetailActivity.this, "获取新闻详情失败: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    try {
                        NewsDetail newsDetail = JSON.parseObject(responseData, NewsDetail.class);
                        if (newsDetail != null) {
                            mainHandler.post(() -> {
                                if (getSupportActionBar() != null) {
                                    getSupportActionBar().setTitle(newsDetail.getNtitle()); // 设置 ActionBar 标题
                                }
                                tvDetailTitle.setText(newsDetail.getNtitle());
                                tvDetailAuthor.setText(newsDetail.getNauthor());

                                // 格式化日期
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                                String formattedDate = sdf.format(new Date(newsDetail.getNcreatedate()));
                                tvDetailDate.setText(formattedDate);

                                tvDetailContent.setText(newsDetail.getNcontent());
                            });
                        } else {
                            mainHandler.post(() -> Toast.makeText(NewsDetailActivity.this, "解析新闻详情失败，数据为空", Toast.LENGTH_SHORT).show());
                        }
                    } catch (Exception e) {
                        mainHandler.post(() -> Toast.makeText(NewsDetailActivity.this, "解析新闻详情 JSON 失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                } else {
                    mainHandler.post(() -> Toast.makeText(NewsDetailActivity.this, "获取新闻详情失败，服务器响应: " + response.code(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // 处理返回按钮点击事件
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}