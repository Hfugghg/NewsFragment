package com.hnjdzy.newsfragment.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View; // 导入 View
import android.view.Window;
import android.widget.Button; // 导入 Button

import androidx.appcompat.app.AppCompatActivity;

import com.hnjdzy.newsfragment.R;

public class SplashActivity extends AppCompatActivity {

    private Handler handler = new Handler(); // 将 handler 声明为成员变量
    private Runnable runnable; // 将 runnable 声明为成员变量

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_splash);

        Button btnSkip = findViewById(R.id.btn_skip); // 找到跳过按钮

        // 定义跳转到 MainActivity 的 Runnable
        runnable = new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                finish();
            }
        };

        // 延迟执行跳转任务
        handler.postDelayed(runnable, 5000); // 5秒后执行

        // 设置跳过按钮的点击事件
        btnSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handler.removeCallbacks(runnable); // 移除延迟任务，防止重复跳转
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 在 Activity 销毁时，确保移除所有未执行的延迟任务，避免内存泄漏
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }
}