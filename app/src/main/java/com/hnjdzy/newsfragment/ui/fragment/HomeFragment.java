package com.hnjdzy.newsfragment.ui.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.recyclerview.widget.RecyclerView; // 别忘了这个 import

import com.alibaba.fastjson.JSON;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.hnjdzy.newsfragment.R;
import com.hnjdzy.newsfragment.adapter.NewsAdapter;
import com.hnjdzy.newsfragment.model.News;
import com.hnjdzy.newsfragment.model.NewsType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HomeFragment extends Fragment {

    private TabLayout tabLayout; // // 声明一个 TabLayout 对象，用于显示新闻分类标签
    private ViewPager2 viewPagerNews; // // 声明一个 ViewPager2 对象，用于滑动显示不同分类的新闻
    private NewsPagerAdapter newsPagerAdapter; // // 声明一个 NewsPagerAdapter 对象，用于 ViewPager2 的适配器
    private final OkHttpClient okHttpClient = new OkHttpClient(); // // 声明一个 OkHttpClient 对象，用于发起网络请求
    private final Handler mainHandler = new Handler(Looper.getMainLooper()); // // 声明一个 Handler 对象，用于在主线程更新 UI
    private List<NewsType> newsTypeList = new ArrayList<>(); // // 声明一个 List，用于存储新闻分类数据
    private SharedPreferences sharedPreferences; // // 声明一个 SharedPreferences 对象，用于存储和读取新闻阅读状态

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // 获取HomeFragment的布局文件
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        tabLayout = view.findViewById(R.id.toubudaohang); // // 初始化 tabLayout
        viewPagerNews = view.findViewById(R.id.view_pager_news); // // 初始化 viewPagerNews
        sharedPreferences = requireContext().getSharedPreferences("news_read_status", Context.MODE_PRIVATE); // // 初始化 sharedPreferences，用于保存新闻阅读状态

        // 导航栏点击变化
        tabLayout.setSelectedTabIndicatorColor(Color.parseColor("#FFFFFF")); // // 设置 TabLayout 选中指示器的颜色为白色

        // 创建request对象，设置url 获取新闻分类
        Request requestTopic = new Request.Builder().url("http://182.42.154.48:8088/news/servlet/ApiServlet?opr=topic").build();

        // 发起异步请求 获取新闻分类
        okHttpClient.newCall(requestTopic).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace(); // 打印详细的错误堆栈信息
                mainHandler.post(() -> Toast.makeText(requireContext(), "网络请求失败: " + e.getMessage(), Toast.LENGTH_LONG).show()); // // 在主线程显示网络请求失败的 Toast 提示
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) { // // 判断网络响应是否成功
                    String responseData = response.body().string(); // // 获取响应体数据
                    try {
                        // 使用 Fastjson 将 JSON 字符串转换为 ArrayList<NewsType>
                        newsTypeList = JSON.parseArray(responseData, NewsType.class);

                        // 在主线程更新 UI
                        mainHandler.post(() -> {
                            // 创建 PagerAdapter
                            newsPagerAdapter = new NewsPagerAdapter(newsTypeList, requireContext(), sharedPreferences); // // 创建 NewsPagerAdapter 实例
                            viewPagerNews.setAdapter(newsPagerAdapter); // // 为 ViewPager2 设置适配器

                            // 将 TabLayout 和 ViewPager2 关联起来
                            new TabLayoutMediator(tabLayout, viewPagerNews, (tab, position) -> {
                                tab.setText(newsTypeList.get(position).getTname()); // // 设置 Tab 的文本为新闻分类名称
                                tab.setTag(newsTypeList.get(position).getTid()); // 设置 tag // // 设置 Tab 的 Tag 为新闻分类 ID
                            }).attach(); // // 将 TabLayout 和 ViewPager2 关联起来

                            // 可选：设置默认选中第一个 Tab
                            if (!newsTypeList.isEmpty()) { // // 如果新闻分类列表不为空
                                viewPagerNews.setCurrentItem(0, false); // false 表示不平滑滚动 // // 设置 ViewPager2 默认选中第一个 Tab
                            }
                        });
                    } catch (Exception e) {
                        mainHandler.post(() -> Toast.makeText(requireContext(), "JSON 解析失败: " + e.getMessage(), Toast.LENGTH_SHORT).show()); // // 在主线程显示 JSON 解析失败的 Toast 提示
                    }
                } else {
                    mainHandler.post(() -> Toast.makeText(requireContext(), "服务器响应失败: " + response.code(), Toast.LENGTH_SHORT).show()); // // 在主线程显示服务器响应失败的 Toast 提示
                }
            }
        });

        return view;
    }

    // 为 ViewPager2 创建一个 PagerAdapter
    private static class NewsPagerAdapter extends RecyclerView.Adapter<NewsPagerAdapter.ViewHolder> {
        private final List<NewsType> newsTypeList; // // 存储新闻分类数据
        private final Context context; // // 上下文对象
        private final SharedPreferences sharedPreferences; // // SharedPreferences 对象，用于保存新闻阅读状态
        private final OkHttpClient okHttpClient = new OkHttpClient(); // // OkHttpClient 对象，用于网络请求
        private final Handler mainHandler = new Handler(Looper.getMainLooper()); // // Handler 对象，用于在主线程更新 UI
        private List<News> currentNewsList = new ArrayList<>(); // // 当前显示的新闻列表
        private NewsAdapter newsAdapter; // // 新闻列表的适配器

        public NewsPagerAdapter(List<NewsType> newsTypeList, Context context, SharedPreferences sharedPreferences) {
            this.newsTypeList = newsTypeList;
            this.context = context;
            this.sharedPreferences = sharedPreferences;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // 为每个分类创建一个 RecyclerView
            RecyclerView recyclerView = new RecyclerView(parent.getContext()); // // 创建一个新的 RecyclerView
            recyclerView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
            recyclerView.setLayoutManager(new LinearLayoutManager(parent.getContext())); // // 设置 RecyclerView 的布局管理器为线性布局
            newsAdapter = new NewsAdapter(null, sharedPreferences, parent.getContext()); // 传递 Context // // 创建 NewsAdapter 实例
            recyclerView.setAdapter(newsAdapter); // // 为 RecyclerView 设置适配器
            return new ViewHolder(recyclerView); // // 返回 ViewHolder
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            // 根据 position 获取对应的新闻分类 ID
            int tid = newsTypeList.get(position).getTid(); // // 获取当前位置的新闻分类 ID
            String newsListUrl = "http://182.42.154.48:8088/news/servlet/ApiServlet?opr=newslist&tid=" + tid; // // 构建获取新闻列表的 URL
            fetchNewsList(newsListUrl, sharedPreferences, holder.recyclerView, context); // 传递 Context // // 调用方法获取新闻列表
        }

        @Override
        public int getItemCount() {
            return newsTypeList.size(); // // 返回新闻分类的数量
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            RecyclerView recyclerView; // // 声明一个 RecyclerView 对象

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                recyclerView = (RecyclerView) itemView; // // 初始化 recyclerView
            }
        }

        // 获取新闻列表的方法
        private void fetchNewsList(String url, SharedPreferences sharedPreferences, RecyclerView recyclerView, Context context) { // 接收 Context 参数
            Request requestNews = new Request.Builder().url(url).build(); // // 创建请求对象
            okHttpClient.newCall(requestNews).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    e.printStackTrace();
                    mainHandler.post(() -> Toast.makeText(context, "获取新闻列表失败: " + e.getMessage(), Toast.LENGTH_LONG).show()); // // 在主线程显示获取新闻列表失败的 Toast 提示
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful()) { // // 判断网络响应是否成功
                        String responseData = response.body().string(); // // 获取响应体数据
                        try {
                            List<News> newsList = JSON.parseArray(responseData, News.class); // // 使用 Fastjson 将 JSON 字符串转换为 List<News>

                            // 在设置新闻列表到 Adapter 之前，读取阅读状态
                            loadReadStatus(newsList, sharedPreferences); // // 加载新闻的阅读状态

                            mainHandler.post(() -> {
                                NewsAdapter adapter = (NewsAdapter) recyclerView.getAdapter(); // // 获取 RecyclerView 的适配器
                                if (adapter != null) {
                                    adapter.setNewsList(newsList); // // 更新适配器中的新闻列表
                                }
                            });
                        } catch (Exception e) {
                            mainHandler.post(() -> Toast.makeText(context, "解析新闻列表 JSON 失败: " + e.getMessage(), Toast.LENGTH_SHORT).show()); // // 在主线程显示 JSON 解析失败的 Toast 提示
                        }
                    } else {
                        mainHandler.post(() -> Toast.makeText(context, "获取新闻列表失败，服务器响应: " + response.code(), Toast.LENGTH_SHORT).show()); // // 在主线程显示服务器响应失败的 Toast 提示
                    }
                }
            });
        }

        // 加载新闻阅读状态的方法
        private void loadReadStatus(List<News> newsList, SharedPreferences sharedPreferences) {
            for (News news : newsList) { // // 遍历新闻列表
                boolean isRead = sharedPreferences.getBoolean(String.valueOf(news.getNid()), false); // // 从 SharedPreferences 中获取新闻的阅读状态
                news.setRead(isRead); // // 设置新闻的阅读状态
            }
        }
    }
}