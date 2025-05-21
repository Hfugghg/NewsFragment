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

    private EditText editTextSearch;
    private Button buttonSearchSimple;
    private RecyclerView recyclerViewSearchResults;
    private NewsAdapter searchResultsAdapter;
    private List<News> searchResultList = new ArrayList<>();
    private CheckBox checkBoxDetailedSearch;
    private Button buttonSearchDetailed;
    private TextView textViewNoResults;

    private OkHttpClient client;
    private Gson gson;

    private AtomicInteger processedCount = new AtomicInteger(0);
    private AtomicInteger totalCount = new AtomicInteger(0);

    // Declare SharedPreferences at the class level
    private SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);
        editTextSearch = view.findViewById(R.id.editTextSearch);
        buttonSearchSimple = view.findViewById(R.id.buttonSearchSimple);
        recyclerViewSearchResults = view.findViewById(R.id.recyclerViewSearchResults);
        checkBoxDetailedSearch = view.findViewById(R.id.checkBoxDetailedSearch);
        buttonSearchDetailed = view.findViewById(R.id.buttonSearchDetailed);
        textViewNoResults = view.findViewById(R.id.textViewNoResults);

        recyclerViewSearchResults.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize sharedPreferences here
        sharedPreferences = requireContext().getSharedPreferences("news_read_status", Context.MODE_PRIVATE);

        // Pass the sharedPreferences instance to the adapter
        searchResultsAdapter = new NewsAdapter(searchResultList, sharedPreferences, requireContext());
        recyclerViewSearchResults.setAdapter(searchResultsAdapter);

        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
        gson = new Gson();

        buttonSearchSimple.setOnClickListener(v -> performSimpleSearch());

        checkBoxDetailedSearch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            buttonSearchDetailed.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        buttonSearchDetailed.setOnClickListener(v -> performDetailedSearch());

        TextView textViewDetailedSearchExplanation = view.findViewById(R.id.textViewDetailedSearchExplanation);
        textViewDetailedSearchExplanation.setOnClickListener(v -> showDetailedSearchExplanation());

        textViewNoResults.setVisibility(View.GONE);

        return view;
    }

    // Add onResume to refresh the adapter when the fragment comes back to the foreground
    @Override
    public void onResume() {
        super.onResume();
        // When the SearchFragment resumes, refresh the adapter to reflect any read status changes
        if (searchResultsAdapter != null) {
            searchResultsAdapter.notifyDataSetChanged();
        }
    }

    private void showNoResultsMessage() {
        recyclerViewSearchResults.setVisibility(View.GONE);
        textViewNoResults.setVisibility(View.VISIBLE);
        textViewNoResults.setText("未找到相关内容");
    }

    private void hideNoResultsMessage() {
        recyclerViewSearchResults.setVisibility(View.VISIBLE);
        textViewNoResults.setVisibility(View.GONE);
    }

    private void updateProgressDisplay() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                int current = processedCount.get();
                int total = totalCount.get();
                if (total > 0) {
                    textViewNoResults.setText("正在查找... (" + current + "/" + total + ")");
                    textViewNoResults.setVisibility(View.VISIBLE);
                    recyclerViewSearchResults.setVisibility(View.GONE);
                } else {
                    textViewNoResults.setText("正在查找...");
                    textViewNoResults.setVisibility(View.VISIBLE);
                    recyclerViewSearchResults.setVisibility(View.GONE);
                }
            });
        }
    }

    private void performSimpleSearch() {
        String keyword = editTextSearch.getText().toString().trim();
        if (TextUtils.isEmpty(keyword)) {
            android.widget.Toast.makeText(getContext(), "请输入搜索关键词", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        searchResultList.clear();
        // Make sure the adapter is refreshed here
        searchResultsAdapter.notifyDataSetChanged();
        processedCount.set(0);
        totalCount.set(0);
        textViewNoResults.setText("");
        updateProgressDisplay();
        fetchNewsTypesAndSearch(keyword, false);
    }

    private void performDetailedSearch() {
        String keyword = editTextSearch.getText().toString().trim();
        if (TextUtils.isEmpty(keyword)) {
            android.widget.Toast.makeText(getContext(), "请输入搜索关键词", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        searchResultList.clear();
        // Make sure the adapter is refreshed here
        searchResultsAdapter.notifyDataSetChanged();
        processedCount.set(0);
        totalCount.set(0);
        textViewNoResults.setText("");
        updateProgressDisplay();
        fetchNewsTypesAndSearch(keyword, true);
    }

    private void fetchNewsTypesAndSearch(String keyword, boolean isDetailed) {
        String newsTypeUrl = "http://182.42.154.48:8088/news/servlet/ApiServlet?opr=topic";
        Request request = new Request.Builder().url(newsTypeUrl).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        android.widget.Toast.makeText(getContext(), "获取新闻分类失败", android.widget.Toast.LENGTH_SHORT).show();
                        showNoResultsMessage();
                    });
                    Log.e("SearchFragment", "获取新闻分类失败: " + e.getMessage());
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    List<NewsType> newsTypeList = gson.fromJson(responseData, new TypeToken<List<NewsType>>() {}.getType());
                    if (newsTypeList != null && !newsTypeList.isEmpty()) {
                        totalCount.set(newsTypeList.size());
                        updateProgressDisplay();

                        CountDownLatch latch = new CountDownLatch(newsTypeList.size());

                        for (NewsType newsType : newsTypeList) {
                            if (isDetailed) {
                                fetchNewsListAndDetailsAndFilter(newsType.getTid(), keyword, latch);
                            } else {
                                fetchNewsListAndFilter(newsType.getTid(), keyword, latch);
                            }
                        }

                        new Thread(() -> {
                            try {
                                latch.await();
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        if (searchResultList.isEmpty()) {
                                            showNoResultsMessage();
                                        } else {
                                            hideNoResultsMessage();
                                            // After all searches are complete, refresh the adapter to ensure read status is correct
                                            searchResultsAdapter.notifyDataSetChanged();
                                        }
                                    });
                                }
                            } catch (InterruptedException e) {
                                Log.e("SearchFragment", "CountDownLatch await interrupted: " + e.getMessage());
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        showNoResultsMessage();
                                    });
                                }
                            }
                        }).start();

                    } else {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                android.widget.Toast.makeText(getContext(), "获取新闻分类数据失败或为空", android.widget.Toast.LENGTH_SHORT).show();
                                showNoResultsMessage();
                            });
                            Log.e("SearchFragment", "获取新闻分类数据失败或为空: " + responseData);
                        }
                    }
                } else {
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
        String newsListUrl = "http://182.42.154.48:8088/news/servlet/ApiServlet?opr=newslist&tid=" + tid;
        Request request = new Request.Builder().url(newsListUrl).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("SearchFragment", "获取新闻列表失败 (tid=" + tid + "): " + e.getMessage());
                processedCount.incrementAndGet();
                updateProgressDisplay();
                latch.countDown();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseData = response.body().string();
                        List<News> newsList = gson.fromJson(responseData, new TypeToken<List<News>>() {}.getType());
                        if (newsList != null) {
                            List<News> filteredList = new ArrayList<>();
                            for (News news : newsList) {
                                if ((news.getNtitle() != null && news.getNtitle().contains(keyword)) ||
                                        (news.getNsummary() != null && news.getNsummary().contains(keyword))) {
                                    // You need to check the read status of the news item here
                                    // The adapter's onBindViewHolder will handle this when notifyDataSetChanged() is called
                                    filteredList.add(news);
                                }
                            }
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    if (!filteredList.isEmpty()) {
                                        searchResultList.addAll(filteredList);
                                        // No need to call notifyDataSetChanged here, it will be called once all requests are done
                                    }
                                });
                            }
                        } else {
                            Log.e("SearchFragment", "获取新闻列表数据失败 (tid=" + tid + "): " + response);
                        }
                    }
                } finally {
                    processedCount.incrementAndGet();
                    updateProgressDisplay();
                    latch.countDown();
                }
            }
        });
    }

    private void fetchNewsListAndDetailsAndFilter(int tid, String keyword, CountDownLatch parentLatch) {
        String newsListUrl = "http://182.42.154.48:8088/news/servlet/ApiServlet?opr=newslist&tid=" + tid;
        Request request = new Request.Builder().url(newsListUrl).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("SearchFragment", "获取新闻列表失败 (详细搜索, tid=" + tid + "): " + e.getMessage());
                processedCount.incrementAndGet();
                updateProgressDisplay();
                parentLatch.countDown();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseData = response.body().string();
                        List<News> newsList = gson.fromJson(responseData, new TypeToken<List<News>>() {}.getType());
                        if (newsList != null && !newsList.isEmpty()) {
                            CountDownLatch childLatch = new CountDownLatch(newsList.size());
                            for (News news : newsList) {
                                fetchNewsDetailsAndFilter(news.getNid(), keyword, childLatch);
                            }

                            new Thread(() -> {
                                try {
                                    childLatch.await();
                                } catch (InterruptedException e) {
                                    Log.e("SearchFragment", "Child CountDownLatch await interrupted: " + e.getMessage());
                                } finally {
                                    parentLatch.countDown();
                                }
                            }).start();

                        } else {
                            Log.e("SearchFragment", "获取新闻列表数据失败 (详细搜索, tid=" + tid + "): " + response);
                            processedCount.incrementAndGet();
                            updateProgressDisplay();
                            parentLatch.countDown();
                        }
                    } else {
                        Log.e("SearchFragment", "获取新闻列表响应失败 (详细搜索, tid=" + tid + "): " + response);
                        processedCount.incrementAndGet();
                        updateProgressDisplay();
                        parentLatch.countDown();
                    }
                } finally {
                    // Handled by parentLatch.countDown()
                }
            }
        });
    }

    private void fetchNewsDetailsAndFilter(int nid, String keyword, CountDownLatch latch) {
        String newsDetailsUrl = "http://182.42.154.48:8088/news/servlet/ApiServlet?opr=readnews&nid=" + nid;
        Request request = new Request.Builder().url(newsDetailsUrl).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("SearchFragment", "获取新闻详情失败 (nid=" + nid + "): " + e.getMessage());
                latch.countDown();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseData = response.body().string();
                        NewsDetail newsDetail = gson.fromJson(responseData, NewsDetail.class);
                        if (newsDetail != null) {
                            if ((newsDetail.getNtitle() != null && newsDetail.getNtitle().contains(keyword)) ||
                                    (newsDetail.getNsummary() != null && newsDetail.getNsummary().contains(keyword)) ||
                                    (newsDetail.getNcontent() != null && newsDetail.getNcontent().contains(keyword))) {
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        boolean alreadyExists = false;
                                        for (News existingNews : searchResultList) {
                                            if (existingNews.getNid() == newsDetail.getNid()) {
                                                alreadyExists = true;
                                                break;
                                            }
                                        }
                                        if (!alreadyExists) {
                                            News newsItem = new News();
                                            newsItem.setNid(newsDetail.getNid());
                                            newsItem.setNtitle(newsDetail.getNtitle());
                                            newsItem.setNauthor(newsDetail.getNauthor());
                                            newsItem.setNcreatedate(newsDetail.getNcreatedate());
                                            newsItem.setNpicpath(newsDetail.getNpicpath());
                                            newsItem.setNsummary(newsDetail.getNsummary());
                                            newsItem.setNtid(newsDetail.getNtid());
                                            newsItem.setNtname(newsDetail.getNtname());
                                            searchResultList.add(newsItem);
                                            // No need to call notifyDataSetChanged here, it will be called once all requests are done
                                        }
                                    });
                                }
                            }
                        } else {
                            Log.e("SearchFragment", "获取新闻详情数据失败 (nid=" + nid + "): " + response);
                        }
                    }
                } finally {
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