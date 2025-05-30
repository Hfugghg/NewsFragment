package com.hnjdzy.newsfragment.ui.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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
import com.hnjdzy.newsfragment.ui.fragment.ProfileFragment; // 引入ProfileFragment以获取API Key

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class NewsDetailActivity extends AppCompatActivity {

    private TextView tvDetailTitle; // 新闻标题TextView
    private TextView tvDetailAuthor; // 新闻作者TextView
    private TextView tvDetailDate;   // 新闻日期TextView
    private TextView tvDetailContent; // 新闻内容TextView
    private final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS) // 设置连接超时时间为 30 秒
            .readTimeout(30, TimeUnit.SECONDS)    // 设置读取超时时间为 30 秒
            .writeTimeout(30, TimeUnit.SECONDS)   // 设置写入超时时间为 30 秒
            .build();
    private ImageButton btnTranslate; // 翻译按钮
    private final Handler mainHandler = new Handler(Looper.getMainLooper()); // 主线程Handler，用于更新UI
    private int nid; // 新闻ID
    private LikeDao likeDao;
    private int currentNewsId = -1; // 存储当前新闻的ID

    private ImageButton btnLike; // 点赞按钮
    private TextView tvLikeCount; // 点赞数量显示
    private LinearLayout llLikeSection;
    // 存储原始新闻内容，用于切换翻译
    private String originalNewsTitle = "";
    private String originalNewsContent = "";
    private boolean isTranslated = false; // 标志当前是否处于翻译状态

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news_detail); // 设置布局文件

        // 初始化所有TextView
        tvDetailTitle = findViewById(R.id.tv_detail_title);
        tvDetailAuthor = findViewById(R.id.tv_detail_author);
        tvDetailDate = findViewById(R.id.tv_detail_date);
        tvDetailContent = findViewById(R.id.tv_detail_content);
        btnTranslate = findViewById(R.id.btn_translate); // 初始化翻译按钮

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

        // 设置点赞按钮的点击事件
        btnLike.setOnClickListener(v -> toggleLike());

        // 设置翻译按钮的点击事件
        btnTranslate.setOnClickListener(v -> toggleTranslation());
    }

    // 重写 onResume() 和 onPause() 方法来管理数据库连接
    @Override
    protected void onResume() {
        super.onResume();
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

            if (currentCount == 0) {
                newCount = likeDao.incrementLikeCount(currentNewsId);
            } else {
                newCount = likeDao.decrementLikeCount(currentNewsId);
            }

            if (newCount != -1) {
                tvLikeCount.setText(String.valueOf(newCount));
                if (newCount > 0) {
                    btnLike.setImageResource(R.drawable.ic_thumb_up_filled);
                } else {
                    btnLike.setImageResource(R.drawable.ic_thumb_up);
                }
            } else {
                Toast.makeText(this, "点赞操作失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 根据新闻ID获取新闻详情
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
                                // 保存原始新闻内容
                                originalNewsTitle = newsDetail.getNtitle();
                                originalNewsContent = newsDetail.getNcontent();

                                if (getSupportActionBar() != null) {
                                    getSupportActionBar().setTitle(originalNewsTitle);
                                }
                                tvDetailTitle.setText(originalNewsTitle);
                                tvDetailAuthor.setText(newsDetail.getNauthor());

                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                                String formattedDate = sdf.format(new Date(newsDetail.getNcreatedate()));
                                tvDetailDate.setText(formattedDate);

                                tvDetailContent.setText(originalNewsContent);
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

    /**
     * 切换翻译状态：如果已翻译则恢复原文，否则进行翻译
     */
    private void toggleTranslation() {
        if (isTranslated) {
            // 恢复原文
            tvDetailTitle.setText(originalNewsTitle);
            tvDetailContent.setText(originalNewsContent);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(originalNewsTitle);
            }
            Toast.makeText(this, "已恢复原文", Toast.LENGTH_SHORT).show();
            isTranslated = false;
        } else {
            // 进行翻译
            translateNewsContent(originalNewsTitle, originalNewsContent);
        }
    }

    /**
     * 调用DeepSeek API进行翻译
     *
     * @param title   待翻译的标题
     * @param content 待翻译的内容
     */
    private void translateNewsContent(String title, String content) {
        String apiKey = ProfileFragment.getApiKey(this); // 获取API密钥
        if (apiKey.isEmpty()) {
            Toast.makeText(this, "请先在个人设置中设置DeepSeek API密钥！", Toast.LENGTH_LONG).show();
            return;
        }

        Log.d("NewsDetailActivity", "DeepSeek API Key: " + apiKey);

        // 构建DeepSeek API请求的JSON体
        MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("model", "deepseek-chat");
            JSONArray messages = new JSONArray();

            // System prompt
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", "你是一个专业的翻译助手。请将用户提供的新闻标题和内容翻译成地道的英文，保留原文的格式和分段。请直接返回翻译后的文本，不要添加任何额外信息或解释。");
            messages.put(systemMessage);

            // User prompt
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", "请翻译以下新闻：\n\n标题：" + title + "\n\n内容：" + content);
            messages.put(userMessage);

            jsonBody.put("messages", messages);
            jsonBody.put("stream", false);

        } catch (JSONException e) {
            e.printStackTrace();
            mainHandler.post(() -> Toast.makeText(NewsDetailActivity.this, "构建翻译请求失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            return;
        }

        RequestBody requestBody = RequestBody.create(jsonBody.toString(), JSON_MEDIA_TYPE);

        Request request = new Request.Builder()
                .url("https://api.deepseek.com/chat/completions")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .post(requestBody)
                .build();

        mainHandler.post(() -> Toast.makeText(NewsDetailActivity.this, "正在翻译中...", Toast.LENGTH_SHORT).show());

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                mainHandler.post(() -> Toast.makeText(NewsDetailActivity.this, "翻译失败: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        Log.d("NewsDetailActivity", "DeepSeek Response: " + responseBody);
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        JSONArray choices = jsonResponse.getJSONArray("choices");
                        if (choices.length() > 0) {
                            JSONObject firstChoice = choices.getJSONObject(0);
                            JSONObject message = firstChoice.getJSONObject("message");
                            String translatedText = message.getString("content").trim();

                            mainHandler.post(() -> {
                                // 解析翻译后的文本，DeepSeek应该会返回带有标题和内容分隔的文本
                                // 这里需要根据DeepSeek返回的格式进行解析
                                String translatedTitle = "";
                                String translatedContent = "";

                                // 简单的解析逻辑，假设返回格式是 "标题：[翻译标题]\n\n内容：[翻译内容]"
                                if (translatedText.contains("标题：") && translatedText.contains("内容：")) {
                                    int titleStartIndex = translatedText.indexOf("标题：") + "标题：".length();
                                    int contentStartIndex = translatedText.indexOf("内容：");

                                    if (contentStartIndex > titleStartIndex) {
                                        translatedTitle = translatedText.substring(titleStartIndex, contentStartIndex).trim();
                                        translatedContent = translatedText.substring(contentStartIndex + "内容：".length()).trim();
                                    } else {
                                        // 如果格式不符合预期，则将整个翻译内容作为新闻内容
                                        translatedContent = translatedText;
                                    }
                                } else {
                                    // 如果没有明确的标题和内容标记，则将整个翻译内容作为新闻内容
                                    translatedContent = translatedText;
                                }

                                if (!translatedTitle.isEmpty()) {
                                    tvDetailTitle.setText(translatedTitle);
                                    if (getSupportActionBar() != null) {
                                        getSupportActionBar().setTitle(translatedTitle);
                                    }
                                }
                                tvDetailContent.setText(translatedContent);
                                isTranslated = true;
                                Toast.makeText(NewsDetailActivity.this, "翻译成功", Toast.LENGTH_SHORT).show();
                            });
                        } else {
                            mainHandler.post(() -> Toast.makeText(NewsDetailActivity.this, "翻译失败: 未找到翻译结果", Toast.LENGTH_SHORT).show());
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        mainHandler.post(() -> Toast.makeText(NewsDetailActivity.this, "解析翻译结果 JSON 失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    Log.e("NewsDetailActivity", "Translation failed, server response: " + response.code() + ", error: " + errorBody);
                    mainHandler.post(() -> Toast.makeText(NewsDetailActivity.this, "翻译失败，服务器响应: " + response.code() + ", 错误信息: " + errorBody, Toast.LENGTH_LONG).show());
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}