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
    @NonNull private final Listener listener;

    public TransactionAdapter(@NonNull Context context, @NonNull List<Transaction> transactionList, @NonNull Listener listener) {
        this.context = context;
        this.transactionList = transactionList;
        this.listener = listener;
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
        } else {
            holder.tvDescription.setText("");
        }

        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
        boolean isIncome = "Income".equals(transaction.getType());
        holder.tvAmount.setText((isIncome ? "+ " : "- ") + formatter.format(transaction.getAmount()));
        holder.tvAmount.setTextColor(ContextCompat.getColor(context,
                isIncome ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));

        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        holder.tvTime.setText(timeFormat.format(new Date(transaction.getTimestamp())));

        // YOUR EXACT ICONS FROM LAYOUT
        setCategoryIcon(holder, transaction.getCategory());

        holder.itemView.setOnClickListener(v -> listener.onRowClick(transaction));
        holder.btnEdit.setOnClickListener(v -> listener.onEditClick(transaction));
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    private void setCategoryIcon(TransactionViewHolder holder, String category) {
        int iconRes = android.R.drawable.ic_menu_info_details;

        switch (category) {
            case "Salary":
                iconRes = android.R.drawable.ic_menu_add;           // ✨ + (Growth)
                holder.cvCategoryIcon.setCardBackgroundColor(0xFF4CAF50);  // Green
                break;
            case "Food":
                iconRes = android.R.drawable.ic_menu_preferences;   // 🍲 Food menu
                holder.cvCategoryIcon.setCardBackgroundColor(0xFFFF9800);  // Orange
                break;
            case "Shopping":
                iconRes = android.R.drawable.ic_dialog_map;         // 🛒 Shopping bag
                holder.cvCategoryIcon.setCardBackgroundColor(0xFF2196F3);  // Blue
                break;
            case "Subscription":
                iconRes = android.R.drawable.ic_lock_lock;          // 🔒 Recurring
                holder.cvCategoryIcon.setCardBackgroundColor(0xFF9C27B0);  // Purple
                break;
        }

        holder.ivCategoryIcon.setImageResource(iconRes);
        holder.ivCategoryIcon.setColorFilter(0xFFFFFFFF); // White
    }




    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory, tvDescription, tvAmount, tvTime;
        CardView cvCategoryIcon;
        ImageView ivCategoryIcon;
        ImageButton btnEdit;

        TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvTime = itemView.findViewById(R.id.tvTime);
            cvCategoryIcon = itemView.findViewById(R.id.cvCategoryIcon);
            ivCategoryIcon = itemView.findViewById(R.id.ivCategoryIcon);  // ✅ YOUR EXACT ID
            btnEdit = itemView.findViewById(R.id.btnEdit);
        }
    }
}
