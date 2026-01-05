package com.example.fintracker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder> {

    public interface Listener {
        void onRowClick(Budget budget);
        void onEditClick(Budget budget);      // kept for future (edit button/icon)
        void onCheckboxChanged();             // kept (not used in your current layout)
    }

    private final Context context;
    private final List<Budget> budgetList;
    private final Listener listener;

    private final Map<String, Integer> categoryColors = new HashMap<>();

    public BudgetAdapter(Context context, List<Budget> budgetList, Listener listener) {
        this.context = context;
        this.budgetList = budgetList;
        this.listener = listener;
        initCategoryColors();
    }

    private void initCategoryColors() {
        categoryColors.put("Shopping", R.color.category_shopping);
        categoryColors.put("Food", R.color.category_food);
        categoryColors.put("Transportation", R.color.category_transport);
        categoryColors.put("Transport", R.color.category_transport);
        categoryColors.put("Entertainment", R.color.purple_primary);
        categoryColors.put("Subscription", R.color.category_subscription);
        categoryColors.put("Bills", R.color.green);
        categoryColors.put("Health", R.color.red);
        categoryColors.put("Education", R.color.blue);
        categoryColors.put("Salary", R.color.category_salary);
        categoryColors.put("Other", R.color.text_secondary);
    }

    private int getCategoryColorRes(String categoryName) {
        Integer res = categoryColors.get(categoryName);
        return res != null ? res : R.color.purple_primary;
    }

    private int getStatusColorRes(int percentage) {
        if (percentage <= 70) return R.color.budget_safe;
        if (percentage <= 90) return R.color.budget_warning;
        if (percentage <= 100) return R.color.budget_critical;
        return R.color.budget_over;
    }

    @NonNull
    @Override
    public BudgetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_budget, parent, false);
        return new BudgetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BudgetViewHolder holder, int position) {
        Budget budget = budgetList.get(position);

        holder.tvCategoryName.setText(budget.getCategoryName());

        int categoryColorRes = getCategoryColorRes(budget.getCategoryName());
        int categoryColor = ContextCompat.getColor(context, categoryColorRes);
        holder.viewCategoryDot.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(categoryColor)
        );

        int percentage = budget.getPercentageSpent();
        boolean isOver = budget.isExceeded();

        String formattedSpent = formatAmount(budget.getSpent(), budget.getCurrency());
        String formattedAmount = formatAmount(budget.getAmount(), budget.getCurrency());
        String formattedRemaining = formatAmount(Math.max(0, budget.getRemaining()), budget.getCurrency());

        if (isOver) {
            holder.tvRemaining.setText(context.getString(R.string.remaining_format, "$0"));
            holder.tvRemaining.setTextColor(ContextCompat.getColor(context, R.color.budget_over));
        } else {
            holder.tvRemaining.setText(context.getString(R.string.remaining_format, formattedRemaining));

            int textColorRes;
            if (percentage <= 70) textColorRes = R.color.text_primary;
            else if (percentage <= 90) textColorRes = R.color.budget_warning;
            else textColorRes = R.color.budget_critical;

            holder.tvRemaining.setTextColor(ContextCompat.getColor(context, textColorRes));
        }

        holder.tvBudgetInfo.setText(
                context.getString(R.string.budget_info_format, formattedSpent, formattedAmount)
        );

        holder.progressBar.setProgress(Math.min(100, percentage));
        int progressColorRes = isOver ? R.color.budget_over : getStatusColorRes(percentage);
        int progressColor = ContextCompat.getColor(context, progressColorRes);
        holder.progressBar.getProgressDrawable().setTint(progressColor);

        if (isOver) {
            holder.ivWarning.setVisibility(View.VISIBLE);
            holder.tvWarning.setVisibility(View.VISIBLE);
        } else {
            holder.ivWarning.setVisibility(View.GONE);
            holder.tvWarning.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onRowClick(budget);
        });
    }

    @Override
    public int getItemCount() {
        return budgetList.size();
    }

    private String formatAmount(double amount, String currency) {
        if ("KHR".equals(currency)) {
            DecimalFormat khrFormat = new DecimalFormat("#,###");
            return "៛" + khrFormat.format(amount);
        } else {
            NumberFormat usdFormat = NumberFormat.getCurrencyInstance(Locale.US);
            return usdFormat.format(amount);
        }
    }

    static class BudgetViewHolder extends RecyclerView.ViewHolder {
        View viewCategoryDot;
        TextView tvCategoryName, tvRemaining, tvBudgetInfo, tvWarning;
        ProgressBar progressBar;
        TextView ivWarning;

        BudgetViewHolder(@NonNull View itemView) {
            super(itemView);
            viewCategoryDot = itemView.findViewById(R.id.viewCategoryDot);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            tvRemaining = itemView.findViewById(R.id.tvRemaining);
            tvBudgetInfo = itemView.findViewById(R.id.tvBudgetInfo);
            tvWarning = itemView.findViewById(R.id.tvWarning);
            progressBar = itemView.findViewById(R.id.progressBar);
            ivWarning = itemView.findViewById(R.id.ivWarning);
        }
    }
}
