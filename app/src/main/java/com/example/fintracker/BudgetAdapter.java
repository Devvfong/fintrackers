package com.example.fintracker;

import android.content.Context;
import android.graphics.Color;
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

/**
 * BudgetAdapter with Status-Based Colors
 *
 * Status Rules:
 * - 0-70%: Safe (Green)
 * - 71-90%: Warning (Orange)
 * - 91-100%: Critical (Tomato Red)
 * - Over 100%: Over Budget (Red)
 */
public class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder> {

    public interface Listener {
        void onEditClick(Budget budget);
        void onCheckboxChanged();
    }

    private final Context context;
    private final List<Budget> budgetList;
    private final Listener listener;
    private boolean isDeleteMode = false;

    // Category colors mapping
    private final Map<String, Integer> categoryColors = new HashMap<>();

    public BudgetAdapter(Context context, List<Budget> budgetList, Listener listener) {
        this.context = context;
        this.budgetList = budgetList;
        this.listener = listener;
        initCategoryColors();
    }

    /**
     * Initialize category colors using color resources
     */
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
        categoryColors.put("Tennis", R.color.green);
        categoryColors.put("Wine", R.color.red);
        categoryColors.put("Sport", R.color.blue);
        categoryColors.put("Test", R.color.yellow);
    }

    /**
     * Get color resource ID for category
     */
    private int getCategoryColorRes(String categoryName) {
        return categoryColors.getOrDefault(categoryName, R.color.purple_primary);
    }

    /**
     * Get status color resource based on percentage
     */
    private int getStatusColorRes(int percentage) {
        if (percentage <= 70) {
            return R.color.budget_safe;       // Green
        } else if (percentage <= 90) {
            return R.color.budget_warning;    // Orange
        } else if (percentage <= 100) {
            return R.color.budget_critical;   // Tomato Red
        } else {
            return R.color.budget_over;       // Red
        }
    }

    public void setDeleteMode(boolean deleteMode) {
        this.isDeleteMode = deleteMode;
        notifyDataSetChanged();
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

        // 1. Set category name and color
        holder.tvCategoryName.setText(budget.getCategoryName());
        int categoryColorRes = getCategoryColorRes(budget.getCategoryName());
        int categoryColor = ContextCompat.getColor(context, categoryColorRes);
        holder.viewCategoryDot.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(categoryColor));

        // 2. Calculate percentage and status
        int percentage = budget.getPercentageSpent();
        boolean isOver = budget.isExceeded();

        // 3. Format amounts
        String formattedSpent = formatAmount(budget.getSpent(), budget.getCurrency());
        String formattedAmount = formatAmount(budget.getAmount(), budget.getCurrency());
        String formattedRemaining = formatAmount(
                Math.max(0, budget.getRemaining()),
                budget.getCurrency()
        );

        // 4. Set remaining amount with color using string resource
        if (isOver) {
            holder.tvRemaining.setText(
                    context.getString(R.string.remaining_format, "$0")
            );
            holder.tvRemaining.setTextColor(
                    ContextCompat.getColor(context, R.color.budget_over)
            );
        } else {
            holder.tvRemaining.setText(
                    context.getString(R.string.remaining_format, formattedRemaining)
            );

            // Color based on status
            int textColorRes;
            if (percentage <= 70) {
                textColorRes = R.color.text_primary;  // Normal black
            } else if (percentage <= 90) {
                textColorRes = R.color.budget_warning;  // Warning orange
            } else {
                textColorRes = R.color.budget_critical;  // Critical red
            }
            holder.tvRemaining.setTextColor(
                    ContextCompat.getColor(context, textColorRes)
            );
        }

        // 5. Set budget info using string resource
        holder.tvBudgetInfo.setText(
                context.getString(R.string.budget_info_format, formattedSpent, formattedAmount)
        );

        // 6. Set progress bar
        holder.progressBar.setProgress(Math.min(100, percentage));

        // Progress bar color based on status
        int progressColorRes = isOver ? R.color.budget_over : getStatusColorRes(percentage);
        int progressColor = ContextCompat.getColor(context, progressColorRes);
        holder.progressBar.getProgressDrawable().setTint(progressColor);

        // 7. Show/hide warning
        if (isOver) {
            holder.ivWarning.setVisibility(View.VISIBLE);
            holder.tvWarning.setVisibility(View.VISIBLE);
        } else {
            holder.ivWarning.setVisibility(View.GONE);
            holder.tvWarning.setVisibility(View.GONE);
        }

        // 8. Click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditClick(budget);
            }
        });
    }

    @Override
    public int getItemCount() {
        return budgetList.size();
    }

    /**
     * Format amount based on currency
     */
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