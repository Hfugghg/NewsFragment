package com.hnjdzy.newsfragment.ui.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.hnjdzy.newsfragment.R; // 确保R文件路径正确
import com.hnjdzy.newsfragment.db.DatabaseHelper;

public class ProfileFragment extends Fragment {

    private static final String PREFS_NAME = "NewsAppPrefs";
    private static final String API_KEY_KEY = "api_key";

    private RelativeLayout itemApiSettings;

    private static final String AVATAR_FILE_NAME = "user_avatar.png";
    // 以下是注册和登录相关
    private static final String PREF_IS_LOGGED_IN = "is_logged_in";
    private static final String PREF_USERNAME = "logged_in_username";
    private static final String PREF_USER_ID = "logged_in_user_id"; // 添加用户ID的SharedPreferences键
    private MaterialButton btnLogout;
    private SharedPreferences sharedPreferences;
    private DatabaseHelper databaseHelper;
    private TextView tvUserName; // 添加 TextView 引用
    private TextView tvUserId;   // 添加 TextView 引用
    private ShapeableImageView ivUserAvatar;
    private RelativeLayout itemEditProfile;


    public ProfileFragment() {
    }

    /**
     * 从SharedPreferences获取API密钥
     *
     * @return 存储的API密钥，如果不存在则返回空字符串
     */
    public static String getApiKey(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(API_KEY_KEY, "");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        btnLogout = view.findViewById(R.id.btn_logout);
        tvUserName = view.findViewById(R.id.tv_user_name); // 绑定布局中的TextView
        tvUserId = view.findViewById(R.id.tv_user_id);     // 绑定布局中的TextView

        sharedPreferences = requireContext().getSharedPreferences("LoginStatus", Context.MODE_PRIVATE);
        databaseHelper = new DatabaseHelper(requireContext());

        updateUI(); // 初始化时更新UI

        btnLogout.setOnClickListener(v -> {
            if (isLoggedIn()) {
                // 当前是“退出登录”状态，点击后执行退出登录
                logoutUser();
            } else {
                // 当前是“登录/注册”状态，点击后弹出登录注册弹窗
                showLoginRegisterDialog();
            }
        });

        // 查找布局中的API设置项
        itemApiSettings = view.findViewById(R.id.item_api_settings);

        // 设置API设置项的点击事件
        itemApiSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showApiSettingsDialog();
            }
        });

        return view;
    }

    private boolean isLoggedIn() {
        return sharedPreferences.getBoolean(PREF_IS_LOGGED_IN, false);
    }

    private void updateUI() {
        if (isLoggedIn()) {
            // 已登录状态
            btnLogout.setText("退出登录");
            String username = sharedPreferences.getString(PREF_USERNAME, "未知用户");
            long userId = sharedPreferences.getLong(PREF_USER_ID, -1);

            tvUserName.setText(username);
            tvUserId.setText("ID: " + (userId != -1 ? String.valueOf(userId) : "N/A")); // 显示用户ID
        } else {
            // 未登录状态
            btnLogout.setText("登录/注册");
            tvUserName.setText("访客用户");
            tvUserId.setText("ID: 请登录"); // 未登录时显示提示
        }
    }

    private void logoutUser() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREF_IS_LOGGED_IN, false);
        editor.remove(PREF_USERNAME);
        editor.remove(PREF_USER_ID); // 移除保存的用户ID
        editor.apply();
        Toast.makeText(requireContext(), "已退出登录", Toast.LENGTH_SHORT).show();
        updateUI(); // 更新UI
    }

    private void showLoginRegisterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("登录/注册");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final EditText etUsername = new EditText(requireContext());
        etUsername.setHint("请输入账号");
        etUsername.setInputType(InputType.TYPE_CLASS_TEXT);
        layout.addView(etUsername);

        final EditText etPassword = new EditText(requireContext());
        etPassword.setHint("请输入密码");
        etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etPassword);

        builder.setView(layout);

        builder.setPositiveButton("登录", (dialog, which) -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "账号或密码不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            long userId = databaseHelper.checkUser(username, password); // 获取用户ID
            if (userId != -1) { // 如果用户ID不是-1，说明登录成功
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean(PREF_IS_LOGGED_IN, true);
                editor.putString(PREF_USERNAME, username);
                editor.putLong(PREF_USER_ID, userId); // 保存用户ID
                editor.apply();
                Toast.makeText(requireContext(), "登录成功", Toast.LENGTH_SHORT).show();
                updateUI();
                dialog.dismiss();
            } else {
                Toast.makeText(requireContext(), "账号或密码错误", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("注册", (dialog, which) -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "账号或密码不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            if (databaseHelper.registerUser(username, password)) {
                Toast.makeText(requireContext(), "注册成功，请登录", Toast.LENGTH_SHORT).show();
                // 注册成功后，可以考虑自动填充用户名并保持弹窗
                // etUsername.setText(username);
                // etPassword.setText("");
            } else {
                Toast.makeText(requireContext(), "注册失败，该账号可能已存在", Toast.LENGTH_SHORT).show();
            }
        });

        // 取消按钮
        builder.setNeutralButton("取消", (dialog, which) -> dialog.dismiss());

//        AlertDialog dialog = builder.create();
//        dialog.show();
        AlertDialog dialog = builder.show();

        // 调整按钮样式，让“登录”和“注册”更明显
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        Button neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);

        if (positiveButton != null) {
            // 您可以在这里设置按钮的颜色、字体等样式
            // positiveButton.setTextColor(getResources().getColor(R.color.colorPrimary));
        }
        if (negativeButton != null) {
            // negativeButton.setTextColor(getResources().getColor(R.color.colorAccent));
        }
    }

    /**
     * 显示API密钥设置对话框
     */
    private void showApiSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("设置 API 密钥");

        // 设置一个EditText用于输入API密钥
        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setHint("请输入DeepSeek API密钥");

        // 从SharedPreferences中读取当前存储的API密钥，并显示在EditText中
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String currentApiKey = sharedPreferences.getString(API_KEY_KEY, "");
        input.setText(currentApiKey);

        builder.setView(input);

        // 设置“保存”按钮
        builder.setPositiveButton("保存", (dialog, which) -> {
            String newApiKey = input.getText().toString().trim();
            saveApiKey(newApiKey);
            Toast.makeText(requireContext(), "API 密钥已保存", Toast.LENGTH_SHORT).show();
        });

        // 设置“取消”按钮
        builder.setNegativeButton("取消", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    /**
     * 保存API密钥到SharedPreferences
     *
     * @param apiKey 要保存的API密钥
     */
    private void saveApiKey(String apiKey) {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(API_KEY_KEY, apiKey);
        editor.apply(); // 使用apply()异步保存，提高性能
    }
}