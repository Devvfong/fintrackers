package com.example.fintracker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    public interface Listener {
        void onEditClick(@NonNull Transaction transaction);
        void onRowClick(@NonNull Transaction transaction);
    }

    @NonNull private final Context context;
    @NonNull private final List<Transaction> transactionList;
    private final Listener listener;

    // ✅ BACKWARD COMPATIBLE for HomeFragment
    public TransactionAdapter(@NonNull Context context, @NonNull List<Transaction> transactionList) {
        this(context, transactionList, null);
    }

    // ✅ MAIN constructor for TransactionFragment
    public TransactionAdapter(@NonNull Context context, @NonNull List<Transaction> transactionList, Listener listener) {
        this.context = context;
        this.transactionList = transactionList;
        this.listener = listener;
    }

    public void removeItem(int position) {
        if (position < 0 || position >= transactionList.size()) return;
        transactionList.remove(position);
        notifyItemRemoved(position);
    }

    public void updateTransactions(@NonNull List<Transaction> newTransactions) {
        transactionList.clear();
        transactionList.addAll(newTransactions);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = transactionList.get(position);

        holder.tvCategory.setText(transaction.getCategory());

        String desc = transaction.getDescription();
        if (desc != null && !desc.trim().isEmpty()) {
            holder.tvDescription.setText(desc);
            holder.tvDescription.setVisibility(View.VISIBLE);
        } else {
            holder.tvDescription.setVisibility(View.GONE);
        }

        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
        if ("Income".equals(transaction.getType())) {
            holder.tvAmount.setText("+ " + formatter.format(transaction.getAmount()));
            holder.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.green_income));
        } else {
            holder.tvAmount.setText("- " + formatter.format(transaction.getAmount()));
            holder.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.red_expense));
        }

        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        holder.tvTime.setText(timeFormat.format(new Date(transaction.getTimestamp())));

        setCategoryIcon(holder, transaction.getCategory());

        // ✅ EDIT ICON CLICK
        if (listener != null) {
            holder.btnEdit.setOnClickListener(v -> listener.onEditClick(transaction));
            holder.itemView.setOnClickListener(v -> listener.onRowClick(transaction));
        }
    }

    private void setCategoryIcon(@NonNull TransactionViewHolder holder, String category) {
        int iconRes = android.R.drawable.ic_menu_more;
        int bgColorRes = R.color.border_light;

        if ("Shopping".equals(category)) {
            iconRes = android.R.drawable.ic_menu_gallery;
            bgColorRes = R.color.category_shopping;
        } else if ("Subscription".equals(category)) {
            iconRes = android.R.drawable.ic_dialog_info;
            bgColorRes = R.color.category_subscription;
        } else if ("Food".equals(category)) {
            iconRes = android.R.drawable.ic_menu_info_details;
            bgColorRes = R.color.category_food;
        } else if ("Salary".equals(category)) {
            iconRes = android.R.drawable.ic_menu_agenda;
            bgColorRes = R.color.category_salary;
        } else if ("Transport".equals(category)) {
            iconRes = android.R.drawable.ic_menu_directions;
            bgColorRes = R.color.category_transport;
        }

        holder.ivCategoryIcon.setImageResource(iconRes);
        holder.ivCategoryIcon.setColorFilter(ContextCompat.getColor(context, android.R.color.white));
        holder.cvCategoryIcon.setCardBackgroundColor(ContextCompat.getColor(context, bgColorRes));
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        CardView cvCategoryIcon;
        ImageView ivCategoryIcon;
        TextView tvCategory, tvDescription, tvAmount, tvTime;
        ImageButton btnEdit; // ✅ MISSING BEFORE

        TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            cvCategoryIcon = itemView.findViewById(R.id.cvCategoryIcon);
            ivCategoryIcon = itemView.findViewById(R.id.ivCategoryIcon);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvTime = itemView.findViewById(R.id.tvTime);
            btnEdit = itemView.findViewById(R.id.btnEdit); // ✅ FIXED
        }
    }
}
