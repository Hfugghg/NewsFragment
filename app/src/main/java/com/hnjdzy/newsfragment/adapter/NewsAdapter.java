package com.hnjdzy.newsfragment.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hnjdzy.newsfragment.model.News;
import com.hnjdzy.newsfragment.ui.activity.NewsDetailActivity;
import com.hnjdzy.newsfragment.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {

    private List<News> newsList;
    private final SharedPreferences sharedPreferences;
    private final Context context; // 需要 Context 来判断主题

    public NewsAdapter(List<News> newsList, SharedPreferences sharedPreferences, Context context) {
        this.newsList = newsList;
        this.sharedPreferences = sharedPreferences;
        this.context = context;
    }

    public void setNewsList(List<News> newsList) {
        this.newsList = newsList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_news, parent, false);
        return new NewsViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        News currentNews = newsList.get(position);
        holder.tvTitle.setText(currentNews.getNtitle());
        holder.tvSummary.setText(currentNews.getNsummary());
        holder.tvAuthor.setText(currentNews.getNauthor());

        // 格式化日期
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        String formattedDate = sdf.format(new Date(currentNews.getNcreatedate()));
        holder.tvDate.setText(formattedDate);

        // 判断当前是否是暗色模式
        int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isDarkMode = nightModeFlags == Configuration.UI_MODE_NIGHT_YES;

        // 根据阅读状态和主题设置颜色
        int colorToSet;
        if (currentNews.isRead()) {
            colorToSet = Color.GRAY;
        } else {
            colorToSet = isDarkMode ? Color.WHITE : holder.defaultTextColor; // 暗色模式下设置为白色，否则使用默认颜色
        }
        holder.tvTitle.setTextColor(colorToSet);
        holder.tvSummary.setTextColor(colorToSet);
        holder.tvAuthor.setTextColor(colorToSet);
        holder.tvDate.setTextColor(colorToSet);

        // 设置点击监听器 (保持不变)
        holder.itemView.setOnClickListener(v -> {
            int newsId = currentNews.getNid();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(String.valueOf(newsId), true);
            editor.apply();
            currentNews.setRead(true);
            notifyItemChanged(position);
            Intent intent = new Intent(v.getContext(), NewsDetailActivity.class);
            intent.putExtra("nid", newsId);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return newsList == null ? 0 : newsList.size();
    }

    static class NewsViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvSummary;
        TextView tvAuthor;
        TextView tvDate;
        int defaultTextColor; // 保存默认颜色

        NewsViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_news_title);
            tvSummary = itemView.findViewById(R.id.tv_news_summary);
            tvAuthor = itemView.findViewById(R.id.tv_news_author);
            tvDate = itemView.findViewById(R.id.tv_news_date);
            defaultTextColor = tvTitle.getTextColors().getDefaultColor(); // 获取标题的默认颜色，其他 TextView 颜色应该一致
        }
    }
}