package com.hnjdzy.newsfragment.ui.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hnjdzy.newsfragment.R;
import com.hnjdzy.newsfragment.adapter.NewsAdapter;
import com.hnjdzy.newsfragment.model.News;
import com.hnjdzy.newsfragment.model.NewsDetail;
import com.hnjdzy.newsfragment.model.NewsType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SearchFragment extends Fragment {

    private EditText editTextSearch; // 用于输入搜索关键词的文本框
    private Button buttonSearchSimple; // 执行简单搜索的按钮
    private RecyclerView recyclerViewSearchResults; // 显示搜索结果的列表视图
    private NewsAdapter searchResultsAdapter; // 搜索结果列表的适配器
    private List<News> searchResultList = new ArrayList<>(); // 存储搜索结果的列表
    private CheckBox checkBoxDetailedSearch; // 用于选择是否进行详细搜索的复选框
    private Button buttonSearchDetailed; // 执行详细搜索的按钮
    private TextView textViewNoResults; // 当没有搜索结果时显示的文本视图

    private OkHttpClient client; // 用于发起网络请求的HTTP客户端
    private Gson gson; // 用于JSON序列化和反序列化的工具

    private AtomicInteger processedCount = new AtomicInteger(0); // 统计已处理项的数量，使用原子类确保线程安全
    private AtomicInteger totalCount = new AtomicInteger(0); // 统计总项的数量，使用原子类确保线程安全

    // 在类级别声明SharedPreferences，用于存储和检索应用配置数据
    private SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 填充布局文件，创建视图对象
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        // 初始化界面元素
        editTextSearch = view.findViewById(R.id.editTextSearch); // 搜索输入框
        buttonSearchSimple = view.findViewById(R.id.buttonSearchSimple); // 简单搜索按钮
        recyclerViewSearchResults = view.findViewById(R.id.recyclerViewSearchResults); // 搜索结果列表
        checkBoxDetailedSearch = view.findViewById(R.id.checkBoxDetailedSearch); // 详细搜索复选框
        buttonSearchDetailed = view.findViewById(R.id.buttonSearchDetailed); // 详细搜索按钮
        textViewNoResults = view.findViewById(R.id.textViewNoResults); // 无结果提示文本

        // 设置搜索结果列表的布局管理器为线性布局
        recyclerViewSearchResults.setLayoutManager(new LinearLayoutManager(getContext()));

        // 初始化 SharedPreferences，用于存储新闻阅读状态
        sharedPreferences = requireContext().getSharedPreferences("news_read_status", Context.MODE_PRIVATE);

        // 将 SharedPreferences 实例传递给适配器，并设置给 RecyclerView
        searchResultsAdapter = new NewsAdapter(searchResultList, sharedPreferences, requireContext());
        recyclerViewSearchResults.setAdapter(searchResultsAdapter);

        // 构建 OkHttpClient 实例，设置连接、读取和写入超时时间
        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS) // 连接超时10秒
                .readTimeout(10, TimeUnit.SECONDS)    // 读取超时10秒
                .writeTimeout(10, TimeUnit.SECONDS)   // 写入超时10秒
                .build();
        // 初始化 Gson 实例，用于 JSON 数据解析
        gson = new Gson();

        // 设置简单搜索按钮的点击监听器
        buttonSearchSimple.setOnClickListener(v -> performSimpleSearch());

        // 设置详细搜索复选框的选中状态改变监听器
        checkBoxDetailedSearch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // 根据复选框的选中状态，控制详细搜索按钮的可见性
            buttonSearchDetailed.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        // 设置详细搜索按钮的点击监听器
        buttonSearchDetailed.setOnClickListener(v -> performDetailedSearch());

        // 初始化并设置详细搜索说明文本的点击监听器
        TextView textViewDetailedSearchExplanation = view.findViewById(R.id.textViewDetailedSearchExplanation);
        textViewDetailedSearchExplanation.setOnClickListener(v -> showDetailedSearchExplanation());

        // 初始状态下，无结果提示文本隐藏
        textViewNoResults.setVisibility(View.GONE);

        // 返回创建的视图
        return view;
    }

    // 当 Fragment 重新回到前台时，刷新 Adapter 以更新视图
    @Override
    public void onResume() {
        super.onResume();
        // 当 SearchFragment（搜索 Fragment）恢复时，刷新其 Adapter，以反映任何阅读状态的变化
        // 例如，如果用户在其他地方阅读了某项内容，返回搜索界面时，该项的阅读状态应立即更新
        if (searchResultsAdapter != null) {
            searchResultsAdapter.notifyDataSetChanged(); // 通知 Adapter 数据已改变，要求其重新绘制列表
        }
    }

    /**
     * 显示“未找到相关内容”的消息，并隐藏搜索结果列表。
     */
    private void showNoResultsMessage() {
        recyclerViewSearchResults.setVisibility(View.GONE); // 隐藏显示搜索结果的 RecyclerView
        textViewNoResults.setVisibility(View.VISIBLE);     // 显示提示消息的 TextView
        textViewNoResults.setText("未找到相关内容");       // 设置提示文本
    }

    /**
     * 隐藏“未找到相关内容”的消息，并显示搜索结果列表。
     */
    private void hideNoResultsMessage() {
        recyclerViewSearchResults.setVisibility(View.VISIBLE); // 显示显示搜索结果的 RecyclerView
        textViewNoResults.setVisibility(View.GONE);          // 隐藏提示消息的 TextView
    }

    /**
     * 更新搜索进度显示。
     * 这个方法会在后台搜索过程中，更新显示当前已处理的数量和总数。
     */
    private void updateProgressDisplay() {
        // 确保 Fragment 依附于 Activity 并且 Activity 未被销毁
        if (getActivity() != null) {
            // 在 UI 线程上运行，因为 UI 更新必须在主线程进行
            getActivity().runOnUiThread(() -> {
                int current = processedCount.get(); // 获取当前已处理的数量
                int total = totalCount.get();       // 获取总共需要处理的数量

                if (total > 0) {
                    // 当有总数时，显示具体的进度信息
                    textViewNoResults.setText("正在查找... (" + current + "/" + total + ")");
                    textViewNoResults.setVisibility(View.VISIBLE); // 显示进度文本
                    recyclerViewSearchResults.setVisibility(View.GONE); // 隐藏结果列表
                } else {
                    // 当总数未知或为零时，只显示“正在查找...”
                    textViewNoResults.setText("正在查找...");
                    textViewNoResults.setVisibility(View.VISIBLE); // 显示进度文本
                    recyclerViewSearchResults.setVisibility(View.GONE); // 隐藏结果列表
                }
            });
        }
    }

    private void performSimpleSearch() {
        // 获取搜索框中的关键词并去除首尾空格
        String keyword = editTextSearch.getText().toString().trim();
        // 如果关键词为空，则提示用户输入并返回
        if (TextUtils.isEmpty(keyword)) {
            android.widget.Toast.makeText(getContext(), "请输入搜索关键词", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        // 清空之前的搜索结果列表
        searchResultList.clear();
        // 确保适配器在此处得到刷新，以清空UI上显示的结果
        searchResultsAdapter.notifyDataSetChanged();
        // 重置已处理和总计的计数器
        processedCount.set(0);
        totalCount.set(0);
        // 清空无结果提示文本
        textViewNoResults.setText("");
        // 更新进度显示
        updateProgressDisplay();
        // 调用方法获取新闻类型并执行搜索，'false'表示执行简单搜索
        fetchNewsTypesAndSearch(keyword, false);
    }

    private void performDetailedSearch() {
        // 获取搜索框中的关键词并去除首尾空格
        String keyword = editTextSearch.getText().toString().trim();
        // 如果关键词为空，则提示用户输入并返回
        if (TextUtils.isEmpty(keyword)) {
            android.widget.Toast.makeText(getContext(), "请输入搜索关键词", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        // 清空之前的搜索结果列表
        searchResultList.clear();
        // 确保适配器在此处得到刷新，以清空UI上显示的结果
        searchResultsAdapter.notifyDataSetChanged();
        // 重置已处理和总计的计数器
        processedCount.set(0);
        totalCount.set(0);
        // 清空无结果提示文本
        textViewNoResults.setText("");
        // 更新进度显示
        updateProgressDisplay();
        // 调用方法获取新闻类型并执行搜索，'true'表示执行详细搜索
        fetchNewsTypesAndSearch(keyword, true);
    }

    private void fetchNewsTypesAndSearch(String keyword, boolean isDetailed) {
        // 定义获取新闻分类的URL
        String newsTypeUrl = "http://182.42.154.48:8088/news/servlet/ApiServlet?opr=topic";
        // 构建网络请求
        Request request = new Request.Builder().url(newsTypeUrl).build();

        // 发起异步网络请求
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // 请求失败时的回调
                if (getActivity() != null) {
                    // 确保在主线程更新UI
                    getActivity().runOnUiThread(() -> {
                        // 显示获取新闻分类失败的Toast提示
                        android.widget.Toast.makeText(getContext(), "获取新闻分类失败", android.widget.Toast.LENGTH_SHORT).show();
                        // 显示无结果消息
                        showNoResultsMessage();
                    });
                    // 打印错误日志
                    Log.e("SearchFragment", "获取新闻分类失败: " + e.getMessage());
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                // 请求成功时的回调
                if (response.isSuccessful() && response.body() != null) {
                    // 获取响应数据
                    String responseData = response.body().string();
                    // 使用Gson将JSON数据解析为新闻分类列表
                    List<NewsType> newsTypeList = gson.fromJson(responseData, new TypeToken<List<NewsType>>() {}.getType());
                    // 检查新闻分类列表是否为空
                    if (newsTypeList != null && !newsTypeList.isEmpty()) {
                        // 设置总的新闻分类数量
                        totalCount.set(newsTypeList.size());
                        // 更新进度显示
                        updateProgressDisplay();

                        // 创建一个CountDownLatch，用于等待所有新闻分类下的搜索完成
                        CountDownLatch latch = new CountDownLatch(newsTypeList.size());

                        // 遍历每个新闻分类
                        for (NewsType newsType : newsTypeList) {
                            // 根据isDetailed参数调用不同的搜索方法
                            if (isDetailed) {
                                // 搜索新闻列表并获取详细信息（如果需要）并进行过滤
                                fetchNewsListAndDetailsAndFilter(newsType.getTid(), keyword, latch);
                            } else {
                                // 搜索新闻列表并进行过滤
                                fetchNewsListAndFilter(newsType.getTid(), keyword, latch);
                            }
                        }

                        // 启动一个新线程等待所有搜索任务完成
                        new Thread(() -> {
                            try {
                                // 等待所有CountDownLatch计数器归零
                                latch.await();
                                if (getActivity() != null) {
                                    // 在主线程更新UI
                                    getActivity().runOnUiThread(() -> {
                                        // 如果搜索结果列表为空，则显示无结果消息
                                        if (searchResultList.isEmpty()) {
                                            showNoResultsMessage();
                                        } else {
                                            // 否则隐藏无结果消息，并刷新适配器以确保阅读状态正确
                                            hideNoResultsMessage();
                                            searchResultsAdapter.notifyDataSetChanged();
                                        }
                                    });
                                }
                            } catch (InterruptedException e) {
                                // CountDownLatch等待被中断时的处理
                                Log.e("SearchFragment", "CountDownLatch await interrupted: " + e.getMessage());
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        showNoResultsMessage();
                                    });
                                }
                            }
                        }).start();

                    } else {
                        // 如果新闻分类数据为空或获取失败
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                android.widget.Toast.makeText(getContext(), "获取新闻分类数据失败或为空", android.widget.Toast.LENGTH_SHORT).show();
                                showNoResultsMessage();
                            });
                            Log.e("SearchFragment", "获取新闻分类数据失败或为空: " + responseData);
                        }
                    }
                } else {
                    // 如果响应不成功或响应体为空
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            android.widget.Toast.makeText(getContext(), "获取新闻分类响应失败", android.widget.Toast.LENGTH_SHORT).show();
                            showNoResultsMessage();
                        });
                        Log.e("SearchFragment", "获取新闻分类响应失败: " + response);
                    }
                }
            }
        });
    }

    private void fetchNewsListAndFilter(int tid, String keyword, CountDownLatch latch) {
        // 构建获取新闻列表的 URL
        String newsListUrl = "http://182.42.154.48:8088/news/servlet/ApiServlet?opr=newslist&tid=" + tid;
        // 构建 OkHttp 请求对象
        Request request = new Request.Builder().url(newsListUrl).build();

        // 使用 OkHttpClient 发起异步网络请求
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // 网络请求失败时的回调
                Log.e("SearchFragment", "获取新闻列表失败 (tid=" + tid + "): " + e.getMessage());
                // 增加已处理请求计数
                processedCount.incrementAndGet();
                // 更新进度显示
                updateProgressDisplay();
                // 减少 CountDownLatch 的计数，表示当前请求已完成
                latch.countDown();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                // 网络请求成功时的回调
                try {
                    // 检查响应是否成功且响应体不为空
                    if (response.isSuccessful() && response.body() != null) {
                        // 获取响应数据为字符串
                        String responseData = response.body().string();
                        // 使用 Gson 将 JSON 字符串解析为 News 对象的列表
                        List<News> newsList = gson.fromJson(responseData, new TypeToken<List<News>>() {}.getType());
                        // 检查新闻列表是否为空
                        if (newsList != null) {
                            // 创建一个用于存储过滤后新闻的列表
                            List<News> filteredList = new ArrayList<>();
                            // 遍历新闻列表进行过滤
                            for (News news : newsList) {
                                // 判断新闻标题或摘要是否包含关键词
                                if ((news.getNtitle() != null && news.getNtitle().contains(keyword)) ||
                                        (news.getNsummary() != null && news.getNsummary().contains(keyword))) {
                                    // 这里需要检查新闻的阅读状态。
                                    // 当调用 notifyDataSetChanged() 时，适配器的 onBindViewHolder 会处理这个。
                                    filteredList.add(news);
                                }
                            }
                            // 确保 Fragment 仍然依附于 Activity
                            if (getActivity() != null) {
                                // 在 UI 线程更新数据
                                getActivity().runOnUiThread(() -> {
                                    // 如果过滤后的列表不为空，则添加到搜索结果列表中
                                    if (!filteredList.isEmpty()) {
                                        searchResultList.addAll(filteredList);
                                        // 这里不需要调用 notifyDataSetChanged，它会在所有请求完成后统一调用
                                    }
                                });
                            }
                        } else {
                            // 响应数据解析失败的情况
                            Log.e("SearchFragment", "获取新闻列表数据失败 (tid=" + tid + "): " + response);
                        }
                    }
                } finally {
                    // 无论请求成功或失败，都在 finally 块中执行以下操作，确保 CountDownLatch 总是被减少
                    processedCount.incrementAndGet(); // 增加已处理请求计数
                    updateProgressDisplay(); // 更新进度显示
                    latch.countDown(); // 减少 CountDownLatch 的计数，表示当前请求已完成
                }
            }
        });
    }

    private void fetchNewsListAndDetailsAndFilter(int tid, String keyword, CountDownLatch parentLatch) {
        // 构建获取新闻列表的URL
        String newsListUrl = "http://182.42.154.48:8088/news/servlet/ApiServlet?opr=newslist&tid=" + tid;
        // 构建网络请求
        Request request = new Request.Builder().url(newsListUrl).build();

        // 发起异步网络请求
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // 请求失败时的回调
                Log.e("SearchFragment", "获取新闻列表失败 (详细搜索, tid=" + tid + "): " + e.getMessage());
                // 增加已处理计数
                processedCount.incrementAndGet();
                // 更新进度显示
                updateProgressDisplay();
                // 通知父级CountDownLatch，表示当前任务完成
                parentLatch.countDown();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                // 请求成功时的回调
                try {
                    // 检查响应是否成功且响应体不为空
                    if (response.isSuccessful() && response.body() != null) {
                        // 获取响应数据为字符串
                        String responseData = response.body().string();
                        // 使用Gson将JSON数据解析为News列表
                        List<News> newsList = gson.fromJson(responseData, new TypeToken<List<News>>() {}.getType());
                        // 检查新闻列表是否不为空
                        if (newsList != null && !newsList.isEmpty()) {
                            // 创建一个子CountDownLatch，用于等待所有新闻详情获取完成
                            CountDownLatch childLatch = new CountDownLatch(newsList.size());
                            // 遍历新闻列表，逐个获取新闻详情并进行过滤
                            for (News news : newsList) {
                                fetchNewsDetailsAndFilter(news.getNid(), keyword, childLatch);
                            }

                            // 启动一个新线程，等待所有新闻详情获取任务完成
                            new Thread(() -> {
                                try {
                                    // 等待子CountDownLatch计数归零，即所有新闻详情都已处理
                                    childLatch.await();
                                } catch (InterruptedException e) {
                                    Log.e("SearchFragment", "子CountDownLatch等待被中断: " + e.getMessage());
                                } finally {
                                    // 不论成功或失败，通知父级CountDownLatch当前任务完成
                                    parentLatch.countDown();
                                }
                            }).start();

                        } else {
                            // 新闻列表数据为空或解析失败
                            Log.e("SearchFragment", "获取新闻列表数据失败 (详细搜索, tid=" + tid + "): " + response);
                            processedCount.incrementAndGet();
                            updateProgressDisplay();
                            parentLatch.countDown();
                        }
                    } else {
                        // 响应不成功或响应体为空
                        Log.e("SearchFragment", "获取新闻列表响应失败 (详细搜索, tid=" + tid + "): " + response);
                        processedCount.incrementAndGet();
                        updateProgressDisplay();
                        parentLatch.countDown();
                    }
                } finally {
                    // 这里不需要额外的parentLatch.countDown()，因为它已经在内部的成功或失败路径中被调用。
                    // 注释掉此行以避免重复递减。
                }
            }
        });
    }

    private void fetchNewsDetailsAndFilter(int nid, String keyword, CountDownLatch latch) {
        // 构建获取新闻详情的URL，其中包含新闻ID (nid)
        String newsDetailsUrl = "http://182.42.154.48:8088/news/servlet/ApiServlet?opr=readnews&nid=" + nid;
        // 创建一个HTTP请求
        Request request = new Request.Builder().url(newsDetailsUrl).build();

        // 发起异步HTTP请求
        client.newCall(request).enqueue(new Callback() {
            @Override
            // 请求失败时的回调方法
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // 记录错误日志
                Log.e("SearchFragment", "获取新闻详情失败 (nid=" + nid + "): " + e.getMessage());
                // 请求完成后，倒计时器减一
                latch.countDown();
            }

            @Override
            // 请求成功并收到响应时的回调方法
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    // 检查响应是否成功且响应体不为空
                    if (response.isSuccessful() && response.body() != null) {
                        // 获取响应数据为字符串
                        String responseData = response.body().string();
                        // 使用Gson库将JSON字符串转换为NewsDetail对象
                        NewsDetail newsDetail = gson.fromJson(responseData, NewsDetail.class);
                        // 如果成功解析出新闻详情
                        if (newsDetail != null) {
                            // 检查新闻标题、摘要或内容是否包含搜索关键词
                            if ((newsDetail.getNtitle() != null && newsDetail.getNtitle().contains(keyword)) ||
                                    (newsDetail.getNsummary() != null && newsDetail.getNsummary().contains(keyword)) ||
                                    (newsDetail.getNcontent() != null && newsDetail.getNcontent().contains(keyword))) {
                                // 确保Fragment已附加到Activity
                                if (getActivity() != null) {
                                    // 在UI线程上更新UI，因为网络回调不在主线程
                                    getActivity().runOnUiThread(() -> {
                                        boolean alreadyExists = false;
                                        // 遍历已有的搜索结果列表，检查新闻是否已存在，避免重复添加
                                        for (News existingNews : searchResultList) {
                                            if (existingNews.getNid() == newsDetail.getNid()) {
                                                alreadyExists = true;
                                                break;
                                            }
                                        }
                                        // 如果新闻不存在于列表中，则添加
                                        if (!alreadyExists) {
                                            // 创建一个新的News对象
                                            News newsItem = new News();
                                            // 设置新闻的各个属性
                                            newsItem.setNid(newsDetail.getNid());
                                            newsItem.setNtitle(newsDetail.getNtitle());
                                            newsItem.setNauthor(newsDetail.getNauthor());
                                            newsItem.setNcreatedate(newsDetail.getNcreatedate());
                                            newsItem.setNpicpath(newsDetail.getNpicpath());
                                            newsItem.setNsummary(newsDetail.getNsummary());
                                            newsItem.setNtid(newsDetail.getNtid());
                                            newsItem.setNtname(newsDetail.getNtname());
                                            // 将新闻添加到搜索结果列表
                                            searchResultList.add(newsItem);
                                            // 无需在这里调用notifyDataSetChanged，因为所有请求完成后会统一刷新UI
                                        }
                                    });
                                }
                            }
                        } else {
                            // 如果新闻详情数据解析失败，记录日志
                            Log.e("SearchFragment", "获取新闻详情数据失败 (nid=" + nid + "): " + response);
                        }
                    }
                } finally {
                    // 无论成功或失败，确保倒计时器减一，这对于CountDownLatch的正确工作至关重要
                    latch.countDown();
                }
            }
        });
    }

    private void showDetailedSearchExplanation() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.detailed_search)
                .setMessage(R.string.detailed_search_explanation)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}