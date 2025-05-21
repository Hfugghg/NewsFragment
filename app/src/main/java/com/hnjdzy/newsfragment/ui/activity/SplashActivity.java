package com.hnjdzy.newsfragment.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;

import androidx.appcompat.app.AppCompatActivity; // 导入 AppCompatActivity

import com.hnjdzy.newsfragment.R;

public class SplashActivity extends AppCompatActivity { // 继承 AppCompatActivity
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // 移除标题栏
        setContentView(R.layout.activity_splash); // 设置布局文件

        final Handler handler = new Handler(); // 创建一个 Handler 对象
        Runnable runnable = new Runnable() { // 创建一个 Runnable 对象
            @Override
            public void run() {
                // 在 Runnable 中定义需要执行的任务
                startActivity(new Intent(SplashActivity.this, MainActivity.class)); // 启动 MainActivity
                finish(); // 结束当前的 SplashActivity
            }
        };
        handler.postDelayed(runnable, 5000); // 延迟 5000 毫秒（5秒）后执行 runnable
    }
}