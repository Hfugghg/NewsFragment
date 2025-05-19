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

    private TabLayout tabLayout;
    private ViewPager2 viewPagerNews;
    private NewsPagerAdapter newsPagerAdapter;
    private final OkHttpClient okHttpClient = new OkHttpClient();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private List<NewsType> newsTypeList = new ArrayList<>();
    private SharedPreferences sharedPreferences;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // 获取HomeFragment的布局文件
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        tabLayout = view.findViewById(R.id.toubudaohang);
        viewPagerNews = view.findViewById(R.id.view_pager_news);
        sharedPreferences = requireContext().getSharedPreferences("news_read_status", Context.MODE_PRIVATE);

        // 导航栏点击变化
        tabLayout.setSelectedTabIndicatorColor(Color.parseColor("#FFFFFF"));

        // 创建request对象，设置url 获取新闻分类
        Request requestTopic = new Request.Builder().url("http://182.42.154.48:8088/news/servlet/ApiServlet?opr=topic").build();

        // 发起异步请求 获取新闻分类
        okHttpClient.newCall(requestTopic).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace(); // 打印详细的错误堆栈信息
                mainHandler.post(() -> Toast.makeText(requireContext(), "网络请求失败: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    try {
                        // 使用 Fastjson 将 JSON 字符串转换为 ArrayList<NewsType>
                        newsTypeList = JSON.parseArray(responseData, NewsType.class);

                        // 在主线程更新 UI
                        mainHandler.post(() -> {
                            // 创建 PagerAdapter
                            newsPagerAdapter = new NewsPagerAdapter(newsTypeList, requireContext(), sharedPreferences);
                            viewPagerNews.setAdapter(newsPagerAdapter);

                            // 将 TabLayout 和 ViewPager2 关联起来
                            new TabLayoutMediator(tabLayout, viewPagerNews, (tab, position) -> {
                                tab.setText(newsTypeList.get(position).getTname());
                                tab.setTag(newsTypeList.get(position).getTid()); // 设置 tag
                            }).attach();

                            // 可选：设置默认选中第一个 Tab
                            if (!newsTypeList.isEmpty()) {
                                viewPagerNews.setCurrentItem(0, false); // false 表示不平滑滚动
                            }
                        });
                    } catch (Exception e) {
                        mainHandler.post(() -> Toast.makeText(requireContext(), "JSON 解析失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                } else {
                    mainHandler.post(() -> Toast.makeText(requireContext(), "服务器响应失败: " + response.code(), Toast.LENGTH_SHORT).show());
                }
            }
        });

        return view;
    }

    // 为 ViewPager2 创建一个 PagerAdapter
    private static class NewsPagerAdapter extends RecyclerView.Adapter<NewsPagerAdapter.ViewHolder> {
        private final List<NewsType> newsTypeList;
        private final Context context;
        private final SharedPreferences sharedPreferences;
        private final OkHttpClient okHttpClient = new OkHttpClient();
        private final Handler mainHandler = new Handler(Looper.getMainLooper());
        private List<News> currentNewsList = new ArrayList<>();
        private NewsAdapter newsAdapter;

        public NewsPagerAdapter(List<NewsType> newsTypeList, Context context, SharedPreferences sharedPreferences) {
            this.newsTypeList = newsTypeList;
            this.context = context;
            this.sharedPreferences = sharedPreferences;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // 为每个分类创建一个 RecyclerView
            RecyclerView recyclerView = new RecyclerView(parent.getContext());
            recyclerView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
            recyclerView.setLayoutManager(new LinearLayoutManager(parent.getContext()));
            newsAdapter = new NewsAdapter(null, sharedPreferences, parent.getContext()); // 传递 Context
            recyclerView.setAdapter(newsAdapter);
            return new ViewHolder(recyclerView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            // 根据 position 获取对应的新闻分类 ID
            int tid = newsTypeList.get(position).getTid();
            String newsListUrl = "http://182.42.154.48:8088/news/servlet/ApiServlet?opr=newslist&tid=" + tid;
            fetchNewsList(newsListUrl, sharedPreferences, holder.recyclerView, context); // 传递 Context
        }

        @Override
        public int getItemCount() {
            return newsTypeList.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            RecyclerView recyclerView;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                recyclerView = (RecyclerView) itemView;
            }
        }

        // 获取新闻列表的方法
        private void fetchNewsList(String url, SharedPreferences sharedPreferences, RecyclerView recyclerView, Context context) { // 接收 Context 参数
            Request requestNews = new Request.Builder().url(url).build();
            okHttpClient.newCall(requestNews).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    e.printStackTrace();
                    mainHandler.post(() -> Toast.makeText(context, "获取新闻列表失败: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String responseData = response.body().string();
                        try {
                            List<News> newsList = JSON.parseArray(responseData, News.class);

                            // 在设置新闻列表到 Adapter 之前，读取阅读状态
                            loadReadStatus(newsList, sharedPreferences);

                            mainHandler.post(() -> {
                                NewsAdapter adapter = (NewsAdapter) recyclerView.getAdapter();
                                if (adapter != null) {
                                    adapter.setNewsList(newsList);
                                }
                            });
                        } catch (Exception e) {
                            mainHandler.post(() -> Toast.makeText(context, "解析新闻列表 JSON 失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    } else {
                        mainHandler.post(() -> Toast.makeText(context, "获取新闻列表失败，服务器响应: " + response.code(), Toast.LENGTH_SHORT).show());
                    }
                }
            });
        }

        // 加载新闻阅读状态的方法
        private void loadReadStatus(List<News> newsList, SharedPreferences sharedPreferences) {
            for (News news : newsList) {
                boolean isRead = sharedPreferences.getBoolean(String.valueOf(news.getNid()), false);
                news.setRead(isRead);
            }
        }
    }
}