package com.hnjdzy.newsfragment.ui.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.hnjdzy.newsfragment.R;
import com.hnjdzy.newsfragment.ui.fragment.AddFragment;
import com.hnjdzy.newsfragment.ui.fragment.HomeFragment;
import com.hnjdzy.newsfragment.ui.fragment.NotificationFragment;
import com.hnjdzy.newsfragment.ui.fragment.ProfileFragment;
import com.hnjdzy.newsfragment.ui.fragment.SearchFragment;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView; // 底部导航栏
    private Fragment currentFragment; // 当前显示的 Fragment
    private HomeFragment homeFragment; // 首页 Fragment
    private SearchFragment searchFragment; // 搜索 Fragment
    private AddFragment addFragment; // 发布 Fragment
    private NotificationFragment notificationFragment; // 通知 Fragment
    private ProfileFragment profileFragment; // 我的 Fragment

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // 设置布局文件

        bottomNavigationView = findViewById(R.id.bottom_navigation); // 获取底部导航栏实例

        // 初始化所有 Fragment
        homeFragment = new HomeFragment();
        searchFragment = new SearchFragment();
        addFragment = new AddFragment();
        notificationFragment = new NotificationFragment();
        profileFragment = new ProfileFragment();

        // 默认加载并显示 HomeFragment
        loadFragment(homeFragment);
        currentFragment = homeFragment; // 设置当前 Fragment 为 HomeFragment

        // 设置底部导航栏的选择监听器
        bottomNavigationView.setOnItemSelectedListener(item -> {
            // 根据点击的菜单项ID切换对应的 Fragment
            if (item.getItemId() == R.id.navigation_home) {
                switchFragment(homeFragment); // 切换到首页 Fragment
                return true;
            } else if (item.getItemId() == R.id.navigation_search) {
                switchFragment(searchFragment); // 切换到搜索 Fragment
                return true;
            } else if (item.getItemId() == R.id.navigation_add) {
                switchFragment(addFragment); // 切换到发布 Fragment
                return true;
            } else if (item.getItemId() == R.id.navigation_notification) {
                switchFragment(notificationFragment); // 切换到通知 Fragment
                return true;
            } else if (item.getItemId() == R.id.navigation_profile) {
                switchFragment(profileFragment); // 切换到我的 Fragment
                return true;
            }
            return false; // 未处理的点击事件
        });
    }

    // 切换 Fragment 的方法
    private void switchFragment(Fragment targetFragment) {
        // 如果目标 Fragment 已经是当前显示的 Fragment，则不进行切换
        if (targetFragment == currentFragment) {
            return;
        }

        FragmentManager fm = getSupportFragmentManager(); // 获取 Fragment 管理器
        FragmentTransaction ft = fm.beginTransaction(); // 开启事务

        // 如果目标 Fragment 还未被添加到 Activity，则添加它
        if (!targetFragment.isAdded()) {
            ft.add(R.id.fragment_container, targetFragment, targetFragment.getClass().getName());
        }

        // 如果当前有 Fragment 正在显示，则隐藏它
        if (currentFragment != null) {
            ft.hide(currentFragment);
        }

        ft.show(targetFragment); // 显示目标 Fragment
        ft.commit(); // 提交事务
        currentFragment = targetFragment; // 更新当前 Fragment
    }

    // 初始加载 Fragment 的方法（只在第一次加载时使用）
    private void loadFragment(Fragment fragment) {
        FragmentManager fm = getSupportFragmentManager(); // 获取 Fragment 管理器
        FragmentTransaction ft = fm.beginTransaction(); // 开启事务
        ft.add(R.id.fragment_container, fragment, fragment.getClass().getName()); // 将 Fragment 添加到容器
        ft.commit(); // 提交事务
        currentFragment = fragment; // 设置当前 Fragment
    }
}