package com.example.fintracker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder> {

    private final Context context;
    private final List<Budget> budgetList;

    public BudgetAdapter(Context context, List<Budget> budgetList) {
        this.context = context;
        this.budgetList = budgetList;
    }

    @NonNull
    @Override
    public BudgetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_budget, parent, false);
        return new BudgetViewHolder(view);
    }

    @SuppressLint("UseCompatLoadingForColorStateLists")
    @Override
    public void onBindViewHolder(@NonNull BudgetViewHolder holder, int position) {
        Budget budget = budgetList.get(position);

        holder.tvCategory.setText(budget.getCategory());

        // Calculate remaining
        double remaining = budget.getRemaining();
        String remainingText = "Remaining $" + String.format(Locale.US, "%.2f", remaining);
        holder.tvRemaining.setText(remainingText);

        // Budget info
        String budgetInfo = String.format(Locale.US, "$%.2f of $%.2f", budget.getSpent(), budget.getLimit());
        holder.tvBudgetInfo.setText(budgetInfo);

        // Progress bar
        int progress = (int) ((budget.getSpent() / budget.getLimit()) * 100);
        holder.progressBar.setProgress(Math.min(progress, 100));

        // Check if exceeded
        if (budget.isExceeded()) {
            holder.tvWarning.setVisibility(View.VISIBLE);
            holder.tvExceed.setVisibility(View.VISIBLE);
            holder.progressBar.setProgressTintList(context.getResources().getColorStateList(R.color.red_expense));
        } else {
            holder.tvWarning.setVisibility(View.GONE);
            holder.tvExceed.setVisibility(View.GONE);
            holder.progressBar.setProgressTintList(context.getResources().getColorStateList(R.color.purple_primary));
        }

        // Set category icon
        holder.ivCategoryIcon.setImageResource(R.drawable.ic_home);
    }

    @Override
    public int getItemCount() {
        return budgetList.size();
    }

    public static class BudgetViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCategoryIcon, tvWarning;
        TextView tvCategory, tvRemaining, tvBudgetInfo, tvExceed;
        ProgressBar progressBar;

        public BudgetViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCategoryIcon = itemView.findViewById(R.id.ivCategoryIcon);
            tvWarning = itemView.findViewById(R.id.tvWarning);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvRemaining = itemView.findViewById(R.id.tvRemaining);
            tvBudgetInfo = itemView.findViewById(R.id.tvBudgetInfo);
            tvExceed = itemView.findViewById(R.id.tvExceed);
            progressBar = itemView.findViewById(R.id.progressBar);
        }
    }
}
