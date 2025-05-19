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
        setContentView(R.layout.activity_splash);

        final Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                finish();
            }
        };
        handler.postDelayed(runnable, 5000);
    }
}