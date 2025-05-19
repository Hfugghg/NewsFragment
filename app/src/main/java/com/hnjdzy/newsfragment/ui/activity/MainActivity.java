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

    private BottomNavigationView bottomNavigationView;
    private Fragment currentFragment;
    private HomeFragment homeFragment;
    private SearchFragment searchFragment;
    private AddFragment addFragment;
    private NotificationFragment notificationFragment;
    private ProfileFragment profileFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // 初始化 Fragment
        homeFragment = new HomeFragment();
        searchFragment = new SearchFragment();
        addFragment = new AddFragment();
        notificationFragment = new NotificationFragment();
        profileFragment = new ProfileFragment();

        // 默认显示 HomeFragment
        loadFragment(homeFragment);
        currentFragment = homeFragment;

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.navigation_home) {
                switchFragment(homeFragment);
                return true;
            } else if (item.getItemId() == R.id.navigation_search) {
                switchFragment(searchFragment);
                return true;
            } else if (item.getItemId() == R.id.navigation_add) {
                switchFragment(addFragment);
                return true;
            } else if (item.getItemId() == R.id.navigation_notification) {
                switchFragment(notificationFragment);
                return true;
            } else if (item.getItemId() == R.id.navigation_profile) {
                switchFragment(profileFragment);
                return true;
            }
            return false;
        });
    }

    private void switchFragment(Fragment targetFragment) {
        if (targetFragment == currentFragment) {
            return;
        }

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        if (!targetFragment.isAdded()) {
            ft.add(R.id.fragment_container, targetFragment, targetFragment.getClass().getName());
        }

        if (currentFragment != null) {
            ft.hide(currentFragment);
        }

        ft.show(targetFragment);
        ft.commit();
        currentFragment = targetFragment;
    }

    // 初始加载 Fragment 的方法
    private void loadFragment(Fragment fragment) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.add(R.id.fragment_container, fragment, fragment.getClass().getName());
        ft.commit();
        currentFragment = fragment;
    }
}