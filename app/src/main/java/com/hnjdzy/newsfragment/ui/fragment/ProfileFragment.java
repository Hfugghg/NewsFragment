package com.hnjdzy.newsfragment.ui.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.hnjdzy.newsfragment.R; // 确保R文件路径正确

public class ProfileFragment extends Fragment {

    private static final String PREFS_NAME = "NewsAppPrefs";
    private static final String API_KEY_KEY = "api_key";

    private RelativeLayout itemApiSettings;

    public ProfileFragment() {
        // Required empty public constructor
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