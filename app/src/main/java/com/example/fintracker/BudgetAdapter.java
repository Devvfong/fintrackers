package com.example.fintracker;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
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
 * - 0-70%: Safe (Green/Category Color)
 * - 71-90%: Warning (Yellow/Orange)
 * - 91-100%: Critical (Dark Orange/Red)
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
    private final Map<String, String> categoryColors = new HashMap<>();

    public BudgetAdapter(Context context, List<Budget> budgetList, Listener listener) {
        this.context = context;
        this.budgetList = budgetList;
        this.listener = listener;
        initCategoryColors();
    }

    /**
     * Initialize category colors
     */
    private void initCategoryColors() {
        categoryColors.put("Shopping", "#FCAC12");      // Orange
        categoryColors.put("Food", "#FD3C4A");          // Red
        categoryColors.put("Transportation", "#0077FF"); // Blue
        categoryColors.put("Entertainment", "#7F3DFF");  // Purple
        categoryColors.put("Bills", "#00A86B");          // Green
        categoryColors.put("Health", "#FF6B6B");         // Light Red
        categoryColors.put("Education", "#4ECDC4");      // Teal
        categoryColors.put("Other", "#B4B4B4");          // Gray
        // Add more categories as needed
    }

    /**
     * Get color for category
     */
    private String getCategoryColor(String categoryName) {
        return categoryColors.getOrDefault(categoryName, "#7F3DFF"); // Default purple
    }

    /**
     * Get status color based on percentage
     */
    private String getStatusColor(int percentage) {
        if (percentage <= 70) {
            return "#00A86B"; // Safe - Green
        } else if (percentage <= 90) {
            return "#FFA500"; // Warning - Orange
        } else if (percentage <= 100) {
            return "#FF6347"; // Critical - Tomato Red
        } else {
            return "#FF0000"; // Over Budget - Red
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
        String categoryColor = getCategoryColor(budget.getCategoryName());
        holder.viewCategoryDot.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(Color.parseColor(categoryColor)));

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

        // 4. Set remaining amount with color
        if (isOver) {
            holder.tvRemaining.setText("Remaining $0");
            holder.tvRemaining.setTextColor(Color.parseColor("#FF0000")); // Red
        } else {
            holder.tvRemaining.setText("Remaining " + formattedRemaining);

            // Color based on status
            if (percentage <= 70) {
                holder.tvRemaining.setTextColor(Color.parseColor("#212121")); // Normal black
            } else if (percentage <= 90) {
                holder.tvRemaining.setTextColor(Color.parseColor("#FFA500")); // Warning orange
            } else {
                holder.tvRemaining.setTextColor(Color.parseColor("#FF6347")); // Critical red
            }
        }

        // 5. Set budget info
        holder.tvBudgetInfo.setText(formattedSpent + " of " + formattedAmount);

        // 6. Set progress bar
        holder.progressBar.setProgress(Math.min(100, percentage));

        // Progress bar color based on status
        String progressColor = isOver ? "#FF0000" : getStatusColor(percentage);
        holder.progressBar.getProgressDrawable().setTint(Color.parseColor(progressColor));

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
        TextView ivWarning;  // Using TextView for emoji

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