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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private TextView textViewNoResults; // 用于显示未找到相关内容

    private OkHttpClient client;
    private Gson gson;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);
        editTextSearch = view.findViewById(R.id.editTextSearch);
        buttonSearchSimple = view.findViewById(R.id.buttonSearchSimple);
        recyclerViewSearchResults = view.findViewById(R.id.recyclerViewSearchResults);
        checkBoxDetailedSearch = view.findViewById(R.id.checkBoxDetailedSearch);
        buttonSearchDetailed = view.findViewById(R.id.buttonSearchDetailed);
        textViewNoResults = view.findViewById(R.id.textViewNoResults); // 初始化未找到结果的 TextView

        recyclerViewSearchResults.setLayoutManager(new LinearLayoutManager(getContext()));
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("news_read_status", Context.MODE_PRIVATE);
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

        // 点击“详细搜索”文字弹出解释
        TextView textViewDetailedSearchExplanation = view.findViewById(R.id.textViewDetailedSearchExplanation);
        textViewDetailedSearchExplanation.setOnClickListener(v -> showDetailedSearchExplanation());

        // 初始状态隐藏未找到结果的 TextView
        textViewNoResults.setVisibility(View.GONE);

        return view;
    }

    private void showNoResultsMessage() {
        recyclerViewSearchResults.setVisibility(View.GONE);
        textViewNoResults.setVisibility(View.VISIBLE);
    }

    private void hideNoResultsMessage() {
        recyclerViewSearchResults.setVisibility(View.VISIBLE);
        textViewNoResults.setVisibility(View.GONE);
    }

    private void performSimpleSearch() {
        String keyword = editTextSearch.getText().toString().trim();
        if (TextUtils.isEmpty(keyword)) {
            android.widget.Toast.makeText(getContext(), "请输入搜索关键词", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        searchResultList.clear();
        searchResultsAdapter.notifyDataSetChanged();
        hideNoResultsMessage(); // 搜索前隐藏未找到结果的提示
        fetchNewsTypesAndSearch(keyword, false); // 执行普通搜索
    }

    private void performDetailedSearch() {
        String keyword = editTextSearch.getText().toString().trim();
        if (TextUtils.isEmpty(keyword)) {
            android.widget.Toast.makeText(getContext(), "请输入搜索关键词", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        searchResultList.clear();
        searchResultsAdapter.notifyDataSetChanged();
        hideNoResultsMessage(); // 搜索前隐藏未找到结果的提示
        fetchNewsTypesAndSearch(keyword, true); // 执行详细搜索
    }

    private void fetchNewsTypesAndSearch(String keyword, boolean isDetailed) {
        String newsTypeUrl = "http://182.42.154.48:8088/news/servlet/ApiServlet?opr=topic";
        Request request = new Request.Builder().url(newsTypeUrl).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> android.widget.Toast.makeText(getContext(), "获取新闻分类失败", android.widget.Toast.LENGTH_SHORT).show());
                    Log.e("SearchFragment", "获取新闻分类失败: " + e.getMessage());
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    List<NewsType> newsTypeList = gson.fromJson(responseData, new TypeToken<List<NewsType>>() {}.getType());
                    if (newsTypeList != null) {
                        AtomicBoolean foundResults = new AtomicBoolean(false);
                        for (NewsType newsType : newsTypeList) {
                            if (isDetailed) {
                                if (fetchNewsListAndDetailsAndFilter(newsType.getTid(), keyword)) {
                                    foundResults.set(true);
                                }
                            } else {
                                if (fetchNewsListAndFilter(newsType.getTid(), keyword)) {
                                    foundResults.set(true);
                                }
                            }
                        }
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (!foundResults.get() && searchResultList.isEmpty()) {
                                    showNoResultsMessage();
                                }
                            });
                        }
                    } else {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> android.widget.Toast.makeText(getContext(), "获取新闻分类数据失败", android.widget.Toast.LENGTH_SHORT).show());
                            Log.e("SearchFragment", "获取新闻分类数据失败: " + response);
                        }
                    }
                }
            }
        });
    }

    private boolean fetchNewsListAndFilter(int tid, String keyword) {
        String newsListUrl = "http://182.42.154.48:8088/news/servlet/ApiServlet?opr=newslist&tid=" + tid;
        Request request = new Request.Builder().url(newsListUrl).build();
        AtomicBoolean resultsFound = new AtomicBoolean(false);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("SearchFragment", "获取新闻列表失败 (tid=" + tid + "): " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    List<News> newsList = gson.fromJson(responseData, new TypeToken<List<News>>() {}.getType());
                    if (newsList != null) {
                        List<News> filteredList = new ArrayList<>();
                        for (News news : newsList) {
                            if ((news.getNtitle() != null && news.getNtitle().contains(keyword)) ||
                                    (news.getNsummary() != null && news.getNsummary().contains(keyword))) {
                                filteredList.add(news);
                                resultsFound.set(true);
                            }
                        }
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                searchResultList.addAll(filteredList);
                                searchResultsAdapter.notifyDataSetChanged();
                                if (!searchResultList.isEmpty()) {
                                    hideNoResultsMessage();
                                }
                            });
                        }
                    } else {
                        Log.e("SearchFragment", "获取新闻列表数据失败 (tid=" + tid + "): " + response);
                    }
                }
            }
        });
        return resultsFound.get();
    }

    private boolean fetchNewsListAndDetailsAndFilter(int tid, String keyword) {
        String newsListUrl = "http://182.42.154.48:8088/news/servlet/ApiServlet?opr=newslist&tid=" + tid;
        Request request = new Request.Builder().url(newsListUrl).build();
        AtomicBoolean resultsFound = new AtomicBoolean(false);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("SearchFragment", "获取新闻列表失败 (详细搜索, tid=" + tid + "): " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    List<News> newsList = gson.fromJson(responseData, new TypeToken<List<News>>() {}.getType());
                    if (newsList != null) {
                        for (News news : newsList) {
                            if (fetchNewsDetailsAndFilter(news.getNid(), keyword)) {
                                resultsFound.set(true);
                            }
                        }
                    } else {
                        Log.e("SearchFragment", "获取新闻列表数据失败 (详细搜索, tid=" + tid + "): " + response);
                    }
                }
            }
        });
        return resultsFound.get();
    }

    private boolean fetchNewsDetailsAndFilter(int nid, String keyword) {
        String newsDetailsUrl = "http://182.42.154.48:8088/news/servlet/ApiServlet?opr=readnews&nid=" + nid;
        Request request = new Request.Builder().url(newsDetailsUrl).build();
        AtomicBoolean resultFound = new AtomicBoolean(false);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("SearchFragment", "获取新闻详情失败 (nid=" + nid + "): " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
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
                                        searchResultsAdapter.notifyDataSetChanged();
                                        hideNoResultsMessage();
                                        resultFound.set(true);
                                    }
                                });
                            }
                        }
                    } else {
                        Log.e("SearchFragment", "获取新闻详情数据失败 (nid=" + nid + "): " + response);
                    }
                }
            }
        });
        return resultFound.get();
    }

    private void showDetailedSearchExplanation() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.detailed_search)
                .setMessage(R.string.detailed_search_explanation)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}