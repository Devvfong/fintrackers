package com.example.fintracker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private Context context;
    private List<Transaction> transactionList;

    public TransactionAdapter(Context context, List<Transaction> transactionList) {
        this.context = context;
        this.transactionList = transactionList;
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
        holder.tvDescription.setText(transaction.getDescription());

        // Format amount
        String amountText;
        if ("Income".equals(transaction.getType())) {
            amountText = "+ $" + String.format(Locale.US, "%.2f", transaction.getAmount());
            holder.tvAmount.setTextColor(context.getResources().getColor(R.color.green_income));
        } else {
            amountText = "- $" + String.format(Locale.US, "%.2f", transaction.getAmount());
            holder.tvAmount.setTextColor(context.getResources().getColor(R.color.red_expense));
        }
        holder.tvAmount.setText(amountText);

        // Format time
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.US);
        String time = sdf.format(new Date(transaction.getTimestamp()));
        holder.tvTime.setText(time);

        // Set category icon (you can customize this based on category)
        holder.ivCategoryIcon.setImageResource(R.drawable.ic_home);
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    public static class TransactionViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCategoryIcon;
        TextView tvCategory, tvDescription, tvAmount, tvTime;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCategoryIcon = itemView.findViewById(R.id.ivCategoryIcon);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }
}
