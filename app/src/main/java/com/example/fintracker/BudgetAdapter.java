package com.example.fintracker;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
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
        void onEditClick(Budget budget);
        void onExtendClick(Budget budget);
        void onCheckboxChanged();
    }

    private final Context context;
    private final List<Budget> budgetList;
    private final Listener listener;
    private final Map<String, Integer> categoryColors = new HashMap<>();
    private final Map<String, Integer> categoryIcons = new HashMap<>();

    public BudgetAdapter(Context context, List<Budget> budgetList, Listener listener) {
        this.context = context;
        this.budgetList = budgetList;
        this.listener = listener;
        initCategoryColors();
        initCategoryIcons();
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

        // Custom names
        categoryColors.put("Tennis", R.color.green);
        categoryColors.put("Wine", R.color.red);
        categoryColors.put("Sport", R.color.blue);
        categoryColors.put("Test", R.color.yellow);
    }

    /**
     * Initialize category icons using REAL icons from drawable folder
     * DEFAULT FALLBACK: other_admission_24px
     */
    private void initCategoryIcons() {
        // Shopping - shopping bag icon
        categoryIcons.put("Shopping", R.drawable.shopping_bag_24px);

        // Food/Wine - meal icon
        categoryIcons.put("Food", R.drawable.award_meal_24px);
        categoryIcons.put("Wine", R.drawable.award_meal_24px);

        // Transportation - transportation icon
        categoryIcons.put("Transportation", R.drawable.emoji_transportation_24px);
        categoryIcons.put("Transport", R.drawable.emoji_transportation_24px);

        // Entertainment - admission icon (default for unknown too)
        categoryIcons.put("Entertainment", R.drawable.other_admission_24px);

        // Subscription - subscription icon
        categoryIcons.put("Subscription", R.drawable.subscriptions_24px);

        // Bills - money icon
        categoryIcons.put("Bills", R.drawable.attach_money_24px);

        // Salary - money icon
        categoryIcons.put("Salary", R.drawable.attach_money_24px);

        // If no specific mapping, getCategoryIcon() will return other_admission_24px as default
    }

    private int getCategoryColorRes(String categoryName) {
        Integer res = categoryColors.get(categoryName);
        return res != null ? res : R.color.purple_primary;
    }

    /**
     * Get category icon - returns other_admission_24px as default
     */
    private int getCategoryIcon(String categoryName) {
        Integer icon = categoryIcons.get(categoryName);
        // DEFAULT: other_admission_24px for any unmapped category
        return icon != null ? icon : R.drawable.other_admission_24px;
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

        // Set dot color
        holder.viewCategoryDot.setBackgroundTintList(ColorStateList.valueOf(categoryColor));

        // Set category icon and background color
        int categoryIcon = getCategoryIcon(budget.getCategoryName());
        holder.ivCategoryIcon.setImageResource(categoryIcon);
        holder.cvCategoryIcon.setCardBackgroundColor(categoryColor);

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

            holder.tvExtend.setVisibility(View.VISIBLE);
            holder.tvExtend.setOnClickListener(v -> {
                if (listener != null) listener.onExtendClick(budget);
            });
        } else {
            holder.ivWarning.setVisibility(View.GONE);
            holder.tvWarning.setVisibility(View.GONE);

            holder.tvExtend.setVisibility(View.GONE);
            holder.tvExtend.setOnClickListener(null);
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

        CardView cvCategoryIcon;
        ImageView ivCategoryIcon;
        View viewCategoryDot;
        TextView tvCategoryName, tvRemaining, tvBudgetInfo, tvWarning;
        ProgressBar progressBar;
        TextView ivWarning;
        TextView tvExtend;

        BudgetViewHolder(@NonNull View itemView) {
            super(itemView);
            cvCategoryIcon = itemView.findViewById(R.id.cvCategoryIcon);
            ivCategoryIcon = itemView.findViewById(R.id.ivCategoryIcon);
            viewCategoryDot = itemView.findViewById(R.id.viewCategoryDot);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            tvRemaining = itemView.findViewById(R.id.tvRemaining);
            tvBudgetInfo = itemView.findViewById(R.id.tvBudgetInfo);
            tvWarning = itemView.findViewById(R.id.tvWarning);
            progressBar = itemView.findViewById(R.id.progressBar);
            ivWarning = itemView.findViewById(R.id.ivWarning);
            tvExtend = itemView.findViewById(R.id.tvExtend);
        }
    }
}